package com.m2le.core;

import ij.IJ;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public final class RemoveDuplicates {
    
    private RemoveDuplicates() { };
    
    public static class FirstPassThread implements Runnable {
        
        private StackContext stack;
        private BlockingQueue<Estimate> estimates;
        private BlockingQueue<Estimate> reduced;
        
        public FirstPassThread(final StackContext stack, final BlockingQueue<Estimate> estimates, final BlockingQueue<Estimate> reduced) {
            this.stack = stack;
            this.estimates = estimates;
            this.reduced = reduced;
        }

        @Override
        public void run() {
            
            final int W = stack.getWidth();
            final int H = stack.getHeight();
            
            int slice = 0;
            Estimate[][] grid = new Estimate[W][H];
            
            // check all potential pixels
            while (true) {
                try {
                    
                    // get pixel
                    final Estimate estimate = estimates.take();
                    
                    // check for the end of the queue
                    if (estimate.isEndOfQueue())
                        break;
                    
                    // check the current slice (reset grid if new slice)
                    if (estimate.getSlice() != slice) {
                        slice = estimate.getSlice();
                        for (int x = 0; x < W; x++) {
                            for (int y = 0; y < H; y++) {
                                grid[x][y] = null;
                            }
                        }
                    }
                    
                    // check for a conflict
                    Estimate compare = grid[estimate.getX()][estimate.getY()];
                    if (compare != null) {
                        // choose the lesser of the two center displacements
                        if (compare.getDistanceFromCenter() < estimate.getDistanceFromCenter()) {
                            
                            // replace estimate
                            estimate.reject();
                            
                        } else {
                            
                            compare.reject();
                            
                            // clear grid
                            final int CW = Math.min(W, compare.getX() + 4);
                            final int CH = Math.min(H, compare.getY() + 4);
                            for (int x = Math.max(0, compare.getX()-3); x < CW; x++) {
                                for (int y = Math.max(0, compare.getY()-3); y < CH; y++) {
                                    if (grid[x][y] == compare)
                                        grid[x][y] = null;
                                }
                            }
                        }
                    }
                    
                    // put marker down
                    final int EW = Math.min(W, estimate.getX() + 4);
                    final int EH = Math.min(H, estimate.getY() + 4);
                    for (int x = Math.max(0, estimate.getX()-3); x < EW; x++) {
                        for (int y = Math.max(0, estimate.getY()-3); y < EH; y++) {
                            if (grid[x][y] == null || (grid[x][y] != null && estimate.getDistanceFromCenter() < grid[x][y].getDistanceFromCenter()))
                                grid[x][y] = estimate;
                        }
                    }
                    
                    // put it back if it survived
                    if (estimate.passed())
                        reduced.put(estimate);
                    
                } catch (InterruptedException e) {
                    IJ.handleException(e);
                }
            }
        }      
    }
    
    public static class SecondPassThread implements Runnable {
        
        private BlockingQueue<Estimate> reduced;
        private BlockingQueue<Estimate> finalreduced;
        
        public SecondPassThread(final BlockingQueue<Estimate> reduced, final BlockingQueue<Estimate> finalreduced) {
            this.reduced = reduced;
            this.finalreduced = finalreduced;
        }

        @Override
        public void run() {
            
            // further reduce the estimates
            while (true) {
                try {
                    
                    // get pixel
                    final Estimate estimate = reduced.take();
                    
                    // check for the end of the queue
                    if (estimate.isEndOfQueue())
                        break;
                    
                    // put it back if it survived
                    if (estimate.passed())
                        finalreduced.put(estimate);
                    
                } catch (InterruptedException e) {
                    IJ.handleException(e);
                }
            }
        }      
    }
    
    public static List<BlockingQueue<Estimate>> findSubset(final StackContext stack, final List<BlockingQueue<Estimate>> estimates) {
        
        final int numCPU = ThreadHelper.getProcessorCount();
        final Thread[] threads = new Thread[numCPU];
        
        final List<BlockingQueue<Estimate>> reduced = new ArrayList<BlockingQueue<Estimate>>(numCPU);
        
        for (int i = 0; i < numCPU; i++) {
            reduced.add(i, new LinkedBlockingQueue<Estimate>());
        }
        
        for (int n = 0; n < numCPU; n++) {
            Runnable r = new FirstPassThread(stack, estimates.get(n), reduced.get(n));
            threads[n] = new Thread(r);
        }
        
        // start the threads
        ThreadHelper.startThreads(threads);
        
        // mark the end of the queue
        ThreadHelper.markEndOfQueue(reduced);
        
        final List<BlockingQueue<Estimate>> finalreduced = new ArrayList<BlockingQueue<Estimate>>(numCPU);
        
        for (int i = 0; i < numCPU; i++) {
            finalreduced.add(i, new LinkedBlockingQueue<Estimate>());
        }
        
        for (int n = 0; n < numCPU; n++) {
            Runnable r = new SecondPassThread(reduced.get(n), finalreduced.get(n));
            threads[n] = new Thread(r);
        }
        
        // start the threads
        ThreadHelper.startThreads(threads);
        
        // mark the end of the queue
        ThreadHelper.markEndOfQueue(finalreduced);
        
        return finalreduced;
    }
}
