package com.example.edgar.reportit;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public class Principal extends AppCompatActivity {

    Button btnFoto, btnSend, btnMap;
    ImageView imgvFoto;
    EditText edtDescription;
    TextView txtvAddress;
    int CAMERA_INTENT = 0, MAP_INTENT = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_principal);
        btnFoto = (Button)findViewById(R.id.btnFoto);
        btnSend = (Button)findViewById(R.id.btnSend);
        btnMap = (Button)findViewById(R.id.btnAddress);
        imgvFoto = (ImageView)findViewById(R.id.imgvFoto);
        edtDescription = (EditText)findViewById(R.id.edtDescription);
        txtvAddress = (TextView)findViewById(R.id.txtvAddress);

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
                UploadTask upload = new UploadTask();
                upload.execute(((BitmapDrawable)imgvFoto.getDrawable()).getBitmap());
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

        }
    }

    private class UploadTask extends AsyncTask<Bitmap, Void, Void> {
        private String description;
        private int type;
        private float pos_x, pos_y;
        private String address;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            description = edtDescription.getText().toString();
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
                        "http://192.168.0.50:3000/reports/post_report"); // server

                MultiPartEntity reqEntity = new MultiPartEntity();
                reqEntity.addPart("user_id","2");
                reqEntity.addPart("report_type_id","2");
                reqEntity.addPart("description", description);
                reqEntity.addPart("pos_x","28");
                reqEntity.addPart("pos_y","-106");
                reqEntity.addPart("address","calle tal");
                reqEntity.addPart("avatar", System.currentTimeMillis() + ".jpg", in);
                httppost.setEntity(reqEntity);

                Log.i("TAG", "request " + httppost.getRequestLine());
                HttpResponse response = null;
                try {
                    response = httpclient.execute(httppost);
                } catch (ClientProtocolException e) {
                    // TODO Auto-generated catch block
                    Toast.makeText(Principal.this, "Error de protocolo", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    Toast.makeText(Principal.this, "Error de IO", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }
                try {
                    if (response != null)
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
            Toast.makeText(Principal.this, "Reporte enviado", Toast.LENGTH_LONG).show();
        }
    }
}

