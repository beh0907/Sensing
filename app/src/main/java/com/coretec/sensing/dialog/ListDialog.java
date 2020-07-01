package com.coretec.sensing.dialog;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.ContextThemeWrapper;

import com.coretec.sensing.R;
import com.coretec.sensing.listener.RecyclerViewClickListener;

//로딩 다이얼로그
public class ListDialog {
    //리스트 다이얼로그 생성
    public static void CreateListDialog(ArrayAdapter<String> arrayAdapter, Context context, RecyclerViewClickListener recyclerViewClickListener){
        AlertDialog.Builder alert = new AlertDialog.Builder(context);
        alert.setTitle("종점 선택");     //타이틀

        //어답터 , 클릭이벤트 설정
        alert.setAdapter(arrayAdapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String menu = arrayAdapter.getItem(which);
                recyclerViewClickListener.onClick(null, which);
            }
        });
        alert.show();
    }
}
