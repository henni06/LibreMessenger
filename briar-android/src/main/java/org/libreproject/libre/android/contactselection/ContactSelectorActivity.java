package org.libreproject.libre.android.contactselection;

import android.os.Bundle;

import org.libreproject.bramble.api.contact.ContactId;
import org.libreproject.bramble.api.nullsafety.MethodsNotNullByDefault;
import org.libreproject.bramble.api.nullsafety.ParametersNotNullByDefault;
import org.libreproject.bramble.api.sync.GroupId;
import org.libreproject.libre.R;
import org.libreproject.libre.android.activity.BriarActivity;
import org.libreproject.libre.android.fragment.BaseFragment.BaseFragmentListener;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.annotation.Nullable;

import androidx.annotation.CallSuper;
import androidx.annotation.LayoutRes;
import androidx.annotation.UiThread;

@MethodsNotNullByDefault
@ParametersNotNullByDefault
public abstract class ContactSelectorActivity
		extends BriarActivity
		implements BaseFragmentListener, ContactSelectorListener {

	protected final static String CONTACTS = "contacts";

	// Subclasses may initialise the group ID in different places
	protected GroupId groupId;
	protected Collection<ContactId> contacts;

	@Override
	public void onCreate(@Nullable Bundle bundle) {
		super.onCreate(bundle);

		setContentView(getLayout());

		if (bundle != null) {
			// restore group ID if it was saved
			byte[] groupBytes = bundle.getByteArray(GROUP_ID);
			if (groupBytes != null) groupId = new GroupId(groupBytes);
			// restore selected contacts if a selection was saved
			ArrayList<Integer> intContacts =
					bundle.getIntegerArrayList(CONTACTS);
			if (intContacts != null) {
				contacts = getContactsFromIntegers(intContacts);
			}
		}
	}

	@LayoutRes
	protected int getLayout() {
		return R.layout.activity_fragment_container;
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (groupId != null) {
			// save the group ID here regardless of how subclasses initialize it
			outState.putByteArray(GROUP_ID, groupId.getBytes());
		}
		if (contacts != null) {
			outState.putIntegerArrayList(CONTACTS,
					getContactsFromIds(contacts));
		}
	}

	@CallSuper
	@UiThread
	@Override
	public void contactsSelected(Collection<ContactId> contacts) {
		this.contacts = contacts;
	}

	static ArrayList<Integer> getContactsFromIds(
			Collection<ContactId> contacts) {
		// transform ContactIds to Integers so they can be added to a bundle
		ArrayList<Integer> intContacts = new ArrayList<>(contacts.size());
		for (ContactId contactId : contacts) {
			intContacts.add(contactId.getInt());
		}
		return intContacts;
	}

	static Collection<ContactId> getContactsFromIntegers(
			ArrayList<Integer> intContacts) {
		// turn contact integers from a bundle back to ContactIds
		List<ContactId> contacts = new ArrayList<>(intContacts.size());
		for (Integer c : intContacts) {
			contacts.add(new ContactId(c));
		}
		return contacts;
	}


}
