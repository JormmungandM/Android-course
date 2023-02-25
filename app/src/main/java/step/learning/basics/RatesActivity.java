package step.learning.basics;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class RatesActivity extends AppCompatActivity {
    private TextView tvJson;
    private String content;
    private List<Rate> rates;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rates);

        tvJson = findViewById(R.id.tvJson);
        new Thread(this::loadUrl).start();
    }

    private void loadUrl() {
        try(InputStream inputStream = new URL("https://bank.gov.ua/NBUStatService/v1/statdirectory/exchange?json").openStream()) {
            StringBuilder sb = new StringBuilder();
            int symbol;
            while((symbol = inputStream.read()) != -1) {
                sb.append((char)symbol);
            }
            content = new String(
                    sb.toString().getBytes(StandardCharsets.ISO_8859_1),
                    StandardCharsets.UTF_8);
            new Thread(this::parseContent).start();
        }
        catch(android.os.NetworkOnMainThreadException ex) {
            Log.d("loadUrl", "NetworkOnMainThreadException: " + ex.getMessage());
        }
        catch(MalformedURLException ex) {
            Log.d("loadUrl", "MalformedURLException: " + ex.getMessage());
        }
        catch(IOException ex) {
            Log.d("loadUrl", "IOException: " + ex.getMessage());
        }
    }

    private void parseContent() {
        rates=new ArrayList<>();
        try {
            JSONArray jRates = new JSONArray(content);
            for(int i = 0; i < jRates.length(); ++i) {
                rates.add(new Rate(jRates.getJSONObject(i)));
            }
            //new Thread(this::showRatesOld).start();
            runOnUiThread(this::showRates);
        }
        catch(JSONException ex) {
            Log.d("parseContent()", ex.getMessage());
        }
    }

    private void showRates() {
        LinearLayout container = findViewById(R.id.ratesContainer);

        Drawable ratesBg = AppCompatResources.getDrawable(getApplicationContext(), R.drawable.rates_bg);
        Drawable oddRatesBg = AppCompatResources.getDrawable(getApplicationContext(), R.drawable.rates_bg_odd);

        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        layoutParams.setMargins(7,5,7,5);

        LinearLayout.LayoutParams oodLayoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        oodLayoutParams.setMargins(7,5,7,5);
        oodLayoutParams.gravity = Gravity.END;

        boolean isOdd = false;
        for(Rate rate : this.rates) {
            TextView tv = new TextView(this);
            tv.setText(rate.getTxt() + "; " + rate.getR030() + "; " + rate.getRate() + "; " + rate.getCC());
            if(isOdd) {
                tv.setBackground(oddRatesBg);
                tv.setLayoutParams(oodLayoutParams);
                tv.setTextColor(Color.BLACK);
            }
            else {
                tv.setBackground(ratesBg);
                tv.setLayoutParams(layoutParams);
                tv.setTextColor(Color.BLACK);
            }
            isOdd = !isOdd;
            tv.setPadding(7, 5, 7, 5);
            container.addView(tv);
        }
    }

//    private void showRatesOld() {
//        StringBuilder str = new StringBuilder();
//        str.append("Date: ").append(rates.get(0).getExchangeDate()).append("\n");
//        str.append("Format: ").append("Name;\tr030;\tRate;\tCC").append("\n\n");
//        rates.forEach(rate -> {
//            str.append(rate.getTxt()).append(";  ");
//            str.append(rate.getR030()).append("; ");
//            str.append(rate.getRate()).append("; ");
//            str.append(rate.getCC()).append(".\n\n");
//        });
//
//        runOnUiThread(() -> tvJson.setText(str.toString()));
//    }

    static class Rate {
        private int r030;
        private String txt;
        private double rate;
        private String cc;
        private String exchangeDate;

        public Rate(JSONObject obj) throws JSONException {
            setR030(obj.getInt("r030"));
            setTxt(obj.getString("txt"));
            setRate(obj.getDouble("rate"));
            setCc(obj.getString("cc"));
            setExchangeDate(obj.getString("exchangedate"));
        }
        public String getExchangeDate() {
            return exchangeDate;
        }

        public void setExchangeDate(String exchangeDate) {
            this.exchangeDate = exchangeDate;
        }

        public String getCC() {
            return cc;
        }

        public void setCc(String cc) {
            this.cc = cc;
        }

        public double getRate() {
            return rate;
        }

        public void setRate(double rate) {
            this.rate = rate;
        }

        public String getTxt() {
            return txt;
        }

        public void setTxt(String txt) {
            this.txt = txt;
        }

        public int getR030() {
            return r030;
        }

        public void setR030(int r030) {
            this.r030 = r030;
        }
    }
}