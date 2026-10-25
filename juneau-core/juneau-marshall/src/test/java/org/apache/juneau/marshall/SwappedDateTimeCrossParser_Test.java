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
package org.apache.juneau.marshall;

import static org.junit.jupiter.api.Assertions.*;

import java.time.*;
import java.util.*;
import java.util.stream.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.bson.*;
import org.apache.juneau.marshall.cbor.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.marshall.csv.*;
import org.apache.juneau.marshall.hjson.*;
import org.apache.juneau.marshall.hocon.*;
import org.apache.juneau.marshall.html.*;
import org.apache.juneau.commons.httppart.*;
import org.apache.juneau.marshall.httppart.*;
import org.apache.juneau.marshall.ini.*;
import org.apache.juneau.marshall.json.*;
import org.apache.juneau.marshall.json5.*;
import org.apache.juneau.marshall.json5l.*;
import org.apache.juneau.marshall.jsonl.*;
import org.apache.juneau.marshall.markdown.*;
import org.apache.juneau.marshall.msgpack.*;
import org.apache.juneau.marshall.oapi.*;
import org.apache.juneau.marshall.parquet.*;
import org.apache.juneau.marshall.parser.*;
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
 * Cross-parser check that bean properties of default-swapped date/time types parse back into typed beans and maps.
 *
 * <p>
 * Guards against parsers handing an already-typed value to a swapped bean property, which previously failed on
 * the swap-aware unswap path.
 */
class SwappedDateTimeCrossParser_Test extends TestBase {

	public static class A_Bean {
		public Calendar f1;
		public Date f2;
		public ZonedDateTime f3;
		public Instant f4;
		public LocalDate f5;
		public LocalDateTime f6;
		public OffsetDateTime f7;
		public LocalTime f8;
		public Duration f9;
		public Period f10;
	}

	private static A_Bean bean() {
		var b = new A_Bean();
		var c = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
		c.setTimeInMillis(1577934245000L);
		b.f1 = c;
		b.f2 = new Date(1577934245000L);
		b.f3 = ZonedDateTime.of(2020, 1, 2, 3, 4, 5, 0, ZoneOffset.UTC);
		b.f4 = Instant.ofEpochSecond(1577934245L);
		b.f5 = LocalDate.of(2020, 1, 2);
		b.f6 = LocalDateTime.of(2020, 1, 2, 3, 4, 5);
		b.f7 = OffsetDateTime.of(2020, 1, 2, 3, 4, 5, 0, ZoneOffset.UTC);
		b.f8 = LocalTime.of(3, 4, 5);
		b.f9 = Duration.ofSeconds(3661);
		b.f10 = Period.of(1, 2, 3);
		return b;
	}

	record Pair(String name, Serializer s, Parser p) {
		@Override public String toString() { return name; }
	}

	static Stream<Pair> pairs() {
		return Stream.of(
			new Pair("json", JsonSerializer.DEFAULT, JsonParser.DEFAULT),
			new Pair("json5", Json5Serializer.DEFAULT, Json5Parser.DEFAULT),
			new Pair("json5l", Json5lSerializer.DEFAULT, Json5lParser.DEFAULT),
			new Pair("jsonl", JsonlSerializer.DEFAULT, JsonlParser.DEFAULT),
			new Pair("hjson", HjsonSerializer.DEFAULT, HjsonParser.DEFAULT),
			new Pair("hocon", HoconSerializer.DEFAULT, HoconParser.DEFAULT),
			new Pair("yaml", YamlSerializer.DEFAULT, YamlParser.DEFAULT),
			new Pair("toml", TomlSerializer.DEFAULT, TomlParser.DEFAULT),
			new Pair("ini", IniSerializer.DEFAULT, IniParser.DEFAULT),
			new Pair("xml", XmlSerializer.DEFAULT, XmlParser.DEFAULT),
			new Pair("html", HtmlSerializer.DEFAULT, HtmlParser.DEFAULT),
			new Pair("markdown", MarkdownSerializer.DEFAULT, MarkdownParser.DEFAULT),
			new Pair("uon", UonSerializer.DEFAULT, UonParser.DEFAULT),
			new Pair("urlencoding", UrlEncodingSerializer.DEFAULT, UrlEncodingParser.DEFAULT),
			new Pair("openapi", OpenApiSerializer.DEFAULT, OpenApiParser.DEFAULT),
			new Pair("csv", CsvSerializer.DEFAULT, CsvParser.DEFAULT),
			new Pair("prototext", PrototextSerializer.DEFAULT, PrototextParser.DEFAULT),
			new Pair("protobuf", ProtobufSerializer.DEFAULT, ProtobufParser.DEFAULT),
			new Pair("msgpack", MsgPackSerializer.DEFAULT, MsgPackParser.DEFAULT),
			new Pair("cbor", CborSerializer.DEFAULT, CborParser.DEFAULT),
			new Pair("bson", BsonSerializer.DEFAULT, BsonParser.DEFAULT),
			new Pair("parquet", ParquetSerializer.DEFAULT, ParquetParser.DEFAULT)
		);
	}

	@ParameterizedTest
	@MethodSource("pairs")
	void a01_typedBeanRoundTrip(Pair pr) throws Exception {
		var in = bean();
		var out = pr.p().read(pr.s().write(in), A_Bean.class);
		assertEquals(in.f1.getTimeInMillis(), out.f1.getTimeInMillis());
		assertEquals(in.f2, out.f2);
		assertEquals(in.f3.toInstant(), out.f3.toInstant());
		assertEquals(in.f4, out.f4);
		assertEquals(in.f5, out.f5);
		assertEquals(in.f6, out.f6);
		assertEquals(in.f7.toInstant(), out.f7.toInstant());
		assertEquals(in.f8, out.f8);
		assertEquals(in.f9, out.f9);
		assertEquals(in.f10, out.f10);
	}

	@ParameterizedTest
	@MethodSource("pairs")
	void a02_mapTarget(Pair pr) throws Exception {
		// Protobuf binary parsing is schema-driven and requires a bean target by design.
		Assumptions.assumeFalse("protobuf".equals(pr.name()));
		var out = pr.p().read(pr.s().write(bean()), JsonMap.class);
		assertNotNull(out);
		assertEquals(10, out.size());
		assertNotNull(out.get("f1"));
		assertNotNull(out.get("f10"));
	}

	public static class B_Bean {
		public Calendar f1;
		public Date f2;
		public Instant f3;
		public LocalDate f4;
		public LocalDateTime f5;
		public Duration f6;
		public Period f7;
	}

	private static HttpPartSchema bSchema(HttpPartFormat f) {
		var s = HttpPartSchema.tString().format(f);
		return HttpPartSchema.tObject().p("f1", s).p("f2", s).p("f3", s).p("f4", s).p("f5", s).p("f6", HttpPartSchema.tString()).p("f7", HttpPartSchema.tString()).build();
	}

	@Test
	void b01_openApiDateTimeFormat() throws Exception {
		var r = OpenApiParser.DEFAULT.read(null, bSchema(HttpPartFormat.DATE_TIME),
			"f1=2020-01-02T03:04:05Z,f2=2020-01-02T03:04:05Z,f3=2020-01-02T03:04:05Z,f4=2020-01-02T03:04:05Z,f5=2020-01-02T03:04:05Z,f6=PT1H,f7=P1Y2M3D", B_Bean.class);
		assertEquals(1577934245000L, r.f1.getTimeInMillis());
		assertEquals(1577934245000L, r.f2.getTime());
		assertEquals(Instant.ofEpochSecond(1577934245L), r.f3);
		assertEquals(2020, r.f5.getYear());
		assertEquals(Duration.ofHours(1), r.f6);
		assertEquals(Period.of(1, 2, 3), r.f7);
	}

	@Test
	void b02_openApiDateFormat() throws Exception {
		var r = OpenApiParser.DEFAULT.read(null, bSchema(HttpPartFormat.DATE),
			"f1=2020-01-02,f2=2020-01-02,f3=2020-01-02,f4=2020-01-02,f5=2020-01-02", B_Bean.class);
		assertEquals(2020, r.f1.get(Calendar.YEAR));
		assertNotNull(r.f2);
		assertNotNull(r.f3);
		assertEquals(LocalDate.of(2020, 1, 2), r.f4);
		assertEquals(2, r.f5.getDayOfMonth());
	}
}
