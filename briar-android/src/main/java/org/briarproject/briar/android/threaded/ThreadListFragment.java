package org.briarproject.briar.android.threaded;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.snackbar.Snackbar;

import org.briarproject.bramble.api.sync.MessageId;
import org.briarproject.briar.R;
import org.briarproject.briar.android.util.BriarSnackbarBuilder;
import org.briarproject.briar.android.view.BriarRecyclerView;
import org.briarproject.briar.android.view.UnreadMessageButton;

import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import static androidx.recyclerview.widget.RecyclerView.NO_POSITION;

public class ThreadListFragment extends Fragment {
	private BriarRecyclerView list;
	private LinearLayoutManager layoutManager;
	private ThreadItemAdapter adapter;
	private ThreadListViewModel viewModel;
	private ThreadScrollListener scrollListener;
	private UnreadMessageButton upButton;
	private UnreadMessageButton downButton;
	public ThreadListFragment(ThreadItemAdapter adapter,ThreadListViewModel viewModel,
			UnreadMessageButton upButton, UnreadMessageButton downButton){
		super();
		this.adapter=adapter;
		this.viewModel=viewModel;
		this.upButton=upButton;
		this.downButton=downButton;
	}

	protected void scrollAfterListCommit(){
		//NH handle this
		//scrollListener.updateUnreadButtons(layoutManager);
	}

	public BriarRecyclerView getList(){
		return list;
	}

	protected ThreadScrollListener	getScrollListener(){
		return scrollListener;
	}


	@Override
	public void onDestroy() {
		super.onDestroy();
		// store list position, so we can restore it when coming back here
		if (layoutManager != null && adapter != null) {
			MessageId id = adapter.getFirstVisibleMessageId(layoutManager);
			viewModel.storeMessageId(id);
		}
	}



	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState){
		View view = inflater.inflate(R.layout.fragment_conversation_list, container, false);
		list = view.findViewById(R.id.list);
		layoutManager = new LinearLayoutManager(this.getActivity());
		list.setLayoutManager(layoutManager);
		list.setAdapter(adapter);
		scrollListener = new ThreadScrollListener<>(adapter, viewModel,
				upButton, downButton);
		list.getRecyclerView().addOnScrollListener(scrollListener);
		return view;
	}


	public void displaySnackbar(@StringRes int stringId) {
		new BriarSnackbarBuilder()
				.make(list, stringId, Snackbar.LENGTH_SHORT)
				.show();
	}

	protected void scrollToItemAtTop(MessageId messageId) {
		int position = adapter.findItemPosition(messageId);
		if (position != NO_POSITION) {
			//NH handle this
			//layoutManager.scrollToPositionWithOffset(position, 0);
		}
	}

	@Override
	public void onStart() {
		super.onStart();

		list.startPeriodicUpdate();
	}

	@Override
	public void onStop() {
		super.onStop();
		list.stopPeriodicUpdate();
	}

	protected void scrollDown(){
		int position = adapter.getVisibleUnreadPosBottom(layoutManager);
		if (position != NO_POSITION) {
			list.getRecyclerView().scrollToPosition(position);
		}
	}

	protected void scrollUp(){
		int position = adapter.getVisibleUnreadPosTop(layoutManager);
		if (position != NO_POSITION) {
			list.getRecyclerView().scrollToPosition(position);
		}
	}
}
