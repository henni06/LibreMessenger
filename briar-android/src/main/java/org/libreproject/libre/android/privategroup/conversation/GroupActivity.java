package org.libreproject.libre.android.privategroup.conversation;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import org.libreproject.bramble.api.nullsafety.MethodsNotNullByDefault;
import org.libreproject.bramble.api.nullsafety.ParametersNotNullByDefault;
import org.libreproject.libre.R;
import org.libreproject.libre.android.activity.ActivityComponent;
import org.libreproject.libre.android.privategroup.creation.GroupInviteActivity;
import org.libreproject.libre.android.privategroup.memberlist.GroupMemberListActivity;
import org.libreproject.libre.android.privategroup.reveal.RevealContactsActivity;
import org.libreproject.libre.android.threaded.ThreadListActivity;
import org.libreproject.libre.android.threaded.ThreadListViewModel;

import javax.annotation.Nullable;
import javax.inject.Inject;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.MenuCompat;
import androidx.lifecycle.ViewModelProvider;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static org.libreproject.bramble.api.nullsafety.NullSafety.requireNonNull;
import static org.libreproject.libre.android.activity.RequestCodes.REQUEST_GROUP_INVITE;
import static org.libreproject.libre.android.util.UiUtils.observeOnce;
import static org.libreproject.libre.api.privategroup.PrivateGroupConstants.MAX_GROUP_POST_TEXT_LENGTH;

@MethodsNotNullByDefault
@ParametersNotNullByDefault
public class GroupActivity extends
		ThreadListActivity<GroupMessageItem, GroupMessageAdapter> {

	@Inject
	ViewModelProvider.Factory viewModelFactory;

	private GroupViewModel viewModel;
	private Menu menu;
	private boolean creator=false;

	@Override
	public void injectActivity(ActivityComponent component) {
		component.inject(this);
		viewModel = new ViewModelProvider(this, viewModelFactory)
				.get(GroupViewModel.class);
	}

	@Override
	protected ThreadListViewModel<GroupMessageItem> getViewModel() {
		return viewModel;
	}

	@Override
	protected GroupMessageAdapter createAdapter() {
		return new GroupMessageAdapter(this);
	}

	@Override
	protected Menu getMenu() {
		return menu;
	}

	@Override
	public void onCreate(@Nullable Bundle state) {
		super.onCreate(state);

		Toolbar toolbar = setUpCustomToolbar(false);
		// Open member list on Toolbar click
		toolbar.setOnClickListener(v -> {
			Intent i = new Intent(GroupActivity.this,
					GroupMemberListActivity.class);
			i.putExtra(GROUP_ID, groupId.getBytes());
			startActivity(i);
		});

		String groupName = getIntent().getStringExtra(GROUP_NAME);
		if (groupName != null) setTitle(groupName);
		observeOnce(viewModel.getPrivateGroup(), this, privateGroup ->
				setTitle(privateGroup.getName())
		);



		observeOnce(viewModel.isCreator(), this, adapter::setIsCreator);
		observeOnce(viewModel.isCreator(), this, this::setCreator);

		// start with group disabled and enable when not dissolved
		//NH solve issue!!!
		//setGroupEnabled(false);
		viewModel.isDissolved().observe(this, dissolved -> {
			setGroupEnabled(!dissolved);
			// only show dialog when no prior state
			if (dissolved && state == null) onGroupDissolved();
		});

	}

	protected void setCreator(boolean creator){
		this.creator=creator;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu items for use in the action bar
		super.onCreateOptionsMenu(menu);
		this.menu=menu;
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.group_actions, menu);
		MenuCompat.setGroupDividerEnabled(menu,true);
		// show items based on role (which will not change, so observe once)
		observeOnce(viewModel.isCreator(), this, isCreator -> {
			menu.findItem(R.id.action_group_reveal).setVisible(!isCreator);
			menu.findItem(R.id.action_group_invite).setVisible(isCreator);
			menu.findItem(R.id.action_group_leave).setVisible(!isCreator);
			menu.findItem(R.id.action_group_dissolve).setVisible(isCreator);
			menu.findItem(R.id.action_location_share).setVisible(!isCreator);

		});
		if(locationObserver.isLocationActivated(groupId)){
			menu.findItem(R.id.action_location_share).setTitle(R.string.menu_hide_location);

		}else{

			menu.findItem(R.id.action_location_share).setTitle(R.string.menu_send_location);

		}
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int itemId = item.getItemId();
		if (itemId == R.id.action_group_member_list) {
			Intent i = new Intent(this, GroupMemberListActivity.class);
			i.putExtra(GROUP_ID, groupId.getBytes());
			startActivity(i);
			return true;
		} else if (itemId == R.id.action_group_reveal) {
			if (requireNonNull(viewModel.isCreator().getValue()))
				throw new IllegalStateException();
			Intent i = new Intent(this, RevealContactsActivity.class);
			i.putExtra(GROUP_ID, groupId.getBytes());
			startActivity(i);
			return true;
		} else if (itemId == R.id.action_group_invite) {
			if (!requireNonNull(viewModel.isCreator().getValue()))
				throw new IllegalStateException();
			Intent i = new Intent(this, GroupInviteActivity.class);
			i.putExtra(GROUP_ID, groupId.getBytes());
			startActivityForResult(i, REQUEST_GROUP_INVITE);
			return true;
		} else if (itemId == R.id.action_group_leave) {
			if (requireNonNull(viewModel.isCreator().getValue()))
				throw new IllegalStateException();
			showLeaveGroupDialog();
			return true;
		} else if (itemId == R.id.action_group_dissolve) {
			if (!requireNonNull(viewModel.isCreator().getValue()))
				throw new IllegalStateException();
			showDissolveGroupDialog();
			return true;
		} else if(itemId==R.id.action_map){
			if(getView()==V_LIST){
				showView(V_MAP);

				menu.findItem(R.id.action_map).setTitle(R.string.menu_list);
				menu.findItem(R.id.action_show_map_functions).setVisible(creator);

				menu.findItem(R.id.action_clear_map).setVisible(creator);

			}else{
				showView(V_LIST);
				if(creator){
					menu.findItem(R.id.action_show_map_functions).setVisible(false);
				}
				menu.findItem(R.id.action_map).setTitle(R.string.menu_map);
				menu.findItem(R.id.action_clear_map).setVisible(false);
			}
			return true;
		} else if(itemId==R.id.action_clear_map){
			threadMap.clearMap();
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onActivityResult(int request, int result,
			@Nullable Intent data) {
		if (request == REQUEST_GROUP_INVITE && result == RESULT_OK) {
			displaySnackbar(R.string.groups_invitation_sent);
		} else super.onActivityResult(request, result, data);
	}

	@Override
	protected int getMaxTextLength() {
		return MAX_GROUP_POST_TEXT_LENGTH;
	}

	@Override
	public void onReplyClick(GroupMessageItem item) {
		Boolean isDissolved = viewModel.isDissolved().getValue();
		if (isDissolved != null && !isDissolved) super.onReplyClick(item);
	}

	private void setGroupEnabled(boolean enabled) {
		sendController.setReady(enabled);
		//NH to be solved!!
		//threadListFragment.getList().getRecyclerView().setAlpha(enabled ? 1f : 0.5f);

		if (!enabled) {
			textInput.setVisibility(GONE);
			if (textInput.isKeyboardOpen()) textInput.hideSoftKeyboard();
		} else {
			textInput.setVisibility(VISIBLE);
		}
	}

	private void showLeaveGroupDialog() {
		AlertDialog.Builder builder =
				new AlertDialog.Builder(this, R.style.LibreDialogTheme);
		builder.setTitle(getString(R.string.groups_leave_dialog_title));
		builder.setMessage(getString(R.string.groups_leave_dialog_message));
		builder.setNegativeButton(R.string.dialog_button_leave,
				(d, w) -> deleteGroup());
		builder.setPositiveButton(R.string.cancel, null);
		builder.show();
	}

	private void showDissolveGroupDialog() {
		AlertDialog.Builder builder =
				new AlertDialog.Builder(this, R.style.LibreDialogTheme);
		builder.setTitle(getString(R.string.groups_dissolve_dialog_title));
		builder.setMessage(getString(R.string.groups_dissolve_dialog_message));
		builder.setNegativeButton(R.string.groups_dissolve_button,
				(d, w) -> deleteGroup());
		builder.setPositiveButton(R.string.cancel, null);
		builder.show();
	}

	private void deleteGroup() {
		// The activity is going to be destroyed by the
		// GroupRemovedEvent being fired
		viewModel.deletePrivateGroup();
	}

	private void onGroupDissolved() {
		AlertDialog.Builder builder =
				new AlertDialog.Builder(this, R.style.LibreDialogTheme);
		builder.setTitle(getString(R.string.groups_dissolved_dialog_title));
		builder.setMessage(getString(R.string.groups_dissolved_dialog_message));
		builder.setNeutralButton(R.string.ok, null);
		builder.show();
	}



}
