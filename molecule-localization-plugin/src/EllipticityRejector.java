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

import static java.lang.Math.abs;
import static java.lang.Math.sqrt;

/**
 * This class rejects a location based on it's ellipticity.
 */
public class EllipticityRejector 
implements ImageProcess, SettingsDialog, DebugStats {
    
    private static final double ELLIPTICITY_DEF = 0.5;
    private static final int DEFAULT_RADIUS = 3;

    /** Global key for the ellipticity threshold constant. */
    public static final String ELLIPTICITY = 
            "Localize_Particles.EllipticityRejector.ellipticity";
    
    private static final String TEST_RADIUS = 
            "Localize_Particles.MaximumLikelyhoodEstimator.init_radius";
    
    // debug counters
    private int totalAttempts;
    private int totalPassed;
    private int totalFailed;
    private double sumOfDifference;
    
    private boolean enabled = true;
    private double ellipThresh;
    private int testingRadius;

    @Override
    public final boolean runProcess(final ImageContext context, 
                              final Point location) {
        
        // check if enabled
        if (!enabled) {
            return true;
        }
        
        final Window window = 
                new Window(
                        location.y - testingRadius,
                        location.y + testingRadius - 1,
                        location.x - testingRadius,
                        location.x + testingRadius - 1);
        
        window.left = (window.left >= 0) 
                        ? window.left : 0;
        window.right = (window.right < context.getWidth()) 
                        ? window.right : context.getWidth() - 1;
        window.top = (window.top >= 0) 
                        ? window.top : 0;
        window.bottom = (window.bottom < context.getHeight()) 
                        ? window.bottom : context.getHeight() - 1;
        
        final double[] centroid = findCentroid(context, window);
        final double[] moment = findSecondMoments(context, window, centroid);
        final double[] eigenValues = findEigenValues(moment);            
        
        if (eigenValues.length != 2) {
            synchronized (this) {
                totalFailed++;
            }
            return false;
        }
        
        final double difference = 
            abs(2.0 * (eigenValues[0] - eigenValues[1]) 
                    / (eigenValues[0] + eigenValues[1]));
        
        // update debugging counters
        synchronized (this) {
            if (difference < ellipThresh) {
                totalPassed++;
            } else {
                totalFailed++;
            }
            
            totalAttempts++;
            sumOfDifference += difference;
        }
        
        if (difference >= ellipThresh) {
            context.setProcessed(window);
        }

        return difference < ellipThresh;
    }

    private static double[] findEigenValues(final double[] moment) {
        
        final double[] eigenValues = {0.0, 0.0};
        final double first = moment[0] + moment[1];
        final double diff = moment[0] - moment[1];
        final double last = sqrt(4.0 * moment[2] * moment[2] + diff * diff);
        
        eigenValues[0] = (first + last) / 2.0;
        eigenValues[1] = (first - last) / 2.0;
        
        return eigenValues;
    }

    private static double[] findSecondMoments(final ImageContext context,
                                              final Window window, 
                                              final double[] centroid) {
        
        final ImageProcessor image = context.getImage();
        
        final double[] moment = {0.0, 0.0, 0.0};
        double sum = 0;
        
        // find second moments
        for (int x = window.left; x <= window.right; x++) {
            for (int y = window.top; y <= window.bottom; y++) {
                
                final double intensity = image.get(x, y) - context.getEstimatedNoise();
                
                moment[0] += intensity * x * x;
                moment[1] += intensity * y * y;
                moment[2] += intensity * x * y;
                
                sum += intensity;
            }
        }
        
        // divide by sum
        for (int i = 0; i < moment.length; i++) {
            moment[i] /= sum;
        }
        
        moment[0] -= centroid[0]*centroid[0];
        moment[1] -= centroid[1]*centroid[1];
        moment[2] -= centroid[0]*centroid[1];
        
        return moment;
    }

    private static double[] findCentroid(final ImageContext context,
                                         final Window window) {
        
        final ImageProcessor image = context.getImage();
        
        final double[] centroid = {0.0, 0.0};
        double sum = 0;
        
        // find second moments
        for (int x = window.left; x <= window.right; x++) {
            for (int y = window.top; y <= window.bottom; y++) {
                
                final double intensity = image.get(x, y) - context.getEstimatedNoise();
                
                centroid[0] += intensity * x;
                centroid[1] += intensity * y;
                
                sum += intensity;
            }
        }
        
        // divide by sum
        for (int i = 0; i < centroid.length; i++) {
            centroid[i] /= sum;
        }
        
        return centroid;
    }

    @Override
    public final void displaySettings(final GenericDialog dialog, 
                                   final Font header) {
        dialog.addMessage("Ellipticity Test", header);
        dialog.addNumericField("Ellipticty Threshold", 
                Prefs.get(ELLIPTICITY, ELLIPTICITY_DEF * 100), 1, 6, "%");
        dialog.addNumericField("Testing Radius", 
                Prefs.get(TEST_RADIUS, DEFAULT_RADIUS), 0, 6, "pixels");
        dialog.addCheckbox("Disable Ellipticity Rejector", false);
    }

    @Override
    public final void saveSettings(final GenericDialog dialog) {
        ellipThresh = dialog.getNextNumber()/100.0;
        enabled = !dialog.getNextBoolean();
        testingRadius = (int) dialog.getNextNumber();
        
        IJ.log("EllipticityRejector Settings: ");
        IJ.log("  Threshold: " + ellipThresh);
        IJ.log("  Enabled: " + (enabled?"True":"False"));
        IJ.log("  Testing Radius: " + testingRadius);
        
        Prefs.set(ELLIPTICITY, ellipThresh*100.0);
    }

    @Override
    public final void resetCounters() {
        synchronized (this) {
            totalAttempts = 0;
            totalPassed = 0;
            totalFailed = 0;
            sumOfDifference = 0;
        }
    }

    @Override
    public final void logCounters() {
        synchronized (this) {
            IJ.log("Ellipticity Stats: ");
            IJ.log("  Pass Rate: " + (100.0 * totalPassed / totalAttempts)
                    + "%");
            IJ.log("  Failure Rate: " + (100.0 * totalFailed / totalAttempts)
                    + "%");
            IJ.log("  Average Percent Difference: "
                    + (100.0 * sumOfDifference / totalAttempts) + "%");
        }
    }
}
