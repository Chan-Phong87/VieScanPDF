package vn.scanbongoc.app;

import android.app.Activity;
import android.content.ClipData;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.core.content.FileProvider;
import androidx.webkit.WebViewAssetLoader;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.OutputStream;

/**
 * Vỏ Android cho ứng dụng Scan Bốn Góc.
 * Toàn bộ giao diện và xử lý ảnh nằm trong assets/index.html chạy bên trong WebView.
 * Lớp này chỉ làm 3 việc: mở camera / thư viện ảnh, lưu PDF vào Tải xuống, mở bảng chia sẻ của Android.
 * Ứng dụng không xin quyền Internet nên tài liệu không bao giờ rời khỏi máy.
 */
public class MainActivity extends Activity {

    private static final int REQ_CHOOSE = 1001;
    private static final String HOST = "appassets.androidplatform.net";

    private WebView web;
    private ValueCallback<Uri[]> fileCallback;
    private Uri cameraUri;
    private File cameraFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        cleanOldCaptures();

        final WebViewAssetLoader loader = new WebViewAssetLoader.Builder()
                .setDomain(HOST)
                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
                .build();

        web = new WebView(this);
        setContentView(web);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(false);
        s.setSupportZoom(false);
        s.setBuiltInZoomControls(false);

        web.addJavascriptInterface(new Bridge(), "AndroidBridge");

        web.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                return loader.shouldInterceptRequest(request.getUrl());
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                // Không cho trang đi ra ngoài ứng dụng.
                return !HOST.equals(request.getUrl().getHost());
            }
        });

        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback,
                                             FileChooserParams params) {
                if (fileCallback != null) {
                    fileCallback.onReceiveValue(null);
                }
                fileCallback = callback;
                cameraUri = null;
                cameraFile = null;

                Intent intent;
                if (params.isCaptureEnabled()) {
                    Uri out = newCameraUri();
                    if (out == null) {
                        fileCallback = null;
                        callback.onReceiveValue(null);
                        return true;
                    }
                    cameraUri = out;
                    intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, out);
                    intent.setClipData(ClipData.newRawUri("photo", out));
                    intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                            | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                } else {
                    intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    intent.setType("image/*");
                    if (params.getMode() == FileChooserParams.MODE_OPEN_MULTIPLE) {
                        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                    }
                }

                try {
                    startActivityForResult(intent, REQ_CHOOSE);
                } catch (Exception e) {
                    deleteCameraFile();
                    fileCallback = null;
                    callback.onReceiveValue(null);
                    Toast.makeText(MainActivity.this,
                            "Không mở được camera hoặc thư viện ảnh", Toast.LENGTH_LONG).show();
                }
                return true;
            }
        });

        web.loadUrl("https://" + HOST + "/assets/index.html");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != REQ_CHOOSE) {
            super.onActivityResult(requestCode, resultCode, data);
            return;
        }
        ValueCallback<Uri[]> callback = fileCallback;
        fileCallback = null;
        if (callback == null) {
            return;
        }
        Uri[] result = null;
        if (cameraUri != null) {
            if (resultCode == RESULT_OK && cameraFile != null && cameraFile.length() > 0) {
                result = new Uri[]{cameraUri};
            } else {
                deleteCameraFile();
            }
        } else {
            result = WebChromeClient.FileChooserParams.parseResult(resultCode, data);
        }
        cameraUri = null;
        callback.onReceiveValue(result);
    }

    @Override
    public void onBackPressed() {
        if (web == null) {
            super.onBackPressed();
            return;
        }
        web.evaluateJavascript(
                "(function(){try{return !!(window.__androidBack&&window.__androidBack());}catch(e){return false;}})()",
                new ValueCallback<String>() {
                    @Override
                    public void onReceiveValue(String value) {
                        if (!"true".equals(value)) {
                            superBack();
                        }
                    }
                });
    }

    private void superBack() {
        super.onBackPressed();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (web != null) web.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (web != null) web.onResume();
    }

    @Override
    protected void onDestroy() {
        if (web != null) {
            web.destroy();
            web = null;
        }
        super.onDestroy();
    }

    /* ---------- ảnh chụp tạm trong bộ nhớ đệm của ứng dụng ---------- */

    private Uri newCameraUri() {
        try {
            File dir = new File(getCacheDir(), "captures");
            if (!dir.exists() && !dir.mkdirs()) {
                return null;
            }
            cameraFile = new File(dir, "IMG_" + System.currentTimeMillis() + ".jpg");
            return FileProvider.getUriForFile(this, getPackageName() + ".files", cameraFile);
        } catch (Exception e) {
            return null;
        }
    }

    private void deleteCameraFile() {
        if (cameraFile != null) {
            //noinspection ResultOfMethodCallIgnored
            cameraFile.delete();
        }
        cameraFile = null;
        cameraUri = null;
    }

    private void cleanOldCaptures() {
        try {
            File[] files = new File(getCacheDir(), "captures").listFiles();
            if (files == null) return;
            long limit = System.currentTimeMillis() - 6L * 60 * 60 * 1000;
            for (File f : files) {
                if (f.lastModified() < limit) {
                    //noinspection ResultOfMethodCallIgnored
                    f.delete();
                }
            }
        } catch (Exception ignored) {
        }
    }

    /* ---------- lưu và chia sẻ PDF ---------- */

    private Uri saveToDownloads(String name, byte[] data) throws Exception {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Downloads.DISPLAY_NAME, name);
        values.put(MediaStore.Downloads.MIME_TYPE, "application/pdf");
        values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/ScanBonGoc");
        values.put(MediaStore.Downloads.IS_PENDING, 1);
        Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
        if (uri == null) {
            throw new Exception("Không tạo được file trong thư mục Tải xuống");
        }
        try {
            OutputStream os = getContentResolver().openOutputStream(uri);
            if (os == null) {
                throw new Exception("Không ghi được file");
            }
            try {
                os.write(data);
            } finally {
                os.close();
            }
        } catch (Exception e) {
            getContentResolver().delete(uri, null, null);
            throw e;
        }
        ContentValues done = new ContentValues();
        done.put(MediaStore.Downloads.IS_PENDING, 0);
        getContentResolver().update(uri, done, null, null);
        return uri;
    }

    private void startShare(Uri uri, String name) {
        try {
            Intent send = new Intent(Intent.ACTION_SEND);
            send.setType("application/pdf");
            send.putExtra(Intent.EXTRA_STREAM, uri);
            send.putExtra(Intent.EXTRA_SUBJECT, name);
            send.setClipData(ClipData.newRawUri(name, uri));
            send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(send, "Chia sẻ PDF"));
        } catch (Exception e) {
            Toast.makeText(this, "Không mở được bảng chia sẻ", Toast.LENGTH_LONG).show();
        }
    }

    /** Cầu nối cho JavaScript: window.AndroidBridge.begin / chunk / finish. */
    private class Bridge {
        private ByteArrayOutputStream buf;
        private String name = "Scan.pdf";

        @JavascriptInterface
        public synchronized void begin(String fileName) {
            buf = new ByteArrayOutputStream(1 << 20);
            String n = fileName == null ? "" : fileName.replaceAll("[\\\\/:*?\"<>|]", " ").trim();
            if (n.isEmpty()) n = "Scan.pdf";
            if (!n.toLowerCase().endsWith(".pdf")) n = n + ".pdf";
            name = n;
        }

        @JavascriptInterface
        public synchronized void chunk(String base64) {
            if (buf == null) return;
            try {
                byte[] b = Base64.decode(base64, Base64.DEFAULT);
                buf.write(b, 0, b.length);
            } catch (Exception e) {
                buf = null;
            }
        }

        @JavascriptInterface
        public synchronized String finish(boolean share) {
            if (buf == null) {
                return "Dữ liệu PDF bị thiếu, hãy tạo lại file.";
            }
            final byte[] data = buf.toByteArray();
            buf = null;
            final String fileName = name;
            try {
                final Uri uri = saveToDownloads(fileName, data);
                if (share) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            startShare(uri, fileName);
                        }
                    });
                }
                return "ok";
            } catch (Exception e) {
                return e.getMessage() == null ? "Lỗi không rõ" : e.getMessage();
            }
        }
    }
}
