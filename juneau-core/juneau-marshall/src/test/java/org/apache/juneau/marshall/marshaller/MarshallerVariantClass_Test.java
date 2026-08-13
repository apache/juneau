/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.marshall.marshaller;

import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.*;

import java.lang.reflect.*;
import java.util.*;
import java.util.stream.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.bson.*;
import org.apache.juneau.marshall.cbor.*;
import org.apache.juneau.marshall.hjson.*;
import org.apache.juneau.marshall.hocon.*;
import org.apache.juneau.marshall.html.*;
import org.apache.juneau.marshall.ini.*;
import org.apache.juneau.marshall.json.*;
import org.apache.juneau.marshall.json5.*;
import org.apache.juneau.marshall.msgpack.*;
import org.apache.juneau.marshall.parser.*;
import org.apache.juneau.marshall.prototext.*;
import org.apache.juneau.marshall.serializer.*;
import org.apache.juneau.marshall.toml.*;
import org.apache.juneau.marshall.uon.*;
import org.apache.juneau.marshall.urlencoding.*;
import org.apache.juneau.marshall.xml.*;
import org.apache.juneau.marshall.yaml.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

/**
 * Drift/presence + round-trip guard for the Feature-B variant marshaller classes.
 *
 * <p>
 * Asserts, for each variant class:
 * <ul>
 * 	<li>It is a subclass of its format facade, and its {@link #DEFAULT} instance's serializer/parser
 * 		identities match the mapped {@code *Serializer.DEFAULT_*}/{@code <Format>Parser.DEFAULT_*} constants.
 * 	<li>It <b>redeclares the full static shortcut surface</b> of its base format (so none is silently
 * 		inherited and mis-bound to the base {@code DEFAULT}).
 * 	<li>Round-trip / wiring: {@code <Variant>.of(x)} equals the variant serializer's own output for a
 * 		representative bean, {@code <Variant>.to(<Variant>.of(bean), …)} round-trips, and the output is in
 * 		the expected variant form (readable/multi-line vs. compact/single-line, or otherwise distinct from
 * 		the base {@code DEFAULT} output).
 * </ul>
 */
class MarshallerVariantClass_Test extends TestBase {

	/** The full set of static shortcut names a facade may declare. */
	private static final Set<String> SHORTCUT_NAMES = Set.of(
		"of", "to",
		"toTokens", "ofTokens", "toRecords", "ofRecords", "toArrayRecords", "ofArrayRecords");

	static Stream<Arguments> variants() {
		return Stream.of(
			// Pre-existing (Group-1) variants.
			arguments(Json5R.class, Json5.class, Json5Serializer.DEFAULT_READABLE, Json5Parser.DEFAULT),
			arguments(IniR.class, Ini.class, IniSerializer.DEFAULT_READABLE, IniParser.DEFAULT),
			arguments(HjsonC.class, Hjson.class, HjsonSerializer.DEFAULT_COMPACT, HjsonParser.DEFAULT),
			// B-marshall-7 additive symmetry variants.
			arguments(JsonR.class, Json.class, JsonSerializer.DEFAULT_READABLE, JsonParser.DEFAULT),
			arguments(TomlR.class, Toml.class, TomlSerializer.DEFAULT_READABLE, TomlParser.DEFAULT),
			arguments(YamlR.class, Yaml.class, YamlSerializer.DEFAULT_READABLE, YamlParser.DEFAULT),
			arguments(PrototextR.class, Prototext.class, PrototextSerializer.DEFAULT_READABLE, PrototextParser.DEFAULT),
			arguments(UonR.class, Uon.class, UonSerializer.DEFAULT_READABLE, UonParser.DEFAULT),
			arguments(UonE.class, Uon.class, UonSerializer.DEFAULT_ENCODING, UonParser.DEFAULT_DECODING),
			arguments(UrlEncodingR.class, UrlEncoding.class, UrlEncodingSerializer.DEFAULT_READABLE, UrlEncodingParser.DEFAULT),
			arguments(UrlEncodingPlain.class, UrlEncoding.class, UrlEncodingSerializer.DEFAULT_PLAINTEXT, UrlEncodingParser.DEFAULT),
			arguments(UrlEncodingExpanded.class, UrlEncoding.class, UrlEncodingSerializer.DEFAULT_EXPANDED, UrlEncodingParser.DEFAULT),
			arguments(HoconBraces.class, Hocon.class, HoconSerializer.DEFAULT_BRACES, HoconParser.DEFAULT),
			arguments(HoconC.class, Hocon.class, HoconSerializer.DEFAULT_COMPACT, HoconParser.DEFAULT),
			arguments(XmlSq.class, Xml.class, XmlSerializer.DEFAULT_SQ, XmlParser.DEFAULT),
			arguments(XmlSqR.class, Xml.class, XmlSerializer.DEFAULT_SQ_READABLE, XmlParser.DEFAULT),
			arguments(XmlNs.class, Xml.class, XmlSerializer.DEFAULT_NS, XmlParser.DEFAULT),
			arguments(XmlNsSq.class, Xml.class, XmlSerializer.DEFAULT_NS_SQ, XmlParser.DEFAULT),
			arguments(XmlNsSqR.class, Xml.class, XmlSerializer.DEFAULT_NS_SQ_READABLE, XmlParser.DEFAULT),
			arguments(HtmlSq.class, Html.class, HtmlSerializer.DEFAULT_SQ, HtmlParser.DEFAULT),
			arguments(HtmlSqR.class, Html.class, HtmlSerializer.DEFAULT_SQ_READABLE, HtmlParser.DEFAULT),
			arguments(HtmlSimpleSq.class, Html.class, HtmlSerializer.DEFAULT_SIMPLE_SQ, HtmlParser.DEFAULT),
			arguments(BsonSpacedHex.class, Bson.class, BsonSerializer.DEFAULT_SPACED_HEX, BsonParser.DEFAULT_SPACED_HEX),
			arguments(BsonBase64.class, Bson.class, BsonSerializer.DEFAULT_BASE64, BsonParser.DEFAULT_BASE64),
			arguments(CborSpacedHex.class, Cbor.class, CborSerializer.DEFAULT_SPACED_HEX, CborParser.DEFAULT_SPACED_HEX),
			arguments(CborBase64.class, Cbor.class, CborSerializer.DEFAULT_BASE64, CborParser.DEFAULT_BASE64),
			arguments(CborNative.class, Cbor.class, CborSerializer.DEFAULT, CborParser.DEFAULT_NATIVE),
			arguments(MsgPackSpacedHex.class, MsgPack.class, MsgPackSerializer.DEFAULT_SPACED_HEX, MsgPackParser.DEFAULT_SPACED_HEX),
			arguments(MsgPackBase64.class, MsgPack.class, MsgPackSerializer.DEFAULT_BASE64, MsgPackParser.DEFAULT_BASE64),
			arguments(MsgPackNative.class, MsgPack.class, MsgPackSerializer.DEFAULT, MsgPackParser.DEFAULT_NATIVE));
	}

	@ParameterizedTest
	@MethodSource("variants")
	void a01_subclassAndWiringIdentity(Class<? extends Marshaller> variant, Class<? extends Marshaller> base, Serializer s, Parser p) throws Exception {
		assertTrue(base.isAssignableFrom(variant), () -> variant.getSimpleName() + " must be a subclass of " + base.getSimpleName());
		var def = (Marshaller) variant.getField("DEFAULT").get(null);
		assertEquals(variant, def.getClass(), () -> variant.getSimpleName() + ".DEFAULT must be an instance of " + variant.getSimpleName());
		assertSame(s, def.getSerializer(), () -> variant.getSimpleName() + ".DEFAULT must be wired to the variant serializer");
		assertSame(p, def.getParser(), () -> variant.getSimpleName() + ".DEFAULT must be wired to the variant parser");
	}

	@ParameterizedTest
	@MethodSource("variants")
	void a02_fullStaticSurfaceRedeclared(Class<? extends Marshaller> variant, Class<? extends Marshaller> base, @SuppressWarnings("unused") Serializer s, @SuppressWarnings("unused") Parser p) {
		for (var m : base.getDeclaredMethods()) {
			if (!Modifier.isStatic(m.getModifiers()) || !Modifier.isPublic(m.getModifiers()))
				continue;
			if (!SHORTCUT_NAMES.contains(m.getName()))
				continue;
			assertDoesNotThrow(() -> variant.getDeclaredMethod(m.getName(), m.getParameterTypes()),
				() -> variant.getSimpleName() + " must redeclare the full static surface of " + base.getSimpleName() + " (missing " + m + ")");
		}
	}

	@Test void b01_json5rReadableRoundTrip() throws Exception {
		var bean = Map.of("a", 1);
		var out = Json5R.of(bean);
		assertEquals(Json5Serializer.DEFAULT_READABLE.writeToString(bean), out);
		assertTrue(out.contains("\n"), () -> "Json5R output should be multi-line readable but was: " + out);
		var m = Json5R.to(out, Map.class);
		assertBean(m, "a", "1");
	}

	@Test void b02_inirReadableRoundTrip() throws Exception {
		var bean = Map.of("a", 1);
		var out = IniR.of(bean);
		assertEquals(IniSerializer.DEFAULT_READABLE.writeToString(bean), out);
		var m = IniR.to(out, Map.class);
		assertBean(m, "a", "1");
	}

	@Test void b03_hjsoncCompactRoundTrip() throws Exception {
		var bean = Map.of("a", 1);
		var out = HjsonC.of(bean);
		assertEquals(HjsonSerializer.DEFAULT_COMPACT.writeToString(bean), out);
		assertFalse(out.trim().contains("\n"), () -> "HjsonC output should be single-line compact but was: " + out);
		var m = HjsonC.to(out, Map.class);
		assertBean(m, "a", "1");
	}

	@Test void b04_jsonrReadableRoundTrip() throws Exception {
		var bean = Map.of("a", 1);
		var out = JsonR.of(bean);
		assertEquals(JsonSerializer.DEFAULT_READABLE.writeToString(bean), out);
		assertNotEquals(Json.of(bean), out, () -> "JsonR output should differ from the compact Json.DEFAULT output");
		assertTrue(out.contains("\n"), () -> "JsonR output should be multi-line readable but was: " + out);
		var m = JsonR.to(out, Map.class);
		assertBean(m, "a", "1");
	}

	@Test void b05_tomlrReadableRoundTrip() throws Exception {
		var bean = Map.of("a", 1);
		var out = TomlR.of(bean);
		assertEquals(TomlSerializer.DEFAULT_READABLE.writeToString(bean), out);
		var m = TomlR.to(out, Map.class);
		assertBean(m, "a", "1");
	}

	@Test void b06_yamlrReadableRoundTrip() throws Exception {
		var bean = Map.of("a", 1);
		var out = YamlR.of(bean);
		assertEquals(YamlSerializer.DEFAULT_READABLE.writeToString(bean), out);
		var m = YamlR.to(out, Map.class);
		assertBean(m, "a", "1");
	}

	@Test void b07_prototextrReadableRoundTrip() throws Exception {
		var bean = Map.of("a", 1);
		var out = PrototextR.of(bean);
		assertEquals(PrototextSerializer.DEFAULT_READABLE.writeToString(bean), out);
		var m = PrototextR.to(out, Map.class);
		assertBean(m, "a", "1");
	}

	@Test void b08_uonrReadableRoundTrip() throws Exception {
		var bean = Map.of("a", 1);
		var out = UonR.of(bean);
		assertEquals(UonSerializer.DEFAULT_READABLE.writeToString(bean), out);
		assertNotEquals(Uon.of(bean), out, () -> "UonR output should differ from the dense Uon.DEFAULT output");
		var m = UonR.to(out, Map.class);
		assertBean(m, "a", "1");
	}

	@Test void b09_uoneEncodingRoundTrip() throws Exception {
		var bean = Map.of("a", "b c");
		var out = UonE.of(bean);
		assertEquals(UonSerializer.DEFAULT_ENCODING.writeToString(bean), out);
		assertNotEquals(Uon.of(bean), out, () -> "UonE output should percent-encode reserved characters unlike Uon.DEFAULT");
		var m = UonE.to(out, Map.class);
		assertBean(m, "a", "b c");
	}

	@Test void b10_urlEncodingrReadableRoundTrip() throws Exception {
		var bean = Map.of("a", 1, "b", 2);
		var out = UrlEncodingR.of(bean);
		assertEquals(UrlEncodingSerializer.DEFAULT_READABLE.writeToString(bean), out);
		var m = UrlEncodingR.to(out, Map.class);
		assertBean(m, "a,b", "1,2");
	}

	@Test void b11_urlEncodingPlainRoundTrip() throws Exception {
		var bean = Map.of("a", "b c");
		var out = UrlEncodingPlain.of(bean);
		assertEquals(UrlEncodingSerializer.DEFAULT_PLAINTEXT.writeToString(bean), out);
		assertNotEquals(UrlEncoding.of(bean), out, () -> "UrlEncodingPlain output should differ from the percent-encoded UrlEncoding.DEFAULT output");
		var m = UrlEncodingPlain.to(out, Map.class);
		assertBean(m, "a", "b c");
	}

	@Test void b12_urlEncodingExpandedRoundTrip() throws Exception {
		var bean = Map.of("a", new String[]{"1", "2"});
		var out = UrlEncodingExpanded.of(bean);
		assertEquals(UrlEncodingSerializer.DEFAULT_EXPANDED.writeToString(bean), out);
		assertNotEquals(UrlEncoding.of(bean), out, () -> "UrlEncodingExpanded output should differ from the default (comma-joined) UrlEncoding.DEFAULT output");
		var m = UrlEncodingExpanded.to(out, Map.class);
		assertEquals(List.of("1", "2"), m.get("a"));
	}

	@Test void b13_hoconBracesRoundTrip() throws Exception {
		var bean = Map.of("a", 1);
		var out = HoconBraces.of(bean);
		assertEquals(HoconSerializer.DEFAULT_BRACES.writeToString(bean), out);
		assertNotEquals(Hocon.of(bean), out, () -> "HoconBraces output should include root braces unlike Hocon.DEFAULT");
		var m = HoconBraces.to(out, Map.class);
		assertBean(m, "a", "1");
	}

	@Test void b14_hoconcCompactRoundTrip() throws Exception {
		var bean = Map.of("a", 1);
		var out = HoconC.of(bean);
		assertEquals(HoconSerializer.DEFAULT_COMPACT.writeToString(bean), out);
		assertFalse(out.trim().contains("\n"), () -> "HoconC output should be single-line compact but was: " + out);
		var m = HoconC.to(out, Map.class);
		assertBean(m, "a", "1");
	}

	@Test void b15_xmlSqSingleQuoteRoundTrip() throws Exception {
		var bean = Map.of("a", 1);
		var out = XmlSq.of(bean);
		assertEquals(XmlSerializer.DEFAULT_SQ.writeToString(bean), out);
		assertTrue(out.contains("'"), () -> "XmlSq output should use single-quoted attributes but was: " + out);
		var m = XmlSq.to(out, Map.class);
		assertBean(m, "a", "1");
	}

	@Test void b16_xmlSqrReadableRoundTrip() throws Exception {
		var bean = Map.of("a", 1);
		var out = XmlSqR.of(bean);
		assertEquals(XmlSerializer.DEFAULT_SQ_READABLE.writeToString(bean), out);
		assertTrue(out.contains("\n"), () -> "XmlSqR output should be multi-line readable but was: " + out);
		var m = XmlSqR.to(out, Map.class);
		assertBean(m, "a", "1");
	}

	@Test void b17_xmlNsNamespaceRoundTrip() throws Exception {
		// Namespace declarations are only emitted for beans carrying namespace metadata (e.g. @Xml(prefix=...));
		// a plain Map has none, so this verifies wiring + round-trip rather than a visibly distinct form.
		var bean = Map.of("a", 1);
		var out = XmlNs.of(bean);
		assertEquals(XmlSerializer.DEFAULT_NS.writeToString(bean), out);
		var m = XmlNs.to(out, Map.class);
		assertBean(m, "a", "1");
	}

	@Test void b18_xmlNsSqNamespaceSingleQuoteRoundTrip() throws Exception {
		var bean = Map.of("a", 1);
		var out = XmlNsSq.of(bean);
		assertEquals(XmlSerializer.DEFAULT_NS_SQ.writeToString(bean), out);
		assertTrue(out.contains("'"), () -> "XmlNsSq output should use single-quoted attributes but was: " + out);
		var m = XmlNsSq.to(out, Map.class);
		assertBean(m, "a", "1");
	}

	@Test void b19_xmlNsSqrNamespaceSingleQuoteReadableRoundTrip() throws Exception {
		var bean = Map.of("a", 1);
		var out = XmlNsSqR.of(bean);
		assertEquals(XmlSerializer.DEFAULT_NS_SQ_READABLE.writeToString(bean), out);
		assertTrue(out.contains("\n") && out.contains("'"), () -> "XmlNsSqR output should be multi-line and single-quoted but was: " + out);
		var m = XmlNsSqR.to(out, Map.class);
		assertBean(m, "a", "1");
	}

	@Test void b20_htmlSqSingleQuoteRoundTrip() throws Exception {
		// Use a URI value so the serializer emits an <a href='...'> attribute to observe quoting style.
		var bean = Map.of("a", java.net.URI.create("http://example.com"));
		var out = HtmlSq.of(bean);
		assertEquals(HtmlSerializer.DEFAULT_SQ.writeToString(bean), out);
		assertTrue(out.contains("href='"), () -> "HtmlSq output should use single-quoted attributes but was: " + out);
		assertTrue(Html.of(bean).contains("href=\""), () -> "Html.DEFAULT output should use double-quoted attributes");
		var m = HtmlSq.to(out, Map.class);
		assertBean(m, "a", "http://example.com");
	}

	@Test void b21_htmlSqrReadableRoundTrip() throws Exception {
		var bean = Map.of("a", 1);
		var out = HtmlSqR.of(bean);
		assertEquals(HtmlSerializer.DEFAULT_SQ_READABLE.writeToString(bean), out);
		assertTrue(out.contains("\n"), () -> "HtmlSqR output should be multi-line readable but was: " + out);
		var m = HtmlSqR.to(out, Map.class);
		assertBean(m, "a", "1");
	}

	@Test void b22_htmlSimpleSqRoundTrip() throws Exception {
		// "Simple" (JSON type tags disabled) + single-quote attributes; for beans with no attributes to
		// quote and no type ambiguity to disambiguate, the rendered text matches Html.DEFAULT, so this
		// verifies wiring + round-trip rather than a visibly distinct form.
		var bean = Map.of("a", 1);
		var out = HtmlSimpleSq.of(bean);
		assertEquals(HtmlSerializer.DEFAULT_SIMPLE_SQ.writeToString(bean), out);
		var m = HtmlSimpleSq.to(out, Map.class);
		assertBean(m, "a", "1");
	}

	// NOTE (TODO-353): BsonSerializer/CborSerializer/MsgPackSerializer now honor the configured
	// BinaryFormat for byte[] output -- SPACED_HEX/BASE64 switch the byte[] wire representation from
	// each format's native binary opcode to that format's native string type containing the
	// spaced-hex/base64 text, so the SpacedHex/Base64 variant serializers produce visibly distinct
	// output from DEFAULT for a POJO with a byte[] field. These tests verify both the distinct wire
	// form and the round-trip back to the original bytes.
	//
	// The round-trip parse is done with an explicit Map<String,byte[]> type hint rather than the bare
	// Map.class used elsewhere in this file: once byte[] is on the wire as a string (SPACED_HEX/BASE64),
	// the string is indistinguishable from a genuine text value without a type hint telling the parser
	// the target is byte[] -- this is expected, since the encoding trades the format's native
	// self-describing binary tag for a text representation that needs the same external typing a text
	// serializer (e.g. JSON) would need to recover byte[] from a string.

	@Test void b23_bsonSpacedHexRoundTrip() throws Exception {
		var bean = Map.of("a", new byte[]{1, 2, 3});
		var out = BsonSpacedHex.of(bean);
		assertArrayEquals(BsonSerializer.DEFAULT_SPACED_HEX.write(bean), out);
		assertFalse(Arrays.equals(Bson.of(bean), out), "SpacedHex output should differ from the native binary output");
		Map<String,byte[]> m = BsonSpacedHex.to(out, Map.class, String.class, byte[].class);
		assertArrayEquals(new byte[]{1, 2, 3}, m.get("a"));
	}

	@Test void b24_bsonBase64RoundTrip() throws Exception {
		var bean = Map.of("a", new byte[]{1, 2, 3});
		var out = BsonBase64.of(bean);
		assertArrayEquals(BsonSerializer.DEFAULT_BASE64.write(bean), out);
		assertFalse(Arrays.equals(Bson.of(bean), out), "Base64 output should differ from the native binary output");
		Map<String,byte[]> m = BsonBase64.to(out, Map.class, String.class, byte[].class);
		assertArrayEquals(new byte[]{1, 2, 3}, m.get("a"));
	}

	@Test void b25_cborSpacedHexRoundTrip() throws Exception {
		var bean = Map.of("a", new byte[]{1, 2, 3});
		var out = CborSpacedHex.of(bean);
		assertArrayEquals(CborSerializer.DEFAULT_SPACED_HEX.write(bean), out);
		assertFalse(Arrays.equals(Cbor.of(bean), out), "SpacedHex output should differ from the native binary output");
		Map<String,byte[]> m = CborSpacedHex.to(out, Map.class, String.class, byte[].class);
		assertArrayEquals(new byte[]{1, 2, 3}, m.get("a"));
	}

	@Test void b26_cborBase64RoundTrip() throws Exception {
		var bean = Map.of("a", new byte[]{1, 2, 3});
		var out = CborBase64.of(bean);
		assertArrayEquals(CborSerializer.DEFAULT_BASE64.write(bean), out);
		assertFalse(Arrays.equals(Cbor.of(bean), out), "Base64 output should differ from the native binary output");
		Map<String,byte[]> m = CborBase64.to(out, Map.class, String.class, byte[].class);
		assertArrayEquals(new byte[]{1, 2, 3}, m.get("a"));
	}

	@Test void b27_cborNativeRoundTrip() throws Exception {
		// CborNative pairs the plain CborSerializer.DEFAULT with a native-mode parser: the serialized
		// bytes are identical to Cbor.DEFAULT (only the parser's token-level interpretation changes), so
		// this test verifies wiring + round-trip rather than a distinct serialized form.
		var bean = Map.of("a", 1);
		var out = CborNative.of(bean);
		assertArrayEquals(Cbor.of(bean), out);
		var m = CborNative.to(out, Map.class);
		assertBean(m, "a", "1");
	}

	@Test void b28_msgPackSpacedHexRoundTrip() throws Exception {
		var bean = Map.of("a", new byte[]{1, 2, 3});
		var out = MsgPackSpacedHex.of(bean);
		assertArrayEquals(MsgPackSerializer.DEFAULT_SPACED_HEX.write(bean), out);
		assertFalse(Arrays.equals(MsgPack.of(bean), out), "SpacedHex output should differ from the native binary output");
		Map<String,byte[]> m = MsgPackSpacedHex.to(out, Map.class, String.class, byte[].class);
		assertArrayEquals(new byte[]{1, 2, 3}, m.get("a"));
	}

	@Test void b29_msgPackBase64RoundTrip() throws Exception {
		var bean = Map.of("a", new byte[]{1, 2, 3});
		var out = MsgPackBase64.of(bean);
		assertArrayEquals(MsgPackSerializer.DEFAULT_BASE64.write(bean), out);
		assertFalse(Arrays.equals(MsgPack.of(bean), out), "Base64 output should differ from the native binary output");
		Map<String,byte[]> m = MsgPackBase64.to(out, Map.class, String.class, byte[].class);
		assertArrayEquals(new byte[]{1, 2, 3}, m.get("a"));
	}

	@Test void b30_msgPackNativeRoundTrip() throws Exception {
		// MsgPackNative pairs the plain MsgPackSerializer.DEFAULT with a native-mode parser: the
		// serialized bytes are identical to MsgPack.DEFAULT (only the parser's token-level interpretation
		// changes), so this test verifies wiring + round-trip rather than a distinct serialized form.
		var bean = Map.of("a", 1);
		var out = MsgPackNative.of(bean);
		assertArrayEquals(MsgPack.of(bean), out);
		var m = MsgPackNative.to(out, Map.class);
		assertBean(m, "a", "1");
	}

}
