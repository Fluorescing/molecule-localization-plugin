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

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageConverter;
import ij.process.ImageProcessor;

/**
 * @author Shane Stahlheber
 */
public class StackContext {
    
    private final ImageStack mStack;
    private final JobContext mJob;
    private boolean mFailed;
    
    public StackContext(final JobContext job) {
        ImagePlus imp = IJ.getImage();
        
        if (imp == null) {
            mStack = null;
            mJob = null;
            mFailed = true;
            return;
        } else {
            mFailed = false;
        }
        
        switch (imp.getType()) {
          case ImagePlus.GRAY8:
          case ImagePlus.GRAY16:
          case ImagePlus.GRAY32:
            // All is OK
            break;
          
          default:
            IJ.log("M2LE Warning: Image (RGB) being converted to grayscale.");
            
            // make a duplicate of the image stack and convert to grayscale
            imp = imp.duplicate();
            final ImageConverter imgConverter = new ImageConverter(imp);
            imgConverter.convertToGray16();
        }
        
        mStack = imp.getImageStack();
        mJob = job;
    }
    
    public boolean loadFailed() {
        return mFailed;
    }
    
    public int getWidth() {
        return mStack.getWidth();
    }
    
    public int getHeight() {
        return mStack.getHeight();
    }
    
    public int getSize() {
        return mStack.getSize();
    }
    
    public ImageProcessor getImageProcessor(final int index) {
        return mStack.getProcessor(index);
    }
    
    public JobContext getJobContext() {
        return mJob;
    }
}
