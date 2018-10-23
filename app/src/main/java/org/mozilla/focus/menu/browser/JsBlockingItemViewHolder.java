/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.menu.browser;

import android.content.Context;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;

import org.mozilla.focus.R;
import org.mozilla.focus.fragment.BrowserFragment;
import org.mozilla.focus.utils.Settings;

import mozilla.components.support.utils.ThreadUtils;
import android.content.SharedPreferences;

/* package */ class JsBlockingItemViewHolder extends BrowserMenuViewHolder implements CompoundButton.OnCheckedChangeListener {
    /* package */ static final int LAYOUT_ID = R.layout.menu_no_js_switch;


    //Mario added settings data access here
    private Context context;
    private SharedPreferences preferences;
    private BrowserFragment fragment;


    /* package */ JsBlockingItemViewHolder(View itemView, final BrowserFragment fragment) {
        super(itemView);

        this.fragment = fragment;
        this.context = itemView.getContext();

        this.preferences = PreferenceManager.getDefaultSharedPreferences(context);
        final Switch switchView = itemView.findViewById(R.id.no_js_switch);
       // Boolean pref_no_js = preferences.getBoolean("pref_key_performance_block_javascript", false);

//        Boolean pref_no_js = Boolean.parseBoolean(context.getResources().getString(R.string.pref_key_performance_block_javascript));
        switchView.setChecked(Settings.getInstance(context).shouldBlockJavaScript());


        switchView.setOnCheckedChangeListener(this);

    }



    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        fragment.getSession().setJSDisabled(isChecked);
        // Delay closing the menu and reloading the website a bit so that the user can actually see
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(context.getResources().getString(R.string.pref_key_performance_block_javascript), isChecked);
        editor.commit();


        // the switch change its state.
        ThreadUtils.INSTANCE.postToMainThreadDelayed(new Runnable() {
            @Override
            public void run() {
                getMenu().dismiss();

                fragment.reload();
            }
        }, /* Switch.THUMB_ANIMATION_DURATION */ 250);
    }


}
