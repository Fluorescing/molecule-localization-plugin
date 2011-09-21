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

/**
 * Reconstructs an image based on the localizations found.
 */
public class Reconstruction {

    private final List<double[]> points = new LinkedList<double[]>();
    
    /**
     * Adds the specified coordinates to the list of particles to render.
     * @param x the x-coordinate of the localization
     * @param y the y-coordinate of the localization
     */
    public final void add(final double x, final double y) {
        synchronized (this) {
            points.add(new double[] {x, y});
        }
    }
    
    /**
     * Renders the image based on the particles added to the reconstruction.
     * @param srcTop the top of the area to reconstruct
     * @param srcLeft the left of the area to reconstruct
     * @param srcWidth the width of the area to reconstruct
     * @param srcHeight the height of the area to reconstruct
     * @param dstWidth the width of the reconstruction
     * @param dstHeight the height of the reconstruction
     * @param dstScale the value scale to use for the resulting data
     * @param scalebarWidth The width of the scale bar in source pixels
     * @return the reconstructed image
     */
    public final int[][] reconstruct(final double srcTop, 
                                     final double srcLeft, 
                                     final double srcWidth, 
                                     final double srcHeight, 
                                     final int dstWidth, 
                                     final int dstHeight,
                                     final int dstScale,
                                     final double scalebarWidth) {
        
        final int[][] image = zeros(new int[dstWidth][dstHeight]);
        
        int max = 0;
        for (double[] point : points) {
            if (point == null) {
                continue;
            }
            final int x = (int) ((point[0] - srcLeft) / srcWidth * dstWidth);
            final int y = (int) ((point[1] - srcTop) / srcHeight * dstHeight);
            
            // skip point if out of view
            if (x < 0 || y < 0 || x >= dstWidth || y >= dstHeight) {
                continue;
            }
            
            // accumulate the point
            image[x][y] += 1;
            
            // update maximum
            if (image[x][y] > max) {
                max = image[x][y];
            }
            
        }
        
        // return an image with a value range from 0.0 to 1.0
        final int[][] temp = div(mul(image, dstScale), max);
        
        // draw scale bar
        final int width = (int) Math.round(scalebarWidth * dstWidth / srcWidth);
        for (int x = 48; x < 48 + width; x++) {
            for (int y = dstHeight - 64; y < dstHeight - 48; y++) {
                if (x < dstWidth) {
                    temp[x][y] = dstScale - 100;
                }
            }
        }
        
        return temp;
    }
    
    // returns a matrix filled in with zeros.
    private static int[][] zeros(final int[][] matrix) {
        for (int x = 0; x < matrix.length; x++) {
            for (int y = 0; y < matrix[x].length; y++) {
                matrix[x][y] = 0;
            }
        }
        return matrix;
    }
    
    // returns the product between a matrix and a scalar.
    private static int[][] mul(final int[][] matrix, 
                               final int scale) {
        for (int x = 0; x < matrix.length; x++) {
            for (int y = 0; y < matrix[x].length; y++) {
                matrix[x][y] *= scale;
            }
        }
        return matrix;
    }
    
    // divides the matrix elements by a scalar.
    private static int[][] div(final int[][] matrix, 
                               final int denominator) {
        for (int x = 0; x < matrix.length; x++) {
            for (int y = 0; y < matrix[x].length; y++) {
                matrix[x][y] /= denominator;
            }
        }
        return matrix;
    }
}
