/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.activity

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.annotation.NonNull
import android.support.annotation.Nullable
import android.support.v4.app.FragmentManager
import android.util.AttributeSet
import android.view.View
import android.view.WindowManager
import mozilla.components.support.utils.SafeIntent
import org.mozilla.focus.R
import org.mozilla.focus.architecture.NonNullObserver
import org.mozilla.focus.biometrics.Biometrics
import org.mozilla.focus.fragment.BrowserFragment
import org.mozilla.focus.fragment.FirstrunFragment
import org.mozilla.focus.fragment.UrlInputFragment
import org.mozilla.focus.locale.LocaleAwareAppCompatActivity
import org.mozilla.focus.session.Session
import org.mozilla.focus.session.SessionManager
import org.mozilla.focus.session.ui.SessionsSheetFragment
import org.mozilla.focus.settings.ExperimentsSettingsFragment
import org.mozilla.focus.telemetry.SentryWrapper
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.focus.utils.AppConstants
import org.mozilla.focus.utils.ExperimentsSyncService
import org.mozilla.focus.utils.Settings
import org.mozilla.focus.utils.SupportUtils
import org.mozilla.focus.utils.ViewUtils
import org.mozilla.focus.viewmodel.MainViewModel
import org.mozilla.focus.web.IWebView
import org.mozilla.focus.web.WebViewProvider
import android.support.v4.content.ContextCompat
import android.support.v7.widget.Toolbar
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import de.mrapp.android.tabswitcher.*


@Suppress("TooManyFunctions")
open class MainActivity : LocaleAwareAppCompatActivity() {
    private val sessionManager: SessionManager = SessionManager.getInstance()

    protected open val isCustomTabMode: Boolean
        get() = false

    protected open val currentSessionForActivity: Session
        get() = sessionManager.currentSession

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            SentryWrapper.init(this)
        }

        initViewModel()

        if (Settings.getInstance(this).shouldUseSecureMode()) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }

        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN

        setContentView(R.layout.activity_main)

        val intent = SafeIntent(intent)

        if (intent.isLauncherIntent) {
            TelemetryWrapper.openFromIconEvent()
        }

        sessionManager.handleIntent(this, intent, savedInstanceState)

        registerSessionObserver()

        WebViewProvider.preload(this)

        val launchCount = Settings.getInstance(this).getAppLaunchCount()
        PreferenceManager.getDefaultSharedPreferences(this)
                .edit()
                .putInt(getString(R.string.app_launch_count), launchCount + 1)
                .apply()
    }

    private fun initViewModel() {
        val viewModel = ViewModelProviders.of(this).get(MainViewModel::class.java)
        viewModel.getExperimentsLiveData().observe(this, Observer { aBoolean ->
            if (aBoolean!!) {
                val preferenceFragment = ExperimentsSettingsFragment()
                supportFragmentManager
                        .beginTransaction()
                        .replace(R.id.container, preferenceFragment, ExperimentsSettingsFragment.FRAGMENT_TAG)
                        .addToBackStack(null)
                        .commitAllowingStateLoss()
            }
        })

        //Create TAB Switcher
        /*
        val context = this.applicationContext
        val tabSwitcher = TabSwitcher(context)
        tabSwitcher.setBackground(ContextCompat.getColor(context, R.color.background_floating_material_dark))
        tabSwitcher.layoutPolicy = LayoutPolicy.AUTO
        tabSwitcher.setTabIcon(R.drawable.ic_close)
        tabSwitcher.setTabIconTint(ContextCompat.getColor(context, android.R.color.black))
        tabSwitcher.setTabBackgroundColor(ContextCompat.getColor(context, R.color.background_material_dark))
        tabSwitcher.tabContentBackgroundColor = ContextCompat.getColor(context, R.color.phone_tab_content_background_color_dark)
        tabSwitcher.setTabTitleTextColor(ContextCompat.getColor(context, R.color.browser_actions_title_color))
        tabSwitcher.setTabCloseButtonIcon(R.drawable.ic_close)
        tabSwitcher.setTabCloseButtonIcon(ContextCompat.getColor(context, android.R.color.black))
        tabSwitcher.setToolbarTitle("TAB Switch Title")
        tabSwitcher.inflateToolbarMenu(R.menu.menu_browser_context, null)
        tabSwitcher.setToolbarNavigationIcon(R.drawable.abc_btn_default_mtrl_shape, null)
        tabSwitcher.setToolbarNavigationIconTint(ContextCompat.getColor(context, android.R.color.black))


        //Setting custom Decorator
        tabSwitcher.decorator = Decorator()
        */
    }

    private fun registerSessionObserver() {
        (if (isCustomTabMode)
            sessionManager.customTabSessions
        else
            sessionManager.sessions).observe(this, createSessionObserver())
    }

    private fun createSessionObserver() = object : NonNullObserver<List<Session>>() {
        private var wasSessionsEmpty = false

        public override fun onValueChanged(t: List<Session>) {
            // If needed show the first run tour on top of the browser or url input fragment.
            val showFirstrun = Settings.getInstance(this@MainActivity).shouldShowFirstrun()

            if (t.isEmpty()) {
                if (!isCustomTabMode) {
                    // There's no active session. Show the URL input screen so that the user can
                    // start a new session.
                    if (showFirstrun) {
                        showFirstrun()
                    } else {
                        showUrlInputScreen()
                    }
                }

                wasSessionsEmpty = true
            } else {
                // This happens when we move from 0 to 1 sessions: either on startup or after an erase.
                if (wasSessionsEmpty) {
                    WebViewProvider.performNewBrowserSessionCleanup()
                    wasSessionsEmpty = false
                }

                if (showFirstrun) {
                    showFirstrun(currentSessionForActivity)
                } else {
                    // We have at least one session. Show a fragment for the current session.
                    showBrowserScreenForCurrentSession()
                }
            }
        }
    }

    override fun applyLocale() {
        // We don't care here: all our fragments update themselves as appropriate
    }

    override fun onResume() {
        super.onResume()

        TelemetryWrapper.startSession()
        checkBiometricStillValid()

        if (Settings.getInstance(this).shouldUseSecureMode()) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    override fun onPause() {
        if (isFinishing) {
            WebViewProvider.performCleanup(this)
        }

        val fragmentManager = supportFragmentManager
        val browserFragment =
                fragmentManager.findFragmentByTag(BrowserFragment.FRAGMENT_TAG) as BrowserFragment?
        browserFragment?.cancelAnimation()

        val urlInputFragment =
                fragmentManager.findFragmentByTag(UrlInputFragment.FRAGMENT_TAG) as UrlInputFragment?
        urlInputFragment?.cancelAnimation()

        super.onPause()

        TelemetryWrapper.stopSession()
    }

    override fun onStop() {
        super.onStop()

        TelemetryWrapper.stopMainActivity()
        ExperimentsSyncService.scheduleSync(this)
    }

    override fun onNewIntent(unsafeIntent: Intent) {
        val intent = SafeIntent(unsafeIntent)

        if (intent.dataString.equals(SupportUtils.OPEN_WITH_DEFAULT_BROWSER_URL)) {
            openGeneralSettings()
            return
        }

        sessionManager.handleNewIntent(this, intent)

        val action = intent.action

        if (ACTION_OPEN == action) {
            TelemetryWrapper.openNotificationActionEvent()
        }

        if (ACTION_ERASE == action) {
            processEraseAction(intent)
        }

        if (intent.isLauncherIntent) {
            TelemetryWrapper.resumeFromIconEvent()
        }
    }

    private fun processEraseAction(intent: SafeIntent) {
        val fromShortcut = intent.getBooleanExtra(EXTRA_SHORTCUT, false)
        val fromNotification = intent.getBooleanExtra(EXTRA_NOTIFICATION, false)

        SessionManager.getInstance().removeAllSessions()

        if (fromShortcut) {
            TelemetryWrapper.eraseShortcutEvent()
        } else if (fromNotification) {
            TelemetryWrapper.eraseAndOpenNotificationActionEvent()
        }
    }

    private fun showUrlInputScreen() {
        val fragmentManager = supportFragmentManager
        val browserFragment = fragmentManager.findFragmentByTag(BrowserFragment.FRAGMENT_TAG) as BrowserFragment?

        val isShowingBrowser = browserFragment != null

        if (isShowingBrowser) {
            ViewUtils.showBrandedSnackbar(findViewById(android.R.id.content),
                    R.string.feedback_erase,
                    resources.getInteger(R.integer.erase_snackbar_delay))
        }

        // We add the url input fragment to the layout if it doesn't exist yet.
        val transaction = fragmentManager
                .beginTransaction()

        // We only want to play the animation if a browser fragment is added and resumed.
        // If it is not resumed then the application is currently in the process of resuming
        // and the session was removed while the app was in the background (e.g. via the
        // notification). In this case we do not want to show the content and remove the
        // browser fragment immediately.
        val shouldAnimate = isShowingBrowser && browserFragment!!.isResumed

        if (shouldAnimate) {
            if (AppConstants.isGeckoBuild) {
                transaction.setCustomAnimations(0, R.anim.erase_animation_gv)
            } else {
                transaction.setCustomAnimations(0, R.anim.erase_animation)
            }
        }

        transaction
                .replace(R.id.container, UrlInputFragment.createWithoutSession(), UrlInputFragment.FRAGMENT_TAG)
                .commit()
    }

    private fun showFirstrun(currentSession: Session? = null) {
        supportFragmentManager
                .beginTransaction()
                .add(R.id.container, FirstrunFragment.create(currentSession), FirstrunFragment.FRAGMENT_TAG)
                .commit()
    }

    protected fun showBrowserScreenForCurrentSession() {
        val currentSession = currentSessionForActivity
        val fragmentManager = supportFragmentManager

        val fragment = fragmentManager.findFragmentByTag(BrowserFragment.FRAGMENT_TAG) as BrowserFragment?
        if (fragment != null && fragment.session == currentSession) {
            // There's already a BrowserFragment displaying this session.
            return
        }

        fragmentManager
                .beginTransaction()
                .replace(R.id.container, BrowserFragment.createForSession(currentSession), BrowserFragment.FRAGMENT_TAG)
                .commit()
    }

    override fun onCreateView(name: String, context: Context, attrs: AttributeSet): View? {
        return if (name == IWebView::class.java.name) {
            // Inject our implementation of IWebView from the WebViewProvider.
            WebViewProvider.create(this, attrs)
        } else super.onCreateView(name, context, attrs)
    }

    override fun onBackPressed() {
        val fragmentManager = supportFragmentManager

        val sessionsSheetFragment = fragmentManager.findFragmentByTag(
                SessionsSheetFragment.FRAGMENT_TAG) as SessionsSheetFragment?
        if (sessionsSheetFragment != null &&
                sessionsSheetFragment.isVisible &&
                sessionsSheetFragment.onBackPressed()) {
            // SessionsSheetFragment handles back presses itself (custom animations).
            return
        }

        val urlInputFragment = fragmentManager.findFragmentByTag(UrlInputFragment.FRAGMENT_TAG) as UrlInputFragment?
        if (urlInputFragment != null &&
                urlInputFragment.isVisible &&
                urlInputFragment.onBackPressed()) {
            // The URL input fragment has handled the back press. It does its own animations so
            // we do not try to remove it from outside.
            return
        }

        val browserFragment = fragmentManager.findFragmentByTag(BrowserFragment.FRAGMENT_TAG) as BrowserFragment?
        if (browserFragment != null &&
                browserFragment.isVisible &&
                browserFragment.onBackPressed()) {
            // The Browser fragment handles back presses on its own because it might just go back
            // in the browsing history.
            return
        }

        super.onBackPressed()
    }

    // Handles the edge case of a user removing all enrolled prints while auth was enabled
    private fun checkBiometricStillValid() {
        // Disable biometrics if the user is no longer eligible due to un-enrolling fingerprints:
        if (!Biometrics.hasFingerprintHardware(this)) {
            PreferenceManager.getDefaultSharedPreferences(this)
                    .edit().putBoolean(getString(R.string.pref_key_biometric),
                            false).apply()
        }
    }

    companion object {
        const val ACTION_ERASE = "erase"
        const val ACTION_OPEN = "open"

        const val EXTRA_TEXT_SELECTION = "text_selection"
        const val EXTRA_NOTIFICATION = "notification"

        private const val EXTRA_SHORTCUT = "shortcut"

        const val EXPERIMENTS_JOB_ID: Int = 4141
    }


    //ADD SWITCHER!!!!!------------------------
    //PERSONAL DECORATOR
    /*

    internal class Decorator:TabSwitcherDecorator() {
        val viewTypeCount:Int
            get() {
                return 2
            }
        @NonNull
        fun onInflateView(@NonNull inflater:LayoutInflater,
                          @Nullable parent:ViewGroup, viewType:Int):View {

           return  BrowserFragment().view()

        }

        fun onShowTab(@NonNull context:Context, @NonNull tabSwitcher:TabSwitcher,
                      @NonNull view:View, @NonNull tab:Tab, index:Int, viewType:Int,
                      @Nullable savedInstanceState:Bundle) {
        }
        fun getViewType(@NonNull tab:Tab, index:Int):Int {
            val parameters = tab.getParameters()
            return if (parameters != null) parameters.getInt("view_type") else 0
        }
        fun onSaveInstanceState(@NonNull view:View, @NonNull tab:Tab, index:Int,
                                viewType:Int, @NonNull outState:Bundle) {
            // Store the tab's current state in the Bundle outState if necessary
        }
    }

    */
}

