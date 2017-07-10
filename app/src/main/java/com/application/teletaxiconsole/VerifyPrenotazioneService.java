package com.application.teletaxiconsole;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.restlet.data.Protocol;
import org.restlet.engine.header.Header;
import org.restlet.engine.header.HeaderConstants;
import org.restlet.resource.ClientResource;
import org.restlet.resource.ResourceException;
import org.restlet.util.Series;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentMap;

import model.Prenotazione;

public class VerifyPrenotazioneService extends Service {

    private static Timer mTimer = null;
    private static int wakeUpTime = 30000;
    private Handler mHandler = new Handler();
    private static final String taxiPref = "TaxiPreferences";
    private static final int NOTIFICATION_ID = 2;
    private SharedPreferences sharedPreferences;
    private Gson gson;
    private static Worker worker;
    private static TimerTask timerTask;
    public static final String VERIFY_PRENOTAZIONE_SERVICE = "com.application.teletaxiconsole.VerifyPrenotazioneService";



    public VerifyPrenotazioneService() {
    }


    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate(){
        GsonBuilder gsonBuilder = new GsonBuilder().setDateFormat("dd/MM/yyyy HH:mm:ss");
        gson = gsonBuilder.create();
        if(mTimer != null) mTimer.cancel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        wakeUpTime = intent.getIntExtra("wakeUpTime", 5000);
        sharedPreferences = getApplicationContext().getSharedPreferences(taxiPref, Context.MODE_PRIVATE);
        mTimer = new Timer();
        final Prenotazione prenotazione = gson.fromJson(sharedPreferences.getString("idLastPrenotazione", null),Prenotazione.class);
        if(prenotazione != null) {
            mTimer.scheduleAtFixedRate(timerTask = new TimerTask() {
                @Override
                public void run() {
                    mHandler.post( worker = new Worker(prenotazione));
                }
            }, 0, wakeUpTime);
        }
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        if(mTimer != null){
            mHandler.removeCallbacks(worker);
            timerTask.cancel();
            mTimer.cancel();
            mTimer.purge();
            mTimer = null;
        }
    }

    class Worker implements Runnable{
        private Prenotazione prenotazione;

        public  Worker(Prenotazione prenotazione){
            this.prenotazione = prenotazione;
        }
        @Override
        public void run(){
            new GetPrenotazioneIdentificativo().execute(prenotazione.getProgressivo());
        }
    }

    class GetPrenotazioneIdentificativo extends AsyncTask<String, Void, Void>{

        @Override
        protected Void doInBackground(String... params) {
            String g = null;
            ClientResource clientResource = null;
            try {
                clientResource = new ClientResource("http://192.168.1.7/teletaxi/prenotazione/" + params[0]);
                clientResource.setProtocol(Protocol.HTTP);
                ConcurrentMap<String, Object> attrs = clientResource.getRequest().getAttributes();
                Series<Header> headers = (Series<Header>) attrs.get(HeaderConstants.ATTRIBUTE_HEADERS);
                if (headers == null) {
                    headers = new Series<Header>(Header.class);
                    Series<Header> prev = (Series<Header>)
                            attrs.putIfAbsent(HeaderConstants.ATTRIBUTE_HEADERS, headers);
                    if (prev != null) {
                        headers = prev;
                    }
                }
                headers.add("Authorization", "cHdk");
                g = clientResource.get().getText();
            } catch (final ResourceException e) {
                Log.e("error!", e.toString());
            } catch (Exception e) {
                Log.e("error!", e.toString());
            }finally{
                final int status = clientResource.getStatus().getCode();
                GsonBuilder gsonBuilder = new GsonBuilder().setDateFormat("dd/MM/yyyy HH:mm:ss");
                Gson gson = gsonBuilder.create();
                if (status == 504) {
                    final NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext());
                    builder.setContentTitle("Prenotazione cancellata!")
                            .setAutoCancel(true)
                            .setColor(getResources().getColor(R.color.colorAccent))
                            .setContentText("Clicca qui per avere informazioni")
                            .setVibrate(new long[]{1000, 1000, 1000, 1000, 1000})
                            .setLights(Color.RED, 3000, 3000)
                            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                            .setSmallIcon(R.drawable.ic_local_taxi_white_48dp)
                            .setPriority(Notification.PRIORITY_HIGH);

                    Intent i =  new Intent(getApplicationContext(), MapsActivity.class);
                    i.putExtra("prenotazioneDelete", true);

                    PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(),
                            NOTIFICATION_ID,
                            i,
                            PendingIntent.FLAG_UPDATE_CURRENT);
                    builder.setContentIntent(pendingIntent);

                    NotificationManager notificationManager =
                            (NotificationManager) getSystemService(Service.NOTIFICATION_SERVICE);
                    Notification notification = builder.build();
                    notification.flags =  Notification.FLAG_ONLY_ALERT_ONCE | Notification.FLAG_AUTO_CANCEL;
                    notificationManager.notify(NOTIFICATION_ID, notification);
                }
            }
            return null;
        }
    }

}
