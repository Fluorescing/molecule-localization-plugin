/*
 * Copyright (C) 2012 Shane Stahlheber
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.m2le.core;
import java.util.Arrays;

import ij.process.ImageProcessor;

/**
 * This is a utility class that provides a global noise estimate for a
 * given image context.
 * <p>
 * Noise estimates are for individual images and are used to find
 * potential molecules.
 * 
 * @see ImageContext
 *
 * @version $Id$
 */
public class NoiseEstimator {
    
    private static final int WINDOW = 16;
    
    private double[][] noisegrid;
    
    public NoiseEstimator(final StackContext stack, final ImageProcessor ip, final double scale) {
        final JobContext job = stack.getJobContext();
        
        final double sorted[] = new double[WINDOW*WINDOW];
        
        final int TILES_X = (int) Math.ceil((double) ip.getWidth() / WINDOW); 
        final int TILES_Y = (int) Math.ceil((double) ip.getHeight() / WINDOW); 
        
        noisegrid = new double[TILES_X][TILES_Y];
        
        // Locate medians
        for (int cx = 0; cx < TILES_X; cx++) {
            for (int cy = 0; cy < TILES_Y; cy++) {
                
                // store unsorted pixel values
                for (int x = 0; (cx*WINDOW+x) < ip.getWidth() && x < WINDOW; x++) {
                    for (int y = 0; (cy*WINDOW+y) < ip.getHeight() && y < WINDOW; y++) {
                        final double S = ip.get(cx*WINDOW+x,cy*WINDOW+y) / scale;
                        
                        sorted[x + y*WINDOW] = S;
                    }
                }
                
                // sort pixel values
                Arrays.sort(sorted);
                
                // store the median
                noisegrid[cx][cy] = Math.max(sorted[WINDOW*WINDOW/2], job.getNumericValue(UserParams.LOWEST_NOISE_EST));
            }
        }
    }
    
    public double getNoiseEstimate(final int x, final int y) {
        final int cx = x / WINDOW;
        final int cy = y / WINDOW;
        
        return noisegrid[cx][cy];
    }
}
