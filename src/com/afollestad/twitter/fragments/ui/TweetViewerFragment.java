package com.afollestad.twitter.fragments.ui;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.text.Html;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import com.afollestad.silk.fragments.SilkFragment;
import com.afollestad.silk.views.image.SilkImageView;
import com.afollestad.twitter.BoidApp;
import com.afollestad.twitter.R;
import com.afollestad.twitter.ui.ComposeActivity;
import com.afollestad.twitter.utilities.text.TextUtils;
import com.devspark.appmsg.AppMsg;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.User;

/**
 * The tweet viewer fragment.
 *
 * @author Aidan Follestad (afollestad)
 */
public class TweetViewerFragment extends SilkFragment {

    private Status mTweet;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        processArgs();
    }

    private void processArgs() {
        mTweet = (Status) getArguments().getSerializable("tweet");
        if (mTweet.isRetweet())
            mTweet = mTweet.getRetweetedStatus();
        View v = getView();
        if (v == null) return;
        SilkImageView profilePic = (SilkImageView) v.findViewById(R.id.profilePic);
        profilePic.setFitView(false).setImageURL(BoidApp.get(getActivity()).getImageLoader(), mTweet.getUser().getProfileImageURL());
        ((TextView) v.findViewById(R.id.fullname)).setText(mTweet.getUser().getName());
        ((TextView) v.findViewById(R.id.source)).setText("via " + Html.fromHtml(mTweet.getSource()).toString());
        TextView content = (TextView) v.findViewById(R.id.content);
        TextUtils.linkifyText(getActivity(), content, mTweet, true, true);

        SilkImageView media = (SilkImageView) v.findViewById(R.id.media);
        if (mTweet.getMediaEntities() != null && mTweet.getMediaEntities().length > 0) {
            media.setVisibility(View.VISIBLE);
            media.setImageURL(BoidApp.get(getActivity()).getImageLoader(), mTweet.getMediaEntities()[0].getMediaURL());
        } else media.setVisibility(View.GONE);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.fragment_tweet_viewer, menu);
        User me = BoidApp.get(getActivity()).getProfile();
        menu.findItem(R.id.delete).setVisible(me.getId() == mTweet.getUser().getId());
        MenuItem favorite = menu.findItem(R.id.favorite);
        int favIcon;
        if (mTweet.isFavorited()) {
            favorite.setTitle(R.string.unfavorite);
            favIcon = R.attr.favoritedIcon;
        } else {
            favorite.setTitle(R.string.favorite);
            favIcon = R.attr.unfavoritedIcon;
        }
        TypedArray ta = getActivity().obtainStyledAttributes(new int[]{favIcon});
        favIcon = ta.getResourceId(0, 0);
        ta.recycle();
        favorite.setIcon(favIcon);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.reply:
                startActivity(new Intent(getActivity(), ComposeActivity.class).putExtra("reply_to", mTweet));
                return true;
            case R.id.retweet:
                showRetweetDialog();
                return true;
            case R.id.favorite:
                toggleFavorite();
                return true;
            case R.id.share:
                performShare();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void toggleFavorite() {
        final ProgressDialog dialog = new ProgressDialog(getActivity());
        dialog.setMessage(getString(R.string.please_wait));
        dialog.setCancelable(false);
        dialog.show();

        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Twitter cl = BoidApp.get(getActivity()).getClient();
                    if (mTweet.isFavorited()) {
                        cl.destroyFavorite(mTweet.getId());
                        mTweet.setIsFavorited(false);
                    } else {
                        cl.createFavorite(mTweet.getId());
                        mTweet.setIsFavorited(true);
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            getActivity().invalidateOptionsMenu();
                        }
                    });
                } catch (final Exception e) {
                    e.printStackTrace();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            AppMsg.makeText(getActivity(), e.getMessage(), AppMsg.STYLE_ALERT).show();
                        }
                    });
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        dialog.dismiss();
                    }
                });
            }
        });
        t.setPriority(Thread.MAX_PRIORITY);
        t.start();
    }

    private void showRetweetDialog() {
        new AlertDialog.Builder(getActivity()).setItems(R.array.retweet_options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    default:
                        performRetweet();
                    case 1:
                        startActivity(new Intent(getActivity(), ComposeActivity.class)
                                .putExtra("content", "\"@" + mTweet.getUser().getScreenName() + ": " + mTweet.getText() + "\" "));
                        break;
                }
            }
        }).show();
    }

    private void performRetweet() {
        final ProgressDialog dialog = new ProgressDialog(getActivity());
        dialog.setMessage(getString(R.string.please_wait));
        dialog.setCancelable(false);
        dialog.show();

        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Twitter cl = BoidApp.get(getActivity()).getClient();
                    cl.retweetStatus(mTweet.getId());
                } catch (final Exception e) {
                    e.printStackTrace();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            AppMsg.makeText(getActivity(), e.getMessage(), AppMsg.STYLE_ALERT).show();
                        }
                    });
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        dialog.dismiss();
                    }
                });
            }
        });
        t.setPriority(Thread.MAX_PRIORITY);
        t.start();
    }

    private void performShare() {
        String shareBody = "@" + mTweet.getUser().getScreenName() + ": " + mTweet.getText();
        Intent sharingIntent = new Intent(Intent.ACTION_SEND)
                .setType("text/plain").putExtra(Intent.EXTRA_TEXT, shareBody);
        startActivity(Intent.createChooser(sharingIntent, getResources().getString(R.string.share_using)));
    }

    @Override
    protected int getLayout() {
        return R.layout.fragment_tweet_viewer;
    }

    @Override
    public String getTitle() {
        return getString(R.string.tweet);
    }

    @Override
    protected void onVisibilityChange(boolean visible) {
    }
}