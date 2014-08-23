package org.npenn.gifted.aacpecsapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Created by robotbrain on 8/15/14.
 */
public class QueueAdapter extends BaseAdapter {
    final MainActivity activity;

    public QueueAdapter(MainActivity activity) {
        this.activity = activity;
    }

    @Override
    public int getCount() {
        return activity.getQueue().size();
    }

    @Override
    public Object getItem(int position) {
        return activity.getQueue().get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    public View getView(int position, View view, ViewGroup viewGroup) {
        if (view == null) {
            view = LayoutInflater.from(activity).inflate(R.layout.word_layout, viewGroup, false);
        }
        Word w = activity.getQueue().get(position);
        ImageView imageView = (ImageView) view.findViewById(R.id.item_image);
        TextView textView = (TextView) view.findViewById(R.id.item_text);
        if (w.image != null) {
            imageView.setImageBitmap(w.image);
            imageView.setVisibility(View.VISIBLE);
            textView.setVisibility(View.INVISIBLE);
        } else {
            textView.setText(w.displayText);
            textView.setVisibility(View.VISIBLE);
            imageView.setVisibility(View.INVISIBLE);
        }
        return view;
    }
}
