package com.marketwidget;

import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Color;
import android.os.IBinder;
import android.widget.RemoteViews;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class UpdateService extends Service {

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        new Thread(this::fetchAndUpdate).start();
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }

    private String httpGet(String urlStr) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection c = (HttpURLConnection) url.openConnection();
        c.setConnectTimeout(12000);
        c.setReadTimeout(12000);
        c.setRequestProperty("User-Agent", "MarketWidget/1.0");
        BufferedReader br = new BufferedReader(new InputStreamReader(c.getInputStream()));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) sb.append(line);
        br.close();
        return sb.toString();
    }

    private void fetchAndUpdate() {
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
            brentP = meta.optDouble("regularMarketPrice", 0);
            double prev = meta.optDouble("previousClose", brentP);
            if (prev > 0) brentC = ((brentP - prev) / prev) * 100.0;
        } catch (Exception e) { e.printStackTrace(); }

        AppWidgetManager mgr = AppWidgetManager.getInstance(this);
        int[] ids = mgr.getAppWidgetIds(new ComponentName(this, MarketWidgetProvider.class));
        if (ids == null || ids.length == 0) { stopSelf(); return; }

        RemoteViews v = new RemoteViews(getPackageName(), R.layout.widget_layout);
        String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
        v.setTextViewText(R.id.tv_updated, time);

        setRow(v, 0, btcP >= 10000 ? String.format(Locale.US,"$%,.0f",btcP) : String.format(Locale.US,"$%.2f",btcP), btcC, false);
        setRow(v, 1, ethP >= 1000  ? String.format(Locale.US,"$%,.0f",ethP) : String.format(Locale.US,"$%.2f",ethP), ethC, false);
        setRow(v, 2, String.format(Locale.US,"$%.2f",solP), solC, false);
        setRow(v, 3, String.format(Locale.US,"$%.2f",brentP), brentC, false);
        setRow(v, 4, goldP >= 1000 ? String.format(Locale.US,"$%,.0f",goldP) : String.format(Locale.US,"$%.2f",goldP), goldC, false);
        setRow(v, 5, String.format(Locale.US,"%.2f₽",usdRub), 0, true);

        for (int id : ids) mgr.updateAppWidget(id, v);
        stopSelf();
    }

    private void setRow(RemoteViews v, int i, String price, double change, boolean noChange) {
        int[] pIds = {R.id.price_0,R.id.price_1,R.id.price_2,R.id.price_3,R.id.price_4,R.id.price_5};
        int[] cIds = {R.id.change_0,R.id.change_1,R.id.change_2,R.id.change_3,R.id.change_4,R.id.change_5};
        v.setTextViewText(pIds[i], price);
        if (noChange) {
            v.setTextViewText(cIds[i], "");
        } else {
            String ch = (change >= 0 ? "+" : "") + String.format(Locale.US,"%.2f%%",change);
            v.setTextViewText(cIds[i], ch);
            v.setTextColor(cIds[i], change >= 0 ? Color.parseColor("#00e676") : Color.parseColor("#ff3d57"));
        }
    }
}
