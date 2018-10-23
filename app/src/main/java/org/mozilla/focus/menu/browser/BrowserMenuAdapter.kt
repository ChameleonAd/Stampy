/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.menu.browser

import android.app.PendingIntent
import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup

import org.mozilla.focus.R
import org.mozilla.focus.customtabs.CustomTabConfig
import org.mozilla.focus.fragment.BrowserFragment

import org.mozilla.focus.utils.Browsers
import org.mozilla.focus.utils.HardwareUtils

import java.lang.ref.WeakReference

class BrowserMenuAdapter(
        private val context: Context,
        private val menu: BrowserMenu,
        private val fragment: BrowserFragment,
        customTabConfig: CustomTabConfig?
) : RecyclerView.Adapter<BrowserMenuViewHolder>() {
    sealed class MenuItem {
        open val viewType = 0

        //TODO: full page, menu tab under url bar as layout
        open class Default(val id: Int, val label: String, val drawableResId: Int) : MenuItem() {
            override val viewType = MenuItemViewHolder.LAYOUT_ID
        }

        class Custom(
                id: Int,
                label: String,
                drawableResId: Int,
                val pendingIntent: PendingIntent
        ) : Default(id, label, drawableResId) {
            override val viewType = CustomTabMenuItemViewHolder.LAYOUT_ID
        }

        object Navigation : MenuItem() {
            override val viewType = NavigationItemViewHolder.LAYOUT_ID
        }

        object BlockingSwitch : MenuItem() {
            override val viewType = BlockingItemViewHolder.LAYOUT_ID
        }

        object RequestDesktopCheck : MenuItem() {
            override val viewType = RequestDesktopCheckItemViewHolder.LAYOUT_ID
        }


        //Add No JS Switch
        object NoJSSwitch : MenuItem() {
            override val viewType = JsBlockingItemViewHolder.LAYOUT_ID
        }

        //Add No Media Switch
        object NoMedia : MenuItem() {
            override val viewType = MediaBlockingItemViewHolder.LAYOUT_ID
        }
    }

    private var items = mutableListOf<MenuItem>()
    private var navigationItemViewHolderReference = WeakReference<NavigationItemViewHolder>(null)
    private var blockingItemViewHolderReference = WeakReference<BlockingItemViewHolder>(null)
    private var noJstemViewHolderReference = WeakReference<JsBlockingItemViewHolder>(null)
    private var noMediaViewHolderReference = WeakReference<MediaBlockingItemViewHolder>(null)

    init {
        initializeMenu(fragment.url, customTabConfig)
    }

    private fun initializeMenu(url: String, customTabConfig: CustomTabConfig?) {
        val resources = context.resources
        val browsers = Browsers(context, url)

        if (shouldShowButtonToolbar()) {
            items.add(MenuItem.Navigation)
        }
        //TODO: Add Switch

        //TODO: Add icons

        //Add No Ads
        items.add(MenuItem.BlockingSwitch)

        //Add NoJS
        items.add(MenuItem.NoJSSwitch)

        //Add NoImage
        items.add(MenuItem.NoMedia)

        //Add Share
        if (customTabConfig == null) {
            items.add(

                    MenuItem.Default(
                            R.id.share, resources.getString(R.string.menu_share),
                            R.drawable.ic_share
                    )
            )
        }


        //DO NOT NEED ANY OF THE FOLLOWING

        /*
       items.add(
           MenuItem.Default(
               R.id.add_to_homescreen,
               resources.getString(R.string.menu_add_to_home_screen),
               R.drawable.ic_home
           )
        )
        */

        /*
        items.add(
            MenuItem.Default(
                R.id.find_in_page,
                resources.getString(R.string.find_in_page),
                R.drawable.ic_search
            )
        )
        */

/*
        if (browsers.hasMultipleThirdPartyBrowsers(context)) {

            items.add(
                    MenuItem.Default(
                            R.id.open_select_browser, resources.getString(
                            R.string.menu_open_with_a_browser2
                    ), R.drawable.ic_open_in
                    )
            )
        }
*/

/*
        if (customTabConfig != null) {
            // "Open in Firefox Focus" to switch from a custom tab to the full-featured browser
            val appName = resources.getString(R.string.app_name)
            val label = resources.getString(R.string.menu_open_with_default_browser2, appName)
            val menuItem = MenuItem.Default(R.id.open_in_firefox_focus, label, 0)

            items.add(menuItem)
        }

*/
//Remove open IN

        if (browsers.hasThirdPartyDefaultBrowser(context)) {

          /*
          items.add(
                    MenuItem.Default(
                            R.id.open_default, resources.getString(
                            R.string.menu_open_with_default_browser2,
                            browsers.defaultBrowser!!.loadLabel(
                                    context.packageManager
                            )
                    ), 0
                    )
            )
            */
        }


        //items.add(MenuItem.RequestDesktopCheck)

        /*
        if (customTabConfig == null) {
            // There’s no need for Settings in a custom tab.
            // The user can go to the browser app itself in order to do this.
            items.add(
                    MenuItem.Default(
                            R.id.settings, resources.getString(R.string.menu_settings),
                            R.drawable.ic_settings
                    )
            )

        }*/


        /*
        if (AppConstants.isGeckoBuild) {
            // "Report Site Issue" is available for builds using GeckoView only
            items.add(
                    MenuItem.Default(
                            R.id.report_site_issue,
                            resources.getString(R.string.menu_report_site_issue),
                            0
                    )
            )
        }
        */


        /*
        if (customTabConfig != null) {
            val customTabItems = customTabConfig.menuItems
                    .map { MenuItem.Custom(R.id.custom_tab_menu_item, it.name, 0, it.pendingIntent) }
            items.addAll(customTabItems)
        }
        */
    }


        fun updateTrackers(trackers: Int) {
            val navigationItemViewHolder = blockingItemViewHolderReference.get() ?: return
            navigationItemViewHolder.updateTrackers(trackers)
        }

        fun updateLoading(loading: Boolean) {
            val navigationItemViewHolder = navigationItemViewHolderReference.get() ?: return
            navigationItemViewHolder.updateLoading(loading)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BrowserMenuViewHolder {
            val inflater = LayoutInflater.from(parent.context)

            return when (viewType) {
                NavigationItemViewHolder.LAYOUT_ID -> {
                    val navigationItemViewHolder = NavigationItemViewHolder(
                            inflater.inflate(R.layout.menu_navigation, parent, false), fragment
                    )
                    navigationItemViewHolderReference = WeakReference(navigationItemViewHolder)
                    navigationItemViewHolder
                }
                BlockingItemViewHolder.LAYOUT_ID -> {
                    val blockingItemViewHolder = BlockingItemViewHolder(
                            inflater.inflate(R.layout.menu_blocking_switch, parent, false), fragment
                    )
                    blockingItemViewHolderReference = WeakReference(blockingItemViewHolder)
                    blockingItemViewHolder
                }
                JsBlockingItemViewHolder.LAYOUT_ID -> {
                    val blockingItemViewHolder = JsBlockingItemViewHolder(
                            inflater.inflate(R.layout.menu_no_js_switch, parent, false), fragment
                    )
                    noJstemViewHolderReference = WeakReference(blockingItemViewHolder)
                    blockingItemViewHolder
                }

                MediaBlockingItemViewHolder.LAYOUT_ID -> {
                    val blockingItemViewHolder = MediaBlockingItemViewHolder(
                            inflater.inflate(R.layout.menu_no_image_switch, parent, false), fragment
                    )
                    noMediaViewHolderReference = WeakReference(blockingItemViewHolder)
                    blockingItemViewHolder
                }


                RequestDesktopCheckItemViewHolder.LAYOUT_ID -> {
                    RequestDesktopCheckItemViewHolder(
                            inflater.inflate(R.layout.request_desktop_check_menu_item, parent, false),
                            fragment
                    )
                }
                MenuItemViewHolder.LAYOUT_ID -> MenuItemViewHolder(
                        inflater.inflate(
                                R.layout.menu_item,
                                parent,
                                false
                        )
                )
                CustomTabMenuItemViewHolder.LAYOUT_ID -> CustomTabMenuItemViewHolder(
                        inflater.inflate(R.layout.custom_tab_menu_item, parent, false)
                )
                else -> throw IllegalArgumentException("Unknown view type: $viewType")
            }
        }

        override fun onBindViewHolder(holder: BrowserMenuViewHolder, position: Int) {
            holder.menu = menu
            holder.setOnClickListener(fragment)

            val item = items[position]
            when (item) {
                is MenuItem.Custom -> (holder as CustomTabMenuItemViewHolder).bind(item)
                is MenuItem.Default -> (holder as MenuItemViewHolder).bind(item)
            }
        }

        override fun getItemViewType(position: Int): Int = items[position].viewType
        override fun getItemCount(): Int = items.size

        // On phones we show an extra row with toolbar items (forward/refresh)
        private fun shouldShowButtonToolbar(): Boolean = !HardwareUtils.isTablet(context)
    }