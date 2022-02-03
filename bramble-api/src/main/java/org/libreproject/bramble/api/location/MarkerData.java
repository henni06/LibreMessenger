package org.libreproject.bramble.api.location;

import org.libreproject.bramble.api.FormatException;
import org.libreproject.bramble.api.data.BdfList;
import org.libreproject.bramble.api.data.BdfReader;
import org.libreproject.bramble.api.data.BdfReaderFactory;
import org.libreproject.bramble.api.sync.MessageId;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.StringTokenizer;

public class MarkerData {

	private String getMessageText(BdfList body) throws FormatException {
		// Message type (0), member (1), parent ID (2), previous message ID (3),
		// text (4), signature (5)
		return body.getString(4);
	}

	private BdfList toList(byte[] b, int off, int len) throws FormatException {
		ByteArrayInputStream in = new ByteArrayInputStream(b, off, len);
		try {
			BdfReader reader = readerFactory.createReader(in);
			BdfList list = reader.readList();
			if (!reader.eof()) throw new FormatException();
			return list;
		} catch (FormatException e) {
			throw e;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public BdfList toList(byte[] b) throws FormatException {
		return toList(b, 0, b.length);
	}

	public BdfList toList(String markerText) throws FormatException {
		return toList(markerText.getBytes());
	}


	private MessageId messageId;
	private String markerText;
	private BdfReaderFactory readerFactory;
	private String markerId;
	public MarkerData(BdfReaderFactory readerFactory,MessageId messageId,byte[] body){


		this.readerFactory=readerFactory;
		HashMap<String,String> keyValues=new HashMap<>();

		try {

			this.markerText = getMessageText(toList(body));


				if (markerText.trim().startsWith("{") &&
						markerText.trim().endsWith("}")) {
					String jsonPart =
							markerText.substring(1, markerText.length() - 2);
					StringTokenizer jsonTokenizer =
							new StringTokenizer(jsonPart, ",");
					while (jsonTokenizer.hasMoreElements()) {
						String jsonElement = jsonTokenizer.nextToken();
						String[] pair = jsonElement.split(":");
						if (pair.length == 2) {
							keyValues.put(pair[0].replace("\"", ""),
									pair[1].replace("\"", ""));

						}
					}
				}

			}
		catch (Exception e){
			e.printStackTrace();
		}
		if(keyValues.containsKey("id")){
			markerId=keyValues.get("id");
		}

		this.messageId=messageId;
	}

	public String getMarkerId(){
		return markerId;
	}

	public MessageId getMessageId(){
		return messageId;
	}
}
