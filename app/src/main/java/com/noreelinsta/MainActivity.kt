package com.noreelinsta

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.preference.PreferenceManager
import com.google.android.material.snackbar.Snackbar
import com.noreelinsta.databinding.ActivityMainBinding
import org.json.JSONObject
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: SharedPreferences

    private var isPageReady = false
    private var lastCustomizationSignature: Int? = null

    private var fileChooserCallback: ValueCallback<Array<Uri>>? = null

    private val pickMediaLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val callback = fileChooserCallback ?: return@registerForActivityResult
            val data = result.data
            val uris: Array<Uri>? = if (result.resultCode == RESULT_OK) {
                when {
                    data?.clipData != null -> {
                        val items = data.clipData!!
                        Array(items.itemCount) { index -> items.getItemAt(index).uri }
                    }
                    data?.data != null -> arrayOf(data.data!!)
                    else -> null
                }
            } else {
                null
            }
            callback.onReceiveValue(uris)
            fileChooserCallback = null
        }

    private val prefListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key.startsWith(PREF_HIDE_PREFIX)) {
                injectCustomizationScript()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        prefs = PreferenceManager.getDefaultSharedPreferences(this)

        setupBackNavigation()
        setupSwipeRefresh()
        setupWebView()

        if (savedInstanceState != null) {
            binding.webView.restoreState(savedInstanceState)
        } else {
            binding.webView.loadUrl(INSTAGRAM_HOME_URL)
        }
    }

    override fun onResume() {
        super.onResume()
        prefs.registerOnSharedPreferenceChangeListener(prefListener)
        injectCustomizationScript()
    }

    override fun onPause() {
        prefs.unregisterOnSharedPreferenceChangeListener(prefListener)
        super.onPause()
    }

    override fun onDestroy() {
        (binding.webView.parent as? ViewGroup)?.removeView(binding.webView)
        binding.webView.destroy()
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        binding.webView.saveState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onCreateOptionsMenu(menu: android.view.Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_refresh -> {
                binding.webView.reload()
                true
            }

            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (binding.webView.canGoBack()) {
                        binding.webView.goBack()
                    } else {
                        finish()
                    }
                }
            }
        )
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setColorSchemeColors(
            ContextCompat.getColor(this, R.color.purple_500),
            ContextCompat.getColor(this, R.color.teal_200)
        )
        binding.swipeRefresh.setOnRefreshListener { binding.webView.reload() }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() = with(binding.webView) {
        WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)
        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            setSupportZoom(false)
            useWideViewPort = false
            loadWithOverviewMode = true
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            userAgentString = MOBILE_USER_AGENT
            javaScriptCanOpenWindowsAutomatically = true
            mediaPlaybackRequiresUserGesture = true
        }

        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(this@with, true)
        }

        webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                if (request == null || !request.isForMainFrame) return false
                return handleExternalNavigation(request.url)
            }

            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                val uri = url?.let { Uri.parse(it) } ?: return false
                return handleExternalNavigation(uri)
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                binding.progressBar.isVisible = true
                binding.progressBar.setProgressCompat(0, false)
                binding.swipeRefresh.isRefreshing = false
                isPageReady = false
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                binding.progressBar.isVisible = false
                binding.swipeRefresh.isRefreshing = false
                isPageReady = true
                injectCustomizationScript(force = true)
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                if (request?.isForMainFrame == true) {
                    binding.progressBar.isVisible = false
                    binding.swipeRefresh.isRefreshing = false
                    Snackbar.make(
                        binding.root,
                        error?.description ?: getString(R.string.webview_error_generic),
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }
        }

        webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                binding.progressBar.isVisible = newProgress in 0..99
                binding.progressBar.setProgressCompat(newProgress, true)
                if (newProgress >= 100) {
                    binding.progressBar.isVisible = false
                }
            }

            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>,
                fileChooserParams: FileChooserParams
            ): Boolean {
                fileChooserCallback?.onReceiveValue(null)
                fileChooserCallback = filePathCallback
                return try {
                    pickMediaLauncher.launch(fileChooserParams.createIntent())
                    true
                } catch (ex: ActivityNotFoundException) {
                    fileChooserCallback = null
                    Toast.makeText(
                        this@MainActivity,
                        getString(R.string.webview_error_generic),
                        Toast.LENGTH_SHORT
                    ).show()
                    false
                }
            }
        }

        setDownloadListener { url, _, _, mimeType, _ ->
            if (url == null) return@setDownloadListener
            try {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse(url)
                    mimeType?.let { putExtra(Intent.EXTRA_MIME_TYPES, arrayOf(it)) }
                }
                startActivity(intent)
            } catch (ex: Exception) {
                Toast.makeText(
                    this@MainActivity,
                    getString(R.string.webview_error_generic),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun handleExternalNavigation(uri: Uri): Boolean {
        val scheme = uri.scheme?.lowercase(Locale.US)
        if (scheme == "intent" || scheme == "mailto" || scheme == "tel") {
            openExternal(uri)
            return true
        }
        val host = uri.host?.lowercase(Locale.US) ?: return false
        return if (INSTAGRAM_HOSTS.any { host == it || host.endsWith(".$it") }) {
            false
        } else {
            openExternal(uri)
            true
        }
    }

    private fun openExternal(uri: Uri) {
        runCatching {
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        }.onFailure {
            Toast.makeText(this, uri.toString(), Toast.LENGTH_SHORT).show()
        }
    }

    private fun injectCustomizationScript(force: Boolean = false) {
        if (!isPageReady) return
        val config = CustomizationConfig(
            hideReels = prefs.getBoolean(PREF_HIDE_REELS, true),
            hideShop = prefs.getBoolean(PREF_HIDE_SHOP, false),
            hideExplore = prefs.getBoolean(PREF_HIDE_EXPLORE, false),
            hideThreads = prefs.getBoolean(PREF_HIDE_THREADS, false)
        )
        val signature = config.hashCode()
        if (!force && signature == lastCustomizationSignature) return
        lastCustomizationSignature = signature
        binding.webView.evaluateJavascript(buildCustomizationScript(config), null)
    }

    private fun buildCustomizationScript(config: CustomizationConfig): String {
        val configJson = JSONObject().apply {
            put("hideReels", config.hideReels)
            put("hideShop", config.hideShop)
            put("hideExplore", config.hideExplore)
            put("hideThreads", config.hideThreads)
        }.toString()

        return """
            (function() {
                if (!window || !document || !document.body) { return; }
                window.__noreelinstaConfig = $configJson;
                const normalizePath = (value) => {
                    if (!value) { return ""; }
                    try {
                        const url = new URL(value, window.location.origin);
                        let normalized = url.pathname;
                        if (!normalized.endsWith("/")) {
                            normalized += "/";
                        }
                        return normalized.toLowerCase();
                    } catch (e) {
                        if (value.startsWith("/")) {
                            return (value.endsWith("/") ? value : value + "/").toLowerCase();
                        }
                        return ("/" + value + "/").toLowerCase();
                    }
                };
                const registry = [
                    { enabled: window.__noreelinstaConfig.hideReels, path: "/reels/" },
                    { enabled: window.__noreelinstaConfig.hideShop, path: "/shop/" },
                    { enabled: window.__noreelinstaConfig.hideExplore, path: "/explore/" },
                    { enabled: window.__noreelinstaConfig.hideThreads, path: "/threads_app/" }
                ];
                const mark = (node, hide) => {
                    if (!node) { return; }
                    const target = node.closest('a, button, div[role="link"], div[role="button"], li, span') || node;
                    if (!target) { return; }
                    if (hide) {
                        if (!target.dataset.noreelinstaHidden) {
                            target.dataset.noreelinstaHidden = "1";
                            target.style.setProperty("display", "none", "important");
                        }
                    } else if (target.dataset.noreelinstaHidden) {
                        target.style.removeProperty("display");
                        delete target.dataset.noreelinstaHidden;
                    }
                };
                const apply = () => {
                    const anchors = Array.from(document.querySelectorAll('a[href]'));
                    registry.forEach((rule) => {
                        const normalized = normalizePath(rule.path);
                        anchors.forEach((anchor) => {
                            const href = anchor.getAttribute("href");
                            if (!href) { return; }
                            if (normalizePath(href) === normalized) {
                                mark(anchor, !!rule.enabled);
                            }
                        });
                    });
                };
                if (!window.__noreelinstaObserver) {
                    window.__noreelinstaObserver = new MutationObserver(() => apply());
                    window.__noreelinstaObserver.observe(document.body, { childList: true, subtree: true });
                }
                apply();
            })();
        """.trimIndent()
    }

    private data class CustomizationConfig(
        val hideReels: Boolean,
        val hideShop: Boolean,
        val hideExplore: Boolean,
        val hideThreads: Boolean
    )

    companion object {
        private const val INSTAGRAM_HOME_URL = "https://www.instagram.com/"
        private const val PREF_HIDE_PREFIX = "pref_hide_"
        const val PREF_HIDE_REELS = "pref_hide_reels"
        const val PREF_HIDE_SHOP = "pref_hide_shop"
        const val PREF_HIDE_EXPLORE = "pref_hide_explore"
        const val PREF_HIDE_THREADS = "pref_hide_threads"

        private val INSTAGRAM_HOSTS = setOf(
            "instagram.com",
            "www.instagram.com",
            "m.instagram.com",
            "l.instagram.com",
            "cdninstagram.com",
            "static.cdninstagram.com"
        )

        private const val MOBILE_USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14; NoReelInsta Build/UPB5.230623.003; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/124.0.0.0 Mobile Safari/537.36"
    }
}
