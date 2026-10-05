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
package org.apache.juneau.marshall.json;

import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.reflect.*;
import org.apache.juneau.commons.svl.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.jcs.*;
import org.apache.juneau.marshall.json5.*;
import org.apache.juneau.marshall.json5l.*;
import org.apache.juneau.marshall.jsonl.*;
import org.junit.jupiter.api.*;

/**
 * Verifies that the JSON-derived formats (JSON5, JSONL, JSON5L, JCS) share the {@link JsonConfig @JsonConfig} and
 * {@link Json @Json} annotations with {@link JsonSerializer} (WORK-J0577), rather than needing their own annotation set.
 */
class JsonFamilyAnnotations_Test extends TestBase {

	static VarResolverSession sr = VarResolver.create().vars(XVar.class).build().createSession();

	@JsonConfig(escapeSolidus="true")
	static class Cfg {}
	static ClassInfo cfg = ClassInfo.of(Cfg.class);

	private static AnnotationWorkList work() {
		return AnnotationWorkList.of(sr, rstream(cfg.getAnnotations()));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// @JsonConfig applied through the builder's annotation support.
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a01_json5_jsonConfig() throws Exception {
		assertEquals("'a\\/b'", Json5Serializer.create().apply(work()).build().write("a/b"));
	}

	@Test void a02_jsonl_jsonConfig() throws Exception {
		assertEquals("\"a\\/b\"\n", JsonlSerializer.create().apply(work()).build().write("a/b"));
	}

	@Test void a03_json5l_jsonConfig() throws Exception {
		assertEquals("\"a\\/b\"\n", Json5lSerializer.create().apply(work()).build().write("a/b"));
	}

	@Test void a04_jcs_jsonConfig() throws Exception {
		// RFC 8785 canonical output never escapes '/', so escapeSolidus is accepted but intentionally has no effect.
		assertEquals("\"a/b\"", JcsSerializer.create().apply(work()).build().write("a/b"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// @Json (class-level) honored by the derived serializers.
	//-----------------------------------------------------------------------------------------------------------------

	@Json(wrapperAttr="w")
	public static class Wrapped {
		public int a = 1;
	}

	@Test void b01_json5_classLevelJson() throws Exception {
		assertEquals("{w:{a:1}}", Json5Serializer.DEFAULT.write(new Wrapped()));
	}

	@Test void b02_json5l_classLevelJson() throws Exception {
		assertEquals("{\"w\":{\"a\":1}}\n", Json5lSerializer.DEFAULT.write(new Wrapped()));
	}

	@Test void b03_jsonl_classLevelJson() throws Exception {
		assertEquals("{\"w\":{\"a\":1}}\n", JsonlSerializer.DEFAULT.write(new Wrapped()));
	}

	@Test void b04_jcs_classLevelJson() throws Exception {
		assertEquals("{\"w\":{\"a\":1}}", JcsSerializer.DEFAULT.write(new Wrapped()));
	}
}
