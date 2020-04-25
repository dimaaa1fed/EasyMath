package com.example.easymath;

import java.util.ArrayList;
import java.util.Stack;
import static java.lang.Math.abs;

public class EasyToken {
    final double scale_factor = 0.5;    // for u, d, ru, rd
    final double dist_factor = 1 + 0.1; // for u, d, ru, rd
    final double div_dist_factor = 1.2;

    public double scale = 1;

    static public int group_global_id = 0;
    public int token_group_id = -1;

    // indexes, or signs
    // small size boxes
    public EasyToken up = null;
    public EasyToken down = null;
    public EasyToken r_up = null;
    public EasyToken r_down = null;

    // same size as me
    public EasyToken right = null;

    public ArrayList<EasyToken>under_divline = new ArrayList<>();
    public ArrayList<EasyToken>under_divline2 = new ArrayList<>();  // end numerator has this ref (ref to end of denomerator)

    public EasyToken owner = null;  //null only if it is entry point
    public EasyToken owner2 = null; // end denumerator has this ref (ref to end of numerator)

    // bound_box in some coordinate system: render transform it
    public EasyTokenBox bbox;
    public EasyValue value;
    private String text = "";

    public ArrayList<EasyTokenBox> div_lines; // refs to EasyExpression
    public EasyTokenBox my_div_line; // start denumerator has it

    boolean is_single_translated = false;

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

    public void DeleteToken() {
        // delete full group
        if (token_group_id != -1) {
            EasyToken start = GetStartOfGroup();
            EasyToken end = GetEndOfGroup();
            Remove(start, end);
            return;
        }

        // delete if one in denumerator
        if (WhoAmI(this) == EasyOwnerType.UNDER_DIVLINE && this.right == null) {
            this.setText("");
            return;
        }

        // delete if one in numerator
        if (this.under_divline.size() > 0 &&
                this.under_divline2.size() == this.under_divline.size()) {
            if (this.text.equals("")) {
                this.setText("");
            } else {
                Remove(this, this);
            }
            return;
        }

        // delete first in numerator if has right
        if (this.under_divline.size() > 0) {
            ArrayList<EasyToken> to_del = new ArrayList<>();
            for (EasyToken ud : this.under_divline) {
                if (this.under_divline2.contains(ud)) {
                    to_del.add(ud);
                }
            }
            for (EasyToken del : to_del) {
                under_divline.remove(del);
            }

            this.right.under_divline.addAll(this.under_divline);

            Remove(this, this);
            return;
        }

        if (this.under_divline2.size() > 0) {
            this.owner.under_divline2.addAll(this.under_divline2);
            Remove(this, this);
            return;
        }

        Remove(this, this);
    }

    public void Remove (EasyToken from, EasyToken to)
    {
        if (from.owner != null) {
            from.owner.right = to.right;
        }
        if (to.right != null) {
            to.right.owner = from.owner;
        }
        CreateBBoxSkeleton();
    }

    public EasyToken CreateTokenGroup(String name) {
        ArrayList<EasyToken> token_group = new ArrayList<>();

        // Create right tokens for groupe : name( )
        EasyToken close_br = CreateRightToken().setText(")");
        EasyToken empty = CreateRightToken();
        EasyToken open_br = CreateRightToken().setText("(");

        for (int i = name.length() - 1; i >= 0; i--) {
            if (i == 0) {
                if (text.equals("")) {
                    token_group.add(this.setText(String.valueOf(name.charAt(i))));
                    break;
                }
            }

            token_group.add(CreateRightToken().setText(String.valueOf(name.charAt(i))));
        }

        // set id
        close_br.token_group_id = group_global_id;
        open_br.token_group_id = group_global_id;
        for (EasyToken token : token_group) {
            token.token_group_id = group_global_id;
        }

        //empty.entry_of_group = true;
        group_global_id++;
        return empty;
    }

    // call: on group name or brackets
    public EasyToken GetEndOfGroup() {
        if (text.equals(")")) {
            return this;
        }

        int group_id = token_group_id;
        EasyToken cur = this;

        // go to entry
        while (cur.right != null && cur.right.token_group_id == group_id) {
            cur = cur.right;
        }

        // skip entry
        while (cur.right != null && cur.right.token_group_id != group_id) {
            cur = cur.right;
        }

        return cur.right;
    }

    public EasyToken GetStartOfGroup() {
        EasyToken cur = this;
        int group_id = token_group_id;

        if (text.equals(")")) {
            while (cur.owner.token_group_id != group_id) {
                cur = cur.owner;
            }
        }

        while (cur.owner != null && cur.owner.token_group_id == group_id) {
            cur = cur.owner;
        }
        return cur;
    }

    // call: start group token
    public EasyToken GetEmptyInGroup() {
        EasyToken cur = this;
        int group_id = cur.token_group_id;
        while (cur.token_group_id == group_id) {
            cur = cur.right;
        }
        return cur;
    }

    public EasyToken GetEmptyToken() {
        if (token_group_id == -1) {
            return this;
        }
        EasyToken start = GetStartOfGroup();
        return start.GetEmptyInGroup().GetEmptyToken();
    }

    public EasyToken CreateUpToken() {
        if (token_group_id != -1 && !text.equals(")")) {
            EasyToken token = GetEndOfGroup();
            return token.CreateRUpToken();
        }

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
        up.FillDivlineRef();
        CreateBBoxSkeleton();  //TODO: add up to calc rightest
        return up;
    }

    public EasyToken CreateRUpToken() {
        if (token_group_id != -1 && !text.equals(")")) {
            EasyToken token = GetEndOfGroup();
            return token.CreateRUpToken();
        }

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
        r_up.FillDivlineRef();
        CreateBBoxSkeleton();
        return r_up;
    }

    public EasyToken CreateRDownToken() {
        if (token_group_id != -1 && !text.equals(")")) {
            EasyToken token = GetEndOfGroup();
            return token.CreateRUpToken();
        }

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
        r_down.FillDivlineRef();
        CreateBBoxSkeleton();
        return r_down;
    }

    public EasyToken CreateDownToken() {
        if (token_group_id != -1 && !text.equals(")")) {
            EasyToken token = GetEndOfGroup();
            return token.CreateRUpToken();
        }

        if (down != null) {
            EasyToken tmp = down;
            down = new EasyToken(null);
            down.owner = this;
            tmp.owner = down;
            tmp.UpdateThenSameCreated();
        } else {
            down = new EasyToken(null);
            down.owner = this;
        }
        down.FillDivlineRef();
        CreateBBoxSkeleton();
        return down;
    }


    public EasyToken CreateRightToken() {
        if (token_group_id != -1 && !text.equals(")")) {
            EasyToken token = GetEndOfGroup();
            return token.CreateRUpToken();
        }

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
            owner2.under_divline2.set(0, right);
            right.owner2 = owner2;
            owner2 = null;
        }
        if (under_divline2.size() != 0) {
            for (EasyToken d_end : under_divline2) {
                d_end.owner2 = right;
            }
            right.under_divline2 = under_divline2;
            under_divline2 = new ArrayList<>();
        }

        right.FillDivlineRef();
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

        //TODO: add correct process then end numerator token is righter
        if (token_group_id != -1 &&
                !((owner == null || owner.token_group_id != token_group_id)  // this is start of group
                        && (end_numerator_token == GetEndOfGroup()))) {
            end_numerator_token = GetEndOfGroup();
            return GetStartOfGroup().CreateUnderDivlineToken(end_numerator_token);
        }

        under_divline.add(0, new EasyToken(null));
        end_numerator_token.under_divline2.add(0, under_divline.get(0));
        under_divline.get(0).owner = this;
        under_divline.get(0).owner2 = end_numerator_token;

        under_divline.get(0).FillDivlineRef();
        CreateBBoxSkeleton();
        return under_divline.get(0);
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
        while (it.HasNext()) {
            EasyToken token = it.Next();
            token.is_single_translated = false;
            EasyOwnerType type = WhoAmI(token);
            if (token.owner == null) {
                token.bbox = token.CreateBBox(type);
            } else {
                token.bbox = token.owner.CreateBBox(type);
            }
        }

        it = new EasyTraversal(this, false);
        while (it.HasNext()) {
            EasyToken token = it.Next();
            if (WhoAmI(token) == EasyOwnerType.UNDER_DIVLINE) {
                token.UpdateDivision(token.owner.under_divline.indexOf(token));
            }
        }
    }

    /*public void UpdateBboxIndxes() {
        EasyTraversal it = new EasyTraversal(this);
        it.SetIgnore(EasyOwnerType.UNDER_DIVLINE);
        it.SetIgnoreFirst(EasyOwnerType.RIGHT);
        it.Next(); // skip updated root
        while (it.HasNext()) {
            EasyToken token = it.Next();
            EasyOwnerType type = WhoAmI(token);
            token.bbox = token.owner.CreateBBox(type);
        }
        //TODO: Update divs in idxes
    }*/

    public void UpdateDivision(int idx)
    {
        EasyToken start_numerator = this.owner;
        EasyToken end_numerator = start_numerator.GetEndOfNumerator(idx);
        EasyToken start_denumerator = start_numerator.under_divline.get(idx);
        EasyToken end_denumerator = start_denumerator.GetEndOfDenumerator();

        EasyToken rightest_in_numerator = GetRightestInNumerator(start_numerator, end_numerator, idx);

        double numerator_max_x = rightest_in_numerator.bbox.right_top.x;
        double numerator_min_x = start_numerator.bbox.left_bottom.x;
        double numerator_len = numerator_max_x - numerator_min_x;

        double denumerator_max_x = end_denumerator.GetRightestSmall().bbox.right_top.x;
        double denumerator_min_x = start_denumerator.bbox.left_bottom.x;
        double denumerator_len = denumerator_max_x - denumerator_min_x;

        double line_len = Math.max(numerator_len, denumerator_len);

        double yoffset = bbox.Height() * div_dist_factor;

        // Translate non numerator rest to end of denumerator in the case, then denumerator is longer
        //TODO: Remove it then GetRightest used in CreateRightToken
        if (abs(line_len - numerator_len) > 0.001) {
            if (end_numerator.right != null) {
                Translate(end_numerator.right, denumerator_len - numerator_len, 0, true);
            }
        }

        // Numerator goes up to yoffset
        TranslateNumeratorRecursive(start_numerator, end_numerator, idx, 0, yoffset);
        // Translate single tokens (no under_divline) to yoffset / 2
        TranlateNearNonDivTokens(start_numerator, end_numerator, 0, yoffset / 2);

        // Align in x ases
        {
            double xoffset_n = 0;
            double xoffset_d = 0;

            if (line_len != numerator_len) {
                xoffset_n = (line_len - numerator_len) / 2;
                //TODO: change, then height of de/numerator don't equals to start
                Vec lb = new Vec(start_denumerator.bbox.left_bottom.x, GetUppest(start_denumerator).bbox.right_top.y);
                Vec rt = new Vec(end_denumerator.GetRightestSmall().bbox.right_top.x, start_numerator.bbox.left_bottom.y);

                start_denumerator.my_div_line = new EasyTokenBox(lb, rt);
                div_lines.add(start_denumerator.my_div_line);
            } else {
                xoffset_d = (line_len - denumerator_len) / 2;

                //TODO: change, then height of de/numerator don't equals to start
                Vec lb = new Vec(start_numerator.bbox.left_bottom.x, GetUppest(start_denumerator).bbox.right_top.y);
                Vec rt = new Vec(rightest_in_numerator.bbox.right_top.x, start_numerator.bbox.left_bottom.y);

                start_denumerator.my_div_line = new EasyTokenBox(lb, rt);
                div_lines.add(start_denumerator.my_div_line);
            }

            if (xoffset_n != 0) {
                TranslateNumerator(start_numerator, end_numerator, idx, xoffset_n, 0, false);
            }

            if (xoffset_d != 0) {
                Translate(start_denumerator, xoffset_d, 0, false);
            }
        }
    }

    /*public void TranslateUp(EasyToken start_numerator, EasyToken end_numerator, int idx, double yoffset) {
        // Right to numerator
        if (end_numerator.right != null) {
            Translate(end_numerator.right, 0, yoffset / 2, true);
        }

        // Left to numerator
        if (this.owner != null && WhoAmI(this) == EasyOwnerType.RIGHT) {
            EasyToken left = this.owner;
            left.right = null;
            Translate(left.GetRoot(), 0, yoffset / 2, true);
            left.right = this;
        }

        TranslateNumerator(start_numerator, end_numerator, idx, 0, yoffset);
    }*/

    public void Translate(EasyToken root, double xoffset, double yoffset, boolean translate_lines) {
        EasyTraversal it = new EasyTraversal(root);
        while (it.HasNext()) {
            EasyToken token = it.Next();
            token.bbox.Translate(xoffset, yoffset);
            if (token.my_div_line != null) {
                if (translate_lines) {
                    token.my_div_line.Translate(xoffset, yoffset);
                }
            }
        }
    }

    public void TranslateOneNode(EasyToken root, double xoffset, double yoffset, boolean translate_lines) {
        EasyTraversal it = new EasyTraversal(root);
        it.SetIgnoreFirst(EasyOwnerType.RIGHT);
        it.SetIgnoreFirst(EasyOwnerType.UNDER_DIVLINE);
        while (it.HasNext()) {
            EasyToken token = it.Next();
            token.bbox.Translate(xoffset, yoffset);
            if (token.my_div_line != null) {
                if (translate_lines) {
                    token.my_div_line.Translate(xoffset, yoffset);
                }
            }
        }
    }

    public void TranslateNumeratorRecursive(EasyToken start_numerator, EasyToken end_numerator, int idx, double xoffset, double yoffset)
    {
        TranslateNumerator(start_numerator, end_numerator, idx, xoffset, yoffset, false);

        EasyToken den_end = end_numerator.GetEndOfDenumerator();
                                                                // in this case it updates in translate numerator
        if (den_end != null /*&& end_numerator.owner2 == null*/ && WhoAmI(start_numerator) != EasyOwnerType.UNDER_DIVLINE) {
            EasyToken n_end = den_end.owner2;
            EasyToken n_start = den_end.GetStartOfNumerator();

            // TODO: now []/([]/[])/([]/[]) bad render
            /*EasyToken flag, cur = n_start;
            while (cur != n_end) {
                if (cur.under_divline.size() != 0) {
                    flag = cur;
                }
                cur = cur.right;
            }
            if (flag = n_start) {

            }*/

            EasyToken den_start = den_end.GetStartOfDenumerator();
            int new_idx = n_start.under_divline.indexOf(den_start);
            TranslateNumerator(n_start, n_end, new_idx, xoffset, yoffset, true);
        }
    }

    public void TranlateNearNonDivTokens(EasyToken start_numerator, EasyToken end_numerator, double xoffset, double yoffset)
    {
        // Translate tokens left to...
        EasyToken left = null;
        if (WhoAmI(start_numerator) == EasyOwnerType.RIGHT) {
            left = start_numerator.owner;
        }

        EasyToken cur = left;
        while (cur != null && cur.under_divline2.size() == 0) {
            if (cur.is_single_translated) {
                break;
            }
            cur.is_single_translated = true;
            TranslateOneNode(cur, xoffset, yoffset, true);
            if (WhoAmI(cur) == EasyOwnerType.RIGHT) {
                cur = cur.owner;
                if (cur.right.under_divline.size() != 0) {
                    break;
                }
            } else {
                cur = null;
            }
        }

        // Translate tokens right to...
        cur = end_numerator.right;
        while (cur != null && cur.under_divline.size() == 0) {
            if (cur.is_single_translated) {
                break;
            }
            cur.is_single_translated = true;
            TranslateOneNode(cur, xoffset, yoffset, true);
            cur = cur.right;
        }
    }

    public void TranslateNumerator(EasyToken start_numerator, EasyToken end_numerator, int idx, double xoffset, double yoffset, boolean tr_nearest) {
        EasyToken t1 = start_numerator.owner;
        EasyToken t2 = end_numerator.right;

        if (WhoAmI(start_numerator) == EasyOwnerType.RIGHT && !tr_nearest) {
            start_numerator.owner = null;
        }
        end_numerator.right = null;

        Stack<EasyToken> deleted = new Stack<>();
        for (int i = start_numerator.under_divline.size() - 1; i >= idx; i--) {
            deleted.push(start_numerator.under_divline.get(i));
        }
        for (int i = start_numerator.under_divline.size() - idx; i > 0; i--) {
            start_numerator.under_divline.remove(start_numerator.under_divline.size() - 1);
        }

        Translate(this.GetRoot(), xoffset, yoffset, true);

        while (!deleted.empty()) {
            start_numerator.under_divline.add(deleted.pop());
        }

        start_numerator.owner = t1;
        end_numerator.right = t2;
    }


    public void CreateBBoxSkeleton() {
        EasyToken root = GetRoot();
        root.UpdateBboxNeightboors();
    }


    public EasyTokenBox CreateRootBbox(){
        return new EasyTokenBox(new Vec(-1 * 0.7 * scale, -1 * scale), new Vec(1 * 0.7 * scale, 1 * scale));
    }


    public EasyTokenBox CreateBBox(EasyOwnerType type) {
        switch (type) {
            case ROOT:
                return CreateRootBbox();
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

    static public EasyOwnerType WhoAmI (EasyToken token) {
        EasyToken my_owner = token.owner;

        if (my_owner == null) {
            return EasyOwnerType.ROOT;
        }

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
        } else if (my_owner.under_divline.contains(token)) {
            return EasyOwnerType.UNDER_DIVLINE;
        }
        return EasyOwnerType.INVALID;
    }

    public EasyToken GetRightestInNumerator (EasyToken start_numerator, EasyToken end_numerator, int idx) {
        EasyToken t1 = start_numerator.owner;
        EasyToken t2 = end_numerator.right;

        //if (WhoAmI(start_numerator) == EasyOwnerType.RIGHT) {
            start_numerator.owner = null;
        //}
        end_numerator.right = null;

        Stack<EasyToken> deleted = new Stack<>();
        for (int i = start_numerator.under_divline.size() - 1; i >= idx; i--) {
            deleted.push(start_numerator.under_divline.get(i));
        }
        for (int i = start_numerator.under_divline.size() - idx; i > 0; i--) {
            start_numerator.under_divline.remove(start_numerator.under_divline.size() - 1);
        }

        EasyToken rightest = GetRightest(this.GetRoot());

        while (!deleted.empty()) {
            start_numerator.under_divline.add(deleted.pop());
        }

        start_numerator.owner = t1;
        end_numerator.right = t2;

        return rightest;
    }

    public EasyToken GetRightest(EasyToken root) {
        EasyToken rightest = null;

        EasyTraversal it = new EasyTraversal(root);
        while (it.HasNext()) {
            EasyToken token = it.Next();
            if (rightest == null) {
                rightest = token;
            } else {
                if (rightest.bbox.right_top.x < token.bbox.right_top.x) {
                    rightest = token;
                }
            }
        }
        return rightest;
    }

    public EasyToken GetUppest(EasyToken root) {
        EasyToken uppest = null;

        EasyTraversal it = new EasyTraversal(root);
        while (it.HasNext()) {
            EasyToken token = it.Next();
            if (uppest == null) {
                uppest = token;
            } else {
                if (uppest.bbox.right_top.y < token.bbox.right_top.y) {
                    uppest = token;
                }
            }
        }
        return uppest;
    }

/////////////////////////////////
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
/////////////////////////////////

    // End denumerator has ref to end of numerator (owner2)
    public EasyToken GetEndOfNumerator(int idx) {
        return under_divline.get(idx).GetEndOfDenumerator().owner2;
    }

    // End of denumerator has not null owner2 in row (start denumerator->right->right->...)
    // Other tokens in this row has null owner2
    public EasyToken GetEndOfDenumerator() {
        EasyToken end_denumerator = this;

        while (end_denumerator != null && end_denumerator.owner2 == null) {
            end_denumerator = end_denumerator.right;
        }
        return end_denumerator;
    }

    public EasyToken GetStartOfDenumerator() {
        EasyToken start_denumerator = this;
        while (WhoAmI(start_denumerator) != EasyOwnerType.UNDER_DIVLINE) {
            start_denumerator = start_denumerator.owner;
        }
        return start_denumerator;
    }

    public EasyToken GetStartOfNumerator() {
        return GetStartOfDenumerator().owner;
    }

    private void FillDivlineRef()
    {
        div_lines = GetRoot().div_lines;
        div_lines.clear(); //TODO: stay that here?
    }

    private EasyToken GetRoot () {
        EasyToken root = this;
        while (root.owner != null) {
            root = root.owner;
        }
        return root;
    }


    static EasyTokenBox ToScreenCoord(int width, int height, EasyTokenBox bbox) {
        EasyTokenBox tr_box = new EasyTokenBox(new Vec(bbox.left_bottom), new Vec(bbox.right_top));
        tr_box.Scale(50);
        tr_box.InverseY();
        tr_box.Translate(width / 2.0f, height / 2.0f);
        return tr_box;
    }

    public String getText() {
        return text;
    }

    public EasyToken setText(String text) {
        this.text = text;
        return this;
    }


    public String ToLatex () {
        EasyToken root = GetRoot();
        return new String(root.ToLatexInternal(0));
    }

    public  StringBuilder ToLatexInternal(int curIdx) {
        int cur_ud = this.under_divline.size() - curIdx - 1;

        // parse division
        if (cur_ud >= 0) {
            EasyToken end_num = this.GetEndOfNumerator(cur_ud);
            EasyToken after_num = end_num.right;
            end_num.right = null;

            StringBuilder div_latex = new StringBuilder("")
                    .append("\\frac{")
                    .append(this.ToLatexInternal(curIdx + 1))
                    .append("}{")
                    .append(this.under_divline.get(cur_ud).ToLatexInternal(0))
                    .append("}");

            end_num.right = after_num;

            if (after_num != null) {
                div_latex.append(after_num.ToLatexInternal(0));
            }

            return div_latex;
        }

        // parse token group
        if (token_group_id != -1) {
            if (right == null || right.token_group_id != token_group_id) {
                return new StringBuilder("");
            }

            EasyToken start = this;
            EasyToken end = GetEndOfGroup();
            EasyToken entry = GetEmptyInGroup();
            EasyToken after_end = end.right;

            StringBuilder group_name = new StringBuilder("\\"); // contains '('
            EasyToken cur = start;
            while (cur != entry) {
                group_name.append(cur.text);
                cur = cur.right;
            }
            group_name.setCharAt(group_name.length() - 1, '{');

            StringBuilder after_end_latex = new StringBuilder();
            if (after_end != null) {
                after_end_latex = after_end.ToLatexInternal(0);
            }

            return group_name
                    .append(entry.ToLatexInternal(0))
                    .append('}')
                    .append(after_end_latex);
        }

        // parse simple token
        if (right == null) {
            return GetMyLatexWithIdxes();
        } else {
            return GetMyLatexWithIdxes().append(right.ToLatexInternal(0));
        }
    }

    public StringBuilder GetMyLatexWithIdxes () {
        StringBuilder my_latex = new StringBuilder("");
        my_latex.append(text);
        if (r_up != null) {
            my_latex.append("^{").append(r_up.ToLatexInternal(0)).append("}");
        }
        if (r_down != null){
            my_latex.append("_{").append(r_down.ToLatexInternal(0).append("}"));
        }
        return my_latex;
    }
}


enum EasyOwnerType {
    UP,
    R_UP,
    R_DOWN,
    DOWN,

    RIGHT,
    UNDER_DIVLINE,

    ROOT,

    INVALID
}