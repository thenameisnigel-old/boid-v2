package com.teamboid.twitter.fragments.base;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.teamboid.twitter.R;
import com.teamboid.twitter.adapters.BoidAdapter;

/**
 * Provides a standardized base for all fragments that contain a list.
 *
 * @param <T> The class contained in the fragment's {@link com.teamboid.twitter.adapters.BoidAdapter}.
 * @author Aidan Follestad (afollestad)
 */
public abstract class BoidListFragment<T> extends CacheableListFragment<T> {

    public BoidListFragment(boolean cachingEnabled) {
        super(cachingEnabled);
    }

    public abstract int getEmptyText();

    public abstract BoidAdapter<T> getAdapter();

    public abstract void onItemClicked(int index);

    public abstract boolean onItemLongClicked(int index);


    public final void setListShown(boolean shown) {
        mListView.setVisibility(shown ? View.VISIBLE : View.GONE);
        if (!shown) {
            mEmptyView.setVisibility(View.GONE);
        } else {
            mListView.setEmptyView(mEmptyView);
            getAdapter().notifyDataSetChanged();
        }
        mProgressView.setVisibility(shown ? View.GONE : View.VISIBLE);
    }

    public final ListView getListView() {
        return mListView;
    }

    public final void runOnUiThread(Runnable runnable) {
        Activity act = getActivity();
        if (act == null)
            return;
        act.runOnUiThread(runnable);
    }


    private ListView mListView;
    private TextView mEmptyView;
    private ProgressBar mProgressView;

    public void ensureViews() {
        if (getView() == null)
            return;
        View v = getView();
        if (mListView == null)
            mListView = (ListView) v.findViewById(R.id.list);
        if (mEmptyView == null)
            mEmptyView = (TextView) v.findViewById(R.id.empty);
        if (mProgressView == null)
            mProgressView = (ProgressBar) v.findViewById(R.id.progress);
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        getActivity().setTitle(getTitle());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.boid_list_fragment, null);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ensureViews();
        mListView.setAdapter(getAdapter());
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                onItemClicked(i);
            }
        });
        mListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                return onItemLongClicked(i);
            }
        });
        mListView.setEmptyView(mEmptyView);
        mEmptyView.setText(getString(getEmptyText()));
    }


    @Override
    public final T[] getCacheWriteables() {
        return getAdapter().toArray();
    }

    @Override
    public final void onCacheRead(T[] contents) {
        getAdapter().set(contents);
    }
}
