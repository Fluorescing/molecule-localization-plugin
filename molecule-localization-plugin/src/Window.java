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
 * This class is used to store the window dimensions of a region of interest.
 */
public class Window {
    
    /** The top of the window. */
    public int top;
    
    /** The bottom of the window. */
    public int bottom;
    
    /** The left of the window. */
    public int left;
    
    /** The right of the window. */
    public int right;
    
    /** 
     * Window constructor.
     * @param top the top of the window
     * @param bottom the bottom of the window
     * @param left the left side of the window
     * @param right the right side of the window
     */
    public Window(final int top, final int bottom, final int left, 
            final int right) {
        this.top = top;
        this.bottom = bottom;
        this.left = left;
        this.right = right;
    }
}
