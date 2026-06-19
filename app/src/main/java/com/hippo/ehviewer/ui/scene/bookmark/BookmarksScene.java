/*
 * Copyright 2016 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hippo.ehviewer.ui.scene.bookmark;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.h6ah4i.android.widget.advrecyclerview.animator.GeneralItemAnimator;
import com.h6ah4i.android.widget.advrecyclerview.animator.SwipeDismissItemAnimator;
import com.h6ah4i.android.widget.advrecyclerview.swipeable.RecyclerViewSwipeManager;
import com.h6ah4i.android.widget.advrecyclerview.swipeable.SwipeableItemAdapter;
import com.h6ah4i.android.widget.advrecyclerview.swipeable.SwipeableItemConstants;
import com.h6ah4i.android.widget.advrecyclerview.swipeable.action.SwipeResultAction;
import com.h6ah4i.android.widget.advrecyclerview.swipeable.action.SwipeResultActionDefault;
import com.h6ah4i.android.widget.advrecyclerview.swipeable.action.SwipeResultActionRemoveItem;
import com.h6ah4i.android.widget.advrecyclerview.touchguard.RecyclerViewTouchActionGuardManager;
import com.h6ah4i.android.widget.advrecyclerview.utils.AbstractSwipeableItemViewHolder;
import com.hippo.android.resource.AttrResources;
import com.hippo.app.EditTextDialogBuilder;
import com.hippo.easyrecyclerview.EasyRecyclerView;
import com.hippo.easyrecyclerview.FastScroller;
import com.hippo.easyrecyclerview.HandlerDrawable;
import com.hippo.easyrecyclerview.MarginItemDecoration;
import com.hippo.ehviewer.EhDB;
import com.tianri.ehviewer_fplus.R;
import com.hippo.ehviewer.Settings;
import com.hippo.ehviewer.client.EhCacheKeyFactory;
import com.hippo.ehviewer.client.EhUtils;
import com.hippo.ehviewer.client.data.GalleryInfo;
import com.hippo.ehviewer.ui.GalleryActivity;
import com.hippo.ehviewer.ui.scene.ToolbarScene;
import com.hippo.ehviewer.ui.scene.TransitionNameFactory;
import com.hippo.ehviewer.widget.SimpleRatingView;
import com.hippo.ripple.Ripple;
import com.hippo.util.DrawableManager;
import com.hippo.view.ViewTransition;
import com.hippo.widget.LoadImageView;
import com.hippo.widget.recyclerview.AutoStaggeredGridLayoutManager;
import com.hippo.lib.yorozuya.AssertUtils;
import com.hippo.lib.yorozuya.ViewUtils;

import java.util.ArrayList;
import java.util.List;

public class BookmarksScene extends ToolbarScene
        implements EasyRecyclerView.OnItemClickListener,
        EasyRecyclerView.OnItemLongClickListener {

    @Nullable
    private EasyRecyclerView mRecyclerView;
    @Nullable
    private ViewTransition mViewTransition;
    @Nullable
    private RecyclerView.Adapter<?> mAdapter;
    @Nullable
    private List<EhDB.PageBookmark> mItems;

    @Override
    public int getNavCheckedItem() {
        return R.id.nav_bookmarks;
    }

    @Nullable
    @Override
    public View onCreateView3(LayoutInflater inflater,
            @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.scene_history, container, false);
        View content = ViewUtils.$$(view, R.id.content);
        mRecyclerView = (EasyRecyclerView) ViewUtils.$$(content, R.id.recycler_view);
        FastScroller fastScroller = (FastScroller) ViewUtils.$$(content, R.id.fast_scroller);
        TextView tip = (TextView) ViewUtils.$$(view, R.id.tip);
        tip.setText(R.string.no_bookmarks);
        mViewTransition = new ViewTransition(content, tip);

        Context context = getEHContext();
        AssertUtils.assertNotNull(context);
        Resources resources = context.getResources();

        Drawable drawable = DrawableManager.getVectorDrawable(context, R.drawable.big_history);
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        tip.setCompoundDrawables(null, drawable, null, null);

        RecyclerViewTouchActionGuardManager guardManager = new RecyclerViewTouchActionGuardManager();
        guardManager.setInterceptVerticalScrollingWhileAnimationRunning(true);
        guardManager.setEnabled(true);
        RecyclerViewSwipeManager swipeManager = new RecyclerViewSwipeManager();
        mAdapter = new BookmarkAdapter();
        mAdapter.setHasStableIds(true);
        mAdapter = swipeManager.createWrappedAdapter(mAdapter);
        mRecyclerView.setAdapter(mAdapter);
        final GeneralItemAnimator animator = new SwipeDismissItemAnimator();
        animator.setSupportsChangeAnimations(false);
        mRecyclerView.setItemAnimator(animator);
        AutoStaggeredGridLayoutManager layoutManager = new AutoStaggeredGridLayoutManager(
                0, StaggeredGridLayoutManager.VERTICAL);
        layoutManager.setColumnSize(resources.getDimensionPixelOffset(Settings.getDetailSizeResId()));
        layoutManager.setStrategy(AutoStaggeredGridLayoutManager.STRATEGY_MIN_SIZE);
        mRecyclerView.setLayoutManager(layoutManager);
        mRecyclerView.setSelector(Ripple.generateRippleDrawable(context, !AttrResources.getAttrBoolean(context, androidx.appcompat.R.attr.isLightTheme), new ColorDrawable(Color.TRANSPARENT)));
        mRecyclerView.setDrawSelectorOnTop(true);
        mRecyclerView.setClipToPadding(false);
        mRecyclerView.setOnItemClickListener(this);
        mRecyclerView.setOnItemLongClickListener(this);
        int interval = resources.getDimensionPixelOffset(R.dimen.gallery_list_interval);
        int paddingH = resources.getDimensionPixelOffset(R.dimen.gallery_list_margin_h);
        int paddingV = resources.getDimensionPixelOffset(R.dimen.gallery_list_margin_v);
        MarginItemDecoration decoration = new MarginItemDecoration(interval, paddingH, paddingV, paddingH, paddingV);
        mRecyclerView.addItemDecoration(decoration);
        decoration.applyPaddings(mRecyclerView);
        guardManager.attachRecyclerView(mRecyclerView);
        swipeManager.attachRecyclerView(mRecyclerView);

        fastScroller.attachToRecyclerView(mRecyclerView);
        HandlerDrawable handlerDrawable = new HandlerDrawable();
        handlerDrawable.setColor(AttrResources.getAttrColor(context, R.attr.widgetColorThemeAccent));
        fastScroller.setHandlerDrawable(handlerDrawable);

        refreshList();
        updateView(false);

        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setTitle(R.string.bookmarks);
        setNavigationIcon(R.drawable.v_arrow_left_dark_x24);
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshList();
        if (mAdapter != null) {
            mAdapter.notifyDataSetChanged();
            updateView(true);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (null != mRecyclerView) {
            mRecyclerView.stopScroll();
            mRecyclerView = null;
        }

        mViewTransition = null;
        mAdapter = null;
        mItems = null;
    }

    private void refreshList() {
        mItems = EhDB.getAllBookmarks();
    }

    private void updateView(boolean animation) {
        if (null == mAdapter || null == mViewTransition) {
            return;
        }

        if (mAdapter.getItemCount() == 0) {
            mViewTransition.showView(1, animation);
        } else {
            mViewTransition.showView(0, animation);
        }
    }

    @Override
    public void onNavigationClick(View view) {
        onBackPressed();
    }

    @Override
    public int getMenuResId() {
        return R.menu.scene_history;
    }

    private void showClearAllDialog() {
        new AlertDialog.Builder(getEHContext())
                .setMessage(R.string.clear_all_bookmarks)
                .setPositiveButton(R.string.clear_all, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (DialogInterface.BUTTON_POSITIVE != which || null == mAdapter) {
                            return;
                        }

                        EhDB.clearAllBookmarks();
                        refreshList();
                        mAdapter.notifyDataSetChanged();
                        updateView(true);
                    }
                }).show();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        Context context = getEHContext();
        if (null == context) {
            return false;
        }

        int id = item.getItemId();
        if (id == R.id.action_clear_all) {
            showClearAllDialog();
            return true;
        }
        return false;
    }

    @Override
    public boolean onItemClick(EasyRecyclerView parent, View view, int position, long id) {
        if (null == mItems) {
            return false;
        }

        EhDB.PageBookmark bookmark = mItems.get(position);
        Intent intent = new Intent(getEHContext(), GalleryActivity.class);
        intent.setAction(GalleryActivity.ACTION_EH);
        intent.putExtra(GalleryActivity.KEY_GALLERY_INFO, bookmark.info);
        intent.putExtra(GalleryActivity.KEY_PAGE, bookmark.page);
        startActivity(intent);
        return true;
    }

    @Override
    public boolean onItemLongClick(EasyRecyclerView parent, View view, int position, long id) {
        final Context context = getEHContext();
        if (null == context || null == mItems) {
            return false;
        }

        final EhDB.PageBookmark bookmark = mItems.get(position);
        final int pos = position;
        new AlertDialog.Builder(context)
                .setTitle(EhUtils.getSuitableTitle(bookmark.info))
                .setItems(new CharSequence[]{getString(R.string.bookmark_edit_note),
                        getString(R.string.bookmark_remove)}, (dialog, which) -> {
                    if (which == 0) {
                        showNoteEditor(bookmark, pos);
                    } else if (which == 1) {
                        EhDB.removeBookmark(bookmark.id);
                        refreshList();
                        if (mAdapter != null) {
                            mAdapter.notifyDataSetChanged();
                            updateView(true);
                        }
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
        return true;
    }

    private void showNoteEditor(EhDB.PageBookmark bookmark, int position) {
        final Context context = getEHContext();
        if (null == context) {
            return;
        }
        EditTextDialogBuilder builder = new EditTextDialogBuilder(context,
                bookmark.note, getString(R.string.bookmark_note_hint));
        builder.setTitle(getString(R.string.bookmark_page_at, bookmark.page + 1));
        builder.setPositiveButton(android.R.string.ok, (dialog, which) -> {
            String note = builder.getText().trim();
            EhDB.updateBookmarkNote(bookmark.id, note.isEmpty() ? null : note);
            refreshList();
            if (mAdapter != null) {
                mAdapter.notifyDataSetChanged();
            }
        });
        builder.setNegativeButton(android.R.string.cancel, null);
        builder.show();
    }

    private class BookmarkHolder extends AbstractSwipeableItemViewHolder {

        public final View card;
        public final LoadImageView thumb;
        public final TextView title;
        public final TextView uploader;
        public final SimpleRatingView rating;
        public final TextView category;
        public final TextView page;
        public final TextView note;
        public final TextView simpleLanguage;

        public BookmarkHolder(View itemView) {
            super(itemView);

            card = itemView.findViewById(R.id.card);
            thumb = (LoadImageView) itemView.findViewById(R.id.thumb);
            title = (TextView) itemView.findViewById(R.id.title);
            uploader = (TextView) itemView.findViewById(R.id.uploader);
            rating = (SimpleRatingView) itemView.findViewById(R.id.rating);
            category = (TextView) itemView.findViewById(R.id.category);
            page = (TextView) itemView.findViewById(R.id.pages);
            note = (TextView) itemView.findViewById(R.id.posted);
            simpleLanguage = (TextView) itemView.findViewById(R.id.simple_language);
        }

        @Override
        public View getSwipeableContainerView() {
            return card;
        }
    }

    private class BookmarkAdapter extends RecyclerView.Adapter<BookmarkHolder>
            implements SwipeableItemAdapter<BookmarkHolder> {

        private final LayoutInflater mInflater;
        private final int mListThumbWidth;
        private final int mListThumbHeight;

        public BookmarkAdapter() {
            mInflater = getLayoutInflater2();

            View calculator = mInflater.inflate(R.layout.item_gallery_list_thumb_height, null);
            ViewUtils.measureView(calculator, 1024, ViewGroup.LayoutParams.WRAP_CONTENT);
            mListThumbHeight = calculator.getMeasuredHeight();
            mListThumbWidth = mListThumbHeight * 2 / 3;
        }

        @Override
        public long getItemId(int position) {
            if (null == mItems) {
                return super.getItemId(position);
            } else {
                return mItems.get(position).id;
            }
        }

        @Override
        public BookmarkHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            BookmarkHolder holder = new BookmarkHolder(mInflater.inflate(R.layout.item_history, parent, false));

            ViewGroup.LayoutParams lp = holder.thumb.getLayoutParams();
            lp.width = mListThumbWidth;
            lp.height = mListThumbHeight;
            holder.thumb.setLayoutParams(lp);

            return holder;
        }

        @Override
        public void onBindViewHolder(BookmarkHolder holder, int position) {
            if (null == mItems) {
                return;
            }

            EhDB.PageBookmark bookmark = mItems.get(position);
            GalleryInfo gi = bookmark.info;
            holder.thumb.load(EhCacheKeyFactory.getThumbKey(gi.gid), gi.thumb);
            holder.title.setText(EhUtils.getSuitableTitle(gi));
            holder.uploader.setText(gi.uploader);
            holder.rating.setRating(gi.rating);
            TextView category = holder.category;
            String newCategoryText = EhUtils.getCategory(gi.category);
            if (!newCategoryText.equals(category.getText())) {
                category.setText(newCategoryText);
                category.setBackgroundColor(EhUtils.getCategoryColor(gi.category));
            }
            holder.page.setText(getString(R.string.bookmark_page_at, bookmark.page + 1));
            if (bookmark.note != null && !bookmark.note.isEmpty()) {
                holder.note.setText(bookmark.note);
            } else {
                holder.note.setText(gi.posted);
            }
            holder.simpleLanguage.setText(gi.simpleLanguage);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                long gid = gi.gid;
                ViewCompat.setTransitionName(holder.thumb, TransitionNameFactory.getThumbTransitionName(gid));
            }
        }

        @Override
        public int getItemCount() {
            return null != mItems ? mItems.size() : 0;
        }

        @Override
        public int onGetSwipeReactionType(BookmarkHolder holder, int position, int x, int y) {
            return SwipeableItemConstants.REACTION_CAN_SWIPE_LEFT;
        }

        @Override
        public void onSwipeItemStarted(BookmarkHolder holder, int position) { }

        @Override
        public void onSetSwipeBackground(BookmarkHolder holder, int position, int type) {}

        @Override
        public SwipeResultAction onSwipeItem(BookmarkHolder holder, int position, int result) {
            switch (result) {
                case SwipeableItemConstants.RESULT_SWIPED_LEFT:
                    return new SwipeResultActionClear(position);
                case SwipeableItemConstants.RESULT_SWIPED_RIGHT:
                case SwipeableItemConstants.RESULT_CANCELED:
                default:
                    return new SwipeResultActionDefault();
            }
        }
    }

    private class SwipeResultActionClear extends SwipeResultActionRemoveItem {

        private final int mPosition;

        protected SwipeResultActionClear(int position) {
            mPosition = position;
        }

        @Override
        protected void onPerformAction() {
            super.onPerformAction();
            if (null == mItems || null == mAdapter || mPosition < 0 || mPosition >= mItems.size()) {
                return;
            }

            EhDB.PageBookmark bookmark = mItems.get(mPosition);
            EhDB.removeBookmark(bookmark.id);
            refreshList();
            mAdapter.notifyDataSetChanged();
            updateView(true);
        }
    }
}
