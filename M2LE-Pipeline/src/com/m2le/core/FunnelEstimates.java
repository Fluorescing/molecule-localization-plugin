package com.m2le.core;

import ij.IJ;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public final class FunnelEstimates {
    
    private FunnelEstimates() { };
    
    public static class FunnelThread implements Runnable {
        
        private BlockingQueue<Estimate> estimates;
        private BlockingQueue<Estimate> funnelled;
        
        public FunnelThread(final BlockingQueue<Estimate> estimates, final BlockingQueue<Estimate> funnelled) {
            this.estimates = estimates;
            this.funnelled = funnelled;
        }

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
                    
                    // put it back
                    funnelled.put(estimate);
                    
                } catch (InterruptedException e) {
                    IJ.handleException(e);
                }
            }
        }      
    }
    
    public static BlockingQueue<Estimate> findSubset(final StackContext stack, final List<BlockingQueue<Estimate>> estimates) {
        
        final int numCPU = ThreadHelper.getProcessorCount();
        final Thread[] threads = new Thread[numCPU];
        
        final BlockingQueue<Estimate> funnelled = new LinkedBlockingQueue<Estimate>();
        
        for (int n = 0; n < numCPU; n++) {
            Runnable r = new FunnelThread(estimates.get(n), funnelled);
            threads[n] = new Thread(r);
        }
        
        // start the threads
        ThreadHelper.startThreads(threads);
        
        // mark the end of the queue
        ThreadHelper.markEndOfQueueSingle(funnelled);
        
        return funnelled;
    }
}
