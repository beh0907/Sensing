package com.coretec.sensing.utils;

import android.graphics.Color;
import android.graphics.drawable.Drawable;

import devlight.io.library.ntb.NavigationTabBar;

//추모방 하단 탭 메뉴
public class TabModel {
    // 메뉴아이콘, 선택시 색상, 메뉴 타이틀
    public static NavigationTabBar.Model createTabModel(Drawable drawable, String color, String title) {
        return new NavigationTabBar.Model.Builder(
                drawable,
                Color.parseColor(color)
        ).title(title)
                .build();
    }
}
