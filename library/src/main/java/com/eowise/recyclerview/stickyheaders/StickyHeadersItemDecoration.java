package com.eowise.recyclerview.stickyheaders;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerViewHelper;
import android.util.SparseBooleanArray;
import android.util.SparseLongArray;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Transformation;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * Created by aurel on 22/09/14.
 */
public class StickyHeadersItemDecoration extends RecyclerView.ItemDecoration {

    private final StickyHeadersAdapter adapter;
    private final RecyclerView parent;
    private final RecyclerView.ViewHolder headerViewHolder;
    private final HashMap<Long, Boolean> headers;
    private final AdapterDataObserver adapterDataObserver;
    private HeaderPosition headerPosition;

    private final int headerHeight;

    public StickyHeadersItemDecoration(StickyHeadersAdapter adapter, RecyclerView parent) {
        this(adapter, parent, HeaderPosition.TOP);
    }

    public StickyHeadersItemDecoration(StickyHeadersAdapter adapter, RecyclerView parent, HeaderPosition headerPosition) {
        this.adapter = adapter;
        this.parent = parent;
        this.headerViewHolder = adapter.onCreateViewHolder(parent);
        this.headerPosition = headerPosition;
        this.headers = new HashMap<Long, Boolean>();
        this.adapterDataObserver = new AdapterDataObserver();

        int widthSpec = View.MeasureSpec.makeMeasureSpec(ViewGroup.LayoutParams.MATCH_PARENT, View.MeasureSpec.AT_MOST);
        int heightSpec = View.MeasureSpec.makeMeasureSpec(ViewGroup.LayoutParams.WRAP_CONTENT, View.MeasureSpec.UNSPECIFIED);
        headerViewHolder.itemView.measure(widthSpec, heightSpec);
        headerHeight = headerViewHolder.itemView.getMeasuredHeight();
    }

    @Override
    public void onDrawOver(Canvas c, RecyclerView parent, RecyclerView.State state) {


        final int childCount = parent.getChildCount();
        final RecyclerView.LayoutManager lm = parent.getLayoutManager();
        View header = headerViewHolder.itemView;
        Long currentHeaderId;
        Float lastY = null;


        if (!header.isLaidOut()) {
            int widthSpec = View.MeasureSpec.makeMeasureSpec(parent.getWidth(), View.MeasureSpec.EXACTLY);
            int heightSpec = View.MeasureSpec.makeMeasureSpec(ViewGroup.LayoutParams.WRAP_CONTENT, View.MeasureSpec.AT_MOST);
            header.measure(widthSpec, heightSpec);
            header.layout(0, 0, header.getMeasuredWidth(), header.getMeasuredHeight());
        }

        for (int i = childCount - 1; i >= 0; i--) {
            final View child = parent.getChildAt(i);
            final RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams)child.getLayoutParams();
            final int position = parent.getChildPosition(child);
            RecyclerView.ViewHolder holder = parent.getChildViewHolder(child);

            if (!lp.isItemRemoved()) {

                float translationY = ViewCompat.getTranslationY(child);

                if (i == 0 || isHeader(holder)) {

                    float y = getHeaderY(child, lm.getDecoratedTop(child)) + translationY;


                    if (lastY != null && lastY < y + headerHeight) {
                        y = lastY - headerHeight;
                    }


                    adapter.onBindViewHolder(headerViewHolder, position);

                    c.save();
                    c.translate(0, y);
                    header.draw(c);
                    c.restore();

                    lastY = y;
                }
            }
        }
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {

        RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams)view.getLayoutParams();
        RecyclerView.ViewHolder holder = parent.getChildViewHolder(view);

        if (!isHeader(holder)) {
            outRect.set(0, 0, 0, 0);
        }
        else {
            switch (headerPosition) {
                case LEFT:
                    outRect.set(headerHeight, 0, 0, 0);
                    break;
                case TOP:
                    outRect.set(0, headerHeight, 0, 0);
                    break;
                case RIGHT:
                    outRect.set(0, 0, headerHeight, 0);
                    break;
                case BOTTOM:
                    outRect.set(0, 0, 0, headerHeight);
                    break;
                case OVERLAY:
                    outRect.set(0, 0, 0, 0);
                    break;
            }
        }

        if (lp.isItemRemoved()) {
            headers.remove(holder.getItemId());
        }
    }

    public void registerAdapterDataObserver(RecyclerView.Adapter adapter) {
        adapter.registerAdapterDataObserver(adapterDataObserver);
    }

    private float getHeaderY(View item, int top) {

        float y;
        switch (headerPosition) {
            case TOP:
                y = top < 0 ? 0 : top;
                break;
            case BOTTOM:
                // TODO: Use getDecoratedBottom
                y = top + item.getHeight() < headerHeight ? 0 : top + item.getHeight();
                break;
            default:
                y = top < 0 ? 0 : top;
                break;
        }

        return y;
    }

    private Boolean isHeader(RecyclerView.ViewHolder holder) {
        if (!headers.containsKey(holder.getItemId())  || headers.get(holder.getItemId()) == null) {
            int itemPosition = RecyclerViewHelper.convertPreLayoutPositionToPostLayout(parent, holder.getPosition());

            if (itemPosition == 0) {
                headers.put(holder.getItemId(), true);
            }
            else {
                headers.put(holder.getItemId(), adapter.getHeaderId(itemPosition) != adapter.getHeaderId(itemPosition -1));
            }
        }

        return headers.get(holder.getItemId());
    }




    private class AdapterDataObserver extends RecyclerView.AdapterDataObserver {

        public AdapterDataObserver() {
        }

        @Override
        public void onItemRangeRemoved(int positionStart, int itemCount) {
            RecyclerView.ViewHolder holder = parent.findViewHolderForPosition(positionStart + 1);
            if (holder != null) {
                headers.put(holder.getItemId(), null);
            }
            else {
                cleanOffScreenItemsIds();
            }
        }

        @Override
        public void onItemRangeInserted(int positionStart, int itemCount) {
            RecyclerView.ViewHolder holder = parent.findViewHolderForPosition(positionStart);
            if (holder != null) {
                headers.put(holder.getItemId(), null);
            }
            else {
                cleanOffScreenItemsIds();
            }
        }

        private void cleanOffScreenItemsIds() {
            Iterator<Long> iterator = headers.keySet().iterator();
            while (iterator.hasNext()) {
                long itemId = iterator.next();
                if (parent.findViewHolderForItemId(itemId) == null) {
                    iterator.remove();
                }
            }
        }
    }

}
