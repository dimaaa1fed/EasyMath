package com.example.easymath;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Rect;
import android.os.Handler;
import android.text.InputType;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;

import java.io.OutputStreamWriter;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;

import static android.os.ParcelFileDescriptor.MODE_APPEND;

public class EasyUI {
    EasyUI(final EasyExpression expression, final EasyExpression input_expression, View drawView, Activity app) {
        this.app = app;
        revert();
        this.drawView = drawView;

        this.expression = expression;
        this.input_expression = input_expression;

        screenWidth = Resources.getSystem().getDisplayMetrics().widthPixels;
        screenHeight = Resources.getSystem().getDisplayMetrics().heightPixels - 200; //Does not same as in canvas

        zoomLineStartX = (int) (screenWidth * (1 - zoomLinePercent));

        this.handleTouch = new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {

                int x = (int) event.getX();
                int y = (int) event.getY();

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        findButton(x, y);
                        findActiveToken(x, y);
                        handler.postDelayed(mLongPressed, android.view.ViewConfiguration.getLongPressTimeout());
                        Log.i("TAG", "touched down");

                        if (x < zoomLineStartX) {
                            moveOrZoom = true;
                        }

                        break;
                    case MotionEvent.ACTION_MOVE:
                        handler.removeCallbacks(mLongPressed);
                        if (active_token != null) {
                            showExampleToken(x, y);
                            return true;
                        }
                        if (active_btn != null) {
                            findActiveToken(x, y);
                            if (active_token != null) {
                                active_token.CreateTokenGroup(active_btn.getText());
                                active_btn = null;
                            }
                        }
                        if (moveOrZoom) {
                            globalTranslate.Translate(-(x - cur_touch.x), -(y - cur_touch.y));
                            cur_touch = new Vec(x, y);
                            Invalidate();
                        } else {
                            if (y - cur_touch.y > 0) {
                                if (globalZoom - zoomStep > 0.1) {
                                    globalZoom -= zoomStep;
                                }
                                expression.UpdateScale(globalZoom);
                            } else {
                                globalZoom += zoomStep;
                                expression.UpdateScale(globalZoom);
                            }
                            Invalidate();
                            Log.i("TAG", new Double(globalZoom).toString());
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                        if (!firstTouch) {
                            firstTouch = true;
                            time = System.currentTimeMillis();
                        } else {
                            if (System.currentTimeMillis() - time <
                                    android.view.ViewConfiguration.getDoubleTapTimeout() * 5
                                    && y > screenHeight * 3 / 4) {
                                latex_text = expression.ToLatex();
                                savetext(latex_text);
                            }
                            firstTouch = false;

                        }


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

        // add funcs
        easy_buttons = new ArrayList<>();
        for (String text : Arrays.asList("sin", "cos", "tn", "ctn", "ln")) {
            EasyTokenBox bbox = new EasyTokenBox(new Vec(-1, -1), new Vec(1, 1));
            EasyToken token = new EasyToken(bbox, null, text);
            easy_buttons.add(token);
        }
    }

    public ArrayList<EasyToken> getEasy_buttons() {
        return easy_buttons;
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
            if (token.value == null) {
                token.value = new EasyValue(0, 255, 0);
            }
            EasyTokenBox cur_box = EasyToken.ToScreenCoord(screenWidth, screenHeight, bbox);
            cur_box.Translate(globalTranslate.x, globalTranslate.y);
            //cur_box.Scale(globalZoom);

            if (cur_box.IsInside(point)) {
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

    public void findButton(int x, int y) {
        Vec point = new Vec(x, y);
        // Draw buttons
        int start_x = screenWidth / 4;
        int start_y = screenHeight / 2 - 30;
        int row = 0, column = 0;
        int bb_w = 100, bb_h = 100, offset = 20;
        for (EasyToken btn: getEasy_buttons()) {
            EasyTokenBox bbox = btn.bbox;
            EasyValue value = btn.value;
            if (value == null) {
                value = new EasyValue(0, 255, 0);
            }

            int cur_x = start_x + (bb_w + offset) * column;
            if (cur_x > screenWidth / 2 - bb_w)
            {
                cur_x = start_x;
                row += 1;
                column = 0;
            }
            int cur_y = start_y - bb_h * row;
            column += 1;
            EasyTokenBox cur_box = EasyToken.ToScreenCoord(screenWidth, screenHeight, bbox);
            cur_box.Translate(cur_x, -cur_y);

            if (cur_box.IsInside(point)) {
                active_btn = btn;
                return;
            }
        }
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
        } else if (angle >= 30 && angle <= 60) {
            active_token.CreateRUpToken();
        } else if (angle >= 330 || angle <= 30) {
            active_token.CreateRightToken();
        } else if (angle >= 300 && angle <= 330) {
            active_token.CreateRDownToken();
        } else if (angle <= 300 && angle >= 240) {
            if (diff.GetLength() < 200) {
                active_token.CreateDownToken();
            } else {
                active_token.CreateUnderDivlineToken(active_token);
            }
        } else {
            //active_token.CreateTokenGroup("cos");
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
        } else if (angle >= 30 && angle <= 60) {
            input_expression.entry_point.CreateRUpToken();
        } else if (angle >= 330 || angle <= 30) {
            input_expression.entry_point.CreateRightToken();
        } else if (angle >= 300 && angle <= 330) {
            input_expression.entry_point.CreateRDownToken();
        } else if (angle <= 300 && angle >= 240) {
            if (diff.GetLength() < 200) {
                input_expression.entry_point.CreateDownToken();
            } else {
                input_expression.entry_point.CreateUnderDivlineToken(input_expression.entry_point);
            }
        } else {
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
        String text_copy = Text.replace(" ", "");
        if (text_copy.equals("") || active_token == null)
            return;

        active_token.setText(String.valueOf(text_copy.charAt(0)));

        EasyToken cur_token = active_token;
        for (int i = 1; i < text_copy.length(); i++) {
            char symb = text_copy.charAt(i);
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

    public void savetext(String textToSave) {
        String msg = "";

        try {

            OutputStreamWriter out = new OutputStreamWriter(app.openFileOutput("TextFile", MODE_APPEND));
            out.write(textToSave);
            out.write('\n');
            out.close();

            msg = "Latex text successfully added";
        } catch (Throwable t) {
            msg = "Error while saving latex text";
        }

        msg += ": '" + textToSave + "'";

        new AlertDialog.Builder(this.drawView.getContext())
                .setTitle("Latex")
                .setMessage(msg)

                // Specifying a listener allows you to take an action before dismissing the dialog.
                // The dialog is automatically dismissed when a dialog button is clicked.
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // Continue with delete operation
                    }
                })
                // A null listener allows the button to dismiss the dialog and take no further action.
                .setNegativeButton(android.R.string.no, null)
                .setIcon(android.R.drawable.ic_dialog_info)
                .show();
    }

    private EasyExpression input_expression;
    private EasyExpression expression;
    private View.OnTouchListener handleTouch;
    private EasyToken active_token;
    private EasyToken active_btn;

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
    private boolean firstTouch = false;
    private long time;
    private String latex_text = "";

    private Vec globalTranslate = new Vec(0, 0);
    private float globalZoom = 1;

    int screenWidth;
    int screenHeight;

    final double zoomLinePercent = 0.2;
    private int zoomLineStartX;
    final double zoomStep = 0.02;

    private boolean moveOrZoom = false;
    private Activity app;
    private ArrayList<EasyToken> easy_buttons;

}
