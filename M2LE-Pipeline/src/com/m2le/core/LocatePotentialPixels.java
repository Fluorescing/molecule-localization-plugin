package com.m2le.core;

import ij.IJ;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public final class LocatePotentialPixels {
    
    private LocatePotentialPixels() { }
    
    public static BlockingQueue<Estimate> findPotentialPixels(StackContext stack) {
        
        final LinkedBlockingQueue<Estimate> pixels = new LinkedBlockingQueue<Estimate>();
        
        final JobContext job = stack.getJobContext();
        
        final double snCutoff = job.getNumericValue(UserParams.SN_RATIO);
        
        // for all slices in the stack
        final int COUNT = stack.getSize();
        
        for (int slice = 1; slice <= COUNT; slice++) {
            
            // get the image processor for this slice
            final ImageProcessor ip = stack.getImageProcessor(slice);
            
            // get the pixel scaling
            int saturation = 65535;
            if (ip instanceof ByteProcessor)
                saturation = 255;
            final double scale = saturation / job.getNumericValue(UserParams.SATURATION);
            
            // estimate noise
            final double noise = NoiseEstimator.estimateNoise(stack, ip, scale);
            
            // for all pixels in the image
            final int W = ip.getWidth();
            final int H = ip.getHeight();
            
            for (int x = 0; x < W; x++) {
                for (int y = 0; y < H; y++) {
                    
                    // store potential pixels in the queue
                    final double S = ip.get(x, y) / scale;
                    
                    if (S > noise*snCutoff) {
                        try {
                            pixels.put(new Estimate(x, y, slice, S));
                        } catch (InterruptedException e) {
                            IJ.handleException(e);
                        }
                    }
                }
            }
        }
        
        // mark the end of the queue
        ThreadHelper.markEndOfQueue(pixels);
        
        return pixels;
    }
}
