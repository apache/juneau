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

import static org.apache.juneau.junit.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.protobuf.ProtobufSerializer_Test.*;
import org.junit.jupiter.api.*;

/**
 * Tests for {@link ProtobufParser} scalar parsing + unknown-field skip.
 */
class ProtobufParser_Test extends TestBase {

	private static byte[] bytes(int...v) {
		var b = new byte[v.length];
		for (var i = 0; i < v.length; i++)
			b[i] = (byte)v[i];
		return b;
	}

	@Test
	void a01_parseIntAndString() throws Exception {
		var b = ProtobufParser.DEFAULT.parse(bytes(0x08,0x96,0x01, 0x12,0x07,0x74,0x65,0x73,0x74,0x69,0x6E,0x67), Simple.class);
		assertBean(b, "id,name", "150,testing");
	}

	@Test
	void a02_unknownFieldSkipped() throws Exception {
		// field9=42 (varint, unknown -> skipped), then field1=150.
		var b = ProtobufParser.DEFAULT.parse(bytes(0x48,0x2A, 0x08,0x96,0x01), Simple.class);
		assertBean(b, "id,name", "150,<null>");
	}

	@Test
	void a03_absentFieldsLeftDefault() throws Exception {
		var b = ProtobufParser.DEFAULT.parse(bytes(0x12,0x01,0x61), Simple.class);
		assertBean(b, "id,name", "0,a");
	}

	@Test
	void a04_enumParse() throws Exception {
		var b = ProtobufParser.DEFAULT.parse(bytes(0x08,0x02), WithEnum.class);
		assertBean(b, "color", "BLUE");
	}

	@Test
	void a05_emptyInputYieldsEmptyBean() throws Exception {
		var b = ProtobufParser.DEFAULT.parse(new byte[0], Simple.class);
		assertBean(b, "id,name", "0,<null>");
	}

	@Test
	void a06_nonBeanTargetThrows() {
		assertThrows(Exception.class, () -> ProtobufParser.DEFAULT.parse(bytes(0x08,0x01), String.class));
	}
}
