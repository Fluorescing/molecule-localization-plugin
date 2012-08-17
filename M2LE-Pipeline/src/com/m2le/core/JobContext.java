/*
 * Copyright (C) 2012 Shane Stahlheber
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.m2le.core;

import java.awt.Font;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import ij.IJ;
import ij.Prefs;
import ij.gui.GenericDialog;

/**
 * Keeps track of job-specific information, including preference setting/saving
 * and debugging information.
 */
public class JobContext {
    
    public enum Type {
        NUMERIC, MESSAGE, CHECKBOX, CHOICE
    }
    
    private class Parameter {
        public Type type;
        public String id;
        public String name;
        public double numeric;
        public boolean checkbox;
        public String choice[];
        public int precision;
        public String units;
    };
    
    private final List<Parameter> mList = new LinkedList<Parameter>();
    private final Map<String,Double>  mNumeric  = new HashMap<String,Double>();
    private final Map<String,Boolean> mCheckbox = new HashMap<String,Boolean>();
    private final Map<String,String>  mChoice   = new HashMap<String,String>();
    private boolean mCanceled = false;
    
    public boolean isCanceled() {
        return mCanceled;
    }
    
    public void addMessage(final String message) {
        final Parameter param = new Parameter();
        param.type = Type.MESSAGE;
        param.name = message;
        mList.add(param);
    }
    
    public void addNumericParam(
            final String id, 
            final String name, 
            final double defaultValue, 
            final int precision, 
            final String units) {
        final Parameter param = new Parameter();
        param.type = Type.NUMERIC;
        param.id = id;
        param.name = name;
        param.numeric = defaultValue;
        param.precision = precision;
        param.units = units;
        mList.add(param);
    }
    
    public void addCheckboxParam(
            final String id, 
            final String name, 
            final boolean defaultValue) {
        final Parameter param = new Parameter();
        param.type = Type.CHECKBOX;
        param.id = id;
        param.name = name;
        param.checkbox = defaultValue;
        mList.add(param);
    }
    
    public void addChoiceParam(
            final String id, 
            final String name, 
            final String[] choices) {
        final Parameter param = new Parameter();
        param.type = Type.CHOICE;
        param.id = id;
        param.name = name;
        param.choice = choices;
        mList.add(param);
    }
    
    public double getNumericValue(final String id) {
        return mNumeric.get(id);
    }
    
    public boolean getCheckboxValue(final String id) {
        return mCheckbox.get(id);
    }
    
    public String getChoice(final String id) {
        return mChoice.get(id);
    }
    
    
    
    // create and show the preference dialog to the user
    private GenericDialog createDialog() {
        final Font header = new Font(Font.SANS_SERIF, Font.BOLD, 14);
        
        final GenericDialog dialog = new GenericDialog("M2LE Options");
        
        dialog.centerDialog(true);
        
        for (Parameter param : mList) {
            switch (param.type) {
            case MESSAGE:
                dialog.addMessage(param.name, header);
                break;
            case NUMERIC:
                dialog.addNumericField(param.name, 
                        Prefs.get(param.id, param.numeric), param.precision,
                        6, param.units);
                break;
            case CHECKBOX:
                dialog.addCheckbox(param.name,
                        Prefs.get(param.id, param.checkbox));
                break;
            case CHOICE:
                dialog.addChoice(param.name, param.choice, param.choice[0]);
                break;
            default:
                break;
            }
        }
        
        dialog.showDialog();
        
        return dialog;
    }
    
    // grab the preferences from the dialog (must be in the same order)
    private void getPreferences(final GenericDialog dialog) {
        
        for (Parameter param : mList) {
            switch (param.type) {
            case NUMERIC:
                final Double numeric = dialog.getNextNumber();
                mNumeric.put(param.id, numeric);
                break;
            case CHECKBOX:
                final Boolean checkbox = dialog.getNextBoolean();
                mCheckbox.put(param.id, checkbox);
                break;
            case CHOICE:
                final String choice = dialog.getNextChoice();
                mChoice.put(param.id, choice);
                break;
            default:
                break;
            }
        }
    }
    
    // save the preferences in ImageJ for later use of the plugin
    private void savePreferences() {
        for (Parameter param : mList) {
            switch (param.type) {
            case NUMERIC:
                Prefs.set(param.id, mNumeric.get(param.id));
                break;
            case CHECKBOX:
                Prefs.set(param.id, mCheckbox.get(param.id));
                break;
            default:
                break;
            }
        }
    }
    
    // log preferences and setup debug stats
    public void logParameters() {
        
        for (Parameter param : mList) {
            switch (param.type) {
            case MESSAGE:
                IJ.log(String.format("==%s==", param.name));
                break;
            case NUMERIC:
                IJ.log(String.format("%s: %g", param.name, mNumeric.get(param.id)));
                break;
            case CHECKBOX:
                IJ.log(String.format("%s: %s", param.name, mCheckbox.get(param.id)?"True":"False"));
                break;
            case CHOICE:
                IJ.log(String.format("%s: %s", param.name, mChoice.get(param.id)));
                break;
            default:
                break;
            }
        }
    }
    
    /**
     * 
     * @return the new job context.
     */
    public void initialize() {
        
        // show the dialog to the user
        final GenericDialog dialog = createDialog();
        
        if (dialog.wasCanceled())
            mCanceled = true;
        else
            mCanceled = false;
        
        // create a jobcontext from the user-set preferences
        getPreferences(dialog);
        
        // save the preferences in ImageJ
        savePreferences();
    }

}
