/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.session.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.preference.PreferenceManager
import android.support.v7.content.res.AppCompatResources
import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.TextView
import org.mozilla.focus.R
import org.mozilla.focus.session.SessionManager
import org.mozilla.focus.session.Source
import org.mozilla.focus.telemetry.TelemetryWrapper

class AddNewTabViewHolder(private val fragment: SessionsSheetFragment, itemView: View) : RecyclerView.ViewHolder(itemView),
    View.OnClickListener {

    init {

        val textView = itemView as TextView
        val leftDrawable = AppCompatResources.getDrawable(itemView.getContext(), R.drawable.ic_tab_new)
        textView.setCompoundDrawablesWithIntrinsicBounds(leftDrawable, null, null, null)
        textView.setOnClickListener(this)
    }

    override fun onClick(view: View) {
        TelemetryWrapper.eraseInTabsTrayEvent()

        fragment.animateAndDismiss().addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                SessionManager.getInstance().selectSession(SessionManager.getInstance()
                        .createNewTabSession(Source.MENU, "stampy:newtab", itemView.context))
                TelemetryWrapper.openLinkInNewTabEvent()
                PreferenceManager.getDefaultSharedPreferences(itemView.context).edit()
                        .putBoolean(
                                itemView.context!!.getString(R.string.has_opened_new_tab),
                                true
                        ).apply()
            }
        })
    }

    companion object {
        const val LAYOUT_ID = R.layout.item_new_tab
    }
}
