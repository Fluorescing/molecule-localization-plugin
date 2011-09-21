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

import ij.gui.GenericDialog;

import java.awt.Font;

/**
 * An interface for setting up the initial settings dialog.
 */
public interface SettingsDialog {
    
    /**
     * Adds options to the dialog at the start of the plug-in.
     * @param dialog the dialog to add to
     * @param header the font for the header
     */
    void displaySettings(GenericDialog dialog, Font header);
    
    /**
     * Retrieves and saves the options from the dialog (assuming the dialog 
     * wasn't cancelled).
     * @param dialog the dialog to save from
     */
    void saveSettings(GenericDialog dialog);
}
