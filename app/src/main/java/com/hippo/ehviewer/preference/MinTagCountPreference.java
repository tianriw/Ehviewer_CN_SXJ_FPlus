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

package com.hippo.ehviewer.preference;

import android.content.Context;
import android.text.InputType;
import android.util.AttributeSet;
import android.view.View;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;

import com.hippo.ehviewer.Settings;
import com.hippo.lib.yorozuya.NumberUtils;
import com.hippo.lib.yorozuya.ViewUtils;
import com.hippo.preference.DialogPreference;
import com.tianri.ehviewer_fplus.R;

public class MinTagCountPreference extends DialogPreference {

    private EditText mEditText;

    public MinTagCountPreference(Context context) {
        super(context);
        init();
    }

    public MinTagCountPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MinTagCountPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setDialogLayoutResource(R.layout.dialog_edittext_builder);
        setNegativeButtonText(android.R.string.cancel);
        updateSummary();
    }

    private void updateSummary() {
        int value = Settings.getMinTagCount();
        setSummary(value > 0
                ? getContext().getString(R.string.settings_eh_min_tag_count_summary, value)
                : getContext().getString(R.string.settings_eh_min_tag_count_summary_off));
    }

    @Override
    protected boolean needInputMethod() {
        return true;
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        super.onPrepareDialogBuilder(builder);
        builder.setPositiveButton(android.R.string.ok, this);
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
        mEditText = (EditText) ViewUtils.$$(view, R.id.edit_text);
        mEditText.setInputType(InputType.TYPE_CLASS_NUMBER);
        int value = Settings.getMinTagCount();
        mEditText.setText(value > 0 ? Integer.toString(value) : "");
        mEditText.setSelection(mEditText.getText().length());
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        if (positiveResult && mEditText != null) {
            int value = NumberUtils.parseIntSafely(mEditText.getText().toString().trim(), 0);
            Settings.putMinTagCount(value);
            updateSummary();
        }
        mEditText = null;
    }
}
