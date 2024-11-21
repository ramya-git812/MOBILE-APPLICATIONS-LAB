package com.example.imageclassifier;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.imageclassifier.ml.Yolo11n;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;
import org.tensorflow.lite.support.image.TensorImage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    TextView result, confidence;
    ImageView imageView;
    Button picture;
    int imageSize = 224;  // Ensure this is the correct input size for your YOLO model

    // Replace this with your actual class labels from the YOLO model
    String[] classes = {"Class1", "Class2", "Class3", "Class4"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        result = findViewById(R.id.result);
        confidence = findViewById(R.id.confidence);
        imageView = findViewById(R.id.imageView);
        picture = findViewById(R.id.button);

        picture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Launch camera if we have permission
                if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    startActivityForResult(cameraIntent, 1);
                } else {
                    // Request camera permission if we don't have it.
                    requestPermissions(new String[]{Manifest.permission.CAMERA}, 100);
                }
            }
        });
    }

    public void classifyImage(Bitmap image) {
        try {
            // Initialize the YOLO model
            Yolo11n model = Yolo11n.newInstance(getApplicationContext());

            // Create a TensorImage from the Bitmap (this automatically handles the image preprocessing)
            TensorImage tensorImage = TensorImage.fromBitmap(image);

            // Run inference on the image using the YOLO model
            Yolo11n.Outputs outputs = model.process(tensorImage);

            // Access the output as a tensor buffer
            TensorBuffer outputFeature = outputs.getOutputAsTensorBuffer();  // Use the correct method here

            // Process the model output
            float[] modelOutput = outputFeature.getFloatArray();

            // Here, you need to decode the raw model output (bounding boxes, class scores, etc.)
            List<String> detectedObjects = processModelOutput(modelOutput);

            // Prepare the result text
            String resultText = "";
            for (String object : detectedObjects) {
                resultText += object + "\n";
            }

            // Set the result text in the UI
            result.setText(resultText);

            // Release model resources when done
            model.close();
        } catch (IOException e) {
            // Handle the exception properly (e.g., show a Toast or log the error)
            e.printStackTrace();
        }
    }

    private List<String> processModelOutput(float[] modelOutput) {
        List<String> detectedObjects = new ArrayList<>();

        // Example: YOLO typically outputs an array in the following format:
        // [x, y, width, height, confidence, class_scores...]
        // You may need to adjust this parsing according to your specific model output.

        // Process the output in chunks (assuming each object has 6 elements: 4 for the box + 1 for confidence + 1 for class index)
        int numOfBoxes = modelOutput.length / 6; // assuming 6 floats per box (modify according to your model output)

        for (int i = 0; i < numOfBoxes; i++) {
            // Extract the confidence and class index for each detected box
            float confidenceScore = modelOutput[i * 6 + 4]; // confidence at index 4 (5th element)

            if (confidenceScore > 0.5) {  // Only consider predictions with high confidence
                int classIndex = (int) modelOutput[i * 6 + 5]; // The class index at position 5 (6th element)
                String label = classes[classIndex];
                detectedObjects.add(label + ": " + (confidenceScore * 100) + "%");
            }
        }

        return detectedObjects;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == 1 && resultCode == RESULT_OK) {
            // Get the image from the camera intent
            Bitmap image = (Bitmap) data.getExtras().get("data");

            // Resize the image to a square (YOLO usually works with square images)
            int dimension = Math.min(image.getWidth(), image.getHeight());
            image = ThumbnailUtils.extractThumbnail(image, dimension, dimension);
            imageView.setImageBitmap(image);

            // Scale the image to the correct input size (e.g., 224x224 for YOLO)
            image = Bitmap.createScaledBitmap(image, imageSize, imageSize, false);

            // Classify the image using the YOLO model
            classifyImage(image);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
