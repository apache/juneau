/*******************************************************************************
 * Licensed Materials - Property of IBM
 * © Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 * Note to U.S. Government Users Restricted Rights:  Use,
 * duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp.
 *******************************************************************************/
package com.ibm.juno.core.test.urlencoding;

import static org.junit.Assert.*;

import org.junit.*;

import com.ibm.juno.core.*;
import com.ibm.juno.core.urlencoding.*;

public class CT_UonSerializer {

	static UonSerializer s = UonSerializer.DEFAULT_ENCODING;
	static UonSerializer ss = UonSerializer.DEFAULT_SIMPLE_ENCODING;
	static UonSerializer su = UonSerializer.DEFAULT;
	static UonSerializer ssu = UonSerializer.DEFAULT_SIMPLE;
	static UonSerializer sr = UonSerializer.DEFAULT_READABLE;


	//====================================================================================================
	// Basic test
	//====================================================================================================
	@Test
	public void testBasic() throws Exception {

		Object t;

		// Simple string
		// Top level
		t = "a";
		assertEquals("a", s.serialize(t));
		assertEquals("a", ss.serialize(t));
		assertEquals("a", su.serialize(t));
		assertEquals("a", ssu.serialize(t));
		assertEquals("a", sr.serialize(t));

		// 2nd level
		t = new ObjectMap("{a:'a'}");
		assertEquals("$o(a=a)", s.serialize(t));
		assertEquals("(a=a)", ss.serialize(t));
		assertEquals("$o(a=a)", su.serialize(t));
		assertEquals("(a=a)", ssu.serialize(t));
		assertEquals("$o(\n\ta=a\n)", sr.serialize(t));

		// Simple map
		// Top level
		t = new ObjectMap("{a:'b',c:123,d:false,e:true,f:null}");
		assertEquals("$o(a=b,c=$n(123),d=$b(false),e=$b(true),f=%00)", s.serialize(t));
		assertEquals("(a=b,c=123,d=false,e=true,f=%00)", ss.serialize(t));
		assertEquals("$o(a=b,c=$n(123),d=$b(false),e=$b(true),f=\u0000)", su.serialize(t));
		assertEquals("(a=b,c=123,d=false,e=true,f=\u0000)", ssu.serialize(t));
		assertEquals("$o(\n\ta=b,\n\tc=$n(123),\n\td=$b(false),\n\te=$b(true),\n\tf=\u0000\n)", sr.serialize(t));

		// 2nd level
		t = new ObjectMap("{a:{a:'b',c:123,d:false,e:true,f:null}}");
		assertEquals("$o(a=$o(a=b,c=$n(123),d=$b(false),e=$b(true),f=%00))", s.serialize(t));
		assertEquals("(a=(a=b,c=123,d=false,e=true,f=%00))", ss.serialize(t));
		assertEquals("$o(a=$o(a=b,c=$n(123),d=$b(false),e=$b(true),f=\u0000))", su.serialize(t));
		assertEquals("(a=(a=b,c=123,d=false,e=true,f=\u0000))", ssu.serialize(t));
		assertEquals("$o(\n\ta=$o(\n\t\ta=b,\n\t\tc=$n(123),\n\t\td=$b(false),\n\t\te=$b(true),\n\t\tf=\u0000\n\t)\n)", sr.serialize(t));

		// Simple map with primitives as literals
		t = new ObjectMap("{a:'b',c:'123',d:'false',e:'true',f:'null'}");
		assertEquals("$o(a=b,c=123,d=false,e=true,f=null)", s.serialize(t));
		assertEquals("(a=b,c=123,d=false,e=true,f=null)", ss.serialize(t));
		assertEquals("$o(a=b,c=123,d=false,e=true,f=null)", su.serialize(t));
		assertEquals("(a=b,c=123,d=false,e=true,f=null)", ssu.serialize(t));
		assertEquals("$o(\n\ta=b,\n\tc=123,\n\td=false,\n\te=true,\n\tf=null\n)", sr.serialize(t));

		// null
		// Note that serializeParams is always encoded.
		// Top level
		t = null;
		assertEquals("%00", s.serialize(t));
		assertEquals("%00", ss.serialize(t));
		assertEquals("\u0000", su.serialize(t));
		assertEquals("\u0000", ssu.serialize(t));
		assertEquals("\u0000", sr.serialize(t));

		// 2nd level
		t = new ObjectMap("{null:null}");
		assertEquals("$o(%00=%00)", s.serialize(t));
		assertEquals("(%00=%00)", ss.serialize(t));
		assertEquals("$o(\u0000=\u0000)", su.serialize(t));
		assertEquals("(\u0000=\u0000)", ssu.serialize(t));
		assertEquals("$o(\n\t\u0000=\u0000\n)", sr.serialize(t));

		// 3rd level
		t = new ObjectMap("{null:{null:null}}");
		assertEquals("$o(%00=$o(%00=%00))", s.serialize(t));
		assertEquals("(%00=(%00=%00))", ss.serialize(t));
		assertEquals("$o(\u0000=$o(\u0000=\u0000))", su.serialize(t));
		assertEquals("(\u0000=(\u0000=\u0000))", ssu.serialize(t));
		assertEquals("$o(\n\t\u0000=$o(\n\t\t\u0000=\u0000\n\t)\n)", sr.serialize(t));

		// Empty array
		// Top level
		t = new String[0];
		assertEquals("$a()", s.serialize(t));
		assertEquals("()", ss.serialize(t));
		assertEquals("$a()", su.serialize(t));
		assertEquals("()", ssu.serialize(t));
		assertEquals("$a()", sr.serialize(t));

		// 2nd level in map
		t = new ObjectMap("{x:[]}");
		assertEquals("$o(x=$a())", s.serialize(t));
		assertEquals("(x=())", ss.serialize(t));
		assertEquals("$o(x=$a())", su.serialize(t));
		assertEquals("(x=())", ssu.serialize(t));
		assertEquals("$o(\n\tx=$a()\n)", sr.serialize(t));

		// Empty 2 dimensional array
		t = new String[1][0];
		assertEquals("$a($a())", s.serialize(t));
		assertEquals("(())", ss.serialize(t));
		assertEquals("$a($a())", su.serialize(t));
		assertEquals("(())", ssu.serialize(t));
		assertEquals("$a(\n\t$a()\n)", sr.serialize(t));

		// Array containing empty string
		// Top level
		t = new String[]{""};
		assertEquals("$a(())", s.serialize(t));
		assertEquals("(())", ss.serialize(t));
		assertEquals("$a(())", su.serialize(t));
		assertEquals("(())", ssu.serialize(t));
		assertEquals("$a(\n\t()\n)", sr.serialize(t));

		// 2nd level
		t = new ObjectMap("{x:['']}");
		assertEquals("$o(x=$a(()))", s.serialize(t));
		assertEquals("(x=(()))", ss.serialize(t));
		assertEquals("$o(x=$a(()))", su.serialize(t));
		assertEquals("(x=(()))", ssu.serialize(t));
		assertEquals("$o(\n\tx=$a(\n\t\t()\n\t)\n)", sr.serialize(t));

		// Array containing 3 empty strings
		t = new String[]{"","",""};
		assertEquals("$a(,,)", s.serialize(t));
		assertEquals("(,,)", ss.serialize(t));
		assertEquals("$a(,,)", su.serialize(t));
		assertEquals("(,,)", ssu.serialize(t));
		assertEquals("$a(\n\t(),\n\t(),\n\t()\n)", sr.serialize(t));

		// String containing \u0000
		// Top level
		t = "\u0000";
		assertEquals("(%00)", s.serialize(t));
		assertEquals("(%00)", ss.serialize(t));
		assertEquals("(\u0000)", su.serialize(t));
		assertEquals("(\u0000)", ssu.serialize(t));
		assertEquals("(\u0000)", sr.serialize(t));

		// 2nd level
		t = new ObjectMap("{'\u0000':'\u0000'}");
		assertEquals("$o((%00)=(%00))", s.serialize(t));
		assertEquals("((%00)=(%00))", ss.serialize(t));
		assertEquals("$o((\u0000)=(\u0000))", su.serialize(t));
		assertEquals("((\u0000)=(\u0000))", ssu.serialize(t));
		assertEquals("$o(\n\t(\u0000)=(\u0000)\n)", sr.serialize(t));

		// Boolean
		// Top level
		t = false;
		assertEquals("$b(false)", s.serialize(t));
		assertEquals("false", ss.serialize(t));
		assertEquals("$b(false)", su.serialize(t));
		assertEquals("false", ssu.serialize(t));
		assertEquals("$b(false)", sr.serialize(t));

		// 2nd level
		t = new ObjectMap("{x:false}");
		assertEquals("$o(x=$b(false))", s.serialize(t));
		assertEquals("(x=false)", ss.serialize(t));
		assertEquals("$o(x=$b(false))", su.serialize(t));
		assertEquals("(x=false)", ssu.serialize(t));
		assertEquals("$o(\n\tx=$b(false)\n)", sr.serialize(t));

		// Number
		// Top level
		t = 123;
		assertEquals("$n(123)", s.serialize(t));
		assertEquals("123", ss.serialize(t));
		assertEquals("$n(123)", su.serialize(t));
		assertEquals("123", ssu.serialize(t));
		assertEquals("$n(123)", sr.serialize(t));

		// 2nd level
		t = new ObjectMap("{x:123}");
		assertEquals("$o(x=$n(123))", s.serialize(t));
		assertEquals("(x=123)", ss.serialize(t));
		assertEquals("$o(x=$n(123))", su.serialize(t));
		assertEquals("(x=123)", ssu.serialize(t));
		assertEquals("$o(\n\tx=$n(123)\n)", sr.serialize(t));

		// Unencoded chars
		// Top level
		t = "x;/?:@-_.!*'";
		assertEquals("x;/?:@-_.!*'", s.serialize(t));
		assertEquals("x;/?:@-_.!*'", ss.serialize(t));
		assertEquals("x;/?:@-_.!*'", su.serialize(t));
		assertEquals("x;/?:@-_.!*'", ssu.serialize(t));
		assertEquals("x;/?:@-_.!*'", sr.serialize(t));

		// 2nd level
		t = new ObjectMap("{x:'x;/?:@-_.!*\\''}");
		assertEquals("$o(x=x;/?:@-_.!*')", s.serialize(t));
		assertEquals("(x=x;/?:@-_.!*')", ss.serialize(t));
		assertEquals("$o(x=x;/?:@-_.!*')", su.serialize(t));
		assertEquals("(x=x;/?:@-_.!*')", ssu.serialize(t));
		assertEquals("$o(\n\tx=x;/?:@-_.!*'\n)", sr.serialize(t));

		// Encoded chars
		// Top level
		t = "x{}|\\^[]`<>#%\"&+";
		assertEquals("x%7B%7D%7C%5C%5E%5B%5D%60%3C%3E%23%25%22%26%2B", s.serialize(t));
		assertEquals("x%7B%7D%7C%5C%5E%5B%5D%60%3C%3E%23%25%22%26%2B", ss.serialize(t));
		assertEquals("x{}|\\^[]`<>#%\"&+", su.serialize(t));
		assertEquals("x{}|\\^[]`<>#%\"&+", ssu.serialize(t));
		assertEquals("x{}|\\^[]`<>#%\"&+", sr.serialize(t));

		// 2nd level
		t = new ObjectMap("{'x{}|\\\\^[]`<>#%\"&+':'x{}|\\\\^[]`<>#%\"&+'}");
		assertEquals("$o(x%7B%7D%7C%5C%5E%5B%5D%60%3C%3E%23%25%22%26%2B=x%7B%7D%7C%5C%5E%5B%5D%60%3C%3E%23%25%22%26%2B)", s.serialize(t));
		assertEquals("(x%7B%7D%7C%5C%5E%5B%5D%60%3C%3E%23%25%22%26%2B=x%7B%7D%7C%5C%5E%5B%5D%60%3C%3E%23%25%22%26%2B)", ss.serialize(t));
		assertEquals("$o(x{}|\\^[]`<>#%\"&+=x{}|\\^[]`<>#%\"&+)", su.serialize(t));
		assertEquals("(x{}|\\^[]`<>#%\"&+=x{}|\\^[]`<>#%\"&+)", ssu.serialize(t));
		assertEquals("$o(\n\tx{}|\\^[]`<>#%\"&+=x{}|\\^[]`<>#%\"&+\n)", sr.serialize(t));

		// Escaped chars
		// Top level
		t = "x$,()~";
		assertEquals("x$,()~", s.serialize(t));
		assertEquals("x$,()~", ss.serialize(t));
		assertEquals("x$,()~", su.serialize(t));
		assertEquals("x$,()~", ssu.serialize(t));
		assertEquals("x$,()~", sr.serialize(t));

		// 2nd level
		// Note behavior on serializeParams() is different since 2nd-level is top level.
		t = new ObjectMap("{'x$,()~':'x$,()~'}");
		assertEquals("$o(x$~,~(~)~~=x$~,~(~)~~)", s.serialize(t));
		assertEquals("(x$~,~(~)~~=x$~,~(~)~~)", ss.serialize(t));
		assertEquals("$o(x$~,~(~)~~=x$~,~(~)~~)", su.serialize(t));
		assertEquals("(x$~,~(~)~~=x$~,~(~)~~)", ssu.serialize(t));
		assertEquals("$o(\n\tx$~,~(~)~~=x$~,~(~)~~\n)", sr.serialize(t));

		// 3rd level
		// Note behavior on serializeParams().
		t = new ObjectMap("{'x$,()~':{'x$,()~':'x$,()~'}}");
		assertEquals("$o(x$~,~(~)~~=$o(x$~,~(~)~~=x$~,~(~)~~))", s.serialize(t));
		assertEquals("(x$~,~(~)~~=(x$~,~(~)~~=x$~,~(~)~~))", ss.serialize(t));
		assertEquals("$o(x$~,~(~)~~=$o(x$~,~(~)~~=x$~,~(~)~~))", su.serialize(t));
		assertEquals("(x$~,~(~)~~=(x$~,~(~)~~=x$~,~(~)~~))", ssu.serialize(t));
		assertEquals("$o(\n\tx$~,~(~)~~=$o(\n\t\tx$~,~(~)~~=x$~,~(~)~~\n\t)\n)", sr.serialize(t));

		// Equals sign
		// Gets encoded at top level, and encoded+escaped at 2nd level.
		// Top level
		t = "x=";
		assertEquals("x=", s.serialize(t));
		assertEquals("x=", ss.serialize(t));
		assertEquals("x=", su.serialize(t));
		assertEquals("x=", ssu.serialize(t));
		assertEquals("x=", sr.serialize(t));

		// 2nd level
		t = new ObjectMap("{'x=':'x='}");
		assertEquals("$o(x~==x~=)", s.serialize(t));
		assertEquals("(x~==x~=)", ss.serialize(t));
		assertEquals("$o(x~==x~=)", su.serialize(t));
		assertEquals("(x~==x~=)", ssu.serialize(t));
		assertEquals("$o(\n\tx~==x~=\n)", sr.serialize(t));

		// 3rd level
		t = new ObjectMap("{'x=':{'x=':'x='}}");
		assertEquals("$o(x~==$o(x~==x~=))", s.serialize(t));
		assertEquals("(x~==(x~==x~=))", ss.serialize(t));
		assertEquals("$o(x~==$o(x~==x~=))", su.serialize(t));
		assertEquals("(x~==(x~==x~=))", ssu.serialize(t));
		assertEquals("$o(\n\tx~==$o(\n\t\tx~==x~=\n\t)\n)", sr.serialize(t));

		// String starting with parenthesis
		// Top level
		t = "()";
		assertEquals("(~(~))", s.serialize(t));
		assertEquals("(~(~))", ss.serialize(t));
		assertEquals("(~(~))", su.serialize(t));
		assertEquals("(~(~))", ssu.serialize(t));
		assertEquals("(~(~))", sr.serialize(t));

		// 2nd level
		t = new ObjectMap("{'()':'()'}");
		assertEquals("$o((~(~))=(~(~)))", s.serialize(t));
		assertEquals("((~(~))=(~(~)))", ss.serialize(t));
		assertEquals("$o((~(~))=(~(~)))", su.serialize(t));
		assertEquals("((~(~))=(~(~)))", ssu.serialize(t));
		assertEquals("$o(\n\t(~(~))=(~(~))\n)", sr.serialize(t));

		// String starting with $
		// Top level
		t = "$a";
		assertEquals("($a)", s.serialize(t));
		assertEquals("($a)", ss.serialize(t));
		assertEquals("($a)", su.serialize(t));
		assertEquals("($a)", ssu.serialize(t));
		assertEquals("($a)", sr.serialize(t));

		// 2nd level
		t = new ObjectMap("{$a:'$a'}");
		assertEquals("$o(($a)=($a))", s.serialize(t));
		assertEquals("(($a)=($a))", ss.serialize(t));
		assertEquals("$o(($a)=($a))", su.serialize(t));
		assertEquals("(($a)=($a))", ssu.serialize(t));
		assertEquals("$o(\n\t($a)=($a)\n)", sr.serialize(t));

		// Blank string
		// Top level
		t = "";
		assertEquals("", s.serialize(t));
		assertEquals("", ss.serialize(t));
		assertEquals("", su.serialize(t));
		assertEquals("", ssu.serialize(t));
		assertEquals("", sr.serialize(t));

		// 2nd level
		t = new ObjectMap("{'':''}");
		assertEquals("$o(=)", s.serialize(t));
		assertEquals("(=)", ss.serialize(t));
		assertEquals("$o(=)", su.serialize(t));
		assertEquals("(=)", ssu.serialize(t));
		assertEquals("$o(\n\t()=()\n)", sr.serialize(t));

		// 3rd level
		t = new ObjectMap("{'':{'':''}}");
		assertEquals("$o(=$o(=))", s.serialize(t));
		assertEquals("(=(=))", ss.serialize(t));
		assertEquals("$o(=$o(=))", su.serialize(t));
		assertEquals("(=(=))", ssu.serialize(t));
		assertEquals("$o(\n\t()=$o(\n\t\t()=()\n\t)\n)", sr.serialize(t));

		// Newline character
		// Top level
		t = "\n";
		assertEquals("%0A", s.serialize(t));
		assertEquals("%0A", ss.serialize(t));
		assertEquals("\n", su.serialize(t));
		assertEquals("\n", ssu.serialize(t));
		assertEquals("(\n)", sr.serialize(t));

		// 2nd level
		t = new ObjectMap("{'\n':'\n'}");
		assertEquals("$o(%0A=%0A)", s.serialize(t));
		assertEquals("(%0A=%0A)", ss.serialize(t));
		assertEquals("$o(\n=\n)", su.serialize(t));
		assertEquals("(\n=\n)", ssu.serialize(t));
		assertEquals("$o(\n\t(\n)=(\n)\n)", sr.serialize(t));

		// 3rd level
		t = new ObjectMap("{'\n':{'\n':'\n'}}");
		assertEquals("$o(%0A=$o(%0A=%0A))", s.serialize(t));
		assertEquals("(%0A=(%0A=%0A))", ss.serialize(t));
		assertEquals("$o(\n=$o(\n=\n))", su.serialize(t));
		assertEquals("(\n=(\n=\n))", ssu.serialize(t));
		assertEquals("$o(\n\t(\n)=$o(\n\t\t(\n)=(\n)\n\t)\n)", sr.serialize(t));
	}

	//====================================================================================================
	// Unicode characters test
	//====================================================================================================
	@Test
	public void testUnicodeChars() throws Exception {
		Object t;

		// 2-byte UTF-8 character
		// Top level
		t = "¢";
		assertEquals("%C2%A2", s.serialize(t));
		assertEquals("%C2%A2", ss.serialize(t));
		assertEquals("¢", su.serialize(t));
		assertEquals("¢", ssu.serialize(t));
		assertEquals("¢", sr.serialize(t));

		// 2nd level
		t = new ObjectMap("{'¢':'¢'}");
		assertEquals("$o(%C2%A2=%C2%A2)", s.serialize(t));
		assertEquals("(%C2%A2=%C2%A2)", ss.serialize(t));
		assertEquals("$o(¢=¢)", su.serialize(t));
		assertEquals("(¢=¢)", ssu.serialize(t));
		assertEquals("$o(\n\t¢=¢\n)", sr.serialize(t));

		// 3rd level
		t = new ObjectMap("{'¢':{'¢':'¢'}}");
		assertEquals("$o(%C2%A2=$o(%C2%A2=%C2%A2))", s.serialize(t));
		assertEquals("(%C2%A2=(%C2%A2=%C2%A2))", ss.serialize(t));
		assertEquals("$o(¢=$o(¢=¢))", su.serialize(t));
		assertEquals("(¢=(¢=¢))", ssu.serialize(t));
		assertEquals("$o(\n\t¢=$o(\n\t\t¢=¢\n\t)\n)", sr.serialize(t));

		// 3-byte UTF-8 character
		// Top level
		t = "€";
		assertEquals("%E2%82%AC", s.serialize(t));
		assertEquals("%E2%82%AC", ss.serialize(t));
		assertEquals("€", su.serialize(t));
		assertEquals("€", ssu.serialize(t));
		assertEquals("€", sr.serialize(t));

		// 2nd level
		t = new ObjectMap("{'€':'€'}");
		assertEquals("$o(%E2%82%AC=%E2%82%AC)", s.serialize(t));
		assertEquals("(%E2%82%AC=%E2%82%AC)", ss.serialize(t));
		assertEquals("$o(€=€)", su.serialize(t));
		assertEquals("(€=€)", ssu.serialize(t));
		assertEquals("$o(\n\t€=€\n)", sr.serialize(t));

		// 3rd level
		t = new ObjectMap("{'€':{'€':'€'}}");
		assertEquals("$o(%E2%82%AC=$o(%E2%82%AC=%E2%82%AC))", s.serialize(t));
		assertEquals("(%E2%82%AC=(%E2%82%AC=%E2%82%AC))", ss.serialize(t));
		assertEquals("$o(€=$o(€=€))", su.serialize(t));
		assertEquals("(€=(€=€))", ssu.serialize(t));
		assertEquals("$o(\n\t€=$o(\n\t\t€=€\n\t)\n)", sr.serialize(t));

		// 4-byte UTF-8 character
		// Top level
		t = "𤭢";
		assertEquals("%F0%A4%AD%A2", s.serialize(t));
		assertEquals("%F0%A4%AD%A2", ss.serialize(t));
		assertEquals("𤭢", su.serialize(t));
		assertEquals("𤭢", ssu.serialize(t));
		assertEquals("𤭢", sr.serialize(t));

		// 2nd level
		t = new ObjectMap("{'𤭢':'𤭢'}");
		assertEquals("$o(%F0%A4%AD%A2=%F0%A4%AD%A2)", s.serialize(t));
		assertEquals("(%F0%A4%AD%A2=%F0%A4%AD%A2)", ss.serialize(t));
		assertEquals("$o(𤭢=𤭢)", su.serialize(t));
		assertEquals("(𤭢=𤭢)", ssu.serialize(t));
		assertEquals("$o(\n\t𤭢=𤭢\n)", sr.serialize(t));

		// 3rd level
		t = new ObjectMap("{'𤭢':{'𤭢':'𤭢'}}");
		assertEquals("$o(%F0%A4%AD%A2=$o(%F0%A4%AD%A2=%F0%A4%AD%A2))", s.serialize(t));
		assertEquals("(%F0%A4%AD%A2=(%F0%A4%AD%A2=%F0%A4%AD%A2))", ss.serialize(t));
		assertEquals("$o(𤭢=$o(𤭢=𤭢))", su.serialize(t));
		assertEquals("(𤭢=(𤭢=𤭢))", ssu.serialize(t));
		assertEquals("$o(\n\t𤭢=$o(\n\t\t𤭢=𤭢\n\t)\n)", sr.serialize(t));
	}
}