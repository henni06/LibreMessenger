package org.libreproject.libre.android.viewmodel;

import org.libreproject.libre.android.contact.add.remote.AddContactViewModel;
import org.libreproject.libre.android.contact.add.remote.PendingContactListViewModel;
import org.libreproject.libre.android.conversation.ConversationViewModel;
import org.libreproject.libre.android.conversation.ImageViewModel;

import javax.inject.Singleton;

import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import dagger.Binds;
import dagger.Module;
import dagger.multibindings.IntoMap;

@Module
public abstract class ViewModelModule {

	@Binds
	@IntoMap
	@ViewModelKey(ConversationViewModel.class)
	abstract ViewModel bindConversationViewModel(
			ConversationViewModel conversationViewModel);

	@Binds
	@IntoMap
	@ViewModelKey(ImageViewModel.class)
	abstract ViewModel bindImageViewModel(
			ImageViewModel imageViewModel);

	@Binds
	@IntoMap
	@ViewModelKey(AddContactViewModel.class)
	abstract ViewModel bindAddContactViewModel(
			AddContactViewModel addContactViewModel);

	@Binds
	@IntoMap
	@ViewModelKey(PendingContactListViewModel.class)
	abstract ViewModel bindPendingRequestsViewModel(
			PendingContactListViewModel pendingContactListViewModel);

	@Binds
	@Singleton
	abstract ViewModelProvider.Factory bindViewModelFactory(
			ViewModelFactory viewModelFactory);

}
