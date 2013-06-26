package com.teamboid.twitter.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import com.teamboid.twitter.R;
import com.teamboid.twitter.adapters.ConversationAdapter;
import com.teamboid.twitter.adapters.MessageAdapter;
import com.teamboid.twitter.utilities.Utils;
import twitter4j.DirectMessage;

/**
 * A feed fragment that displays direct messages from a conversation, and allows you to send messages.
 */
public class MessagesFragment extends Fragment {

    private MessageAdapter adapter;
    private ConversationAdapter.Conversation mConvo;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mConvo = (ConversationAdapter.Conversation) Utils.deserializeObject(getArguments().getString("conversation"));
        mConvo.sort();
        getActivity().setTitle(mConvo.getEndUser().getName());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.convo_viewer, null);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        adapter = new MessageAdapter(getActivity());
        adapter.add(mConvo.getMessages().toArray(new DirectMessage[0]), true);
        ListView list = (ListView) view.findViewById(R.id.list);
        list.setEmptyView(view.findViewById(R.id.empty));
        list.setAdapter(adapter);
    }
}
