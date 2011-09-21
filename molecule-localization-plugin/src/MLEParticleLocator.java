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

import ij.ImageStack;

/**
 * The Maximum Likelihood flavor of the particle locator.
 */
public class MLEParticleLocator extends AbstractParticleLocator {

    /**
     * @param stack the stack of images to process.
     */
    public MLEParticleLocator(final ImageStack stack) {
        super(stack);

        final EllipticityRejector ellipRejector = new EllipticityRejector();

        // create new instances of used classes
        final MaximumLikelihoodEstimator mle = 
                new MaximumLikelihoodEstimator();
        
        processes.add(ellipRejector);
        processes.add(mle); // add to list of processes
        
        settings.add(ellipRejector);
        settings.add(mle);  // to retrieve settings
        
        debugging.add(ellipRejector);
        debugging.add(mle); // for logging purposes
    }
}