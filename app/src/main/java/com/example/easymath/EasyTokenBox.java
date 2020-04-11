package com.example.easymath;

public class EasyTokenBox {
    public EasyTokenBox(Vec left_bottom_, Vec right_top_) {
        left_bottom = left_bottom_;
        right_top = right_top_;
    }

    public EasyTokenBox(Vec center, double w, double h) {
        left_bottom = new Vec(center.x - w / 2, center.y - h / 2);
        right_top = new Vec(center.x + w / 2, center.y + h / 2);
    }

    public EasyTokenBox(EasyTokenBox box) {
        left_bottom = new Vec(box.left_bottom);
        right_top = new Vec(box.right_top);
    }

    public void Scale (double val) {
        left_bottom.Scale(val);
        right_top.Scale(val);
    }

    public void InverseY () {
        left_bottom.y *= -1;
        right_top.y *= -1;
    }


    public void Translate (double x, double y) {
        left_bottom.Translate(x, y);
        right_top.Translate(x, y);
    }

    public void Translate (Vec v) {
        left_bottom.Translate(v);
        right_top.Translate(v);
    }

    public double Width() {
        return right_top.x - left_bottom.x;
    }

    public double Height()  {
        return right_top.y - left_bottom.y;
    }

    public Vec Center() {
        return left_bottom.GetAdded(right_top).GetScaled(0.5);
    }

    public Vec LeftUp() {
        return left_bottom.GetAdded(new Vec(0, Height()));
    }

    public Vec RightBottom() {
        return right_top.GetAdded(new Vec(0, -Height()));
    }

    public Vec left_bottom;
    public Vec right_top;
}
