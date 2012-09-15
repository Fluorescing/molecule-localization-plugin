package com.m2le.core;

import ij.WindowManager;
import ij.text.TextWindow;

import java.awt.Frame;
import java.util.LinkedList;
import java.util.List;

public final class UserParams {
    
    private UserParams() { }
    
    public static final String VERSION = "1.1.12-1";
    
    public static final String SN_RATIO = "M2LEPL.IA.SNC";
    public static final String LOWEST_NOISE_EST = "M2LEPL.IA.LNE";
    public static final String PIXEL_SIZE = "M2LEPL.IA.PS";
    public static final String SATURATION = "M2LEPL.IA.SP";
    public static final String DEBUG_MODE = "M2LEPL.IA.DM";
    public static final String ECC_THRESHOLD = "M2LEPL.MR.ET";
    public static final String ECC_RADIUS = "M2LEPL.MR.TR";
    public static final String ECC_DISABLED = "M2LEPL.MR.DER";
    public static final String WAVELENGTH = "M2LEPL.ML.LW";
    public static final String USABLE_PIXEL = "M2LEPL.ML.UP";
    public static final String ML_POS_EPSILON = "M2LEPL.ML.PT";
    public static final String ML_INT_EPSILON = "M2LEPL.ML.IT";
    public static final String ML_WID_EPSILON = "M2LEPL.ML.WT";
    public static final String ML_MAX_ITERATIONS = "M2LEPL.ML.MI";
    public static final String ML_RADIUS = "M2LEPL.ML.IR";
    public static final String ML_MAX_NOISE = "M2LEPL.ML.MNM";
    public static final String ML_MIN_NOISE = "M2LEPL.ML.MNB";
    public static final String ML_MAX_WIDTH = "M2LEPL.ML.MAW";
    public static final String ML_MIN_WIDTH = "M2LEPL.ML.MIW";
    public static final String DB_TABLE = "M2LEPL.DB.DT";
    public static final String DB_ROI = "M2LEPL.DB.ROI";
    
    /**
     * Returns a version string containing the major, minor, and build version.
     * @return The version string.
     */
    public static String getVersionString() {
        return VERSION;
    }
    
    // get a list of all text windows (except log)
    private static String[] getResultsTables() {
        final Frame[] frames = WindowManager.getNonImageWindows();
        final List<String> tables = new LinkedList<String>();
        tables.add("");
        for (Frame f : frames) {
            if (!f.getTitle().equalsIgnoreCase("log") 
                    && f.getClass().equals(TextWindow.class)) {
                tables.add(f.getTitle());
            }
        }
        return tables.toArray(new String[0]);
    }
    
    public static void getUserParameters(final JobContext job) {
        
        final String[] tables = getResultsTables();
        
        job.addMessage("Image Analysis");
        job.addNumericParam(SN_RATIO,           "SignalNoise Cutoff",       4.0,  2, "");
        job.addNumericParam(LOWEST_NOISE_EST,   "Lowest Noise Estimate",    2.0,  0, "photons");
        job.addNumericParam(PIXEL_SIZE,         "Pixel Size",             110.0,  2, "nanometers");
        job.addNumericParam(SATURATION,         "Saturation Point",     65535.0,  2, "photons");
        
        job.addMessage("Debug Options");
        job.addCheckboxParam(DEBUG_MODE,        "Debug Mode",   false);
        job.addChoiceParam(DB_TABLE,            "Debug Table",  tables);
        
        job.addMessage("Molecule Rejection");
        job.addNumericParam(ECC_THRESHOLD,      "Eccentricity Threshold",    .6,  1, "");
        job.addCheckboxParam(ECC_DISABLED,      "Disable Ellipticity Rejector", false);
        
        job.addMessage("Maximum Likelihood Estimator");
        job.addNumericParam(WAVELENGTH,         "Light Wavelength",       550.0,  1, "nanometers");
        job.addNumericParam(USABLE_PIXEL,       "Usable Pixel",            90.0,  1, "%");
        job.addNumericParam(ML_POS_EPSILON,     "Position Threshold",       0.0001, 4, "nanometers");
        job.addNumericParam(ML_INT_EPSILON,     "Intensity Threshold",      0.01,  4, "%");
        job.addNumericParam(ML_WID_EPSILON,     "Width Threshold",          0.0001, 4, "???");
        job.addNumericParam(ML_MAX_ITERATIONS,  "Maximum Iterations",      50.0,  0, "");
        
        job.addMessage("Parameter Bounds");
        job.addNumericParam(ML_MAX_NOISE,       "Max Noise Multiplier",     2.0,  0, "");
        job.addNumericParam(ML_MIN_NOISE,       "Min Noise Bound",          1.0,  2, "photons");
        job.addNumericParam(ML_MAX_WIDTH,       "MaxWidth",                 3.0,  2, "???");
        job.addNumericParam(ML_MIN_WIDTH,       "MinWidth",                 1.5,  2, "???");
        
        job.addMessage(String.format("Version: %s", VERSION));
    }
}
