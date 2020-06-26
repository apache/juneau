// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau.utils;

import static org.apache.juneau.assertions.Assertions.*;
import static org.apache.juneau.internal.StringUtils.*;
import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.*;

import org.apache.juneau.svl.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class StringVarResolverTest {

	//====================================================================================================
	// test - Basic tests
	//====================================================================================================
	@Test
	public void test() throws Exception {
		VarResolver vr = new VarResolverBuilder().vars(XVar.class).build();
		String t;

		t = null;
		assertEquals(null, vr.resolve(t));

		t = "";
		assertEquals("", vr.resolve(t));

		t = "foo";
		assertEquals("foo", vr.resolve(t));

		t = "$X{y}";
		assertEquals("xyx", vr.resolve(t));

		t = "$X{y}x";
		assertEquals("xyxx", vr.resolve(t));

		t = "x$X{y}";
		assertEquals("xxyx", vr.resolve(t));

		t = "$X{y}$X{y}";
		assertEquals("xyxxyx", vr.resolve(t));

		t = "z$X{y}z$X{y}z";
		assertEquals("zxyxzxyxz", vr.resolve(t));

		t = "$X{$X{y}}";
		assertEquals("xxyxx", vr.resolve(t));

		t = "$X{z$X{y}z}";
		assertEquals("xzxyxzx", vr.resolve(t));

		t = "$X.{y}";
		assertEquals("$X.{y}", vr.resolve(t));

		t = "z$X.{y}z";
		assertEquals("z$X.{y}z", vr.resolve(t));

		t = "z$X.{$X.{z}}z";
		assertEquals("z$X.{$X.{z}}z", vr.resolve(t));

		// Non-existent vars
		t = "$Y{y}";
		assertEquals("$Y{y}", vr.resolve(t));

		t = "$Y{y}x";
		assertEquals("$Y{y}x", vr.resolve(t));

		t = "x$Y{y}";
		assertEquals("x$Y{y}", vr.resolve(t));

		// Incomplete vars
		// TODO - fix
//		t = "x$Y{y";
//		assertEquals("x$Y{y", vr.resolve(t));
	}

	public static class XVar extends SimpleVar {
		public XVar() {
			super("X");
		}
		@Override
		public String resolve(VarResolverSession session, String arg) {
			return "x" + arg + "x";
		}
	}

	//====================================================================================================
	// test - No-name variables
	//====================================================================================================
	@Test
	public void test2() throws Exception {
		VarResolver vr = new VarResolverBuilder().vars(BlankVar.class).build();
		String t;

		t = "${y}";
		assertEquals("xyx", vr.resolve(t));

		t = "${${y}}";
		assertEquals("xxyxx", vr.resolve(t));

		t = "${${y}${y}}";
		assertEquals("xxyxxyxx", vr.resolve(t));

		t = "z${z${y}z}z";
		assertEquals("zxzxyxzxz", vr.resolve(t));
	}

	public static class BlankVar extends SimpleVar {
		public BlankVar() {
			super("");
		}
		@Override
		public String resolve(VarResolverSession session, String arg) {
			return "x" + arg + "x";
		}
	}

	//====================================================================================================
	// test - No-name variables
	//====================================================================================================
	@Test
	public void testEscaped$() throws Exception {
		VarResolver vr = new VarResolverBuilder().vars(BlankVar.class).build();
		String t;

		t = "${y}";
		assertEquals("xyx", vr.resolve(t));
		t = "\\${y}";
		assertEquals("${y}", vr.resolve(t));

		t = "foo\\${y}foo";
		assertEquals("foo${y}foo", vr.resolve(t));

		// TODO - This doesn't work.
		//t = "${\\${y}}";
		//assertEquals("x${y}x", vr.resolve(t));
	}

	//====================================================================================================
	// test - Escape sequences.
	//====================================================================================================
	@Test
	public void testEscapedSequences() throws Exception {
		VarResolver vr = new VarResolverBuilder().vars(XVar.class).build();
		String t;
		char b = '\\';

		t = "A|A".replace('|',b);
		assertEquals("A|A".replace('|',b), vr.resolve(t));
		t = "A||A".replace('|',b);
		assertEquals("A|A".replace('|',b), vr.resolve(t));
		t = "A|A$X{B}".replace('|',b);
		assertEquals("A|AxBx".replace('|',b), vr.resolve(t));
		t = "A||A$X{B}".replace('|',b);
		assertEquals("A|AxBx".replace('|',b), vr.resolve(t));
		t = "A|$X{B}".replace('|',b);
		assertEquals("A$X{B}".replace('|',b), vr.resolve(t));
		t = "A||$X{B}".replace('|',b);
		assertEquals("A|xBx".replace('|',b), vr.resolve(t));
		t = "A$X|{B}".replace('|',b);
		assertEquals("A$X{B}".replace('|',b), vr.resolve(t));
		t = "A$X{B|}".replace('|',b);
		assertEquals("A$X{B}".replace('|',b), vr.resolve(t));
		t = "A$X{B}|".replace('|',b);
		assertEquals("AxBx|".replace('|',b), vr.resolve(t));
	}

	//====================================================================================================
	// Test $E variables
	//====================================================================================================
	@Test
	public void test$E() throws Exception {
		String t;

		t = "$E{PATH}";
		assertFalse(isEmpty(VarResolver.DEFAULT.resolve(t)));
	}

	//====================================================================================================
	// Test that StringResolver(parent) works as expected.
	//====================================================================================================
	@Test
	public void testParent() throws Exception {
		VarResolver vr = new VarResolverBuilder().defaultVars().vars(XMultipartVar.class).build();
		String t;
		System.setProperty("a", "a1");
		System.setProperty("b", "b1");

		t = "$X{$S{a},$S{b}}";
		assertEquals("a1+b1", vr.resolve(t));
		t = "$X{$S{a}}";
		assertEquals("a1", vr.resolve(t));
	}

	public static class XMultipartVar extends MultipartVar {
		public XMultipartVar() {
			super("X");
		}
		@Override /* MultipartVar */
		public String resolve(VarResolverSession session, String[] args) {
			return join(args, '+');
		}
	}

	//====================================================================================================
	// Test false triggers.
	//====================================================================================================
	@Test
	public void testFalseTriggers() throws Exception {
		VarResolverBuilder vrb = new VarResolverBuilder().defaultVars();
		String in = null;

		// Should reject names with characters outside A-Za-z
		for (Class<?> c : new Class[]{InvalidVar1.class, InvalidVar2.class, InvalidVar3.class, InvalidVar4.class, InvalidVar5.class}) {
			assertThrown(()->{vrb.vars(c);}).exists();
		}

		VarResolver vr = vrb.build();

		// These should all be unchanged.
		in = "$@{foobar}";
		assertEquals(in, vr.resolve(in));
		in = "$[{foobar}";
		assertEquals(in, vr.resolve(in));
		in = "$`{foobar}";
		assertEquals(in, vr.resolve(in));
		in = "$|{foobar}";
		assertEquals(in, vr.resolve(in));
		in = "${{foobar}";
		assertEquals(in, vr.resolve(in));
		in = "${$foobar}";
		assertEquals(in, vr.resolve(in));

		System.setProperty("foobar", "baz");

		in = "$";
		assertEquals(in, vr.resolve(in));

		in = "$S";
		assertEquals(in, vr.resolve(in));

		in = "$S{";
		assertEquals(in, vr.resolve(in));

		in = "$S{foobar";

		assertEquals(in, vr.resolve(in));
		in = "$S{foobar}$";
		assertEquals("baz$", vr.resolve(in));

		in = "$S{foobar}$S";
		assertEquals("baz$S", vr.resolve(in));

		in = "$S{foobar}$S{";
		assertEquals("baz$S{", vr.resolve(in));

		in = "$S{foobar}$S{foobar";
		assertEquals("baz$S{foobar", vr.resolve(in));

		System.clearProperty("foobar");
		in = "$S{foobar}";

		// Test nulls returned by StringVar.
		// Should be converted to blanks.
		vrb.vars(AlwaysNullVar.class);

		vr = vrb.build();

		in = "$A{xxx}";
		assertEquals("", vr.resolve(in));
		in = "x$A{xxx}";
		assertEquals("x", vr.resolve(in));
		in = "$A{xxx}x";
		assertEquals("x", vr.resolve(in));
	}

	public static class AlwaysNullVar extends SimpleVar {
		public AlwaysNullVar() {
			super("A");
		}
		@Override
		public String resolve(VarResolverSession session, String key) {
			return null;
		}
	}

	public static class InvalidVar extends SimpleVar {
		public InvalidVar(String c) {
			super(c);
		}
		@Override
		public String resolve(VarResolverSession session, String key) {
			return null;
		}
	}

	public static class InvalidVar1 extends InvalidVar {
		public InvalidVar1() {
			super(null);
		}
	}
	public static class InvalidVar2 extends InvalidVar {
		public InvalidVar2() {
			super("@");
		}
	}
	public static class InvalidVar3 extends InvalidVar {
		public InvalidVar3() {
			super("[");
		}
	}
	public static class InvalidVar4 extends InvalidVar {
		public InvalidVar4() {
			super("`");
		}
	}
	public static class InvalidVar5 extends InvalidVar {
		public InvalidVar5() {
			super("|");
		}
	}
}
