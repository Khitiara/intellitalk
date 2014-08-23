package org.npenn.gifted.aacpecsapp;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.Intent;
import android.content.Loader;
import android.os.Bundle;


public class LoadingActivity extends Activity implements LoaderManager.LoaderCallbacks<ContentLoader.IntellitalkContent> {

    private Loader<ContentLoader.IntellitalkContent> loader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_loading);
        getLoaderManager().destroyLoader(0);
        loader = getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public void onBackPressed() {
        loader.cancelLoad();
    }

    private void startMain() {
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        startActivity(intent);
    }

    @Override
    public Loader<ContentLoader.IntellitalkContent> onCreateLoader(int id, Bundle args) {
        return new ContentLoader(this);
    }

    @Override
    public void onLoadFinished(Loader<ContentLoader.IntellitalkContent> loader, ContentLoader.IntellitalkContent data) {
        if (data != null) {
            IntellitalkState.INSTANCE.data = data;
            startMain();
        } else if (IntellitalkState.INSTANCE.data != null) {
            startMain();
        } else {
            finish();
            System.exit(0);
        }
    }

    @Override
    public void onLoaderReset(Loader<ContentLoader.IntellitalkContent> loader) {

    }
}
