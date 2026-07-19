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
package org.apache.juneau.marshall.msgpack;

import static org.apache.juneau.commons.utils.StringUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Media type tests for MessagePack.
 *
 * <p>
 * Verifies that the serializer emits the RFC-standard <c>application/msgpack</c> Content-Type and
 * that the parser accepts both the new <c>application/msgpack</c> value and the legacy
 * <c>octal/msgpack</c> alias (backward compatibility).
 */
class MsgPackMediaType_Test extends TestBase {

	@Test
	void i01_producesCorrectMediaType() {
		var ct = MsgPackSerializer.DEFAULT.getResponseContentType();
		assertEquals("application", ct.getType());
		assertEquals("msgpack", ct.getSubType());
		assertEquals("application/msgpack", ct.toString());
	}

	@Test
	void i02_consumesApplicationMsgpack() {
		var types = MsgPackParser.DEFAULT.getMediaTypes().stream()
			.map(mt -> mt.getType() + "/" + mt.getSubType())
			.toList();
		assertTrue(types.contains("application/msgpack"),
			"Expected parser to consume application/msgpack: " + types);
		assertTrue(MsgPackParser.DEFAULT.canHandle("application/msgpack"),
			"Expected canHandle(application/msgpack) to be true");
	}

	@Test
	void i03_consumesOctalMsgpackAlias() {
		var types = MsgPackParser.DEFAULT.getMediaTypes().stream()
			.map(mt -> mt.getType() + "/" + mt.getSubType())
			.toList();
		assertTrue(types.contains("octal/msgpack"),
			"Expected parser to retain octal/msgpack as a backward-compat alias: " + types);
		assertTrue(MsgPackParser.DEFAULT.canHandle("octal/msgpack"),
			"Expected canHandle(octal/msgpack) to be true");
	}

	@Test
	void i04_serializerAcceptsBothMediaTypes() {
		var types = new ArrayList<String>();
		MsgPackSerializer.DEFAULT.forEachAcceptMediaType(mt -> types.add(mt.getType() + "/" + mt.getSubType()));
		assertTrue(types.contains("application/msgpack"),
			"Expected serializer Accept list to contain application/msgpack: " + types);
		assertTrue(types.contains("octal/msgpack"),
			"Expected serializer Accept list to retain octal/msgpack alias: " + types);
	}

	@Test
	void i05_primaryMediaTypeIsApplicationMsgpack() {
		assertEquals("application/msgpack", MsgPackSerializer.DEFAULT.getPrimaryMediaType().toString());
		assertEquals("application/msgpack", MsgPackParser.DEFAULT.getPrimaryMediaType().toString());
	}

	@Test
	void i06_parserStillDecodesWireData() throws Exception {
		var bytes = fromSpacedHex("A5 68 65 6C 6C 6F");
		var s = MsgPackParser.DEFAULT.read(new ByteArrayInputStream(bytes), String.class);
		assertEquals("hello", s);
	}
}
