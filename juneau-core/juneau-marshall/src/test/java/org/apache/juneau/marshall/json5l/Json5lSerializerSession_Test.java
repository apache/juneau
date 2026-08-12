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
package org.apache.juneau.marshall.json5l;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.json.*;
import org.junit.jupiter.api.*;

/**
 * Coverage tests for {@link Json5lSerializerSession}.
 */
class Json5lSerializerSession_Test extends TestBase {

	//====================================================================================================
	// a. getJsonWriter(SerializerPipe): raw-output-already-a-JsonWriter reuse arm.
	//====================================================================================================

	@Test void a01_getJsonWriter_rawOutputAlreadyJsonWriter_reused() throws Exception {
		var sw = new StringWriter();
		// A caller-supplied JsonWriter passed directly as the output target is reused as-is (its
		// getRawOutput() identity check short-circuits building a brand-new BasicJsonWriter).
		try (var preBuilt = JsonWriter.create(sw, false, 0, false, '"', true, false, null)) {
			var s = Json5lSerializer.create().json5Sugar().build().createSession().build();
			s.write("hello", preBuilt);
			assertTrue(sw.toString().contains("hello"), () -> "Expected 'hello' in: " + sw);
		}
	}

	@Test void a02_getJsonWriter_rawOutputNotJsonWriter_createsNew() throws Exception {
		var s = Json5lSerializer.create().json5Sugar().build();
		var json = s.write("hello");
		assertTrue(json.contains("hello"), () -> "Expected 'hello' in: " + json);
	}
}
