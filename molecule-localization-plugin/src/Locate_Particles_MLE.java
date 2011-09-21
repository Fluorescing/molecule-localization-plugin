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
import ij.ImagePlus;
import ij.plugin.PlugIn;
import ij.process.ImageConverter;

/** 
 * The locator plug-in using the maximum likelihood estimator.
 */
public class Locate_Particles_MLE implements PlugIn {
    
    @Override
    public final void run(final String args) {
        final ImagePlus imp = getImage();
        
        final AbstractParticleLocator context =
                new MLEParticleLocator(imp.getImageStack());
        
        final int localizations = context.start();
        
        IJ.log("[" + localizations + " localizations]");
    }
    
    // returns an appropriate image-plus object if available
    private static ImagePlus getImage() {
        ImagePlus imp = IJ.getImage();
        
        // ensure that the image is gray-scaled
        if (imp != null && imp.getType() != ImagePlus.GRAY8 
                        && imp.getType() != ImagePlus.GRAY16
                        && imp.getType() != ImagePlus.GRAY32) {
            imp = imp.duplicate();
            final ImageConverter imgc = new ImageConverter(imp);
            imgc.convertToGray16();
        }
        
        return imp;
    }
}
