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
package org.apache.juneau.marshall.uon;

import static org.apache.juneau.commons.utils.Shorts.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.marshall.json5.*;
import org.junit.jupiter.api.*;

@SuppressWarnings({
	"java:S5961" // High assertion count acceptable in comprehensive test
})
class UonSerializer_Test {

	static UonSerializer s = UonSerializer.DEFAULT_ENCODING;
	static UonSerializer su = UonSerializer.DEFAULT;
	static UonSerializer sr = UonSerializer.DEFAULT_READABLE;

	//====================================================================================================
	// Basic test
	//====================================================================================================
	@Test void a01_basic() throws Exception {
		// Simple string
		// Top level
		var t = (Object)"a";
		assertEquals("a", s.write(t));
		assertEquals("a", su.write(t));
		assertEquals("a", sr.write(t));

		// 2nd level
		t = Json5Map.ofString("{a:'a'}");
		assertEquals("(a=a)", s.write(t));
		assertEquals("(a=a)", su.write(t));
		assertEquals("(\n\ta=a\n)", sr.write(t));

		// Simple map
		// Top level
		t = Json5Map.ofString("{a:'b',c:123,d:false,e:true,f:null}");
		assertEquals("(a=b,c=123,d=false,e=true,f=null)", s.write(t));
		assertEquals("(a=b,c=123,d=false,e=true,f=null)", su.write(t));
		assertEquals("(\n\ta=b,\n\tc=123,\n\td=false,\n\te=true,\n\tf=null\n)", sr.write(t));

		// 2nd level
		t = Json5Map.ofString("{a:{a:'b',c:123,d:false,e:true,f:null}}");
		assertEquals("(a=(a=b,c=123,d=false,e=true,f=null))", s.write(t));
		assertEquals("(a=(a=b,c=123,d=false,e=true,f=null))", su.write(t));
		assertEquals("(\n\ta=(\n\t\ta=b,\n\t\tc=123,\n\t\td=false,\n\t\te=true,\n\t\tf=null\n\t)\n)", sr.write(t));

		// Simple map with primitives as literals
		t = Json5Map.ofString("{a:'b',c:'123',d:'false',e:'true',f:'null'}");
		assertEquals("(a=b,c='123',d='false',e='true',f='null')", s.write(t));
		assertEquals("(a=b,c='123',d='false',e='true',f='null')", su.write(t));
		assertEquals("(\n\ta=b,\n\tc='123',\n\td='false',\n\te='true',\n\tf='null'\n)", sr.write(t));

		// null
		// Note that writeParams is always encoded.
		// Top level
		t = null;
		assertEquals("null", s.write(t));
		assertEquals("null", su.write(t));
		assertEquals("null", sr.write(t));

		// 2nd level
		t = Json5Map.ofString("{null:null}");
		assertEquals("(null=null)", s.write(t));
		assertEquals("(null=null)", su.write(t));
		assertEquals("(\n\tnull=null\n)", sr.write(t));

		// 3rd level
		t = Json5Map.ofString("{null:{null:null}}");
		assertEquals("(null=(null=null))", s.write(t));
		assertEquals("(null=(null=null))", su.write(t));
		assertEquals("(\n\tnull=(\n\t\tnull=null\n\t)\n)", sr.write(t));

		// Empty array
		// Top level
		t = new String[0];
		assertEquals("@()", s.write(t));
		assertEquals("@()", su.write(t));
		assertEquals("@()", sr.write(t));

		// 2nd level in map
		t = Json5Map.ofString("{x:[]}");
		assertEquals("(x=@())", s.write(t));
		assertEquals("(x=@())", su.write(t));
		assertEquals("(\n\tx=@()\n)", sr.write(t));

		// Empty 2 dimensional array
		t = new String[1][0];
		assertEquals("@(@())", s.write(t));
		assertEquals("@(@())", su.write(t));
		assertEquals("@(\n\t@()\n)", sr.write(t));

		// Array containing empty string
		// Top level
		t = a("");
		assertEquals("@('')", s.write(t));
		assertEquals("@('')", su.write(t));
		assertEquals("@(\n\t''\n)", sr.write(t));

		// 2nd level
		t = Json5Map.ofString("{x:['']}");
		assertEquals("(x=@(''))", s.write(t));
		assertEquals("(x=@(''))", su.write(t));
		assertEquals("(\n\tx=@(\n\t\t''\n\t)\n)", sr.write(t));

		// Array containing 3 empty strings
		t = a("","","");
		assertEquals("@('','','')", s.write(t));
		assertEquals("@('','','')", su.write(t));
		assertEquals("@(\n\t'',\n\t'',\n\t''\n)", sr.write(t));

		// String containing \u0000
		// Top level
		t = "\u0000";
		assertEquals("%00", s.write(t));
		assertEquals("\u0000", su.write(t));
		assertEquals("\u0000", sr.write(t));

		// 2nd level
		t = Json5Map.ofString("{'\u0000':'\u0000'}");
		assertEquals("(%00=%00)", s.write(t));
		assertEquals("(\u0000=\u0000)", su.write(t));
		assertEquals("(\n\t\u0000=\u0000\n)", sr.write(t));

		// Boolean
		// Top level
		t = false;
		assertEquals("false", s.write(t));
		assertEquals("false", su.write(t));
		assertEquals("false", sr.write(t));

		// 2nd level
		t = Json5Map.ofString("{x:false}");
		assertEquals("(x=false)", s.write(t));
		assertEquals("(x=false)", su.write(t));
		assertEquals("(\n\tx=false\n)", sr.write(t));

		// Number
		// Top level
		t = 123;
		assertEquals("123", s.write(t));
		assertEquals("123", su.write(t));
		assertEquals("123", sr.write(t));

		// 2nd level
		t = Json5Map.ofString("{x:123}");
		assertEquals("(x=123)", s.write(t));
		assertEquals("(x=123)", su.write(t));
		assertEquals("(\n\tx=123\n)", sr.write(t));

		// Unencoded chars
		// Top level
		t = "x;/?:@-_.!*'";
		assertEquals("x;/?:@-_.!*~'", s.write(t));
		assertEquals("x;/?:@-_.!*~'", su.write(t));
		assertEquals("x;/?:@-_.!*~'", sr.write(t));

		// 2nd level
		t = Json5Map.ofString("{x:'x;/?:@-_.!*\\''}");
		assertEquals("(x=x;/?:@-_.!*~')", s.write(t));
		assertEquals("(x=x;/?:@-_.!*~')", su.write(t));
		assertEquals("(\n\tx=x;/?:@-_.!*~'\n)", sr.write(t));

		// Encoded chars
		// Top level
		t = "x{}|\\^[]`<>#%\"&+";
		assertEquals("x%7B%7D%7C%5C%5E%5B%5D%60%3C%3E%23%25%22%26%2B", s.write(t));
		assertEquals("x{}|\\^[]`<>#%\"&+", su.write(t));
		assertEquals("x{}|\\^[]`<>#%\"&+", sr.write(t));

		// 2nd level
		t = Json5Map.ofString("{'x{}|\\\\^[]`<>#%\"&+':'x{}|\\\\^[]`<>#%\"&+'}");
		assertEquals("(x%7B%7D%7C%5C%5E%5B%5D%60%3C%3E%23%25%22%26%2B=x%7B%7D%7C%5C%5E%5B%5D%60%3C%3E%23%25%22%26%2B)", s.write(t));
		assertEquals("(x{}|\\^[]`<>#%\"&+=x{}|\\^[]`<>#%\"&+)", su.write(t));
		assertEquals("(\n\tx{}|\\^[]`<>#%\"&+=x{}|\\^[]`<>#%\"&+\n)", sr.write(t));

		// Escaped chars
		// Top level
		t = "x$,()~";
		assertEquals("'x$,()~~'", s.write(t));
		assertEquals("'x$,()~~'", su.write(t));
		assertEquals("'x$,()~~'", sr.write(t));

		// 2nd level
		// Note behavior on writeParams() is different since 2nd-level is top level.
		t = Json5Map.ofString("{'x$,()~':'x$,()~'}");
		assertEquals("('x$,()~~'='x$,()~~')", s.write(t));
		assertEquals("('x$,()~~'='x$,()~~')", su.write(t));
		assertEquals("(\n\t'x$,()~~'='x$,()~~'\n)", sr.write(t));

		// 3rd level
		// Note behavior on writeParams().
		t = Json5Map.ofString("{'x$,()~':{'x$,()~':'x$,()~'}}");
		assertEquals("('x$,()~~'=('x$,()~~'='x$,()~~'))", s.write(t));
		assertEquals("('x$,()~~'=('x$,()~~'='x$,()~~'))", su.write(t));
		assertEquals("(\n\t'x$,()~~'=(\n\t\t'x$,()~~'='x$,()~~'\n\t)\n)", sr.write(t));

		// Equals sign
		// Gets encoded at top level, and encoded+escaped at 2nd level.
		// Top level
		t = "x=";
		assertEquals("'x='", s.write(t));
		assertEquals("'x='", su.write(t));
		assertEquals("'x='", sr.write(t));

		// 2nd level
		t = Json5Map.ofString("{'x=':'x='}");
		assertEquals("('x='='x=')", s.write(t));
		assertEquals("('x='='x=')", su.write(t));
		assertEquals("(\n\t'x='='x='\n)", sr.write(t));

		// 3rd level
		t = Json5Map.ofString("{'x=':{'x=':'x='}}");
		assertEquals("('x='=('x='='x='))", s.write(t));
		assertEquals("('x='=('x='='x='))", su.write(t));
		assertEquals("(\n\t'x='=(\n\t\t'x='='x='\n\t)\n)", sr.write(t));

		// String starting with parenthesis
		// Top level
		t = "()";
		assertEquals("'()'", s.write(t));
		assertEquals("'()'", su.write(t));
		assertEquals("'()'", sr.write(t));

		// 2nd level
		t = Json5Map.ofString("{'()':'()'}");
		assertEquals("('()'='()')", s.write(t));
		assertEquals("('()'='()')", su.write(t));
		assertEquals("(\n\t'()'='()'\n)", sr.write(t));

		// String starting with $
		// Top level
		t = "$a";
		assertEquals("$a", s.write(t));
		assertEquals("$a", su.write(t));
		assertEquals("$a", sr.write(t));

		// 2nd level
		t = Json5Map.ofString("{$a:'$a'}");
		assertEquals("($a=$a)", s.write(t));
		assertEquals("($a=$a)", su.write(t));
		assertEquals("(\n\t$a=$a\n)", sr.write(t));

		// Blank string
		// Top level
		t = "";
		assertEquals("''", s.write(t));
		assertEquals("''", su.write(t));
		assertEquals("''", sr.write(t));

		// 2nd level
		t = Json5Map.ofString("{'':''}");
		assertEquals("(''='')", s.write(t));
		assertEquals("(''='')", su.write(t));
		assertEquals("(\n\t''=''\n)", sr.write(t));

		// 3rd level
		t = Json5Map.ofString("{'':{'':''}}");
		assertEquals("(''=(''=''))", s.write(t));
		assertEquals("(''=(''=''))", su.write(t));
		assertEquals("(\n\t''=(\n\t\t''=''\n\t)\n)", sr.write(t));

		// Newline character
		// Top level
		t = "\n";
		assertEquals("'%0A'", s.write(t));
		assertEquals("'\n'", su.write(t));
		assertEquals("'\n'", sr.write(t));

		// 2nd level
		t = Json5Map.ofString("{'\n':'\n'}");
		assertEquals("('%0A'='%0A')", s.write(t));
		assertEquals("('\n'='\n')", su.write(t));
		assertEquals("(\n\t'\n'='\n'\n)", sr.write(t));

		// 3rd level
		t = Json5Map.ofString("{'\n':{'\n':'\n'}}");
		assertEquals("('%0A'=('%0A'='%0A'))", s.write(t));
		assertEquals("('\n'=('\n'='\n'))", su.write(t));
		assertEquals("(\n\t'\n'=(\n\t\t'\n'='\n'\n\t)\n)", sr.write(t));
	}

	//====================================================================================================
	// Unicode characters test
	//====================================================================================================
	@Test void a02_unicodeChars() {

		// 2-byte UTF-8 character
		// Top level
		var t = (Object)"¢";
		assertEquals("%C2%A2", s.write(t));
		assertEquals("¢", su.write(t));
		assertEquals("¢", sr.write(t));

		// 2nd level
		t = Json5Map.ofString("{'¢':'¢'}");
		assertEquals("(%C2%A2=%C2%A2)", s.write(t));
		assertEquals("(¢=¢)", su.write(t));
		assertEquals("(\n\t¢=¢\n)", sr.write(t));

		// 3rd level
		t = Json5Map.ofString("{'¢':{'¢':'¢'}}");
		assertEquals("(%C2%A2=(%C2%A2=%C2%A2))", s.write(t));
		assertEquals("(¢=(¢=¢))", su.write(t));
		assertEquals("(\n\t¢=(\n\t\t¢=¢\n\t)\n)", sr.write(t));

		// 3-byte UTF-8 character
		// Top level
		t = "€";
		assertEquals("%E2%82%AC", s.write(t));
		assertEquals("€", su.write(t));
		assertEquals("€", sr.write(t));

		// 2nd level
		t = Json5Map.ofString("{'€':'€'}");
		assertEquals("(%E2%82%AC=%E2%82%AC)", s.write(t));
		assertEquals("(€=€)", su.write(t));
		assertEquals("(\n\t€=€\n)", sr.write(t));

		// 3rd level
		t = Json5Map.ofString("{'€':{'€':'€'}}");
		assertEquals("(%E2%82%AC=(%E2%82%AC=%E2%82%AC))", s.write(t));
		assertEquals("(€=(€=€))", su.write(t));
		assertEquals("(\n\t€=(\n\t\t€=€\n\t)\n)", sr.write(t));

		// 4-byte UTF-8 character
		// Top level
		t = "𤭢";
		assertEquals("%F0%A4%AD%A2", s.write(t));
		assertEquals("𤭢", su.write(t));
		assertEquals("𤭢", sr.write(t));

		// 2nd level
		t = Json5Map.ofString("{'𤭢':'𤭢'}");
		assertEquals("(%F0%A4%AD%A2=%F0%A4%AD%A2)", s.write(t));
		assertEquals("(𤭢=𤭢)", su.write(t));
		assertEquals("(\n\t𤭢=𤭢\n)", sr.write(t));

		// 3rd level
		t = Json5Map.ofString("{'𤭢':{'𤭢':'𤭢'}}");
		assertEquals("(%F0%A4%AD%A2=(%F0%A4%AD%A2=%F0%A4%AD%A2))", s.write(t));
		assertEquals("(𤭢=(𤭢=𤭢))", su.write(t));
		assertEquals("(\n\t𤭢=(\n\t\t𤭢=𤭢\n\t)\n)", sr.write(t));
	}
}