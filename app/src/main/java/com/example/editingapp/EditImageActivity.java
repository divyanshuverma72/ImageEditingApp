package com.example.editingapp;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.editingapp.databinding.ActivityEditImageBinding;
import com.theartofdev.edmodo.cropper.CropImage;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Stack;

public class EditImageActivity extends AppCompatActivity implements View.OnClickListener {

    private ImageView imageView;
    private Bitmap currentBitmap;
    private final Stack<Bitmap> stack = new Stack<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityEditImageBinding activityEditImageBinding = ActivityEditImageBinding.inflate(getLayoutInflater());
        setContentView(activityEditImageBinding.getRoot());

        imageView = activityEditImageBinding.capturedImage;
        Button undo = activityEditImageBinding.undo;
        Button rotate = activityEditImageBinding.rotate;
        Button crop = activityEditImageBinding.crop;
        Button save = activityEditImageBinding.save;

        undo.setOnClickListener(this);
        rotate.setOnClickListener(this);
        crop.setOnClickListener(this);
        save.setOnClickListener(this);

        String image_path = getIntent().getStringExtra("imageUri");
        Uri myUri = Uri.parse(image_path);
        try {
            currentBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), myUri);
            imageView.setImageBitmap(currentBitmap);
            stack.push(currentBitmap);
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.rotate) {
            rotateImage();
        } else if (view.getId() == R.id.crop) {
            cropImage(getImageUri(this, currentBitmap));
        } else if (view.getId() == R.id.undo) {
            if (stack.size() >= 2) {
                stack.pop();
                imageView.setImageBitmap(stack.peek());
                currentBitmap = stack.peek();
            }
        } else {
            saveImageToGallery(currentBitmap, this);
        }
    }

    private void cropImage(Uri imageUri) {
        CropImage.activity(imageUri)
                .start(this);
    }

    private void rotateImage() {
        Matrix mat = new Matrix();
        mat.postRotate(90);
        currentBitmap = Bitmap.createBitmap(currentBitmap, 0, 0,
                currentBitmap.getWidth(), currentBitmap.getHeight(), mat, true);
        imageView.setImageBitmap(currentBitmap);
        stack.push(currentBitmap);
    }

    public Uri getImageUri(Context inContext, Bitmap inImage) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(inContext.getContentResolver(), inImage, "TitleEDit", null);
        return Uri.parse(path);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                Uri resultUri = null;
                if (result != null) {
                    resultUri = result.getUri();
                }
                imageView.setImageURI(resultUri);
                try {
                    currentBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), resultUri);
                    stack.push(currentBitmap);
                    getContentResolver().
                            delete(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                    "TITLE ='" + "TitleEDit" + "'", null);
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }
        }
    }

    public void saveImageToGallery(Bitmap currentBitmap, Context context) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        currentBitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(context.getContentResolver(), currentBitmap, String.valueOf(System.currentTimeMillis()), null);
        Toast.makeText(this, R.string.image_saved_msg, Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("savedImageUri", path);
        startActivity(intent);
        finish();
    }
}
