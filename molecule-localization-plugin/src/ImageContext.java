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

import ij.process.ImageProcessor;

/**
 * Stores information relevant to the current image slice.
 */
public class ImageContext {
    
    private ImageProcessor image;      // current image
    private final int width;
    private final int height;
    private boolean[][] maskDone;
    private final double noise;        // estimated background noise
    private Coordinates centroid;
    private double photoncount;
    private Window window;
    
    private final AbstractParticleLocator mLocator;
    
    /**
     * Creates an ImageContext.
     * @param image the ImageProcessor to be analyzed.
     * @param pluginContext the particle locator context
     */
    public ImageContext(final ImageProcessor image, 
            final AbstractParticleLocator pluginContext) {
        
        // set image and properties
        this.image = image;
        width = image.getWidth();
        height = image.getHeight();
        
        // allocate processed-mask
        maskDone = new boolean[image.getWidth()][image.getHeight()];
        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                maskDone[x][y] = false;
            }
        }
        
        // save the locator context
        mLocator = pluginContext;
        
        // estimate and store the background noise (this should be last)
        noise = pluginContext.getNoiseEstimator().calculateBackground(this);
    }
    
    /**
     * Resets the processed-mask to false.
     */
    public void resetProcessed() {
        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                maskDone[x][y] = false;
            }
        }
    }
    
    /**
     * Retrieves the width of the image.
     * @return the width of the image.
     */
    public final int getWidth() {
        return width;
    }
    
    /**
     * Retrieves the height of the image.
     * @return the height of the image.
     */
    public final int getHeight() {
        return height;
    }
    
    /**
     * Retrieves the current image.
     * @return the attached ImageProcessor.
     */
    public final ImageProcessor getImage() {
        return image;
    }
    
    /**
     * Replaces the current image processor with a new image processor.
     * @param newImage the new image processor
     */
    public final void replaceImage(final ImageProcessor newImage) {
        this.image = newImage;
    }
    
    /**
     * Checks if the pixel has been processed.
     * @param x the x-coordinate
     * @param y the y-coordinate
     * @return true if processed; false otherwise.
     */
    public final boolean isProcessed(final int x, final int y) {
        return maskDone[x][y];
    }
    
    /**
     * Sets the pixel as processed.
     * @param x the x-coordinate
     * @param y the y-coordinate
     */
    public final void setProcessed(final int x, final int y) {
        maskDone[x][y] = true;
    }
    
    /**
     * Sets the window as processed.
     * @param window the region to mark as processed
     */
    public final void setProcessed(final Window window) {
        final int left = (window.left >= 0) 
                            ? window.left : 0;
        final int right = (window.right < getWidth()) 
                            ? window.right : getWidth();
        final int top = (window.top >= 0) 
                            ? window.top : 0;
        final int bottom = (window.bottom < getHeight()) 
                            ? window.bottom : getHeight();
        
        for (int x = left; x < right; x++) {
            for (int y = top; y < bottom; y++) {
                maskDone[x][y] = true;
            }
        }
    }

    /**
     * Retrieves the main locator context (the plug-in), which contains the
     * global variables.
     * @return the locator context
     */
    public final AbstractParticleLocator getLocatorContext() {
        return mLocator;
    }

    /**
     * Retrieves the estimated noise of the image.
     * @return the estimated noise of the image (not necessarily photon counts)
     */
    public final double getEstimatedNoise() {
        return noise;
    }
    
    
    /**
     * Sets the current centroid to the specified coordinates. Set to null
     * to clear.
     * @param coordinates the specified coordinates
     */
    public void setCentroid(final Coordinates coordinates) {
        centroid = coordinates;
    }
    
    /**
     * Sets the current photon count estimate to the specified value.
     * @param estimate the estimated photon count per molecule
     */
    public void setPhotonCount(final double estimate) {
        photoncount = estimate;
    }
    
    /**
     * Sets the current window to the specified window.  Set to null to clear.
     * @param window the specified window
     */
    public void setWindow(final Window window) {
        this.window = window;
    }
    
    /**
     * Returns the current centroid if it exists.
     * @return the current centroid; null otherwise
     */
    public Coordinates getCentroid() {
        return centroid;
    }
    
    /**
     * Get the current photon count estimate to the specified value.
     * @return the estimated photon count
     */
    public double getPhotonCount() {
        return photoncount;
    }
    
    /**
     * Returns the current window if it exists.
     * @return the current window; null otherwise
     */
    public Window getWindow() {
        return window;
    }
}
