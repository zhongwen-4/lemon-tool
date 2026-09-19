package com.lemon.mrs;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends Activity {
    private static final int PICK_ZIP = 1;
    private static final String CACHE_NAME = "picked_module.zip";

    static {
        System.loadLibrary("mrs_jni");
    }

    private static native String nativeScan(String path);

    private TextView output;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_main);
        output = (TextView) findViewById(R.id.output);
        Button pick = (Button) findViewById(R.id.pick);
        pick.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                startActivityForResult(intent, PICK_ZIP);
            }
        });
    }

    @Override
    protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request != PICK_ZIP || result != RESULT_OK || data == null) return;
        Uri uri = data.getData();
        if (uri == null) return;

        try {
            final File cached = copyToCache(uri);
            output.setText(getString(R.string.scanning));
            new Thread(new Runnable() {
                @Override
                public void run() {
                    final String report = nativeScan(cached.getAbsolutePath());
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            output.setText(report);
                        }
                    });
                }
            }).start();
        } catch (Exception error) {
            output.setText("读取文件失败：" + error.getMessage());
        }
    }

    private File copyToCache(Uri uri) throws Exception {
        File target = new File(getCacheDir(), CACHE_NAME);
        InputStream input = getContentResolver().openInputStream(uri);
        if (input == null) throw new IllegalStateException("无法打开所选文件");
        try {
            OutputStream output = new FileOutputStream(target);
            try {
                byte[] buffer = new byte[65536];
                int read;
                while ((read = input.read(buffer)) > 0) output.write(buffer, 0, read);
            } finally {
                output.close();
            }
        } finally {
            input.close();
        }
        return target;
    }
}
