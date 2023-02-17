package step.learning.basics;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

public class CalculatorActivity extends AppCompatActivity {
    private TextView tvHistory;
    private TextView tvResult;

    private String minusSign;

    private double argument1;
    private double argument2;
    private String operation;
    private boolean needClear;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calculator);

        tvHistory = findViewById(R.id.tvHistory);
        tvResult = findViewById(R.id.tvResult);
        tvHistory.setText("");
        tvResult.setText("0");
        minusSign = getString(R.string.minus_sign);
        argument2 = 0;
        int[] digitButtons = { R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4, R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9 };
        for (int digit : digitButtons) {
            findViewById(digit).setOnClickListener(this::digitClick);
        }
        findViewById(R.id.btnPlusMinus).setOnClickListener(this::pmClick);
        findViewById(R.id.btnComa).setOnClickListener(this::comaClick);
        findViewById(R.id.btnInverse).setOnClickListener(this::inverseClick);
        findViewById(R.id.btnSqrt).setOnClickListener(this::sqrtClick);
        findViewById(R.id.btnClearE).setOnClickListener(this::clearEClick);
        findViewById(R.id.btnClear).setOnClickListener(this::clearClick);
        findViewById(R.id.btnBackspace).setOnClickListener(this::backspaceClick);
        findViewById(R.id.btnPlus).setOnClickListener(this::operationClick);
        findViewById(R.id.btnMultiply).setOnClickListener(this::operationClick);
        findViewById(R.id.btnDivide).setOnClickListener(this::operationClick);
        findViewById(R.id.btnMinus).setOnClickListener(this::operationClick);
        findViewById(R.id.btnEquals).setOnClickListener(this::equalsClick);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putCharSequence("history", tvHistory.getText());
        outState.putCharSequence("result", tvResult.getText());
        Log.d("onSaveInstanceState", "Data is saved");
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        tvHistory.setText(savedInstanceState.getCharSequence("history"));
        tvResult.setText(savedInstanceState.getCharSequence("result"));
        Log.d("onRestoreInstanceState", "Data is restored");
    }

    private void pmClick(View v) {
        String result = tvResult.getText().toString();
        if(result.equals("0")) return;

        if(result.startsWith(minusSign)) {
            result = result.substring(1);
        }
        else {
            result = minusSign + result;
        }
        tvResult.setText(result);
    }

    private void comaClick(View v) {
        String coma = ".";
        String result = tvResult.getText().toString();
        if(getNumberOfDigits(result) >= 10) return;
        if(result.contains(coma)) return;
        result += coma;
        tvResult.setText(result);
    }

    private void digitClick(View v) {
        String result = tvResult.getText().toString();
        String digit = ((Button)v).getText().toString();
        if(getNumberOfDigits(result) >= 10) return;
        if(result.equals("0") || needClear) {
            result = digit;
            needClear = false;
        }
        else {
            result += digit;
        }
        tvResult.setText(result);
        argument2 = getArgument(result);
    }

    private void inverseClick(View v) {
        String result = tvResult.getText().toString();
        double arg = getArgument(result);
        if(arg == 0) {
            alert(R.string.calc_division_by_zero_error);
            return;
        }
        tvHistory.setText(String.format("1/(%s)", result));
        arg = 1 / arg;
        setArgument(arg);
    }

    private void sqrtClick(View v) {
        String result = tvResult.getText().toString();
        double arg = getArgument(result);
        if(arg < 0) {
            alert(R.string.calc_negative_sqrt_error);
            return;
        }
        if(arg == 0)
            return;
        tvHistory.setText(String.format("sqrt(%s)",  result));
        arg = Math.sqrt(arg);
        setArgument(arg);
    }

    private void clearEClick(View v) {
        tvResult.setText("0");
    }

    private void clearClick(View v) {
        tvResult.setText("0");
        tvHistory.setText("0");
        argument1 = 0;
        argument2 = 0;
        operation = null;
    }

    private void backspaceClick(View v) {
        String result = tvResult.getText().toString();
        int len = result.length();
        if(len <= 1) {
            tvResult.setText("0");
            return;
        }
        result = result.substring(0, len - 1);
        if(result.equals(minusSign)) {
            result = "0";
        }
        tvResult.setText(result);
    }

    private void operationClick(View v) {
        operation = ((Button)v).getText().toString();
        String result = tvResult.getText().toString();
        argument1 = getArgument(result);
        tvHistory.setText(result + " " + operation);
        needClear = true;
    }

    private void equalsClick(View v) {
        if(operation == null) {
            return;
        }
        String result = tvResult.getText().toString();

        if(tvHistory.getText().toString().contains("=")) {
            argument1 = getArgument(result);
            String arg2 = getResult(argument2);
            tvHistory.setText(tvResult.getText().toString() + " " + operation + " " + arg2 + " = ");
        }
        else {
            tvHistory.setText(tvHistory.getText().toString() + " " + result + " = ");
        }
        if(operation.equals(getString(R.string.btn_plus_text))) {
            setArgument(argument1 + argument2);
        }
        if(operation.equals(getString(R.string.btn_minus_text))) {
            setArgument(argument1 - argument2);
        }
        if(operation.equals(getString(R.string.btn_multiply_text))) {
            setArgument(argument1 * argument2);
        }
        if(operation.equals(getString(R.string.btn_divide_text))) {
            setArgument(argument1 / argument2);
        }
    }

    private void setArgument(double arg) {
        String result = getResult(arg);
        tvResult.setText(result.replace("-", minusSign));
    }

    private String getResult(double arg) {
        String result = String.format(Locale.getDefault(), "%.10f", arg);
        while(result.endsWith("0") && result.contains(".") || result.endsWith(".")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    private double getArgument(String resultText) {
        return Double.parseDouble(resultText.replace(minusSign, "-"));
    }

    private void alert(int messageId) {
        Toast.makeText(CalculatorActivity.this, messageId, Toast.LENGTH_SHORT).show();
        long[] vibrationPattern = { 0, 200, 100, 200 };
        Vibrator vibrator;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            VibratorManager vibratorManager = (VibratorManager)getSystemService(VIBRATOR_MANAGER_SERVICE);
            vibrator = vibratorManager.getDefaultVibrator();
        }
        else {
            vibrator = (Vibrator)getSystemService(VIBRATOR_SERVICE);
        }
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(vibrationPattern, -1));
        }
        else {
            vibrator.vibrate(vibrationPattern, -1);
        }
    }

    private int getNumberOfDigits(String str) {
        int number = 0;
        for(int i = 0; i < str.length(); i++) {
            if(Character.isDigit(str.charAt(i))) {
                number++;
            }
        }
        return number;
    }
}