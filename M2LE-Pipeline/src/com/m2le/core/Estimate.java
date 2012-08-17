package com.m2le.core;

public class Estimate {
    private int x;
    private int y;
    private int slice;
    private double signal;
    
    private double eccentricity;
    private double major;
    private double minor;
    
    private double estrx;
    private double estry;
    private double estIx;
    private double estIy;
    private double estbx;
    private double estby;
    private double estwx;
    private double estwy;

    private boolean rejected;
    
    public Estimate(Pixel pixel) {
        this.x = pixel.getX();
        this.y = pixel.getY(); 
        this.slice = pixel.getSlice();
        this.signal = pixel.getSignal();
        this.eccentricity = pixel.getEccentricity();
        this.major = pixel.getMajorAxis();
        this.minor = pixel.getMinorAxis();
        this.rejected = false;
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
}
