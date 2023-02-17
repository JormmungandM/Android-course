package step.learning.basics;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.exitButton).setOnClickListener(this::exitButtonClick);

        findViewById(R.id.calcButton).setOnClickListener(this::calcButtonClick);
    }

    private void exitButtonClick(View v) {
        finish();
    }

    private void calcButtonClick(View v) {
        Intent calcIntent = new Intent(this, CalculatorActivity.class);
        startActivity(calcIntent);
    }
}