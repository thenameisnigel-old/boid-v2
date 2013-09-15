package com.afollestad.twitter.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v4.view.ViewPager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import com.afollestad.silk.views.image.SilkImageView;
import com.afollestad.twitter.BoidApp;
import com.afollestad.twitter.R;
import com.afollestad.twitter.adapters.emojis.EmojiPagerAdapter;
import com.afollestad.twitter.data.EmojiDataSource;
import com.afollestad.twitter.data.EmojiRecent;
import com.afollestad.twitter.services.ComposerService;
import com.afollestad.twitter.ui.theming.ThemedLocationActivity;
import com.afollestad.twitter.utilities.TweetUtils;
import com.afollestad.twitter.utilities.Utils;
import com.afollestad.twitter.utilities.text.EmojiConverter;
import com.afollestad.twitter.utilities.text.TextUtils;
import com.afollestad.twitter.views.CounterEditText;
import com.astuetz.viewpager.extensions.PagerSlidingTabStrip;
import twitter4j.Status;
import twitter4j.User;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * The tweet composition UI.
 *
 * @author Aidan Follestad (afollestad)
 */
public class ComposeActivity extends ThemedLocationActivity {

    private Status mReplyTo;
    private boolean mAttachLocation;
    private String mCurrentCapturePath;
    private String mCurrentGalleryPath;

    private boolean isEmojiShowing;
    private static EmojiDataSource dataSource;
    private static ArrayList<EmojiRecent> recents;
    private static CounterEditText input;
    private static EmojiPagerAdapter emojiAdapter;

    private final static int CAPTURE_RESULT = 100;
    private final static int GALLERY_RESULT = 200;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_composer);
        setupInput();
        setUpEmojiKeyboard();
        getActionBar().setDisplayHomeAsUpEnabled(true);
        processIntent();
    }

    @Override
    public void onLocationUpdate(Location location) {
    }

    private void processIntent() {
        EditText input = (EditText) findViewById(R.id.input);
        input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                invalidateOptionsMenu();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        Intent i = getIntent();
        input.setText("");
        if (i.hasExtra("mention")) {
            User mention = (User) i.getSerializableExtra("mention");
            input.append("@" + mention.getScreenName() + " ");
        }
        if (i.hasExtra("content"))
            input.append(i.getStringExtra("content"));
        if (i.hasExtra("reply_to")) {
            mReplyTo = (Status) i.getSerializableExtra("reply_to");
            if (mReplyTo.isRetweet())
                mReplyTo = mReplyTo.getRetweetedStatus();
            input.append(TweetUtils.getReplyAll(BoidApp.get(this).getProfile(), mReplyTo));
            setTitle(R.string.reply);
        } else {
            setTitle(R.string.compose);
        }
        setupInReplyTo();
    }

    private void setupInReplyTo() {
        View frame = findViewById(R.id.inReplyToFrame);
        View label = findViewById(R.id.inReplyToLabel);
        if (mReplyTo != null) {
            frame.setVisibility(View.VISIBLE);
            label.setVisibility(View.VISIBLE);
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            boolean mDisplayRealNames = prefs.getBoolean("display_realname", true);
            SilkImageView profilePic = (SilkImageView) frame.findViewById(R.id.replyProfilePic);
            profilePic.setImageURL(BoidApp.get(this).getImageLoader(), mReplyTo.getUser().getBiggerProfileImageURL());
            ((TextView) frame.findViewById(R.id.replyUsername)).setText(TweetUtils.getDisplayName(mReplyTo.getUser(), mDisplayRealNames));
            TextUtils.linkifyText(this, (TextView) frame.findViewById(R.id.replyContent), mReplyTo, false, false);
        } else {
            frame.setVisibility(View.GONE);
            label.setVisibility(View.GONE);
        }
    }

    private void setupInput() {
        final CounterEditText input = (CounterEditText) findViewById(R.id.input);
        input.setCounterView((TextView) findViewById(R.id.counter));
    }

    private void setUpEmojiKeyboard() {
        isEmojiShowing = false;
        input = (CounterEditText) findViewById(R.id.input);
        dataSource = new EmojiDataSource(this);
        dataSource.open();
        recents = (ArrayList<EmojiRecent>) dataSource.getAllRecents();

        Display d = ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        int keyboardHeight = (int) (d.getHeight() / 3.0);

        ViewPager vp = (ViewPager) findViewById(R.id.emojiKeyboardPager);
        vp.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, keyboardHeight));
        PagerSlidingTabStrip tabs = (PagerSlidingTabStrip) findViewById(R.id.emojiTabs);
        tabs.setIndicatorColor(getResources().getColor(android.R.color.holo_blue_dark));
        emojiAdapter = new EmojiPagerAdapter(this, vp, recents, keyboardHeight);
        vp.setAdapter(emojiAdapter);
        tabs.setViewPager(vp);
        vp.setCurrentItem(1);

        final Context context = this;

        ImageButton delete = (ImageButton) findViewById(R.id.delete);
        delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                removeText(context);
            }
        });
    }

    private void send(MenuItem item) {
        final EditText input = (EditText) findViewById(R.id.input);
        item.setEnabled(false);
        input.setEnabled(false);
        Bundle extras = new Bundle();
        extras.putString("content", input.getText().toString().trim());
        extras.putSerializable("reply_to", mReplyTo);
        if (mAttachLocation)
            extras.putParcelable("location", getCurrentLocation());
        if (mCurrentCapturePath != null)
            extras.putString("media", mCurrentCapturePath);
        else if (mCurrentGalleryPath != null)
            extras.putString("media", mCurrentGalleryPath);
        startService(new Intent(this, ComposerService.class).putExtras(extras));
        finish();
    }

    private boolean invalidateTweetButton() {
        EditText input = (EditText) findViewById(R.id.input);
        return input.getText().toString().trim().length() <= 140 && (!input.getText().toString().trim().isEmpty() || mCurrentCapturePath != null || mCurrentGalleryPath != null);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_composer, menu);
        menu.findItem(R.id.locate).setIcon(mAttachLocation ? R.drawable.ic_location_unattach : Utils.resolveThemeAttr(this, R.attr.attachLocation));
        MenuItem media = menu.findItem(R.id.media);
        media.setIcon(mCurrentCapturePath != null || mCurrentGalleryPath != null ?
                R.drawable.ic_gallery_unattach : Utils.resolveThemeAttr(this, R.attr.attachMedia));
        menu.findItem(R.id.send).setEnabled(invalidateTweetButton());
        MenuItem emoji = menu.findItem(R.id.emoji);
        emoji.setIcon(isEmojiShowing ? R.drawable.ic_emoji_keyboard_showing : Utils.resolveThemeAttr(this, R.attr.emojiKeyboard));
        return super.onCreateOptionsMenu(menu);
    }

    private void capture() {
        if (mCurrentCapturePath != null) {
            mCurrentCapturePath = null;
            invalidateOptionsMenu();
            return;
        }
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "capture_" + timeStamp + "_";
        File image;
        try {
            image = File.createTempFile(imageFileName, ".jpg", getExternalCacheDir());
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
            return;
        }
        mCurrentCapturePath = image.getAbsolutePath();
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                .putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(image));
        startActivityForResult(takePictureIntent, CAPTURE_RESULT);
    }

    private void selectGallery() {
        if (mCurrentGalleryPath != null) {
            mCurrentGalleryPath = null;
            invalidateOptionsMenu();
            return;
        }
        Intent intent = new Intent(Intent.ACTION_PICK).setType("image/*");
        startActivityForResult(intent, GALLERY_RESULT);
    }

    private void insertEmojiKeyboard() {
        if (isEmojiShowing) {
            isEmojiShowing = false;
            findViewById(R.id.emojiKeyboard).setVisibility(View.GONE);
        } else {
            isEmojiShowing = true;
            View keyboard = findViewById(R.id.emojiKeyboard);
            keyboard.setVisibility(View.VISIBLE);
            InputMethodManager imm = (InputMethodManager) getSystemService(
                    Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(keyboard.getWindowToken(), 0);
            findViewById(R.id.input).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (isEmojiShowing) {
                        insertEmojiKeyboard();
                    }
                }
            });
        }
        invalidateOptionsMenu();
    }

    private String getRealPathFromURI(Uri contentUri) {
        String[] proj = {MediaStore.Images.Media.DATA};
        Cursor cursor = getContentResolver().query(contentUri, proj, null, null, null);
        if (!cursor.moveToFirst()) {
            return null;
        }
        return cursor.getString(0);
    }

    public static void insertEmoji(Context context, String emoji, int icon) {
        input.append(EmojiConverter.getSmiledText(context, emoji));

        for (int i = 0; i < recents.size(); i++) {
            if (recents.get(i).text.equals(emoji)) {
                dataSource.updateRecent(icon + "");
                recents.get(i).count++;
                return;
            }
        }

        EmojiRecent recent = dataSource.createRecent(emoji, icon + "");

        if (recent != null) {
            recents.add(recent);
        }
    }

    public static void removeText(Context context) {
        String currentText = input.getText().toString();
        if (currentText.length() > 0) {
            // FIXME most emoji strings are 2 characters long, so they are first turned into a black box/question mark and then removed
            input.setText(EmojiConverter.getSmiledText(context, currentText.substring(0, currentText.length() - 1)));
            input.setSelection(currentText.length() - 1);
        }
    }

    public static void removeRecent(int position) {
        dataSource.deleteRecent(recents.get(position).id);
        recents.remove(position);
        emojiAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CAPTURE_RESULT) {
            if (resultCode == RESULT_CANCELED)
                mCurrentCapturePath = null;
        } else if (requestCode == GALLERY_RESULT) {
            if (resultCode == RESULT_CANCELED)
                mCurrentGalleryPath = null;
            else mCurrentGalleryPath = getRealPathFromURI(data.getData());
        }
        invalidateOptionsMenu();
    }

    private void attachMedia() {
        new AlertDialog.Builder(this).setTitle(R.string.attach_media)
                .setItems(R.array.media_attach_types, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        switch (which) {
                            default:
                                capture();
                                break;
                            case 1:
                                selectGallery();
                                break;
                        }
                    }
                }).show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.send:
                send(item);
                return true;
            case R.id.locate:
                mAttachLocation = !mAttachLocation;
                invalidateOptionsMenu();
                return true;
            case R.id.media:
                attachMedia();
                return true;
            case R.id.emoji:
                insertEmojiKeyboard();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (isEmojiShowing) {
            insertEmojiKeyboard();
        } else {
            super.onBackPressed();
        }
    }
}
