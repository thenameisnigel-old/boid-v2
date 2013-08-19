package com.teamboid.twitter.adapters;

import android.content.Context;
import android.view.View;
import android.widget.TextView;
import com.afollestad.silk.adapters.SilkAdapter;
import com.teamboid.twitter.R;
import twitter4j.Trend;

/**
 * A list adapter that displays trends.
 *
 * @author Aidan Follestad (afollestad)
 */
public class TrendAdapter extends SilkAdapter<Trend> {

    public TrendAdapter(Context context) {
        super(context);
    }

    @Override
    public int getLayout(int index, int type) {
        return R.layout.list_item_trend;
    }

    @Override
    public View onViewCreated(int index, View view, Trend item) {
        ((TextView) view).setText(item.getName());
        return view;
    }
}
