package org.libreproject.bramble.properties;

import org.libreproject.bramble.api.FormatException;
import org.libreproject.bramble.api.client.ClientHelper;
import org.libreproject.bramble.api.data.BdfDictionary;
import org.libreproject.bramble.api.data.BdfEntry;
import org.libreproject.bramble.api.data.BdfList;
import org.libreproject.bramble.api.data.MetadataEncoder;
import org.libreproject.bramble.api.plugin.TransportId;
import org.libreproject.bramble.api.properties.TransportProperties;
import org.libreproject.bramble.api.sync.Group;
import org.libreproject.bramble.api.sync.Message;
import org.libreproject.bramble.api.system.Clock;
import org.libreproject.bramble.test.BrambleMockTestCase;
import org.jmock.Expectations;
import org.junit.Test;

import java.io.IOException;

import static org.libreproject.bramble.api.plugin.TransportId.MAX_TRANSPORT_ID_LENGTH;
import static org.libreproject.bramble.api.properties.TransportPropertyManager.CLIENT_ID;
import static org.libreproject.bramble.api.properties.TransportPropertyManager.MAJOR_VERSION;
import static org.libreproject.bramble.test.TestUtils.getGroup;
import static org.libreproject.bramble.test.TestUtils.getMessage;
import static org.libreproject.bramble.test.TestUtils.getTransportId;
import static org.libreproject.bramble.util.StringUtils.getRandomString;
import static org.junit.Assert.assertEquals;

public class TransportPropertyValidatorTest extends BrambleMockTestCase {

	private final ClientHelper clientHelper = context.mock(ClientHelper.class);

	private final TransportId transportId;
	private final BdfDictionary bdfDictionary;
	private final TransportProperties transportProperties;
	private final Group group;
	private final Message message;
	private final TransportPropertyValidator tpv;

	public TransportPropertyValidatorTest() {
		transportId = getTransportId();
		bdfDictionary = BdfDictionary.of(new BdfEntry("foo", "bar"));
		transportProperties = new TransportProperties();
		transportProperties.put("foo", "bar");

		group = getGroup(CLIENT_ID, MAJOR_VERSION);
		message = getMessage(group.getId());

		MetadataEncoder metadataEncoder = context.mock(MetadataEncoder.class);
		Clock clock = context.mock(Clock.class);
		tpv = new TransportPropertyValidator(clientHelper, metadataEncoder,
				clock);
	}

	@Test
	public void testValidateProperMessage() throws IOException {
		BdfList body = BdfList.of(transportId.getString(), 4, bdfDictionary);

		context.checking(new Expectations() {{
			oneOf(clientHelper).parseAndValidateTransportProperties(
					bdfDictionary);
			will(returnValue(transportProperties));
		}});

		BdfDictionary result =
				tpv.validateMessage(message, group, body).getDictionary();
		assertEquals(transportId.getString(), result.getString("transportId"));
		assertEquals(4, result.getLong("version").longValue());
	}

	@Test(expected = FormatException.class)
	public void testValidateWrongVersionValue() throws IOException {
		BdfList body = BdfList.of(transportId.getString(), -1, bdfDictionary);
		tpv.validateMessage(message, group, body);
	}

	@Test(expected = FormatException.class)
	public void testValidateWrongVersionType() throws IOException {
		BdfList body = BdfList.of(transportId.getString(), bdfDictionary,
				bdfDictionary);
		tpv.validateMessage(message, group, body);
	}

	@Test(expected = FormatException.class)
	public void testValidateLongTransportId() throws IOException {
		String wrongTransportIdString =
				getRandomString(MAX_TRANSPORT_ID_LENGTH + 1);
		BdfList body = BdfList.of(wrongTransportIdString, 4, bdfDictionary);
		tpv.validateMessage(message, group, body);
	}

	@Test(expected = FormatException.class)
	public void testValidateEmptyTransportId() throws IOException {
		BdfList body = BdfList.of("", 4, bdfDictionary);
		tpv.validateMessage(message, group, body);
	}
}
