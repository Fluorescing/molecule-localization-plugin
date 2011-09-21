/**
 * Copyright (C) 2011 Shane Stahlheber
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * Acknowledgments:
 * This plug-in was developed with support from California State University 
 * Program for Education and Research in Biotechnology (CSUPERB).
 */

import java.awt.Font;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.Prefs;
import ij.gui.GenericDialog;
import ij.measure.ResultsTable;
import ij.process.ImageProcessor;

/**
 * Core code for the particle locating plug-in.  This class can be extended to
 * allow for different "flavors" of particle localization.  
 * 
 * <p>For example, {@code MLEParticleLocator} extends this class to use a 
 * Maximum Likelihood Estimator as a final image process.
 * 
 * <p>Each instance of this class stands alone, and multiple instances should be
 * able to run at the same time without issue.
 */
public abstract class AbstractParticleLocator implements SettingsDialog {
    
    private static final double DEFAULT_SIZE = 106.0;   // pixel size
    private static final int SOME_PRIME = 101;          // any prime number
    private static final int DEFAULT_SNR = 4;           // signal-to-noise ratio
    private static final int DEFAULT_SCALE = 300;       // intensity scale
    
    private static final String SNR = 
            "Localize_Particles.ParticleLocatorContext.snr";
    private static final String PIXEL_SIZE = 
            "Localize_Particles.ParticleLocatorContext.pixel_size";
    private static final String PHOTON_SCALE = 
            "Localize_Particles.ParticleLocatorContext.photon_scale";
    
    private final ImageStack stack;                 // stack of images
    private int localizations;                      // number of particles
    private final Reconstruction reconstruct;       // reconstruction instance
    private final BackgroundNoise noiseEstimator;
    private final ImageStack debugStack;            // stack of debugging images
    private final ResultsTable results;             // ImageJ results table
    private double signalNoiseRatio;
    private double photonScale;                     // photons per max. value
    private double pixelSize;
    private boolean debugImages = false;
    
    /** 
     * An array of instances of process objects.  This array holds the 
     * processes to be performed on potential particle.  Order is important.
     */
    protected List<ImageProcess> processes;
    
    /**
     * An array of instances of setting objects.  These are objects that give
     * the user a set of options to change at the beginning of the plug-in.
     * Order does not matter.
     */
    protected List<SettingsDialog> settings;
    
    /**
     * An array of debugging objects.  These objects display the debugging 
     * statistics of the running processes.
     */
    protected List<DebugStats> debugging;
    
    /**
     * Retrieves the signal-to-noise ratio used to decide whether a pixel
     * indicates a potential particle.
     * @return the signal-to-noise ratio
     */
    public final double getSignalToNoiseRatio() {
        return signalNoiseRatio;
    }

    /**
     * Retrieves the scale used to convert the image intensity to photon count.
     * @return the photon scale
     */
    public final double getPhotonScale() {
        return photonScale;
    }

    /**
     * Constructor.
     * @param stack the stack of images to be processed
     */
    public AbstractParticleLocator(final ImageStack stack) {
        
        // initialize the plug-in components
        this.stack = stack;
        results = new ResultsTable();
        localizations = 0;
        debugStack = new ImageStack(stack.getWidth(), stack.getHeight());
        noiseEstimator = new BackgroundNoise();

        // new reconstruction instance
        reconstruct = new Reconstruction();
        
        // create list of processes
        processes = new ArrayList<ImageProcess>(0);
        
        // create list of settings
        settings = new ArrayList<SettingsDialog>(0);
        settings.add(this);
        settings.add(getNoiseEstimator());

        debugging = new ArrayList<DebugStats>(0);
        debugging.add(getNoiseEstimator());
    }
    
    /**
     * Retrieves the current stack of images.
     * @return the image stack associated with this context
     */
    public final ImageStack getStack() {
        return stack;
    }

    @Override
    public final void displaySettings(final GenericDialog dialog, 
                                final Font header) {
        // display section title
        dialog.addMessage("Global", header);
        
        // request settings (with default values)
        dialog.addNumericField("Signal-to-Noise Ratio", 
                Prefs.get(SNR, DEFAULT_SNR), 2);
        dialog.addNumericField("Pixel Size", 
                Prefs.get(PIXEL_SIZE, DEFAULT_SIZE), 2, 6, "nm");
        dialog.addNumericField("Full Photon Scale", 
                Prefs.get(PHOTON_SCALE, DEFAULT_SCALE), 2, 6, "photons");
        dialog.addCheckbox("Generate Debugging Images", false);
    }

    @Override
    public final void saveSettings(final GenericDialog dialog) {
        
        // retrieve the values from the dialog
        signalNoiseRatio = dialog.getNextNumber();
        pixelSize = dialog.getNextNumber();
        photonScale = dialog.getNextNumber();
        debugImages = dialog.getNextBoolean();
        
        // log the values retrieved values
        IJ.log("Signal-to-Noise Ratio: " + signalNoiseRatio);
        IJ.log("Pixel Size: " + getPixelSize());
        IJ.log("Photons per Full Intensity Scale: " + photonScale);
        IJ.log("Debug Images: " + (debugImages ? "Enabled" : "Disabled"));
        
        // save to ImageJ for use during the next use of the plug-in
        Prefs.set(SNR, signalNoiseRatio);
        Prefs.set(PIXEL_SIZE, getPixelSize());
        Prefs.set(PHOTON_SCALE, photonScale);
    }
    
    /**
     * Starts the search and localization of particles in the stack of images.
     * @return the number of localizations
     */
    public final int start() {
        
        // get preferences; exit if cancelled
        if (!retrieveOptions()) {
            return 0;
        }
        
        // reset all debug counters
        for (DebugStats tracker : debugging) {
            tracker.resetCounters();
        }
        
        // start search
        searchStack();
        
        // display all debug counters
        for (DebugStats tracker : debugging) {
            tracker.logCounters();
        }
        
        synchronized (this) {
            return localizations;
        }
    }
    
    // search each slice of the image stack
    private void searchStack() {
        
        // initialize threads
        final int numCPU = Runtime.getRuntime().availableProcessors();
        Thread[] thread = new Thread[numCPU];
        
        // for all images in the stack
        final AtomicInteger iter = new AtomicInteger(0);

        // create and start processing threads
        for (int i = 0; i < thread.length; i++) {
            thread[i] = new Thread("Thread" + i) {
                @Override
                public void run() { 
                    searchStackWorker(iter);
                }
            };
        }
        
        for (int i = 0; i < thread.length; i++) {
            thread[i].start();
        }
        
        // join threads and wait to finish
        try {   
            for (int i = 0; i < thread.length; i++) {
                thread[i].join();
            }
        } catch (InterruptedException e) {
            IJ.handleException(e);
        }
        
        // display reconstruction
        final int[][] image =
                reconstruct.reconstruct(0, 0, 
                        stack.getWidth(), 
                        stack.getHeight(), 
                        stack.getWidth() * 32, 
                        stack.getHeight() * 32, 
                        65535,
                        1000.0 / pixelSize
                        );
        
        final ImagePlus impRecon = 
                IJ.createImage("Reconstruction", "16-bit black", 
                        stack.getWidth() * 32, 
                        stack.getHeight() * 32, 1);
        
        impRecon.getProcessor().setIntArray(image);
        impRecon.show();
        
        // display debug stack
        if (debugImages) {
            new ImagePlus("Debugging Stack", debugStack).show();
        }
        
        // reset progress bar
        IJ.showProgress(2);
        
        // display results
        results.show("Localization Results");
    }
    
    // search stack worker (per thread)
    private void searchStackWorker(final AtomicInteger iter) {
        
        final int stackSize = stack.getSize();
        final int indicator = stackSize / SOME_PRIME;
        
        // search every image slice
        try {
            for (int n = iter.getAndIncrement(); n < stackSize; 
                     n = iter.getAndIncrement()) {

                searchImage(stack.getProcessor(n + 1), n + 1);
                
                // space out progress indications to prevent slow down
                if (indicator != 0 && n % indicator == 0) {
                    IJ.showStatus("Analyzing: " + n + "/" 
                            + stackSize);
                    IJ.showProgress(n, stackSize);
                    if (IJ.escapePressed()) {
                        break;
                    }
                }
            
            }
        } catch (Exception e) {
            IJ.handleException(e);
        }
        
    }

    // search within the image
    private void searchImage(final ImageProcessor image,
                             final int slice) {
        
        // setup debug highlighter
        DebugImage debugImage = new DebugImage(null);
        if (debugImages) {
            debugImage = new DebugImage(image);
        }
        
        // create new image context
        final ImageContext context = new ImageContext(image, this);
        
        // locate all potential particles and clear "processed"-mask
        final FindParticles findParticles = new FindParticles(context);
        
        context.resetProcessed();
        
        // update background noise highlights
        final Window bgWindow = noiseEstimator.getWindow();
        
        for (int x = bgWindow.left; x <= bgWindow.right; x++) {
           for (int y = bgWindow.top; y <= bgWindow.bottom; y++) {
               debugImage.greenHighlight(x, y);
           }
        }
        
        while (findParticles.hasNext()) {
            final Point location = findParticles.getNext();
            
            // have the locator skip this pixel in the future
            context.setProcessed(location.x, location.y);
            debugImage.blueHighlight(location.x, location.y);
            
            // run all image processes
            boolean passed = true;
            for (int i = 0; i < processes.size() && passed; i++) {
                passed = processes.get(i).runProcess(context, location);
            }
            
            // check if passed
            if (passed) {
                
                // set the processed region as processed
                final Window window = context.getWindow();
                
                for (int x = window.left; x <= window.right; x++) {
                    for (int y = window.top; y <= window.bottom; y++) {
                        context.setProcessed(x, y);
                        debugImage.redHighlight(x, y);
                    }
                }

                // debugging info
                synchronized (this) {
                    localizations++;
                }
                
                // get centroid and save in the results table
                final Coordinates centroid = context.getCentroid();
                
                synchronized (results) {
                    results.incrementCounter();
                    results.addValue("Frame Number", slice);
                    results.addValue("X (px)", centroid.getX());
                    results.addValue("Y (px)", centroid.getY());
                    results.addValue("X (nm)", 
                            centroid.getX() * getPixelSize());
                    results.addValue("Y (nm)", 
                            centroid.getY() * getPixelSize());
                }
                
                // add to reconstructed image
                reconstruct.add(centroid.getX(), centroid.getY());
            }
        }
        
        // add debug image to stack
        if (debugImages) {
            synchronized (debugStack) {
                debugStack.addSlice("Slice", debugImage.getImage());
            }
        }
    }
    
    // display a dialog to the user requesting options
    private boolean retrieveOptions() {
        final Font header = new Font(Font.SANS_SERIF, Font.BOLD, 14);
        final GenericDialog dialog = new GenericDialog("Localization Options");
        dialog.centerDialog(true);
         
        // add image process options and show dialog (exit if canceled)
        for (int i = 0; i < settings.size(); i++) {
            settings.get(i).displaySettings(dialog, header);
        }
        
        dialog.showDialog();
        
        if (dialog.wasCanceled()) {
            return false;
        }
        
        // save image process options
        for (int i = 0; i < settings.size(); i++) {
            settings.get(i).saveSettings(dialog);
        }
        
        return true;
    }

    /**
     * Retrieves the current instance of the background noise estimator.
     * @return the mBgNoiseFinder
     */
    protected final BackgroundNoise getNoiseEstimator() {
        return noiseEstimator;
    }

    /**
     * Retrieves the size (width or height) of a pixel in nanometers.
     * @return the pixel size
     */
    public final double getPixelSize() {
        return pixelSize;
    }
}
