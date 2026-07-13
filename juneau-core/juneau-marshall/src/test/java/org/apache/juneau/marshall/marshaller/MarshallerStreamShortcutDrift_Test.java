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

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.*;
import java.util.*;
import java.util.stream.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.stream.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

/**
 * Drift guard for the curated streaming static shortcuts (see the marshaller shortcuts design).
 *
 * <p>
 * Iterates every concrete marshaller class and asserts that the set of streaming static shortcuts
 * it <i>declares</i> ({@code toTokens}/{@code ofTokens}/{@code toRecords}/{@code ofRecords}/
 * {@code toArrayRecords}/{@code ofArrayRecords}) <i>exactly</i> matches the streaming role interfaces
 * that its {@link Marshaller#DEFAULT}-style default instance's parser/serializer actually implement.
 *
 * <p>
 * This both validates the hand-declared per-class matrix and prevents future drift when a format
 * adopts or drops a streaming role.  Capability is derived by reflection ({@code instanceof} on the
 * live parser/serializer) so it correctly accounts for inheritance.
 */
class MarshallerStreamShortcutDrift_Test extends TestBase {

	/** The six curated streaming static shortcut names. */
	private static final Set<String> STREAMING_SHORTCUTS = Set.of(
		"toTokens", "ofTokens", "toRecords", "ofRecords", "toArrayRecords", "ofArrayRecords");

	static List<Class<? extends Marshaller>> marshallerClasses() {
		return List.of(
			Json.class, Json5.class, Json5l.class, Jsonl.class, Xml.class, Html.class,
			Uon.class, UrlEncoding.class, OpenApi.class, MsgPack.class, Bson.class, Cbor.class,
			Protobuf.class, Prototext.class, Parquet.class, Csv.class, Toml.class, Yaml.class,
			Hjson.class, Hocon.class, Jcs.class, Ini.class, Markdown.class, MarkdownDoc.class,
			Sse.class, PlainText.class);
	}

	@ParameterizedTest
	@MethodSource("marshallerClasses")
	void a01_streamingStaticsMatchDefaultRoles(Class<? extends Marshaller> c) throws Exception {
		var def = (Marshaller) c.getField("DEFAULT").get(null);
		var parser = def.getParser();
		var serializer = def.getSerializer();

		var expected = new TreeSet<String>();
		if (parser instanceof TokenReadable)
			expected.add("toTokens");
		if (serializer instanceof TokenWritable)
			expected.add("ofTokens");
		if (parser instanceof RecordReadable)
			expected.add("toRecords");
		if (serializer instanceof RecordWritable)
			expected.add("ofRecords");
		if (parser instanceof ArrayRecordReadable)
			expected.add("toArrayRecords");
		if (serializer instanceof ArrayRecordWritable)
			expected.add("ofArrayRecords");

		var actual = Arrays.stream(c.getDeclaredMethods())
			.filter(m -> Modifier.isStatic(m.getModifiers()))
			.map(Method::getName)
			.filter(STREAMING_SHORTCUTS::contains)
			.collect(Collectors.toCollection(TreeSet::new));

		assertEquals(expected, actual,
			() -> c.getSimpleName() + ": declared streaming static shortcuts must exactly match the roles implemented by its DEFAULT parser/serializer");
	}
}
