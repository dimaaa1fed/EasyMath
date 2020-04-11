package com.example.easymath;

import android.widget.ArrayAdapter;

import java.util.ArrayList;

public class EasyExpression {
    public EasyExpression(){
        entry_point = new EasyToken();
        entry_point.div_lines = div_lines;
    }

    public EasyToken GetEntryTokenByClick (double x, double y) {
        return new EasyToken();
    }

    public EasyTraversal Iterator (EasyToken token) {
        return new EasyTraversal(token);
    }

    public EasyTraversal Iterator () {
        return new EasyTraversal(entry_point);
    }

    public ArrayList<EasyTokenBox> GetDivisionLines () {
        ArrayList<EasyTokenBox> res = new ArrayList<>();

        for (EasyTokenBox line : div_lines) {
            res.add(new EasyTokenBox(line.Center(), line.Width(), div_lines_height));
        }

        return res;
    };

    public double div_lines_height = 0.1;
    public ArrayList<EasyTokenBox> div_lines = new ArrayList<>();
    public EasyToken entry_point;  // First token in expression
}
