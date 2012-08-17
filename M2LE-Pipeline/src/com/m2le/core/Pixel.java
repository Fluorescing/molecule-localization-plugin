package com.m2le.core;

public class Pixel {
    
    private int x;
    private int y;
    private int slice;
    private double signal;
    
    private double eccentricity;
    private double major;
    private double minor;

    private boolean rejected;
    
    public Pixel(int x, int y, int slice, double signal) {
        this.x = x;
        this.y = y; 
        this.slice = slice;
        this.signal = signal;
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
}
