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
package org.apache.juneau.uon;

import static org.junit.Assert.*;
import org.apache.juneau.collections.*;
import org.junit.jupiter.api.*;

class UonSerializerTest {

	static UonSerializer s = UonSerializer.DEFAULT_ENCODING;
	static UonSerializer su = UonSerializer.DEFAULT;
	static UonSerializer sr = UonSerializer.DEFAULT_READABLE;

	//====================================================================================================
	// Basic test
	//====================================================================================================
	@Test void testBasic() throws Exception {

		Object t;

		// Simple string
		// Top level
		t = "a";
		assertEquals("a", s.serialize(t));
		assertEquals("a", su.serialize(t));
		assertEquals("a", sr.serialize(t));

		// 2nd level
		t = JsonMap.ofJson("{a:'a'}");
		assertEquals("(a=a)", s.serialize(t));
		assertEquals("(a=a)", su.serialize(t));
		assertEquals("(\n\ta=a\n)", sr.serialize(t));

		// Simple map
		// Top level
		t = JsonMap.ofJson("{a:'b',c:123,d:false,e:true,f:null}");
		assertEquals("(a=b,c=123,d=false,e=true,f=null)", s.serialize(t));
		assertEquals("(a=b,c=123,d=false,e=true,f=null)", su.serialize(t));
		assertEquals("(\n\ta=b,\n\tc=123,\n\td=false,\n\te=true,\n\tf=null\n)", sr.serialize(t));

		// 2nd level
		t = JsonMap.ofJson("{a:{a:'b',c:123,d:false,e:true,f:null}}");
		assertEquals("(a=(a=b,c=123,d=false,e=true,f=null))", s.serialize(t));
		assertEquals("(a=(a=b,c=123,d=false,e=true,f=null))", su.serialize(t));
		assertEquals("(\n\ta=(\n\t\ta=b,\n\t\tc=123,\n\t\td=false,\n\t\te=true,\n\t\tf=null\n\t)\n)", sr.serialize(t));

		// Simple map with primitives as literals
		t = JsonMap.ofJson("{a:'b',c:'123',d:'false',e:'true',f:'null'}");
		assertEquals("(a=b,c='123',d='false',e='true',f='null')", s.serialize(t));
		assertEquals("(a=b,c='123',d='false',e='true',f='null')", su.serialize(t));
		assertEquals("(\n\ta=b,\n\tc='123',\n\td='false',\n\te='true',\n\tf='null'\n)", sr.serialize(t));

		// null
		// Note that serializeParams is always encoded.
		// Top level
		t = null;
		assertEquals("null", s.serialize(t));
		assertEquals("null", su.serialize(t));
		assertEquals("null", sr.serialize(t));

		// 2nd level
		t = JsonMap.ofJson("{null:null}");
		assertEquals("(null=null)", s.serialize(t));
		assertEquals("(null=null)", su.serialize(t));
		assertEquals("(\n\tnull=null\n)", sr.serialize(t));

		// 3rd level
		t = JsonMap.ofJson("{null:{null:null}}");
		assertEquals("(null=(null=null))", s.serialize(t));
		assertEquals("(null=(null=null))", su.serialize(t));
		assertEquals("(\n\tnull=(\n\t\tnull=null\n\t)\n)", sr.serialize(t));

		// Empty array
		// Top level
		t = new String[0];
		assertEquals("@()", s.serialize(t));
		assertEquals("@()", su.serialize(t));
		assertEquals("@()", sr.serialize(t));

		// 2nd level in map
		t = JsonMap.ofJson("{x:[]}");
		assertEquals("(x=@())", s.serialize(t));
		assertEquals("(x=@())", su.serialize(t));
		assertEquals("(\n\tx=@()\n)", sr.serialize(t));

		// Empty 2 dimensional array
		t = new String[1][0];
		assertEquals("@(@())", s.serialize(t));
		assertEquals("@(@())", su.serialize(t));
		assertEquals("@(\n\t@()\n)", sr.serialize(t));

		// Array containing empty string
		// Top level
		t = new String[]{""};
		assertEquals("@('')", s.serialize(t));
		assertEquals("@('')", su.serialize(t));
		assertEquals("@(\n\t''\n)", sr.serialize(t));

		// 2nd level
		t = JsonMap.ofJson("{x:['']}");
		assertEquals("(x=@(''))", s.serialize(t));
		assertEquals("(x=@(''))", su.serialize(t));
		assertEquals("(\n\tx=@(\n\t\t''\n\t)\n)", sr.serialize(t));

		// Array containing 3 empty strings
		t = new String[]{"","",""};
		assertEquals("@('','','')", s.serialize(t));
		assertEquals("@('','','')", su.serialize(t));
		assertEquals("@(\n\t'',\n\t'',\n\t''\n)", sr.serialize(t));

		// String containing \u0000
		// Top level
		t = "\u0000";
		assertEquals("%00", s.serialize(t));
		assertEquals("\u0000", su.serialize(t));
		assertEquals("\u0000", sr.serialize(t));

		// 2nd level
		t = JsonMap.ofJson("{'\u0000':'\u0000'}");
		assertEquals("(%00=%00)", s.serialize(t));
		assertEquals("(\u0000=\u0000)", su.serialize(t));
		assertEquals("(\n\t\u0000=\u0000\n)", sr.serialize(t));

		// Boolean
		// Top level
		t = false;
		assertEquals("false", s.serialize(t));
		assertEquals("false", su.serialize(t));
		assertEquals("false", sr.serialize(t));

		// 2nd level
		t = JsonMap.ofJson("{x:false}");
		assertEquals("(x=false)", s.serialize(t));
		assertEquals("(x=false)", su.serialize(t));
		assertEquals("(\n\tx=false\n)", sr.serialize(t));

		// Number
		// Top level
		t = 123;
		assertEquals("123", s.serialize(t));
		assertEquals("123", su.serialize(t));
		assertEquals("123", sr.serialize(t));

		// 2nd level
		t = JsonMap.ofJson("{x:123}");
		assertEquals("(x=123)", s.serialize(t));
		assertEquals("(x=123)", su.serialize(t));
		assertEquals("(\n\tx=123\n)", sr.serialize(t));

		// Unencoded chars
		// Top level
		t = "x;/?:@-_.!*'";
		assertEquals("x;/?:@-_.!*~'", s.serialize(t));
		assertEquals("x;/?:@-_.!*~'", su.serialize(t));
		assertEquals("x;/?:@-_.!*~'", sr.serialize(t));

		// 2nd level
		t = JsonMap.ofJson("{x:'x;/?:@-_.!*\\''}");
		assertEquals("(x=x;/?:@-_.!*~')", s.serialize(t));
		assertEquals("(x=x;/?:@-_.!*~')", su.serialize(t));
		assertEquals("(\n\tx=x;/?:@-_.!*~'\n)", sr.serialize(t));

		// Encoded chars
		// Top level
		t = "x{}|\\^[]`<>#%\"&+";
		assertEquals("x%7B%7D%7C%5C%5E%5B%5D%60%3C%3E%23%25%22%26%2B", s.serialize(t));
		assertEquals("x{}|\\^[]`<>#%\"&+", su.serialize(t));
		assertEquals("x{}|\\^[]`<>#%\"&+", sr.serialize(t));

		// 2nd level
		t = JsonMap.ofJson("{'x{}|\\\\^[]`<>#%\"&+':'x{}|\\\\^[]`<>#%\"&+'}");
		assertEquals("(x%7B%7D%7C%5C%5E%5B%5D%60%3C%3E%23%25%22%26%2B=x%7B%7D%7C%5C%5E%5B%5D%60%3C%3E%23%25%22%26%2B)", s.serialize(t));
		assertEquals("(x{}|\\^[]`<>#%\"&+=x{}|\\^[]`<>#%\"&+)", su.serialize(t));
		assertEquals("(\n\tx{}|\\^[]`<>#%\"&+=x{}|\\^[]`<>#%\"&+\n)", sr.serialize(t));

		// Escaped chars
		// Top level
		t = "x$,()~";
		assertEquals("'x$,()~~'", s.serialize(t));
		assertEquals("'x$,()~~'", su.serialize(t));
		assertEquals("'x$,()~~'", sr.serialize(t));

		// 2nd level
		// Note behavior on serializeParams() is different since 2nd-level is top level.
		t = JsonMap.ofJson("{'x$,()~':'x$,()~'}");
		assertEquals("('x$,()~~'='x$,()~~')", s.serialize(t));
		assertEquals("('x$,()~~'='x$,()~~')", su.serialize(t));
		assertEquals("(\n\t'x$,()~~'='x$,()~~'\n)", sr.serialize(t));

		// 3rd level
		// Note behavior on serializeParams().
		t = JsonMap.ofJson("{'x$,()~':{'x$,()~':'x$,()~'}}");
		assertEquals("('x$,()~~'=('x$,()~~'='x$,()~~'))", s.serialize(t));
		assertEquals("('x$,()~~'=('x$,()~~'='x$,()~~'))", su.serialize(t));
		assertEquals("(\n\t'x$,()~~'=(\n\t\t'x$,()~~'='x$,()~~'\n\t)\n)", sr.serialize(t));

		// Equals sign
		// Gets encoded at top level, and encoded+escaped at 2nd level.
		// Top level
		t = "x=";
		assertEquals("'x='", s.serialize(t));
		assertEquals("'x='", su.serialize(t));
		assertEquals("'x='", sr.serialize(t));

		// 2nd level
		t = JsonMap.ofJson("{'x=':'x='}");
		assertEquals("('x='='x=')", s.serialize(t));
		assertEquals("('x='='x=')", su.serialize(t));
		assertEquals("(\n\t'x='='x='\n)", sr.serialize(t));

		// 3rd level
		t = JsonMap.ofJson("{'x=':{'x=':'x='}}");
		assertEquals("('x='=('x='='x='))", s.serialize(t));
		assertEquals("('x='=('x='='x='))", su.serialize(t));
		assertEquals("(\n\t'x='=(\n\t\t'x='='x='\n\t)\n)", sr.serialize(t));

		// String starting with parenthesis
		// Top level
		t = "()";
		assertEquals("'()'", s.serialize(t));
		assertEquals("'()'", su.serialize(t));
		assertEquals("'()'", sr.serialize(t));

		// 2nd level
		t = JsonMap.ofJson("{'()':'()'}");
		assertEquals("('()'='()')", s.serialize(t));
		assertEquals("('()'='()')", su.serialize(t));
		assertEquals("(\n\t'()'='()'\n)", sr.serialize(t));

		// String starting with $
		// Top level
		t = "$a";
		assertEquals("$a", s.serialize(t));
		assertEquals("$a", su.serialize(t));
		assertEquals("$a", sr.serialize(t));

		// 2nd level
		t = JsonMap.ofJson("{$a:'$a'}");
		assertEquals("($a=$a)", s.serialize(t));
		assertEquals("($a=$a)", su.serialize(t));
		assertEquals("(\n\t$a=$a\n)", sr.serialize(t));

		// Blank string
		// Top level
		t = "";
		assertEquals("''", s.serialize(t));
		assertEquals("''", su.serialize(t));
		assertEquals("''", sr.serialize(t));

		// 2nd level
		t = JsonMap.ofJson("{'':''}");
		assertEquals("(''='')", s.serialize(t));
		assertEquals("(''='')", su.serialize(t));
		assertEquals("(\n\t''=''\n)", sr.serialize(t));

		// 3rd level
		t = JsonMap.ofJson("{'':{'':''}}");
		assertEquals("(''=(''=''))", s.serialize(t));
		assertEquals("(''=(''=''))", su.serialize(t));
		assertEquals("(\n\t''=(\n\t\t''=''\n\t)\n)", sr.serialize(t));

		// Newline character
		// Top level
		t = "\n";
		assertEquals("'%0A'", s.serialize(t));
		assertEquals("'\n'", su.serialize(t));
		assertEquals("'\n'", sr.serialize(t));

		// 2nd level
		t = JsonMap.ofJson("{'\n':'\n'}");
		assertEquals("('%0A'='%0A')", s.serialize(t));
		assertEquals("('\n'='\n')", su.serialize(t));
		assertEquals("(\n\t'\n'='\n'\n)", sr.serialize(t));

		// 3rd level
		t = JsonMap.ofJson("{'\n':{'\n':'\n'}}");
		assertEquals("('%0A'=('%0A'='%0A'))", s.serialize(t));
		assertEquals("('\n'=('\n'='\n'))", su.serialize(t));
		assertEquals("(\n\t'\n'=(\n\t\t'\n'='\n'\n\t)\n)", sr.serialize(t));
	}

	//====================================================================================================
	// Unicode characters test
	//====================================================================================================
	@Test void testUnicodeChars() throws Exception {
		Object t;

		// 2-byte UTF-8 character
		// Top level
		t = "¢";
		assertEquals("%C2%A2", s.serialize(t));
		assertEquals("¢", su.serialize(t));
		assertEquals("¢", sr.serialize(t));

		// 2nd level
		t = JsonMap.ofJson("{'¢':'¢'}");
		assertEquals("(%C2%A2=%C2%A2)", s.serialize(t));
		assertEquals("(¢=¢)", su.serialize(t));
		assertEquals("(\n\t¢=¢\n)", sr.serialize(t));

		// 3rd level
		t = JsonMap.ofJson("{'¢':{'¢':'¢'}}");
		assertEquals("(%C2%A2=(%C2%A2=%C2%A2))", s.serialize(t));
		assertEquals("(¢=(¢=¢))", su.serialize(t));
		assertEquals("(\n\t¢=(\n\t\t¢=¢\n\t)\n)", sr.serialize(t));

		// 3-byte UTF-8 character
		// Top level
		t = "€";
		assertEquals("%E2%82%AC", s.serialize(t));
		assertEquals("€", su.serialize(t));
		assertEquals("€", sr.serialize(t));

		// 2nd level
		t = JsonMap.ofJson("{'€':'€'}");
		assertEquals("(%E2%82%AC=%E2%82%AC)", s.serialize(t));
		assertEquals("(€=€)", su.serialize(t));
		assertEquals("(\n\t€=€\n)", sr.serialize(t));

		// 3rd level
		t = JsonMap.ofJson("{'€':{'€':'€'}}");
		assertEquals("(%E2%82%AC=(%E2%82%AC=%E2%82%AC))", s.serialize(t));
		assertEquals("(€=(€=€))", su.serialize(t));
		assertEquals("(\n\t€=(\n\t\t€=€\n\t)\n)", sr.serialize(t));

		// 4-byte UTF-8 character
		// Top level
		t = "𤭢";
		assertEquals("%F0%A4%AD%A2", s.serialize(t));
		assertEquals("𤭢", su.serialize(t));
		assertEquals("𤭢", sr.serialize(t));

		// 2nd level
		t = JsonMap.ofJson("{'𤭢':'𤭢'}");
		assertEquals("(%F0%A4%AD%A2=%F0%A4%AD%A2)", s.serialize(t));
		assertEquals("(𤭢=𤭢)", su.serialize(t));
		assertEquals("(\n\t𤭢=𤭢\n)", sr.serialize(t));

		// 3rd level
		t = JsonMap.ofJson("{'𤭢':{'𤭢':'𤭢'}}");
		assertEquals("(%F0%A4%AD%A2=(%F0%A4%AD%A2=%F0%A4%AD%A2))", s.serialize(t));
		assertEquals("(𤭢=(𤭢=𤭢))", su.serialize(t));
		assertEquals("(\n\t𤭢=(\n\t\t𤭢=𤭢\n\t)\n)", sr.serialize(t));
	}
}