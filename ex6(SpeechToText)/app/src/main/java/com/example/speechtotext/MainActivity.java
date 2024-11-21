package com.example.speechtotext;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import okhttp3.MultipartBody;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private static final int SAMPLE_RATE = 16000; // Whisper model sample rate
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private boolean permissionToRecordAccepted = false;
    private String[] permissions = {Manifest.permission.RECORD_AUDIO};
    private OkHttpClient client;
    private TextView transcribedText;
    private Button recordButton;
    private boolean isRecording = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        transcribedText = findViewById(R.id.transcribedText);
        recordButton = findViewById(R.id.recordButton);

        ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION);

        client = new OkHttpClient();

        recordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isRecording) {
                    startRecording();
                } else {
                    stopRecording();
                }
            }
        });
    }

    private void startRecording() {
        isRecording = true;
        recordButton.setText("Stop Recording");

        new Thread(new Runnable() {
            @Override
            public void run() {
                AudioRecord recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT));

                recorder.startRecording();

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buffer = new byte[4096];
                int bytesRead;

                while (isRecording) {
                    bytesRead = recorder.read(buffer, 0, buffer.length);
                    if (bytesRead > 0) {
                        baos.write(buffer, 0, bytesRead);
                    }
                }

                recorder.stop();
                recorder.release();

                // Save audio to file and send to API
                sendAudioToApi(baos.toByteArray());
            }
        }).start();
    }

    private void stopRecording() {
        isRecording = false;
        recordButton.setText("Start Recording");
    }

    private void sendAudioToApi(byte[] audioData) {
        try {
            // Save audio to a temporary file
            File tempFile = File.createTempFile("audio",".wav", getCacheDir());
            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                fos.write(audioData);
            }

            // Log the file details
            Log.i("Audio Upload", "File name: " + tempFile.getName());
            Log.i("Audio Upload", "File path: " + tempFile.getAbsolutePath());
            Log.i("Audio Upload", "File size: " + tempFile.length() + " bytes");

            // Create a multipart request body
            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", tempFile.getName(),
                            RequestBody.create(MediaType.parse("audio/wav"), tempFile))
                    .build();

            // Create the request
            Request request = new Request.Builder()
                    .url("http://10.0.2.2:8000/transcribe/")
                    .post(requestBody)
                    .build();

            // Execute the request
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    Log.e("API Error", e.getMessage());
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    String responseData = response.body().string();
                    Log.d("API Response", responseData);
                    if (response.isSuccessful()) {
                        runOnUiThread(() -> transcribedText.setText(responseData));
                    } else {
                        Log.e("API Error", "Response not successful: " + response.code());
                        Log.e("API Error", responseData);
                    }
                }
            });

        } catch (IOException e) {
            Log.e("File Error", e.getMessage());
        }
    }




    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        permissionToRecordAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
        if (!permissionToRecordAccepted) finish();
    }
}
