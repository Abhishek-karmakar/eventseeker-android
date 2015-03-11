package com.wcities.eventseeker.viewdata;

import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ItemDecoration;
import android.view.View;

public class ItemDecorationItemOffset extends ItemDecoration {

	private int offset;

    public ItemDecorationItemOffset(int offset) {
        this.offset = offset;
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        outRect.left = offset;
        outRect.right = offset;
        outRect.bottom = offset;
        outRect.top = offset;
    }
}
