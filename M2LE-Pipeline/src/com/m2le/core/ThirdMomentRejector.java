package com.m2le.core;

import ij.IJ;
import ij.process.ImageProcessor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public final class ThirdMomentRejector {

    private ThirdMomentRejector() { }
    
    public static class ThirdRejectionThread implements Runnable {
        
        private StackContext stack;
        private BlockingQueue<Estimate> estimates;
        private BlockingQueue<Estimate> finalestimates;
        
        public ThirdRejectionThread(final StackContext stack, final BlockingQueue<Estimate> estimates, final BlockingQueue<Estimate> finalestimates) {
            this.stack = stack;
            this.estimates = estimates;
            this.finalestimates = finalestimates;
        }

        @Override
        public void run() {
            
            final boolean disabled = stack.getJobContext().getCheckboxValue(UserParams.THRD_DISABLED);
            
            // check all potential pixels
            while (true) {
                try {
                    
                    // get pixel
                    final Estimate estimate = estimates.take();
                    
                    // check for the end of the queue
                    if (estimate.isEndOfQueue())
                        break;
                    
                    // process the pixel
                    updatePixel(stack, estimate);
                    
                    // put it back if it survived
                    if (estimate.passed() || disabled) {
                        estimate.unreject();
                        finalestimates.put(estimate);
                    }
                    
                } catch (InterruptedException e) {
                    IJ.handleException(e);
                }
            }
        }      
    }
    
    public static List<BlockingQueue<Estimate>> findSubset(final StackContext stack, final List<BlockingQueue<Estimate>> estimates) {
        
        final int numCPU = ThreadHelper.getProcessorCount();
        final Thread[] threads = new Thread[numCPU];
        
        final List<BlockingQueue<Estimate>> finalestimates = new ArrayList<BlockingQueue<Estimate>>(numCPU);
        
        for (int i = 0; i < numCPU; i++) {
            finalestimates.add(i, new LinkedBlockingQueue<Estimate>());
        }
        
        
        for (int n = 0; n < numCPU; n++) {
            Runnable r = new ThirdRejectionThread(stack, estimates.get(n), finalestimates.get(n));
            threads[n] = new Thread(r);
        }
        
        // start the threads
        ThreadHelper.startThreads(threads);
        
        // mark the end of the queue
        ThreadHelper.markEndOfQueue(finalestimates);
        
        return finalestimates;
    }
    
    private static void updatePixel(final StackContext stack, final Estimate estimate) {
    
        final ImageProcessor ip = stack.getImageProcessor(estimate.getSlice());
        final JobContext job = stack.getJobContext();
        
        // get the window dimensions
        final int x = estimate.getX();
        final int y = estimate.getY();
        
        final double wavelength = stack.getJobContext().getNumericValue(UserParams.WAVELENGTH) / stack.getJobContext().getNumericValue(UserParams.PIXEL_SIZE);
        final double width = (estimate.getWidthEstimateX() + estimate.getWidthEstimateY())/2.0;
        
        // prevent out-of-bounds errors
        final int left     = Math.max(0, x - 3);
        final int right    = Math.min(ip.getWidth(), x + 4);
        final int top      = Math.max(0, y - 3);
        final int bottom   = Math.min(ip.getHeight(), y + 4);
        
        final double noise = StaticMath.estimateNoise(stack, estimate);
        final double acceptance = job.getNumericValue(UserParams.THRD_THRESHOLD);
        final double intensity = StaticMath.estimatePhotonCount(ip, left, right, top, bottom, noise);
        final double threshold = StaticMath.calculateThirdThreshold(intensity, acceptance*100.0);
        
        // compute eigenvalues
        final double[] centroid = new double[] {estimate.getXEstimate(), estimate.getYEstimate()};
        final double[] sumdiff = StaticMath.calculateMonteCarloThirdMoments(ip, centroid, left, right, top, bottom, noise, wavelength, width);
        
        // save major/minor axis and eccentricity
        final double thirdsum = sumdiff[0]/StaticMath.getThirdMomentSumScaling(intensity);
        final double thirddiff = sumdiff[1]/StaticMath.getThirdMomentDiffScaling(intensity);
        estimate.setThirdMomentSum(thirdsum);
        estimate.setThirdMomentDiff(thirddiff);
        
        // check if the pixel should be rejected
        if ((thirddiff*thirddiff + thirdsum*thirdsum) >= threshold) 
            estimate.reject();
    }
}
