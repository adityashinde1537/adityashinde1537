package in.cresto.tracker;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONObject;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int ASK_GPS = 130;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private TextView state, coordinates, speed, distance, accuracy, elapsed, substatus;
    private Button start, stop, openMap;
    private boolean registered = false;
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) { refresh(); }
    };

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(Color.rgb(7, 19, 36));
        getWindow().setNavigationBarColor(Color.rgb(7, 19, 36));
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.rgb(7, 19, 36));
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(20), dp(30), dp(20), dp(26));
        scroll.addView(body);

        TextView brand = txt("CRESTO", 32, 0xFF77E9BB, true);
        brand.setLetterSpacing(.11f);
        body.addView(brand);
        body.addView(txt("CAMPUS TRANSPORT  /  GPS DRIVER DEMO", 11, 0xFF96A9BD, true));
        gap(body, 26);

        LinearLayout banner = box();
        body.addView(banner);
        banner.addView(txt("DRIVER TRACKING", 12, 0xFF80F2C5, true));
        state = txt("READY TO START", 25, Color.WHITE, true);
        banner.addView(state);
        substatus = txt("Tap Start Tracking to use this phone's real GPS.", 13, 0xFFABBACB, false);
        banner.addView(substatus);
        gap(body, 18);

        LinearLayout info = box();
        body.addView(info);
        info.addView(txt("CURRENT GPS COORDINATES", 12, 0xFF80F2C5, true));
        coordinates = txt("No GPS fix yet", 20, Color.WHITE, true);
        info.addView(coordinates);
        gap(body, 18);

        LinearLayout metrics = new LinearLayout(this);
        metrics.setOrientation(LinearLayout.VERTICAL);
        body.addView(metrics);
        speed = metric(metrics, "SPEED", "0.0 km/h");
        distance = metric(metrics, "TRIP DISTANCE", "0.00 km");
        accuracy = metric(metrics, "GPS ACCURACY", "—");
        elapsed = metric(metrics, "DURATION", "00:00");
        gap(body, 16);

        start = btn("▶  START LIVE TRACKING", 0xFF61DFAC, 0xFF081D2D);
        body.addView(start);
        start.setOnClickListener(v -> startGps());
        gap(body, 9);

        stop = btn("■  END TRIP & STOP GPS", 0xFF253850, Color.WHITE);
        body.addView(stop);
        stop.setOnClickListener(v -> stopGps());
        gap(body, 9);

        openMap = btn("⌖  OPEN CURRENT LOCATION IN MAP", 0xFF253850, Color.WHITE);
        body.addView(openMap);
        openMap.setOnClickListener(v -> showMap());
        gap(body, 20);

        TextView privacy = txt("PRIVACY  •  Your GPS location stays on this phone. Tracking runs only after you tap Start. An ongoing notification is displayed during an active trip.", 12, 0xFF8FA5B8, false);
        body.addView(privacy);
        setContentView(scroll);
        refresh();
    }

    private int dp(int n) { return (int)(n * getResources().getDisplayMetrics().density + .5f); }
    private void gap(LinearLayout l, int n) { View v = new View(this); l.addView(v, new LinearLayout.LayoutParams(1, dp(n))); }
    private TextView txt(String s, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s); t.setTextColor(color); t.setTextSize(size);
        t.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        t.setPadding(0, dp(4), 0, dp(4));
        return t;
    }
    private LinearLayout box() {
        LinearLayout x = new LinearLayout(this);
        x.setOrientation(LinearLayout.VERTICAL);
        x.setPadding(dp(18), dp(16), dp(18), dp(16));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFF14273C); bg.setCornerRadius(dp(18));
        bg.setStroke(dp(1), 0xFF25415A);
        x.setBackground(bg);
        return x;
    }
    private TextView metric(LinearLayout dest, String label, String initial) {
        LinearLayout row = box();
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.bottomMargin = dp(9);
        dest.addView(row, p);
        row.addView(txt(label, 12, 0xFFA0B7CD, true));
        TextView value = txt(initial, 25, Color.WHITE, true);
        row.addView(value);
        return value;
    }
    private Button btn(String label, int bgColor, int fgColor) {
        Button b = new Button(this);
        b.setText(label); b.setTextSize(13); b.setAllCaps(false);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setTextColor(fgColor);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(bgColor); bg.setCornerRadius(dp(14));
        b.setBackground(bg);
        b.setMinHeight(dp(56));
        b.setPadding(dp(12), dp(12), dp(12), dp(12));
        return b;
    }

    @Override protected void onStart() {
        super.onStart();
        if (!registered) {
            IntentFilter f = new IntentFilter(GpsService.CHANGED);
            if (Build.VERSION.SDK_INT >= 33) registerReceiver(receiver, f, Context.RECEIVER_NOT_EXPORTED);
            else registerReceiver(receiver, f);
            registered = true;
        }
        refresh(); handler.post(tick);
    }
    @Override protected void onStop() {
        handler.removeCallbacks(tick);
        if (registered) { unregisterReceiver(receiver); registered = false; }
        super.onStop();
    }
    private final Runnable tick = new Runnable() {
        @Override public void run() { refresh(); handler.postDelayed(this, 1000); }
    };

    private void startGps() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, ASK_GPS);
            return;
        }
        Intent i = new Intent(this, GpsService.class).setAction(GpsService.START);
        try { startForegroundService(i); }
        catch (Exception e) { Toast.makeText(this, "GPS could not start: " + e.getMessage(), Toast.LENGTH_LONG).show(); }
        handler.postDelayed(this::refresh, 500);
    }
    private void stopGps() {
        Intent i = new Intent(this, GpsService.class).setAction(GpsService.STOP);
        try { startService(i); }
        catch (Exception e) { Toast.makeText(this, "Could not stop: " + e.getMessage(), Toast.LENGTH_LONG).show(); }
        handler.postDelayed(this::refresh, 400);
    }
    @Override public void onRequestPermissionsResult(int req, String[] permissions, int[] grants) {
        super.onRequestPermissionsResult(req, permissions, grants);
        if (req == ASK_GPS) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) startGps();
            else Toast.makeText(this, "Allow precise location to enable phone GPS tracking.", Toast.LENGTH_LONG).show();
        }
    }
    private void showMap() {
        try {
            String loc = getSharedPreferences("cresto_gps", MODE_PRIVATE).getString("position", "");
            if (loc.isEmpty()) { Toast.makeText(this, "Start tracking and wait for GPS fix.", Toast.LENGTH_LONG).show(); return; }
            JSONObject j = new JSONObject(loc);
            double lat = j.getDouble("lat"), lng = j.getDouble("lng");
            String url = String.format(Locale.US, "https://www.openstreetmap.org/?mlat=%.6f&mlon=%.6f#map=17/%.6f/%.6f", lat, lng, lat, lng);
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch(Exception e) { Toast.makeText(this, "Map unavailable", Toast.LENGTH_SHORT).show(); }
    }
    private void refresh() {
        if (state == null) return;
        android.content.SharedPreferences p = getSharedPreferences("cresto_gps", MODE_PRIVATE);
        boolean running = p.getBoolean("running", false);
        state.setText(running ? "●  GPS TRACKING LIVE" : "READY / STOPPED");
        state.setTextColor(running ? 0xFF78EBB9 : Color.WHITE);
        substatus.setText(p.getString("message", "Use this phone's GPS for tracking."));
        start.setEnabled(!running);
        start.setAlpha(running ? .5f : 1f);
        stop.setEnabled(running);
        stop.setAlpha(running ? 1f : .5f);
        distance.setText(String.format(Locale.US, "%.2f km", p.getFloat("meters", 0f) / 1000f));
        long ms = running ? System.currentTimeMillis() - p.getLong("started", System.currentTimeMillis()) : p.getLong("ended", 0) - p.getLong("started", 0);
        ms = Math.max(0, ms);
        elapsed.setText(String.format(Locale.US, "%02d:%02d:%02d", ms/3600000, ms/60000%60, ms/1000%60));
        String position = p.getString("position", "");
        if (position.isEmpty()) { coordinates.setText("Waiting for GPS fix…"); speed.setText("0.0 km/h"); accuracy.setText("—"); return; }
        try {
            JSONObject loc = new JSONObject(position);
            coordinates.setText(String.format(Locale.US, "%.6f,  %.6f", loc.getDouble("lat"), loc.getDouble("lng")));
            speed.setText(String.format(Locale.US, "%.1f km/h", loc.getDouble("speed")));
            accuracy.setText(String.format(Locale.US, "± %.0f m", loc.getDouble("accuracy")));
        } catch (Exception ignored) {}
    }
}
