package com.hippo.ehviewer.ui.scene.tracking;

import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.hippo.easyrecyclerview.EasyRecyclerView;
import com.hippo.ehviewer.Settings;
import com.tianri.ehviewer_fplus.R;
import com.hippo.ehviewer.client.EhCacheKeyFactory;
import com.hippo.ehviewer.client.EhUtils;
import com.hippo.ehviewer.client.data.GalleryInfo;
import com.hippo.ehviewer.sync.TrackingManager;
import com.hippo.ehviewer.ui.scene.ToolbarScene;
import com.hippo.ehviewer.ui.scene.gallery.detail.GalleryDetailScene;
import com.hippo.scene.Announcer;
import com.hippo.view.ViewTransition;
import com.hippo.widget.LoadImageView;
import com.hippo.lib.yorozuya.ViewUtils;

import java.util.ArrayList;
import java.util.List;

public final class TrackingScene extends ToolbarScene
        implements EasyRecyclerView.OnItemClickListener {

    private EasyRecyclerView recyclerView;
    private ViewTransition viewTransition;
    private final List<TrackingManager.TrackedGallery> items = new ArrayList<>();
    private TrackingAdapter adapter;

    @Override
    public int getNavCheckedItem() {
        return R.id.nav_tracking;
    }

    @Nullable
    @Override
    public View onCreateView3(LayoutInflater inflater, @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.scene_history, container, false);
        View content = view.findViewById(R.id.content);
        recyclerView = content.findViewById(R.id.recycler_view);
        TextView tip = view.findViewById(R.id.tip);
        tip.setText(R.string.no_tracking_galleries);
        tip.setCompoundDrawables(null, null, null, null);
        viewTransition = new ViewTransition(content, tip);
        recyclerView.setLayoutManager(new LinearLayoutManager(getEHContext()));
        recyclerView.setOnItemClickListener(this);
        adapter = new TrackingAdapter(inflater);
        recyclerView.setAdapter(adapter);
        reload();
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setTitle(R.string.tracking);
        setNavigationIcon(R.drawable.v_arrow_left_dark_x24);
        if (!Settings.getTrackingEnabled()) {
            showFirstRunDialog();
        } else {
            TrackingManager.syncCloudTags(requireContext());
        }
    }

    @Override
    public int getMenuResId() {
        return R.menu.scene_tracking;
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_add_tracking_tag) {
            showAddTagDialog();
            return true;
        } else if (id == R.id.action_refresh_tracking) {
            runCheck();
            return true;
        } else if (id == R.id.action_manage_tracking_tags) {
            showTagManager();
            return true;
        } else if (id == R.id.action_tracking_mark_all_read) {
            TrackingManager.markAllRead();
            reload();
            return true;
        }
        return false;
    }

    private void showTagManager() {
        List<TrackingManager.TrackedTag> tags = TrackingManager.getAllTags();
        if (tags.isEmpty()) {
            Toast.makeText(requireContext(), R.string.no_tracking_tags, Toast.LENGTH_SHORT).show();
            return;
        }
        List<String> labels = new ArrayList<>();
        for (TrackingManager.TrackedTag tag : tags) {
            labels.add(getString(tag.source == TrackingManager.SOURCE_CLOUD
                            ? R.string.tracking_tag_cloud : R.string.tracking_tag_local)
                    + " · " + (tag.enabled ? getString(R.string.enabled)
                    : getString(R.string.disabled)) + "\n" + tag.name);
        }
        ArrayAdapter<String> tagAdapter = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_list_item_1, labels);
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(R.string.manage_tracking_tags)
                .setMessage(R.string.tracking_tag_long_press_hint)
                .setAdapter(tagAdapter, null)
                .setNegativeButton(android.R.string.cancel, null)
                .create();
        dialog.setOnShowListener(ignored -> {
            ListView listView = dialog.getListView();
            listView.setOnItemLongClickListener((parent, view, position, id) -> {
                showTagActions(tags.get(position), dialog);
                return true;
            });
        });
        dialog.show();
    }

    private void showTagActions(TrackingManager.TrackedTag tag, AlertDialog parent) {
        String toggle = getString(tag.enabled
                ? R.string.pause_tracking_tag : R.string.enable_tracking_tag);
        String[] actions = {toggle, getString(R.string.reset_tracking_baseline),
                getString(R.string.delete_tracking_tag)};
        new AlertDialog.Builder(requireContext())
                .setTitle(tag.name)
                .setItems(actions, (dialog, which) -> {
                    if (which == 0) {
                        TrackingManager.setTagEnabled(tag.name, !tag.enabled);
                    } else if (which == 1) {
                        TrackingManager.resetTagBaseline(tag.name);
                    } else {
                        TrackingManager.deleteTag(tag.name);
                    }
                    parent.dismiss();
                    showTagManager();
                })
                .show();
    }

    private void showFirstRunDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.tracking_first_title)
                .setMessage(R.string.tracking_first_message)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    Settings.putTrackingEnabled(true);
                    TrackingManager.syncCloudTags(requireContext());
                    runCheck();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void showAddTagDialog() {
        EditText input = new EditText(requireContext());
        input.setSingleLine(true);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.add_tracking_tag)
                .setView(input)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    TrackingManager.addLocalTag(input.getText().toString());
                    if (Settings.getTrackingEnabled()) {
                        runCheck();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void runCheck() {
        TrackingManager.check(requireContext(), true, new TrackingManager.Callback() {
            @Override
            public void onSuccess(int added) {
                reload();
                Toast.makeText(requireContext(),
                        getString(R.string.tracking_check_done, added), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(Throwable error) {
                Toast.makeText(requireContext(), R.string.tracking_check_failed,
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void reload() {
        items.clear();
        items.addAll(TrackingManager.getUnread());
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
        if (viewTransition != null) {
            viewTransition.showView(items.isEmpty() ? 1 : 0);
        }
    }

    @Override
    public boolean onItemClick(EasyRecyclerView parent, View view, int position, long id) {
        TrackingManager.TrackedGallery item = items.get(position);
        TrackingManager.markRead(item.info.gid);
        items.remove(position);
        adapter.notifyItemRemoved(position);
        Bundle args = new Bundle();
        args.putString(GalleryDetailScene.KEY_ACTION, GalleryDetailScene.ACTION_GALLERY_INFO);
        args.putParcelable(GalleryDetailScene.KEY_GALLERY_INFO, item.info);
        startScene(new Announcer(GalleryDetailScene.class).setArgs(args));
        if (items.isEmpty()) {
            viewTransition.showView(1);
        }
        return true;
    }

    private final class TrackingAdapter extends RecyclerView.Adapter<TrackingHolder> {
        private final LayoutInflater inflater;

        TrackingAdapter(LayoutInflater inflater) {
            this.inflater = inflater;
            setHasStableIds(true);
        }

        @Override
        public long getItemId(int position) {
            return items.get(position).info.gid;
        }

        @Override
        public TrackingHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new TrackingHolder(inflater.inflate(R.layout.item_history, parent, false));
        }

        @Override
        public void onBindViewHolder(TrackingHolder holder, int position) {
            TrackingManager.TrackedGallery item = items.get(position);
            GalleryInfo info = item.info;
            holder.thumb.load(EhCacheKeyFactory.getThumbKey(info.gid), info.thumb);
            holder.title.setText(EhUtils.getSuitableTitle(info));
            holder.uploader.setText(item.matchedTags.replace('\n', ' '));
            holder.category.setText(EhUtils.getCategory(info.category));
            holder.category.setBackgroundColor(EhUtils.getCategoryColor(info.category));
            holder.posted.setText(info.posted);
            holder.language.setText(info.simpleLanguage);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }
    }

    private static final class TrackingHolder extends RecyclerView.ViewHolder {
        final LoadImageView thumb;
        final TextView title;
        final TextView uploader;
        final TextView category;
        final TextView posted;
        final TextView language;

        TrackingHolder(View itemView) {
            super(itemView);
            thumb = itemView.findViewById(R.id.thumb);
            title = itemView.findViewById(R.id.title);
            uploader = itemView.findViewById(R.id.uploader);
            category = itemView.findViewById(R.id.category);
            posted = itemView.findViewById(R.id.posted);
            language = itemView.findViewById(R.id.simple_language);
            View rating = itemView.findViewById(R.id.rating);
            rating.setVisibility(View.GONE);
        }
    }
}
