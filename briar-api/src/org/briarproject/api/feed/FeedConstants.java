package org.briarproject.api.feed;

public interface FeedConstants {

	/* delay after start before fetching feed, in minutes */
	int FETCH_DELAY_INITIAL = 1;

	/* the interval the feed should be fetched, in minutes */
	int FETCH_INTERVAL = 30;

	// group metadata keys
	String KEY_FEEDS = "feeds";
	String KEY_FEED_URL = "feedURL";
	String KEY_BLOG_GROUP_ID = "blogGroupId";
	String KEY_FEED_TITLE = "feedTitle";
	String KEY_FEED_DESC = "feedDesc";
	String KEY_FEED_AUTHOR = "feedAuthor";
	String KEY_FEED_ADDED = "feedAdded";
	String KEY_FEED_UPDATED = "feedUpdated";
	String KEY_FEED_LAST_ENTRY = "feedLastEntryTime";

}
