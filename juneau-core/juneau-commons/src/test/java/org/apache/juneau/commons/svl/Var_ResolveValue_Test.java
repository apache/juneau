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

import org.apache.juneau.commons.TestBase;
import org.junit.jupiter.api.*;

/**
 * Tests {@link Var#resolveValue(VarResolverSession, String)} and its defaulting/multipart variants.
 */
class Var_ResolveValue_Test extends TestBase {

	public static class TypedVar extends SimpleVar {
		public TypedVar() {
			super("TV");
		}

		@Override
		public String resolve(VarResolverSession s, String arg) {
			return "str:" + arg;
		}

		@Override
		protected Object resolveValue(VarResolverSession s, String arg) {
			return arg.isEmpty() ? null : Integer.valueOf(arg);
		}
	}

	public static class Streamed extends StreamedVar {
		public Streamed() {
			super("ST");
		}

		@Override
		public void resolveTo(VarResolverSession s, java.io.Writer w, String arg) throws Exception {
			w.write("<" + arg + ">");
		}
	}

	public static class Dflt extends DefaultingVar {
		public Dflt() {
			super("DF");
		}

		@Override
		public String resolve(VarResolverSession s, String arg) {
			return "str:" + arg;
		}

		@Override
		protected Object resolveValue(VarResolverSession s, String arg) {
			return "k".equals(arg) ? null : "val:" + arg;
		}
	}

	public static class Multi extends MultipartResolvingVar {
		public Multi() {
			super("MP");
		}

		@Override
		public String resolve(VarResolverSession s, String arg) {
			return "str:" + arg;
		}

		@Override
		protected Object resolveValue(VarResolverSession s, String arg) {
			return "b".equals(arg) ? Integer.valueOf(5) : null;
		}
	}

	private static VarResolverSession session() {
		return VarResolver.DEFAULT.createSession();
	}

	@Test void a01_defaultDelegatesToResolve() throws Exception {
		var v = new SimpleVar("X") {
			@Override
			public String resolve(VarResolverSession s, String a) {
				return a + "!";
			}
		};
		assertEquals("a!", v.resolveValue(session(), "a"));
	}

	@Test void a02_overrideReturnsTyped() throws Exception {
		assertEquals(5, new TypedVar().resolveValue(session(), "5"));
	}

	@Test void a03_nullMeansAbsent() throws Exception {
		assertNull(new TypedVar().resolveValue(session(), ""));
	}

	@Test void a04_streamedVarBuffers() throws Exception {
		assertEquals("<q>", new Streamed().resolveValue(session(), "q"));
	}

	@Test void a05_defaultingVarAppliesDefault() throws Exception {
		assertEquals("dflt", new Dflt().doResolveValue(session(), "k,dflt"));
		assertEquals("val:z", new Dflt().doResolveValue(session(), "z,dflt"));
	}

	@Test void a06_multipartResolvingVarFirstNonNull() throws Exception {
		assertEquals(5, new Multi().doResolveValue(session(), "a,b,c"));
		assertNull(new Multi().doResolveValue(session(), "a,c"));
	}
}
