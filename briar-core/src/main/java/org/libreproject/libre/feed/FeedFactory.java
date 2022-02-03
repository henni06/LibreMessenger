package org.libreproject.libre.feed;

import com.rometools.rome.feed.synd.SyndFeed;

import org.libreproject.bramble.api.FormatException;
import org.libreproject.bramble.api.data.BdfDictionary;
import org.libreproject.libre.api.feed.Feed;

interface FeedFactory {

	/**
	 * Create a new feed based on the feed url
	 * and the metadata of an existing {@link SyndFeed}.
	 */
	Feed createFeed(String url, SyndFeed feed);

	/**
	 * Creates a new updated feed, based on the given existing feed,
	 * new metadata from the given {@link SyndFeed}
	 * and the time of the last feed entry.
	 */
	Feed createFeed(Feed feed, SyndFeed f, long lastEntryTime);

	/**
	 * De-serializes a {@link BdfDictionary} into a {@link Feed}.
	 */
	Feed createFeed(BdfDictionary d) throws FormatException;

	/**
	 * Serializes a {@link Feed} into a {@link BdfDictionary}.
	 */
	BdfDictionary feedToBdfDictionary(Feed feed);

}
