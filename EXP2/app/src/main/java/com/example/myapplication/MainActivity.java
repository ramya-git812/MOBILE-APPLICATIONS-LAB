package com.example.exp2a;
import android.graphics.Typeface;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.myapplication.R;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        TextView textView = findViewById(R.id.textView);
        Button changeTextButton = findViewById(R.id.changeTextButton);
        changeTextButton.setOnClickListener(view -> {
            textView.setTextSize(32); // Set text size to 32sp
            textView.setTypeface(Typeface.SERIF, Typeface.ITALIC); // Use built-in Serif font with bold style
            textView.setText("Welcome!"); // Change the text

            // Show a Toast message
            Toast.makeText(MainActivity.this, "Congratulations! Text Changed!", Toast.LENGTH_SHORT).show();
        });
    }
}