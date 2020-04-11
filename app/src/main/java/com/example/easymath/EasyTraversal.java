package com.example.easymath;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

// Simple iterator in EasyExpression
// Deep traversal from entry_point
public class EasyTraversal {
    public EasyTraversal(EasyToken entry_point_) {
        entry_point = entry_point_;
        stack = new Stack<>();
        ignored_types = new ArrayList<>();
        ignored_first_types = new ArrayList<>();
        CreateStack();
    }

    public EasyToken Next() {
        return stack.pop();
    }

    public boolean HasNext() {
        return !stack.empty();
    }

    // Recalculates stack: TODO: change interface if needed
    public void SetIgnore(EasyOwnerType type) {
        ignored_types.add(type);
        stack = new Stack<>();
        CreateStack();
    }

    // Recalculates stack: TODO: change interface if needed
    public void SetIgnoreFirst(EasyOwnerType type) {
        ignored_first_types.add(type);
        stack = new Stack<>();
        CreateStack();
    }

    private EasyToken entry_point;
    private Stack<EasyToken> stack;
    private List<EasyOwnerType> ignored_types;
    private List<EasyOwnerType> ignored_first_types;

    private void CreateStack() {
        stack.push(entry_point);
        TraverseToken(entry_point, true);

        Stack<EasyToken> stackReversed = new Stack<>();
        while (!stack.empty()) {
            stackReversed.push(stack.pop());
        }

        stack = stackReversed;
    }

    private void TraverseToken(EasyToken token, boolean isFirstEnter) {
        if (!NeedSkip(EasyOwnerType.UP, isFirstEnter)) {
            TraverseTokenInternal(token.up);
        }
        if (!NeedSkip(EasyOwnerType.R_UP, isFirstEnter)) {
            TraverseTokenInternal(token.r_up);
        }
        if (!NeedSkip(EasyOwnerType.R_DOWN, isFirstEnter)) {
            TraverseTokenInternal(token.r_down);
        }
        if (!NeedSkip(EasyOwnerType.DOWN, isFirstEnter)) {
            TraverseTokenInternal(token.down);
        }


        if (!NeedSkip(EasyOwnerType.RIGHT, isFirstEnter)) {
            TraverseTokenInternal(token.right);
        }
        if (!NeedSkip(EasyOwnerType.UNDER_DIVLINE, isFirstEnter)) {
            TraverseTokenInternal(token.under_divline);
        }
    }

    private void TraverseTokenInternal(EasyToken token) {
        if (token != null) {
            stack.push(token);
            TraverseToken(token, false);
        }
    }

    private boolean NeedSkip(EasyOwnerType type, boolean isFirstEnter) {
        return ignored_types.contains(type) || (isFirstEnter && ignored_first_types.contains((type)));
    }
}