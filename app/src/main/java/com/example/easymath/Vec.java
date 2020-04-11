package com.example.easymath;

public class Vec {
    public Vec (double x_, double y_) {
        x = x_;
        y = y_;
    }

    public Vec (Vec v) {
        x = v.x;
        y = v.y;
    }

    public void Scale (double val) {
        x *= val;
        y *= val;
    }

    public Vec GetScaled (double val) {
       return new Vec(x * val, y * val);
    }


    public void Translate (double x_, double y_) {
        x += x_;
        y += y_;
    }

    public void Translate (Vec v) {
        x += v.x;
        y += v.y;
    }

    public Vec GetTranslated (double x_, double y_) {
        return new Vec(x + x_, y + y_);
    }


    public Vec GetTranslated (Vec v) {
        return new Vec(x + v.x, y + v.y);
    }

    public Vec GetAdded(Vec v) {
        return new Vec(x + v.x, y + v.y);
    }

    public double x;
    public double y;
}
