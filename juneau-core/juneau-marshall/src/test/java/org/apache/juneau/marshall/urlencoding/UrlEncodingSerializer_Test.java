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
package org.apache.juneau.marshall.urlencoding;

import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.commons.utils.Shorts.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.bean.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.json5.*;
import org.junit.jupiter.api.*;

@SuppressWarnings({
	"java:S5961" // High assertion count acceptable in comprehensive test
})
class UrlEncodingSerializer_Test extends TestBase {

	static UrlEncodingSerializer s = UrlEncodingSerializer.DEFAULT.copy().addRootType().build();
	static UrlEncodingSerializer sr = UrlEncodingSerializer.DEFAULT_READABLE.copy().addRootType().build();

	//====================================================================================================
	// Basic test
	//====================================================================================================
	@Test void a01_basic() throws Exception {

		// Simple string
		// Top level
		var t = (Object)"a";
		assertEquals("_value=a", s.write(t));

		// 2nd level
		t = Json5Map.ofString("{a:'a'}");
		assertEquals("a=a", s.write(t));
		assertEquals("a=a", sr.write(t));

		// Simple map
		// Top level
		t = Json5Map.ofString("{a:'b',c:123,d:false,e:true,f:null}");
		assertEquals("a=b&c=123&d=false&e=true&f=null", s.write(t));
		assertEquals("a=b\n&c=123\n&d=false\n&e=true\n&f=null", sr.write(t));

		// 2nd level
		t = Json5Map.ofString("{a:{a:'b',c:123,d:false,e:true,f:null}}");
		assertEquals("a=(a=b,c=123,d=false,e=true,f=null)", s.write(t));
		assertEquals("a=(\n\ta=b,\n\tc=123,\n\td=false,\n\te=true,\n\tf=null\n)", sr.write(t));

		// Simple map with primitives as literals
		t = Json5Map.ofString("{a:'b',c:'123',d:'false',e:'true',f:'null'}");
		assertEquals("a=b&c='123'&d='false'&e='true'&f='null'", s.write(t));
		assertEquals("a=b\n&c='123'\n&d='false'\n&e='true'\n&f='null'", sr.write(t));

		// null
		// Note that writeParams is always encoded.
		// Top level
		t = null;
		assertEquals("_value=null", s.write(t));
		assertEquals("_value=null", sr.write(t));

		// 2nd level
		t = Json5Map.ofString("{null:null}");
		assertEquals("null=null", s.write(t));
		assertEquals("null=null", sr.write(t));

		// 3rd level
		t = Json5Map.ofString("{null:{null:null}}");
		assertEquals("null=(null=null)", s.write(t));
		assertEquals("null=(\n\tnull=null\n)", sr.write(t));

		// Empty array
		// Top level
		t = new String[0];
		assertEquals("", s.write(t));
		assertEquals("", sr.write(t));

		// 2nd level in map
		t = Json5Map.ofString("{x:[]}");
		assertEquals("x=@()", s.write(t));
		assertEquals("x=@()", sr.write(t));

		// Empty 2 dimensional array
		t = new String[1][0];
		assertEquals("0=@()", s.write(t));
		assertEquals("0=@()", sr.write(t));

		// Array containing empty string
		// Top level
		t = a("");
		assertEquals("0=''", s.write(t));
		assertEquals("0=''", sr.write(t));

		// 2nd level
		t = Json5Map.ofString("{x:['']}");
		assertEquals("x=@('')", s.write(t));
		assertEquals("x=@(\n\t''\n)", sr.write(t));

		// Array containing 3 empty strings
		t = a("","","");
		assertEquals("0=''&1=''&2=''", s.write(t));
		assertEquals("0=''\n&1=''\n&2=''", sr.write(t));

		// String containing \u0000
		// Top level
		t = "\u0000";
		assertEquals("_value=%00", s.write(t));
		assertEquals("_value=%00", sr.write(t));

		// 2nd level
		t = Json5Map.ofString("{'\u0000':'\u0000'}");
		assertEquals("%00=%00", s.write(t));
		assertEquals("%00=%00", sr.write(t));

		// Boolean
		// Top level
		t = false;
		assertEquals("_value=false", s.write(t));
		assertEquals("_value=false", sr.write(t));

		// 2nd level
		t = Json5Map.ofString("{x:false}");
		assertEquals("x=false", s.write(t));
		assertEquals("x=false", sr.write(t));

		// Number
		// Top level
		t = 123;
		assertEquals("_value=123", s.write(t));
		assertEquals("_value=123", sr.write(t));

		// 2nd level
		t = Json5Map.ofString("{x:123}");
		assertEquals("x=123", s.write(t));
		assertEquals("x=123", sr.write(t));

		// Unencoded chars
		// Top level
		t = "x;/?:@-_.!*'";
		assertEquals("_value=x;/?:@-_.!*~'", s.write(t));
		assertEquals("_value=x;/?:@-_.!*~'", sr.write(t));

		// 2nd level
		t = Json5Map.ofString("{x:'x;/?:@-_.!*\\''}");
		assertEquals("x=x;/?:@-_.!*~'", s.write(t));
		assertEquals("x=x;/?:@-_.!*~'", sr.write(t));

		// Encoded chars
		// Top level
		t = "x{}|\\^[]`<>#%\"&+";
		assertEquals("_value=x%7B%7D%7C%5C%5E%5B%5D%60%3C%3E%23%25%22%26%2B", s.write(t));
		assertEquals("_value=x%7B%7D%7C%5C%5E%5B%5D%60%3C%3E%23%25%22%26%2B", sr.write(t));

		// 2nd level
		t = Json5Map.ofString("{'x{}|\\\\^[]`<>#%\"&+':'x{}|\\\\^[]`<>#%\"&+'}");
		assertEquals("x%7B%7D%7C%5C%5E%5B%5D%60%3C%3E%23%25%22%26%2B=x%7B%7D%7C%5C%5E%5B%5D%60%3C%3E%23%25%22%26%2B", s.write(t));
		assertEquals("x%7B%7D%7C%5C%5E%5B%5D%60%3C%3E%23%25%22%26%2B=x%7B%7D%7C%5C%5E%5B%5D%60%3C%3E%23%25%22%26%2B", sr.write(t));

		// Escaped chars
		// Top level
		t = "x$,()~";
		assertEquals("_value='x$,()~~'", s.write(t));
		assertEquals("_value='x$,()~~'", sr.write(t));

		// 2nd level
		t = Json5Map.ofString("{'x$,()~':'x$,()~'}");
		assertEquals("'x$,()~~'='x$,()~~'", s.write(t));
		assertEquals("'x$,()~~'='x$,()~~'", sr.write(t));

		// 3rd level
		t = Json5Map.ofString("{'x$,()~':{'x$,()~':'x$,()~'}}");
		assertEquals("'x$,()~~'=('x$,()~~'='x$,()~~')", s.write(t));
		assertEquals("'x$,()~~'=(\n\t'x$,()~~'='x$,()~~'\n)", sr.write(t));

		// Equals sign
		// Gets encoded at top level, and encoded+escaped at 2nd level.
		// Top level
		t = "x=";
		assertEquals("_value='x='", s.write(t));
		assertEquals("_value='x='", sr.write(t));

		// 2nd level
		t = Json5Map.ofString("{'x=':'x='}");
		assertEquals("'x%3D'='x='", s.write(t));
		assertEquals("'x%3D'='x='", sr.write(t));

		// 3rd level
		t = Json5Map.ofString("{'x=':{'x=':'x='}}");
		assertEquals("'x%3D'=('x='='x=')", s.write(t));
		assertEquals("'x%3D'=(\n\t'x='='x='\n)", sr.write(t));

		// String starting with parenthesis
		// Top level
		t = "()";
		assertEquals("_value='()'", s.write(t));
		assertEquals("_value='()'", sr.write(t));

		// 2nd level
		t = Json5Map.ofString("{'()':'()'}");
		assertEquals("'()'='()'", s.write(t));
		assertEquals("'()'='()'", sr.write(t));

		// String starting with $
		// Top level
		t = "$a";
		assertEquals("_value=$a", s.write(t));
		assertEquals("_value=$a", sr.write(t));

		// 2nd level
		t = Json5Map.ofString("{$a:'$a'}");
		assertEquals("$a=$a", s.write(t));
		assertEquals("$a=$a", sr.write(t));

		// Blank string
		// Top level
		t = "";
		assertEquals("_value=''", s.write(t));
		assertEquals("_value=''", sr.write(t));

		// 2nd level
		t = Json5Map.ofString("{'':''}");
		assertEquals("''=''", s.write(t));
		assertEquals("''=''", sr.write(t));

		// 3rd level
		t = Json5Map.ofString("{'':{'':''}}");
		assertEquals("''=(''='')", s.write(t));
		assertEquals("''=(\n\t''=''\n)", sr.write(t));

		// Newline character
		// Top level
		t = "\n";
		assertEquals("_value='%0A'", s.write(t));
		assertEquals("_value='%0A'", sr.write(t));

		// 2nd level
		t = Json5Map.ofString("{'\n':'\n'}");
		assertEquals("'%0A'='%0A'", s.write(t));
		assertEquals("'%0A'='%0A'", sr.write(t));

		// 3rd level
		t = Json5Map.ofString("{'\n':{'\n':'\n'}}");
		assertEquals("'%0A'=('%0A'='%0A')", s.write(t));
		assertEquals("'%0A'=(\n\t'%0A'='%0A'\n)", sr.write(t));
	}

	//====================================================================================================
	// Unicode characters test
	//====================================================================================================
	@Test void a02_unicodeChars() throws Exception {

		// 2-byte UTF-8 character
		// Top level
		var t = (Object)"¢";
		assertEquals("_value=%C2%A2", s.write(t));
		assertEquals("_value=%C2%A2", sr.write(t));

		// 2nd level
		t = Json5Map.ofString("{'¢':'¢'}");
		assertEquals("%C2%A2=%C2%A2", s.write(t));
		assertEquals("%C2%A2=%C2%A2", sr.write(t));

		// 3rd level
		t = Json5Map.ofString("{'¢':{'¢':'¢'}}");
		assertEquals("%C2%A2=(%C2%A2=%C2%A2)", s.write(t));
		assertEquals("%C2%A2=(\n\t%C2%A2=%C2%A2\n)", sr.write(t));

		// 3-byte UTF-8 character
		// Top level
		t = "€";
		assertEquals("_value=%E2%82%AC", s.write(t));
		assertEquals("_value=%E2%82%AC", sr.write(t));

		// 2nd level
		t = Json5Map.ofString("{'€':'€'}");
		assertEquals("%E2%82%AC=%E2%82%AC", s.write(t));
		assertEquals("%E2%82%AC=%E2%82%AC", sr.write(t));

		// 3rd level
		t = Json5Map.ofString("{'€':{'€':'€'}}");
		assertEquals("%E2%82%AC=(%E2%82%AC=%E2%82%AC)", s.write(t));
		assertEquals("%E2%82%AC=(\n\t%E2%82%AC=%E2%82%AC\n)", sr.write(t));

		// 4-byte UTF-8 character
		// Top level
		t = "𤭢";
		assertEquals("_value=%F0%A4%AD%A2", s.write(t));
		assertEquals("_value=%F0%A4%AD%A2", sr.write(t));

		// 2nd level
		t = Json5Map.ofString("{'𤭢':'𤭢'}");
		assertEquals("%F0%A4%AD%A2=%F0%A4%AD%A2", s.write(t));
		assertEquals("%F0%A4%AD%A2=%F0%A4%AD%A2", sr.write(t));

		// 3rd level
		t = Json5Map.ofString("{'𤭢':{'𤭢':'𤭢'}}");
		assertEquals("%F0%A4%AD%A2=(%F0%A4%AD%A2=%F0%A4%AD%A2)", s.write(t));
		assertEquals("%F0%A4%AD%A2=(\n\t%F0%A4%AD%A2=%F0%A4%AD%A2\n)", sr.write(t));
	}

	//====================================================================================================
	// Multi-part parameters on beans via URLENC_expandedParams
	//====================================================================================================
	@Test void a03_multiPartParametersOnBeansViaProperty() throws Exception {
		var t = DTOs.B.create();
		var s2 = UrlEncodingSerializer.DEFAULT;
		var r = s2.write(t);

		var e = """
			f01=@(a,b)\
			&f02=@(c,d)\
			&f03=@(1,2)\
			&f04=@(3,4)\
			&f05=@(@(e,f),@(g,h))\
			&f06=@(@(i,j),@(k,l))\
			&f07=@((a=a,b=1,c=true),(a=a,b=1,c=true))\
			&f08=@((a=a,b=1,c=true),(a=a,b=1,c=true))\
			&f09=@(@((a=a,b=1,c=true)),@((a=a,b=1,c=true)))\
			&f10=@(@((a=a,b=1,c=true)),@((a=a,b=1,c=true)))\
			&f11=@(a,b)\
			&f12=@(c,d)\
			&f13=@(1,2)\
			&f14=@(3,4)\
			&f15=@(@(e,f),@(g,h))\
			&f16=@(@(i,j),@(k,l))\
			&f17=@((a=a,b=1,c=true),(a=a,b=1,c=true))\
			&f18=@((a=a,b=1,c=true),(a=a,b=1,c=true))\
			&f19=@(@((a=a,b=1,c=true)),@((a=a,b=1,c=true)))\
			&f20=@(@((a=a,b=1,c=true)),@((a=a,b=1,c=true)))""";
		assertEquals(e, r);

		s2 = UrlEncodingSerializer.create().expandedParams().build();
		r = s2.write(t);
		e = """
			f01=a&f01=b\
			&f02=c&f02=d\
			&f03=1&f03=2\
			&f04=3&f04=4\
			&f05=@(e,f)&f05=@(g,h)\
			&f06=@(i,j)&f06=@(k,l)\
			&f07=(a=a,b=1,c=true)&f07=(a=a,b=1,c=true)\
			&f08=(a=a,b=1,c=true)&f08=(a=a,b=1,c=true)\
			&f09=@((a=a,b=1,c=true))&f09=@((a=a,b=1,c=true))\
			&f10=@((a=a,b=1,c=true))&f10=@((a=a,b=1,c=true))\
			&f11=a&f11=b\
			&f12=c&f12=d\
			&f13=1&f13=2\
			&f14=3&f14=4\
			&f15=@(e,f)&f15=@(g,h)\
			&f16=@(i,j)&f16=@(k,l)\
			&f17=(a=a,b=1,c=true)&f17=(a=a,b=1,c=true)\
			&f18=(a=a,b=1,c=true)&f18=(a=a,b=1,c=true)\
			&f19=@((a=a,b=1,c=true))&f19=@((a=a,b=1,c=true))\
			&f20=@((a=a,b=1,c=true))&f20=@((a=a,b=1,c=true))""";
		assertEquals(e, r);
	}

	@Test void a04_multiPartParametersOnBeansViaProperty_usingConfig() throws Exception {
		var t = DTOs2.B.create();
		var s2 = UrlEncodingSerializer.DEFAULT.copy().applyAnnotations(DTOs2.Annotations.class).build();
		var r = s2.write(t);

		var e = """
			f01=@(a,b)\
			&f02=@(c,d)\
			&f03=@(1,2)\
			&f04=@(3,4)\
			&f05=@(@(e,f),@(g,h))\
			&f06=@(@(i,j),@(k,l))\
			&f07=@((a=a,b=1,c=true),(a=a,b=1,c=true))\
			&f08=@((a=a,b=1,c=true),(a=a,b=1,c=true))\
			&f09=@(@((a=a,b=1,c=true)),@((a=a,b=1,c=true)))\
			&f10=@(@((a=a,b=1,c=true)),@((a=a,b=1,c=true)))\
			&f11=@(a,b)\
			&f12=@(c,d)\
			&f13=@(1,2)\
			&f14=@(3,4)\
			&f15=@(@(e,f),@(g,h))\
			&f16=@(@(i,j),@(k,l))\
			&f17=@((a=a,b=1,c=true),(a=a,b=1,c=true))\
			&f18=@((a=a,b=1,c=true),(a=a,b=1,c=true))\
			&f19=@(@((a=a,b=1,c=true)),@((a=a,b=1,c=true)))\
			&f20=@(@((a=a,b=1,c=true)),@((a=a,b=1,c=true)))""";
		assertEquals(e, r);

		s2 = UrlEncodingSerializer.create().expandedParams().applyAnnotations(DTOs2.Annotations.class).build();
		r = s2.write(t);
		e = """
			f01=a&f01=b\
			&f02=c&f02=d\
			&f03=1&f03=2\
			&f04=3&f04=4\
			&f05=@(e,f)&f05=@(g,h)\
			&f06=@(i,j)&f06=@(k,l)\
			&f07=(a=a,b=1,c=true)&f07=(a=a,b=1,c=true)\
			&f08=(a=a,b=1,c=true)&f08=(a=a,b=1,c=true)\
			&f09=@((a=a,b=1,c=true))&f09=@((a=a,b=1,c=true))\
			&f10=@((a=a,b=1,c=true))&f10=@((a=a,b=1,c=true))\
			&f11=a&f11=b\
			&f12=c&f12=d\
			&f13=1&f13=2\
			&f14=3&f14=4\
			&f15=@(e,f)&f15=@(g,h)\
			&f16=@(i,j)&f16=@(k,l)\
			&f17=(a=a,b=1,c=true)&f17=(a=a,b=1,c=true)\
			&f18=(a=a,b=1,c=true)&f18=(a=a,b=1,c=true)\
			&f19=@((a=a,b=1,c=true))&f19=@((a=a,b=1,c=true))\
			&f20=@((a=a,b=1,c=true))&f20=@((a=a,b=1,c=true))""";
		assertEquals(e, r);
	}

	//====================================================================================================
	// Multi-part parameters on beans via @UrlEncoding.expandedParams on class
	//====================================================================================================
	@Test void a05_multiPartParametersOnBeansViaAnnotationOnClass() throws Exception {
		var t = DTOs.C.create();
		var s2 = UrlEncodingSerializer.DEFAULT;
		var r = s2.write(t);

		var e = ""
			+ "f01=a&f01=b"
			+ "&f02=c&f02=d"
			+ "&f03=1&f03=2"
			+ "&f04=3&f04=4"
			+ "&f05=@(e,f)&f05=@(g,h)"
			+ "&f06=@(i,j)&f06=@(k,l)"
			+ "&f07=(a=a,b=1,c=true)&f07=(a=a,b=1,c=true)"
			+ "&f08=(a=a,b=1,c=true)&f08=(a=a,b=1,c=true)"
			+ "&f09=@((a=a,b=1,c=true))&f09=@((a=a,b=1,c=true))"
			+ "&f10=@((a=a,b=1,c=true))&f10=@((a=a,b=1,c=true))"
			+ "&f11=a&f11=b"
			+ "&f12=c&f12=d"
			+ "&f13=1&f13=2"
			+ "&f14=3&f14=4"
			+ "&f15=@(e,f)&f15=@(g,h)"
			+ "&f16=@(i,j)&f16=@(k,l)"
			+ "&f17=(a=a,b=1,c=true)&f17=(a=a,b=1,c=true)"
			+ "&f18=(a=a,b=1,c=true)&f18=(a=a,b=1,c=true)"
			+ "&f19=@((a=a,b=1,c=true))&f19=@((a=a,b=1,c=true))"
			+ "&f20=@((a=a,b=1,c=true))&f20=@((a=a,b=1,c=true))";
		assertEquals(e, r);

		s2 = UrlEncodingSerializer.create().expandedParams().build();
		r = s2.write(t);
		e = """
			f01=a&f01=b\
			&f02=c&f02=d\
			&f03=1&f03=2\
			&f04=3&f04=4\
			&f05=@(e,f)&f05=@(g,h)\
			&f06=@(i,j)&f06=@(k,l)\
			&f07=(a=a,b=1,c=true)&f07=(a=a,b=1,c=true)\
			&f08=(a=a,b=1,c=true)&f08=(a=a,b=1,c=true)\
			&f09=@((a=a,b=1,c=true))&f09=@((a=a,b=1,c=true))\
			&f10=@((a=a,b=1,c=true))&f10=@((a=a,b=1,c=true))\
			&f11=a&f11=b\
			&f12=c&f12=d\
			&f13=1&f13=2\
			&f14=3&f14=4\
			&f15=@(e,f)&f15=@(g,h)\
			&f16=@(i,j)&f16=@(k,l)\
			&f17=(a=a,b=1,c=true)&f17=(a=a,b=1,c=true)\
			&f18=(a=a,b=1,c=true)&f18=(a=a,b=1,c=true)\
			&f19=@((a=a,b=1,c=true))&f19=@((a=a,b=1,c=true))\
			&f20=@((a=a,b=1,c=true))&f20=@((a=a,b=1,c=true))""";
		assertEquals(e, r);
	}

	@Test void a06_multiPartParametersOnBeansViaAnnotationOnClass_usingConfig() throws Exception {
		var t = DTOs2.C.create();
		var s2 = UrlEncodingSerializer.DEFAULT.copy().applyAnnotations(DTOs2.Annotations.class).build();
		var r = s2.write(t);

		var e = ""
			+ "f01=a&f01=b"
			+ "&f02=c&f02=d"
			+ "&f03=1&f03=2"
			+ "&f04=3&f04=4"
			+ "&f05=@(e,f)&f05=@(g,h)"
			+ "&f06=@(i,j)&f06=@(k,l)"
			+ "&f07=(a=a,b=1,c=true)&f07=(a=a,b=1,c=true)"
			+ "&f08=(a=a,b=1,c=true)&f08=(a=a,b=1,c=true)"
			+ "&f09=@((a=a,b=1,c=true))&f09=@((a=a,b=1,c=true))"
			+ "&f10=@((a=a,b=1,c=true))&f10=@((a=a,b=1,c=true))"
			+ "&f11=a&f11=b"
			+ "&f12=c&f12=d"
			+ "&f13=1&f13=2"
			+ "&f14=3&f14=4"
			+ "&f15=@(e,f)&f15=@(g,h)"
			+ "&f16=@(i,j)&f16=@(k,l)"
			+ "&f17=(a=a,b=1,c=true)&f17=(a=a,b=1,c=true)"
			+ "&f18=(a=a,b=1,c=true)&f18=(a=a,b=1,c=true)"
			+ "&f19=@((a=a,b=1,c=true))&f19=@((a=a,b=1,c=true))"
			+ "&f20=@((a=a,b=1,c=true))&f20=@((a=a,b=1,c=true))";
		assertEquals(e, r);

		s2 = UrlEncodingSerializer.create().expandedParams().applyAnnotations(DTOs2.Annotations.class).build();
		r = s2.write(t);
		e = """
			f01=a&f01=b\
			&f02=c&f02=d\
			&f03=1&f03=2\
			&f04=3&f04=4\
			&f05=@(e,f)&f05=@(g,h)\
			&f06=@(i,j)&f06=@(k,l)\
			&f07=(a=a,b=1,c=true)&f07=(a=a,b=1,c=true)\
			&f08=(a=a,b=1,c=true)&f08=(a=a,b=1,c=true)\
			&f09=@((a=a,b=1,c=true))&f09=@((a=a,b=1,c=true))\
			&f10=@((a=a,b=1,c=true))&f10=@((a=a,b=1,c=true))\
			&f11=a&f11=b\
			&f12=c&f12=d\
			&f13=1&f13=2\
			&f14=3&f14=4\
			&f15=@(e,f)&f15=@(g,h)\
			&f16=@(i,j)&f16=@(k,l)\
			&f17=(a=a,b=1,c=true)&f17=(a=a,b=1,c=true)\
			&f18=(a=a,b=1,c=true)&f18=(a=a,b=1,c=true)\
			&f19=@((a=a,b=1,c=true))&f19=@((a=a,b=1,c=true))\
			&f20=@((a=a,b=1,c=true))&f20=@((a=a,b=1,c=true))""";
		assertEquals(e, r);
	}

	@Test void a07_multiPartParametersOnMapOfStringArrays() throws Exception {
		var t = map();
		t.put("f1", a("bar"));
		t.put("f2", a("bar","baz"));
		t.put("f3", a());
		var s2 = UrlEncodingSerializer.DEFAULT_EXPANDED;
		var r = s2.write(t);
		var e = "f1=bar&f2=bar&f2=baz";
		assertEquals(e, r);
	}

	//====================================================================================================
	// Test URLENC_paramFormat == PLAINTEXT.
	//====================================================================================================
	@Test void a08_plainTextParams() throws Exception {
		var s2 = UrlEncodingSerializer.DEFAULT.copy().paramFormatPlain().build();

		assertEquals("_value=foo", s2.write("foo"));
		assertEquals("_value='foo'", s2.write("'foo'"));
		assertEquals("_value=(foo)", s2.write("(foo)"));
		assertEquals("_value=@(foo)", s2.write("@(foo)"));

		var m = mapBuilder(String.class,Object.class).add("foo","foo").add("'foo'","'foo'").add("(foo)","(foo)").add("@(foo)","@(foo)").build();
		assertEquals("foo=foo&'foo'='foo'&(foo)=(foo)&@(foo)=@(foo)", s2.write(m));

		var l = l("foo", "'foo'", "(foo)", "@(foo)");
		assertEquals("0=foo&1='foo'&2=(foo)&3=@(foo)", s2.write(l));

		var a = new A();
		assertEquals("'foo'='foo'&(foo)=(foo)&@(foo)=@(foo)&foo=foo", s2.write(a));
	}

	@Marshalled
	public static class A {

		@BeanProp(name="foo")
		public String f1 = "foo";

		@BeanProp(name="'foo'")
		public String f2 = "'foo'";

		@BeanProp(name="(foo)")
		public String f3 = "(foo)";

		@BeanProp(name="@(foo)")
		public String f4 = "@(foo)";
	}
}