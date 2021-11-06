package com.hiro_alpha.seichiphoto;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, TextWatcher {

    private Point imageRatio;
    private Bitmap overlayBitmap = null;

    private TextView openCVCondition, imageCondition, imageConvert;
    private AutoCompleteTextView ratioSelector;
    private ArrayAdapter<CharSequence> ratioAdapter;
    private Button getImage, cameraStart;

    private DataHandler dataHandler;

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

        //DataHandler定義
        dataHandler = (DataHandler) this.getApplication();

        //状態表示
        openCVCondition = findViewById(R.id.opencvCondition);

        imageCondition = findViewById(R.id.imageCondition);
        imageCondition.setText(getString(R.string.notLoaded));
        imageCondition.setTextColor(Color.RED);

        imageConvert = findViewById(R.id.imageConvert);
        imageConvert.setText(getString(R.string.notLoaded));
        imageConvert.setTextColor(Color.RED);

        //アスペクト比スピナー
        ratioSelector = findViewById(R.id.ratioSelector);
        ratioAdapter = ArrayAdapter.createFromResource(this, R.array.ratio_array, android.R.layout.simple_spinner_dropdown_item);
        ratioSelector.setAdapter(ratioAdapter);
        ratioSelector.setText(getString(R.string.ratio4_3), false);
        ratioSelector.addTextChangedListener(this);

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

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
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
            openCVCondition.setText(getString(R.string.offline));
            openCVCondition.setTextColor(Color.RED);

            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            openCVCondition.setText(getString(R.string.online));
            openCVCondition.setTextColor(Color.GREEN);

            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
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
                    imageCondition.setText(getString(R.string.OK));
                    imageCondition.setTextColor(Color.GREEN);

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
                    imageCondition.setTextColor(Color.RED);
                }
            }
        }
    });

    //輪郭化クラス
    private void getImageOutline(Bitmap imageBitmap){
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

        //TextViewに状態を表示
        imageConvert.setText(getString(R.string.OK));
        imageConvert.setTextColor(Color.GREEN);

        makeTransparent(bitmapFinal, width, height);
    }

    //背景透過
    private void makeTransparent(Bitmap bitmap, int width, int height){
        //白
        int color_white = Color.rgb(255,255,255);
        //黒
        int color_black = Color.rgb(0, 0, 0);
        //赤
        int color_red = Color.rgb(255, 0, 0);

        int[] pixels = new int[width * height];
        Bitmap bitmapTrans = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);

        //色変換
        for (int y=0;y<height;y++) {
            for (int x = 0; x < width; x++) {
                //白 → 透明
                if (pixels[x + y * width] == color_white) {
                    pixels[x + y * width] = 0;
                }

                //黒 → 赤
                if (pixels[x + y * width] == color_black) {
                    pixels[x + y * width] = color_red;
                }
            }
        }
        //bitmapTrans.eraseColor(Color.argb(0,0,0,0));
        bitmapTrans.setPixels(pixels, 0, width, 0, 0, width, height);

        overlayBitmap = bitmapTrans;

        ImageView outlineImage = findViewById(R.id.outlineImage);
        outlineImage.setImageBitmap(overlayBitmap);

        getImageRatio(overlayBitmap.getWidth(), overlayBitmap.getHeight());
    }

    //カメラアスペクト比設定
    private void getImageRatio(int imageX, int imageY){
        //最大公約数
        int gcd = 0;
        if (0 >= imageX || 0 >= imageY || imageX == imageY) {

            imageRatio = new Point(0,0);
        } else {
            int x = imageX;
            int y = imageY;
            int r = x % y;
            while (0 != r){
                x = y;
                y = r;
                r = x % y;
            }

            gcd = y;
        }

        int ratioX = imageX / gcd;
        int ratioY = imageY / gcd;
        float fRatioX = (float) imageX / gcd;
        float fRatioY = (float) imageY / gcd;

        //Log.d("Activity.MainActivity", "Image Ratio : " + ratioX + ":" + ratioY);

        //横画像の場合逆にする
        if (ratioX > ratioY) {
            int container = ratioX;
            ratioX = ratioY;
            ratioY = container;

            float fContainer = fRatioX;
            fRatioX = fRatioY;
            fRatioY = fContainer;
        }

        imageRatio = new Point(ratioX, ratioY);

        //カメラアスペクト比設定
        float ratio = fRatioX / fRatioY;
        float ratio3_4 = (float) 3/4;   //0.7500
        float ratio9_16 = (float) 9/16; //0.5625
        if (ratio <= ratio9_16){ //9:16より小さい
            //9:16
            ratioSelector.setText(getString(R.string.ratio16_9), false);
        } else if (ratio >= ratio3_4){ //3:4より大きい
            //3:4
            ratioSelector.setText(getString(R.string.ratio4_3), false);
        } else if ((ratio < ratio3_4) && (ratio > ratio9_16)){   //間
            //差
            float difference3_4 = ratio3_4 - ratio;
            float difference9_16 = ratio - ratio9_16;

            if (difference3_4 > difference9_16){
                //9:16
                ratioSelector.setText(getString(R.string.ratio16_9), false);
            } else {
                //3:4
                ratioSelector.setText(getString(R.string.ratio4_3), false);
            }
        } else {
            Toast.makeText(getApplicationContext() , getString(R.string.error_unexpectedRatio), Toast.LENGTH_LONG).show();

            //3:4
            ratioSelector.setText(getString(R.string.ratio4_3), false);
        }
    }

    //カメラプレビューウィンドウサイズ設定
    private Point previewSize(Point ratio){
        LinearLayout layout = findViewById(R.id.mainLayout);
        int layoutWidth = layout.getWidth();
        int layoutHeight = layout.getHeight() - (int) new ConvertDp2Px().convert(135, this);
        //int dpi = getResources().getDisplayMetrics().densityDpi;
        //Log.d("Activity.MainActivity", "DPI: " + dpi);
        Log.d("Activity.MainActivity", "LayoutSize: " + layoutWidth +  ", " + layoutHeight);

        int previewWidth = layoutWidth;
        int previewHeight = (layoutWidth / ratio.x) * ratio.y;
        Log.d("Activity.MainActivity", "Preview Size(Not Adj): " + previewWidth +  ", " + previewHeight);

        //最適化
        while (previewHeight > layoutHeight) {
            previewWidth = previewWidth - 10;
            previewHeight = (previewWidth / ratio.x) * ratio.y;
        }

        Log.d("Activity.MainActivity", "Preview Size: " + previewWidth +  ", " + previewHeight);

        return new Point(previewWidth, previewHeight);
    }

    //その他
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
                dataHandler.setOverlayImage(overlayBitmap);

                //カメラプレビューサイズ
                Point ratio;
                Log.d("Activity.OnTextChange", "Ratio Changed : " + dataHandler.getCamRatio());
                if (dataHandler.getCamRatio() == 1){

                    ratio = new Point(9, 16);
                } else {

                    ratio = new Point(3, 4);
                }
                Point resolution = previewSize(ratio);

                //カメラ開始
                Intent intent = new Intent(this, CameraActivity.class);
                intent.putExtra("WIDTH", resolution.x);
                intent.putExtra("HEIGHT", resolution.y);
                startActivity(intent);
            } else {
                Toast.makeText(getApplicationContext() , getString(R.string.error_noBitmap), Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

    }

    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        String ratio = charSequence.toString();
        int ratioId = 0;
        if (ratio.equals("16:9")) {
            ratioId = 1;
        }

        //ハンドラーにratioIdをセット
        dataHandler.setCamRatio(ratioId);
    }

    @Override
    public void afterTextChanged(Editable editable) {

    }
}