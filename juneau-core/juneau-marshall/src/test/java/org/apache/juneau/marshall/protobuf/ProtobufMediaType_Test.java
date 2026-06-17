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
package org.apache.juneau.marshall.protobuf;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

// Note:  the marshaller is fully-qualified below to avoid clashing with the @Protobuf annotation in this package.

/**
 * Media-type + marshaller tests for the protobuf binary codec.
 */
class ProtobufMediaType_Test extends TestBase {

	@Test
	void i01_producesCorrectMediaType() {
		var ct = ProtobufSerializer.DEFAULT.getResponseContentType();
		assertEquals("application", ct.getType());
		assertEquals("protobuf", ct.getSubType());
	}

	@Test
	void i02_consumesBothTypes() {
		var types = ProtobufParser.DEFAULT.getMediaTypes().stream()
			.map(mt -> mt.getType() + "/" + mt.getSubType())
			.toList();
		assertTrue(types.contains("application/protobuf"), "Expected application/protobuf: " + types);
		assertTrue(types.contains("application/x-protobuf"), "Expected application/x-protobuf: " + types);
	}

	@Test
	void i03_noCollisionWithTextProto() {
		// Binary uses application/protobuf; text proto uses text/protobuf.  No overlap.
		var binary = ProtobufSerializer.DEFAULT.getResponseContentType();
		assertEquals("application/protobuf", binary.getType() + "/" + binary.getSubType());
	}

	@Test
	void j01_marshallerRoundTrip() throws Exception {
		var bytes = org.apache.juneau.marshall.marshaller.Protobuf.of(new ProtobufSerializer_Test.Simple(150, "testing"));
		var b = org.apache.juneau.marshall.marshaller.Protobuf.to(bytes, ProtobufSerializer_Test.Simple.class);
		assertEquals(150, b.id);
		assertEquals("testing", b.name);
	}

	@Test
	void j02_marshallerDefaultWriteRead() throws Exception {
		var bytes = org.apache.juneau.marshall.marshaller.Protobuf.DEFAULT.write(new ProtobufSerializer_Test.Simple(7, "x"));
		var b = org.apache.juneau.marshall.marshaller.Protobuf.DEFAULT.read(bytes, ProtobufSerializer_Test.Simple.class);
		assertEquals(7, b.id);
	}
}
