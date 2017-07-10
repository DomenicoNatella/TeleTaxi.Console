package com.application.teletaxiconsole;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.restlet.data.Protocol;
import org.restlet.engine.header.Header;
import org.restlet.engine.header.HeaderConstants;
import org.restlet.resource.ClientResource;
import org.restlet.resource.ResourceException;
import org.restlet.util.Series;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentMap;

import model.Prenotazione;
import model.Taxi;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private Location location;
    private Prenotazione prenotazioneRcv;
    private static final String taxiPref = "TaxiPreferences";
    private static final String idTaxi = "TaxiID";
    private Intent intent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        intent = getIntent();
        if(intent != null && intent.getBooleanExtra("prenotazioneDelete", false)){
            stopService(new Intent(getApplicationContext(), VerifyPrenotazioneService.class));
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage("La prenotazione e' stata cancellata ")
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
       else if(intent != null){
            Gson gson = new GsonBuilder()
                    .setDateFormat("dd/MM/yyyy HH:mm:ss")
                    .create();
            prenotazioneRcv = gson.fromJson(intent.getStringExtra("fromServicePrenotazione"), Prenotazione.class);
            SharedPreferences.Editor editor = getApplicationContext().getSharedPreferences(taxiPref, Context.MODE_PRIVATE).edit();
            editor.putString("idLastPrenotazione", intent.getStringExtra("fromServicePrenotazione"));
            editor.commit();
            location = getLatLongFromAddress(prenotazioneRcv.getPosizioneCliente());
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if(intent != null && !intent.getBooleanExtra("prenotazioneDelete", false) && location != null) {
            LatLng locationLatLng = new LatLng(location.getLatitude(), location.getLongitude());
            mMap.addMarker(new MarkerOptions().position(locationLatLng).title(prenotazioneRcv.toString()));
            float zoomLevel = (float) 16.0; //This goes up to 21
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(locationLatLng, zoomLevel));
            mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
                @Override
                public boolean onMarkerClick(Marker marker) {
                    new PostPrenotazioneTaxi().execute();
                    new PostStatoTaxi().execute();
                    startService(new Intent(getApplicationContext(), VerifyPrenotazioneService.class));
                    Intent i = new Intent(getApplicationContext(), RequestPrenotazioneService.class);
                    i.putExtra("wakeUpTime", 600000);
                    startService(i);
                    startActivity(new Intent(Intent.ACTION_VIEW,
                            Uri.parse("https://www.google.com/maps/dir/?api=1" + "&destination=" + prenotazioneRcv.getDestinazione() + "&travelmode=car")));
                    return true;
                }
            });
        }

    }

    public Location getLatLongFromAddress(String strAddress){
        Geocoder coder = new Geocoder(this);
        List<Address> address;
        try {
            address = coder.getFromLocationName(strAddress, 5);
            if (address == null) {
                return null;
            }else {
                Address location = address.get(0);
                Location loc_tmp = new Location(LocationManager.GPS_PROVIDER);
                loc_tmp.setLatitude(location.getLatitude());
                loc_tmp.setLongitude(location.getLongitude());
                return loc_tmp;
            }
        } catch (IOException e) { return null;}
        catch(IndexOutOfBoundsException e){return null;}

    }


    class PostPrenotazioneTaxi extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            GsonBuilder gsonBuilder = new GsonBuilder().setDateFormat("dd/MM/yyyy HH:mm:ss");
            Gson gson = gsonBuilder.create();
            String g = null;
            ClientResource clientResource = null;
            prenotazioneRcv.setAssegnata(true);
            try {
                clientResource = new ClientResource("http://192.168.1.7/teletaxi/prenotazione");
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
                g = clientResource.post(gson.toJson(prenotazioneRcv, Prenotazione.class)).getText();
                final int status = clientResource.getStatus().getCode();
                if (status == 200) {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            Toast.makeText(getApplicationContext(), "Prenotazione accettata!", Toast.LENGTH_LONG).show();
                        }
                    });
                }
            } catch (final ResourceException e) {
                Log.e("error!", e.toString());
            } catch (Exception e) {
                Log.e("error!", e.toString());
            }
            return null;
        }
    }

   class PostStatoTaxi extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
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
                        if (prev != null) headers = prev;
                    }
                    headers.add("Content-Type", "application/json; charset=UTF-8");
                    headers.add("Authorization", "cHdk");
                    g = clientResource.get().getText();
                    int status = clientResource.getStatus().getCode();
                    if (status == 200) {
                        Taxi taxiRcv = gson.fromJson(g, Taxi.class);
                        taxiRcv.setDestinazione(prenotazioneRcv.getDestinazione());
                        taxiRcv.impostaStato("occupato");
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


