package com.hiro_alpha.seichiphoto;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.media.Image;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.util.Log;
import android.util.Size;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.WindowInsets;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraControl;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.FocusMeteringAction;
import androidx.camera.core.FocusMeteringResult;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.MeteringPoint;
import androidx.camera.core.MeteringPointFactory;
import androidx.camera.core.Preview;
import androidx.camera.core.SurfaceOrientedMeteringPointFactory;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.app.ComponentActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CameraActivity extends AppCompatActivity implements View.OnClickListener, View.OnTouchListener {

    private boolean rotate = false;
    private final String[] REQUIRED_PERMISSIONS = new String[]{"android.permission.CAMERA", "android.permission.WRITE_EXTERNAL_STORAGE"};

    private ImageView overlay;
    private ImageView takePhoto;
    private ImageView cameraFlip;
    private ImageView cameraSetting;
    private PreviewView previewView;

    private Camera camera = null;
    private Preview preview = null;
    private ImageCapture imageCapture = null;

    private BitmapHandler handler;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        handler = (BitmapHandler) this.getApplication();
        Bitmap overlayImage = handler.getObj();
        //回転
        if (overlayImage.getWidth() > overlayImage.getHeight()){
            Matrix matrix = new Matrix();
            matrix.postRotate(90);

            Bitmap scaledBitmap = Bitmap.createScaledBitmap(overlayImage, overlayImage.getWidth(), overlayImage.getHeight(), true);
            overlayImage = Bitmap.createBitmap(scaledBitmap, 0, 0, scaledBitmap.getWidth(), scaledBitmap.getHeight(), matrix, true);

            rotate = true;
        }

        overlay = findViewById(R.id.overlay);
        overlay.setImageBitmap(overlayImage);
        overlay.setOnTouchListener(this);

        previewView = findViewById(R.id.previewView);

        takePhoto = findViewById(R.id.takePhoto);
        takePhoto.setOnClickListener(this);

        cameraFlip = findViewById(R.id.cameraFlip);
        cameraFlip.setOnClickListener(this);

        cameraSetting = findViewById(R.id.cameraSetting);
        cameraSetting.setOnClickListener(this);

        //権限確認
        if (checkPermissions()){
            startCamera();
        } else {
            Toast.makeText(getApplicationContext() , getString(R.string.error_cameraPermission), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        handler.cleanObj();
    }

    //カメラ設定・開始
    private void startCamera() {
        final ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        Context context = this;

        cameraProviderFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                    preview = new Preview.Builder().build();
                    imageCapture = new ImageCapture.Builder().build();

                    CameraSelector cameraSelector = new CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build();

                    cameraProvider.unbindAll();
                    //カメラ設定
                    camera = cameraProvider.bindToLifecycle((LifecycleOwner) context, cameraSelector, imageCapture, preview);
                    preview.setSurfaceProvider(previewView.createSurfaceProvider(camera.getCameraInfo()));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, ContextCompat.getMainExecutor(this));
    }

    //撮影
    private void takePhoto(){
        String imageFolderPass = Environment.getExternalStorageDirectory() + "/Pictures/seichiPhoto/";
        File folder = new File(imageFolderPass);
        if (!folder.exists()){
            folder.mkdirs();
        }

        SimpleDateFormat imageNameFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        String imageName = imageNameFormat.format(new Date()) + ".jpg";

        String imagePass = imageFolderPass + imageName;
        File imageFile = new File(imagePass);

        ImageCapture.OutputFileOptions outputFileOptions = new ImageCapture.OutputFileOptions.Builder(imageFile).build();
        imageCapture.takePicture(outputFileOptions, ContextCompat.getMainExecutor(this), new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                Toast.makeText(getApplicationContext() , "Saved@"+ imagePass, Toast.LENGTH_LONG).show();
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                exception.printStackTrace();
            }
        });
    }

    @Override
    public void onClick(View view) {
        if (view == takePhoto){
            takePhoto();
        }

        if (view == cameraSetting){


            finish();
        }
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        //AF
        if (view == overlay){
            if (motionEvent.getAction() == MotionEvent.ACTION_DOWN){
                return true;
            }

            if (motionEvent.getAction() == MotionEvent.ACTION_UP){
                //Log.d("debugLog", "X: " + motionEvent.getX() + ", Y: " + motionEvent.getY());

                CameraControl cameraControl = camera.getCameraControl();

                MeteringPointFactory factory = new SurfaceOrientedMeteringPointFactory(previewView.getWidth(), previewView.getHeight());
                MeteringPoint point = factory.createPoint(motionEvent.getX(), motionEvent.getY());

                FocusMeteringAction action = new FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF).build();

                ListenableFuture listenableFuture = cameraControl.startFocusAndMetering(action);
                listenableFuture.addListener(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            //FocusMeteringResult result = listenableFuture.get();

                        } catch (Exception e){
                            e.printStackTrace();
                        }
                    }
                }, ContextCompat.getMainExecutor(this));

            }
        }

        return false;
    }

    //権限確認
    private boolean checkPermissions(){
        for(String permission : REQUIRED_PERMISSIONS){
            if(ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED){
                return false;
            }
        }
        return true;
    }
}
