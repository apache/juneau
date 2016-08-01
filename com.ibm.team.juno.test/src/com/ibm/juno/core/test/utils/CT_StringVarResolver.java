/*******************************************************************************
 * Licensed Materials - Property of IBM
 * © Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 * 
 * Note to U.S. Government Users Restricted Rights:  Use,
 * duplication or disclosure restricted by GSA ADP Schedule 
 * Contract with IBM Corp.
 *******************************************************************************/
package com.ibm.juno.core.test.utils;

import static org.junit.Assert.*;

import org.junit.*;

import com.ibm.juno.core.utils.*;

public class CT_StringVarResolver {

	//====================================================================================================
	// test - Basic tests
	//====================================================================================================
	@Test
	public void test() throws Exception {	
		StringVarResolver vr = new StringVarResolver()
			.addVar("X", new StringVar() {
				@Override /* StringVar */
				public String resolve(String arg) {
					return "x" + arg + "x";
				}
			});
		String t;
		
		t = null;
		assertEquals("", vr.resolve(t));

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

	//====================================================================================================
	// test - No-name variables
	//====================================================================================================
	@Test
	public void test2() throws Exception {	
		StringVarResolver vr = new StringVarResolver()
			.addVar("", new StringVar() {
				@Override /* StringVar */
				public String resolve(String arg) {
					return "x" + arg + "x";
				}
			});
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

	//====================================================================================================
	// test - No-name variables
	//====================================================================================================
	@Test
	public void testEscaped$() throws Exception {
		StringVarResolver vr = new StringVarResolver()
			.addVar("", new StringVar() {
				@Override /* StringVar */
				public String resolve(String arg) {
					return "x" + arg + "x";
				}
			});

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
		StringVarResolver vr = new StringVarResolver()
		.addVar("x", new StringVar() {
			@Override /* StringVar */
			public String resolve(String arg) {
				return "x" + arg + "x";
			}
		});

		String t;
		char b = '\\';

		t = "A|A".replace('|',b);
		assertEquals("A|A".replace('|',b), vr.resolve(t));
		t = "A||A".replace('|',b);
		assertEquals("A|A".replace('|',b), vr.resolve(t));
		t = "A|A$x{B}".replace('|',b);
		assertEquals("A|AxBx".replace('|',b), vr.resolve(t));
		t = "A||A$x{B}".replace('|',b);
		assertEquals("A|AxBx".replace('|',b), vr.resolve(t));
		t = "A|$x{B}".replace('|',b);
		assertEquals("A$x{B}".replace('|',b), vr.resolve(t));
		t = "A||$x{B}".replace('|',b);
		assertEquals("A|xBx".replace('|',b), vr.resolve(t));
		t = "A$x|{B}".replace('|',b);
		assertEquals("A$x{B}".replace('|',b), vr.resolve(t));
		t = "A$x{B|}".replace('|',b);
		assertEquals("A$x{B}".replace('|',b), vr.resolve(t));
		t = "A$x{B}|".replace('|',b);
		assertEquals("AxBx|".replace('|',b), vr.resolve(t));
	}

	//====================================================================================================
	// Test $E variables
	//====================================================================================================
	@Test
	public void test$E() throws Exception {
		String t;

		t = "$E{PATH}";
		assertFalse(StringUtils.isEmpty(StringVarResolver.DEFAULT.resolve(t)));
	}

	//====================================================================================================
	// Test that StringResolver(parent) works as expected.
	//====================================================================================================
	@Test
	public void testParent() throws Exception {
		StringVarResolver svr = new StringVarResolver(StringVarResolver.DEFAULT);
		svr.addVar("X", new StringVarMultipart() {
			@Override /* StringVarMultipart */
			public String resolve(String[] args) {
				return StringUtils.join(args, '+');
			}
		});
		String t;
		System.setProperty("a", "a1");
		System.setProperty("b", "b1");

		t = "$X{$S{a},$S{b}}";
		assertEquals("a1+b1", svr.resolve(t));
		t = "$X{$S{a}}";
		assertEquals("a1", svr.resolve(t));
	}

	//====================================================================================================
	// Test false triggers.
	//====================================================================================================
	@Test
	public void testFalseTriggers() throws Exception {
		StringVarResolver svr = StringVarResolver.DEFAULT;
		String in = null;

		try {
			svr.addVar("a", null);
			fail();
		} catch (IllegalArgumentException e) {
			assertEquals("Field 'v' cannot be null.", e.getLocalizedMessage());
		}

		// Should reject names with characters outside A-Za-z
		for (char c : new char[]{'@','[','`','|'}) {
			try {
				svr.addVar(""+c, new StringVar() {
					@Override /* StringVar */
					public String resolve(String arg) {
						return null;
					}
				});
				fail();
			} catch (IllegalArgumentException e) {
				assertEquals("Invalid var name.  Must consist of only uppercase and lowercase ASCII letters.", e.getLocalizedMessage());
			}
		}

		// These should all be unchanged.
		in = "$@{foobar}";
		assertEquals(in, svr.resolve(in));
		in = "$[{foobar}";
		assertEquals(in, svr.resolve(in));
		in = "$`{foobar}";
		assertEquals(in, svr.resolve(in));
		in = "$|{foobar}";
		assertEquals(in, svr.resolve(in));
		in = "${{foobar}";
		assertEquals(in, svr.resolve(in));
		in = "${$foobar}";
		assertEquals(in, svr.resolve(in));

		System.setProperty("foobar", "baz");

		in = "$";
		assertEquals(in, svr.resolve(in));

		in = "$S";
		assertEquals(in, svr.resolve(in));

		in = "$S{";
		assertEquals(in, svr.resolve(in));

		in = "$S{foobar";

		assertEquals(in, svr.resolve(in));
		in = "$S{foobar}$";
		assertEquals("baz$", svr.resolve(in));

		in = "$S{foobar}$S";
		assertEquals("baz$S", svr.resolve(in));

		in = "$S{foobar}$S{";
		assertEquals("baz$S{", svr.resolve(in));

		in = "$S{foobar}$S{foobar";
		assertEquals("baz$S{foobar", svr.resolve(in));

		System.clearProperty("foobar");
		in = "$S{foobar}";

		// Test nulls returned by StringVar.
		// Should be converted to blanks.
		svr.addVar("A", new StringVar() {
			@Override /* StringVar */
			public String resolve(String arg) {
				return null;
			}
		});

		in = "$A{xxx}";
		assertEquals("", svr.resolve(in));
		in = "x$A{xxx}";
		assertEquals("x", svr.resolve(in));
		in = "$A{xxx}x";
		assertEquals("x", svr.resolve(in));
	}
}
