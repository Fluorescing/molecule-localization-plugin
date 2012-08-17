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

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;

import ij.IJ;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;

public final class MoleculeLocator {
    
    private MoleculeLocator() { }
    
    public static BlockingQueue<Estimate> findSubset(
            final StackContext stack, 
            final BlockingQueue<Estimate> pixels) {
        
        final PriorityBlockingQueue<Estimate> estimates = 
                new PriorityBlockingQueue<Estimate>(pixels.size());
        
        // check all potential pixels
        while (!pixels.isEmpty()) {
            try {
                
                // get pixel
                final Estimate estimate = pixels.take();
                
                // process the pixel
                findMaxLikelihood(stack, estimate);
                
                // put it back if it survived
                if (estimate.passed())
                    estimates.put(estimate);
                
            } catch (InterruptedException e) {
                IJ.handleException(e);
            }
        }
        
        return estimates;
    }
    
    private static boolean doIteration(
            final StackContext stack,
            final Estimate estimate,
            final SignalArray signal,
            final Parameters parameters,
            Parameters delta,
            final double[] likelihood,
            final double length, 
            final double wavenumber, 
            final double pixelsize,
            final double usablepixel,
            final double initialnoise) {
        
        final JobContext job = stack.getJobContext();
        
        final double posthreshold = job.getNumericValue(UserParams.ML_POS_EPSILON);
        final double intthreshold = job.getNumericValue(UserParams.ML_INT_EPSILON)/100.0;
        final double widthreshold = job.getNumericValue(UserParams.ML_WID_EPSILON);
        
        final double minNoiseBound = job.getNumericValue(UserParams.ML_MIN_NOISE);
        final double maxNoiseMulti = job.getNumericValue(UserParams.ML_MAX_NOISE);
        
        delta = GaussianModel.computeNewtonRaphson(signal, parameters,
                                                   delta,
                                                   length, wavenumber, 
                                                   pixelsize, usablepixel);
    
        double coefficient = 1.0;
        
        final Parameters newparameters = new Parameters();
        
        for (int k = 0; k < 10; k++) {
            
            // update the new parameters
            newparameters.update(parameters, delta, coefficient);
            
            if (newparameters.background < minNoiseBound) {
                newparameters.background = minNoiseBound;
            } else if (newparameters.background > maxNoiseMulti*initialnoise) {
                newparameters.background = maxNoiseMulti*initialnoise;
            }
            
            final double newlikelihood = 
                    GaussianModel.computeLogLikelihood(signal, newparameters, 
                                                       length, wavenumber, 
                                                       pixelsize, usablepixel);
        
            if (newlikelihood > likelihood[0]) {
                likelihood[0] = newlikelihood;
                break;
            } else {
                coefficient /= 2.0;
            }
        }
        
        double intdiff = 2.*(parameters.intensity - newparameters.intensity)
                                / (parameters.intensity + newparameters.intensity);
        
        if (intdiff < 0) {
            intdiff = -intdiff;
        }
        
        if ((delta.position<0?-delta.position:delta.position) < posthreshold) {
            return true;
        } else if (intdiff < intthreshold) {
            return true;
        } else if ((delta.width<0?-delta.width:delta.width) < widthreshold) {
            return true;
        }
        
        // update the final parameters
        parameters.set(newparameters);
        
        return false;
    }
    
    public static Estimate findMaxLikelihood(
            final StackContext stack, 
            final Estimate estimate) {
        
        final JobContext job     = stack.getJobContext();
        final ImageProcessor ip  = stack.getImageProcessor(estimate.getSlice());
        
        // preferences and constants
        final double wavenumber = 2.0*Math.PI/job.getNumericValue(UserParams.WAVELENGTH);
        final double pixelsize = job.getNumericValue(UserParams.PIXEL_SIZE);
        final double usablepixel = job.getNumericValue(UserParams.USABLE_PIXEL)/100.0;
        
        // get the pixel scaling
        int saturation = 65535;
        if (ip instanceof ByteProcessor)
            saturation = 255;
        final double scale = saturation / job.getNumericValue(UserParams.SATURATION);
        
        final int maxIter = (int) job.getNumericValue(UserParams.ML_MAX_ITERATIONS);
        final double minWidth = job.getNumericValue(UserParams.ML_MIN_WIDTH);
        final double maxWidth = job.getNumericValue(UserParams.ML_MAX_WIDTH);
        
        // center/focus point
        final int cx = estimate.getX();
        final int cy = estimate.getY();
        
        // set window size
        final int left   = Math.max(0, cx - 3);
        final int right  = Math.min(ip.getWidth(), cx + 4);
        final int top    = Math.max(0, cy - 3);
        final int bottom = Math.min(ip.getHeight(), cy + 4);
        
        final int width = right - left;
        final int height = bottom - top;
        
        // check for sufficient size
        if (width < 4 || height < 4) {
            estimate.reject();
            return estimate;
        }
        
        // flatten region into two arrays
        final SignalArray xsignal = new SignalArray(width, pixelsize);
        final SignalArray ysignal = new SignalArray(height, pixelsize);
        
        // accumulate pixel signal
        for (int x = left; x < right; x++) {
            for (int y = top; y < bottom; y++) {
                final double S = ip.get(x, y) / scale;
                xsignal.accumulate(x-left, S);
                ysignal.accumulate(y-top, S);
            }
        }
                
        // get initial estimates
        final Parameters xparam = new Parameters(stack, estimate, xsignal, 
                                           height, wavenumber, 
                                           pixelsize, usablepixel);
        
        final Parameters yparam = new Parameters(stack, estimate, ysignal, 
                                           width, wavenumber, 
                                           pixelsize, usablepixel);
        
        final double initialnoise = (xparam.background + yparam.background)/2.;
        
        
        boolean xdone = false;
        boolean ydone = false;
        
        final double[] xlikelihood = {
                GaussianModel.computeLogLikelihood(xsignal, xparam, 
                                                   height, wavenumber, 
                                                   pixelsize, usablepixel)};
        
        final double[] ylikelihood = {
                GaussianModel.computeLogLikelihood(ysignal, yparam, 
                                                   width, wavenumber, 
                                                   pixelsize, usablepixel)};
        
        final Parameters delta = new Parameters();
        
        // update parameters
        for (int iter = 0; iter < maxIter; iter++) {
            if (!xdone) {
                xdone = doIteration(stack, estimate, xsignal, 
                                    xparam, delta, xlikelihood,
                                    height, wavenumber, 
                                    pixelsize, usablepixel, 
                                    initialnoise);
            }
            
            if (!ydone) {
                ydone = doIteration(stack, estimate, ysignal, 
                                    yparam, delta, ylikelihood,
                                    width, wavenumber, 
                                    pixelsize, usablepixel, 
                                    initialnoise);
            }
            
            if (xdone && ydone) {
                break;
            }
        }
        
        // check for invalid parameters (to reject)
        if (!xparam.isValid() || !yparam.isValid()) {
            estimate.reject();
            return estimate;
        }
        
        if (xparam.position < 0. || xparam.position > pixelsize*width ||
                yparam.position < 0. || yparam.position > pixelsize*height) {
            estimate.reject();
            return estimate;
        }
        
        if (xparam.width < minWidth || xparam.width > maxWidth
                || yparam.width < minWidth || yparam.width > maxWidth) {
            estimate.reject();
            return estimate;
        }
        
        // record information
        estimate.setXEstimate(xparam.position/pixelsize + left); 
        estimate.setYEstimate(yparam.position/pixelsize + top);
        estimate.setIntensityEstimateX(xparam.intensity);
        estimate.setIntensityEstimateY(yparam.intensity);
        estimate.setBackgroundEstimateX(xparam.background);
        estimate.setBackgroundEstimateY(yparam.background);
        estimate.setWidthEstimateX(xparam.width);
        estimate.setWidthEstimateY(yparam.width);
        
        return estimate;
    }
}
