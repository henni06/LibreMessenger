package org.briarproject.android.contact;

import org.briarproject.api.introduction.IntroductionRequest;

// This class is not thread-safe
abstract class ConversationIntroductionItem extends ConversationItem {

	private final IntroductionRequest ir;
	private boolean answered;

	public ConversationIntroductionItem(IntroductionRequest ir) {
		super(ir.getMessageId(), ir.getTimestamp());

		this.ir = ir;
		this.answered = ir.wasAnswered();
	}

	public IntroductionRequest getIntroductionRequest() {
		return ir;
	}

	public boolean wasAnswered() {
		return answered;
	}

	public void setAnswered(boolean answered) {
		this.answered = answered;
	}
}
