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

import ij.IJ;
import ij.Prefs;
import ij.gui.GenericDialog;
import ij.process.ImageProcessor;
import static java.lang.Math.sqrt;
import static java.lang.Math.abs;

/**
 * Estimates the background noise by looking at an empty patch.  This class is 
 * using a Singleton design pattern.
 */
public class BackgroundNoise implements SettingsDialog, DebugStats {
    
    /** The global keyword for the radius to use. */
    public static final String RADIUS = 
        "Localize_Particles.BackgroundNoise.radius";
    
    /** The global keyword for the percent difference threshold. */
    public static final String DIFF_THRESHOLD = 
        "Localize_Particles.BackgroundNoise.diffthresh";
    
    /** The global keyword for the maximum number of iterations allowed. */
    public static final String MAX_ITERATION = 
        "Localize_Particles.BackgroundNoise.maxiter";
    
    /** The global keyword for the expected number of photons per pixel. */
    public static final String EXPECTED_NOISE = 
        "Localize_Particles.BackgroundNoise.expected";
    
    // debug counters
    private int totalAttempts;
    private int totalIterations;
    private double sumOfDifferences;
    private double sumOfStdDev;
    private double sumOfAverage;

    private int radius;

    private int maxIterations;

    private double threshold;

    // the window used to collect the background noise
    private Window bestWindow;
    
    @Override
    public final void resetCounters() {
        synchronized (this) {
            totalAttempts = 0;
            totalIterations = 0;
            sumOfDifferences = 0;
            sumOfStdDev = 0;
            sumOfAverage = 0;
        }
    }
    
    @Override
    public final void logCounters() {
        synchronized (this) {
            IJ.log("Background Noise Stats: ");
            IJ.log("  Average Iterations: " 
                        + ((double) totalIterations / totalAttempts));
            IJ.log("  Average Mean: " + (sumOfAverage / totalIterations));
            IJ.log("  Average Standard Deviation: " 
                        + (sumOfStdDev / totalIterations));
            IJ.log("  Average Difference: " 
                        + (sumOfDifferences / totalIterations));
        }
    }
    
    /**
     * Attempts to make an estimate of the background noise by using the pixel
     * with the least intensity as a point of focus. The algorithm searches for
     * a patch that fits the expected distribution of Poisson noise.
     * @param context the current image context
     * @return the average noise
     */
    public final double calculateBackground(final ImageContext context) {
        synchronized (this) {
            totalAttempts++;
        }
        
        final double photonScale = context.getImage().getMax() / 
                context.getLocatorContext().getPhotonScale();
        
        final ImageProcessor image = context.getImage();
        
        double bestDifference = Double.MAX_VALUE;
        int bestTop = 0;
        int bestBottom = 0;
        int bestLeft = 0;
        int bestRight = 0;
        double bestAverage = 0;
            
        int iteration = 0;
        double difference;
            
        do {
            // increment iteration counter
            iteration++;
            
            // get a point to focus on
            final Point center = findMinimum(context);
            
            // get the window to calculate from
            final Window window = getWindow(image, center, radius);
                                    
            // find the average and standard deviation squared
            final double average = findAverage(window.top, 
                                               window.bottom, 
                                               window.left, 
                                               window.right, 
                                               image, 
                                               photonScale);
            
            final double stddevsqr = findStdDevSquared(
                                        average,
                                        window.top, 
                                        window.bottom, 
                                        window.left, 
                                        window.right, 
                                        image,
                                        photonScale);
            
            // find the percent difference and repeat as necessary
            final double sqrtAverage = sqrt(average);
            final double stddev = sqrt(stddevsqr);

            difference = 2.0 * (sqrtAverage - stddev) 
                             / (sqrtAverage + stddev);
            
            // check for bad numbers
            if (Double.isNaN(difference) 
                    || Double.isInfinite(difference)) {
                continue;
            }
            
            // debug counters
            synchronized (this) {
                totalIterations++;
                sumOfStdDev += stddev;
                sumOfAverage += average;
                sumOfDifferences += difference;
            }
            
            // update best values
            if (abs(difference) < bestDifference) {
                bestTop = window.top;
                bestBottom = window.bottom;
                bestLeft = window.left;
                bestRight = window.right;
                bestAverage = average;
                bestDifference = abs(difference);
            }
            
        } while (abs(difference) > threshold && iteration < maxIterations);
        
        
        // save the boundary for debugging purposes
        bestWindow = new Window(bestTop, bestBottom, bestLeft, bestRight);
        
        return bestAverage * photonScale;
    }
    
    // get a new window
    private static Window getWindow(final ImageProcessor image,
                                             final Point center, 
                                             final int radius) {
        int top = center.y - radius;
        int left = center.x - radius;
        int bottom = center.y + radius;
        int right = center.x + radius;
        
        if (top < 0) {
            top = 0;
        }
        if (bottom >= image.getHeight()) {
            bottom = image.getHeight() - 1;
        }
        if (left < 0) {
            left = 0;
        }
        if (right >= image.getWidth()) {
            right = image.getWidth() - 1;
        }
        
        return new Window(top, bottom, left, right);
    }
    
    // gets the average intensity of the specified region
    private static double findAverage(final int top, final int bottom, 
                                      final int left, final int right, 
                                      final ImageProcessor image,
                                      final double scale) {
        double sum = 0;
        int count = 0;
        for (int x = left; x <= right; x++) {
            for (int y = top; y <= bottom; y++) {
                sum += image.get(x, y) / scale;
                count++;
            }
        }
        
        return sum / count;
    }
    
    // gets the standard deviation
    private static double findStdDevSquared(final double average,
                                            final int top, final int bottom, 
                                            final int left, final int right, 
                                            final ImageProcessor image,
                                            final double scale) {
        double sum = 0;
        int count = 0;
        for (int x = left; x <= right; x++) {
            for (int y = top; y <= bottom; y++) {
                final int intensity = image.get(x, y);
                final double delta = intensity / scale - average;
                sum += delta * delta;
                count++;
            }
        }
        
        return sum / count;
    }
    
    // find the pixel position with the least intensity
    private static Point findMinimum(final ImageContext context) {
        final ImageProcessor image = context.getImage();
        final Point min = new Point(0, 0);
        int minIntensity = Integer.MAX_VALUE;
        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                final int intensity = image.get(x, y);
                if (intensity < minIntensity && !context.isProcessed(x, y)) {
                    minIntensity = intensity;
                    min.x = x;
                    min.y = y;
                }
            }
        }
        context.setProcessed(min.x, min.y);
        return min;
    }
    
    /**
     * Retrieves the window used to estimate the background noise.
     * @return a window
     */
    public Window getWindow() {
        return bestWindow;
    }
    
    @Override
    public final void displaySettings(final GenericDialog dialog, 
                                final Font header) {
        
        // add the dialog options
        dialog.addMessage("Noise Estimator", header);
        dialog.addNumericField("Backgound Radius", 
                Prefs.get(RADIUS, 7), 0, 6, "pixels");
        dialog.addNumericField("Difference Threshold", 
                Prefs.get(DIFF_THRESHOLD, 15.0), 1, 6, "%");
        dialog.addNumericField("Maximum NE Iterations", 
                Prefs.get(MAX_ITERATION, 10), 0);
    }
    
    @Override
    public final void saveSettings(final GenericDialog dialog) {
        
        radius = (int) dialog.getNextNumber();
        threshold = dialog.getNextNumber() / 100.0;
        maxIterations = (int) dialog.getNextNumber();
        
        // display variables in the log
        IJ.log("BackgroundNoise Settigns: ");
        IJ.log("  Background Radius: " + radius);
        IJ.log("  Background Threshold: " + threshold);
        IJ.log("  Background Iterations: " + maxIterations);
        
        // save to ImageJ
        Prefs.set(RADIUS, radius);
        Prefs.set(DIFF_THRESHOLD, threshold * 100.0);
        Prefs.set(MAX_ITERATION, maxIterations);
    }
}
