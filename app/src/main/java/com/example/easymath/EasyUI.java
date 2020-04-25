package com.example.easymath;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Handler;
import android.text.InputType;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;

public class EasyUI {
    EasyUI(EasyExpression expression, EasyExpression input_expression, View drawView) {
        revert();
        this.drawView = drawView;

        this.expression = expression;
        this.input_expression = input_expression;

        screenWidth  = Resources.getSystem().getDisplayMetrics().widthPixels;
        screenHeight = Resources.getSystem().getDisplayMetrics().heightPixels - 200; //Does not same as in canvas

        zoomLineStartX = (int)(screenWidth * (1 - zoomLinePercent));

        this.handleTouch = new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {

                int x = (int) event.getX();
                int y = (int) event.getY();

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        findActiveToken(x, y);
                        handler.postDelayed(mLongPressed, android.view.ViewConfiguration.getLongPressTimeout());
                        Log.i("TAG", "touched down");

                        if (x < zoomLineStartX)  {
                            moveOrZoom = true;
                        }

                        break;
                    case MotionEvent.ACTION_MOVE:
                        handler.removeCallbacks(mLongPressed);
                        if (active_token != null) {
                            showExampleToken(x, y);
                            return true;
                        }
                        if (moveOrZoom) {
                            globalTranslate.Translate(-(x - cur_touch.x), -(y - cur_touch.y));
                            cur_touch = new Vec(x, y);
                            Invalidate();
                        } else {
                            if (y - cur_touch.y > 0) {
                                globalZoom -= zoomStep;
                            } else {
                                globalZoom += zoomStep;
                            }
                            Invalidate();
                            Log.i("TAG", new Double(globalZoom).toString());
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                        if (start_touch != null &&
                                start_touch.GetAdded(new Vec(-x, -y)).GetLength() <= 4 &&
                                !goneFlag) {
                            break;

                        }
                        moveOrZoom = false;

                        calcNewToken(x, y);
                        revert();
                        v.performClick();
                        Log.i("TAG", "touched up");
                        break;
                }

                return true;
            }
        };

        // Long tap check
        this.goneFlag = false;
        this.handler = new Handler();
        this.mLongPressed = new Runnable() {
            public void run() {
                goneFlag = true;
                //Code for long click
                addText();
                // lol
                goneFlag = false;
            }
        };
    }

    public void Invalidate() {
        this.drawView.invalidate();
    }

    public boolean findActiveToken(int x, int y) {
        revert();

        Vec point = new Vec(x, y);
        EasyTraversal it = expression.Iterator();
        while (it.HasNext()) {
            EasyToken token = it.Next();
            EasyTokenBox bbox = token.bbox;
            if ( token.value == null) {
                token.value = new EasyValue(0, 255, 0);
            }
            EasyTokenBox cur_box = EasyToken.ToScreenCoord(screenWidth, screenHeight, bbox);
            if (cur_box.IsInside(point.GetTranslated(new Vec(-globalTranslate.x, -globalTranslate.y)))) {
                this.active_token = token;
                this.start_touch = point;
                this.cur_touch = point;
                return true;
            }
        }
        this.start_touch = point;
        this.cur_touch = point;
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
        this.cur_touch = null;
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
            active_token.CreateUpToken();
        }
        else if (angle >= 30 && angle <= 60) {
            active_token.CreateRUpToken();
        }
        else if (angle >= 330 || angle <= 30) {
            active_token.CreateRightToken();
        }
        else if (angle >= 300 && angle <= 330) {
            active_token.CreateRDownToken();
        }
        else if (angle <= 300 && angle >= 240) {
            if (diff.GetLength() < 200) {
                active_token.CreateDownToken();
            } else {
                active_token.CreateUnderDivlineToken(active_token);
            }
        }
        else
        {
            // do not have move
            return;
        }
        revert();
        this.drawView.invalidate();
    }

    public void showExampleToken(int x, int y) {
        Vec cur_point = new Vec(x, y);
        Vec diff = cur_point.GetAdded(start_touch.GetScaled(-1));
        input_expression.Reset();
        if (diff.GetLength() < minActionLength) {
            return;
        }


        double angle = cur_point.GetAngleToX(start_touch, true);
        if (angle >= 60 && angle <= 120) {
            input_expression.entry_point.CreateUpToken();
        }
        else if (angle >= 30 && angle <= 60) {
            input_expression.entry_point.CreateRUpToken();
        }
        else if (angle >= 330 || angle <= 30) {
            input_expression.entry_point.CreateRightToken();
        }
        else if (angle >= 300 && angle <= 330) {
            input_expression.entry_point.CreateRDownToken();
        }
        else if (angle <= 300 && angle >= 240) {
            if (diff.GetLength() < 200) {
                input_expression.entry_point.CreateDownToken();
            } else {
                input_expression.entry_point.CreateUnderDivlineToken(input_expression.entry_point);
            }
        }
        else
        {
            // do not have move
            return;
        }
        //revert();
        this.drawView.invalidate();
    }

    public void addText() {
        if (active_token == null)
            return;

        AlertDialog.Builder builder = new AlertDialog.Builder(this.drawView.getContext());
        builder.setTitle("Put expression");

        // Set up the input
        final EditText input = new EditText(this.drawView.getContext());
        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mText = input.getText().toString();
                TokenAddText(mText);
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();

    }

    private void TokenAddText(String Text) {
        if (Text.equals("") || active_token == null)
            return;
        active_token.setText(String.valueOf(Text.charAt(0)));

        EasyToken cur_token = active_token;
        for (int i = 1; i < Text.length(); i++) {
            char symb = Text.charAt(i);
            cur_token = cur_token.CreateRightToken();
            cur_token.setText(String.valueOf(symb));
        }

        revert();
        this.drawView.invalidate();
    }

    public Vec GetGlobalTranslate() {
        return new Vec(globalTranslate);
    }

    public float GetGlobalZoom() {
        return globalZoom;
    }

    private EasyExpression input_expression;
    private EasyExpression expression;
    private View.OnTouchListener handleTouch;
    private EasyToken active_token;

    private Vec start_touch;
    private Vec cur_touch;

    private double minActionLength = 100;
    private View drawView;
    //Declare this flag globally
    private boolean goneFlag;

    //Put this into the class
    private final Handler handler;
    private Runnable mLongPressed;
    private String mText = "";

    private Vec globalTranslate = new Vec(0, 0);
    private float globalZoom = 1;

    int screenWidth;
    int screenHeight;

    final double zoomLinePercent = 0.2;
    private int  zoomLineStartX;
    final double zoomStep = 0.02;

    private boolean moveOrZoom = false;
}
