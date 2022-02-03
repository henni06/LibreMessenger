package org.libreproject.bramble.data;

import org.libreproject.bramble.api.data.BdfWriter;
import org.libreproject.bramble.api.data.BdfWriterFactory;
import org.libreproject.bramble.api.nullsafety.NotNullByDefault;

import java.io.OutputStream;

import javax.annotation.concurrent.Immutable;

@Immutable
@NotNullByDefault
class BdfWriterFactoryImpl implements BdfWriterFactory {

	@Override
	public BdfWriter createWriter(OutputStream out) {
		return new BdfWriterImpl(out);
	}
}
