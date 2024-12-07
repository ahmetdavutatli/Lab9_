package com.example.lab9_;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

public class MainActivity extends AppCompatActivity {

    EditText txtURL;
    Button btnDownload;
    ImageView imgView;

    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static final String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtURL = findViewById(R.id.txtURL);
        btnDownload = findViewById(R.id.btnDownload);
        imgView = findViewById(R.id.imgView);

        btnDownload.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (!Environment.isExternalStorageManager()) {
                    requestManageExternalStoragePermission();
                } else {
                    startDownload();
                }
            } else {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, PERMISSIONS_STORAGE, REQUEST_EXTERNAL_STORAGE);
                } else {
                    startDownload();
                }
            }
        });
    }

    private void requestManageExternalStoragePermission() {
        Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
        intent.setData(Uri.fromParts("package", getPackageName(), null));
        startActivity(intent);
    }

    private void startDownload() {
        String url = txtURL.getText().toString().trim();
        if (!url.isEmpty()) {
            new DownloadTask().execute(url);
        } else {
            Toast.makeText(this, "Please enter a valid URL", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_EXTERNAL_STORAGE && grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startDownload();
        } else {
            Toast.makeText(this, "Storage permission not granted", Toast.LENGTH_SHORT).show();
        }
    }

    private String getImagePath() {
        File directory = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (directory != null && !directory.exists()) {
            directory.mkdirs();
        }
        return directory + "/temp.jpg";
    }

    private boolean downloadFile(String strURL, String imagePath, ProgressDialog progressDialog) {
        try {
            URL url = new URL(strURL);
            URLConnection connection = url.openConnection();
            connection.connect();

            int fileSize = connection.getContentLength();
            if (fileSize <= 0) {
                throw new Exception("File size is zero or unknown.");
            }

            InputStream input = new BufferedInputStream(url.openStream());
            OutputStream output = new FileOutputStream(imagePath);

            byte[] data = new byte[1024];
            long total = 0;
            int count;

            while ((count = input.read(data)) != -1) {
                total += count;
                output.write(data, 0, count);

                if (fileSize > 0) {
                    int progress = (int) ((total * 100) / fileSize);
                    progressDialog.setProgress(progress);
                }
            }

            output.flush();
            output.close();
            input.close();
            return true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            runOnUiThread(() -> Toast.makeText(this, "File not found on server", Toast.LENGTH_SHORT).show());
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            runOnUiThread(() -> Toast.makeText(this, "Download failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            return false;
        }
    }


    private void preview(String imagePath) {
        File file = new File(imagePath);
        if (file.exists()) {
            Bitmap image = BitmapFactory.decodeFile(imagePath);
            if (image != null) {
                int W = 400;
                int H = (int) ((float) image.getHeight() * W / image.getWidth());
                Bitmap scaledImage = Bitmap.createScaledBitmap(image, W, H, false);
                imgView.setImageBitmap(scaledImage);
            } else {
                Toast.makeText(this, "Image could not be loaded", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "File not found!", Toast.LENGTH_SHORT).show();
        }
    }

    class DownloadTask extends AsyncTask<String, Integer, String> {

        ProgressDialog progressDialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = new ProgressDialog(MainActivity.this);
            progressDialog.setMax(100);
            progressDialog.setIndeterminate(false);
            progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            progressDialog.setTitle("Downloading Image...");
            progressDialog.setMessage("Please wait.");
            progressDialog.show();
        }

        @Override
        protected String doInBackground(String... urls) {
            String imagePath = getImagePath();
            boolean success = downloadFile(urls[0], imagePath, progressDialog);
            return success ? imagePath : null;
        }

        @Override
        protected void onPostExecute(String imagePath) {
            progressDialog.dismiss();
            if (imagePath != null) {
                preview(imagePath);
            } else {
                Toast.makeText(MainActivity.this, "Download failed", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
