package com.teamboid.twitter.fragments;

import com.teamboid.twitter.BoidApp;
import com.teamboid.twitter.R;
import com.teamboid.twitter.adapters.ConversationAdapter;
import com.teamboid.twitter.base.BoidAdapter;
import com.teamboid.twitter.base.FeedFragment;
import twitter4j.DirectMessage;
import twitter4j.ResponseList;
import twitter4j.Twitter;

/**
 * A feed fragment that displays the current user's message conversations.
 */
public class ConversationFragment extends FeedFragment<ConversationAdapter.Conversation> {

    private ConversationAdapter adapter;

    @Override
    public int getEmptyText() {
        return R.string.no_conversations;
    }

    @Override
    public BoidAdapter<ConversationAdapter.Conversation> getAdapter() {
        if (adapter == null)
            adapter = new ConversationAdapter(getActivity());
        return adapter;
    }

    @Override
    public void onItemClicked(int index) {
        //TODO
    }

    @Override
    public boolean onItemLongClicked(int index) {
        //TODO
        return false;
    }

    @Override
    public ConversationAdapter.Conversation[] refresh() throws Exception {
        Twitter cl = BoidApp.get(getActivity()).getClient();
        ConversationAdapter.ConversationOrganizer organizer = new ConversationAdapter.ConversationOrganizer(getActivity());
        ResponseList<DirectMessage> msges = cl.getDirectMessages();
        if (msges.size() > 0)
            organizer.add(msges.toArray(new DirectMessage[0]));
        msges = cl.getSentDirectMessages();
        if (msges.size() > 0)
            organizer.add(msges.toArray(new DirectMessage[0]));
        return organizer.toArray();
    }

    @Override
    public int getTitle() {
        return R.string.messages;
    }
}