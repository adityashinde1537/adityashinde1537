package in.cresto.tracker;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import org.json.JSONObject;
import java.util.Locale;

public class GpsService extends Service implements LocationListener {
    public static final String START = "in.cresto.tracker.START";
    public static final String STOP = "in.cresto.tracker.STOP";
    public static final String CHANGED = "in.cresto.tracker.CHANGED";
    private static final String CHANNEL = "cresto_location";
    private static final int NOTIF_ID = 23;
    private LocationManager lm;
    private Location last;
    private boolean active = false;

    @Override public void onCreate() {
        super.onCreate();
        lm = (LocationManager)getSystemService(LOCATION_SERVICE);
    }
    @Override public int onStartCommand(Intent intent, int flags, int id) {
        if (intent != null && STOP.equals(intent.getAction())) {
            finish(); return START_NOT_STICKY;
        }
        if (!active) begin();
        return active ? START_STICKY : START_NOT_STICKY;
    }
    private SharedPreferences prefs() { return getSharedPreferences("cresto_gps", MODE_PRIVATE); }
    private void begin() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            writeMessage("Precise GPS location permission is required."); stopSelf(); return;
        }
        if (lm == null || !lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            writeMessage("Enable your phone's Location / GPS in Settings."); stopSelf(); return;
        }
        NotificationManager nm = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        nm.createNotificationChannel(new NotificationChannel(CHANNEL, "CRESTO GPS tracking", NotificationManager.IMPORTANCE_LOW));
        try {
            Notification n = notification("Waiting for GPS signal");
            if (Build.VERSION.SDK_INT >= 29) startForeground(NOTIF_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
            else startForeground(NOTIF_ID, n);
        } catch(Exception e) {
            writeMessage("GPS service could not start: " + e.getMessage()); stopSelf(); return;
        }
        active = true; last = null;
        prefs().edit().putBoolean("running", true).putLong("started", System.currentTimeMillis())
            .putLong("ended", 0).putFloat("meters", 0f).putString("position", "")
            .putString("message", "Searching for GPS satellites…").apply();
        notifyUi();
        try {
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000L, 2f, this, Looper.getMainLooper());
            if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
                lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5000L, 5f, this, Looper.getMainLooper());
        } catch(SecurityException e) { writeMessage("Location permission removed."); finish(); }
    }
    @Override public void onLocationChanged(Location loc) {
        if (!active || loc == null) return;
        try {
            float total = prefs().getFloat("meters", 0);
            if (last != null && loc.hasAccuracy() && loc.getAccuracy() <= 45f) {
                float delta = last.distanceTo(loc);
                if (delta >= 3f && delta <= 250f) total += delta;
            }
            last = loc;
            JSONObject j = new JSONObject();
            j.put("lat", loc.getLatitude()); j.put("lng", loc.getLongitude());
            j.put("speed", loc.hasSpeed() ? Math.max(0, loc.getSpeed() * 3.6f) : 0.0f);
            j.put("accuracy", loc.hasAccuracy() ? loc.getAccuracy() : -1.0f);
            prefs().edit().putString("position", j.toString()).putFloat("meters", total)
                .putString("message", "Receiving live coordinates from " + loc.getProvider().toUpperCase(Locale.US)).apply();
            NotificationManager nm = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
            nm.notify(NOTIF_ID, notification(String.format(Locale.US, "%.5f, %.5f", loc.getLatitude(), loc.getLongitude())));
            notifyUi();
        } catch(Exception e) { writeMessage("Could not read GPS position."); }
    }
    private Notification notification(String msg) {
        int flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        PendingIntent open = PendingIntent.getActivity(this, 1, new Intent(this, MainActivity.class), flags);
        PendingIntent stop = PendingIntent.getService(this, 2, new Intent(this, GpsService.class).setAction(STOP), flags);
        return new Notification.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle("CRESTO • GPS Tracking Active")
            .setContentText(msg).setContentIntent(open)
            .setOngoing(true).setOnlyAlertOnce(true)
            .addAction(android.R.drawable.ic_media_pause, "End trip", stop).build();
    }
    private void finish() {
        active = false;
        if (lm != null) lm.removeUpdates(this);
        prefs().edit().putBoolean("running", false)
            .putLong("ended", System.currentTimeMillis())
            .putString("message", "Trip ended. GPS tracking stopped.").apply();
        notifyUi();
        stopForeground(STOP_FOREGROUND_REMOVE);
        stopSelf();
    }
    private void writeMessage(String msg) {
        prefs().edit().putBoolean("running", false).putString("message", msg).apply();
        notifyUi();
    }
    private void notifyUi() {
        Intent i = new Intent(CHANGED);
        i.setPackage(getPackageName());
        sendBroadcast(i);
    }
    @Override public void onDestroy() {
        if (lm != null) lm.removeUpdates(this);
        if (active) {
            prefs().edit().putBoolean("running", false).putString("message", "GPS tracking stopped.").apply();
            notifyUi();
        }
        active = false;
        super.onDestroy();
    }
    @Override public IBinder onBind(Intent intent) { return null; }
}
