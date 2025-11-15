# Add project specific ProGuard rules here.
# You can control the Kotlin metadata that is kept, shrinker settings, etc.

# Keep WebView clients to avoid reflection issues.
-keepclassmembers class * extends android.webkit.WebViewClient {
    public *;
}
-keepclassmembers class * extends android.webkit.WebChromeClient {
    public *;
}
