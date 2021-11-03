package com.hiro_alpha.seichiphoto;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.os.Bundle;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Arrays;

public class CameraActivity extends AppCompatActivity implements View.OnClickListener {

    TextureView cameraPreview;
    CameraDevice cameraDevice = null;
    CameraCaptureSession cameraCaptureSession = null;
    CaptureRequest captureRequest = null;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        cameraPreview = findViewById(R.id.cameraPreview);

        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);

        //カメラID取得
        String cameraId = null;
        try {
            String[] cameraIdList = cameraManager.getCameraIdList();
            for (String id : cameraIdList) {
                CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(id);

                switch (cameraCharacteristics.get(CameraCharacteristics.LENS_FACING)) {
                    //インカメ
                    case CameraCharacteristics.LENS_FACING_FRONT:
                        break;

                    //外カメ
                    case CameraCharacteristics.LENS_FACING_BACK:
                        cameraId = id;
                        break;
                    default:
                }
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        //権限確認&カメラオープン
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getApplicationContext() , getString(R.string.error_cameraPermission), Toast.LENGTH_LONG).show();
                finish();
            } else {
                //カメラオープン
                cameraManager.openCamera(cameraId, stateCallback, null);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }


    }

    //CameraDevice.StateCallback
    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice mCameraDevice) {
            //接続成功
            cameraDevice = mCameraDevice;
            createCameraPreviewSession();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice mCameraDevice) {
            //接続切断
            mCameraDevice.close();
            cameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice mCameraDevice, int i) {
            //エラー
            mCameraDevice.close();
            cameraDevice = null;
        }
    };

    //カメラプレビュー
    private void createCameraPreviewSession(){
        try {
            SurfaceTexture surfaceTexture = cameraPreview.getSurfaceTexture();
            surfaceTexture.setDefaultBufferSize(1080, 1920);

            Surface surface = new Surface(surfaceTexture);

            CaptureRequest.Builder builder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            builder.addTarget(surface);

            cameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession mCameraCaptureSession) {
                    //準備完了
                    cameraCaptureSession = mCameraCaptureSession;
                    try {
                        captureRequest = builder.build();
                        cameraCaptureSession.setRepeatingRequest(captureRequest, null, null);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Toast.makeText(getApplicationContext() , getString(R.string.error_cameraSession), Toast.LENGTH_LONG).show();
                }
            }, null);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onClick(View view) {

    }
}
