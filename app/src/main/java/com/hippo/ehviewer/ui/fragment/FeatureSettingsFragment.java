package com.hippo.ehviewer.ui.fragment;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;

import com.hippo.ehviewer.Settings;
import com.hippo.ehviewer.ui.MainActivity;
import com.tianri.ehviewer_fplus.R;

public final class FeatureSettingsFragment extends BasePreferenceFragmentCompat
        implements Preference.OnPreferenceChangeListener {

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        addPreferencesFromResource(R.xml.feature_settings);
        Preference liveMode = findPreference(Settings.KEY_LIVE_MODE);
        if (liveMode != null) {
            liveMode.setOnPreferenceChangeListener(this);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (Settings.KEY_LIVE_MODE.equals(preference.getKey()) && Boolean.TRUE.equals(newValue)) {
            new AlertDialog.Builder(requireContext())
                    .setTitle(R.string.live_mode_enable_title)
                    .setMessage(R.string.live_mode_enable_message)
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                        Settings.putLiveMode(true);
                        Intent intent = new Intent(requireContext(), MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
            return false;
        }
        return true;
    }
}
