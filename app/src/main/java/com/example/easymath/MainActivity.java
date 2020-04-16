package com.example.easymath;
import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Rect;
import android.os.Bundle;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;


public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.draw_view = new DrawView(this);
        this.easy_ui = new EasyUI(this.draw_view.expression, this.draw_view.input_expression, this.draw_view);

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
            fontPaint.setStyle(Paint.Style.STROKE);

            // ширина текста
            width = fontPaint.measureText(text);

            // посимвольная ширина
            widths = new float[text.length()];
            fontPaint.getTextWidths(text, widths);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            /*canvas.drawARGB(80, 102, 204, 255);

            canvas.translate(50, 250);

            // вывод текста
            canvas.drawText(text, 0, 0, fontPaint);

            // линия шириной в текст
            canvas.drawLine(0, 0, width, 0, fontPaint);

            // посимвольные красные точки
            canvas.drawCircle(0, 0, 3, redPaint);
            for (float w : widths) {
                canvas.translate(w, 0);
                canvas.drawCircle(0, 0, 3, redPaint);
            }*/



            center_x = canvas.getWidth() / 2;
            center_y = canvas.getHeight() / 2;

            // RENDER TRAVERSAL
            DrawExpression(canvas, expression, 0, 0);
            DrawExpression(canvas, input_expression, -200, -600);
        }

        private void DrawExpression (Canvas canvas, EasyExpression ex, double xoffset, double yoffset) {
            EasyTraversal it = ex.Iterator();
            while (it.HasNext()) {
                EasyToken token = it.Next();
                EasyTokenBox bbox = token.bbox;
                EasyValue value = token.value;
                if (value == null) {
                    value = new EasyValue(0, 255, 0);
                }
                DrawTokenRect(canvas, bbox, value, xoffset, yoffset);
            }

            ArrayList<EasyTokenBox> div_lines = ex.GetDivisionLines();
            for (EasyTokenBox line : div_lines) {
                DrawTokenRect(canvas, line, new EasyValue(0, 0, 0), xoffset, yoffset);
            }
        }

        private void DrawTokenRect(Canvas canvas, EasyTokenBox bbox, EasyValue value, double xoffset, double yoffset) {
            EasyTokenBox screenBox = EasyToken.ToScreenCoord(canvas.getWidth(), canvas.getHeight(), bbox);
            screenBox.Translate(xoffset, yoffset);
            Rect myRect = new Rect();
            myRect.set((int)screenBox.left_bottom.x, (int)screenBox.left_bottom.y, (int)screenBox.right_top.x, (int)screenBox.right_top.y);
            Paint paint = new Paint();
            paint.setColor(Color.rgb(value.r, value.g, value.b));
            paint.setStyle(Paint.Style.FILL);
            canvas.drawRect(myRect, paint);
        }

        @Override
        public boolean performClick() {
            return super.performClick();
        }
    }

    private DrawView draw_view;
    private EasyUI easy_ui;
}

