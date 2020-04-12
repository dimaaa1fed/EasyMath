package com.example.easymath;

import android.content.res.Resources;
import android.media.session.MediaSession;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

public class EasyUI {
    EasyUI(EasyExpression expression, View drawView) {
        revert();
        this.drawView = drawView;
        this.expression = expression;
        this.handleTouch = new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {

                int x = (int) event.getX();
                int y = (int) event.getY();

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        findActiveToken(x, y);

                        Log.i("TAG", "touched down");
                        break;
                    case MotionEvent.ACTION_MOVE:
                        Log.i("TAG", "moving: (" + x + ", " + y + ")");
                        calcNewToken(x, y);
                        break;
                    case MotionEvent.ACTION_UP:
                        revert();
                        v.performClick();
                        Log.i("TAG", "touched up");
                        break;
                }

                return true;
            }
        };
    }

    public boolean findActiveToken(int x, int y) {
        revert();

        int width = Resources.getSystem().getDisplayMetrics().widthPixels;
        int height = Resources.getSystem().getDisplayMetrics().heightPixels;

        // RENDER TRAVERSAL
        Vec point = new Vec(x, y);
        EasyTraversal it = expression.Iterator();
        while (it.HasNext()) {
            EasyToken token = it.Next();
            EasyTokenBox bbox = token.bbox;
            EasyValue value = token.value;
            if (value == null) {
                value = new EasyValue(0, 255, 0);
            }
            EasyTokenBox cur_box = EasyToken.ToScreenCoord(width, height, bbox);
            if (cur_box.IsInside(point)) {
                this.active_token = token;
                this.start_touch = point;
                return true;
            }
        }
        return false;
    }

    public View.OnTouchListener getHandleTouch() {
        return handleTouch;
    }

    public EasyExpression getExpression() {
        return expression;
    }

    public void setExpression(EasyExpression expression) {
        this.expression = expression;
    }

    public void revert() {
        this.active_token = null;
        this.start_touch = null;
    }

    public void calcNewToken(int x, int y) {
        if (active_token == null || start_touch == null) {
            revert();
            return;
        }
        Vec cur_point = new Vec(x, y);
        Vec diff = cur_point.GetAdded(start_touch.GetScaled(-1));
        if (diff.GetLength() < minActionLength) {
            return;
        }

        double angle = cur_point.GetAngleToX(start_touch, true);
        if (angle >= 60 && angle <= 120) {
            // add up
            active_token.CreateUpToken();
        }
        else if (angle >= 30 && angle <= 60) {
            // add right upp
            active_token.CreateRUpToken();
        }
        else if (angle >= -30 && angle <= 30)
        {
            // add right
            active_token.CreateRightToken();
        }
        else if (angle >= -60 && angle <= -30) {
            // add right down
            active_token.CreateRDownToken();
        }
        else if (angle >= -120 && angle <= -60) {
            // add down box
            active_token.CreateDownToken();
        }
        else
        {
            // do not have move
            Log.i("TAG", "Wrong angle");
            return;
        }


        revert();
        this.drawView.invalidate();
    }

    private EasyExpression expression;
    private View.OnTouchListener handleTouch;
    private EasyToken active_token;
    private Vec start_touch;
    private double minActionLength = 10;
    private View drawView;
}
