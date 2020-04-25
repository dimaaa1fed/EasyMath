package com.example.easymath;
import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Rect;
import android.os.Bundle;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;

import java.util.ArrayList;


public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.draw_view = new DrawView(this);
        this.easy_ui = new EasyUI(this.draw_view.expression, this.draw_view.input_expression,
                this.draw_view, this);

        this.draw_view.setOnTouchListener(easy_ui.getHandleTouch());

        setContentView(draw_view);

    }

    class DrawView extends View {

        EasyExpression expression = new EasyExpression();
        EasyExpression input_expression = new EasyExpression();

        Paint fontPaint;
        Paint redPaint;
        String text = "Test width text";
        int fontSize = 100;
        float[] widths;
        float width;

        float center_x;
        float center_y;

        public DrawView(Context context) {
            super(context);

            //
            input_expression.entry_point.SetValue(new EasyValue(0, 0, 0));

            // CREATE EXPRESSION
            EasyToken a = expression.entry_point.SetValue(new EasyValue(255, 0 ,0));
            /*EasyToken b = a.CreateUnderDivlineToken(a);

            EasyToken c = b.CreateRightToken();
            EasyToken d = c.CreateUnderDivlineToken(c);
            b.CreateUnderDivlineToken(b);
*/
            redPaint = new Paint();
            redPaint.setColor(Color.RED);

            fontPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            fontPaint.setTextSize(fontSize);
            fontPaint.setStyle(Paint.Style.FILL_AND_STROKE);

            // ширина текста
            width = fontPaint.measureText(text);

            // посимвольная ширина
            widths = new float[text.length()];
            fontPaint.getTextWidths(text, widths);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            center_x = canvas.getWidth() / 2;
            center_y = canvas.getHeight() / 2;

            // RENDER TRAVERSAL
            DrawExpression(canvas, expression, easy_ui.GetGlobalTranslate().x, easy_ui.GetGlobalTranslate().y, 1);
            DrawExpression(canvas, input_expression, -200, -600, 1);

            // Draw buttons
            int start_x = easy_ui.screenWidth / 4;
            int start_y = easy_ui.screenHeight / 2 - 30;
            int row = 0, column = 0;
            int bb_w = 100, bb_h = 100, offset = 20;
            for (EasyToken btn: easy_ui.getEasy_buttons()) {
                EasyTokenBox bbox = btn.bbox;
                EasyValue value = btn.value;
                if (value == null) {
                    value = new EasyValue(0, 255, 0);
                }

                int cur_x = start_x + (bb_w + offset) * column;
                if (cur_x > easy_ui.screenWidth / 2 - bb_w)
                {
                    cur_x = start_x;
                    row += 1;
                    column = 0;
                }
                int cur_y = start_y - bb_h * row;
                column += 1;

                DrawTokenRect(btn.getText(), canvas, bbox, value, cur_x + offset, -cur_y, 1, false);
            }
        }

        private void DrawExpression (Canvas canvas, EasyExpression ex, double xoffset, double yoffset, double scale) {
            EasyTraversal it = ex.Iterator();
            while (it.HasNext()) {
                EasyToken token = it.Next();
                EasyTokenBox bbox = token.bbox;
                EasyValue value = token.value;
                if (value == null) {
                    value = new EasyValue(0, 255, 0);
                }
                DrawTokenRect(token.getText(), canvas, bbox, value, xoffset, yoffset, scale, false);
            }

            ArrayList<EasyTokenBox> div_lines = ex.GetDivisionLines();
            for (EasyTokenBox line : div_lines) {
                DrawTokenRect("", canvas, line, new EasyValue(0, 0, 0), xoffset, yoffset, scale, true);
            }
        }

        private void DrawTokenRect(String text, Canvas canvas, EasyTokenBox bbox, EasyValue value, double xoffset, double yoffset, double scale, boolean is_lines) {
            EasyTokenBox screenBox = EasyToken.ToScreenCoord(canvas.getWidth(), canvas.getHeight(), bbox);
            //EasyTokenBox tr = screenBox.GetTransformed(xoffset, yoffset, scale);
            screenBox.Translate(xoffset, yoffset);
            screenBox.Scale(scale);

            Rect myRect = new Rect();
            myRect.set((int)(screenBox.left_bottom.x),
                    (int)(screenBox.left_bottom.y),
                    (int)(screenBox.right_top.x),
                    (int)(screenBox.right_top.y));

            Paint paint = new Paint();
            paint.setColor(Color.rgb(value.r, value.g, value.b));
            if (is_lines) {
                paint.setStyle(Paint.Style.FILL);
            } else {
                paint.setStyle(Paint.Style.STROKE);
            }
            canvas.drawRect(myRect, paint);
            // draw text
            if (text != null && !text.equals(""))
            {
                // get center
                int c_x = myRect.centerX(), c_y = myRect.centerY();
                int box_w = Math.abs(myRect.width()), box_h = Math.abs(myRect.height());
                // tune text

                Rect text_bounds = new Rect();
                fontPaint.getTextBounds(text, 0, text.length(), text_bounds);
                // make less than rect
                while (text_bounds.width() > box_w || text_bounds.height() > box_h) {
                    fontSize -= 1;
                    fontPaint.setTextSize(fontSize);
                    fontPaint.getTextBounds(text, 0, text.length(), text_bounds);
                 }
                // try to make bigger
                while (true) {
                    fontPaint.setTextSize(fontSize + 1);
                    fontPaint.getTextBounds(text, 0, text.length(), text_bounds);
                    if (text_bounds.width() > box_w || text_bounds.height() > box_h) {
                        fontPaint.setTextSize(fontSize);
                        break;
                    }
                    fontSize += 1;
                }

                canvas.drawText(text, c_x - (box_w / 2), c_y + (box_h / 2), fontPaint);
            }
        }

        @Override
        public boolean performClick() {
            return super.performClick();
        }
    }

    private DrawView draw_view;
    private EasyUI easy_ui;
}

