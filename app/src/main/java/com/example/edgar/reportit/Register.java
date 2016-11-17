package com.example.edgar.reportit;

import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.AsyncTask;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
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
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class Register extends AppCompatActivity {

    EditText edtUsername, edtPassword, edtEmail;
    Button btnSign;
    SQLiteDatabase sqldb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        sqldb = openOrCreateDatabase("report_it",MODE_PRIVATE,null);

        edtUsername = (EditText)findViewById(R.id.edtUserSign);
        edtPassword = (EditText)findViewById(R.id.edtPassSign);
        edtEmail = (EditText)findViewById(R.id.edtEmailSign);
        btnSign = (Button)findViewById(R.id.btnSign);

        btnSign.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                HttpAsyncTask loginRequest = new HttpAsyncTask();
                if(validate()){
                    loginRequest.execute("https://salty-earth-57909.herokuapp.com/users");
                }
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
            jsonObject1.accumulate("username", edtUsername.getText().toString());
            jsonObject1.accumulate("email", edtEmail.getText().toString());
            jsonObject1.accumulate("password", edtPassword.getText().toString());
            jsonObject1.accumulate("role_id", "4");
            System.out.println("JSON: " + jsonObject1.toString());
            JSONObject jsonObject = new JSONObject();
            jsonObject.accumulate("user", jsonObject1);

            // 4. convert JSONObject to JSON to String
            json = jsonObject.toString();
            System.out.println("JSON: " + json);

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
            btnSign.setEnabled(false);
            dialog = new ProgressDialog(Register.this);
            dialog.setMessage("Creando cuenta...");
            dialog.show();
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
            btnSign.setEnabled(true);
            try {
                JSONObject jsonObj = new JSONObject(response);
                if(jsonObj.get("login").equals("OK")){
                    Intent intento = new Intent(Register.this, Principal.class);
                    sqldb.beginTransaction();
                    ContentValues args = new ContentValues();
                    args.put("id", jsonObj.getInt("user"));
                    args.put("username", edtUsername.getText().toString());
                    try{
                        long row = sqldb.insert("USER",null,args);
                        if(row != -1){
                            sqldb.setTransactionSuccessful();
                            finish();
                            startActivity(intento);
                        }else{
                            Toast.makeText(Register.this, "Problema al guardar en la base de datos local", Toast.LENGTH_SHORT).show();
                        }
                    }catch(SQLiteException e){
                        Toast.makeText(Register.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    }finally{
                        sqldb.endTransaction();
                    }
                }else{
                    AlertDialog.Builder builder;
                    builder = new AlertDialog.Builder(Register.this);
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
        if(edtUsername.getText().toString().trim().equals(""))
            return false;
        else if(edtPassword.getText().toString().trim().equals(""))
            return false;
        else
            return true;
    }

    private static String convertInputStreamToString(InputStream inputStream) throws IOException {
        BufferedReader bufferedReader = new BufferedReader( new InputStreamReader(inputStream));
        String line = "";
        String result = "";
        while((line = bufferedReader.readLine()) != null)
            result += line;
        inputStream.close();
        return result;
    }
}
