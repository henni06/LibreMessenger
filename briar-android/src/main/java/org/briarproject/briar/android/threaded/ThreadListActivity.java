package org.briarproject.briar.android.threaded;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.MenuItem;

import com.google.android.material.snackbar.Snackbar;

import org.briarproject.bramble.api.nullsafety.MethodsNotNullByDefault;
import org.briarproject.bramble.api.nullsafety.ParametersNotNullByDefault;
import org.briarproject.bramble.api.sync.GroupId;
import org.briarproject.bramble.api.sync.MessageId;
import org.briarproject.briar.R;
import org.briarproject.briar.android.activity.BriarActivity;
import org.briarproject.briar.android.sharing.SharingController.SharingInfo;
import org.briarproject.briar.android.threaded.ThreadItemAdapter.ThreadItemListener;
import org.briarproject.briar.android.util.BriarSnackbarBuilder;
import org.briarproject.briar.android.view.BriarRecyclerView;
import org.briarproject.briar.android.view.TextInputView;
import org.briarproject.briar.android.view.TextSendController;
import org.briarproject.briar.android.view.TextSendController.SendListener;
import org.briarproject.briar.android.view.TextSendController.SendState;
import org.briarproject.briar.android.view.UnreadMessageButton;
import org.briarproject.briar.android.viewmodel.LiveResult;
import org.briarproject.briar.api.attachment.AttachmentHeader;
import org.osmdroid.util.GeoPoint;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.ActionBar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import static androidx.recyclerview.widget.RecyclerView.NO_POSITION;
import static org.briarproject.bramble.util.StringUtils.isNullOrEmpty;
import static org.briarproject.briar.android.view.TextSendController.SendState.SENT;

@MethodsNotNullByDefault
@ParametersNotNullByDefault
public abstract class ThreadListActivity<I extends ThreadItem, A extends ThreadItemAdapter<I>>
		extends BriarActivity implements SendListener, ThreadItemListener<I> {

	public static final int V_LIST=0;
	public static final int V_MAP=1;
	public static final LocationObserver locationObserver=new LocationObserver();
	private int view;
	private FragmentStateAdapter pagerAdapter;

	protected ThreadListFragment threadListFragment;
	private ThreadMap threadMap;
	protected final A adapter = createAdapter();
	protected abstract ThreadListViewModel<I> getViewModel();
	protected abstract A createAdapter();

	protected TextInputView textInput;
	protected TextSendController sendController;
	protected GroupId groupId;


	private static LocationManager locationManager;
	//private ThreadScrollListener<I> scrollListener;

	@CallSuper
	@Override
	public void onCreate(@Nullable Bundle state) {
		super.onCreate(state);

		setContentView(R.layout.activity_threaded_conversation);

		threadMap=new ThreadMap();


		Intent i = getIntent();
		byte[] b = i.getByteArrayExtra(GROUP_ID);
		if (b == null) throw new IllegalStateException("No GroupId in intent.");
		groupId = new GroupId(b);
		ThreadListViewModel<I> viewModel = getViewModel();
		viewModel.setGroupId(groupId);


		textInput = findViewById(R.id.text_input_container);
		sendController = new TextSendController(textInput, this, false);
		textInput.setSendController(sendController);
		textInput.setMaxTextLength(getMaxTextLength());
		textInput.setReady(true);

		UnreadMessageButton upButton = findViewById(R.id.upButton);
		UnreadMessageButton downButton = findViewById(R.id.downButton);
		threadListFragment=new ThreadListFragment(adapter,viewModel,upButton,downButton);

		showView(V_LIST);

		upButton.setOnClickListener(v -> {
			threadListFragment.scrollUp();
		});
		downButton.setOnClickListener(v -> {
			threadListFragment.scrollDown();
		});

		viewModel.getItems().observe(this, result -> result
				.onError(this::handleException)
				.onSuccess(this::displayItems)
		);

		viewModel.getLocations().observe(this,this::handleLocationData);
		viewModel.getSharingInfo().observe(this, this::setToolbarSubTitle);

		viewModel.getGroupRemoved().observe(this, removed -> {
			if (removed) supportFinishAfterTransition();
		});
		viewModel.setThreadMap(threadMap);

	}

	@CallSuper
	@Override
	public void onStart() {
		super.onStart();
		getViewModel().blockAndClearNotifications();

	}

	@CallSuper
	@Override
	public void onStop() {
		super.onStop();
		getViewModel().unblockNotifications();

	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == android.R.id.home) {
			supportFinishAfterTransition();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onBackPressed() {
		if (adapter.getHighlightedItem() != null) {
			textInput.clearText();
			getViewModel().setReplyId(null);
			updateTextInput();
		} else {
			super.onBackPressed();
		}
	}


	protected void displayItems(List<I> items) {

		if (items.isEmpty()) {
			threadListFragment.getList().showData();

		} else {
			adapter.submitList(items, () -> {
				// do stuff *after* list had been updated
				scrollAfterListCommitted();
				updateTextInput();
			});
		}
	}

	/**
	 * Scrolls to the first visible item last time the activity was open,
	 * if one exists and this is the first time, the list gets displayed.
	 * Or scrolls to a locally added item that has just been added to the list.
	 */

	private void scrollAfterListCommitted() {
		MessageId restoredFirstVisibleItemId =
				getViewModel().getAndResetRestoredMessageId();
		MessageId scrollToItem =
				getViewModel().getAndResetScrollToItem();
		if (restoredFirstVisibleItemId != null) {
			threadListFragment.scrollToItemAtTop(restoredFirstVisibleItemId);
		} else if (scrollToItem != null) {
			threadListFragment.scrollToItemAtTop(scrollToItem);
		}
		threadListFragment.scrollAfterListCommit();
	}

	protected void displaySnackbar(@StringRes int stringId) {
		threadListFragment.displaySnackbar(stringId);
	}

	@Override
	public void onReplyClick(I item) {
		getViewModel().setReplyId(item.getId());
		updateTextInput();
		// FIXME This does not work for a hardware keyboard
		if (textInput.isKeyboardOpen()) {
			threadListFragment.scrollToItemAtTop(item.getId());
		} else {
			// wait with scrolling until keyboard opened
			textInput.setOnKeyboardShownListener(() -> {
				threadListFragment.scrollToItemAtTop(item.getId());
				textInput.setOnKeyboardShownListener(null);
			});
		}
	}

	protected void handleLocationData(LiveResult<List<GeoPoint>> points) {
		System.out.println(points);
	}

	protected void setToolbarSubTitle(SharingInfo sharingInfo) {
		ActionBar actionBar = getSupportActionBar();
		if(locationObserver.isLocationActivated(groupId)){
			actionBar.setSubtitle(R.string.sharing_warning);
		}else
		if (actionBar != null) {
			actionBar.setSubtitle(getString(R.string.shared_with,
					sharingInfo.total, sharingInfo.online));
		}
	}






	private void updateTextInput() {
		MessageId replyId = getViewModel().getReplyId();
		if (replyId != null) {
			textInput.setHint(R.string.forum_message_reply_hint);
			textInput.showSoftKeyboard();
		} else {
			textInput.setHint(R.string.forum_new_message_hint);
		}
		adapter.setHighlightedItem(replyId);
	}



	@Override
	public LiveData<SendState> onSendClick(@Nullable String text,
			List<AttachmentHeader> headers, long expectedAutoDeleteTimer) {
		if (isNullOrEmpty(text)) throw new AssertionError();

		MessageId replyId = getViewModel().getReplyId();
		getViewModel().createAndStoreMessage(text, replyId);
		textInput.hideSoftKeyboard();
		textInput.clearText();
		getViewModel().setReplyId(null);
		updateTextInput();
		return new MutableLiveData<>(SENT);
	}

	protected abstract int getMaxTextLength();

	protected int getView(){
		return view;
	}

	protected void showView(int view){
		this.view=view;
		FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
		switch(view){
			case V_LIST:
				transaction.replace(R.id.conversation_container, threadListFragment);
				break;
			case V_MAP:
				transaction.replace(R.id.conversation_container, threadMap);
				break;
		}
		transaction.addToBackStack(null);

		transaction.commit();
	}

	@SuppressLint("MissingPermission")
	protected boolean publishLocation(){
		if(locationManager==null){
			locationManager =
					(LocationManager) this
							.getSystemService(Context.LOCATION_SERVICE);
		}
		if(!locationObserver.isLocationActivated(this.groupId)){



			LocationListener locationListener = new LocationListener() {
				public void onLocationChanged(Location location) {
					try {
						GeoPoint currentLocation =
								new GeoPoint(location.getLatitude(),
										location.getLongitude());
						double bearing=location.getBearing();
						//double speed=location.getSpeed();
						//double alt=location.getAltitude();
						getViewModel().createAndStoreMessage(
								"{\"type\":\"location\",\"lng\":" +
										currentLocation.getLongitude() +
										",\"lat\":" +
										currentLocation.getLatitude() + ",\"bearing\":"+
					bearing+"}",
								null);
					} catch (Exception e) {
					}
				}

				public void onStatusChanged(String provider, int status,
						Bundle extras) {
				}

				public void onProviderEnabled(String provider) {
				}

				public void onProviderDisabled(String provider) {
				}
			};
			Criteria criteria = new Criteria();
			String provider = locationManager.getBestProvider(criteria, false);
			ThreadMap.requestPermissionsIfNecessary(this, new String[] {
					Manifest.permission.ACCESS_FINE_LOCATION,
					Manifest.permission.WRITE_EXTERNAL_STORAGE,
					Manifest.permission.ACCESS_COARSE_LOCATION});
			locationManager.requestLocationUpdates(provider, 10000, 10,
					locationListener);
			locationObserver.add(this.groupId,locationListener);
			return true;
		}else{

			locationManager.removeUpdates(locationObserver.get(groupId));
			locationObserver.remove(this.groupId);
			return false;
		}
	}
}
