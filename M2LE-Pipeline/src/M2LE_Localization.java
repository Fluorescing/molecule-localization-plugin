
import java.util.concurrent.LinkedBlockingQueue;

import com.m2le.core.EccentricityRejector;
import com.m2le.core.Estimate;
import com.m2le.core.JobContext;
import com.m2le.core.LocatePotentialPixels;
import com.m2le.core.MoleculeLocator;
import com.m2le.core.Pixel;
import com.m2le.core.RemoveDuplicates;
import com.m2le.core.StackContext;
import com.m2le.core.UserParams;

import ij.IJ;
import ij.WindowManager;
import ij.measure.ResultsTable;
import ij.plugin.PlugIn;
import ij.text.TextPanel;
import ij.text.TextWindow;

public class M2LE_Localization implements PlugIn {

    /**
     * The starting point for the M2LE plugin for ImageJ.
     * @param arg the arguments given by ImageJ
     */
    @Override
    public void run(String arg) {
     // Get the job from the user
        final JobContext job = new JobContext();
        
        UserParams.getUserParameters(job);
        job.initialize();
        
        // check if user cancelled
        if (job.isCanceled()) {
            return;
        }
        
        final boolean debugMode = job.getCheckboxValue(UserParams.DEBUG_MODE);
        
        final String debugTableTitle = job.getChoice(UserParams.DB_TABLE);
        final TextPanel tp = ((TextWindow) WindowManager.getFrame(debugTableTitle)).getTextPanel();
        final ResultsTable debugTable = (tp == null) ? null : tp.getResultsTable();
        
        // load the image stack
        final StackContext stack = new StackContext(job);
        
        if (stack.loadFailed()) {
            IJ.showMessage("M2LE Warning", "No images to analyze!");
            return;
        }
        
        // iterate through all images in the stack
        final ResultsTable results = new ResultsTable();
        results.setPrecision(10);
        
        // find all potential pixels
        LinkedBlockingQueue<Pixel> potential = LocatePotentialPixels.findPotentialPixels(stack);
        
        // find subset of potential pixels that pass an eccentricity test
        if (!job.getCheckboxValue(UserParams.ECC_DISABLED)) {
            potential = EccentricityRejector.findSubset(stack, potential);
        }
        
        // transform the PE pixels into localization estimates
        LinkedBlockingQueue<Estimate> estimates = MoleculeLocator.findSubset(stack, potential);
        
        // weed out duplicates (choose the estimate carefully)
        estimates = RemoveDuplicates.findSubset(stack, estimates);
        
        final int pixelSize = (int) job.getNumericValue(UserParams.PIXEL_SIZE);
        
        // add to results table
        int SIZE = estimates.size();
        while (!estimates.isEmpty()) {
            try {
                // get pixel
                final Estimate estimate = estimates.take();
                
                results.incrementCounter();
                results.addValue("Frame", estimate.getSlice());
                results.addValue("x (px)", estimate.getXEstimate());
                results.addValue("y (px)", estimate.getYEstimate());
                results.addValue("x (nm)", estimate.getXEstimate()*pixelSize);
                results.addValue("y (nm)", estimate.getYEstimate()*pixelSize);
                results.addValue("Intensity x", estimate.getIntensityEstimateX());
                results.addValue("Intensity y", estimate.getIntensityEstimateY());
                results.addValue("Background x", estimate.getBackgroundEstimateX());
                results.addValue("Background y", estimate.getBackgroundEstimateY());
                results.addValue("Width x", estimate.getWidthEstimateX());
                results.addValue("Width y", estimate.getWidthEstimateY());
                
                if (debugMode) {
                    results.addValue("Minor Axis", estimate.getMinorAxis());
                    results.addValue("Major Axis", estimate.getMajorAxis());
                    
                    if (debugTable != null) {
                        for (int column = 0; debugTable.columnExists(column); column++) {
                            final String name = "D_"+debugTable.getColumnHeading(column);
                            final double value = debugTable.getValueAsDouble(column, estimate.getSlice()-1);
                            results.addValue(name, value);
                        }
                    }
                }
                
            } catch (InterruptedException e) {
                IJ.handleException(e);
            }
        }
        
        // show the results
        results.show("Localization Results");
        
        IJ.log(String.format("[%d localizations]", SIZE));
    }

}
