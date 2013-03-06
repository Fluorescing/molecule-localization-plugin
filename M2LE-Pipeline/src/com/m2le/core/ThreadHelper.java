package com.m2le.core;

import java.util.List;
import java.util.concurrent.BlockingQueue;

import ij.IJ;

public final class ThreadHelper {
    
    private ThreadHelper() { }
    
    public static int getProcessorCount() {
        return Runtime.getRuntime().availableProcessors();
    }
    
    public static void markEndOfQueue(List<BlockingQueue<Estimate>> estimates) {
        // mark the end of the queue
        int numCPU = ThreadHelper.getProcessorCount();
        
        for (int n = 0; n < numCPU; n++)
            estimates.get(n).add(new Estimate());
    }
    
    public static void markEndOfQueueSingle(BlockingQueue<Estimate> estimates) {
        estimates.add(new Estimate());
    }
    
    public static void startThreads(final Thread[] threads) {
        
        // start the threads
        for (Thread thread : threads) {
            thread.start();
        }
        
        // join the threads (waits for them to finish)
        try {
            for (Thread thread : threads) {
                thread.join();
            }
        } catch (InterruptedException e) {
            IJ.handleException(e);
        }
    }
}
