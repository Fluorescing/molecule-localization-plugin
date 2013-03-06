package com.m2le.core;

public class Estimate implements Comparable<Estimate> {
    private int x;
    private int y;
    private int slice;
    private double signal;
    
    private double eccentricity;
    private double major;
    private double minor;
    
    private double thirdmomentsum;
    private double thirdmomentdiff;
    
    private double[] hu = new double[8];
    
    private double estrx;
    private double estry;
    private double estIx;
    private double estIy;
    private double estbx;
    private double estby;
    private double estwx;
    private double estwy;

    private boolean rejected;
    
    private boolean eos;
    
    private boolean hasDistance;
    private double centerDistance;
    
    // end of stream
    public Estimate() {
        this.hasDistance = false;
        this.eos = true;
        this.slice = Integer.MAX_VALUE;
    }
    
    public Estimate(int x, int y, int slice, double signal) {
        this.hasDistance = false;
        this.eos = false;
        this.x = x;
        this.y = y; 
        this.slice = slice;
        this.signal = signal;
        this.rejected = false;
    }
    
    public boolean isEndOfQueue() {
        return eos;
    }
    
    public double getDistanceFromCenter() {
        // calculate the distance to center if not already done
        if (!hasDistance) {
            final double dx = estrx - (0.5+x);
            final double dy = estry - (0.5+y);
            centerDistance = Math.sqrt(dx*dx + dy*dy);
            hasDistance = true;
        }
        
        return centerDistance;
    }
    
    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getSlice() {
        return slice;
    }

    public double getSignal() {
        return signal;
    }

    public double getEccentricity() {
        return eccentricity;
    }
    
    public double getMajorAxis() {
        return major;
    }
    
    public double getMinorAxis() {
        return minor;
    }
    
    public void reject() {
        this.rejected = true;
    }
    
    public void unreject() {
        this.rejected = false;
    }
    
    public boolean passed() {
        return !rejected;
    }
    
    public void setEccentricity(double eccentricity) {
        this.eccentricity = eccentricity;
    }
    
    public void setAxis(double major, double minor) {
        this.major = major;
        this.minor = minor;
    }

    public double getXEstimate() {
        return estrx;
    }

    public void setXEstimate(double estrx) {
        this.estrx = estrx;
    }

    public double getYEstimate() {
        return estry;
    }

    public void setYEstimate(double estry) {
        this.estry = estry;
    }

    public double getIntensityEstimateX() {
        return estIx;
    }

    public void setIntensityEstimateX(double estIx) {
        this.estIx = estIx;
    }

    public double getIntensityEstimateY() {
        return estIy;
    }

    public void setIntensityEstimateY(double estIy) {
        this.estIy = estIy;
    }

    public double getBackgroundEstimateX() {
        return estbx;
    }

    public void setBackgroundEstimateX(double estbx) {
        this.estbx = estbx;
    }

    public double getBackgroundEstimateY() {
        return estby;
    }

    public void setBackgroundEstimateY(double estby) {
        this.estby = estby;
    }

    public double getWidthEstimateX() {
        return estwx;
    }

    public void setWidthEstimateX(double estwx) {
        this.estwx = estwx;
    }

    public double getWidthEstimateY() {
        return estwy;
    }

    public void setWidthEstimateY(double estwy) {
        this.estwy = estwy;
    }

    @Override
    public int compareTo(Estimate o) {
        if (this.getSlice() == o.getSlice())
            return 0;
        else if (this.getSlice() > o.getSlice())
            return 1;
        else
            return -1;
    }

    public double getThirdMomentSum() {
        return thirdmomentsum;
    }

    public void setThirdMomentSum(double thirdmomentsum) {
        this.thirdmomentsum = thirdmomentsum;
    }

    public double getThirdMomentDiff() {
        return thirdmomentdiff;
    }

    public void setThirdMomentDiff(double thirdmomentdiff) {
        this.thirdmomentdiff = thirdmomentdiff;
    }
    
    public void setHu(int index, double value) {
        hu[index] = value;
    }
    
    public double getHu(int index) {
        return hu[index];
    }
}
