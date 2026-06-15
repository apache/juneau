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
package org.apache.juneau.marshall.stream;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.bson.*;
import org.apache.juneau.marshall.cbor.*;
import org.apache.juneau.marshall.csv.*;
import org.apache.juneau.marshall.hjson.*;
import org.apache.juneau.marshall.hocon.*;
import org.apache.juneau.marshall.html.*;
import org.apache.juneau.marshall.ini.*;
import org.apache.juneau.marshall.jcs.*;
import org.apache.juneau.marshall.json.*;
import org.apache.juneau.marshall.json5.*;
import org.apache.juneau.marshall.jsonl.*;
import org.apache.juneau.marshall.markdown.*;
import org.apache.juneau.marshall.msgpack.*;
import org.apache.juneau.marshall.oapi.*;
import org.apache.juneau.marshall.parquet.*;
import org.apache.juneau.marshall.parser.*;
import org.apache.juneau.marshall.plaintext.*;
import org.apache.juneau.marshall.proto.*;
import org.apache.juneau.marshall.serializer.*;
import org.apache.juneau.marshall.sse.*;
import org.apache.juneau.marshall.toml.*;
import org.apache.juneau.marshall.uon.*;
import org.apache.juneau.marshall.urlencoding.*;
import org.apache.juneau.marshall.xml.*;
import org.apache.juneau.marshall.yaml.*;
import org.junit.jupiter.api.*;

/**
 * Cross-format coverage for the record / array-record streaming-capability flags
 * ({@link RecordReadable#isRecordStreaming()}, {@link ArrayRecordReadable#isArrayRecordStreaming()}
 * and the writable equivalents) on every format's convenience parser/serializer class and on its
 * per-invocation session.  Asserts the convenience-class flag agrees with the session flag (the
 * TODO-175aa reader/writer-mirror invariant) so a drift between the two surfaces is caught.
 */
class FormatStreamingCapability_Test extends TestBase {

	private static List<Parser> parsers() {
		return List.of(
			JsonParser.DEFAULT, Json5Parser.DEFAULT, JsonlParser.DEFAULT,
			CborParser.DEFAULT, MsgPackParser.DEFAULT, BsonParser.DEFAULT,
			ProtoParser.DEFAULT, ParquetParser.DEFAULT, YamlParser.DEFAULT,
			TomlParser.DEFAULT, HoconParser.DEFAULT, HjsonParser.DEFAULT,
			CsvParser.DEFAULT, PlainTextParser.DEFAULT, MarkdownParser.DEFAULT,
			IniParser.DEFAULT, UonParser.DEFAULT, OpenApiParser.DEFAULT,
			UrlEncodingParser.DEFAULT, XmlParser.DEFAULT, HtmlParser.DEFAULT,
			SseParser.DEFAULT);
	}

	private static List<Serializer> serializers() {
		return List.of(
			JsonSerializer.DEFAULT, Json5Serializer.DEFAULT, JsonlSerializer.DEFAULT,
			CborSerializer.DEFAULT, MsgPackSerializer.DEFAULT, BsonSerializer.DEFAULT,
			ProtoSerializer.DEFAULT, ParquetSerializer.DEFAULT, YamlSerializer.DEFAULT,
			TomlSerializer.DEFAULT, HoconSerializer.DEFAULT, HjsonSerializer.DEFAULT,
			CsvSerializer.DEFAULT, PlainTextSerializer.DEFAULT, MarkdownSerializer.DEFAULT,
			IniSerializer.DEFAULT, UonSerializer.DEFAULT, OpenApiSerializer.DEFAULT,
			UrlEncodingSerializer.DEFAULT, XmlSerializer.DEFAULT, HtmlSerializer.DEFAULT,
			SseSerializer.DEFAULT, JsonSchemaSerializer.DEFAULT, JcsSerializer.DEFAULT);
	}

	@Test void a01_parserRecordStreamingFlagAgreesWithSession() {
		var checked = 0;
		for (var p : parsers()) {
			var session = p.getSession();
			if (p instanceof RecordReadable cp && session instanceof RecordReadable sp) {
				assertEquals(cp.isRecordStreaming(), sp.isRecordStreaming(),
					() -> p.getClass().getSimpleName() + ": record-streaming flag must agree between convenience class and session");
				checked++;
			}
		}
		assertTrue(checked >= 20, "expected most formats to expose a record-stream reader");
	}

	@Test void a02_parserArrayRecordStreamingFlagAgreesWithSession() {
		var checked = 0;
		for (var p : parsers()) {
			var session = p.getSession();
			if (p instanceof ArrayRecordReadable cp && session instanceof ArrayRecordReadable sp) {
				assertEquals(cp.isArrayRecordStreaming(), sp.isArrayRecordStreaming(),
					() -> p.getClass().getSimpleName() + ": array-record-streaming flag must agree between convenience class and session");
				checked++;
			}
		}
		assertTrue(checked > 0, "expected at least one format to expose an array-record reader");
	}

	@Test void a03_serializerRecordStreamingFlagAgreesWithSession() {
		var checked = 0;
		for (var s : serializers()) {
			var session = s.getSession();
			if (s instanceof RecordWritable cs && session instanceof RecordWritable ss) {
				assertEquals(cs.isRecordStreaming(), ss.isRecordStreaming(),
					() -> s.getClass().getSimpleName() + ": record-streaming flag must agree between convenience class and session");
				checked++;
			}
		}
		assertTrue(checked >= 20, "expected most formats to expose a record-stream writer");
	}

	@Test void a04_serializerArrayRecordStreamingFlagAgreesWithSession() {
		var checked = 0;
		for (var s : serializers()) {
			var session = s.getSession();
			if (s instanceof ArrayRecordWritable cs && session instanceof ArrayRecordWritable ss) {
				assertEquals(cs.isArrayRecordStreaming(), ss.isArrayRecordStreaming(),
					() -> s.getClass().getSimpleName() + ": array-record-streaming flag must agree between convenience class and session");
				checked++;
			}
		}
		assertTrue(checked > 0, "expected at least one format to expose an array-record writer");
	}
}
