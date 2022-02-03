package org.libreproject.libre.android.util;

public interface ItemReturningAdapter<I> {

	I getItemAt(int position);

	int getItemCount();

}
