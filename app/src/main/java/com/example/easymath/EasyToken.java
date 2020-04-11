package com.example.easymath;


import java.util.ArrayList;
import java.util.Stack;

public class EasyToken {

    final double scale_factor = 0.5; // for u, d, ru, rd
    final double dist_factor = 1 + 0.1; // for u, d, ru, rd
    final double div_dist_factor = 1.2;

    // indexes, or signs
    // small size boxes
    public EasyToken up = null;
    public EasyToken down = null;
    public EasyToken r_up = null;
    public EasyToken r_down = null;

    // same size as me
    public EasyToken right = null;
    public EasyToken under_divline = null;
    public EasyToken under_divline2 = null;  // end numerator has this ref (ref to end of denomerator)

    public EasyToken owner = null;  //null only if it is entry point
    public EasyToken owner2 = null; // end denumerator has this ref (ref to end of numerator)

    // bound_box in some coordinate system: render transform it
    public EasyTokenBox bbox;
    public EasyValue value;

    public ArrayList<EasyTokenBox> div_lines; // refs to EasyExpression

    public EasyToken() {
    }

    public EasyToken(EasyTokenBox bbox) {
        this(bbox, null);
    }

    public EasyToken(EasyTokenBox bbox_, EasyValue val) {
        if (bbox_ != null) {
            bbox = new EasyTokenBox(bbox_);
        }
        if (val != null) {
            value = new EasyValue(val);
        }
    }

    public EasyToken SetValue(EasyValue val) {
        value = val;
        return this;
    }

    public EasyToken CreateUpToken() {
        if (up != null) {
            EasyToken tmp = up;
            up = new EasyToken(null);
            up.owner = this;
            tmp.owner = up;
            tmp.UpdateThenSameCreated();
        } else {
            up = new EasyToken(null);
            up.owner = this;
        }
        FillDivlineRef();
        CreateBBoxSkeleton();  //TODO: add up to calc rightest
        return up;
    }

    public EasyToken CreateRUpToken() {
        if (r_up != null) {
            EasyToken tmp = r_up;
            r_up = new EasyToken(null);
            r_up.owner = this;
            tmp.owner = r_up;
            tmp.UpdateThenSameCreated();
        } else {
            r_up = new EasyToken(null);
            r_up.owner = this;
        }
        FillDivlineRef();
        CreateBBoxSkeleton();
        return r_up;
    }

    public EasyToken CreateRDownToken() {
        if (r_down != null) {
            EasyToken tmp = r_down;
            r_down = new EasyToken(null);
            r_down.owner = this;
            tmp.owner = r_down;
            tmp.UpdateThenSameCreated();
        } else {
            r_down = new EasyToken(null);
            r_down.owner = this;
        }
        FillDivlineRef();
        CreateBBoxSkeleton();
        return r_down;
    }

    public EasyToken CreateDownToken() {
        if (down != null) {
            EasyToken tmp = down;
            down = new EasyToken(null);
            down.owner = this;
            tmp.owner = down;
            tmp.UpdateThenSameCreated();
            UpdateBboxNeightboors();
        } else {
            down = new EasyToken(null);
            down.owner = this;
        }
        FillDivlineRef();
        CreateBBoxSkeleton();
        return down;
    }

    public EasyToken CreateRightToken() {
        if (right != null) {
            EasyToken tmp = right;
            right = new EasyToken(null);
            right.owner = this;
            tmp.owner = right;
            tmp.UpdateThenSameCreated();
        } else {
            right = new EasyToken(null);
            right.owner = this;
        }

        // update end denumerator/numerator ref
        if (owner2 != null) {
            owner2.under_divline2 = right;
            right.owner2 = owner2;
            owner2 = null;
        }
        if (under_divline2 != null) {
            under_divline2.owner2 = right;
            right.under_divline2 = under_divline2;
            under_divline2 = null;
        }

        FillDivlineRef();
        CreateBBoxSkeleton();
        return right;
    }

    // Call this on start_numerator_token
    public EasyToken CreateUnderDivlineToken(EasyToken end_numerator_token) {
        // Just for debug check, that end numerator token is righter
        try {
            EasyToken cur = this;
            while (cur != end_numerator_token) {
                cur = cur.right;
            }
        } catch (NullPointerException e) {
            assert (false) : "Bad end numerator token in CreateUnderDivlineToken ";
        }
        // End Debug check

        under_divline = new EasyToken(null);
        end_numerator_token.under_divline2 = under_divline;
        under_divline.owner = this;
        under_divline.owner2 = end_numerator_token;

        FillDivlineRef();
        CreateBBoxSkeleton();
        return under_divline;
    }

    private EasyTokenBox CreateUnderDivlineBBox() {
        return new EasyTokenBox(
                bbox.Center(), bbox.Width(), bbox.Height()
        );
    }

    private EasyTokenBox CreateUpBBox() {
        double w = bbox.Width() * scale_factor;
        double h = bbox.Height() * scale_factor;

        return new EasyTokenBox(
                bbox.Center().GetTranslated(0, bbox.Height() / 2 * dist_factor + h / 2),
                w,
                h
        );
    }

    private EasyTokenBox CreateRUpBBox() {
        double w = bbox.Width() * scale_factor;
        double h = bbox.Height() * scale_factor;

        double factor = 0.25;

        return new EasyTokenBox(
                bbox.Center().GetTranslated(bbox.Width() / 2 * dist_factor + w / 2, bbox.Height() / 4 * factor + h / 2),
                w,
                h
        );
    }

    private EasyTokenBox CreateRDownBBox() {
        double w = bbox.Width() * scale_factor;
        double h = bbox.Height() * scale_factor;

        double factor = 0.25;

        return new EasyTokenBox(
                bbox.Center().GetTranslated(bbox.Width() / 2 * dist_factor + w / 2, -bbox.Height() / 4 * factor - h / 2),
                w,
                h
        );
    }

    private EasyTokenBox CreateDownBBox() {
        double w = bbox.Width() * scale_factor;
        double h = bbox.Height() * scale_factor;

        return new EasyTokenBox(
                bbox.Center().GetTranslated(0, -bbox.Height() / 2 * dist_factor - h / 2),
                w,
                h
        );
    }

    private EasyTokenBox CreateRightBBox() {
        EasyToken rightest = this.GetRightestSmall();
        double rightest_x = rightest.bbox.right_top.x;
        double offset = rightest_x - bbox.right_top.x;

        double w = bbox.Width() * 1;
        double h = bbox.Height() * 1;

        EasyTokenBox new_bbox;

        double offset_factor = 1.1;
        new_bbox = new EasyTokenBox(
                bbox.Center().GetTranslated(bbox.Width() * offset_factor + offset, 0),
                w,
                h
        );

        return new_bbox;
    }

    public void UpdateThenSameCreated() {
        // May be make other politic???
        // (Ex. then add r_up, prev r_up become r_up of current r_up)
        owner.right = this;
    }

    public void UpdateBboxNeightboors() {
        EasyTraversal it = new EasyTraversal(this);
        it.SetIgnore(EasyOwnerType.UNDER_DIVLINE);
        it.Next(); // skip updated root
        while (it.HasNext()) {
            EasyToken token = it.Next();
            EasyOwnerType type = WhoAmI(token);
            token.bbox = token.owner.CreateBBox(type);
        }

        if (this.under_divline != null) {
            this.under_divline.bbox = this.CreateUnderDivlineBBox();
            this.under_divline.UpdateBboxNeightboors();
            this.UpdateDivision();
        }
    }

    public void UpdateBboxIndxes() {
        EasyTraversal it = new EasyTraversal(this);
        it.SetIgnore(EasyOwnerType.UNDER_DIVLINE);
        it.SetIgnoreFirst(EasyOwnerType.RIGHT);
        it.Next(); // skip updated root
        while (it.HasNext()) {
            EasyToken token = it.Next();
            EasyOwnerType type = WhoAmI(token);
            token.bbox = token.owner.CreateBBox(type);
        }

        /*if (this.under_divline != null) {
            this.under_divline.bbox = this.CreateUnderDivlineBBox();
            this.under_divline.UpdateBboxNeightboors();
            this.UpdateDivision();
        }*/
    }

    public void UpdateDivision()
    {
        assert under_divline == this;

        EasyToken start_numerator = this;
        EasyToken end_numerator = start_numerator.GetEndOfNumerator();
        EasyToken start_denumerator = this.under_divline;
        EasyToken end_denumerator = start_denumerator.GetEndOfDenumerator();

        double numerator_max_x = end_numerator.GetRightestSmall().bbox.right_top.x;
        double numerator_min_x = start_numerator.bbox.left_bottom.x;
        double numerator_len = numerator_max_x - numerator_min_x;

        double denumerator_max_x = end_denumerator.GetRightestSmall().bbox.right_top.x;
        double denumerator_min_x = start_denumerator.bbox.left_bottom.x;
        double denumerator_len = denumerator_max_x - denumerator_min_x;

        double line_len = Math.max(numerator_len, denumerator_len);

        double yoffset = bbox.Height() * div_dist_factor;

        if (line_len != numerator_len) {
            // Translate non numerator rest
            if (end_numerator.right != null) {
                end_numerator.right.bbox = end_denumerator.CreateRightBBox();
                end_numerator.right.UpdateBboxNeightboors();
            }
        }

        EasyToken cur = end_numerator.right;
        while (cur != null) {
            cur.bbox.Translate(0, -yoffset / 2);
            cur.UpdateBboxIndxes();
            cur = cur.right;
        }

        cur = start_denumerator;
        while (cur != null) {
            cur.bbox.Translate(0, -yoffset);
            cur.UpdateBboxIndxes();
            cur = cur.right;
        }

        double xoffset_n = 0;
        double xoffset_d = 0;

        if (line_len != numerator_len) {
            xoffset_n = (line_len - numerator_len) / 2;
            //TODO: change, then height of de/numerator don't equals to start
            Vec lb = new Vec(start_denumerator.bbox.left_bottom.x, start_denumerator.bbox.right_top.y);
            Vec rt = new Vec(end_denumerator.GetRightestSmall().bbox.right_top.x, start_numerator.bbox.left_bottom.y);

            div_lines.add(new EasyTokenBox(lb, rt));
        } else {
            xoffset_d = (line_len - denumerator_len) / 2;

            //TODO: change, then height of de/numerator don't equals to start
            Vec lb = new Vec(start_numerator.bbox.left_bottom.x, start_denumerator.bbox.right_top.y);
            Vec rt = new Vec(end_numerator.GetRightestSmall().bbox.right_top.x, start_numerator.bbox.left_bottom.y);
            div_lines.add(new EasyTokenBox(lb, rt));
        }

        if (xoffset_n != 0) {
            cur = start_numerator;
            while (cur != end_numerator) {
                cur.bbox.Translate(xoffset_n, 0);
                cur.UpdateBboxIndxes();
                cur = cur.right;
            }
            cur.bbox.Translate(xoffset_n, 0);
            cur.UpdateBboxIndxes();
        }

        if (xoffset_d != 0) {
            cur = start_denumerator;
            while (cur != end_denumerator) {
                cur.bbox.Translate(xoffset_d, 0);
                cur.UpdateBboxIndxes();
                cur = cur.right;
            }
            cur.bbox.Translate(xoffset_d, 0);
            cur.UpdateBboxIndxes();
        }
    }

    public void CreateBBoxSkeleton() {
        EasyToken root = GetRoot();
        root.bbox = CreateRootBbox();
        root.UpdateBboxNeightboors();
    }

    public EasyTokenBox CreateRootBbox(){
        return new EasyTokenBox(new Vec(-1 * 0.7, -1), new Vec(1 * 0.7, 1));
    }


    public EasyTokenBox CreateBBox(EasyOwnerType type) {
        switch (type) {
            case UP:
                return CreateUpBBox();
            case R_UP:
                return CreateRUpBBox();
            case R_DOWN:
                return CreateRDownBBox();
            case DOWN:
                return CreateDownBBox();
            case RIGHT:
                return CreateRightBBox();
            case UNDER_DIVLINE:
                return CreateUnderDivlineBBox();
            default:
                assert (false) ;
        }
        return null;
    }

    public EasyOwnerType WhoAmI (EasyToken token) {
        EasyToken my_owner = token.owner;
        if (my_owner.up == token) {
            return EasyOwnerType.UP;
        } else if (my_owner.r_up == token) {
            return EasyOwnerType.R_UP;
        } else if (my_owner.r_down == token) {
            return EasyOwnerType.R_DOWN;
        } else if (my_owner.down == token) {
            return EasyOwnerType.DOWN;
        } else if (my_owner.right == token) {
            return EasyOwnerType.RIGHT;
        } else if (my_owner.under_divline == token) {
            return EasyOwnerType.UNDER_DIVLINE;
        }
        return EasyOwnerType.INVALID;
    }

    // TODO: up down???
    // Get token, that has got rightest bbox (recursion don't go to right firstly)
    public EasyToken GetRightestSmall () {
        EasyToken rightest = this;
        return _GetRightestSmallInternal(this, rightest);
    }

    public EasyToken _GetRightestSmallInternal (EasyToken token, EasyToken rightest) {
        if (token == null) {
            return rightest;
        }

        if (rightest.bbox.right_top.x < token.bbox.right_top.x) {
            rightest = token;
        }

        rightest = __GetRightestSmallInternal(token.r_up, rightest);
        rightest = __GetRightestSmallInternal(token.r_down, rightest);
        return rightest;
    }

    public EasyToken __GetRightestSmallInternal (EasyToken token, EasyToken rightest) {
        if (token == null) {
            return rightest;
        }

        if (rightest.bbox.right_top.x < token.bbox.right_top.x) {
            rightest = token;
        }

        rightest = __GetRightestSmallInternal(token.r_up, rightest);
        rightest = __GetRightestSmallInternal(token.r_down, rightest);
        rightest = __GetRightestSmallInternal(token.right, rightest);
        return rightest;
    }

    public EasyToken GetEndOfNumerator() {
        assert (under_divline != null);

        EasyToken end_numerator = this;

        try {
            EasyToken end_denumerator = under_divline.GetEndOfDenumerator();

            while (end_numerator.under_divline2 != end_denumerator) {
                end_numerator = end_numerator.right;
            }
        } catch (NullPointerException e) {
            assert (false) : "Internal error in expression";
        }

        return end_numerator;
    }

    public EasyToken GetEndOfDenumerator() {
        assert (owner.under_divline != null);

        EasyToken end_denumerator = this;

        while (end_denumerator.owner2 == null) {
            end_denumerator = end_denumerator.right;
        }
        return end_denumerator;
    }

    private void FillDivlineRef()
    {
        div_lines = GetRoot().div_lines;
    }

    private EasyToken GetRoot () {
        EasyToken root = this;
        while (root.owner != null) {
            root = root.owner;
        }
        return root;
    }
}

enum EasyOwnerType {
    UP,
    R_UP,
    R_DOWN,
    DOWN,

    RIGHT,
    UNDER_DIVLINE,

    INVALID
}