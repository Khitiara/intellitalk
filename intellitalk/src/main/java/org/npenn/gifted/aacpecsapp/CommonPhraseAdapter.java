package org.npenn.gifted.aacpecsapp;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

class CommonPhraseAdapter extends BaseAdapter {

    private final Activity context;

    public CommonPhraseAdapter(Activity context) {
        this.context = context;
    }

    @Override
    public int getCount() {
        return ContentLoader.INSTANCE.content.commonWords.size();
    }

    @Override
    public Object getItem(int i) {
        return ContentLoader.INSTANCE.content.commonWords.get(i);
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        if (view == null) {
            view = context.getLayoutInflater().inflate(R.layout.word_layout, null);
        }
        Word w = ContentLoader.INSTANCE.content.commonWords.get(i);
        if (w.image != null) {
            ((ImageView) view.findViewById(R.id.item_image)).setImageBitmap(w.image);
        }
        ((TextView) view.findViewById(R.id.item_text)).setText(w.displayText);
        return view;
    }


}
