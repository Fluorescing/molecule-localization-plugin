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

public class SignalArray {
    
    public double[] mSignal;
    private final double[] mPosition;
    
    public SignalArray(final int size, final double interval) {
        mSignal = new double[size];
        mPosition = new double[size];
        
        double position = interval/2.;
        for (int i = 0; i < size; i++) {
            mPosition[i] = position;
            position += interval;
        }
    }
    
    public void accumulate(final int index, final double value) {
        mSignal[index] += value;
    }
    
    public void set(final int index, final double value) {
        mSignal[index] = value;
    }
    
    public double get(final int index) {
        return mSignal[index];
    }
    
    public double getPosition(final int index) {
        return mPosition[index];
    }
    
    public int size() {
        return mSignal.length;
    }
}
