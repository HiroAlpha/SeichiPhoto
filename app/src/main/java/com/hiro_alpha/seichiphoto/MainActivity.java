package com.hiro_alpha.seichiphoto;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Insets;
import android.graphics.Point;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.WindowMetrics;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private Button getImage, cameraStart;

    private Bitmap overlayBitmap = null;

    //OpenCV読み込み用
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                } break;
                default: {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getImage = findViewById(R.id.getImage);
        getImage.setOnClickListener(this);

        cameraStart = findViewById(R.id.cameraStart);
        cameraStart.setOnClickListener(this);

        //権限確認
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.CAMERA
            }, 1);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        //OpenCV確認
        if (!OpenCVLoader.initDebug()) {
            //表示
            Toast.makeText(getApplicationContext() , getString(R.string.openCVNot), Toast.LENGTH_LONG).show();
            TextView openCVCondition = findViewById(R.id.opencvConsition);
            openCVCondition.setText(getString(R.string.offline));

            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            TextView openCVCondition = findViewById(R.id.opencvConsition);
            openCVCondition.setText(getString(R.string.online));

            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    @Override
    public void onClick(View view) {
        if (view == getImage){
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");
            Intent chooserIntent = Intent.createChooser(intent, "単一画像の選択");

            getImageLauncher.launch(chooserIntent);
        }

        if (view == cameraStart){
            if (overlayBitmap != null) {
                //ハンドラーにBitmapをセット
                BitmapHandler handler = (BitmapHandler)this.getApplication();
                handler.setObj(overlayBitmap);

                //カメラ開始
                Intent intent = new Intent(this, CameraActivity.class);
                startActivity(intent);
            } else {
                Toast.makeText(getApplicationContext() , getString(R.string.error_noBitmap), Toast.LENGTH_LONG).show();
            }
        }
    }

    //画像取得Result
    ActivityResultLauncher<Intent> getImageLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
        @Override
        public void onActivityResult(ActivityResult result) {
            if(result.getResultCode() == RESULT_OK) {
                Intent resultData = result.getData();

                if (resultData != null) {
                    Uri uri = resultData.getData();

                    //TextViewに状態を表示
                    TextView imageCondition = findViewById(R.id.imageCondition);
                    imageCondition.setText(getString(R.string.OK));

                    //UriのBitmap化&表示&輪郭化クラスへ
                    ImageView originalImage = findViewById(R.id.originalImage);
                    try {
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                        originalImage.setImageBitmap(bitmap);
                        getImageOutline(bitmap);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    //TextViewに状態を表示
                    TextView imageCondition = findViewById(R.id.imageCondition);
                    imageCondition.setText(getString(R.string.imageIsNull));
                }
            }
        }
    });

    //輪郭化クラス
    private void getImageOutline(Bitmap imageBitmap){
        ImageView outlineImage = findViewById(R.id.outlineImage);

        //Bitmapの高さと幅
        int height = imageBitmap.getHeight();
        int width = imageBitmap.getWidth();

        //OpenCVオブジェクト作成
        Mat matOrig = new Mat(height, width, CvType.CV_8UC4);
        Utils.bitmapToMat(imageBitmap, matOrig);

        //グレースケールに変換
        Mat matGray = new Mat(height, width, CvType.CV_8UC1);
        Imgproc.cvtColor(matOrig, matGray, Imgproc.COLOR_RGB2GRAY);

        //輪郭に変換
        Mat matEdge = new Mat(height, width, CvType.CV_8UC1);
        Imgproc.Canny(matGray, matEdge, 30, 50);

        //色反転
        Mat matAbs = new Mat(height, width, CvType.CV_8UC1);
        Core.absdiff(matEdge, new Scalar(255, 255, 255), matAbs);

        //bitmap化
        Bitmap bitmapFinal = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(matAbs, bitmapFinal);
        outlineImage.setImageBitmap(bitmapFinal);

        //TextViewに状態を表示
        TextView imageConvert = findViewById(R.id.imageConvert);
        imageConvert.setText(getString(R.string.OK));

        makeTransparent(bitmapFinal, width, height);
    }

    //背景透過
    private void makeTransparent(Bitmap bitmap, int width, int height){
        int[] pixels = new int[width * height];
        int color = bitmap.getPixel(0,0);

        Bitmap bitmapTrans = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
        for (int y=0;y<height;y++) {
            for (int x = 0; x < width; x++) {
                if (pixels[x + y * width] == color) {
                    pixels[x + y * width] = 0;
                }
            }
        }
        bitmapTrans.eraseColor(Color.argb(0,0,0,0));
        bitmapTrans.setPixels(pixels, 0, width, 0, 0, width, height);

        overlayBitmap = bitmapTrans;
    }
}