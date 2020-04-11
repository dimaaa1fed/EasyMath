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
        setContentView(new DrawView(this));
    }

    class DrawView extends View {

        EasyExpression expression = new EasyExpression();

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

            // CREATE EXPRESSION
            expression.entry_point.SetValue(new EasyValue(255, 0 ,0));
            expression.entry_point.CreateUpToken();
            EasyToken r_up = expression.entry_point.CreateRUpToken();
            EasyToken r_down = expression.entry_point.CreateRDownToken();
            r_down.CreateRightToken();
            r_up.CreateRUpToken();
            EasyToken end_numerator = expression.entry_point.CreateRightToken().SetValue(new EasyValue(0, 0, 255)).CreateRDownToken();
           // expression.entry_point.CreateRDownToken().CreateRDownToken();
           // expression.entry_point.CreateRightToken().CreateRightToken();
            EasyToken start_denumerator = expression.entry_point.CreateUnderDivlineToken(expression.entry_point).SetValue(new EasyValue(0, 0, 0));
            start_denumerator.CreateRightToken().SetValue(new EasyValue(50, 50, 50));
            start_denumerator.CreateRightToken();
            start_denumerator.CreateRDownToken().CreateRDownToken().CreateRDownToken();


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
            EasyTraversal it = expression.Iterator();
            while (it.HasNext()) {
                EasyToken token = it.Next();
                EasyTokenBox bbox = token.bbox;
                EasyValue value = token.value;
                if (value == null) {
                    value = new EasyValue(0, 255, 0);
                }
                DrawTokenRect(canvas, bbox, value);
            }

            ArrayList<EasyTokenBox> div_lines = expression.GetDivisionLines();
            for (EasyTokenBox line : div_lines) {
                DrawTokenRect(canvas, line, new EasyValue(0, 0, 0));
            }
            //expression.entry_point.SetValue(new EasyValue(1, 0 ,0));
        }

        private void DrawTokenRect(Canvas canvas, EasyTokenBox bbox, EasyValue value) {
            EasyTokenBox screenBox = ToScreenCoord(canvas, bbox);

            Rect myRect = new Rect();
            myRect.set((int)screenBox.left_bottom.x, (int)screenBox.right_top.y, (int)screenBox.right_top.x, (int)screenBox.left_bottom.y);
            Paint paint = new Paint();
            paint.setColor(Color.rgb(value.r, value.g, value.b));
            paint.setStyle(Paint.Style.FILL);
            canvas.drawRect(myRect, paint);
        }

        private EasyTokenBox ToScreenCoord (Canvas canvas, EasyTokenBox bbox) {
            EasyTokenBox tr_box = new EasyTokenBox(new Vec(bbox.left_bottom), new Vec(bbox.right_top));
            tr_box.Scale(50);
            tr_box.InverseY();
            tr_box.Translate(canvas.getWidth() / 2, canvas.getHeight() / 2);
            return tr_box;
        }
    }
}
