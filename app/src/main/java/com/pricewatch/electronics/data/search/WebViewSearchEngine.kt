package com.pricewatch.electronics.data.search

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.JavascriptInterface
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.pricewatch.electronics.core.common.Result
import com.pricewatch.electronics.core.common.SearchText
import com.pricewatch.electronics.core.network.ProviderPrice
import com.pricewatch.electronics.domain.models.Site
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Searches the shops' own websites from inside the app, using the phone's
 * real browser engine (WebView). The shops live in the backend — the user
 * never types a URL. A shop that blocks the load, shows a bot check, or
 * times out is reported as failed, never faked.
 *
 * WebView is used because retail sites block plain HTTP clients (datacentre
 * IPs, non-browser TLS) but serve their public search pages to a real
 * browser on the user's own connection — the same pages the user browses
 * on their phone. No login, CAPTCHA solving, or access control is bypassed:
 * if a shop shows a check, the load fails and the user can open the shop
 * in their browser instead.
 */
@Singleton
class WebViewSearchEngine @Inject constructor(
    @ApplicationContext private val context: Context
) {

    data class RawItem(
        val title: String,
        val priceText: String,
        val url: String,
        val imageUrl: String?
    )

    private val desktopUa =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36"
    private val loadTimeoutMs = 15_000L
    private val settleDelayMs = 2_500L

    suspend fun searchSite(site: Site, config: SiteSearchConfig, query: String): Result<List<RawItem>> {
        val url = SearchText.buildSearchUrl(config.searchUrlTemplate, query)
        return when (val loaded = loadAndExtract(url, searchJs(config))) {
            is Result.Success -> {
                try {
                    val items = parseItems(loaded.data)
                    Result.Success(items)
                } catch (e: Exception) {
                    Result.Error("Could not read results from ${site.name}.", e)
                }
            }
            is Result.Error -> Result.Error(loaded.message, loaded.cause)
            Result.Loading -> Result.Error("Search failed for ${site.name}.")
        }
    }

    /** Current price of a single product page, for Check Now on tracked products. */
    suspend fun fetchPrice(url: String, site: Site): Result<ProviderPrice> {
        val config = SiteSearchConfigs.forSite(site.id)
        return when (val loaded = loadAndExtract(url, priceJs(config?.productPriceSelector))) {
            is Result.Success -> {
                val priceText = try {
                    JSONObject(loaded.data).optString("priceText").takeIf { it.isNotBlank() }
                } catch (e: Exception) {
                    null
                }
                val minor = priceText?.let { SearchText.parsePriceToMinor(it) }
                if (minor != null) {
                    Result.Success(ProviderPrice(minor, "GBP", null))
                } else {
                    Result.Error("Price not found on the ${site.name} page.")
                }
            }
            is Result.Error -> Result.Error(loaded.message, loaded.cause)
            Result.Loading -> Result.Error("Price check failed for ${site.name}.")
        }
    }

    private fun parseItems(json: String): List<RawItem> {
        val out = mutableListOf<RawItem>()
        val arr: JSONArray = try {
            JSONObject(json).optJSONArray("items") ?: JSONArray()
        } catch (e: Exception) {
            return out
        }
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val title = o.optString("title").trim()
            val priceText = o.optString("priceText")
            val url = o.optString("url")
            if (title.isBlank() || priceText.isBlank() || url.isBlank()) continue
            out += RawItem(title, priceText, url, o.optString("imageUrl").takeIf { it.isNotBlank() })
        }
        return out
    }

    /**
     * Loads [url] in a throwaway WebView on the main thread, runs [js],
     * and returns whatever the page hands back through the bridge.
     */
    @SuppressLint("SetJavaScriptEnabled")
    private suspend fun loadAndExtract(url: String, js: String): Result<String> =
        withContext(Dispatchers.Main) {
            val webView = WebView(context)
            try {
                webView.settings.javaScriptEnabled = true
                webView.settings.domStorageEnabled = true
                webView.settings.loadsImagesAutomatically = false
                webView.settings.blockNetworkImage = true
                webView.settings.userAgentString = desktopUa
                withTimeoutOrNull(loadTimeoutMs) {
                    suspendCancellableCoroutine<Result<String>> { cont ->
                        val bridge = ResultBridge(cont)
                        webView.addJavascriptInterface(bridge, "PriceWatchBridge")
                        webView.webViewClient = object : WebViewClient() {
                            private var extractionStarted = false

                            override fun onPageFinished(view: WebView, url: String) {
                                if (extractionStarted) return
                                extractionStarted = true
                                view.postDelayed(
                                    { view.evaluateJavascript("javascript:$js", null) },
                                    settleDelayMs
                                )
                            }

                            override fun onReceivedError(
                                view: WebView,
                                request: WebResourceRequest,
                                error: WebResourceError
                            ) {
                                if (request.isForMainFrame && cont.isActive) {
                                    cont.resume(Result.Error("Could not load the shop page."))
                                }
                            }

                            @Deprecated("Deprecated in Java")
                            override fun onReceivedError(
                                view: WebView,
                                errorCode: Int,
                                description: String?,
                                failingUrl: String?
                            ) {
                                if (cont.isActive) {
                                    cont.resume(Result.Error("Could not load the shop page."))
                                }
                            }

                            override fun onReceivedHttpError(
                                view: WebView,
                                request: WebResourceRequest,
                                errorResponse: android.webkit.WebResourceResponse
                            ) {
                                if (request.isForMainFrame && cont.isActive) {
                                    cont.resume(
                                        Result.Error("Shop returned HTTP ${errorResponse.statusCode}.")
                                    )
                                }
                            }
                        }
                        cont.invokeOnCancellation { webView.stopLoading() }
                        webView.loadUrl(url)
                    }
                } ?: Result.Error("Timed out loading the shop page.")
            } catch (e: Exception) {
                Result.Error(e.message ?: "Could not load the shop page.", e)
            } finally {
                try {
                    webView.stopLoading()
                } catch (_: Exception) {
                }
                try {
                    webView.destroy()
                } catch (_: Exception) {
                }
            }
        }

    private class ResultBridge(
        private val cont: CancellableContinuation<Result<String>>
    ) {
        @JavascriptInterface
        fun onResult(json: String) {
            if (cont.isActive) cont.resume(Result.Success(json))
        }
    }

    private fun jsString(value: String?): String =
        if (value == null) "null" else JSONObject.quote(value)

    private fun searchJs(config: SiteSearchConfig): String {
        val js = """
            (function() {
              function txt(el){ return el ? (el.innerText || '').replace(/\s+/g,' ').trim() : ''; }
              function abs(u){ if(!u) return ''; try { return new URL(u, location.href).href; } catch(e){ return ''; } }
              var items = [];
              function push(title, priceText, url, img){
                if(!title || title.length < 8 || !priceText || !url) return;
                if(url.indexOf('http') !== 0) return;
                items.push({title:title, priceText:priceText, url:url, imageUrl:img||null});
              }
              var ITEM_SEL = __ITEM_SEL__;
              if (ITEM_SEL) {
                var nodes = document.querySelectorAll(ITEM_SEL);
                for (var i=0;i<nodes.length;i++){
                  var el = nodes[i];
                  var tEl = __TITLE_SEL__ ? el.querySelector(__TITLE_SEL__) : null;
                  var pEl = __PRICE_SEL__ ? el.querySelector(__PRICE_SEL__) : null;
                  var aEl = __LINK_SEL__ ? el.querySelector(__LINK_SEL__) : el;
                  var imEl = el.querySelector('img');
                  var href = aEl ? (aEl.getAttribute('href') || aEl.href || '') : '';
                  push(txt(tEl), txt(pEl), abs(href), imEl ? (imEl.getAttribute('src') || '') : null);
                }
              }
              if (items.length === 0) {
                var seen = {};
                var links = document.querySelectorAll('a[href]');
                for (var j=0;j<links.length;j++){
                  var a = links[j];
                  var href2 = abs(a.getAttribute('href') || '');
                  if(!href2 || seen[href2]) continue;
                  var node = a, priceText = null, hops = 0;
                  while (node && hops < 5) {
                    var m = (node.innerText || '').match(/£\s?[\d,]+(?:\.\d{1,2})?/);
                    if (m) { priceText = m[0]; break; }
                    node = node.parentElement; hops++;
                  }
                  if(!priceText) continue;
                  var title = txt(a);
                  var img = a.querySelector('img');
                  if((!title || title.length < 8) && img && img.alt) title = (img.alt||'').replace(/\s+/g,' ').trim();
                  if(!title || title.length < 8) continue;
                  seen[href2] = true;
                  push(title, priceText, href2, img ? (img.getAttribute('src')||'') : null);
                }
              }
              var out = [], seenU = {};
              for (var k=0;k<items.length;k++){ var it=items[k]; if(!seenU[it.url]){ seenU[it.url]=1; out.push(it); } }
              PriceWatchBridge.onResult(JSON.stringify({items: out.slice(0, 30)}));
            })()
        """.trimIndent()
        return js
            .replace("__ITEM_SEL__", jsString(config.itemSelector))
            .replace("__TITLE_SEL__", jsString(config.titleSelector))
            .replace("__PRICE_SEL__", jsString(config.priceSelector))
            .replace("__LINK_SEL__", jsString(config.linkSelector))
    }

    private fun priceJs(priceSelector: String?): String {
        val js = """
            (function() {
              function priceOf(text){ var m=(text||'').match(/£\s?([\d,]+(?:\.\d{1,2})?)/); return m?m[0]:null; }
              var sel = __PRICE_SEL__;
              if (sel) { var el = document.querySelector(sel); if (el) { var p = priceOf(el.innerText); if (p) { PriceWatchBridge.onResult(JSON.stringify({priceText:p})); return; } } }
              var text = document.body ? (document.body.innerText || '') : '';
              var all = text.match(/£\s?[\d,]+(?:\.\d{1,2})?/g) || [];
              if (!all.length) { PriceWatchBridge.onResult(JSON.stringify({priceText:null})); return; }
              var counts = {};
              for (var i=0;i<all.length;i++){ counts[all[i]] = (counts[all[i]]||0)+1; }
              var best=null, bestN=0;
              for (var k in counts){ if(counts[k]>bestN){bestN=counts[k];best=k;} }
              if (bestN < 2) { PriceWatchBridge.onResult(JSON.stringify({priceText:null})); return; }
              PriceWatchBridge.onResult(JSON.stringify({priceText:best}));
            })()
        """.trimIndent()
        return js.replace("__PRICE_SEL__", jsString(priceSelector))
    }
}
