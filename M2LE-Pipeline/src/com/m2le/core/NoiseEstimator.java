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
public final class NoiseEstimator {
    
    private NoiseEstimator() { }
    
    private static final int SIZE = 100;
    
    public static double estimateNoise(final StackContext stack, final ImageProcessor ip, final double scale) {
        final JobContext job = stack.getJobContext();
        
        final int histogram[] = new int[SIZE];
        
        int max = 0;
        int est = 0;
        
        // Generate histogram and locate mode
        for (int x = 0; x < ip.getWidth(); x++) {
            for (int y = 0; y < ip.getHeight(); y++) {
                final double S = ip.get(x,y) / scale;
                final int i = (int) S;
                
                if (i < SIZE) {
                    histogram[i]++;
                    if (histogram[i] > max) {
                        max = histogram[i];
                        est = i;
                    }
                }
            }
        }
        
        return Math.max(est, job.getNumericValue(UserParams.LOWEST_NOISE_EST));
    }
}
