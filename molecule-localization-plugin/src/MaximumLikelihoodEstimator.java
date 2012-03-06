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
 * The MLE algorithm was conceived by Alex Small and developed by Rebecca Starr, 
 * with support from California State University Program for Education and 
 * Research in Biotechnology (CSUPERB).
 */

import ij.IJ;
import ij.Prefs;
import ij.gui.GenericDialog;
import ij.process.ImageProcessor;

import java.awt.Font;
import java.awt.Point;

import static java.lang.Math.PI;
import static java.lang.Math.pow;
import static java.lang.Math.sqrt;
import static java.lang.Math.exp;

/**
 * The maximum likelihood estimator.
 */
public class MaximumLikelihoodEstimator
implements ImageProcess, SettingsDialog, DebugStats {
    
    // default values
    private static final int MAX_ITER_DEF = 10;
    private static final double WAVELENGTH_DEF = 550.0;
    private static final double USABLE_PIXEL_DEF = 0.9;
    private static final double ALPHA = 0.287;
    private static final double POS_EPSILON_DEF = 0.01;
    private static final double INT_EPSILON_DEF = 0.1;
    private static final int DEFAULT_RADIUS = 3;
    
    // keys for storing ImageJ preferences
    private static final String INT_MAX_ITER = 
            "Localize_Particles.MaximumLikelyhoodEstimator.max_iter";
    
    private static final String WAVELENGTH = 
            "Localize_Particles.MaximumLikelyhoodEstimator.wavelength";
    
    private static final String USABLE_PIXEL = 
            "Localize_Particles.MaximumLikelyhoodEstimator.usable_pixel";
    
    private static final String POS_EPSILON = 
            "Localize_Particles.MaximumLikelyhoodEstimator.pos_epsilon";
    
    private static final String INT_EPSILON = 
            "Localize_Particles.MaximumLikelyhoodEstimator.int_epsilon";
    
    private static final String INIT_RADIUS = 
            "Localize_Particles.MaximumLikelyhoodEstimator.init_radius";
    
    // common variables; these should not differ with each image slice
    private double pixelSize;
    private int maxIterations;
    private double wavelength;
    private double usablePixelCoeff;
    private double posEpsilon;
    private double intEpsilon;
    private int initialRadius;
    
    // fields for logging purposes
    private int iterations;
    private int totalAttempts;
    private double intensityCoeff;

    @Override
    public final boolean runProcess(final ImageContext context, 
                                    final Point location) {
        
        final ImageProcessor image = context.getImage();
        
        final double photonScale = context.getImage().getMax() / 
                context.getLocatorContext().getPhotonScale();
        
        // get estimated background photon count
        //final double bgNoise = context.getEstimatedNoise() / photonScale;
        
        // get a square chunk of the image
        final int size = 2 * initialRadius + 1;
        final int left = location.x - initialRadius;
        final int top = location.y - initialRadius;
        
        // ensure that the window does not overlap the border of the image
        if (left < 0 
                || left + size >= image.getWidth() 
                || top < 0 
                || top + size >= image.getHeight()) {
                return false;
        }
        
        final int width = size;
        final int height = size;
        
        // check that the size is above a certain number of pixels
        if (width < 4 || height < 4) {
            return false;
        }
        
        double[] xData = zeros(width);
        double[] yData = zeros(height);
        
        // accumulate pixel intensities down to single row and column
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                final double S = image.get(x + left, y + top) / photonScale;
                xData[x] += S;
                yData[y] += S;
            }
        }
        
        // estimate the background noise window
        final double bgNoise = Math.min(min(xData)/width, min(yData)/height);
        
        // estimate the initial center of mass
        pixelSize = context.getLocatorContext().getPixelSize();
        final double cmX = 
                (findCenterOfMass(xData, bgNoise * height)) * pixelSize;
        final double cmY = 
                (findCenterOfMass(yData, bgNoise * width)) * pixelSize;
        
        // run MLE for x position
        final double photonCoeffX = (max(xData) - min(xData))
                / max(findExpectedCount(cmX, width, height));
        
        double[] params = {cmX, photonCoeffX, bgNoise};
        
        // get x estimates
        runMaximumLikelyhoodEstimator(params, xData, height);
        final double xResult = params[0];
        
        if (Double.isNaN(params[0]) || Double.isNaN(params[1]) 
                || Double.isNaN(params[2]) || params[1] < 0 || params[2] < 0) {
            return false;
        }

        synchronized (this) {
            intensityCoeff += params[1]/2.0;
        }
        
        final double photonCoeffY = (max(yData) - min(yData))
                / max(findExpectedCount(cmY, height, width));
        
        // save the photon coefficient for later use (estimating photon count)
        double photonCoeff = params[1];
        
        // update just the y-position
        params[0] = cmY;
        params[1] = photonCoeffY;
        params[2] = bgNoise;
        
        // get y estimates
        runMaximumLikelyhoodEstimator(params, yData, width); 
        final double yResult = params[0];
        
        if (Double.isNaN(params[0]) || Double.isNaN(params[1]) 
                || Double.isNaN(params[2]) || params[1] < 0 || params[2] < 0) {
            return false;
        }
        
        synchronized (this) {
            intensityCoeff += params[1]/2.0;
        }
        
        
        
        if (xResult < 0.0 || xResult > width * pixelSize
                || yResult < 0.0 || yResult > height * pixelSize) {
            return false;
        }
        
        // TODO: modify "processed" size; this is a temporary solution
        context.setWindow(
                new Window(
                        location.y - 3,
                        location.y + 3,
                        location.x - 3,
                        location.x + 3));
        
        // set the current centroid and photon count estimate
        context.setCentroid(
                new Coordinates(
                        xResult / pixelSize + left,
                        yResult / pixelSize + top));
        
        // take the average of the two photon coefficients
        photonCoeff = (photonCoeff + params[1]) / 2.0;
        
        // set a good estimate for the photon count
        double expSum = 0;
        final double[] expC = findExpectedCount(cmY, height, width);
        for (int i = 0; i < height; i++) {
            expSum += expC[i] * photonCoeff;
        }
        
        context.setPhotonCount(expSum);
        context.setBackgroundLevel(params[2]);
        
        
        return true;
    }
    
    private static double[] zeros(final int length) {
        final double[] temp = new double[length];
        
        for (int i = 0; i < length; i++) {
            temp[i] = 0;
        }
        
        return temp;
    }

    private static double findCenterOfMass(final double[] data, 
                                           final double bgNoise) {
        double center = 0;
        double sum = 0;
        
        for (int i = 0; i < data.length; i++) {
            center += Math.max(data[i] - bgNoise, 0) * (i + 0.5);
            sum += Math.max(data[i] - bgNoise, 0);
        }
        
        return center / sum;
    }

    /**
     * Estimates the position of the particle along one axis based on the data
     * provided.
     * @param params the estimated position of the particle, the photon 
     * coefficient found, and the number of photon counts in the background
     * @param data the summation of photon counts along rows or columns
     * @param length the number of elements summed up to obtain the data array
     */
    private void runMaximumLikelyhoodEstimator(final double[] params,
                                               final double[] data,
                                               final double length) {
        
        synchronized (this) {
            totalAttempts++;
        }
        
        // adjust the position using an iterative method; exit if very little 
        // change occurs.
        for (int y = 0; y < maxIterations; y++) {
            
            final double paramPos = params[0];
            final double paramPhoton = params[1];
            final double paramBg = params[2];
            
            final double[] firstDeriv = 
                    findFirstDerivative(paramPos, data.length, length);
            
            final double[] secondDeriv = 
                    findSecondDerivative(paramPos, data.length, length);
            
            final double[] incomplExpected = 
                    findExpectedCount(paramPos, data.length, length);
            
            double numerPos = 0;
            double denomPos = 0;
            double numerPhoton = 0;
            double denomPhoton = 0;
            double numerBg = 0;
            double denomBg = 0;
            
            // sum the calculations for all data points
            for (int n = 0; n < data.length; n++) {
                
                final double complExpected = 
                        paramPhoton * incomplExpected[n] + length * paramBg;
                
                final double shared1 = data[n] / complExpected - 1.0;
                final double shared2 = data[n] / (complExpected*complExpected);
                
                final double d1Pos = paramPhoton*firstDeriv[n];
                final double d2Pos = paramPhoton*secondDeriv[n];
                
                numerPos += shared1 * d1Pos;
                denomPos += shared1 * d2Pos - shared2 * (d1Pos*d1Pos);
                
                final double d1Photon = incomplExpected[n];
                
                numerPhoton += shared1 * d1Photon;
                denomPhoton += -shared2 * (d1Photon*d1Photon);
                
                numerBg += shared1 * length;
                denomBg += -shared2 * (length*length);
            }
            
            // adjust position
            final double posDelta = numerPos / denomPos;
            
            // update parameters
            params[0] -= posDelta;
            params[1] -= numerPhoton / denomPhoton;
            params[2] -= numerBg / denomBg;
            
            synchronized (this) {
                iterations++;
            }
            
            final double intPDiff = 2.0 * Math.abs(paramPhoton - params[1]) 
                                                / (paramPhoton + params[1]);
            
            // end early if finished early
            if (Math.abs(posDelta) < posEpsilon && intPDiff < intEpsilon) {
                break;
            }
        }
    }
    
    /**
     * @param position the estimated position of the particle
     * @param pixelCount the number of data points
     * @return a list of the expected first derivatives
     */
    private double[] findFirstDerivative(
            final double position,
            final int pixelCount,
            final double length) {
        
        final int numPixels = pixelCount;
        final double usablePixel = usablePixelCoeff * pixelSize;
        final double sigma = ALPHA * pow(2.0 * PI / wavelength, 2);
        final double[] firstDerivatives = new double[numPixels];
        
        for (int i = 0; i < numPixels; i++) {
            final double p = (i + 0.5) * pixelSize - position;
            
            final double firstTerm = -sigma * pow(p - usablePixel/2.0, 2);
            final double secondTerm = -sigma * pow(p + usablePixel/2.0, 2);
            
            firstDerivatives[i] = length * sqrt(PI / sigma) 
                                        * (exp(firstTerm) - exp(secondTerm));
        }
        
        return firstDerivatives;
    }
    
    /**
     * @param position the estimated position of the particle
     * @param pixelCount the number of data points
     * @return a list of the expected second derivatives
     */
    private double[] findSecondDerivative(
            final double position,
            final int pixelCount,
            final double length) {
    
        final int numPixels = pixelCount;
        final double usablePixel = usablePixelCoeff * pixelSize;
        final double sigma = ALPHA * pow(2.0 * PI / wavelength, 2);
        final double[] secondDerivatives = new double[numPixels];
        
        for (int i = 0; i < numPixels; i++) {
            final double p = (i + 0.5) * pixelSize - position;
            
            final double firstTerm = -sigma * pow(p - usablePixel/2.0, 2);
            final double secondTerm = -sigma * pow(p + usablePixel/2.0, 2);
            
            final double firstConstant = 2.0 * sigma * (p - usablePixel/2.0);
            final double secondConstant = 2.0 * sigma * (p + usablePixel/2.0);
            
            secondDerivatives[i] = length * sqrt(PI / sigma) 
                    * (firstConstant*exp(firstTerm) - secondConstant*exp(secondTerm));
        }
        
        return  secondDerivatives;
    }
    
    /**
     * @param position the estimated position of the particle
     * @param pixelCount the number of data points
     * @return a list of the expected second derivatives
     */
    private double[] findExpectedCount(
            final double position,
            final int pixelCount,
            final double length) {
        
        final double usablePixel = usablePixelCoeff * pixelSize;
        final double sigma = ALPHA * pow(2.0 * PI / wavelength, 2);
        final double[] expectedCount = new double[pixelCount];
        
        for (int i = 0; i < pixelCount; i++) {
            final double p = (i + 0.5) * pixelSize - position;
            final double firstTerm = sqrt(sigma) * (p - usablePixel/2.0);
            final double secondTerm = sqrt(sigma) * (p + usablePixel/2.0);
            
            expectedCount[i] = 
                    length * PI / 2 / sigma * (erf(secondTerm) - erf(firstTerm));
        }
        
        return expectedCount;
    }
    
    // returns the minimum value of the array
    private static double min(final double[] array) {
        double min = array[0];
        for (double value : array) {
            if (value < min) {
                min = value;
            }
        }
        return min;
    }
    
    // returns the maximum value of the array
    private static double max(final double[] array) {
        double max = array[0];
        for (double value : array) {
            if (value > max) {
                max = value;
            }
        }
        return max;
    }
    
    // fast error function approximation
    private static double erf(double x) {
        final double v = Math.abs(x);
        
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

    @Override
    public final void resetCounters() {
        synchronized (this) {
            iterations = 0;
            totalAttempts = 0;
            intensityCoeff = 0;
        }
    }

    @Override
    public final void logCounters() {
        synchronized (this) {
            IJ.log("MLE Stats: ");
            IJ.log("  Average Number of Iterations: "
                + ((double) iterations / totalAttempts));
            IJ.log("  Average Intensity Coefficient: "
                    + (intensityCoeff / totalAttempts));
        }
    }

    @Override
    public final void displaySettings(
            final GenericDialog dialog, 
            final Font header) {
        
        dialog.addMessage("Maximum Likelyhood Estimator", header);
        dialog.addNumericField("Light Wavelength", 
                Prefs.get(WAVELENGTH, WAVELENGTH_DEF), 1, 6, "nm");
        dialog.addNumericField("Usable Pixel", 
                Prefs.get(USABLE_PIXEL, USABLE_PIXEL_DEF), 1, 6, "coefficient");
        dialog.addNumericField("Position Threshold", 
                Prefs.get(POS_EPSILON, POS_EPSILON_DEF), 2, 6, "nm");
        dialog.addNumericField("Intensity Threshold", 
                Prefs.get(INT_EPSILON, INT_EPSILON_DEF), 1, 6, "%");
        dialog.addNumericField("Maximum Iterations", 
                Prefs.get(INT_MAX_ITER, MAX_ITER_DEF), 0);
        dialog.addNumericField("Initial Radius", 
                Prefs.get(INIT_RADIUS, DEFAULT_RADIUS), 0, 6, "pixels");
    }

    @Override
    public final void saveSettings(final GenericDialog dialog) {
        
        wavelength = dialog.getNextNumber();
        usablePixelCoeff = dialog.getNextNumber();
        posEpsilon = dialog.getNextNumber();
        intEpsilon = dialog.getNextNumber();
        maxIterations = (int) dialog.getNextNumber();
        initialRadius = (int) dialog.getNextNumber();
        
        IJ.log("MaximumLikelyhoodEstimator Settings: ");
        IJ.log("  Wavelength: " + wavelength);
        IJ.log("  Usable Pixel: " + usablePixelCoeff);
        IJ.log("  Position Threshold: " + posEpsilon);
        IJ.log("  Intensity Threshold: " + posEpsilon);
        IJ.log("  Maximum Iterations: " + maxIterations);
        IJ.log("  Initial Radius: " + initialRadius);
        
        Prefs.set(WAVELENGTH, wavelength);
        Prefs.set(USABLE_PIXEL, usablePixelCoeff);
        Prefs.set(POS_EPSILON, posEpsilon);
        Prefs.set(INT_EPSILON, intEpsilon);
        Prefs.set(INT_MAX_ITER, maxIterations);
        Prefs.set(INIT_RADIUS, initialRadius);
    }
}
