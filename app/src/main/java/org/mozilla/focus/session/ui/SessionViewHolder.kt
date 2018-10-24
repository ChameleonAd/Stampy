/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.session.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.support.v7.widget.RecyclerView
import android.text.TextUtils
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.TextView
import org.mozilla.focus.R
import org.mozilla.focus.session.Session
import org.mozilla.focus.session.SessionManager
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.focus.ext.beautifyUrl
import java.lang.ref.WeakReference
import android.support.constraint.solver.widgets.WidgetContainer.getBounds
import android.util.Log
import android.view.View.OnTouchListener



class SessionViewHolder internal constructor(
    private val fragment: SessionsSheetFragment,
    private val textView: TextView
) : RecyclerView.ViewHolder(textView), OnTouchListener {
    companion object {
        @JvmField
        internal val LAYOUT_ID = R.layout.item_session
    }

    private var sessionReference: WeakReference<Session> = WeakReference<Session>(null)

    init {
       // textView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_link, 0, 0, 0)
        //textView.setOnClickListener(this)
        textView.setOnTouchListener(this)
    }

    fun bind(session: Session) {
        this.sessionReference = WeakReference(session)

        updateTitle(session)

        val isCurrentSession = SessionManager.getInstance().isCurrentSession(session)

        updateTextBackgroundColor(isCurrentSession)
    }

    private fun updateTextBackgroundColor(isCurrentSession: Boolean) {
        val drawable = if (isCurrentSession) {
            R.drawable.background_list_item_current_session
        } else {
            R.drawable.background_list_item_session
        }
        textView.setBackgroundResource(drawable)
    }

    private fun updateTitle(session: Session) {
        textView.text =
                if (TextUtils.isEmpty(session.pageTitle.value)) session.url.value.beautifyUrl()
                else session.pageTitle.value
    }
/*
    override fun onClick(view: View) {
       //val session = sessionReference.get() ?: return
        //selectSession(session)
        //print("HEREREEEE")
    }
*/


    override fun onTouch(v: View, event: MotionEvent): Boolean {
        Log.d("", "onTouch entered");
        if (event.action == MotionEvent.ACTION_DOWN) {
            Log.d("", "ACTION_DOWN");
            v.performClick()
            val session = sessionReference.get()

            if(session==null) {
                return false
            }


            if (event.rawX >= (textView.getRight() - textView.compoundDrawablesRelative[2].bounds.width())*1.2 ) {
                SessionManager.getInstance().removeRegularSession(session.uuid)
                TelemetryWrapper.switchTabInTabsTrayEvent()
            } else {
                selectSession(session)
            }
            return true
        }
        return false
    }


    private fun selectSession(session: Session) {
        fragment.animateAndDismiss().addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                SessionManager.getInstance().selectSession(session)

                TelemetryWrapper.switchTabInTabsTrayEvent()
            }
        })
    }
}
