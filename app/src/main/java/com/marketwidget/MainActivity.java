package com.marketwidget;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {

    private static final int BG = 0xFF0D1117;
    private static final int BG2 = 0xFF161B22;
    private static final int TEXT = 0xFFEEEEEE;
    private static final int MUTED = 0x99FFFFFF;
    private static final int DIV = 0x22FFFFFF;

    private TextView tvUpdated;
    private TextView[] priceViews = new TextView[6];
    private TextView[] changeViews = new TextView[6];

    private final String[] SYMS  = {"BTC","ETH","SOL","BRENT","XAU","USD/₽"};
    private final String[] NAMES = {"Bitcoin","Ethereum","Solana","Нефть Brent","Золото","Доллар США"};
    private final int[]    COLORS= {0xFFF7931A,0xFF627EEA,0xFF9945FF,0xFF2ecc71,0xFFFFD700,0xFF4fa3ff};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        getWindow().setStatusBarColor(BG);

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(BG);
        scroll.setFillViewport(true);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        root.setPadding(dp(16), dp(48), dp(16), dp(24));

        // Header
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams hParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        hParams.setMargins(0, 0, 0, dp(20));
        header.setLayoutParams(hParams);

        TextView tvTitle = new TextView(this);
        tvTitle.setText("Рынки");
        tvTitle.setTextColor(TEXT);
        tvTitle.setTextSize(26);
        tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
        tvTitle.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        tvUpdated = new TextView(this);
        tvUpdated.setText("обновление...");
        tvUpdated.setTextColor(MUTED);
        tvUpdated.setTextSize(11);
        tvUpdated.setTypeface(Typeface.MONOSPACE);

        TextView refreshBtn = new TextView(this);
        refreshBtn.setText("  ↻");
        refreshBtn.setTextColor(0xFF4fa3ff);
        refreshBtn.setTextSize(20);
        refreshBtn.setOnClickListener(v -> fetchData());

        header.addView(tvTitle);
        header.addView(tvUpdated);
        header.addView(refreshBtn);
        root.addView(header);

        // Cards
        String[] sections = {"КРИПТО", "СЫРЬЁ И ВАЛЮТА"};
        int[] sectionStart = {0, 3};
        int[] sectionEnd   = {3, 6};

        for (int s = 0; s < 2; s++) {
            TextView secLabel = new TextView(this);
            secLabel.setText(sections[s]);
            secLabel.setTextColor(MUTED);
            secLabel.setTextSize(10);
            secLabel.setTypeface(Typeface.MONOSPACE);
            secLabel.setLetterSpacing(0.12f);
            LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            slp.setMargins(dp(4), s == 0 ? 0 : dp(20), 0, dp(8));
            secLabel.setLayoutParams(slp);
            root.addView(secLabel);

            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setBackgroundColor(BG2);
            card.setPadding(dp(16), dp(8), dp(16), dp(8));
            // rounded via tag workaround — set bg programmatically
            android.graphics.drawable.GradientDrawable cardBg = new android.graphics.drawable.GradientDrawable();
            cardBg.setColor(BG2);
            cardBg.setCornerRadius(dp(16));
            card.setBackground(cardBg);

            for (int i = sectionStart[s]; i < sectionEnd[s]; i++) {
                if (i > sectionStart[s]) {
                    View div = new View(this);
                    div.setBackgroundColor(DIV);
                    LinearLayout.LayoutParams dp1 = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 1);
                    dp1.setMargins(0, dp(2), 0, dp(2));
                    div.setLayoutParams(dp1);
                    card.addView(div);
                }
                card.addView(makeRow(i));
            }
            root.addView(card);
        }

        scroll.addView(root);
        setContentView(scroll);

        startService(new Intent(this, UpdateService.class));
        fetchData();
    }

    private LinearLayout makeRow(int i) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(52));
        row.setLayoutParams(rp);

        // Icon circle
        TextView icon = new TextView(this);
        icon.setText(SYMS[i].length() > 3 ? SYMS[i].substring(0,3) : SYMS[i]);
        icon.setTextColor(COLORS[i]);
        icon.setTextSize(9);
        icon.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        icon.setGravity(Gravity.CENTER);
        android.graphics.drawable.GradientDrawable iconBg = new android.graphics.drawable.GradientDrawable();
        iconBg.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        int alpha = 0x22000000 | (COLORS[i] & 0x00FFFFFF);
        iconBg.setColor(alpha);
        icon.setBackground(iconBg);
        LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(dp(40), dp(40));
        ip.setMargins(0, 0, dp(12), 0);
        icon.setLayoutParams(ip);

        // Name col
        LinearLayout nameCol = new LinearLayout(this);
        nameCol.setOrientation(LinearLayout.VERTICAL);
        nameCol.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView sym = new TextView(this);
        sym.setText(SYMS[i]);
        sym.setTextColor(TEXT);
        sym.setTextSize(14);
        sym.setTypeface(Typeface.DEFAULT_BOLD);

        TextView name = new TextView(this);
        name.setText(NAMES[i]);
        name.setTextColor(MUTED);
        name.setTextSize(11);

        nameCol.addView(sym);
        nameCol.addView(name);

        // Price col
        LinearLayout priceCol = new LinearLayout(this);
        priceCol.setOrientation(LinearLayout.VERTICAL);
        priceCol.setGravity(Gravity.END);
        priceCol.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        priceViews[i] = new TextView(this);
        priceViews[i].setText("—");
        priceViews[i].setTextColor(TEXT);
        priceViews[i].setTextSize(14);
        priceViews[i].setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        priceViews[i].setGravity(Gravity.END);

        changeViews[i] = new TextView(this);
        changeViews[i].setText("");
        changeViews[i].setTextColor(MUTED);
        changeViews[i].setTextSize(11);
        changeViews[i].setTypeface(Typeface.MONOSPACE);
        changeViews[i].setGravity(Gravity.END);

        priceCol.addView(priceViews[i]);
        priceCol.addView(changeViews[i]);

        row.addView(icon);
        row.addView(nameCol);
        row.addView(priceCol);
        return row;
    }

    private void fetchData() {
        new Thread(() -> {
            double btcP=0,btcC=0,ethP=0,ethC=0,solP=0,solC=0,goldP=0,goldC=0,usdRub=0,brentP=0,brentC=0;
            try {
                String r = httpGet("https://api.coingecko.com/api/v3/simple/price?ids=bitcoin,ethereum,solana,gold,tether&vs_currencies=usd,rub&include_24hr_change=true");
                JSONObject j = new JSONObject(r);
                if (j.has("bitcoin"))  { btcP=j.getJSONObject("bitcoin").optDouble("usd",0);   btcC=j.getJSONObject("bitcoin").optDouble("usd_24h_change",0); }
                if (j.has("ethereum")) { ethP=j.getJSONObject("ethereum").optDouble("usd",0);  ethC=j.getJSONObject("ethereum").optDouble("usd_24h_change",0); }
                if (j.has("solana"))   { solP=j.getJSONObject("solana").optDouble("usd",0);    solC=j.getJSONObject("solana").optDouble("usd_24h_change",0); }
                if (j.has("gold"))     { goldP=j.getJSONObject("gold").optDouble("usd",0);     goldC=j.getJSONObject("gold").optDouble("usd_24h_change",0); }
                if (j.has("tether"))   { usdRub=j.getJSONObject("tether").optDouble("rub",0); }
            } catch (Exception e) { e.printStackTrace(); }
            try {
                String r = httpGet("https://query1.finance.yahoo.com/v8/finance/chart/BZ=F?interval=1d&range=1d");
                JSONObject meta = new JSONObject(r).getJSONObject("chart").getJSONArray("result").getJSONObject(0).getJSONObject("meta");
                brentP = meta.optDouble("regularMarketPrice",0);
                double prev = meta.optDouble("previousClose", brentP);
                if (prev > 0) brentC = ((brentP - prev) / prev) * 100.0;
            } catch (Exception e) { e.printStackTrace(); }

            final double[] prices = {btcP,ethP,solP,brentP,goldP,usdRub};
            final double[] changes = {btcC,ethC,solC,brentC,goldC,0};
            final boolean[] noChange = {false,false,false,false,false,true};
            final String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());

            new Handler(Looper.getMainLooper()).post(() -> {
                tvUpdated.setText(time);
                for (int i = 0; i < 6; i++) {
                    String pStr;
                    if (i == 5) pStr = String.format(Locale.US,"%.2f₽", prices[i]);
                    else if (prices[i] >= 10000) pStr = String.format(Locale.US,"$%,.0f", prices[i]);
                    else if (prices[i] >= 100)   pStr = String.format(Locale.US,"$%.2f",  prices[i]);
                    else                          pStr = String.format(Locale.US,"$%.2f",  prices[i]);
                    priceViews[i].setText(pStr);
                    if (!noChange[i]) {
                        double ch = changes[i];
                        String chStr = (ch >= 0 ? "+" : "") + String.format(Locale.US,"%.2f%%", ch);
                        changeViews[i].setText(chStr);
                        changeViews[i].setTextColor(ch >= 0 ? 0xFF00e676 : 0xFFff3d57);
                    }
                }
            });
        }).start();
    }

    private String httpGet(String urlStr) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection c = (HttpURLConnection) url.openConnection();
        c.setConnectTimeout(12000);
        c.setReadTimeout(12000);
        c.setRequestProperty("User-Agent","MarketWidget/1.0");
        BufferedReader br = new BufferedReader(new InputStreamReader(c.getInputStream()));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) sb.append(line);
        br.close();
        return sb.toString();
    }

    private int dp(int val) {
        return Math.round(val * getResources().getDisplayMetrics().density);
    }
}
