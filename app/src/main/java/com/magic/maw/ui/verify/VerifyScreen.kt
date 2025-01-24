package com.magic.maw.ui.verify

import android.annotation.SuppressLint
import android.net.Uri
import android.net.http.SslError
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.magic.maw.MyApp
import com.magic.maw.R
import com.magic.maw.ui.components.RegisterView
import com.magic.maw.ui.components.SourceChangeChecker
import com.magic.maw.util.Logger
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val viewName = "VerifyView"
private val logger = Logger(viewName)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerifyScreen(
    modifier: Modifier = Modifier,
    url: String,
    onSuccess: (String) -> Unit,
    onCancel: () -> Unit,
) {
    val defaultTitle = stringResource(R.string.verification)
    val title = remember { mutableStateOf(defaultTitle) }
    RegisterView(name = viewName)
    SourceChangeChecker(onChanged = onCancel)
    Scaffold(
        modifier = modifier,
        topBar = {
            VerifyTopBar(
                title = title,
                enableShadow = true,
                onFinish = onCancel
            )
        }
    ) { innerPadding ->
        VerifyContent(
            modifier = Modifier.padding(innerPadding),
            url = url,
            onUpdateTitle = { title.value = it },
            onSuccess = onSuccess,
            onCancel = onCancel,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VerifyTopBar(
    modifier: Modifier = Modifier,
    title: MutableState<String>,
    enableShadow: Boolean = true,
    onFinish: () -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    CenterAlignedTopAppBar(
        modifier = modifier.let { if (enableShadow) it.shadow(3.dp) else it },
        title = { Text(title.value) },
        navigationIcon = {
            IconButton(onClick = onFinish) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "",
                )
            }
        },
        scrollBehavior = scrollBehavior
    )
}

@SuppressLint("JavascriptInterface", "SetJavaScriptEnabled")
@Composable
private fun VerifyContent(
    modifier: Modifier = Modifier,
    url: String,
    onUpdateTitle: (String) -> Unit,
    onSuccess: (String) -> Unit,
    onCancel: () -> Unit,
) {
    val progress = remember { mutableFloatStateOf(0f) }
    val webView = remember { mutableStateOf<WebView?>(null) }
    val scope = rememberCoroutineScope()
    DisposableEffect(Unit) {
        onDispose {
            webView.value?.destroy()
            webView.value = null
        }
    }
    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                if (webView.value != null && webView.value != this) {
                    webView.value?.destroy()
                }
                webView.value = this
            }.apply {
                webChromeClient = object : WebChromeClient() {
                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                        progress.floatValue = newProgress / 100f
                    }
                }
                webViewClient = object : WebViewClient() {
                    var isCloudflareChallenge = false

                    @Deprecated("Deprecated in Java")
                    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                        return url?.let { shouldOverrideUrlLoading(Uri.parse(it)) } != false
                    }

                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): Boolean {
                        return request?.let { shouldOverrideUrlLoading(it.url) } != false
                    }

                    private fun shouldOverrideUrlLoading(uri: Uri): Boolean {
                        return uri.scheme != "http" && uri.scheme != "https"
                    }

                    override fun onPageFinished(v: WebView?, url: String?) {
                        val cookieManager = CookieManager.getInstance()
                        url?.let {
                            val cookie = cookieManager.getCookie(it)
                            logger.info("url: $url, cookie: $cookie")
                        }
                        val view = v ?: return
                        val title = view.title ?: return
                        if (title != url && title != view.url && title.isNotBlank()) {
                            onUpdateTitle.invoke(title)
                        }
                        view.evaluateJavascript("!!window._cf_chl_opt") {
                            logger.info("获取完成 ret: $it, host:${Uri.parse(url).host}")
                            if (it == "true") {
                                isCloudflareChallenge = true
                            } else if (isCloudflareChallenge) {
                                scope.launch {
                                    delay(500)
                                    view.evaluateJavascript(VerifyViewDefaults.JSK_CHECK) {}
                                }
                            }
                        }
                    }

                    @SuppressLint("WebViewClientOnReceivedSslError")
                    override fun onReceivedSslError(
                        view: WebView?,
                        handler: SslErrorHandler?,
                        error: SslError?
                    ) {
                        handler?.proceed()
                    }
                }
                settings.apply {
                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    domStorageEnabled = true
                    allowContentAccess = true
                    useWideViewPort = true
                    loadWithOverviewMode = true
                    javaScriptEnabled = true
                    builtInZoomControls = true
                    displayZoomControls = false
                    userAgentString = VerifyViewDefaults.UserAgent
                }
            }
        }) {
            logger.info("reload url: $url")
            val headerMap = hashMapOf(
                "User-Agent" to VerifyViewDefaults.UserAgent,
            )
            it.addJavascriptInterface(JsKit(onSuccess), "jsk")
            it.loadUrl(url, headerMap)
        }

        VerifyProgressBar(progress = progress)

        BackHandler {
            webView.value?.let {
                if (it.canGoBack()) {
                    it.goBack()
                    return@BackHandler
                }
            }
            onCancel()
        }
    }
}

@Composable
private fun VerifyProgressBar(modifier: Modifier = Modifier, progress: MutableFloatState) {
    if (progress.floatValue != 1.0f) {
        LinearProgressIndicator(
            modifier = modifier.fillMaxWidth(),
            progress = { progress.floatValue }
        )
    }
}

private fun getHost(url: String): String? {
    val uri = Uri.parse(url)
    return uri.host
}

private class JsKit(private val onCheck: (String) -> Unit) {
    @Suppress("unused")
    @JavascriptInterface
    fun checkContent(text: String) {
        onCheck.invoke(text)
    }
}

object VerifyViewDefaults {
    val UserAgent: String by lazy {
        WebSettings.getDefaultUserAgent(MyApp.app).apply {
            logger.info("Default UA: $this")
        }
    }
    const val JSK_CHECK = "javascript:jsk.checkContent(document.documentElement.textContent)"
}