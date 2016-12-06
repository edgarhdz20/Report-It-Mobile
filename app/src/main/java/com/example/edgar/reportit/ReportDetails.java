package com.example.edgar.reportit;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.InputStream;

public class ReportDetails extends AppCompatActivity implements OnMapReadyCallback, GoogleApiClient.OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks {

    TextView txtvType, txtvAddress, txtvDescription;
    ImageView img;
    GoogleMap map;
    private double lat, lng;
    SQLiteDatabase sqldb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_details);

        sqldb = openOrCreateDatabase("report_it",MODE_PRIVATE,null);

        txtvType = (TextView)findViewById(R.id.txtvType);
        txtvAddress = (TextView)findViewById(R.id.txtvAddress);
        txtvDescription = (TextView)findViewById(R.id.txtvDescription);
        img = (ImageView)findViewById(R.id.img);

        Intent intento = getIntent();
        Bundle bundle = intento.getExtras();

        Cursor c = sqldb.rawQuery("SELECT * FROM REPORTS WHERE id = " + bundle.getInt("ID"), null);
        c.moveToFirst();

        String[] tipos = {"Fuga","Bache","Cableado"};
        txtvType.setText(tipos[c.getInt(c.getColumnIndex("report_type_id")) -  1]);
        txtvDescription.setText(c.getString(c.getColumnIndex("description")));
        txtvAddress.setText(c.getString(c.getColumnIndex("address")));

        String url = "https://salty-earth-57909.herokuapp.com/reports/get_report_photo?id=" + c.getInt(c.getColumnIndex("id"));
        new DownloadImageTask(img).execute(url);

        lat = c.getDouble(c.getColumnIndex("pos_x"));
        lng = c.getDouble(c.getColumnIndex("pos_y"));

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        GoogleMapOptions options = new GoogleMapOptions().liteMode(true);
        System.out.println(options.getLiteMode());
        mapFragment.newInstance(options);
        mapFragment.getMapAsync(this);
    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        LatLng currentLocation = new LatLng(lat, lng);
        Marker marcador = googleMap.addMarker(new MarkerOptions().position(currentLocation).title("Posicion generica").draggable(true));
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 17));
    }

    private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        ImageView bmImage;

        public DownloadImageTask(ImageView bmImage) {
            this.bmImage = bmImage;
        }

        protected Bitmap doInBackground(String... urls) {
            String urldisplay = urls[0];
            Bitmap mIcon11 = null;
            try {
                InputStream in = new java.net.URL(urldisplay).openStream();
                mIcon11 = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                Log.e("Error", e.getMessage());
                e.printStackTrace();
            }
            return mIcon11;
        }

        protected void onPostExecute(Bitmap result) {
            bmImage.setImageBitmap(result);
        }
    }
}
