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

/**
 * Stores the coordinates of a point in double-precision floating-point values.
 */
public class Coordinates {
    
    private double x;
    private double y;
    
    /**
     * @param x the x-coordinate
     * @param y the y-coordinate
     */
    public Coordinates(final double x, final double y) {
        this.x = x;
        this.y = y;
    }
    
    
    /**
     * @return the x-coordinate
     */
    public double getX() {
        return x;
    }
    
    /**
     * @return the y-coordinate
     */
    public double getY() {
        return y;
    }
    
    /**
     * @param x the x-coordinate
     */
    public void setX(final double x) {
        this.x = x;
    }
    
    /**
     * @param y the y-coordinate
     */
    public void setY(final double y) {
        this.y = y;
    }
    
}
