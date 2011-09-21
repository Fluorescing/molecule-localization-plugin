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

import java.util.LinkedList;
import java.util.List;

import ij.ImagePlus;
import ij.process.ImageConverter;
import ij.process.ImageProcessor;

/**
 * Holds a duplicate stack that can be modified to show internal information
 * while debugging.
 */
public class DebugImage {
    
    private final ImageProcessor image;
    private boolean nullImage = false;
    
    // highlighters
    private final List<int[]> red;
    private final List<int[]> green;
    private final List<int[]> blue;
    
    /**
     * Constructs around an ImageProcessor.
     * @param image the image to highlight
     */
    public DebugImage(final ImageProcessor image) {
        if (image == null) {
            nullImage = true;
            this.image = null;
            red = null;
            green = null;
            blue = null;
        } else {
            red = new LinkedList<int[]>();
            green = new LinkedList<int[]>();
            blue = new LinkedList<int[]>();
            
            final ImagePlus impTemp = new ImagePlus("Slice", image);
            final ImageConverter imgConv = new ImageConverter(impTemp);
            imgConv.convertToRGB();
            this.image = impTemp.getProcessor();
        }
    }
    
    /**
     * Marks a pixel to be highlighted.
     * @param x the x-coordinate
     * @param y the y-coordinate
     */
    public final void greenHighlight(final int x, final int y) {
        if (!nullImage) {
            if (x >= 0 && y >= 0 
                    && x < image.getWidth() 
                    && y < image.getHeight()) {
                green.add(new int[] {x, y});
            }
        }
    }
    
    /**
     * Marks a pixel to be highlighted.
     * @param x the x-coordinate
     * @param y the y-coordinate
     */
    public final void redHighlight(final int x, final int y) {
        if (!nullImage) {
            if (x >= 0 && y >= 0 
                    && x < image.getWidth() 
                    && y < image.getHeight()) {
                red.add(new int[] {x, y});
            }
        }
    }
    
    /**
     * Marks a pixel to be highlighted.
     * @param x the x-coordinate
     * @param y the y-coordinate
     */
    public final void blueHighlight(final int x, final int y) {
        if (!nullImage) {
            if (x >= 0 && y >= 0 
                    && x < image.getWidth() 
                    && y < image.getHeight()) {
                blue.add(new int[] {x, y});
            }
        }
    }
    
    
    /**
     * Retrieves the final debug image.
     * @return the highlighted image
     */
    public final ImageProcessor getImage() {
        if (!nullImage) {    
            // red highlights
            for (int[] point : red) {
                image.set(point[0], point[1], 
                        image.get(point[0], point[1]) | 0x800000);
            }
            
            // green highlights
            for (int[] point : green) {
                image.set(point[0], point[1], 
                        image.get(point[0], point[1]) | 0x8000);
            }
            
            // blue highlights
            for (int[] point : blue) {
                image.set(point[0], point[1], 
                        image.get(point[0], point[1]) | 0x80);
            }
        }
        
        return image;
    }
}
