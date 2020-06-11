package com.example.facedetection;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.face.FirebaseVisionFace;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetector;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions;

import java.io.IOException;
import java.security.Permission;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private Button cameraButton, gallery;
    private FirebaseVisionImage image;
    private FirebaseVisionFaceDetector detector;
    private  final static int REQUEST_IMAGE_CAPTURE = 5;
    private final static int REQUEST_IMAGE_CHOICE =6;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        cameraButton = findViewById(R.id.camera_button);

        gallery = findViewById(R.id.gallery_button);

        cameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

                if(takePictureIntent.resolveActivity(getPackageManager())!=null){
                    startActivityForResult(takePictureIntent,REQUEST_IMAGE_CAPTURE);
                }
            }
        });


        gallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(ContextCompat.checkSelfPermission(getApplicationContext(),Manifest.permission.READ_EXTERNAL_STORAGE)== PackageManager.PERMISSION_GRANTED) {
                    Intent choosePictureIntent = new Intent(Intent.ACTION_PICK);
                    choosePictureIntent.setType("image/*");
                    startActivityForResult(choosePictureIntent, REQUEST_IMAGE_CHOICE);
                }

                else{
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},0);
                }

            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            Bitmap bitmap = (Bitmap) extras.get("data");
            detectFace(bitmap);
        }
        else if(requestCode == REQUEST_IMAGE_CHOICE && resultCode == RESULT_OK){
            Uri uri = data.getData();
            try {
              Bitmap  bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(),uri);
              detectFace(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }



    private void detectFace(Bitmap bitmap) {

        FirebaseVisionFaceDetectorOptions highAccuracyOpts =
                new FirebaseVisionFaceDetectorOptions.Builder()
                        .setModeType(FirebaseVisionFaceDetectorOptions.ACCURATE_MODE)
                        .setClassificationType(FirebaseVisionFaceDetectorOptions.ALL_CLASSIFICATIONS)
                        .setClassificationType(FirebaseVisionFaceDetectorOptions.ALL_LANDMARKS)
                        .setMinFaceSize(0.15f)
                        .setTrackingEnabled(true)
                        .build();

        try {
            image = FirebaseVisionImage.fromBitmap(bitmap);
            detector = FirebaseVision.getInstance().getVisionFaceDetector(highAccuracyOpts);
        } catch (Exception e) {
            e.printStackTrace();
        }

        detector.detectInImage(image).addOnSuccessListener(new OnSuccessListener<List<FirebaseVisionFace>>() {
            @Override
            public void onSuccess(List<FirebaseVisionFace> firebaseVisionFaces) {
              String resultText = "";
              int i = 1;
              for(FirebaseVisionFace face : firebaseVisionFaces){
                  resultText = resultText.concat("\n" + "Smile " + i + " : "+face.getSmilingProbability()*100+"%")
                                          .concat("\nLeft Eye: " + face.getLeftEyeOpenProbability()*100+"%")
                                          .concat("\nRight Eye: " + face.getRightEyeOpenProbability()*100 + "%" + "\n");

                  i++;
              }

              if(firebaseVisionFaces.size() == 0){
                  Toast.makeText(getApplicationContext(),"No Faces Detected",Toast.LENGTH_SHORT).show();
              }
              else{
                  Bundle bundle = new Bundle();
                  bundle.putString(LCOFaceDetection.RESULT_TEXT,resultText);
                  DialogFragment resultDialog = new ResultDialog();
                  resultDialog.setArguments(bundle);
                  resultDialog.setCancelable(false);
                  resultDialog.show(getSupportFragmentManager(), LCOFaceDetection.RESULT_DIALOG);
              }
            }
        });
    }
}
