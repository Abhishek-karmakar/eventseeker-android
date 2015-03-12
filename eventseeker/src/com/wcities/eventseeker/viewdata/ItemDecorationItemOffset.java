package com.wcities.eventseeker.viewdata;

import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ItemDecoration;
import android.view.View;

public class ItemDecorationItemOffset extends ItemDecoration {

	private int lrOffset, tbOffset;

    public ItemDecorationItemOffset(int lrOffset, int tbOffset) {
        this.lrOffset = lrOffset;
        this.tbOffset = tbOffset;
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        outRect.left = lrOffset;
        outRect.right = lrOffset;
        outRect.bottom = tbOffset;
        outRect.top = tbOffset;
    }
}
