package com.mitsukova.audible.ui.bookDetail

import android.graphics.Bitmap
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import com.mitsukova.audible.data.model.BookSettingsEntity
import com.mitsukova.audible.data.model.Page
import com.mitsukova.audible.data.repository.bookDetail.WebViewListener
import java.util.Timer
import java.util.TimerTask

class PageAdapter(
    private val pages: List<Page>,
    private val viewPager: ViewPager,
    private val bookSettingsEntity: BookSettingsEntity?,
    private val webViewListener: WebViewListener
) : PagerAdapter() {

    private var autoScrollTimer: Timer? = null

    override fun getCount(): Int {
        return pages.size - 1
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view == `object`
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val webView = createWebView(container)
        val settings = webView.settings
        webView.tag = position
        settings.javaScriptEnabled = true

        webView.clearHistory()
        webView.clearCache(true)


        val customCss = """
        <style>
            body {
                font-size: ${bookSettingsEntity?.fontSize}px;
                line-height: ${bookSettingsEntity?.lineSpacing};
                background-color: ${bookSettingsEntity?.backgroundColor};
                color: ${bookSettingsEntity?.textColor};
            }
        </style>
        """.trimIndent()

        val styledContent = "$customCss${pages[position].content}"
        webView.loadDataWithBaseURL(null, styledContent, "text/html", "UTF-8", null)

        webView.addJavascriptInterface(object {
            @JavascriptInterface
            fun onTextExtracted(text: String) {
                webViewListener.onTextExtracted(text)
                webViewListener.onWebViewCreated(webView)
            }
            @JavascriptInterface
            fun onTextSelected(text: String) {
                webViewListener.onTextSelected(text)
            }
        }, "AndroidInterface")

        webView.webViewClient = object : WebViewClient() {
            var currentPagePosition = -1

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                currentPagePosition = viewPager.currentItem
            }

            override fun onPageFinished(view: WebView?, url: String?) {}
        }

        container.addView(webView)

        startAutoScroll()

        return webView
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        container.removeView(`object` as View)
    }

    fun createWebView(container: ViewGroup): WebView {
        val webView = WebView(container.context)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.cacheMode = WebSettings.LOAD_NO_CACHE

        return webView
    }

    fun getPageCount(): Int {
        return pages.size
    }

    fun startAutoScroll() {
        if (!bookSettingsEntity?.autoScrollEnabled!!) {
            return
        }

        stopAutoScroll()

        val baseInterval = ((bookSettingsEntity.autoScrollSpeed) * 500).toLong()
        val minInterval = 100
        val scrollInterval = maxOf(baseInterval, minInterval.toLong())

        stopAutoScroll()

        autoScrollTimer = Timer()

        autoScrollTimer?.schedule(object : TimerTask() {
            override fun run() {
                viewPager.post {
                    for (i in 0 until viewPager.childCount) {
                        val webView = viewPager.getChildAt(i) as? WebView
                        webView?.evaluateJavascript("window.scrollBy(0, 2)") {}
                    }
                }
            }
        }, scrollInterval, scrollInterval)
    }

    fun stopAutoScroll() {
        autoScrollTimer?.cancel()
        autoScrollTimer = null
    }

    fun getPageIndexByHref(href: String): Int {
        for ((index, page) in pages.withIndex()) {
            if (page.href == href) {
                return index
            }
        }
        return -1
    }
}

