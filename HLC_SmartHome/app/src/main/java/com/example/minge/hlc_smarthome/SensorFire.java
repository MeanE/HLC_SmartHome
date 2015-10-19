package com.example.minge.hlc_smarthome;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.IBinder;
import android.support.v7.app.NotificationCompat;
import android.view.View;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by MingE on 2015/9/28.
 */
public class SensorFire extends Sensor {
    TextView tv_fire;

    int lastId = -1, id = 0;
    boolean isError = false;

    final int NOTIFICATION_ID = 0xe1;

    Thread bindThread = null;

    @Override
    protected void setURL() {
        String channelID = "55748"; //瓦斯爐(火焰)
        String key = "MVAVXSAMB8XZQYEO";
        String urlString = "http://api.thingspeak.com/channels/" + channelID + "/feed/last.json" +
                "?key=" + key;
        try {
            url = new URL(urlString);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void initUI(Activity act, View v) {
        this.act = act;
        this.v = v;

        tv_fire = (TextView) v.findViewById(R.id.tv_fire);
    }

    @Override
    protected JSONObject getJSON() {
        JSONObject jsonObj = null;
        try {
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    connection.getInputStream(), "UTF-8"));
            String jsonString = reader.readLine();
            reader.close();

            jsonObj = new JSONObject(jsonString);
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return jsonObj;
    }

    @Override
    public void onCreate() {
        setURL();

        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        JSONObject jsonObj = getJSON();

                        //int status = Integer.parseInt(jsonObj.get("field1").toString());
                        id = Integer.parseInt(jsonObj.get("entry_id").toString());

                        if (lastId != id && lastId != -1) {
                            //if (intent.getStringExtra("onDestroy").equals("1"))
                            if (act.isDestroyed())
                                setUpNotification();
                            isError = true;
                            Thread.sleep(18000);
                        } else {
                            Thread.sleep(1000);
                        }

                        lastId = id;
                    } catch (JSONException e) {
                        e.printStackTrace();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    protected void setUpNotification() {
        NotificationCompat.Builder notifcationCompatBuilder = new NotificationCompat.Builder(this);
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_fire);
        notifcationCompatBuilder.setWhen(System.currentTimeMillis())
                .setSmallIcon(R.mipmap.logo)
                .setLargeIcon(bitmap)
                .setContentTitle("瓦斯爐")
                .setContentText("偵測到火源，請盡速確認是否關閉！")
                .setDefaults(Notification.DEFAULT_ALL);

        Notification notification = notifcationCompatBuilder.build();
        notification.flags |= Notification.FLAG_AUTO_CANCEL;

        Intent intent = new Intent(this, MainActivity.class);
        //click notification return MainActivity
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 4,
                intent, PendingIntent.FLAG_UPDATE_CURRENT);
        notification.contentIntent = pendingIntent;

        NotificationManager notificationManager = (NotificationManager) getSystemService(this.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID, notification);
        //startForeground(NOTIFICATION_ID, notification);
    }

    @Override
    protected void bindServiceToDead(){
        bindThread.interrupt();
        bindThread = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new MyBinder();
    }

    class MyBinder extends Binder {
        SensorFire getService() {
            return SensorFire.this;
        }

        void start() {
            bindThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    while (!act.isDestroyed()) {
                        try {
                            if (lastId != -1 && isError) {
                                act.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        tv_fire.setText("開啟");
                                        tv_fire.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_exclamation, 0);
                                    }
                                });
                                isError = false;
                                Thread.sleep(18000);
                            } else {
                                act.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        tv_fire.setText("未開啟");
                                        tv_fire.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_check, 0);
                                    }
                                });
                                Thread.sleep(1000);
                            }

                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            });

            bindThread.start();
        }
    }
}
