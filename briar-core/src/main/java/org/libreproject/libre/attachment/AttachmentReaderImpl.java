package org.libreproject.libre.attachment;

import org.libreproject.bramble.api.FormatException;
import org.libreproject.bramble.api.client.ClientHelper;
import org.libreproject.bramble.api.data.BdfDictionary;
import org.libreproject.bramble.api.db.DbException;
import org.libreproject.bramble.api.db.NoSuchMessageException;
import org.libreproject.bramble.api.sync.Message;
import org.libreproject.bramble.api.sync.MessageId;
import org.libreproject.libre.api.attachment.Attachment;
import org.libreproject.libre.api.attachment.AttachmentHeader;
import org.libreproject.libre.api.attachment.AttachmentReader;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import javax.inject.Inject;

import static org.libreproject.libre.api.attachment.MediaConstants.MSG_KEY_CONTENT_TYPE;
import static org.libreproject.libre.api.attachment.MediaConstants.MSG_KEY_DESCRIPTOR_LENGTH;

public class AttachmentReaderImpl implements AttachmentReader {

	private final ClientHelper clientHelper;

	@Inject
	public AttachmentReaderImpl(ClientHelper clientHelper) {
		this.clientHelper = clientHelper;
	}

	@Override
	public Attachment getAttachment(AttachmentHeader h) throws DbException {
		// TODO: Support large messages
		MessageId m = h.getMessageId();
		Message message = clientHelper.getMessage(m);
		// Check that the message is in the expected group, to prevent it from
		// being loaded in the context of a different group
		if (!message.getGroupId().equals(h.getGroupId())) {
			throw new NoSuchMessageException();
		}
		byte[] body = message.getBody();
		try {
			BdfDictionary meta = clientHelper.getMessageMetadataAsDictionary(m);
			String contentType = meta.getString(MSG_KEY_CONTENT_TYPE);
			if (!contentType.equals(h.getContentType()))
				throw new NoSuchMessageException();
			int offset = meta.getLong(MSG_KEY_DESCRIPTOR_LENGTH).intValue();
			InputStream stream = new ByteArrayInputStream(body, offset,
					body.length - offset);
			return new Attachment(h, stream);
		} catch (FormatException e) {
			throw new NoSuchMessageException();
		}
	}

}
