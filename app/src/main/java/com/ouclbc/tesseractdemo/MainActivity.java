package com.ouclbc.tesseractdemo;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private Button mTestButton;
    private TextView mTextView;
    private ImageView mImageView;
    private static final String TESSBASE_PATH = Environment.getExternalStorageDirectory().toString();
    private static final String DEFAULT_LANGUAGE = "eng";//"chi_sim";
    private static final String TESSDATA_PATH = TESSBASE_PATH + "/tessdata/";
    private static final int PERMISSION_REQUEST_CODE=0;
    /**
     * assets中的文件名
     */
    private static final String DEFAULT_LANGUAGE_NAME = DEFAULT_LANGUAGE + ".traineddata";
    /**
     * 保存到SD卡中的完整文件名
     */
    private static final String LANGUAGE_PATH = TESSDATA_PATH + DEFAULT_LANGUAGE_NAME;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mTestButton = (Button) findViewById(R.id.button);
        mTextView = (TextView) findViewById(R.id.text);
        mImageView = findViewById(R.id.imageView);

        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                    checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
            }
        }
        mTestButton.setOnClickListener(v->{
            new Thread(new Runnable() {
                @Override
                public void run() {
                    if (Build.VERSION.SDK_INT<23 && !new File(LANGUAGE_PATH).exists()){
                        copyToSD(LANGUAGE_PATH, DEFAULT_LANGUAGE_NAME);
                    }
                    Bitmap bitmap = BitmapFactory.decodeFile(TESSBASE_PATH+File.separator+"test.jpg");
                    TessBaseAPI tessBaseAPI = new TessBaseAPI();
                    tessBaseAPI.init(TESSBASE_PATH,DEFAULT_LANGUAGE);
                    tessBaseAPI.setPageSegMode(TessBaseAPI.PageSegMode.PSM_AUTO);
                    tessBaseAPI.setImage(bitmap);
                    final String text= tessBaseAPI.getUTF8Text();
                    Log.d(TAG, "run: text="+text);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mImageView.setImageBitmap(bitmap);
                            mTextView.setText(text);
                        }
                    });
                    tessBaseAPI.end();
                }
            }).start();
        });
    }
    /**
     * 将assets中的识别库复制到SD卡中
     * @param path  要存放在SD卡中的 完整的文件名。这里是"/storage/emulated/0//tessdata/*.traineddata"
     * @param name  assets中的文件名 这里是 "*.traineddata"
     */
    public void copyToSD(String path, String name) {
        Log.i(TAG, "copyToSD: "+path);
        Log.i(TAG, "copyToSD: "+name);

        //如果存在就删掉
        File f = new File(path);
        if (f.exists()){
            f.delete();
        }
        if (!f.exists()){
            File p = new File(f.getParent());
            if (!p.exists()){
                p.mkdirs();
            }
            try {
                f.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        InputStream is=null;
        OutputStream os=null;
        try {
            is = this.getAssets().open(name);
            File file = new File(path);
            os = new FileOutputStream(file);
            byte[] bytes = new byte[2048];
            int len = 0;
            while ((len = is.read(bytes)) != -1) {
                os.write(bytes, 0, len);
            }
            os.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (is != null)
                    is.close();
                if (os != null)
                    os.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    /**
     * 请求到权限后在这里复制识别库
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.i(TAG, "onRequestPermissionsResult: "+grantResults[0]);
        switch (requestCode){
            case PERMISSION_REQUEST_CODE:
                if (grantResults.length>0&&grantResults[0]==PackageManager.PERMISSION_GRANTED){
                    Log.i(TAG, "onRequestPermissionsResult: copy");
                    copyToSD(LANGUAGE_PATH, DEFAULT_LANGUAGE_NAME);
                }
                break;
            default:
                break;
        }
    }
}
