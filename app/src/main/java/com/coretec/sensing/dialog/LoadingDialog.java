package com.coretec.sensing.dialog;

import android.app.ProgressDialog;
import android.content.Context;

import androidx.appcompat.view.ContextThemeWrapper;

import com.coretec.sensing.R;

//로딩 다이얼로그
public class LoadingDialog {
    private static ProgressDialog progressDialog = null;

    public static void showDialog(Context context, String message) {
        ContextThemeWrapper cw = new ContextThemeWrapper( context, R.style.DialogTheme);
        progressDialog = new ProgressDialog(cw);
        progressDialog.setMessage(message);
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(false);
        progressDialog.show();
    }

    public static void hideDialog() {
        if (progressDialog == null)
            return;

        if (progressDialog.isShowing())
            progressDialog.dismiss();
    }
}
