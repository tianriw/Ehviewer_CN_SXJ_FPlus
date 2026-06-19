package com.hippo.ehviewer.ui.scene.statistics;

import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.hippo.ehviewer.client.EhUtils;
import com.hippo.ehviewer.stats.StatisticsManager;
import com.hippo.ehviewer.ui.scene.ToolbarScene;
import com.hippo.ehviewer.widget.ContributionView;
import com.tianri.ehviewer_fplus.R;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;

public final class StatisticsScene extends ToolbarScene {

    private int year = Calendar.getInstance().get(Calendar.YEAR);
    private TextView yearView;
    private TextView summaryView;
    private TextView favoriteView;
    private TextView tagsView;
    private ContributionView contributionView;

    @Override
    public int getNavCheckedItem() {
        return R.id.nav_statistics;
    }

    @Nullable
    @Override
    public View onCreateView3(LayoutInflater inflater, @Nullable ViewGroup container,
                              @Nullable android.os.Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.scene_statistics, container, false);
        yearView = view.findViewById(R.id.statistics_year);
        summaryView = view.findViewById(R.id.statistics_summary);
        favoriteView = view.findViewById(R.id.statistics_favorite_gallery);
        tagsView = view.findViewById(R.id.statistics_top_tags);
        contributionView = view.findViewById(R.id.statistics_contributions);
        bind();
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable android.os.Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setTitle(R.string.statistics);
        setNavigationIcon(R.drawable.v_arrow_left_dark_x24);
    }

    @Override
    public int getMenuResId() {
        return R.menu.scene_statistics;
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_statistics_previous_year) {
            year--;
            bind();
            return true;
        } else if (id == R.id.action_statistics_next_year) {
            int current = Calendar.getInstance().get(Calendar.YEAR);
            if (year < current) {
                year++;
                bind();
            }
            return true;
        } else if (id == R.id.action_statistics_clear) {
            new AlertDialog.Builder(requireContext())
                    .setMessage(R.string.clear_statistics_confirm)
                    .setPositiveButton(R.string.clear_all, (dialog, which) -> {
                        StatisticsManager.clear();
                        bind();
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
            return true;
        }
        return false;
    }

    private void bind() {
        if (yearView == null) {
            return;
        }
        StatisticsManager.Snapshot snapshot = StatisticsManager.getSnapshot(year);
        yearView.setText(getString(R.string.statistics_year, year));
        String latest = snapshot.latestTime == 0
                ? getString(R.string.statistics_never)
                : DateFormat.getDateTimeInstance().format(new Date(snapshot.latestTime));
        summaryView.setText(getString(R.string.statistics_summary,
                snapshot.totalViews, snapshot.uniqueGalleries, latest));
        if (snapshot.favoriteGallery == null) {
            favoriteView.setText(R.string.statistics_no_favorite);
        } else {
            favoriteView.setText(getString(R.string.statistics_favorite_gallery,
                    EhUtils.getSuitableTitle(snapshot.favoriteGallery),
                    snapshot.favoriteGalleryViews));
        }
        StringBuilder tags = new StringBuilder(getString(R.string.statistics_top_tags));
        for (StatisticsManager.TagStat tag : snapshot.topTags) {
            tags.append('\n').append(tag.tag).append("  ")
                    .append(tag.views).append(" / ").append(tag.uniqueGalleries);
        }
        tagsView.setText(tags);
        contributionView.setCounts(snapshot.dailyCounts);
    }
}
