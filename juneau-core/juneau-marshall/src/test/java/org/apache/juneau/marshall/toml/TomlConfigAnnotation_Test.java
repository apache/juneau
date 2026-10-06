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
package org.apache.juneau.marshall.toml;

import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.reflect.*;
import org.apache.juneau.commons.svl.*;
import org.apache.juneau.marshall.*;
import org.junit.jupiter.api.*;

/**
 * Tests the properties of the {@link TomlConfig @TomlConfig} annotation (WORK-J0577).
 */
class TomlConfigAnnotation_Test extends TestBase {

	static VarResolverSession sr = VarResolver.create().vars(XVar.class).build().createSession();

	private static AnnotationWorkList work(Class<?> c) {
		return AnnotationWorkList.of(sr, rstream(ClassInfo.of(c).getAnnotations()));
	}

	private static Map<String,Object> map() {
		var m = new LinkedHashMap<String,Object>();
		m.put("b", 1);
		m.put("a", null);
		return m;
	}

	@TomlConfig(nullValue="~", sortKeys="true")
	static class A {}

	@Test void a01_serializer_nullValue() throws Exception {
		var s = TomlSerializer.create().keepNullProperties().apply(work(A.class)).build();
		assertTrue(s.write(map()).contains("a = \"~\""));
	}

	public static class Zed { public int z = 1; public int a = 2; }

	@Test void a01b_sortKeys() throws Exception {
		var toml = TomlSerializer.create().apply(work(A.class)).build().write(new Zed());
		assertTrue(toml.indexOf("a = 2") < toml.indexOf("z = 1"), toml);
	}

	@Test void a02_parser_nullValue() throws Exception {
		var s = TomlSerializer.create().keepNullProperties().apply(work(A.class)).build();
		var p = TomlParser.create().apply(work(A.class)).build();
		var m = p.read(s.write(map()), Map.class);
		assertTrue(m.containsKey("a"));
		assertNull(m.get("a"));
		assertBean(m, "b", "1");
	}

	public static class Inner { public String s = "x"; public int i = 1; }
	public static class Outer { public Inner inner = new Inner(); }

	@TomlConfig(useInlineTables="false")
	static class B {}

	@Test void b01_useInlineTables_false() throws Exception {
		var toml = TomlSerializer.create().apply(work(B.class)).build().write(new Outer());
		assertFalse(toml.contains("inner = {"), toml);
	}

	@TomlConfig(inlineTableThreshold="1")
	static class C {}

	@Test void b02_inlineTableThreshold() throws Exception {
		var toml = TomlSerializer.create().apply(work(C.class)).build().write(new Outer());
		assertFalse(toml.contains("inner = {"), toml);
		assertTrue(TomlSerializer.create().build().write(new Outer()).contains("inner = {"));
	}

	@TomlConfig(nullValue="$X{~}", sortKeys="$X{true}", useInlineTables="$X{false}", inlineTableThreshold="$X{1}")
	static class D {}

	@Test void c01_varResolved() throws Exception {
		var s = TomlSerializer.create().keepNullProperties().apply(work(D.class)).build();
		assertTrue(s.write(map()).contains("a = \"~\""));
		var toml = s.write(new Zed());
		assertTrue(toml.indexOf("a = 2") < toml.indexOf("z = 1"), toml);
		assertFalse(s.write(new Outer()).contains("inner = {"));
	}

	@TomlConfig
	static class E {}

	@Test void d01_defaultsUnchanged() throws Exception {
		var s = TomlSerializer.create().apply(work(E.class)).build();
		assertEquals(TomlSerializer.DEFAULT.write(map()), s.write(map()));
		assertEquals(TomlSerializer.DEFAULT.write(new Outer()), s.write(new Outer()));
	}

	@Test void d02_badThreshold_throws() {
		@TomlConfig(inlineTableThreshold="abc") class F {}
		var work = work(F.class);
		var builder = TomlSerializer.create();
		assertThrows(RuntimeException.class, () -> builder.apply(work));
	}
}
