package com.example.edgar.reportit;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class Principal extends AppCompatActivity {

    Button btnFoto, btnSend, btnMap;
    ImageView imgvFoto;
    EditText edtDescription;
    TextView txtvAddress, userName;
    RadioButton rdbBache;
    RadioButton rdbFuga;
    int CAMERA_INTENT = 0, MAP_INTENT = 1;
    double lat, lng;
    SQLiteDatabase sqldb;
    private ListView mDrawerList;
    RelativeLayout mDrawerPane;
    ArrayList<NavItem> mNavItems = new ArrayList<NavItem>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_principal);

        mNavItems.add(new NavItem("Reportes enviados", "Ver reportes enviados", R.drawable.reports_icon));
        //mNavItems.add(new NavItem("Nuevo reporte", "Crear un reporte", R.drawable.new_report_icon));
        mNavItems.add(new NavItem("Salir", "Cerrar sesion", R.drawable.logout_icon));

        mDrawerList = (ListView) findViewById(R.id.left_drawer);
        mDrawerPane = (RelativeLayout) findViewById(R.id.drawerPane);
        DrawerListAdapter adapter = new DrawerListAdapter(this, mNavItems);
        mDrawerList.setAdapter(adapter);

        mDrawerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                System.out.println(position);
                switch (position){
                    case 0:
                        Intent intento = new Intent(Principal.this, Report_List.class);
                        startActivity(intento);
                        break;
                    case 1:
                        log_out();
                        break;
                }
            }
        });

        sqldb = openOrCreateDatabase("report_it",MODE_PRIVATE,null);

        btnFoto = (Button)findViewById(R.id.btnFoto);
        btnSend = (Button)findViewById(R.id.btnSend);
        btnMap = (Button)findViewById(R.id.btnAddress);
        imgvFoto = (ImageView)findViewById(R.id.imgvFoto);
        edtDescription = (EditText)findViewById(R.id.edtDescription);
        txtvAddress = (TextView)findViewById(R.id.txtvAddress);
        userName = (TextView)findViewById(R.id.userName);
        rdbFuga = (RadioButton)findViewById(R.id.rdbFugaAgua);
        rdbBache = (RadioButton)findViewById(R.id.rdbBache);

        Cursor c = sqldb.rawQuery("SELECT username FROM USER",null);
        c.moveToFirst();
        userName.setText(c.getString(0));

        btnFoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intento = new Intent("android.media.action.IMAGE_CAPTURE");
                startActivityForResult(intento,CAMERA_INTENT);
            }
        });

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(Principal.this);
                builder.setCancelable(true);
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

                UploadTask upload = new UploadTask();
                if(imgvFoto.getDrawable() == null){
                    builder.setMessage("Se requiere una foto");
                    builder.create();
                    builder.show();
                }else if(edtDescription.getText().toString().trim().equals("")) {
                    builder.setMessage("Se requiere una descripcion");
                    builder.create();
                    builder.show();
                }else if(txtvAddress.getText().toString().trim().equals("") || txtvAddress.getText().toString().equals("Sin ubicacion seleccionada")){
                    builder.setMessage("Se requiere una ubicacion");
                    builder.create();
                    builder.show();
                }else{
                    upload.execute(((BitmapDrawable)imgvFoto.getDrawable()).getBitmap());
                }
            }
        });

        btnMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent mapActivity = new Intent(Principal.this, LocationSelect.class);
                startActivityForResult(mapActivity,MAP_INTENT);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == CAMERA_INTENT && resultCode == RESULT_OK){
            Bundle extras = data.getExtras();
            Bitmap imageBmp = (Bitmap) extras.get("data");
            imgvFoto.setImageBitmap(imageBmp);
        }
        if(requestCode == MAP_INTENT && resultCode == RESULT_OK){
            Bundle extras = data.getExtras();
            lat = extras.getDouble("LAT");
            lng = extras.getDouble("LNG");
            txtvAddress.setText(extras.getString("ADD"));
        }
    }

    private class UploadTask extends AsyncTask<Bitmap, Void, Void> {
        private String user_id;
        private String description;
        private String type;
        private String pos_x, pos_y;
        private String address;
        AlertDialog.Builder builder;
        ProgressDialog dialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            dialog = new ProgressDialog(Principal.this);
            dialog.setMessage("Enviando reporte...");
            dialog.setCancelable(false);
            dialog.show();
            btnSend.setEnabled(false);
            btnSend.setText("Enviando...");
            description = edtDescription.getText().toString();
            pos_x = String.valueOf(lat);
            pos_y = String.valueOf(lng);
            address = txtvAddress.getText().toString();
            builder = new AlertDialog.Builder(Principal.this);
            builder.setCancelable(true);
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });
            if(rdbFuga.isChecked()){
                type = "1";
            }else if(rdbBache.isChecked()){
                type = "2";
            }else{
                type= "3";
            }
            Cursor c = sqldb.rawQuery("SELECT id FROM USER", null);
            c.moveToFirst();
            user_id = c.getString(0);
        }

        protected Void doInBackground(Bitmap... bitmaps) {
            if (bitmaps[0] == null)
                return null;

            Bitmap bitmap = bitmaps[0];
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream); // convert Bitmap to ByteArrayOutputStream
            InputStream in = new ByteArrayInputStream(stream.toByteArray()); // convert ByteArrayOutputStream to ByteArrayInputStream

            DefaultHttpClient httpclient = new DefaultHttpClient();
            try {
                HttpPost httppost = new HttpPost(
                        "https://salty-earth-57909.herokuapp.com/reports/post_report"); // server

                MultiPartEntity reqEntity = new MultiPartEntity();
                reqEntity.addPart("user_id",user_id);
                reqEntity.addPart("report_type_id",type);
                reqEntity.addPart("description", description);
                reqEntity.addPart("pos_x",pos_x);
                reqEntity.addPart("pos_y",pos_y);
                reqEntity.addPart("address",address);
                reqEntity.addPart("avatar", System.currentTimeMillis() + ".jpg", in);
                httppost.setEntity(reqEntity);

                Log.i("TAG", "request " + httppost.getRequestLine());
                HttpResponse response = null;
                try {
                    response = httpclient.execute(httppost);
                } catch (ClientProtocolException e) {
                    // TODO Auto-generated catch block
                    builder.setMessage("Ocurrio un error: " + e.getMessage());
                    e.printStackTrace();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    builder.setMessage("Ocurrio un error: " + e.getMessage());
                    e.printStackTrace();
                }
                try {
                    if (response != null)
                        try {
                            InputStream instream = response.getEntity().getContent();
                            String result = convertStreamToString(instream);
                            JSONObject myObject = new JSONObject(result);
                            builder.setMessage(myObject.getString("message"));
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    Log.i("TAG", "response " + response.getStatusLine().toString());
                } finally {

                }
            } finally {

            }

            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }

            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            // TODO Auto-generated method stub
            super.onPostExecute(result);
            dialog.dismiss();
            builder.create();
            builder.show();
            btnSend.setText("ENVIAR");
            btnSend.setEnabled(true);
        }
    }

    private static String convertStreamToString(InputStream is) {

        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();

        String line = null;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //return super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //return super.onOptionsItemSelected(item);
        if(item.getItemId() == R.id.act_logout){
            log_out();
        }
        return true;
    }

    public void log_out(){
        System.out.println("delete all principal");
        sqldb.delete("USER",null,null);
        sqldb.delete("REPORTS",null,null);
        Intent intento = new Intent(Principal.this, Login.class);
        startActivity(intento);
        finish();
    }
}