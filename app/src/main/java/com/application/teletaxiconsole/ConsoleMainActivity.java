package com.application.teletaxiconsole;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.AnimationDrawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.StrictMode;
import android.provider.Settings;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.restlet.data.Protocol;
import org.restlet.engine.header.Header;
import org.restlet.engine.header.HeaderConstants;
import org.restlet.resource.ClientResource;
import org.restlet.resource.ResourceException;
import org.restlet.util.Series;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentMap;

import model.Prenotazione;
import model.Taxi;


public class ConsoleMainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static final String taxiPref = "TaxiPreferences";
    private static final String idTaxi = "TaxiID";
    private SharedPreferences sharedPreferences;
    private Gson gson;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_console_main);
         sharedPreferences = getSharedPreferences(taxiPref, Context.MODE_PRIVATE);

        gson = new GsonBuilder()
                .setDateFormat("dd/MM/yyyy HH:mm:ss")
                .create();
        final int idTaxiReceived = sharedPreferences.getInt(idTaxi, -1);
       
        if(idTaxiReceived == -1){
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            final LayoutInflater inflater = this.getLayoutInflater();

            builder.setView(inflater.inflate(R.layout.dialog_id_taxi, null))
                    .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            EditText et = (EditText) ((AlertDialog) dialog).findViewById(R.id.idTaxi);
                            editor.putInt(idTaxi, Integer.parseInt(String.valueOf(et.getText())));
                            editor.commit();
                         }
                    })
                    .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {}
                    });
            builder.create().show();
        }else{
            startService(new Intent(this, RequestPrenotazioneService.class));
        }

        Toolbar appbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(appbar);

        ImageView i = (ImageView)findViewById(R.id.background);
        i.setBackgroundResource(R.drawable.background_gif);

        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);

        if(isActiveGPS() && isActiveConnection()) {
            final LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                Location location = manager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                Geocoder gcd = new Geocoder(getApplicationContext(), Locale.getDefault());
                List<Address> addresses = null;
                try {
                    addresses = gcd.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                } catch (Exception e) {
                    getSupportActionBar().setTitle("Posizione attuale: " + "non rilevata");
                }
                if (addresses != null && addresses.size() > 0 )
                    getSupportActionBar().setTitle("Posizione attuale: " + addresses.get(0).getLocality());
                else
                    getSupportActionBar().setTitle("Posizione attuale: " + "non rilevata");
            }else getSupportActionBar().setTitle("Posizione attuale: " + "non rilevata");
          }else getSupportActionBar().setTitle("Posizione attuale: " + "non rilevata");

        AnimationDrawable pro = (AnimationDrawable) i.getBackground();
        pro.start();

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new GetStatoTaxi().execute(view);
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.console_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) { return true; }
        return super.onOptionsItemSelected(item);
    }

    public boolean isActiveGPS() {
        final LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        final boolean[] actived = new boolean[1];

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            actived[0] = false;
        }
        boolean isGPS = manager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if(isGPS){
            Toast.makeText(getApplicationContext(), "Il GPS e' attivo", Toast.LENGTH_LONG).show();
            actived[0] = true;
        }else{
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this)
                    .setMessage("Il servizio GPS non e' attivo, vuoi attivarlo?")
                    .setCancelable(false)
                    .setPositiveButton("Abilita", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            startActivityForResult(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS),0);
                            actived[0] = true;
                        }
                    })
                    .setNegativeButton("Cancella", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                            actived[0] = false;
                        }
                    });
            AlertDialog alert = alertDialogBuilder.create();
            alert.show();
        }
        return actived[0];
    }

    private boolean isActiveConnection(){
        final boolean[] actived = new boolean[1];
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        if(activeNetworkInfo != null && activeNetworkInfo.isConnected()){
            return true;
        }else{
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this)
                    .setMessage("Il servizio Internet non e' attivo, vuoi attivarlo?")
                    .setCancelable(false)
                    .setPositiveButton("Abilita", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            startActivityForResult(new Intent(Settings.ACTION_WIFI_SETTINGS),0);
                            actived[0] = true;
                        }
                    })
                    .setNegativeButton("Cancella", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                            actived[0] = false;
                        }
                    });
            AlertDialog alert = alertDialogBuilder.create();
            alert.show();
        }
        return false;
    }


    @Override
    protected void onStop() {
        super.onStop();
        stopService(new Intent(getApplicationContext(), RequestPrenotazioneService.class));
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(final MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.nav_manage) {
            final CharSequence[] items = {"Libero", "Occupato"};
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.state)
                    .setItems(items, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            new PostStatoTaxi().execute(String.valueOf(items[which]));
                        }
                    });
            builder.create().show();
        }else if(id == R.id.nav_share){
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            Prenotazione prenotazioneRcv =
                    gson.fromJson(sharedPreferences.getString("idLastPrenotazione", null), Prenotazione.class);
            if(prenotazioneRcv != null) {
                builder.setMessage("Il numero di cellulare del cliente "+ prenotazioneRcv.getCliente().getNome()
                        +" "+prenotazioneRcv.getCliente().getCognome()+": "+prenotazioneRcv.getCliente().getTelefono())
                        .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                            }
                        })
                        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                            }
                        });
                // Create the AlertDialog object and return it
                builder.create().show();
            }
        }
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }



     class GetStatoTaxi extends AsyncTask<View, Void, Void> {
        String g;

        @Override
        protected Void doInBackground(final View... params) {
            ClientResource clientResource = null;
            final Taxi taxiReceived;
            try {
                int idTaxiReceived = sharedPreferences.getInt(idTaxi, -1);
                clientResource = new ClientResource("http://192.168.1.7/teletaxi/taxi/"+idTaxiReceived);
                clientResource.setProtocol(Protocol.HTTP);
                ConcurrentMap<String, Object> attrs = clientResource.getRequest().getAttributes();
                Series<Header> headers = (Series<Header>) attrs.get(HeaderConstants.ATTRIBUTE_HEADERS);
                if (headers == null) {
                    headers = new Series<Header>(Header.class);
                    Series<Header> prev =(Series<Header>)
                            attrs.putIfAbsent(HeaderConstants.ATTRIBUTE_HEADERS, headers);
                    if (prev != null) { headers = prev; }
                }
                headers.add("Authorization", "cHdk");
                g = clientResource.get().getText();
            } catch (final ResourceException e) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        Log.e("error", e.toString());
                        Toast.makeText(getApplicationContext(), "Connessione al server assente!"+e, Toast.LENGTH_LONG).show();
                    }
                });
            } catch (Exception e) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        Toast.makeText(getApplicationContext(), "Impossibile accedere al database!", Toast.LENGTH_LONG).show();
                    }
                });
            }
            final int status = clientResource.getStatus().getCode();
            Gson gson = new Gson();
            if (status == 200) {
                taxiReceived = gson.fromJson(g, Taxi.class);
            } else {
                taxiReceived = null;
            }
            runOnUiThread(new Runnable() {
                public void run() {
                    StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
                    StrictMode.setThreadPolicy(policy);
                    if(status == 200) {
                        Snackbar.make(params[0], "Lo stato del taxi e': "+taxiReceived.getStato(), Snackbar.LENGTH_LONG)
                                .setAction("Action", null).show();
                    }else if(status == 506){
                        Snackbar.make(params[0], "Non e' stato ancora registrato nessun taxi con questo ID. Contattare l'assistenza!"
                                , Snackbar.LENGTH_LONG).setAction("Action", null).show();
                    }
                }
            });
            return null;
        }
    }


    class PostStatoTaxi extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... params) {
            GsonBuilder gsonBuilder = new GsonBuilder().setDateFormat("dd/MM/yyyy HH:mm:ss");
            Gson gson = gsonBuilder.create();
            String g = null;
            ClientResource clientResource = null;
            try {
                SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences(taxiPref, Context.MODE_PRIVATE);
                int idTaxiRcv = sharedPreferences.getInt(idTaxi, -1);
                if (idTaxiRcv != -1) {
                    clientResource = new ClientResource("http://192.168.1.7/teletaxi/taxi/" + idTaxiRcv);
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
                    headers.add("Content-Type", "application/json; charset=UTF-8");
                    headers.add("Authorization", "cHdk");
                    g = clientResource.get().getText();
                    int status = clientResource.getStatus().getCode();
                    if (status == 200) {
                        Taxi taxiRcv = gson.fromJson(g, Taxi.class);
                        taxiRcv.impostaStato(params[0]);
                        final LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                                ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {}
                        Location location = manager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                        Geocoder gcd = new Geocoder(getApplicationContext(), Locale.getDefault());
                        List<Address> addresses = null;
                        try {
                            addresses = gcd.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                        } catch (Exception e) {
                        }

                        if (addresses != null && addresses.size() > 0) {
                            taxiRcv.setPosizioneCorrente(addresses.get(0).getThoroughfare() + ", " + addresses.get(0).getLocality());
                            Log.e("posizione",addresses.get(0).getThoroughfare() + ", " + addresses.get(0).getLocality() );
                        }
                        clientResource = new ClientResource("http://192.168.1.7/teletaxi/taxi");
                        clientResource.setProtocol(Protocol.HTTP);
                        attrs = clientResource.getRequest().getAttributes();
                        headers = (Series<Header>) attrs.get(HeaderConstants.ATTRIBUTE_HEADERS);
                        if (headers == null) {
                            headers = new Series<Header>(Header.class);
                            Series<Header> prev = (Series<Header>)
                                    attrs.putIfAbsent(HeaderConstants.ATTRIBUTE_HEADERS, headers);
                            if (prev != null) {
                                headers = prev;
                            }
                        }
                        headers.add("Content-Type", "application/json; charset=UTF-8");
                        headers.add("Authorization", "cHdk");
                        g = clientResource.post(gson.toJson(taxiRcv, Taxi.class)).getText();
                        status = clientResource.getStatus().getCode();
                        if (status == 200) {
                            Toast.makeText(getApplicationContext(), "Stato del Taxi: " + taxiRcv.getStato(), Toast.LENGTH_LONG).show();
                        }
                    }
                }
            } catch (final ResourceException e) {
                Log.e("error!", e.toString());
            } catch (Exception e) {
                Log.e("error!", e.toString());
            }
            return null;
        }
    }

}
