package com.afollestad.twitter.adapters;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.AbsListView;
import android.widget.TextView;
import com.afollestad.silk.adapters.SilkAdapter;
import com.afollestad.silk.images.SilkImageManager;
import com.afollestad.silk.views.image.SilkImageView;
import com.afollestad.twitter.BoidApp;
import com.afollestad.twitter.R;
import com.afollestad.twitter.utilities.TweetUtils;
import com.afollestad.twitter.utilities.text.TextUtils;
import twitter4j.User;

/**
 * A list adapter that displays users.
 *
 * @author Aidan Follestad (afollestad)
 */
public class UserAdapter extends SilkAdapter<User> {

    public UserAdapter(Context context) {
        super(context);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        mDisplayRealNames = prefs.getBoolean("display_realname", true);
        mImageLoader = BoidApp.get(context).getImageLoader();
    }

    private final boolean mDisplayRealNames;
    private final SilkImageManager mImageLoader;

    @Override
    public int getLayout(int index, int type) {
        return R.layout.list_item_user;
    }

    @Override
    public View onViewCreated(int index, View recycled, User item) {
        SilkImageView profilePic = (SilkImageView) recycled.findViewById(R.id.profilePic);
        if (getScrollState() == AbsListView.OnScrollListener.SCROLL_STATE_FLING) {
            profilePic.setImageResource(R.drawable.ic_contact_picture);
        } else {
            profilePic.setImageURL(mImageLoader, item.getBiggerProfileImageURL());
        }
        ((TextView) recycled.findViewById(R.id.username)).setText(TweetUtils.getDisplayName(item, mDisplayRealNames));
        TextUtils.linkifyText(getContext(), (TextView) recycled.findViewById(R.id.description), item, false, false);
        return recycled;
    }

    @Override
    public long getItemId(int i) {
        return getItem(i).getId();
    }
}