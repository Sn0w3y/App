package com.example.soltegra

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.*
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.example.soltegra.ui.theme.SoltegraTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SoltegraTheme {
                WebViewPage(url = "https://portal.soltegra.io")
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewPage(url: String) {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                webViewClient = SoltegraWebViewClient(context)
                loadUrl(url)
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

class SoltegraWebViewClient(private val context: android.content.Context) : WebViewClient() {

    private var lastRequest: WebResourceRequest? = null

    override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
        lastRequest = request
        return super.shouldInterceptRequest(view, request)
    }

    override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
        super.onPageStarted(view, url, favicon)
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        lastRequest?.let { request ->
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val urlConnection = URL(request.url.toString()).openConnection() as HttpURLConnection
                    urlConnection.connect()
                    if (urlConnection.responseCode != 200) {
                        view?.post {
                            view.loadUrl("file:///android_asset/error.html")
                        }
                    }
                    urlConnection.disconnect()
                } catch (e: Exception) {
                    view?.post {
                        view.loadUrl("file:///android_asset/error.html")
                    }
                }
            }
        }
    }

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        val url = request?.url.toString()
        return if (Uri.parse(url).host == "portal.soltegra.io") {
            view?.loadUrl(url)
            false
        } else if (url.startsWith("file:///android_asset/")) {
            view?.loadUrl(url)
            false
        } else {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
            true
        }
    }

    override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
        super.onReceivedError(view, request, error)
        view?.loadUrl("file:///android_asset/error.html")
    }
}
