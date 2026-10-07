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
package org.apache.juneau.marshall.parser;

import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.stream.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.bean.*;
import org.apache.juneau.marshall.bson.*;
import org.apache.juneau.marshall.cbor.*;
import org.apache.juneau.marshall.csv.*;
import org.apache.juneau.marshall.hjson.*;
import org.apache.juneau.marshall.html.*;
import org.apache.juneau.marshall.ini.*;
import org.apache.juneau.marshall.markdown.*;
import org.apache.juneau.marshall.msgpack.*;
import org.apache.juneau.marshall.oapi.*;
import org.apache.juneau.marshall.protobuf.*;
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
 * Tests {@code @BeanProp(required)} enforcement across parser formats (WORK-J0585).
 */
@SuppressWarnings("unused") // Fixture fields are read reflectively.
class MissingRequiredProperty_Formats_Test extends TestBase {

	public static class A {
		@BeanProp(required=true) public String name;
		public int age;
	}

	/** Same shape as A, without the required property, used to produce input that lacks 'name'. */
	public static class ALoose { public int age = 7; }

	public static class AFull { public String name = "n"; public int age = 7; }

	public static class T {
		@BeanProp(required=true) public String name;
		@BeanProp(required=true) public List<String> tags;
	}

	public static class X {
		@BeanProp(required=true) public String name;
		@BeanProp(required=true) @Xml(format=XmlFormat.COLLAPSED, childName="tag") public List<String> tags;
	}

	static Stream<Arguments> formats() {
		return Stream.of(
			Arguments.of("xml",       XmlSerializer.DEFAULT,         XmlParser.DEFAULT),
			Arguments.of("urlenc",    UrlEncodingSerializer.DEFAULT, UrlEncodingParser.DEFAULT),
			Arguments.of("uon",       UonSerializer.DEFAULT,         UonParser.DEFAULT),
			Arguments.of("msgpack",   MsgPackSerializer.DEFAULT,     MsgPackParser.DEFAULT),
			Arguments.of("cbor",      CborSerializer.DEFAULT,        CborParser.DEFAULT),
			Arguments.of("bson",      BsonSerializer.DEFAULT,        BsonParser.DEFAULT),
			Arguments.of("yaml",      YamlSerializer.DEFAULT,        YamlParser.DEFAULT),
			Arguments.of("html",      HtmlSerializer.DEFAULT,        HtmlParser.DEFAULT),
			Arguments.of("toml",      TomlSerializer.DEFAULT,        TomlParser.DEFAULT),
			Arguments.of("hjson",     HjsonSerializer.DEFAULT,       HjsonParser.DEFAULT),
			Arguments.of("prototext", PrototextSerializer.DEFAULT,   PrototextParser.DEFAULT),
			Arguments.of("protobuf",  ProtobufSerializer.DEFAULT,    ProtobufParser.DEFAULT),
			Arguments.of("ini",       IniSerializer.DEFAULT,         IniParser.DEFAULT),
			Arguments.of("markdown",  MarkdownSerializer.DEFAULT,    MarkdownParser.DEFAULT)
		);
	}

	@ParameterizedTest(name="{0}")
	@MethodSource("formats")
	void a01_missingRequired_fails(String label, Serializer s, Parser p) throws Exception {
		Object in = s.write(new ALoose());
		var e = assertThrows(MissingRequiredPropertyException.class, () -> p.read(in, A.class), label);
		assertEquals(List.of("name"), e.getPropertyNames(), label);
	}

	@ParameterizedTest(name="{0}")
	@MethodSource("formats")
	void a02_positiveControl_roundTrips(String label, Serializer s, Parser p) throws Exception {
		var a = p.read(s.write(new AFull()), A.class);
		assertEquals("n", a.name, label);
	}

	@Test void b01_csv_listOfBeans() throws Exception {
		var in = CsvSerializer.DEFAULT.write(List.of(new ALoose()));
		assertThrows(MissingRequiredPropertyException.class, () -> CsvParser.DEFAULT.read(in, List.class, A.class));
	}

	@Test void b02_openApi() throws Exception {
		assertThrows(MissingRequiredPropertyException.class, () -> OpenApiParser.DEFAULT.read("age=7", A.class));
	}

	// Required collections populated through BeanPropertyMeta.add(...) (expanded-params URL encoding, collapsed XML):
	// the key must count as present.
	@Test void c01_urlEncoding_expandedParams_addPathRecordsPresence() throws Exception {
		var t = UrlEncodingParser.create().expandedParams().build().read("name=n&tags=a&tags=b", T.class);
		assertBean(t, "name,tags", "n,[a,b]");
	}

	@Test void c02_xml_collapsed_addPathRecordsPresence() throws Exception {
		var t = XmlParser.DEFAULT.read("<object><name>n</name><tag>a</tag><tag>b</tag></object>", X.class);
		assertBean(t, "name,tags", "n,[a,b]");
	}

	@Test void c03_xml_collapsed_missingNameStillFails() {
		var e = assertThrows(MissingRequiredPropertyException.class, () -> XmlParser.DEFAULT.read("<object><tag>a</tag></object>", X.class));
		assertEquals(List.of("name"), e.getPropertyNames());
	}
}
