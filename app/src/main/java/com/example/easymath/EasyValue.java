package com.example.easymath;

public class EasyValue {
    public EasyValue (int r_, int g_, int b_) {
        r = r_;
        g = g_;
        b = b_;
    }

    public EasyValue (EasyValue val) {
        r = val.r;
        g = val.g;
        b = val.b;
    }

    public int r, g, b; //TODO: make real values
}
