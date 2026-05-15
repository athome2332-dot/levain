package com.nihon.jft

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Message
import android.view.KeyEvent
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        webView = WebView(this)
        setContentView(webView)

        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.allowFileAccess = true
        
        // Ensure popups and new windows open in the same WebView
        webView.settings.setSupportMultipleWindows(true)
        webView.settings.javaScriptCanOpenWindowsAutomatically = true

        webView.webViewClient = object : WebViewClient() {
            @Deprecated("Deprecated in Java")
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                url?.let {
                    if (it.startsWith("http://") || it.startsWith("https://")) {
                        // Force all web links to open inside this WebView
                        return false 
                    } else if (it.startsWith("intent:") || it.startsWith("mailto:") || it.startsWith("tel:")) {
                        // Allow OS to handle special schemes (emails, calls)
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(it))
                            startActivity(intent)
                            return true
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
                return false
            }

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val urlToLoad = request?.url.toString()
                if (urlToLoad.startsWith("http://") || urlToLoad.startsWith("https://")) {
                    // Force all web links to open inside this WebView natively
                    return false
                } else {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(urlToLoad))
                        startActivity(intent)
                        return true
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                return false
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                // Remove target="_blank" from all links to prevent external browsers from trying to catch them
                view?.evaluateJavascript(
                    "javascript:(function() { " +
                            "var links = document.getElementsByTagName('a'); " +
                            "for(var i=0; i<links.length; i++) { " +
                            "links[i].removeAttribute('target'); " +
                            "} " +
                            "})();"
                ) {}
            }
        }
        
        webView.webChromeClient = object : WebChromeClient() {
            override fun onCreateWindow(
                view: WebView?,
                isDialog: Boolean,
                isUserGesture: Boolean,
                resultMsg: Message?
            ): Boolean {
                // Intercept new window requests and load them in our main WebView
                val transport = resultMsg?.obj as WebView.WebViewTransport
                val newWebView = WebView(this@MainActivity)
                newWebView.webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                        webView.loadUrl(request?.url.toString())
                        return true
                    }
                }
                transport.webView = newWebView
                resultMsg.sendToTarget()
                return true
            }
        }

        webView.loadUrl(getString(R.string.website_url))
        
        requestNotificationPermission()
    }

    private fun requestNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && webView.canGoBack()) {
            webView.goBack()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}