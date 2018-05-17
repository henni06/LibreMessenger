package org.briarproject.bramble.data;

import org.briarproject.bramble.api.FormatException;
import org.briarproject.bramble.api.data.BdfDictionary;
import org.briarproject.bramble.api.data.BdfList;
import org.briarproject.bramble.test.BrambleTestCase;
import org.junit.Test;

import java.io.ByteArrayInputStream;

import static org.briarproject.bramble.api.data.BdfDictionary.NULL_VALUE;
import static org.briarproject.bramble.api.data.BdfReader.DEFAULT_MAX_BUFFER_SIZE;
import static org.briarproject.bramble.data.BdfReaderImpl.DEFAULT_NESTED_LIMIT;
import static org.briarproject.bramble.util.StringUtils.fromHexString;
import static org.briarproject.bramble.util.StringUtils.getRandomString;
import static org.briarproject.bramble.util.StringUtils.toHexString;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BdfReaderImplTest extends BrambleTestCase {

	private BdfReaderImpl r = null;

	@Test
	public void testReadEmptyInput() throws Exception {
		setContents("");
		assertTrue(r.eof());
	}

	@Test
	public void testReadNull() throws Exception {
		setContents("00");
		r.readNull();
		assertTrue(r.eof());
	}

	@Test
	public void testSkipNull() throws Exception {
		setContents("00");
		r.skipNull();
		assertTrue(r.eof());
	}

	@Test
	public void testReadBoolean() throws Exception {
		setContents("10" + "11");
		assertFalse(r.readBoolean());
		assertTrue(r.readBoolean());
		assertTrue(r.eof());
	}

	@Test
	public void testSkipBoolean() throws Exception {
		setContents("10" + "11");
		r.skipBoolean();
		r.skipBoolean();
		assertTrue(r.eof());
	}

	@Test
	public void testReadLong8() throws Exception {
		setContents("21" + "00" + "21" + "FF"
				+ "21" + "7F" + "21" + "80");
		assertEquals(0, r.readLong());
		assertEquals(-1, r.readLong());
		assertEquals(Byte.MAX_VALUE, r.readLong());
		assertEquals(Byte.MIN_VALUE, r.readLong());
		assertTrue(r.eof());
	}

	@Test
	public void testSkipLong8() throws Exception {
		setContents("21" + "00");
		r.skipLong();
		assertTrue(r.eof());
	}

	@Test
	public void testReadLong16() throws Exception {
		setContents("22" + "0080" + "22" + "FF7F"
				+ "22" + "7FFF" + "22" + "8000");
		assertEquals(Byte.MAX_VALUE + 1, r.readLong());
		assertEquals(Byte.MIN_VALUE - 1, r.readLong());
		assertEquals(Short.MAX_VALUE, r.readLong());
		assertEquals(Short.MIN_VALUE, r.readLong());
		assertTrue(r.eof());
	}

	@Test
	public void testSkipLong16() throws Exception {
		setContents("22" + "0080");
		r.skipLong();
		assertTrue(r.eof());
	}

	@Test
	public void testReadLong32() throws Exception {
		setContents("24" + "00008000" + "24" + "FFFF7FFF"
				+ "24" + "7FFFFFFF" + "24" + "80000000");
		assertEquals(Short.MAX_VALUE + 1, r.readLong());
		assertEquals(Short.MIN_VALUE - 1, r.readLong());
		assertEquals(Integer.MAX_VALUE, r.readLong());
		assertEquals(Integer.MIN_VALUE, r.readLong());
		assertTrue(r.eof());
	}

	@Test
	public void testSkipLong32() throws Exception {
		setContents("24" + "00008000");
		r.skipLong();
		assertTrue(r.eof());
	}

	@Test
	public void testReadLong64() throws Exception {
		setContents("28" + "0000000080000000" + "28" + "FFFFFFFF7FFFFFFF"
				+ "28" + "7FFFFFFFFFFFFFFF" + "28" + "8000000000000000");
		assertEquals(Integer.MAX_VALUE + 1L, r.readLong());
		assertEquals(Integer.MIN_VALUE - 1L, r.readLong());
		assertEquals(Long.MAX_VALUE, r.readLong());
		assertEquals(Long.MIN_VALUE, r.readLong());
		assertTrue(r.eof());
	}

	@Test
	public void testSkipLong() throws Exception {
		setContents("28" + "0000000080000000");
		r.skipLong();
		assertTrue(r.eof());
	}

	@Test
	public void testReadDouble() throws Exception {
		// http://babbage.cs.qc.edu/IEEE-754/Decimal.html
		// http://steve.hollasch.net/cgindex/coding/ieeefloat.html
		setContents("38" + "0000000000000000" + "38" + "3FF0000000000000"
				+ "38" + "4000000000000000" + "38" + "BFF0000000000000"
				+ "38" + "8000000000000000" + "38" + "FFF0000000000000"
				+ "38" + "7FF0000000000000" + "38" + "7FF8000000000000");
		assertEquals(0, Double.compare(0.0, r.readDouble()));
		assertEquals(0, Double.compare(1.0, r.readDouble()));
		assertEquals(0, Double.compare(2.0, r.readDouble()));
		assertEquals(0, Double.compare(-1.0, r.readDouble()));
		assertEquals(0, Double.compare(-0.0, r.readDouble()));
		assertEquals(0, Double.compare(Double.NEGATIVE_INFINITY,
				r.readDouble()));
		assertEquals(0, Double.compare(Double.POSITIVE_INFINITY,
				r.readDouble()));
		assertTrue(Double.isNaN(r.readDouble()));
		assertTrue(r.eof());
	}

	@Test
	public void testSkipFloat() throws Exception {
		setContents("38" + "0000000000000000");
		r.skipDouble();
		assertTrue(r.eof());
	}

	@Test
	public void testReadString8() throws Exception {
		String longest = getRandomString(Byte.MAX_VALUE);
		String longHex = toHexString(longest.getBytes("UTF-8"));
		// "foo", the empty string, and 127 random letters
		setContents("41" + "03" + "666F6F" + "41" + "00" +
				"41" + "7F" + longHex);
		assertEquals("foo", r.readString());
		assertEquals("", r.readString());
		assertEquals(longest, r.readString());
		assertTrue(r.eof());
	}

	@Test(expected = FormatException.class)
	public void testReadString8ChecksMaxLength() throws Exception {
		int maxBufferSize = 3;
		// "foo", "fooo"
		setContents("41" + "03" + "666F6F"
				+ "41" + "04" + "666F6F6F", maxBufferSize);
		assertEquals("foo", r.readString());
		assertTrue(r.hasString());
		r.readString();
	}

	@Test
	public void testSkipString8() throws Exception {
		String longest = getRandomString(Byte.MAX_VALUE);
		String longHex = toHexString(longest.getBytes("UTF-8"));
		// "foo", the empty string, and 127 random letters
		setContents("41" + "03" + "666F6F" + "41" + "00" +
				"41" + "7F" + longHex);
		r.skipString();
		r.skipString();
		r.skipString();
		assertTrue(r.eof());
	}

	@Test
	public void testReadString16() throws Exception {
		String shortest = getRandomString(Byte.MAX_VALUE + 1);
		String shortHex = toHexString(shortest.getBytes("UTF-8"));
		String longest = getRandomString(Short.MAX_VALUE);
		String longHex = toHexString(longest.getBytes("UTF-8"));
		// 128 random letters and 2^15 -1 random letters
		setContents("42" + "0080" + shortHex + "42" + "7FFF" + longHex);
		assertEquals(shortest, r.readString());
		assertEquals(longest, r.readString());
		assertTrue(r.eof());
	}

	@Test(expected = FormatException.class)
	public void testReadString16ChecksMaxLength() throws Exception {
		int maxBufferSize = Byte.MAX_VALUE + 1;
		String valid = getRandomString(Byte.MAX_VALUE + 1);
		String validHex = toHexString(valid.getBytes("UTF-8"));
		String invalidhex = validHex + "20";
		// 128 random letters, the same plus a space
		setContents("42" + "0080" + validHex
				+ "42" + "0081" + invalidhex, maxBufferSize);
		assertEquals(valid, r.readString());
		assertTrue(r.hasString());
		r.readString();
	}

	@Test
	public void testSkipString16() throws Exception {
		String shortest = getRandomString(Byte.MAX_VALUE + 1);
		String shortHex = toHexString(shortest.getBytes("UTF-8"));
		String longest = getRandomString(Short.MAX_VALUE);
		String longHex = toHexString(longest.getBytes("UTF-8"));
		// 128 random letters and 2^15 - 1 random letters
		setContents("42" + "0080" + shortHex + "42" + "7FFF" + longHex);
		r.skipString();
		r.skipString();
		assertTrue(r.eof());
	}

	@Test
	public void testReadString32() throws Exception {
		String shortest = getRandomString(Short.MAX_VALUE + 1);
		String shortHex = toHexString(shortest.getBytes("UTF-8"));
		// 2^15 random letters
		setContents("44" + "00008000" + shortHex);
		assertEquals(shortest, r.readString());
		assertTrue(r.eof());
	}

	@Test(expected = FormatException.class)
	public void testReadString32ChecksMaxLength() throws Exception {
		int maxBufferSize = Short.MAX_VALUE + 1;
		String valid = getRandomString(maxBufferSize);
		String validHex = toHexString(valid.getBytes("UTF-8"));
		String invalidHex = validHex + "20";
		// 2^15 random letters, the same plus a space
		setContents("44" + "00008000" + validHex +
				"44" + "00008001" + invalidHex, maxBufferSize);
		assertEquals(valid, r.readString());
		assertTrue(r.hasString());
		r.readString();
	}

	@Test
	public void testSkipString32() throws Exception {
		String shortest = getRandomString(Short.MAX_VALUE + 1);
		String shortHex = toHexString(shortest.getBytes("UTF-8"));
		// 2^15 random letters, twice
		setContents("44" + "00008000" + shortHex +
				"44" + "00008000" + shortHex);
		r.skipString();
		r.skipString();
		assertTrue(r.eof());
	}

	@Test
	public void testReadUtf8String() throws Exception {
		String unicode = "\uFDD0\uFDD1\uFDD2\uFDD3";
		String hex = toHexString(unicode.getBytes("UTF-8"));
		// STRING_8 tag, "foo", the empty string, and the test string
		setContents("41" + "03" + "666F6F" + "41" + "00" + "41" + "0C" + hex);
		assertEquals("foo", r.readString());
		assertEquals("", r.readString());
		assertEquals(unicode, r.readString());
		assertTrue(r.eof());
	}

	@Test
	public void testReadRaw8() throws Exception {
		byte[] longest = new byte[Byte.MAX_VALUE];
		String longHex = toHexString(longest);
		// {1, 2, 3}, {}, and 127 zero bytes
		setContents("51" + "03" + "010203" + "51" + "00" +
				"51" + "7F" + longHex);
		assertArrayEquals(new byte[] {1, 2, 3}, r.readRaw());
		assertArrayEquals(new byte[0], r.readRaw());
		assertArrayEquals(longest, r.readRaw());
		assertTrue(r.eof());
	}

	@Test(expected = FormatException.class)
	public void testReadRaw8ChecksMaxLength() throws Exception {
		int maxBufferSize = 3;
		// {1, 2, 3}, {1, 2, 3, 4}
		setContents("51" + "03" + "010203" + "51" + "04" + "01020304",
				maxBufferSize);
		assertArrayEquals(new byte[] {1, 2, 3}, r.readRaw());
		assertTrue(r.hasRaw());
		r.readRaw();
	}

	@Test
	public void testSkipRaw8() throws Exception {
		byte[] longest = new byte[Byte.MAX_VALUE];
		String longHex = toHexString(longest);
		// {1, 2, 3}, {}, and 127 zero bytes
		setContents("51" + "03" + "010203" + "51" + "00" +
				"51" + "7F" + longHex);
		r.skipRaw();
		r.skipRaw();
		r.skipRaw();
		assertTrue(r.eof());
	}

	@Test
	public void testReadRaw16() throws Exception {
		byte[] shortest = new byte[Byte.MAX_VALUE + 1];
		String shortHex = toHexString(shortest);
		byte[] longest = new byte[Short.MAX_VALUE];
		String longHex = toHexString(longest);
		// 128 zero bytes and 2^15 - 1 zero bytes
		setContents("52" + "0080" + shortHex + "52" + "7FFF" + longHex);
		assertArrayEquals(shortest, r.readRaw());
		assertArrayEquals(longest, r.readRaw());
		assertTrue(r.eof());
	}

	@Test(expected = FormatException.class)
	public void testReadRaw16ChecksMaxLength() throws Exception {
		int maxBufferSize = Byte.MAX_VALUE + 1;
		byte[] valid = new byte[maxBufferSize];
		String validHex = toHexString(valid);
		String invalidHex = validHex + "00";
		// 128 zero bytes, 129 zero bytes
		setContents("52" + "0080" + validHex
				+ "52" + "0081" + invalidHex, maxBufferSize);
		assertArrayEquals(valid, r.readRaw());
		assertTrue(r.hasRaw());
		r.readRaw();
	}

	@Test
	public void testSkipRaw16() throws Exception {
		byte[] shortest = new byte[Byte.MAX_VALUE + 1];
		String shortHex = toHexString(shortest);
		byte[] longest = new byte[Short.MAX_VALUE];
		String longHex = toHexString(longest);
		// 128 zero bytes and 2^15 - 1 zero bytes
		setContents("52" + "0080" + shortHex + "52" + "7FFF" + longHex);
		r.skipRaw();
		r.skipRaw();
		assertTrue(r.eof());
	}

	@Test
	public void testReadRaw32() throws Exception {
		byte[] shortest = new byte[Short.MAX_VALUE + 1];
		String shortHex = toHexString(shortest);
		// 2^15 zero bytes
		setContents("54" + "00008000" + shortHex);
		assertArrayEquals(shortest, r.readRaw());
		assertTrue(r.eof());
	}

	@Test(expected = FormatException.class)
	public void testReadRaw32ChecksMaxLength() throws Exception {
		int maxBufferSize = Short.MAX_VALUE + 1;
		byte[] valid = new byte[maxBufferSize];
		String validHex = toHexString(valid);
		String invalidHex = validHex + "00";
		// 2^15 zero bytes, 2^15 + 1 zero bytes
		setContents("54" + "00008000" + validHex +
				"54" + "00008001" + invalidHex, maxBufferSize);
		assertArrayEquals(valid, r.readRaw());
		assertTrue(r.hasRaw());
		r.readRaw();
	}

	@Test
	public void testSkipRaw32() throws Exception {
		byte[] shortest = new byte[Short.MAX_VALUE + 1];
		String shortHex = toHexString(shortest);
		// 2^15 zero bytes, twice
		setContents("54" + "00008000" + shortHex +
				"54" + "00008000" + shortHex);
		r.skipRaw();
		r.skipRaw();
		assertTrue(r.eof());
	}

	@Test
	public void testReadList() throws Exception {
		// A list containing 1, "foo", and null
		setContents("60" + "21" + "01" +
				"41" + "03" + "666F6F" +
				"00" + "80");
		BdfList list = r.readList();
		assertEquals(3, list.size());
		assertEquals(1L, list.get(0));
		assertEquals("foo", list.get(1));
		assertEquals(NULL_VALUE, list.get(2));
	}

	@Test(expected = FormatException.class)
	public void testReadListChecksMaxLengthForString() throws Exception {
		// A list containing "foo", a list containing "fooo"
		setContents("60" + "41" + "03" + "666F6F" + "80"
				+ "60" + "41" + "04" + "666F6F6F" + "80", 3);
		BdfList list = r.readList();
		assertEquals(1, list.size());
		assertEquals("foo", list.get(0));
		assertTrue(r.hasList());
		r.readList();
	}

	@Test(expected = FormatException.class)
	public void testReadListChecksMaxLengthForRaw() throws Exception {
		// A list containing {1, 2, 3}, a list containing {1, 2, 3, 4}
		setContents("60" + "51" + "03" + "010203" + "80"
				+ "60" + "51" + "04" + "01020304" + "80", 3);
		BdfList list = r.readList();
		assertEquals(1, list.size());
		assertArrayEquals(new byte[] {1, 2, 3}, (byte[]) list.get(0));
		assertTrue(r.hasList());
		r.readList();
	}

	@Test
	public void testReadListManually() throws Exception {
		// A list containing 1, "foo", and null
		setContents("60" + "21" + "01" +
				"41" + "03" + "666F6F" +
				"00" + "80");
		r.readListStart();
		assertFalse(r.hasListEnd());
		assertEquals(1, r.readLong());
		assertFalse(r.hasListEnd());
		assertEquals("foo", r.readString());
		assertFalse(r.hasListEnd());
		assertTrue(r.hasNull());
		r.readNull();
		assertTrue(r.hasListEnd());
		r.readListEnd();
		assertTrue(r.eof());
	}

	@Test
	public void testSkipList() throws Exception {
		// A list containing 1, "foo", and 128
		setContents("60" + "21" + "01" +
				"41" + "03" + "666F6F" +
				"22" + "0080" + "80");
		r.skipList();
		assertTrue(r.eof());
	}

	@Test
	public void testReadDictionary() throws Exception {
		// A dictionary containing "foo" -> 123 and "bar" -> null
		setContents("70" + "41" + "03" + "666F6F" + "21" + "7B" +
				"41" + "03" + "626172" + "00" + "80");
		BdfDictionary dictionary = r.readDictionary();
		assertEquals(2, dictionary.size());
		assertTrue(dictionary.containsKey("foo"));
		assertEquals(123L, dictionary.get("foo"));
		assertTrue(dictionary.containsKey("bar"));
		assertEquals(NULL_VALUE, dictionary.get("bar"));
	}

	@Test(expected = FormatException.class)
	public void testReadDictionaryChecksMaxLengthForKey() throws Exception {
		// A dictionary containing "foo" -> null, a dictionary containing
		// "fooo" -> null
		setContents("70" + "41" + "03" + "666F6F" + "00" + "80"
				+ "70" + "41" + "04" + "666F6F6F" + "00" + "80", 3);
		BdfDictionary dictionary = r.readDictionary();
		assertEquals(1, dictionary.size());
		assertEquals(NULL_VALUE, dictionary.get("foo"));
		assertTrue(r.hasDictionary());
		r.readDictionary();
	}

	@Test(expected = FormatException.class)
	public void testReadDictionaryChecksMaxLengthForString() throws Exception {
		// A dictionary containing "foo" -> "bar", a dictionary containing
		// "foo" -> "baar"
		String foo = "41" + "03" + "666F6F";
		setContents("70" + foo + "41" + "03" + "626172" + "80"
				+ "70" + foo + "41" + "04" + "62616172" + "80", 3);
		BdfDictionary dictionary = r.readDictionary();
		assertEquals(1, dictionary.size());
		assertEquals("bar", dictionary.get("foo"));
		assertTrue(r.hasDictionary());
		r.readDictionary();
	}

	@Test(expected = FormatException.class)
	public void testReadDictionaryChecksMaxLengthForRaw() throws Exception {
		// A dictionary containing "foo" -> {1, 2, 3}, a dictionary containing
		// "foo" -> {1, 2, 3, 4}
		String foo = "41" + "03" + "666F6F";
		setContents("70" + foo + "51" + "03" + "010203" + "80"
				+ "70" + foo + "51" + "04" + "01020304" + "80", 3);
		BdfDictionary dictionary = r.readDictionary();
		assertEquals(1, dictionary.size());
		assertArrayEquals(new byte[] {1, 2, 3}, (byte[]) dictionary.get("foo"));
		assertTrue(r.hasDictionary());
		r.readDictionary();
	}

	@Test
	public void testReadDictionaryManually() throws Exception {
		// A dictionary containing "foo" -> 123 and "bar" -> null
		setContents("70" + "41" + "03" + "666F6F" + "21" + "7B" +
				"41" + "03" + "626172" + "00" + "80");
		r.readDictionaryStart();
		assertFalse(r.hasDictionaryEnd());
		assertEquals("foo", r.readString());
		assertFalse(r.hasDictionaryEnd());
		assertEquals(123, r.readLong());
		assertFalse(r.hasDictionaryEnd());
		assertEquals("bar", r.readString());
		assertFalse(r.hasDictionaryEnd());
		assertTrue(r.hasNull());
		r.readNull();
		assertTrue(r.hasDictionaryEnd());
		r.readDictionaryEnd();
		assertTrue(r.eof());
	}

	@Test
	public void testSkipDictionary() throws Exception {
		// A map containing "foo" -> 123 and "bar" -> null
		setContents("70" + "41" + "03" + "666F6F" + "21" + "7B" +
				"41" + "03" + "626172" + "00" + "80");
		r.skipDictionary();
		assertTrue(r.eof());
	}

	@Test
	public void testSkipNestedListsAndDictionaries() throws Exception {
		// A list containing a dictionary containing "" -> an empty list
		setContents("60" + "70" + "4100" + "60" + "80" + "80" + "80");
		r.skipList();
		assertTrue(r.eof());
	}

	@Test
	public void testNestedListWithinDepthLimit() throws Exception {
		// A list containing a list containing a list containing a list...
		String lists = "";
		for (int i = 1; i <= DEFAULT_NESTED_LIMIT; i++) lists += "60";
		for (int i = 1; i <= DEFAULT_NESTED_LIMIT; i++) lists += "80";
		setContents(lists);
		r.readList();
		assertTrue(r.eof());
	}

	@Test(expected = FormatException.class)
	public void testNestedListOutsideDepthLimit() throws Exception {
		// A list containing a list containing a list containing a list...
		String lists = "";
		for (int i = 1; i <= DEFAULT_NESTED_LIMIT + 1; i++) lists += "60";
		for (int i = 1; i <= DEFAULT_NESTED_LIMIT + 1; i++) lists += "80";
		setContents(lists);
		r.readList();
	}

	@Test
	public void testNestedDictionaryWithinDepthLimit() throws Exception {
		// A dictionary containing a dictionary containing a dictionary...
		String dicts = "";
		for (int i = 1; i <= DEFAULT_NESTED_LIMIT; i++)
			dicts += "70" + "41" + "03" + "666F6F";
		dicts += "11";
		for (int i = 1; i <= DEFAULT_NESTED_LIMIT; i++)
			dicts += "80";
		setContents(dicts);
		r.readDictionary();
		assertTrue(r.eof());
	}

	@Test(expected = FormatException.class)
	public void testNestedDictionaryOutsideDepthLimit() throws Exception {
		// A dictionary containing a dictionary containing a dictionary...
		String dicts = "";
		for (int i = 1; i <= DEFAULT_NESTED_LIMIT + 1; i++)
			dicts += "70" + "41" + "03" + "666F6F";
		dicts += "11";
		for (int i = 1; i <= DEFAULT_NESTED_LIMIT + 1; i++)
			dicts += "80";
		setContents(dicts);
		r.readDictionary();
	}

	@Test(expected = FormatException.class)
	public void testOpenList() throws Exception {
		// A list that is not closed
		String list = "60";
		setContents(list);
		r.readList();
	}

	@Test(expected = FormatException.class)
	public void testOpenDictionary() throws Exception {
		// A dictionary that is not closed
		String dicts = "70" + "41" + "03" + "666F6F";
		setContents(dicts);
		r.readDictionary();
	}

	private void setContents(String hex) {
		setContents(hex, DEFAULT_MAX_BUFFER_SIZE);
	}

	private void setContents(String hex, int maxBufferSize) {
		ByteArrayInputStream in = new ByteArrayInputStream(fromHexString(hex));
		r = new BdfReaderImpl(in, DEFAULT_NESTED_LIMIT, maxBufferSize);
	}
}
