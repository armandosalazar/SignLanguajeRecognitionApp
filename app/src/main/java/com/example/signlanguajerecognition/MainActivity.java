package com.example.signlanguajerecognition;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import org.opencv.android.OpenCVLoader;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (OpenCVLoader.initDebug()) Toast.makeText(this, "Successfully", Toast.LENGTH_SHORT).show();
        else Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show();

        Button cameraButton = findViewById(R.id.cameraButton);
        cameraButton.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, CameraActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP)));
    }
}