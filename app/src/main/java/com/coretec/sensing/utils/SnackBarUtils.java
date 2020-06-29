package com.coretec.sensing.utils;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import com.coretec.sensing.R;
import com.google.android.material.snackbar.Snackbar;

//하단 알림 스낵바 유틸
public class SnackBarUtils {
    public static void createSnackBar(Context context, View view, String content, int color) {
        Snackbar snackbar = Snackbar.make(view, content, Snackbar.LENGTH_SHORT);
        View sbView = snackbar.getView();
        TextView txtContent = sbView.findViewById(R.id.snackbar_text);
        txtContent.setTextColor(context.getResources().getColor(color));
        snackbar.show();
    }
}
