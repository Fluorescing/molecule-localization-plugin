
import java.util.concurrent.BlockingQueue;

import com.m2le.core.EccentricityRejector;
import com.m2le.core.Estimate;
import com.m2le.core.JobContext;
import com.m2le.core.LocatePotentialPixels;
import com.m2le.core.MoleculeLocator;
import com.m2le.core.RemoveDuplicates;
import com.m2le.core.StackContext;
import com.m2le.core.ThirdMomentRejector;
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
        ResultsTable debugTable = null;
        
        final String debugTableTitle = job.getChoice(UserParams.DB_TABLE);
        if (!debugTableTitle.equals("")) {
            final TextPanel tp = ((TextWindow) WindowManager.getFrame(debugTableTitle)).getTextPanel();
            debugTable = (tp == null) ? null : tp.getResultsTable();
        }
    
        // load the image stack
        final StackContext stack = new StackContext(job);
        
        if (stack.loadFailed()) {
            IJ.showMessage("M2LE Warning", "No images to analyze!");
            return;
        }
        
        // iterate through all images in the stack
        final ResultsTable results = new ResultsTable();
        results.setPrecision(10);
        
        IJ.showProgress(0, 100);
        IJ.showStatus("Locating Potential Molecules...");
        
        // find all potential pixels
        BlockingQueue<Estimate> potential = LocatePotentialPixels.findPotentialPixels(stack);
        
        IJ.showProgress(25, 100);
        IJ.showStatus("Testing Eccentricity...");
        
        // find subset of potential pixels that pass an eccentricity test
        potential = EccentricityRejector.findSubset(stack, potential);
        
        IJ.showProgress(50, 100);
        IJ.showStatus("Localizing Molecules...");
        
        // transform the PE pixels into localization estimates
        BlockingQueue<Estimate> estimates = MoleculeLocator.findSubset(stack, potential);
        
        IJ.showProgress(63, 100);
        IJ.showStatus("Reticulating Splines...");
        
        // find subset of potential pixels that pass the third moments test
        estimates = ThirdMomentRejector.findSubset(stack, estimates);
        
        IJ.showProgress(75, 100);
        IJ.showStatus("Removing Duplicates...");
        
        // weed out duplicates (choose the estimate carefully)
        estimates = RemoveDuplicates.findSubset(stack, estimates);
        
        IJ.showProgress(100, 100);
        IJ.showStatus("Printing Results...");
        
        final int pixelSize = (int) job.getNumericValue(UserParams.PIXEL_SIZE);
        
        // add to results table
        int SIZE = estimates.size();
        while (true) {
            try {
                // get pixel
                final Estimate estimate = estimates.take();
                
                // check for the end of the queue
                if (estimate.isEndOfQueue())
                    break;

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
                    results.addValue("ROI x", estimate.getX()+0.5);
                    results.addValue("ROI y", estimate.getY()+0.5);
                    
                    results.addValue("thirdsum", estimate.getThirdMomentSum());
                    results.addValue("thirddiff", estimate.getThirdMomentDiff());
                    
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
        
        IJ.showStatus(String.format("%d Localizations.", SIZE));
        IJ.log(String.format("[%d localizations]", SIZE));
    }

}
