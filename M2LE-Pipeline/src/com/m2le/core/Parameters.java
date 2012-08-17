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

/**
 * This is a vector-type for the parameter space used for the Gaussian model.
 * @see GaussianModel
 */
public class Parameters {
    
    public double position;
    public double intensity;
    public double background;
    public double width;
    
    /**
     * Constructs a parameter vector with values of zero.
     */
    public Parameters() { }
    
    /**
     * Constructs a parameter vector by estimating the values from a region
     * of interest.
     * @param region the region of interest context
     * @param signal the array of summed signals
     * @param length the number of pixels summed per signal array element
     * @param wavenumber the wavenumber of light
     * @param pixelsize the size of the pixel across (nanometers)
     * @param usablepixel the fraction of the pixel that is usable
     */
    public Parameters(
            final StackContext stack,
            final Estimate estimate,
            final SignalArray signal, 
            final int length, 
            final double wavenumber, 
            final double pixelsize,
            final double usablepixel) {
        
        // estimate background
        this.background = StaticMath.min(signal)/length;
        
        // estimate center
        this.position = StaticMath.estimateCenter(signal, background*length);
        
        // estimate width
        this.width = 0.5*(estimate.getMajorAxis() + estimate.getMinorAxis()); 
        this.width *= 1.4142135623730950488016887242097*pixelsize*wavenumber;
        
        // estimate intensity
        this.intensity = (StaticMath.max(signal) - StaticMath.min(signal))
                        / StaticMath.max(GaussianModel.getPartialExpectedArray(
                                                signal.size(), this, 
                                                length, wavenumber, 
                                                pixelsize, usablepixel));
    }
    
    /**
     * Constructs a parameter vector with the following values.
     * @param position the position estimate (nanometers)
     * @param intensity the intensity coefficient (arbitrary units)
     * @param background the background noise level (photons per pixel)
     * @param width the width of the molecule (arbitrary units)
     */
    public Parameters(
            final double position, 
            final double intensity, 
            final double background, 
            final double width) {
        this.position = position;
        this.intensity = intensity;
        this.background = background;
        this.width = width;
    }
    
    /**
     * Sets the parameters to (original - delta*coefficient)
     * @param original the previous parameter values
     * @param delta the change in the parameters
     * @param coefficient the fraction of the change to make
     */
    public void update(
            final Parameters original, 
            final Parameters delta, 
            final double coefficient) {
        this.position = original.position - delta.position*coefficient;
        this.intensity = original.intensity - delta.intensity*coefficient;
        this.background = original.background - delta.background*coefficient;
        this.width = original.width - delta.width*coefficient;
    }
    
    /**
     * Copies the parameter values from the parameter vector.
     * @param parameters the parameter vector
     */
    public void set(final Parameters parameters) {
        this.position = parameters.position;
        this.intensity = parameters.intensity;
        this.background = parameters.background;
        this.width = parameters.width;
    }
    
    public void set(final double position, 
                    final double intensity, 
                    final double background, 
                    final double width) {
        this.position = position;
        this.intensity = intensity;
        this.background = background;
        this.width = width;
    }
    
    /**
     * Checks if the parameters are valid.
     * @return true if value; false otherwise.
     */
    public boolean isValid() {
        if (Double.isInfinite(position)   || Double.isNaN(position)   ||
            Double.isInfinite(intensity)  || Double.isNaN(intensity)  ||
            Double.isInfinite(background) || Double.isNaN(background) ||
            Double.isInfinite(width)      || Double.isNaN(width)) {
          return false;
        }
        
        if (intensity < 0. || background < 0. || width < 0.) {
          return false;
        }
        
        return true;
    }
}
