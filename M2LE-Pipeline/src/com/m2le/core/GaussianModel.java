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

import static java.lang.Math.PI;
import static java.lang.Math.log;
import static java.lang.Math.sqrt;
import static java.lang.Math.exp;


public final class GaussianModel {
    
    private GaussianModel() { }
    
    private static final double SQRTPI = sqrt(PI);
    
    public static double getPartialExpected(
            final double position, 
            final Parameters parameters, 
            final double windowsize, 
            final double wavenumber, 
            final double pixelsize,
            final double usablepixel) {
        
        final double x = position;
        final double x0 = parameters.position;
        final double w = parameters.width;
        final double L = windowsize;
        final double k = wavenumber;
        final double a = pixelsize*usablepixel;
        
        return (L*PI*w*w*(-StaticMath.erf((k*(-a/2. + x - x0))/w) 
                         + StaticMath.erf((k*( a/2. + x - x0))/w)))/(2.*k*k);
    }
    
    public static SignalArray getPartialExpectedArray(
            final int size,
            final Parameters parameters, 
            final double windowsize, 
            final double wavenumber, 
            final double pixelsize,
            final double usablepixel) {
        
        final SignalArray expected = new SignalArray(size, pixelsize);
        
        for (int n = 0; n < size; n++) {
            expected.set(n, getPartialExpected(expected.getPosition(n), 
                                               parameters, windowsize, 
                                               wavenumber, pixelsize, 
                                               usablepixel));
        }
        
        return expected;
    }
    
    public static double getFullExpected(
            final double position, 
            final Parameters parameters, 
            final double windowsize, 
            final double wavenumber,
            final double pixelsize,
            final double usablepixel) {
                            
        final double x = position;
        final double x0 = parameters.position;
        final double I0 = parameters.intensity;
        final double bg = parameters.background;
        final double w = parameters.width;
        final double L = windowsize;
        final double k = wavenumber;
        final double a = pixelsize*usablepixel;
        
        return bg*L + (I0*L*PI*w*w*(-StaticMath.erf((k*(-a/2. + x - x0))/w) 
                                   + StaticMath.erf((k*( a/2. + x - x0))/w)))
                               /(2.*k*k);
    }
    
    public static double[] getFirstDerivatives(
            final double position, 
            final Parameters parameters, 
            final double windowsize, 
            final double wavenumber, 
            final double pixelsize,
            final double usablepixel) {
        
        final double x = position;
        final double x0 = parameters.position;
        final double I0 = parameters.intensity;
        final double w = parameters.width;
        final double L = windowsize;
        final double k = wavenumber;
        final double a = pixelsize*usablepixel;
        
        // optimizations
        final double k2 = k*k;
        final double w2 = w*w;
        final double y1 = a/2. + x - x0;
        final double y2 = -a/2. + x - x0;
        final double y1p2 = y1*y1;
        final double y2p2 = y2*y2;
        
        final double[] array = new double[4];
        
        array[0] = (I0*L*PI*((2*k)/(exp((k2*y2p2)/w2)*SQRTPI*w) - 
                        (2*k)/(exp((k2*y1p2)/w2)*SQRTPI*w))*w2)/(2.*k2);
        array[1] = (L*PI*w2*(-StaticMath.erf((k*(y2))/w) 
                       + StaticMath.erf((k*(y1))/w)))/(2.*k2);
        array[2] = L;
        array[3] = (I0*L*PI*w2*((2*k*(y2))/(exp((k2*y2p2)/w2)*SQRTPI*w2) - 
                           (2*k*(y1))/(exp((k2*y1p2)/w2)*SQRTPI*w2)))/(2.*k2) + 
              (I0*L*PI*w*(-StaticMath.erf((k*(y2))/w) 
                         + StaticMath.erf((k*(y1))/w)))/k2;
        
        return array;
    }
    
    public static double[] getSecondDerivatives(
            final double position, 
            final Parameters parameters, 
            final double windowsize, 
            final double wavenumber, 
            final double pixelsize,
            final double usablepixel) {
        
        final double x = position;
        final double x0 = parameters.position;
        final double I0 = parameters.intensity;
        final double w = parameters.width;
        final double L = windowsize;
        final double k = wavenumber;
        final double a = pixelsize*usablepixel;
        
        // optimizations
        final double k2 = k*k;
        final double k3 = k2*k;
        final double w2 = w*w;
        final double w3 = w2*w;
        final double w5 = w2*w3;
        final double y1 = a/2. + x - x0;
        final double y2 = -a/2. + x - x0;
        final double y1p2 = y1*y1;
        final double y1p3 = y1*y1p2;
        final double y2p2 = y2*y2;
        final double y2p3 = y2*y2p2;
        
        final double[] array = new double[4];
        
        // These are not meant to be human readable. They were
        //   automatically generated and then further optimized.
        array[0] = (I0*L*PI*w2*((4*k3*(y2))/(exp((k2*y2p2)/w2)*SQRTPI*w3) - 
                     (4*k3*(y1))/(exp((k2*y1p2)/w2)*SQRTPI*w3)))/(2.*k2);
        array[1] = 0;
        array[2] = 0;
        array[3] = (I0*L*(4*PI*w*((2*k*(y2))/(exp((k2*y2p2)/w2)*SQRTPI*w2) - 
                       (2*k*(y1))/(exp((k2*y1p2)/w2)*SQRTPI*w2)) + 
               PI*w2*((-4*k*(y2))/(exp((k2*y2p2)/w2)*SQRTPI*w3) + 
                      (4*k3*y2p3)/(exp((k2*y2p2)/w2)*SQRTPI*w5) + 
                       (4*k*(y1))/(exp((k2*y1p2)/w2)*SQRTPI*w3) - 
                      (4*k3*y1p3)/(exp((k2*y1p2)/w2)*SQRTPI*w5)) + 
               2*PI*(-StaticMath.erf((k*(y2))/w) 
                    + StaticMath.erf((k*(y1))/w))))/(2.*k2);
    
        return array;
    }
    
    public static double computeLogLikelihood(
            final SignalArray signal,
            final Parameters parameters,
            final double windowsize,
            final double wavenumber,
            final double pixelsize,
            final double usablepixel) {
        
        double logLikelihood = 0;
        
        for (int n = 0; n < signal.size(); n++) {
            
            final double expected = 
                    getFullExpected(signal.getPosition(n), 
                                    parameters, 
                                    windowsize, wavenumber, 
                                    pixelsize, usablepixel);
            
            logLikelihood += signal.mSignal[n]*log(expected) - expected;
        }
        
        return logLikelihood;
    }

    public static Parameters computeNewtonRaphson(
            final SignalArray signal,
            final Parameters parameters,
            final Parameters delta,
            final double windowsize,
            final double wavenumber,
            final double pixelsize,
            final double usablepixel) {
        
        // allocate first and second derivates of loglikelihood
        final double[] firstL  = new double[4];
        final double[] secondL = new double[4];
        
        // compute the log-likelihood derivatives
        for (int n = 0; n < signal.size(); n++) {
            final double expected = 
                    getFullExpected(signal.getPosition(n), 
                                    parameters, 
                                    windowsize, wavenumber, 
                                    pixelsize, usablepixel);
            
            final double[] first  = 
                    getFirstDerivatives(signal.getPosition(n), 
                                        parameters, 
                                        windowsize, wavenumber, 
                                        pixelsize, usablepixel);
            
            final double[] second = 
                    getSecondDerivatives(signal.getPosition(n), 
                                         parameters, 
                                         windowsize, wavenumber, 
                                         pixelsize, usablepixel);
        
            for (int i = 0; i < 4; i++) {
                firstL[i]  += (signal.mSignal[n]/expected - 1.)*first[i];
                secondL[i] += (signal.mSignal[n]/expected - 1.)*second[i] 
                       - (signal.mSignal[n]*first[i]*first[i]/(expected*expected));
            }
        }
        
        // compute the recommended change in parameter values
        delta.set(firstL[0]/secondL[0],
                  firstL[1]/secondL[1],
                  firstL[2]/secondL[2],
                  firstL[3]/secondL[3]);
        
        return delta;
    }
}
