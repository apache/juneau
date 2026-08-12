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
package org.apache.juneau.marshall.json5;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.json.*;
import org.junit.jupiter.api.*;

/**
 * Coverage-focused tests for {@link Json5SerializerSession} targeting {@code getJsonWriter}'s two dispatch arms:
 *  - the raw output is not yet a {@link JsonWriter} -&gt; a new one is created and installed on the pipe.
 *  - the raw output is already a {@link JsonWriter} (e.g. the caller wrote directly to one) -&gt; it's reused as-is.
 */
class Json5SerializerSession_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// a0x - getJsonWriter: raw output is a plain Writer -> new JsonWriter created (the common/default path).
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_getJsonWriter_plainWriter() throws Exception {
		var sw = new StringWriter();
		Json5Serializer.DEFAULT.write(map("a", 1), sw);
		assertEquals("{a:1}", sw.toString());
	}

	//------------------------------------------------------------------------------------------------------------------
	// b0x - getJsonWriter: raw output is already a JsonWriter -> reused directly, no wrapping/setWriter() call.
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_getJsonWriter_alreadyJsonWriter() throws Exception {
		var sw = new StringWriter();
		try (var jw = JsonWriter.create(sw, false, 100, false, '"', true, false, null)) {
			Json5Serializer.DEFAULT.write(map("a", 1), jw);
			assertEquals("{a:1}", sw.toString());
		}
	}

	private static Map<String,Object> map(String key, Object value) {
		var m = new LinkedHashMap<String,Object>();
		m.put(key, value);
		return m;
	}
}
