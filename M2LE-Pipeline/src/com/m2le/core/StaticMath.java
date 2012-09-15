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

import ij.process.ByteProcessor;
import ij.process.ImageProcessor;

//TODO: Remove the dependence on global noise estimates.
public final class StaticMath {
    
    private StaticMath() { }
    
    public static double calculateThreshold(double photons, double acc) {
        
        final double x0h = acc - 89.952;
        final double x0 = 61172.0/(x0h*x0h + 1307.9) - 97.515;
        final double y0 = 2.1759e-6*Math.pow(acc, 2.2837) + 0.082876;
        final double Ah = acc - 120.7;
        final double A = 992.92/(Ah*Ah - 35.069) + 2.9048;

        return A/Math.sqrt(photons - x0) + y0;
    }
    
    /*public static double calculateThreshold(double photons, double acc) {
        final double acc2 = acc*acc;
        final double acc3 = acc2*acc;
        final double acc4 = acc3*acc;
        final double acc5 = acc4*acc;
        
        final double max = 0.10042 + 1.339e-6*Math.pow(acc, 2.4224);
        final double base = 0.2585 + 2.8643e-8*Math.pow(acc, 3.4203);
        final double rate = 4.1116 - 0.24717*acc + 0.0085424*acc2 - 0.00014249*acc3 + 1.1653e-6*acc4 - 3.7425e-9*acc5;
        final double xhalf = -4190.8 + 427.36*acc - 13.766*acc2 + 0.21598*acc3 - 0.0016615*acc4 + 5.0288e-6*acc5; 
        
        return base + (max - base)/(1+Math.pow(xhalf/photons, rate));
    }*/

    public static double estimatePhotonCount(
            final ImageProcessor ip,
            final int left, final int right, 
            final int top, final int bottom,
            final double noise) {
        
        double sum = 0.0;
        
        for (int x = left; x < right; x++) {
            for (int y = top; y < bottom; y++) {
                sum += Math.max(ip.get(x, y) - noise, 0.0);
            }
        }
        
        return sum;
    }
    
    /**
     * @param array
     * @return the minimum value in the array
     */
    public static double min(final SignalArray array) {
        double min = array.get(0);
        
        for (int i = 1; i < array.size(); i++) {
            if (array.get(i) < min) {
                min = array.get(i);
            }
        }
        
        return min;
    }
    
    /**
     * @param array
     * @return the maximum value in the array
     */
    public static double max(final SignalArray array) {
        double max = array.get(0);
        
        for (int i = 1; i < array.size(); i++) {
            if (array.get(i) > max) {
                max = array.get(i);
            }
        }
        
        return max;
    }
    
    // fractional error less than x.xx * 10 ^ -4.
    // Algorithm 26.2.17 in Abromowitz and Stegun, Handbook of Mathematical.
    // http://introcs.cs.princeton.edu/java/21function/ErrorFunction.java.html
    public static double erf(final double z) {
        final double t = 1.0 / (1.0 + 0.47047 * ((z<0)?-z:z));
        final double poly = t * (0.3480242 + t * (-0.0958798 + t * (0.7478556)));
        final double ans = 1.0 - poly * Math.exp(-z*z);
        
        if (z >= 0)
            return  ans;
        else
            return -ans;
    }

    /**
     * fast error function approximation
     * @param x
     * @return the value of the error function
     */
    public static double erf2(final double x) {
        double v;
        
        if (x < 0)
            v = -x;
        else
            v = x;
        
        final double p = 0.3275911;
        final double t = 1.0/(1.0 + p*v);
        final double t2 = t*t;
        final double t3 = t2*t;
        final double t4 = t3*t;
        final double t5 = t4*t;
        final double a1 =  0.254829592 * t;
        final double a2 = -0.284496736 * t2;
        final double a3 =  1.421413741 * t3;
        final double a4 = -1.453152027 * t4;
        final double a5 =  1.061405429 * t5;
        final double result = 1.0 - (a1 + a2 + a3 + a4 + a5) * Math.exp(-v*v);
        
        if (x < 0)
            return -result;
        
        return result;
    }
    
    public static double estimateCenter(final SignalArray signal, final double noise) {
        double center = 0;
        double sum = 0;
        
        for (int i = 0; i < signal.size(); i++) {
            center += Math.max(signal.get(i) - noise, 0)*signal.getPosition(i);
            sum += Math.max(signal.get(i) - noise, 0);
        }
        
        return center / sum;
    }

    public static double[] estimateCentroid(
            final ImageProcessor ip,
            final int left, final int right, 
            final int top, final int bottom,
            final double noise) {
        
        final double[] centroid = {0,0};
        double sum = 0;
        
        // find second moments
        for (int x = left; x < right; x++) {
            for (int y = top; y < bottom; y++) {
                
                final double S = Math.max(ip.get(x, y) - noise, 0.0);
                
                centroid[0] += S*(x+0.5);
                centroid[1] += S*(y+0.5);
                
                sum += S;
            }
        }
        
        centroid[0] /= sum;
        centroid[1] /= sum;
        
        return centroid;
    }

    public static double[] estimateSecondMoments(
            final ImageProcessor ip, 
            final double[] centroid, 
            final int left, final int right, 
            final int top, final int bottom,
            final double noise) {
        
        final double[] moment = {0.0, 0.0, 0.0};
        double sum = 0;
        
        // find second moments
        for (int x = left; x < right; x++) {
            for (int y = top; y < bottom; y++) {
                
                final double S = Math.max(ip.get(x, y) - noise, 0.0);
                
                moment[0] += S*(x+0.5)*(x+0.5);
                moment[1] += S*(y+0.5)*(y+0.5);
                moment[2] += S*(x+0.5)*(y+0.5);
                
                sum += S;
            }
        }
        
        moment[0] /= sum;
        moment[1] /= sum;
        moment[2] /= sum;
        
        moment[0] -= centroid[0]*centroid[0];
        moment[1] -= centroid[1]*centroid[1];
        moment[2] -= centroid[0]*centroid[1];
        
        return moment;
    }
    
    public static double[] estimateThirdMoments(
            final ImageProcessor ip, 
            final double[] centroid, 
            final int left, final int right, 
            final int top, final int bottom,
            final double noise,
            final double wavelength,
            final double width) {
        
        final double[] moment = {0.0, 0.0, 0.0, 0.0};
        double sum = 0;
        
        final double alpha = 8.8857658763167324940317619801214/(wavelength*width);
      
        // find third moments
        for (int x = left; x < right; x++) {
            for (int y = top; y < bottom; y++) {
                
             final double S = Math.max(ip.get(x, y) - noise, 0.0);
                
             for (int i = 0; i < 10; i++) {
              for (int j = 0; j < 10; j++) {
                
                final double x0 = (x+(i/10.+0.05)-centroid[0])*alpha;
                final double y0 = (y+(j/10.+0.05)-centroid[1])*alpha;
                
                final double mask = Math.exp(-(x0*x0 + y0*y0));
                
                moment[0] += S*(   x0*(8.*x0*x0 - 12.) + 2.*x0*(4.*y0*y0 -  2.))*mask;
                moment[1] += S*(   y0*(8.*y0*y0 - 12.) + 2.*y0*(4.*x0*x0 -  2.))*mask;
                moment[2] += S*(6.*x0*(4.*y0*y0 -  2.)  -   x0*(8.*x0*x0 - 12.))*mask;
                moment[3] += S*(   y0*(8.*y0*y0 - 12.) - 6.*y0*(4.*x0*x0 -  2.))*mask;
                
                sum += S;
               
              }
             }
             
            }
        }
        moment[0] /= sum;
        moment[1] /= sum;
        moment[2] /= sum;
        moment[3] /= sum;
        
        return moment;
    }

    public static double[] findEigenValues(final double[] moment) {
        
        final double[] eigenValues = {0.0, 0.0};
        final double first = moment[0] + moment[1];
        final double diff = moment[0] - moment[1];
        final double last = Math.sqrt(4.0 * moment[2] * moment[2] + diff * diff);
        
        eigenValues[0] = (first + last) / 2.0;
        eigenValues[1] = (first - last) / 2.0;
        
        return eigenValues;
    } 
    
    public static double estimateNoise(final StackContext stack, final Estimate pixel) {
        
        final JobContext job = stack.getJobContext();
        final ImageProcessor ip = stack.getImageProcessor(pixel.getSlice());
        
        // get the pixel scaling
        int saturation = 65535;
        if (ip instanceof ByteProcessor)
            saturation = 255;
        final double scale = saturation / job.getNumericValue(UserParams.SATURATION);
        
        double noiseEstimate = -1.0;
        
        final int left = Math.max(0, pixel.getX() - 3);
        final int right = Math.min(ip.getWidth(), pixel.getX() + 4);
        final int top = Math.max(0, pixel.getY() - 3);
        final int bottom = Math.min(ip.getHeight(), pixel.getY() + 4);
        
        for (int x = left; x < right; x++) {
            double tempnoise = 0.0;
            for (int y = top; y < bottom; y++) {
                tempnoise += ip.get(x, y) / scale;
            }
            tempnoise /= (bottom-top);
            if (tempnoise < noiseEstimate || noiseEstimate < 0.0) {
                noiseEstimate = tempnoise;
            }
        }
        
        for (int y = top; y < bottom; y++) {
            double tempnoise = 0.0;
            for (int x = left; x < right; x++) {
                tempnoise += ip.get(x, y) / scale;
            }
            tempnoise /= (right-left);
            if (tempnoise < noiseEstimate || noiseEstimate < 0.0) {
                noiseEstimate = tempnoise;
            }
        }
        
        return noiseEstimate;
    }
}
