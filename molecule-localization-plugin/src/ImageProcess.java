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

/**
 * An interface for objects that provide a method testing for particles.
 */
public interface ImageProcess {
    
    /**
     * Runs a process on the image at the specified location.
     * @param context the current image context
     * @param location the location of the point
     * @return true if passes; false if rejected
     */
    boolean runProcess(ImageContext context, Point location);
}
