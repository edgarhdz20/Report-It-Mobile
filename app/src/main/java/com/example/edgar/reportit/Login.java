package com.example.edgar.reportit;

import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.BoolRes;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class Login extends AppCompatActivity {

    EditText edtUser, edtPassword;
    Button btnLogin, btnSignup;
    SQLiteDatabase sqldb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sqldb = openOrCreateDatabase("report_it",MODE_PRIVATE,null);

        try{
            sqldb.execSQL("create table USER (id integer, username text, email text)");
        }catch(SQLiteException e){
            e.printStackTrace();
        }

        try{
            sqldb.execSQL("create table REPORTS (id integer, user_id integer, report_type_id integer, description text, pos_x float, pos_y float, address text, url text, status integer)");
        }catch(SQLiteException e){
            e.printStackTrace();
        }

        Cursor c = sqldb.rawQuery("SELECT * FROM USER",null);
        if(c.moveToFirst()){
            Intent intento = new Intent(Login.this, Principal.class);
            startActivity(intento);
            finish();
        }

        setContentView(R.layout.activity_login);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        edtUser = (EditText)findViewById(R.id.edtUsername);
        edtPassword = (EditText)findViewById(R.id.edtPassword);
        btnLogin = (Button)findViewById(R.id.btnLogin);
        btnSignup = (Button)findViewById(R.id.btnSignup);

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                HttpAsyncTask loginRequest = new HttpAsyncTask();
                if(validate()){
                    loginRequest.execute("https://salty-earth-57909.herokuapp.com/users/sign_in");
                }
            }
        });

        btnSignup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intento = new Intent(Login.this, Register.class);
                startActivity(intento);
            }
        });
    }

    public String POST(String url){
        InputStream inputStream = null;
        String result = "";
        try {

            // 1. create HttpClient
            HttpClient httpclient = new DefaultHttpClient();

            // 2. make POST request to the given URL
            HttpPost httpPost = new HttpPost(url);

            String json = "";

            // 3. build jsonObject
            JSONObject jsonObject1 = new JSONObject();
            jsonObject1.accumulate("username", edtUser.getText().toString());
            jsonObject1.accumulate("password", edtPassword.getText().toString());
            System.out.println("JSON: " + jsonObject1.toString());
            JSONObject jsonObject = new JSONObject();
            jsonObject.accumulate("user", jsonObject1);

            // 4. convert JSONObject to JSON to String
            json = jsonObject.toString();

            // ** Alternative way to convert Person object to JSON string usin Jackson Lib
            // ObjectMapper mapper = new ObjectMapper();
            // json = mapper.writeValueAsString(person);

            // 5. set json to StringEntity
            StringEntity se = new StringEntity(json);

            // 6. set httpPost Entity
            httpPost.setEntity(se);

            // 7. Set some headers to inform server about the type of the content
            httpPost.setHeader("Accept", "application/json");
            httpPost.setHeader("Content-type", "application/json");

            // 8. Execute POST request to the given URL
            HttpResponse httpResponse = httpclient.execute(httpPost);

            // 9. receive response as inputStream
            inputStream = httpResponse.getEntity().getContent();

            // 10. convert inputstream to string
            if(inputStream != null)
                result = convertInputStreamToString(inputStream);
            else
                result = "Did not work!";

        } catch (Exception e) {
            Log.d("InputStream", e.getLocalizedMessage());
        }

        // 11. return result
        return result;
    }

    private class HttpAsyncTask extends AsyncTask<String, Void, String> {
        String response;
        ProgressDialog dialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            dialog = new ProgressDialog(Login.this);
            dialog.setMessage("Iniciando Sesion...");
            dialog.setCancelable(false);
            dialog.show();
            btnLogin.setEnabled(false);
            btnSignup.setEnabled(false);
        }

        @Override
        protected String doInBackground(String... urls) {
            response = POST(urls[0]);
            return response;
        }

        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(String result) {
            dialog.dismiss();
            btnLogin.setEnabled(true);
            btnSignup.setEnabled(true);
            try {
                JSONObject jsonObj = new JSONObject(response);
                if(jsonObj.get("login").equals("OK")){
                    Intent intento = new Intent(Login.this, Principal.class);
                    sqldb.beginTransaction();
                    ContentValues args = new ContentValues();
                    args.put("id", jsonObj.getInt("user"));
                    args.put("username", edtUser.getText().toString());
                    args.put("email", jsonObj.getString("email"));
                    try{
                        long row = sqldb.insert("USER",null,args);
                        if(row != -1){
                            sqldb.setTransactionSuccessful();
                            String URL = "https://salty-earth-57909.herokuapp.com/reports/get_all_reports_user?user_id=" + jsonObj.getInt("user");
                            new LoadJSONTask().execute(URL);
                            //finish();
                            //startActivity(intento);
                        }else{
                            Toast.makeText(Login.this, "Problema al guardar en la base de datos local", Toast.LENGTH_SHORT).show();
                        }
                    }catch(SQLiteException e){
                        Toast.makeText(Login.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    }finally{
                        sqldb.endTransaction();
                    }
                }else{
                    AlertDialog.Builder builder;
                    builder = new AlertDialog.Builder(Login.this);
                    builder.setCancelable(true);
                    builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });
                    builder.setMessage(jsonObj.get("login").toString());
                    builder.create();
                    builder.show();
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private boolean validate(){
        if(edtUser.getText().toString().trim().equals(""))
            return false;
        else if(edtPassword.getText().toString().trim().equals(""))
            return false;
        else
            return true;
    }

    private static String convertInputStreamToString(InputStream inputStream) throws IOException{
        BufferedReader bufferedReader = new BufferedReader( new InputStreamReader(inputStream));
        String line = "";
        String result = "";
        while((line = bufferedReader.readLine()) != null)
            result += line;
        inputStream.close();
        return result;
    }

    class LoadJSONTask extends AsyncTask<String, Void, Boolean> {

        ProgressDialog dialog;

        public LoadJSONTask() {
            dialog = new ProgressDialog(Login.this);
            dialog.setMessage("Cargando reportes enviados...");
            dialog.setCancelable(false);
            dialog.show();
        }

        @Override
        protected Boolean doInBackground(String... strings) {
            boolean b = true;
            try {
                String stringResponse = loadJSON(strings[0]);
                JSONArray json = new JSONArray(stringResponse);
                JSONObject jo;

                sqldb.beginTransaction();

                for(int i = 0;i<json.length();i++){
                    jo = (JSONObject) json.get(i);
                    ContentValues cv = new ContentValues();
                    cv.put("id",jo.getInt("id"));
                    cv.put("user_id",jo.getInt("user_id"));
                    cv.put("report_type_id",jo.getInt("report_type_id"));
                    cv.put("description",jo.getString("description"));
                    cv.put("pos_x",jo.getDouble("pos_x"));
                    cv.put("pos_y",jo.getDouble("pos_y"));
                    cv.put("address",jo.getString("address"));
                    cv.put("url",jo.getJSONObject("avatar").getString("url"));
                    //cv.put("status",jo.getBoolean("status"));
                    sqldb.insert("REPORTS",null,cv);
                }
                sqldb.setTransactionSuccessful();
            } catch (IOException e) {
                e.printStackTrace();
                b = false;
            } catch (JSONException e) {
                e.printStackTrace();
                b = false;
            }catch(SQLiteException e){
                Toast.makeText(Login.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                b = false;
            }finally{
                sqldb.endTransaction();
                return b;
            }
        }

        @Override
        protected void onPostExecute(Boolean b) {
            dialog.dismiss();
            if(b){
                Intent intento = new Intent(Login.this, Principal.class);
                finish();
                startActivity(intento);
            }
        }

        private String loadJSON(String jsonURL) throws IOException {

            java.net.URL url = new URL(jsonURL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(30000);
            conn.setConnectTimeout(35000);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            conn.connect();

            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            StringBuilder response = new StringBuilder();

            while ((line = in.readLine()) != null) {
                response.append(line);
            }

            in.close();
            return response.toString();
        }
    }
}