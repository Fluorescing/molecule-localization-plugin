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

import java.awt.Point;
import java.util.Comparator;
import java.util.PriorityQueue;

import ij.process.ImageProcessor;

/**
 * Searches for all potential pixels and stores them for later retrieval.
 */
public class FindParticles {
    
    private final ImageContext context; // the current image context
    private Point next = null;          // the location of the next spot
    private boolean ready = false;      // is the next spot is ready
    private boolean found = true;       // indicates if there is another spot
    private final int threshold;        // the threshold at which spots occur
    private final PriorityQueue<WeightedPoint> queue;
    
    /**
     * Default constructor.
     * @param context the image context to work from
     */
    public FindParticles(final ImageContext context) {
        this.context = context;

        // get signal-to-noise ratio
        final double snr = context.getLocatorContext().getSignalToNoiseRatio();
        
        // estimate background noise
        final double noise = context.getEstimatedNoise();
        
        // calculate noise threshold
        threshold = (int) Math.round(snr * noise);
        
        final Comparator<WeightedPoint> comparator = 
            new WeightedPoint.WeightedPointComparator();
        
        queue = new PriorityQueue<WeightedPoint>(10, comparator);
        
        // fill a queue of spots to look at
        fillQueue();
    }
    
    // enqueue all of the potential particle locations.
    private void fillQueue() {
        final ImageProcessor image = context.getImage();
        
        // find the next, unprocessed max
        for (int i = 0; i < image.getWidth(); i++) {
            for (int j = 0; j < image.getHeight(); j++) {
                final int intensity = image.get(i, j);
                if (intensity > threshold) {
                    queue.add(new WeightedPoint(i, j, intensity));
                }
            }
        }
    }
    
    // readies the next location in the queue
    private void findLocation() {
        do {
            next = queue.poll();
            
            if (next == null) {
                break;
            }
            
        } while (context.isProcessed(next.x, next.y));
        
        // has a max been found?
        if (next == null) {
            found = false;
        } else {
            found = true;
        }
        
        ready = true;
    }
    
    /**
     * Checks if a there is another max pixel to process.
     * @return true if there is another location; false otherwise
     */
    public final boolean hasNext() {
        if (!ready) {
            findLocation();
        }
        
        return found;
    }
    
    /**
     * Retrieves the next point in the image.
     * @return the next point
     */
    public final Point getNext() {
        if (!ready) {
            findLocation();
        }
        
        ready = false;
        return next;
    }
}
