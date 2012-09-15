package com.m2le.core;

import ij.IJ;
import ij.process.ImageProcessor;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public final class ThirdMomentRejector {

    private ThirdMomentRejector() { }
    
    public static BlockingQueue<Estimate> findSubset(final StackContext stack, final BlockingQueue<Estimate> estimates) {
        final LinkedBlockingQueue<Estimate> finalestimates = new LinkedBlockingQueue<Estimate>();
        
        final int numCPU = ThreadHelper.getProcessorCount();
        final Thread[] threads = new Thread[numCPU];
        
        for (int n = 0; n < numCPU; n++) {
            threads[n] = new Thread(String.format("ThirdMomentThread%d", n)) {
                @Override
                public void run() {
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
                            // TODO: currently passing through all estimates
                            //if (estimate.passed())
                            finalestimates.put(estimate);
                            
                        } catch (InterruptedException e) {
                            IJ.handleException(e);
                        }
                    }
                }         
            };
        }
        
        // start the threads
        ThreadHelper.startThreads(threads);
        
        // mark the end of the queue
        ThreadHelper.markEndOfQueue(finalestimates);
        
        return finalestimates;
    }
    
    private static void updatePixel(final StackContext stack, final Estimate estimate) {
    
        final ImageProcessor ip = stack.getImageProcessor(estimate.getSlice());

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
        
        // compute eigenvalues
        final double[] centroid = new double[] {estimate.getXEstimate(), estimate.getYEstimate()};
        final double[] eta  = StaticMath.estimateThirdMoments(ip, centroid, left, right, top, bottom, noise, wavelength, width);
        
        final double eigen1 = estimate.getMajorAxis()*estimate.getMajorAxis();
        final double eigen2 = estimate.getMinorAxis()*estimate.getMinorAxis();
        //final double factor = Math.pow(eigen1 + eigen2, 1.5);
    
        
        // save major/minor axis and eccentricity
        estimate.setThirdMomentSum(eta[0]*eta[0] + eta[1]*eta[1]);
        estimate.setThirdMomentDiff(eta[2]*eta[2] + eta[3]*eta[3]);
        
        // check if the pixel should be rejected
        //if (eccentricity >= threshold) 
        //    estimate.reject();
    }
}
