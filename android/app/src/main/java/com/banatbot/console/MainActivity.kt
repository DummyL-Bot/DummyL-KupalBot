package com.banatbot.console

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    private lateinit var consoleWebView: WebView
    private lateinit var fbLoginOverlay: LinearLayout
    private lateinit var fbWebView: WebView
    private lateinit var btnFbLogin: Button
    private lateinit var btnServerUrl: Button

    private var pendingCookiesJson: String? = null
    private var autoCapturedThisSession = false

    private val prefs by lazy {
        getSharedPreferences("banat_console_prefs", Context.MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.WHITE)
        }

        // Native top bar for quick In-App Facebook Login & Server URL setup
        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(8), dp(12), dp(8))
            setBackgroundColor(Color.parseColor("#F8FAFC"))
        }

        val titleView = TextView(this).apply {
            text = "Banat Mobile"
            textSize = 15f
            setTextColor(Color.parseColor("#0F172A"))
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }

        btnFbLogin = Button(this).apply {
            text = "FB Login"
            textSize = 12f
            isAllCaps = false
            setOnClickListener { openFacebookLoginView() }
        }

        btnServerUrl = Button(this).apply {
            text = "Server"
            textSize = 12f
            isAllCaps = false
            setOnClickListener { showServerUrlDialog() }
        }

        topBar.addView(titleView)
        topBar.addView(btnFbLogin)
        topBar.addView(btnServerUrl)

        val contentFrame = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        }

        consoleWebView = WebView(this)
        setupConsoleWebView(consoleWebView)

        fbLoginOverlay = buildFacebookLoginOverlay()
        fbLoginOverlay.visibility = View.GONE

        contentFrame.addView(
            consoleWebView,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
        contentFrame.addView(
            fbLoginOverlay,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        root.addView(topBar)
        root.addView(contentFrame)
        setContentView(root)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (fbLoginOverlay.visibility == View.VISIBLE) {
                    if (fbWebView.canGoBack()) {
                        fbWebView.goBack()
                    } else {
                        closeFacebookLoginView()
                    }
                    return
                }
                if (consoleWebView.canGoBack()) {
                    consoleWebView.goBack()
                    return
                }
                finish()
            }
        })

        consoleWebView.loadUrl(getSavedServerUrl())
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupConsoleWebView(webView: WebView) {
        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            cacheMode = WebSettings.LOAD_DEFAULT
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
        }

        webView.addJavascriptInterface(AndroidBridge(), "BanatAndroidBridge")
        webView.webChromeClient = WebChromeClient()
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                return false
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                deliverPendingCookiesToConsole()
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun buildFacebookLoginOverlay(): LinearLayout {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
        }

        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10), dp(6), dp(10), dp(6))
            setBackgroundColor(Color.parseColor("#EFF6FF"))
        }

        val infoText = TextView(this).apply {
            text = "Mag-login sa Facebook para auto-connect"
            textSize = 12f
            setTextColor(Color.parseColor("#1E3A8A"))
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }

        val captureBtn = Button(this).apply {
            text = "Connect Now"
            textSize = 11f
            isAllCaps = false
            setOnClickListener {
                if (!tryCaptureFacebookCookies(forceNotify = true)) {
                    Toast.makeText(
                        this@MainActivity,
                        "Mag-login muna sa Facebook (hinihintay ang c_user at xs).",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        val closeBtn = Button(this).apply {
            text = "Close"
            textSize = 11f
            isAllCaps = false
            setOnClickListener { closeFacebookLoginView() }
        }

        bar.addView(infoText)
        bar.addView(captureBtn)
        bar.addView(closeBtn)

        fbWebView = WebView(this).apply {
            CookieManager.getInstance().setAcceptCookie(true)
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.userAgentString =
                "Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
            webChromeClient = WebChromeClient()
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    return false
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    CookieManager.getInstance().flush()
                    if (!autoCapturedThisSession) {
                        tryCaptureFacebookCookies(forceNotify = false)
                    }
                }
            }
        }

        container.addView(bar)
        container.addView(
            fbWebView,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )
        return container
    }

    private fun openFacebookLoginView() {
        autoCapturedThisSession = false
        fbLoginOverlay.visibility = View.VISIBLE
        fbWebView.loadUrl("https://m.facebook.com/")
    }

    private fun closeFacebookLoginView() {
        fbLoginOverlay.visibility = View.GONE
    }

    private fun tryCaptureFacebookCookies(forceNotify: Boolean): Boolean {
        val cm = CookieManager.getInstance()
        val cookieMap = linkedMapOf<String, String>()
        val urls = listOf(
            "https://www.facebook.com",
            "https://m.facebook.com",
            "https://mbasic.facebook.com"
        )

        for (url in urls) {
            val raw = cm.getCookie(url) ?: continue
            raw.split(";").forEach { part ->
                val idx = part.indexOf('=')
                if (idx > 0) {
                    val key = part.substring(0, idx).trim()
                    val value = part.substring(idx + 1).trim()
                    if (key.isNotEmpty() && value.isNotEmpty()) {
                        cookieMap[key] = value
                    }
                }
            }
        }

        val cUser = cookieMap["c_user"] ?: cookieMap["i_user"]
        val xs = cookieMap["xs"]
        if (cUser.isNullOrBlank() || xs.isNullOrBlank()) {
            return false
        }

        val jsonArr = JSONArray()
        cookieMap.forEach { (k, v) ->
            jsonArr.put(JSONObject().apply {
                put("key", k)
                put("value", v)
            })
        }

        pendingCookiesJson = jsonArr.toString()
        autoCapturedThisSession = true
        closeFacebookLoginView()
        deliverPendingCookiesToConsole()

        if (forceNotify || fbLoginOverlay.visibility != View.VISIBLE) {
            Toast.makeText(
                this,
                "Facebook session captured (UID: $cUser)!",
                Toast.LENGTH_SHORT
            ).show()
        }
        return true
    }

    private fun deliverPendingCookiesToConsole() {
        val payload = pendingCookiesJson ?: return
        val quoted = JSONObject.quote(payload)
        runOnUiThread {
            consoleWebView.evaluateJavascript(
                "if (typeof window.onBanatAndroidCookies === 'function') { window.onBanatAndroidCookies($quoted); }",
                null
            )
        }
    }

    private fun getSavedServerUrl(): String {
        val saved = prefs.getString("server_url", null)?.trim()
        return if (!saved.isNullOrEmpty()) saved else getString(R.string.default_server_url)
    }

    private fun showServerUrlDialog() {
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
            setText(getSavedServerUrl())
            setSelection(text.length)
        }

        AlertDialog.Builder(this)
            .setTitle("Banat Server URL")
            .setMessage("Ilagay ang URL ng naka-host mong Banat Bot server:")
            .setView(input)
            .setPositiveButton("Save & Reload") { _, _ ->
                val nextUrl = input.text.toString().trim()
                if (nextUrl.startsWith("http://") || nextUrl.startsWith("https://")) {
                    prefs.edit().putString("server_url", nextUrl).apply()
                    consoleWebView.loadUrl(nextUrl)
                } else {
                    Toast.makeText(this, "Dapat magsimula sa http:// o https://", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun dp(value: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value.toFloat(),
            resources.displayMetrics
        ).toInt()
    }

    inner class AndroidBridge {
        @JavascriptInterface
        fun openFacebookLogin() {
            runOnUiThread { openFacebookLoginView() }
        }

        @JavascriptInterface
        fun consumeExtractedCookies(): String {
            val value = pendingCookiesJson ?: ""
            pendingCookiesJson = null
            return value
        }

        @JavascriptInterface
        fun peekExtractedCookies(): String {
            return pendingCookiesJson ?: ""
        }
    }
}
