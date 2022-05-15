package com.example.editingapp;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.editingapp.databinding.ActivityMainBinding;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.Objects;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private PreviewView previewView;
    private ImageCapture imageCapture;
    private ImageView imageView;
    private Button restartCamera;
    private Button capturePhoto;
    private final int REQUEST_CODE_PERMISSIONS = 1001;
    private final String[] REQUIRED_PERMISSIONS = new String[]{"android.permission.CAMERA", "android.permission.WRITE_EXTERNAL_STORAGE"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityMainBinding activityMainBinding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(activityMainBinding.getRoot());

        previewView = activityMainBinding.previewView;
        restartCamera = activityMainBinding.restartCamera;
        imageView = activityMainBinding.imageView;
        capturePhoto = activityMainBinding.bCapture;
        Button upload = activityMainBinding.uploadFromGallery;

        restartCamera.setVisibility(View.GONE);
        capturePhoto.setVisibility(View.VISIBLE);

        String image_path = getIntent().getStringExtra("savedImageUri");
        if (image_path != null) {
            Uri myUri = Uri.parse(image_path);
            imageView.setVisibility(View.VISIBLE);
            imageView.setImageURI(myUri);
            restartCamera.setVisibility(View.VISIBLE);
            capturePhoto.setVisibility(View.GONE);
            previewView.setVisibility(View.GONE);
        }

        capturePhoto.setOnClickListener(this);
        upload.setOnClickListener(this);
        restartCamera.setOnClickListener(this);

        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }
    }

    void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder()
                .build();

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                .build();

        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        imageCapture = new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build();

        cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);
    }

    private boolean allPermissionsGranted() {

        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera();
            } else {
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show();
                this.finish();
            }
        }
    }

    private void startCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindPreview(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                // No errors need to be handled for this Future.
                // This should never be reached.
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void capturePhoto() {
        long timestamp = System.currentTimeMillis();

        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, timestamp);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");

        imageCapture.takePicture(
                new ImageCapture.OutputFileOptions.Builder(
                        getContentResolver(),
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        contentValues
                ).build(),
                ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        Intent intent = new Intent(MainActivity.this, EditImageActivity.class);
                        intent.putExtra("imageUri", Objects.requireNonNull(outputFileResults.getSavedUri()).toString());
                        startActivity(intent);
                        finish();
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Toast.makeText(MainActivity.this, "Error saving photo: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.bCapture) {
            capturePhoto();
        } else if (view.getId() == R.id.restart_camera) {
            imageView.setVisibility(View.GONE);
            restartCamera.setVisibility(View.GONE);
            capturePhoto.setVisibility(View.VISIBLE);
            previewView.setVisibility(View.VISIBLE);
        } else {
            Intent intent = new Intent(
                    Intent.ACTION_PICK,
                    android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

            callEditImageActivity.launch(intent);
        }
    }

    ActivityResultLauncher<Intent> callEditImageActivity = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    Uri selectedImage = null;
                    if (result.getData() != null) {
                        selectedImage = result.getData().getData();
                    }
                    Intent intent = new Intent(MainActivity.this, EditImageActivity.class);
                    assert selectedImage != null;
                    intent.putExtra("imageUri", selectedImage.toString());
                    startActivity(intent);
                    finish();
                }
            }
    );
}
