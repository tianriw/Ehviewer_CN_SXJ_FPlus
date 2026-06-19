package com.hippo.ehviewer.ui.scene.live;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.hippo.easyrecyclerview.EasyRecyclerView;
import com.hippo.ehviewer.Settings;
import com.hippo.ehviewer.live.LiveModeData;
import com.hippo.ehviewer.ui.GalleryActivity;
import com.hippo.ehviewer.ui.MainActivity;
import com.hippo.ehviewer.ui.scene.ToolbarScene;
import com.hippo.util.IoThreadPoolExecutor;
import com.tianri.ehviewer_fplus.R;

import java.io.File;

public final class LiveModeScene extends ToolbarScene
        implements EasyRecyclerView.OnItemClickListener {

    @Override
    public int getNavCheckedItem() {
        return R.id.nav_live_mode;
    }

    @Nullable
    @Override
    public View onCreateView3(LayoutInflater inflater, @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.scene_history, container, false);
        View content = view.findViewById(R.id.content);
        EasyRecyclerView recyclerView = content.findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setOnItemClickListener(this);
        recyclerView.setAdapter(new LiveModeAdapter(inflater));
        view.findViewById(R.id.tip).setVisibility(View.GONE);
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setTitle(R.string.live_mode);
    }

    @Override
    public int getMenuResId() {
        return R.menu.scene_live_mode;
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        if (item.getItemId() == R.id.action_exit_live_mode) {
            showExitDialog();
            return true;
        }
        return false;
    }

    private void showExitDialog() {
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(R.string.live_mode_exit_title)
                .setMessage(R.string.live_mode_exit_message)
                .setPositiveButton(R.string.live_mode_hold_to_exit, null)
                .setNegativeButton(android.R.string.cancel, null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnLongClickListener(view -> {
                    Settings.putLiveMode(false);
                    dialog.dismiss();
                    Intent intent = new Intent(requireContext(), MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    return true;
                }));
        dialog.show();
    }

    @Override
    public boolean onItemClick(EasyRecyclerView parent, View view, int position, long id) {
        IoThreadPoolExecutor.Companion.getInstance().execute(() -> {
            try {
                File dir = LiveModeData.ensureGallery(requireContext(), position);
                requireActivity().runOnUiThread(() -> {
                    Intent intent = new Intent(requireContext(), GalleryActivity.class);
                    intent.setAction(GalleryActivity.ACTION_DIR);
                    intent.putExtra(GalleryActivity.KEY_FILENAME, dir.getAbsolutePath());
                    startActivity(intent);
                });
            } catch (Exception e) {
                requireActivity().runOnUiThread(() -> Toast.makeText(
                        requireContext(), R.string.live_mode_open_failed, Toast.LENGTH_SHORT).show());
            }
        });
        return true;
    }

    private static final class LiveModeAdapter extends RecyclerView.Adapter<LiveModeHolder> {
        private final LayoutInflater inflater;

        LiveModeAdapter(LayoutInflater inflater) {
            this.inflater = inflater;
            setHasStableIds(true);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public LiveModeHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new LiveModeHolder(inflater.inflate(R.layout.item_live_gallery, parent, false));
        }

        @Override
        public void onBindViewHolder(LiveModeHolder holder, int position) {
            holder.title.setText(LiveModeData.TITLES[position]);
            holder.tags.setText(LiveModeData.TAGS[position]);
        }

        @Override
        public int getItemCount() {
            return LiveModeData.TITLES.length;
        }
    }

    private static final class LiveModeHolder extends RecyclerView.ViewHolder {
        final TextView title;
        final TextView tags;

        LiveModeHolder(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.title);
            tags = itemView.findViewById(R.id.tags);
        }
    }
}
