package com.tvbrowser.app

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var urlEditText: EditText
    private lateinit var goButton: Button
    private lateinit var pinButton: Button
    private lateinit var pinnedContainer: LinearLayout
    private lateinit var sharedPreferences: SharedPreferences

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sharedPreferences = getSharedPreferences("TVBrowserPrefs", Context.MODE_PRIVATE)

        webView = findViewById(R.id.webView)
        urlEditText = findViewById(R.id.urlEditText)
        goButton = findViewById(R.id.goButton)
        pinButton = findViewById(R.id.pinButton)
        pinnedContainer = findViewById(R.id.pinnedContainer)

        // Setup WebView
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                urlEditText.setText(url)
            }
        }
        webView.webChromeClient = WebChromeClient()
        val webSettings: WebSettings = webView.settings
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true
        webView.setBackgroundColor(Color.TRANSPARENT)

        goButton.setOnClickListener {
            loadUrl(urlEditText.text.toString())
        }

        pinButton.setOnClickListener {
            val currentUrl = webView.url
            if (!currentUrl.isNullOrEmpty() && currentUrl != "about:blank") {
                pinWebsite(currentUrl)
            } else {
                Toast.makeText(this, "Cannot pin this page", Toast.LENGTH_SHORT).show()
            }
        }

        urlEditText.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_GO ||
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
                loadUrl(urlEditText.text.toString())
                true
            } else {
                false
            }
        }

        // Handle back button for WebView navigation
        webView.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_BACK) {
                if (webView.canGoBack()) {
                    webView.goBack()
                    return@setOnKeyListener true
                }
            }
            false
        }

        refreshPinnedSites()

        // Initial Load
        webView.loadUrl("https://www.google.com")
        urlEditText.setText("https://www.google.com")
    }

    private fun loadUrl(query: String) {
        var finalUrl = query.trim()
        if (finalUrl.isNotEmpty()) {
            if (!finalUrl.startsWith("http://") && !finalUrl.startsWith("https://")) {
                if (finalUrl.contains(".") && !finalUrl.contains(" ")) {
                    finalUrl = "https://$finalUrl"
                } else {
                    finalUrl = "https://www.google.com/search?q=${android.net.Uri.encode(finalUrl)}"
                }
            }
            webView.loadUrl(finalUrl)
            webView.requestFocus()
        }
    }

    private fun pinWebsite(url: String) {
        val pinnedSitesStr = sharedPreferences.getString("pinnedSites", "") ?: ""
        val pinnedSites = if (pinnedSitesStr.isEmpty()) mutableListOf() else pinnedSitesStr.split(",").toMutableList()

        if (!pinnedSites.contains(url)) {
            pinnedSites.add(url)
            sharedPreferences.edit().putString("pinnedSites", pinnedSites.joinToString(",")).apply()
            Toast.makeText(this, "Pinned!", Toast.LENGTH_SHORT).show()
            refreshPinnedSites()
        } else {
            Toast.makeText(this, "Already pinned", Toast.LENGTH_SHORT).show()
        }
    }

    private fun removePin(url: String) {
        val pinnedSitesStr = sharedPreferences.getString("pinnedSites", "") ?: ""
        val pinnedSites = if (pinnedSitesStr.isEmpty()) mutableListOf() else pinnedSitesStr.split(",").toMutableList()

        if (pinnedSites.contains(url)) {
            pinnedSites.remove(url)
            sharedPreferences.edit().putString("pinnedSites", pinnedSites.joinToString(",")).apply()
            Toast.makeText(this, "Pin Removed", Toast.LENGTH_SHORT).show()
            refreshPinnedSites()
        }
    }

    private fun refreshPinnedSites() {
        pinnedContainer.removeAllViews()
        val pinnedSitesStr = sharedPreferences.getString("pinnedSites", "") ?: ""
        if (pinnedSitesStr.isEmpty()) {
            pinnedContainer.visibility = View.GONE
            return
        }
        
        pinnedContainer.visibility = View.VISIBLE
        val pinnedSites = pinnedSitesStr.split(",")

        val heightPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 40f, resources.displayMetrics).toInt()
        val marginPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8f, resources.displayMetrics).toInt()
        val paddingPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16f, resources.displayMetrics).toInt()

        for ((index, site) in pinnedSites.withIndex()) {
            val button = Button(this)
            
            // Extract domain for simple name
            var siteName = site.replace("https://", "").replace("http://", "").replace("www.", "")
            if (siteName.contains("/")) {
                siteName = siteName.substring(0, siteName.indexOf("/"))
            }
            button.text = siteName.take(15)
            button.isAllCaps = false

            button.setBackgroundResource(R.drawable.pin_tile_background)
            button.setTextColor(Color.WHITE)
            button.isFocusable = true

            val layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, heightPx)
            layoutParams.marginEnd = marginPx
            button.layoutParams = layoutParams
            button.gravity = Gravity.CENTER
            button.setPadding(paddingPx, 0, paddingPx, 0)

            button.setOnClickListener {
                loadUrl(site)
            }

            button.setOnLongClickListener {
                removePin(site)
                true
            }
            
            button.nextFocusDownId = R.id.webView
            button.nextFocusUpId = R.id.urlEditText
            
            if (index == 0) {
                 button.nextFocusLeftId = R.id.urlEditText
            }

            pinnedContainer.addView(button)
        }
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}
