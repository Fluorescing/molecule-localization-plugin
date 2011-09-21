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
import java.io.Serializable;
import java.util.Comparator;

/**
 * This class extends the basic point structure and give each point a weight. 
 * The weight is to allow the points to be sorted by brightness.
 */
@SuppressWarnings("serial")
public class WeightedPoint extends Point {
    
    private int weight;
    
    /**
     * @param xpos the x-coordinate
     * @param ypos the y-coordinate
     * @param weight the weight of the point
     */
    public WeightedPoint(final int xpos, final int ypos, final int weight) {
        super(xpos, ypos);
        this.weight = weight;
    }
    
    /**
     * @return the weight of this point.
     */
    public final int getWeight() {
        return weight;
    }
    
    /**
     * @param weight the weight of the point
     */
    public final void setWeight(final int weight) {
        this.weight = weight;
    }
    
    @Override
    public final boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final WeightedPoint point = (WeightedPoint) obj;
        if (point.weight != weight) {
            return false;
        }
        return super.equals(obj);
    }
    
    @Override
    public final int hashCode() {
        return super.hashCode() + 1;
    }
    
    /**
     * This comparator allows Java's built-in array sorter to sort the points
     * in decreasing order.  Use getWeight() to compare individual points.
     * 
     * @author Shane Stahlheber
     */
    public static class WeightedPointComparator implements 
            Comparator<WeightedPoint>, Serializable {

        @Override
        public final int compare(final WeightedPoint point1, 
                final WeightedPoint point2) {
            return point2.weight - point1.weight;
        }
        
    }
}
