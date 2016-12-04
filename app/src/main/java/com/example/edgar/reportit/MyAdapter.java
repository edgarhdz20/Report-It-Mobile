package com.example.edgar.reportit;

import android.content.Context;
import android.media.Image;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by edgar on 2/12/16.
 */

public class MyAdapter extends ArrayAdapter<JSONObject> {

    private final Context context;
    private final ArrayList<JSONObject> arreglo;

    public MyAdapter(Context context, ArrayList<JSONObject> arreglo) {
        super(context, R.layout.report_list_item, arreglo);
        this.context = context;
        this.arreglo = arreglo;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.report_list_item,parent,false);
        TextView txtvTitle = (TextView)rowView.findViewById(R.id.title);
        TextView txtvSubtitle = (TextView)rowView.findViewById(R.id.subTitle);
        ImageView icon = (ImageView)rowView.findViewById(R.id.icon);

        icon.setBackgroundResource(R.drawable.report_icon_bg);
        icon.setPadding(15,15,15,15);

        try {
            switch (arreglo.get(position).getInt("report_type_id")){
                case 1:
                    icon.setImageResource( R.drawable.fuga_icon);
                    break;
                case 2:
                    icon.setImageResource( R.drawable.bache_icon);
                    break;
                case 3:
                    icon.setImageResource( R.drawable.cableado_icon);
                    break;
            }
            txtvTitle.setText(arreglo.get(position).getInt("id") + " - " + arreglo.get(position).getString("description"));
            txtvSubtitle.setText(arreglo.get(position).getString("address"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return rowView;
    }
}
