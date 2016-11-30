package org.briarproject.bramble.crypto;

import org.briarproject.bramble.api.crypto.PasswordStrengthEstimator;
import org.briarproject.bramble.api.nullsafety.NotNullByDefault;

import java.util.HashSet;

import javax.annotation.concurrent.Immutable;

@Immutable
@NotNullByDefault
class PasswordStrengthEstimatorImpl implements PasswordStrengthEstimator {

	private static final int LOWER = 26;
	private static final int UPPER = 26;
	private static final int DIGIT = 10;
	private static final int OTHER = 10;
	private static final double STRONG = Math.log(Math.pow(LOWER + UPPER +
			DIGIT + OTHER, 10));

	@Override
	public float estimateStrength(String password) {
		HashSet<Character> unique = new HashSet<Character>();
		int length = password.length();
		for (int i = 0; i < length; i++) unique.add(password.charAt(i));
		boolean lower = false, upper = false, digit = false, other = false;
		for (char c : unique) {
			if (Character.isLowerCase(c)) lower = true;
			else if (Character.isUpperCase(c)) upper = true;
			else if (Character.isDigit(c)) digit = true;
			else other = true;
		}
		int alphabetSize = 0;
		if (lower) alphabetSize += LOWER;
		if (upper) alphabetSize += UPPER;
		if (digit) alphabetSize += DIGIT;
		if (other) alphabetSize += OTHER;
		double score = Math.log(Math.pow(alphabetSize, unique.size()));
		return Math.min(1, (float) (score / STRONG));
	}
}
