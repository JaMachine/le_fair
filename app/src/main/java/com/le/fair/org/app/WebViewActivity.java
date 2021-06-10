package com.le.fair.org.app;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.view.View;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import pl.droidsonroids.gif.GifImageView;

import static com.le.fair.org.app.ConnectionService.BroadcastStringForAction;
import static com.le.fair.org.app.MainActivity.dc;
import static com.le.fair.org.app.MainActivity.main;

public class WebViewActivity extends AppCompatActivity {
    protected WebView myView;
    protected boolean myOnline;
    protected ValueCallback<Uri> myUploadMsg;
    protected Uri myCameraFolder = null;
    protected GifImageView myInternetStatus;
    protected ValueCallback<Uri[]> myFilesFolder;
    protected String myPhotosFolder;
    protected IntentFilter myFilter;
    protected static int requestCode = 1;
    protected static int resultCode = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_view);
        fullScreen();

        myInternetStatus = findViewById(R.id.wv_internet_status);


        myView = findViewById(R.id.wv);
        if (Build.VERSION.SDK_INT >= 23 && (ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)) {
            ActivityCompat.requestPermissions(WebViewActivity.this, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA}, 1);
        }
        myView.setWebViewClient(new customWebViewClient());
        myView.getSettings().setJavaScriptEnabled(true);
        myView.getSettings().setAllowFileAccess(true);
        myView.getSettings().setDomStorageEnabled(true);
        myView.getSettings().setLoadsImagesAutomatically(true);
        if (savedInstanceState != null)
            myView.restoreState(savedInstanceState.getBundle("webViewState"));

        myView.setWebChromeClient(new WebChromeClient() {
            private File createImageFile() throws IOException {
                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                String imageFileName = "JPEG_" + timeStamp + "_";
                File storageDir = Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_PICTURES);
                File imageFile = File.createTempFile(
                        imageFileName,
                        ".jpg",
                        storageDir
                );
                return imageFile;

            }

            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> filePath, WebChromeClient.FileChooserParams fileChooserParams) {
                if (myFilesFolder != null) {
                    myFilesFolder.onReceiveValue(null);
                }
                myFilesFolder = filePath;
                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                    File photoFile = null;
                    try {
                        photoFile = createImageFile();
                        takePictureIntent.putExtra("PhotoPath", myPhotosFolder);
                    } catch (IOException e) {
                        e.fillInStackTrace();
                    }
                    if (photoFile != null) {
                        myPhotosFolder = "file:" + photoFile.getAbsolutePath();
                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                                Uri.fromFile(photoFile));
                    } else {
                        takePictureIntent = null;
                    }
                }
                Intent contentSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
                contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
                contentSelectionIntent.setType("image/*");
                Intent[] intentArray;
                if (takePictureIntent != null) {
                    intentArray = new Intent[]{takePictureIntent};
                } else {
                    intentArray = new Intent[0];
                }
                Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
                chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent);
                chooserIntent.putExtra(Intent.EXTRA_TITLE, "Image Chooser");
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray);
                startActivityForResult(chooserIntent, requestCode);
                return true;
            }

            public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType) {
                myUploadMsg = uploadMsg;
                File imageStorageDir = new File(
                        Environment.getExternalStoragePublicDirectory(
                                Environment.DIRECTORY_PICTURES)
                        , "AndroidExampleFolder");
                if (!imageStorageDir.exists()) {
                    imageStorageDir.mkdirs();
                }
                File file = new File(
                        imageStorageDir + File.separator + "IMG_"
                                + String.valueOf(System.currentTimeMillis())
                                + ".jpg");
                myCameraFolder = Uri.fromFile(file);
                final Intent captureIntent = new Intent(
                        android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, myCameraFolder);
                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("image/*");
                Intent chooserIntent = Intent.createChooser(i, "Image Chooser");
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS
                        , new Parcelable[]{captureIntent});
                startActivityForResult(chooserIntent, resultCode);
            }

            public void openFileChooser(ValueCallback<Uri> uploadMsg,
                                        String acceptType,
                                        String capture) {
                openFileChooser(uploadMsg, acceptType);
            }

        });
        myFilter = new IntentFilter();
        myFilter.addAction(BroadcastStringForAction);
        Intent intent = new Intent(this, ConnectionService.class);
        startService(intent);
        if (isOnline(getApplicationContext()))
            showWebView();
        else hideWebView();
    }

    public BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(BroadcastStringForAction)) {
                if (intent.getStringExtra("online_status").equals("true"))
                    showWebView();
                else hideWebView();
            }
        }
    };

    public void showWebView() {
        if (!myOnline) {
            myInternetStatus.setVisibility(View.GONE);
            myView.loadUrl(main);
            myView.setVisibility(View.VISIBLE);
            myOnline = true;
        }
    }

    public void hideWebView() {
        myOnline = false;
        myInternetStatus.setVisibility(View.VISIBLE);
        myView.setVisibility(View.GONE);
    }

    public class customWebViewClient extends WebViewClient {
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if (Uri.parse(url).getHost().contains(dc("cGludGVyZXN0"))) return true;
            if (Uri.parse(url).getHost().contains(dc("dHdpdHRlcg=="))) return true;
            if (Uri.parse(url).getHost().contains(dc("ZmFjZWJvb2s="))) return true;
            if (Uri.parse(url).getHost().contains(dc("aW5zdGFncmFt"))) return true;
            if (Uri.parse(url).getHost().contains(dc("eW91dHViZQ=="))) return true;
            if (Uri.parse(url).getHost().contains(dc("bGlua2Vk"))) return true;
            return false;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (requestCode != WebViewActivity.requestCode || myFilesFolder == null) {
                super.onActivityResult(requestCode, resultCode, data);
                return;
            }
            Uri[] results = null;
            if (resultCode == Activity.RESULT_OK) {
                if (data == null) {
                    if (myPhotosFolder != null) {
                        results = new Uri[]{Uri.parse(myPhotosFolder)};
                    }
                } else {
                    String dataString = data.getDataString();
                    if (dataString != null) {
                        results = new Uri[]{Uri.parse(dataString)};
                    }
                }
            }
            myFilesFolder.onReceiveValue(results);
            myFilesFolder = null;
        } else if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
            if (requestCode != WebViewActivity.resultCode || myUploadMsg == null) {
                super.onActivityResult(requestCode, resultCode, data);
                return;
            }
            if (requestCode == WebViewActivity.resultCode) {
                if (null == this.myUploadMsg) {
                    return;
                }
                Uri result = null;
                try {
                    if (resultCode != RESULT_OK) {
                        result = null;
                    } else {
                        result = data == null ? myCameraFolder : data.getData();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                myUploadMsg.onReceiveValue(result);
                myUploadMsg = null;
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (myView.canGoBack()) myView.goBack();
        else super.onBackPressed();
    }

    @Override
    protected void onResume() {
        fullScreen();
        registerReceiver(broadcastReceiver, myFilter);
        super.onResume();
    }

    private void fullScreen() {
        View v = findViewById(R.id.wv_container);
        v.setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_FULLSCREEN);
    }

    @Override
    protected void onRestart() {
        registerReceiver(broadcastReceiver, myFilter);
        fullScreen();
        super.onRestart();
    }

    @Override
    protected void onPause() {
        unregisterReceiver(broadcastReceiver);
        fullScreen();
        super.onPause();
    }

    public boolean isOnline(Context context) {
        ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = manager.getActiveNetworkInfo();
        if (info != null && info.isConnectedOrConnecting()) return true;
        else return false;
    }
}