package com.example.edgar.reportit;

import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.AsyncTask;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SimpleAdapter;
import android.widget.TextView;
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
import java.lang.reflect.Array;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Report_List extends AppCompatActivity {

    private ListView mListView;
    private ListView mDrawerList;
    TextView userName;
    RelativeLayout mDrawerPane;
    ArrayList<NavItem> mNavItems = new ArrayList<NavItem>();
    SQLiteDatabase sqldb;

    private List<HashMap<String, String>> mAndroidMapList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_list);

        userName = (TextView)findViewById(R.id.userName);

        sqldb = openOrCreateDatabase("report_it",MODE_PRIVATE,null);
        Cursor c = sqldb.rawQuery("SELECT id, username FROM USER",null);
        c.moveToFirst();
        //URL += c.getInt(0);
        userName.setText(c.getString(1));
        //System.out.println("URL: " + URL);

        //mNavItems.add(new NavItem("Reportes enviados", "Ver reportes enviados", R.drawable.reports_icon));
        mNavItems.add(new NavItem("Nuevo reporte", "Crear un reporte", R.drawable.new_report_icon));
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
                        finish();
                        break;
                    case 1:
                        log_out();
                        break;
                }
            }
        });

        mListView = (ListView) findViewById(R.id.list_view);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intento = new Intent(Report_List.this, ReportDetails.class);
                intento.putExtra("ID", (int)view.getTag());
                startActivity(intento);
            }
        });

        Cursor cursor = sqldb.rawQuery("SELECT * FROM REPORTS ORDER BY CAST(id AS INTEGER) DESC",null);
        new LoadDBTask().execute(cursor);
    }

    class LoadDBTask extends AsyncTask <Cursor, ArrayList, ArrayList>{
        @Override
        protected ArrayList doInBackground(Cursor... cursor) {
            ArrayList response = new ArrayList();
            try {
                while(cursor[0].moveToNext()){
                    JSONObject jo = new JSONObject();
                    jo.put("id",cursor[0].getInt(cursor[0].getColumnIndex("id")));
                    jo.put("report_type_id",cursor[0].getInt(cursor[0].getColumnIndex("report_type_id")));
                    jo.put("description",cursor[0].getString(cursor[0].getColumnIndex("description")));
                    jo.put("address",cursor[0].getString(cursor[0].getColumnIndex("address")));
                    response.add(jo);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return(response);
        }

        @Override
        protected void onPostExecute(ArrayList response) {
            loadListView(response);
        }
    }

    private void loadListView(ArrayList array) {
        MyAdapter adapter = new MyAdapter(Report_List.this, array);
        mListView.setAdapter(adapter);
    }

    public void log_out(){
        sqldb.delete("USER",null,null);
        sqldb.delete("REPORTS",null,null);
        Intent intento = new Intent(Report_List.this, Login.class);
        startActivity(intento);
        finish();
    }
}
