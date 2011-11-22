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

import org.apache.commons.math.MathException;

import static java.lang.Math.PI;
import static java.lang.Math.pow;
import static java.lang.Math.sqrt;
import static java.lang.Math.exp;
import static org.apache.commons.math.special.Erf.erf;

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
    private static final double EPSILON_DEF = 0.00001;
    private static final int DEFAULT_RADIUS = 3;
    
    // keys for storing ImageJ preferences
    private static final String INT_MAX_ITER = 
            "Localize_Particles.MaximumLikelyhoodEstimator.max_iter";
    
    private static final String WAVELENGTH = 
            "Localize_Particles.MaximumLikelyhoodEstimator.wavelength";
    
    private static final String USABLE_PIXEL = 
            "Localize_Particles.MaximumLikelyhoodEstimator.usable_pixel";
    
    private static final String EPSILON = 
            "Localize_Particles.MaximumLikelyhoodEstimator.epsilon";
    
    private static final String INIT_RADIUS = 
            "Localize_Particles.MaximumLikelyhoodEstimator.init_radius";
    
    // common variables; these should not differ with each image slice
    private double pixelSize;
    private int maxIterations;
    private double wavelength;
    private double usablePixelCoeff;
    private double epsilon;   
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
        final double bgNoise = context.getEstimatedNoise() / photonScale;
        
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
        
        double sum = 0.0;
        double[] xData = zeros(width);
        double[] yData = zeros(height);
        
        // accumulate pixel intensities down to single row and column
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                final double S = image.get(x + left, y + top) / photonScale;
                xData[x] += S;
                yData[y] += S;
                sum += S;
            }
        }
        
        // estimate the initial center of mass
        pixelSize = context.getLocatorContext().getPixelSize();
        final double cmX = 
                (findCenterOfMass(xData, bgNoise * height) - width / 2.0) 
                    * pixelSize;
        final double cmY = 
                (findCenterOfMass(yData, bgNoise * width) - height / 2.0) 
                    * pixelSize;
        
        // run MLE for x position
        final double photonCoeffX = (max(xData) - min(xData))
                / max(findExpectedCount(cmX, width));
        
        //final double photonCoeffX = 
        //        (sum(xData) - bgNoise * width * height) 
        //        / sum (findExpectedCount(cmX, width));
        
        synchronized (this) {
            intensityCoeff += photonCoeffX;
        }
        
        final double xResult = 
                runMaximumLikelyhoodEstimator(
                        cmX, 
                        photonCoeffX, 
                        bgNoise, 
                        xData);

        // run MLE for y position
        final double photonCoeffY = (max(yData) - min(yData)) 
                / max(findExpectedCount(cmY, height));
        
        //final double photonCoeffY = 
        //        (sum(yData) - bgNoise * width * height) 
        //         / sum (findExpectedCount(cmY, height));
        
        final double yResult = 
                runMaximumLikelyhoodEstimator(
                        cmY, 
                        photonCoeffY, 
                        bgNoise, 
                        yData); 
        
        
        if (Double.isNaN(xResult) || Double.isNaN(yResult)) {
            return false;
        }
        
        //if (Math.hypot(xResult - cmX, yResult - cmY) > mPixelSize) {
        //    return false;
        //}
        
        final double xBorder = width * pixelSize / 2;
        final double yBorder = height * pixelSize / 2;
        
        if (xResult <= -xBorder || xResult >= xBorder
                || yResult <= -yBorder || yResult >= yBorder) {
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
                        xResult / pixelSize + left + width / 2.0,
                        yResult / pixelSize + top + height / 2.0));
        
        context.setPhotonCount(sum 
                - context.getEstimatedNoise() * width * height / photonScale);
        
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
            center += Math.max(data[i] - bgNoise, 0) * (i + 1);
            sum += Math.max(data[i] - bgNoise, 0);
        }
        
        return center / sum;
    }

    /**
     * Estimates the position of the particle along one axis based on the data
     * provided.
     * @param position the estimated position of the particle
     * @param photonCoefficient the photon coefficient found
     * @param background the number of photon counts in the background
     * @param data the summation of photon counts along rows or columns
     * @return the position of the particle along one axis
     */
    private double runMaximumLikelyhoodEstimator(
            final double position,
            final double photonCoefficient,
            final double background,
            final double[] data) {
        
        synchronized (this) {
            totalAttempts++;
        }
        
        double newPosition = position;

        // adjust the position using an iterative method; exit if very little 
        // change occurs.
        for (int y = 0; y < maxIterations; y++) {
            
            final double[] firstDeriv = 
                    findFirstDerivative(newPosition, data.length);
            
            final double[] secondDeriv = 
                    findSecondDerivative(newPosition, data.length);
            
            final double[] expectedCount = 
                    findExpectedCount(newPosition, data.length);
            
            double numer = 0;
            double denom = 0;
            
            // sum the calculations for all data points
            for (int n = 0; n < data.length; n++) {
                
                final double delta1 = lambdaMLENumerator(
                        data[n], 
                        expectedCount[n] * photonCoefficient 
                            + background * data.length, 
                        firstDeriv[n] * photonCoefficient);
                
                final double delta2 = lambdaMLEDenomenator(
                        data[n], 
                        expectedCount[n] * photonCoefficient 
                            + background * data.length, 
                        firstDeriv[n] * photonCoefficient, 
                        secondDeriv[n] * photonCoefficient);
                
                numer += delta1;
                
                denom += delta2;
            }
            
            synchronized (this) {
                iterations++;
            }
            
            final double delta = numer / denom;
            
            // adjust position
            newPosition -= delta;    
            
            // end early if finished early
            if (Math.abs(delta) < epsilon) {
                break;
            }
        }
        
        return newPosition;
    }
    
    // the purpose of this method is to simplify
    private static double lambdaMLENumerator(
            final double data,
            final double expectedCount, 
            final double firstDeriv) {
        
        return firstDeriv * (data / expectedCount - 1);
    }
    
    // the purpose of this method is to simplify 
    private static double lambdaMLEDenomenator(
            final double data, 
            final double expectedCount, 
            final double firstDeriv, 
            final double secondDeriv) {
        
        return secondDeriv * (data / expectedCount - 1) 
                - (firstDeriv * firstDeriv) * data 
                / (expectedCount * expectedCount);
    }
    
    /**
     * @param position the estimated position of the particle
     * @param pixelCount the number of data points
     * @return a list of the expected first derivatives
     */
    private double[] findFirstDerivative(
            final double position,
            final int pixelCount) {
        
        final int numPixels = pixelCount;
        final double usablePixel = usablePixelCoeff * pixelSize;
        final double sigma = ALPHA * pow((2 * PI) / wavelength, 2);
        final double[] firstDerivatives = new double[numPixels];
        
        for (int i = 1; i <= numPixels; i++) {
            final double p = (i - (numPixels + 1) / 2) * pixelSize - position;
            
            final double firstTerm = -sigma * pow(p - usablePixel / 2, 2);
            final double secondTerm = -sigma * pow(p + usablePixel / 2, 2);
            
            firstDerivatives[i - 1] = 
                    sqrt(PI / sigma) * (exp(firstTerm) - exp(secondTerm));
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
            final int pixelCount) {
    
        final int numPixels = pixelCount;
        final double usablePixel = usablePixelCoeff * pixelSize;
        final double sigma = ALPHA * pow((2 * PI) / wavelength, 2);
        final double[] secondDerivatives = new double[numPixels];
        
        for (int i = 1; i <= numPixels; i++) {
            final double p = (i - (numPixels + 1) / 2) * pixelSize - position;
            
            final double firstTerm = -sigma * pow(p - usablePixel / 2, 2);
            final double secondTerm = -sigma * pow(p + usablePixel / 2, 2);
            
            final double firstConstant = 2 * sigma * (p - usablePixel / 2);
            final double secondConstant = 2 * sigma * (p + usablePixel / 2);
            
            secondDerivatives[i - 1] = sqrt(PI / sigma) 
                    * (firstConstant * exp(firstTerm) 
                            - secondConstant * exp(secondTerm));
        }
        
        return secondDerivatives;
    }
    
    /**
     * @param position the estimated position of the particle
     * @param pixelCount the number of data points
     * @return a list of the expected second derivatives
     */
    private double[] findExpectedCount(
            final double position,
            final int pixelCount) {
        
        final double usablePixel = usablePixelCoeff * pixelSize;
        final double sigma = ALPHA * pow((2 * PI) / wavelength, 2);
        final double[] expectedCount = new double[pixelCount];
        
        for (int i = 1; i <= pixelCount; i++) {
            final double p = (i - (pixelCount + 1) / 2) * pixelSize - position;
            final double firstTerm = sqrt(sigma) * (p - usablePixel / 2);
            final double secondTerm = sqrt(sigma) * (p + usablePixel / 2);
            
            try {
                expectedCount[i - 1] = 
                        PI / 2 / sigma * (erf(secondTerm) - erf(firstTerm));
            } catch (MathException e) {
                IJ.error("MaximumLikelyhoodEstimator", "erf error!");
            }
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
        dialog.addNumericField("Minimum Change", 
                Prefs.get(EPSILON, EPSILON_DEF), 5, 6, "");
        dialog.addNumericField("Maximum Iterations", 
                Prefs.get(INT_MAX_ITER, MAX_ITER_DEF), 0);
        dialog.addNumericField("Initial Radius", 
                Prefs.get(INIT_RADIUS, DEFAULT_RADIUS), 0, 6, "pixels");
    }

    @Override
    public final void saveSettings(final GenericDialog dialog) {
        
        wavelength = dialog.getNextNumber();
        usablePixelCoeff = dialog.getNextNumber();
        epsilon = dialog.getNextNumber();
        maxIterations = (int) dialog.getNextNumber();
        initialRadius = (int) dialog.getNextNumber();
        
        IJ.log("MaximumLikelyhoodEstimator Settings: ");
        IJ.log("  Wavelength: " + wavelength);
        IJ.log("  Usable Pixel: " + usablePixelCoeff);
        IJ.log("  Minimum Change: " + epsilon);
        IJ.log("  Maximum Iterations: " + maxIterations);
        IJ.log("  Initial Radius: " + initialRadius);
        
        Prefs.set(WAVELENGTH, wavelength);
        Prefs.set(USABLE_PIXEL, usablePixelCoeff);
        Prefs.set(EPSILON, epsilon);
        Prefs.set(INT_MAX_ITER, maxIterations);
        Prefs.set(INIT_RADIUS, initialRadius);
    }
}
