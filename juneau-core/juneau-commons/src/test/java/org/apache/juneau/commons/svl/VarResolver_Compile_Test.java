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
package org.apache.juneau.commons.svl;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.commons.*;
import org.junit.jupiter.api.*;

/**
 * Tests {@link VarResolver#compile(String)} and {@link VarTemplate} basic resolve semantics.
 *
 * <p>
 * Phase A acceptance: the new API exists and behaves identically to the existing
 * {@link VarResolver#resolve(String)} path. No perf change yet (Phase B installs the cached-segment
 * implementation that backs the perf win).
 */
class VarResolver_Compile_Test extends TestBase {

	private static VarResolver vr() {
		return VarResolver.create().vars(EchoVar.class).build();
	}

	public static class EchoVar extends SimpleVar {
		public EchoVar() {
			super("E");
		}
		@Override
		public String resolve(VarResolverSession session, String arg) {
			return "[" + arg + "]";
		}
	}

	@Test void a01_compileLiteral() {
		var vr = vr();
		var tpl = vr.compile("hello world");
		assertEquals("hello world", tpl.resolve(vr.createSession()));
		assertTrue(tpl.isLiteral());
	}

	@Test void a02_compileLiteralNullAndEmpty() {
		var vr = vr();
		var t1 = vr.compile(null);
		assertNull(t1.resolve(vr.createSession()));
		assertTrue(t1.isLiteral());

		var t2 = vr.compile("");
		assertEquals("", t2.resolve(vr.createSession()));
		assertTrue(t2.isLiteral());
	}

	@Test void a03_compileWithSimpleVar() {
		var vr = vr();
		var tpl = vr.compile("$E{x}");
		assertEquals("[x]", tpl.resolve(vr.createSession()));
		assertFalse(tpl.isLiteral());
	}

	@Test void a04_compileWithDefault() {
		var vr = vr();
		var tpl = vr.compile("$E{x}-$E{y}");
		assertEquals("[x]-[y]", tpl.resolve(vr.createSession()));
		assertFalse(tpl.isLiteral());
	}

	@Test void a05_compileNested() {
		var vr = vr();
		var tpl = vr.compile("$E{$E{x}}");
		assertEquals("[[x]]", tpl.resolve(vr.createSession()));
	}

	@Test void a06_compileEscape() {
		var vr = vr();
		// Backslash-dollar should escape the $; output is literal "$E{x}".
		var tpl = vr.compile("\\$E{x}");
		assertEquals("$E{x}", tpl.resolve(vr.createSession()));
		// After Phase B's segment-aware compile, the escape is interpreted at compile time and
		// the template reduces to a single LiteralSegment("$E{x}") — isLiteral() correctly
		// reflects this. The Phase A naive scan returned false here because backslash was
		// present in the source; the Phase B segment-aware answer is the more useful one.
		assertTrue(tpl.isLiteral());
	}

	@Test void a07_compileMultipleResolves() {
		var vr = vr();
		var tpl = vr.compile("$E{a}$E{b}");
		var s = vr.createSession();
		assertEquals("[a][b]", tpl.resolve(s));
		assertEquals("[a][b]", tpl.resolve(s));
		assertEquals("[a][b]", tpl.resolve(s));
	}

	@Test void a08_getSourceReturnsRawInput() {
		var vr = vr();
		var tpl = vr.compile("$E{a}");
		assertEquals("$E{a}", tpl.getSource());
	}

	@Test void a09_compileViaSession() {
		var vr = vr();
		var s = vr.createSession();
		var tpl = s.compile("$E{x}");
		assertEquals("[x]", tpl.resolve(s));
	}

	@Test void a10_isLiteralFalseForDollarBraceShortcut() {
		var vr = VarResolver.create().defaultVars().build();
		var tpl = vr.compile("${user.home}");
		assertFalse(tpl.isLiteral());
	}

	@Test void a11_isLiteralFalseForHashBrace() {
		var vr = vr();
		var tpl = vr.compile("#{upper(foo)}");
		assertFalse(tpl.isLiteral());
	}

	@Test void a12_toStringIncludesSource() {
		var vr = vr();
		var tpl = vr.compile("hello");
		assertEquals("VarTemplate[hello]", tpl.toString());
	}
}
