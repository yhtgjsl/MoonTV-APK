package com.moontv.mm;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.ViewGroup;
import android.net.Uri;
import android.webkit.ValueCallback;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private View mOriginalContentView; // 保存原始内容视图
    private boolean isFullscreen = false; // 标记是否在全屏状态
    private WebChromeClient.CustomViewCallback customViewCallback;
    private WebChromeClient chromeClient;
    private View customView; // 保存全屏视图

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 设置沉浸模式
        setImmersiveMode();
        setContentView(R.layout.activity_main);
        
        // 正确保存根视图
        ViewGroup rootView = (ViewGroup) findViewById(android.R.id.content);
        mOriginalContentView = rootView.getChildAt(0);

        // 初始化WebView
        webView = findViewById(R.id.webView);
        initWebView();

        // 加载目标URL
        webView.loadUrl("http://yshome.cool:8088");
    }

    // 初始化WebView设置
    private void initWebView() {
        WebSettings webSettings = webView.getSettings();
        // 启用JavaScript
        webSettings.setJavaScriptEnabled(true);
        // 禁用缩放功能，防止双指捏合放大缩小
        webSettings.setSupportZoom(false);
        webSettings.setBuiltInZoomControls(false);
        webSettings.setDisplayZoomControls(false);
        // 自适应屏幕
        webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(true);
        // 启用DOM存储
        webSettings.setDomStorageEnabled(true);
        // 设置缓存模式
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);

        // 设置WebViewClient，避免打开系统浏览器
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }
        });

        // 设置WebChromeClient，处理JavaScript对话框等
        // 初始化WebChromeClient
        chromeClient = new WebChromeClient() {
            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                if (customView != null) {
                    if (customViewCallback != null) {
                        try {
                            customViewCallback.onCustomViewHidden();
                        } catch (Exception e) {
                            // 捕获可能的异常
                        }
                        customViewCallback = null;
                    }
                    customView = null;
                }

                customView = view;
                customViewCallback = callback;
                isFullscreen = true;
                setContentView(customView);
                setImmersiveMode();
            }

            @Override
            public void onHideCustomView() {
                exitFullscreen();
            }

            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback,
                                           FileChooserParams fileChooserParams) {
                return true;
            }
        };
        webView.setWebChromeClient(chromeClient);
    }

    // 设置沉浸模式
    private void setImmersiveMode() {
        // 全屏显示
        Window window = getWindow();
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // 针对不同Android版本设置沉浸式模式
        View decorView = window.getDecorView();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            // Android 11及以上使用新API
            WindowInsetsController controller = decorView.getWindowInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            // 旧版本兼容
            decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    // 当用户点击返回按钮时，优先退出全屏模式，如果不在全屏则让WebView返回上一页或退出应用
    @Override
    public void onBackPressed() {
        // 检查是否在全屏播放状态
        if (isFullscreen) {
            // 在全屏时，调用自定义方法退出全屏模式
            exitFullscreen();
            return;
        }

        // 不在全屏，处理正常的WebView返回逻辑
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    // 自定义退出全屏方法
    private void exitFullscreen() {
        // 确保在主线程中执行UI操作
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                isFullscreen = false;
                
                // 安全地调用customViewCallback
                if (customViewCallback != null) {
                    try {
                        customViewCallback.onCustomViewHidden();
                    } catch (Exception e) {
                        // 捕获可能的异常，避免闪退
                    } finally {
                        customViewCallback = null;
                    }
                }
                
                // 移除全屏视图
                if (customView != null) {
                    try {
                        // 从父视图中移除
                        if (customView.getParent() != null && customView.getParent() instanceof View) {
                            View parent = (View) customView.getParent();
                            if (parent instanceof ViewGroup) {
                                ((ViewGroup) parent).removeView(customView);
                            }
                        }
                    } catch (Exception e) {
                        // 捕获可能的异常
                    }
                    customView = null;
                }
                
                // 恢复原始视图
                try {
                    if (mOriginalContentView != null) {
                        setContentView(mOriginalContentView);
                        // 重新获取WebView引用
                        webView = (WebView) mOriginalContentView.findViewById(R.id.webView);
                    } else {
                        // 如果原始视图为空，重新设置布局
                        setContentView(R.layout.activity_main);
                        webView = findViewById(R.id.webView);
                    }
                } catch (Exception e) {
                    // 捕获可能的异常
                    setContentView(R.layout.activity_main);
                    webView = findViewById(R.id.webView);
                }
                
                // 恢复沉浸模式
                setImmersiveMode();
            }
        });
    }

    // 当窗口焦点变化时，重新设置沉浸模式
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            setImmersiveMode();
        }
    }
}
