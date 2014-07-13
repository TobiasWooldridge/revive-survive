package tw.tobias.reviveandsurvive;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import tw.tobias.reviveandsurvive.client.PitStop;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;

public class PitStopAdapter extends ArrayAdapter<PitStop> {
    private final ArrayList<PitStop> items;
    private final Context context;
    private final static String TAG = "PitStopAdapter";

    Set<String> filters;

    public PitStopAdapter(Context context, ArrayList<PitStop> items) {
        super(context, R.layout.stop_row, items);

        this.context = context;
        this.items = items;

        filters = Collections.emptySet();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View rowView = inflater.inflate(R.layout.stop_row, parent, false);

        TextView typeView = (TextView) rowView.findViewById(R.id.stop_type);
        typeView.setText(items.get(position).getType());

        TextView descriptionView = (TextView) rowView.findViewById(R.id.stop_description);
        descriptionView.setText(items.get(position).getName());

        TextView glyphView = (TextView) rowView.findViewById(R.id.glyph);
        glyphView.setText(items.get(position).getGlyph());

        TextView distView = (TextView) rowView.findViewById(R.id.distance_km);
        distView.setText(String.format("%.3f km", items.get(position).getDistance()/1000));

        return rowView;
    }
}