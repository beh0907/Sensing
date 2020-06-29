package com.coretec.sensing.dialog;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

//로딩 다이얼로그
public class AlarmDialog {
    public static void showDialog(Context context, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage(message);

        builder.setPositiveButton("확인", new DialogInterface.OnClickListener(){
            @Override
            public void onClick(DialogInterface dialog, int id)
            {
                dialog.dismiss();
            }
        });
        builder.setCancelable(false);
        AlertDialog dialog = builder.create();
        dialog.show();
    }
}
