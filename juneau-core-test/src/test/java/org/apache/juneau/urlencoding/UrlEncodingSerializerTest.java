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
package org.apache.juneau.urlencoding;

import static org.junit.Assert.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.json.JsonSerializer;
import org.junit.*;

@SuppressWarnings("javadoc")
public class UrlEncodingSerializerTest {

	static UrlEncodingSerializer s = UrlEncodingSerializer.DEFAULT;
	static UrlEncodingSerializer sr = UrlEncodingSerializer.DEFAULT_READABLE;


	//====================================================================================================
	// Basic test
	//====================================================================================================
	@Test
	public void testBasic() throws Exception {

		Object t;

		// Simple string
		// Top level
		t = "a";
		assertEquals("_value=a", s.serialize(t));

		// 2nd level
		t = new ObjectMap("{a:'a'}");
		assertEquals("a=a", s.serialize(t));
		assertEquals("a=a", sr.serialize(t));

		// Simple map
		// Top level
		t = new ObjectMap("{a:'b',c:123,d:false,e:true,f:null}");
		assertEquals("a=b&c=123&d=false&e=true&f=null", s.serialize(t));
		assertEquals("a=b\n&c=123\n&d=false\n&e=true\n&f=null", sr.serialize(t));

		// 2nd level
		t = new ObjectMap("{a:{a:'b',c:123,d:false,e:true,f:null}}");
		assertEquals("a=(a=b,c=123,d=false,e=true,f=null)", s.serialize(t));
		assertEquals("a=(\n\ta=b,\n\tc=123,\n\td=false,\n\te=true,\n\tf=null\n)", sr.serialize(t));

		// Simple map with primitives as literals
		t = new ObjectMap("{a:'b',c:'123',d:'false',e:'true',f:'null'}");
		assertEquals("a=b&c='123'&d='false'&e='true'&f='null'", s.serialize(t));
		assertEquals("a=b\n&c='123'\n&d='false'\n&e='true'\n&f='null'", sr.serialize(t));

		// null
		// Note that serializeParams is always encoded.
		// Top level
		t = null;
		assertEquals("_value=null", s.serialize(t));
		assertEquals("_value=null", sr.serialize(t));

		// 2nd level
		t = new ObjectMap("{null:null}");
		assertEquals("null=null", s.serialize(t));
		assertEquals("null=null", sr.serialize(t));

		// 3rd level
		t = new ObjectMap("{null:{null:null}}");
		assertEquals("null=(null=null)", s.serialize(t));
		assertEquals("null=(\n\tnull=null\n)", sr.serialize(t));

		// Empty array
		// Top level
		t = new String[0];
		assertEquals("_value=@()", s.serialize(t));
		assertEquals("_value=@()", sr.serialize(t));

		// 2nd level in map
		t = new ObjectMap("{x:[]}");
		assertEquals("x=@()", s.serialize(t));
		assertEquals("x=@()", sr.serialize(t));

		// Empty 2 dimensional array
		t = new String[1][0];
		assertEquals("_value=@(@())", s.serialize(t));
		assertEquals("_value=@(\n\t@()\n)", sr.serialize(t));

		// Array containing empty string
		// Top level
		t = new String[]{""};
		assertEquals("_value=@('')", s.serialize(t));
		assertEquals("_value=@(\n\t''\n)", sr.serialize(t));

		// 2nd level
		t = new ObjectMap("{x:['']}");
		assertEquals("x=@('')", s.serialize(t));
		assertEquals("x=@(\n\t''\n)", sr.serialize(t));

		// Array containing 3 empty strings
		t = new String[]{"","",""};
		assertEquals("_value=@('','','')", s.serialize(t));
		assertEquals("_value=@(\n\t'',\n\t'',\n\t''\n)", sr.serialize(t));

		// String containing \u0000
		// Top level
		t = "\u0000";
		assertEquals("_value=%00", s.serialize(t));
		assertEquals("_value=%00", sr.serialize(t));

		// 2nd level
		t = new ObjectMap("{'\u0000':'\u0000'}");
		assertEquals("%00=%00", s.serialize(t));
		assertEquals("%00=%00", sr.serialize(t));

		// Boolean
		// Top level
		t = false;
		assertEquals("_value=false", s.serialize(t));
		assertEquals("_value=false", sr.serialize(t));

		// 2nd level
		t = new ObjectMap("{x:false}");
		assertEquals("x=false", s.serialize(t));
		assertEquals("x=false", sr.serialize(t));

		// Number
		// Top level
		t = 123;
		assertEquals("_value=123", s.serialize(t));
		assertEquals("_value=123", sr.serialize(t));

		// 2nd level
		t = new ObjectMap("{x:123}");
		assertEquals("x=123", s.serialize(t));
		assertEquals("x=123", sr.serialize(t));

		// Unencoded chars
		// Top level
		t = "x;/?:@-_.!*'";
		assertEquals("_value=x;/?:@-_.!*~'", s.serialize(t));
		assertEquals("_value=x;/?:@-_.!*~'", sr.serialize(t));

		// 2nd level
		t = new ObjectMap("{x:'x;/?:@-_.!*\\''}");
		assertEquals("x=x;/?:@-_.!*~'", s.serialize(t));
		assertEquals("x=x;/?:@-_.!*~'", sr.serialize(t));

		// Encoded chars
		// Top level
		t = "x{}|\\^[]`<>#%\"&+";
		assertEquals("_value=x%7B%7D%7C%5C%5E%5B%5D%60%3C%3E%23%25%22%26%2B", s.serialize(t));
		assertEquals("_value=x%7B%7D%7C%5C%5E%5B%5D%60%3C%3E%23%25%22%26%2B", sr.serialize(t));

		// 2nd level
		t = new ObjectMap("{'x{}|\\\\^[]`<>#%\"&+':'x{}|\\\\^[]`<>#%\"&+'}");
		assertEquals("x%7B%7D%7C%5C%5E%5B%5D%60%3C%3E%23%25%22%26%2B=x%7B%7D%7C%5C%5E%5B%5D%60%3C%3E%23%25%22%26%2B", s.serialize(t));
		assertEquals("x%7B%7D%7C%5C%5E%5B%5D%60%3C%3E%23%25%22%26%2B=x%7B%7D%7C%5C%5E%5B%5D%60%3C%3E%23%25%22%26%2B", sr.serialize(t));

		// Escaped chars
		// Top level
		t = "x$,()~";
		assertEquals("_value='x$,()~~'", s.serialize(t));
		assertEquals("_value='x$,()~~'", sr.serialize(t));

		// 2nd level
		t = new ObjectMap("{'x$,()~':'x$,()~'}");
		assertEquals("'x$,()~~'='x$,()~~'", s.serialize(t));
		assertEquals("'x$,()~~'='x$,()~~'", sr.serialize(t));

		// 3rd level
		t = new ObjectMap("{'x$,()~':{'x$,()~':'x$,()~'}}");
		assertEquals("'x$,()~~'=('x$,()~~'='x$,()~~')", s.serialize(t));
		assertEquals("'x$,()~~'=(\n\t'x$,()~~'='x$,()~~'\n)", sr.serialize(t));

		// Equals sign
		// Gets encoded at top level, and encoded+escaped at 2nd level.
		// Top level
		t = "x=";
		assertEquals("_value='x='", s.serialize(t));
		assertEquals("_value='x='", sr.serialize(t));

		// 2nd level
		t = new ObjectMap("{'x=':'x='}");
		assertEquals("'x%3D'='x='", s.serialize(t));
		assertEquals("'x%3D'='x='", sr.serialize(t));

		// 3rd level
		t = new ObjectMap("{'x=':{'x=':'x='}}");
		assertEquals("'x%3D'=('x='='x=')", s.serialize(t));
		assertEquals("'x%3D'=(\n\t'x='='x='\n)", sr.serialize(t));

		// String starting with parenthesis
		// Top level
		t = "()";
		assertEquals("_value='()'", s.serialize(t));
		assertEquals("_value='()'", sr.serialize(t));

		// 2nd level
		t = new ObjectMap("{'()':'()'}");
		assertEquals("'()'='()'", s.serialize(t));
		assertEquals("'()'='()'", sr.serialize(t));

		// String starting with $
		// Top level
		t = "$a";
		assertEquals("_value=$a", s.serialize(t));
		assertEquals("_value=$a", sr.serialize(t));

		// 2nd level
		t = new ObjectMap("{$a:'$a'}");
		assertEquals("$a=$a", s.serialize(t));
		assertEquals("$a=$a", sr.serialize(t));

		// Blank string
		// Top level
		t = "";
		assertEquals("_value=''", s.serialize(t));
		assertEquals("_value=''", sr.serialize(t));

		// 2nd level
		t = new ObjectMap("{'':''}");
		assertEquals("''=''", s.serialize(t));
		assertEquals("''=''", sr.serialize(t));

		// 3rd level
		t = new ObjectMap("{'':{'':''}}");
		assertEquals("''=(''='')", s.serialize(t));
		assertEquals("''=(\n\t''=''\n)", sr.serialize(t));

		// Newline character
		// Top level
		t = "\n";
		assertEquals("_value='%0A'", s.serialize(t));
		assertEquals("_value='%0A'", sr.serialize(t));

		// 2nd level
		t = new ObjectMap("{'\n':'\n'}");
		assertEquals("'%0A'='%0A'", s.serialize(t));
		assertEquals("'%0A'='%0A'", sr.serialize(t));

		// 3rd level
		t = new ObjectMap("{'\n':{'\n':'\n'}}");
		assertEquals("'%0A'=('%0A'='%0A')", s.serialize(t));
		assertEquals("'%0A'=(\n\t'%0A'='%0A'\n)", sr.serialize(t));
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
		assertEquals("_value=%C2%A2", s.serialize(t));
		assertEquals("_value=%C2%A2", sr.serialize(t));

		// 2nd level
		t = new ObjectMap("{'¢':'¢'}");
		assertEquals("%C2%A2=%C2%A2", s.serialize(t));
		assertEquals("%C2%A2=%C2%A2", sr.serialize(t));

		// 3rd level
		t = new ObjectMap("{'¢':{'¢':'¢'}}");
		assertEquals("%C2%A2=(%C2%A2=%C2%A2)", s.serialize(t));
		assertEquals("%C2%A2=(\n\t%C2%A2=%C2%A2\n)", sr.serialize(t));

		// 3-byte UTF-8 character
		// Top level
		t = "€";
		assertEquals("_value=%E2%82%AC", s.serialize(t));
		assertEquals("_value=%E2%82%AC", sr.serialize(t));

		// 2nd level
		t = new ObjectMap("{'€':'€'}");
		assertEquals("%E2%82%AC=%E2%82%AC", s.serialize(t));
		assertEquals("%E2%82%AC=%E2%82%AC", sr.serialize(t));

		// 3rd level
		t = new ObjectMap("{'€':{'€':'€'}}");
		assertEquals("%E2%82%AC=(%E2%82%AC=%E2%82%AC)", s.serialize(t));
		assertEquals("%E2%82%AC=(\n\t%E2%82%AC=%E2%82%AC\n)", sr.serialize(t));

		// 4-byte UTF-8 character
		// Top level
		t = "𤭢";
		assertEquals("_value=%F0%A4%AD%A2", s.serialize(t));
		assertEquals("_value=%F0%A4%AD%A2", sr.serialize(t));

		// 2nd level
		t = new ObjectMap("{'𤭢':'𤭢'}");
		assertEquals("%F0%A4%AD%A2=%F0%A4%AD%A2", s.serialize(t));
		assertEquals("%F0%A4%AD%A2=%F0%A4%AD%A2", sr.serialize(t));

		// 3rd level
		t = new ObjectMap("{'𤭢':{'𤭢':'𤭢'}}");
		assertEquals("%F0%A4%AD%A2=(%F0%A4%AD%A2=%F0%A4%AD%A2)", s.serialize(t));
		assertEquals("%F0%A4%AD%A2=(\n\t%F0%A4%AD%A2=%F0%A4%AD%A2\n)", sr.serialize(t));
	}

	//====================================================================================================
	// Multi-part parameters on beans via URLENC_expandedParams
	//====================================================================================================
	@Test
	public void testMultiPartParametersOnBeansViaProperty() throws Exception {
		UrlEncodingSerializer s;
		DTOs.B t = DTOs.B.create();
		String r;

		s = UrlEncodingSerializer.DEFAULT;
		r = s.serialize(t);
		String e = ""
			+ "f01=@(a,b)"
			+ "&f02=@(c,d)"
			+ "&f03=@(1,2)"
			+ "&f04=@(3,4)"
			+ "&f05=@(@(e,f),@(g,h))"
			+ "&f06=@(@(i,j),@(k,l))"
			+ "&f07=@((a=a,b=1,c=true),(a=a,b=1,c=true))"
			+ "&f08=@((a=a,b=1,c=true),(a=a,b=1,c=true))"
			+ "&f09=@(@((a=a,b=1,c=true)),@((a=a,b=1,c=true)))"
			+ "&f10=@(@((a=a,b=1,c=true)),@((a=a,b=1,c=true)))"
			+ "&f11=@(a,b)"
			+ "&f12=@(c,d)"
			+ "&f13=@(1,2)"
			+ "&f14=@(3,4)"
			+ "&f15=@(@(e,f),@(g,h))"
			+ "&f16=@(@(i,j),@(k,l))"
			+ "&f17=@((a=a,b=1,c=true),(a=a,b=1,c=true))"
			+ "&f18=@((a=a,b=1,c=true),(a=a,b=1,c=true))"
			+ "&f19=@(@((a=a,b=1,c=true)),@((a=a,b=1,c=true)))"
			+ "&f20=@(@((a=a,b=1,c=true)),@((a=a,b=1,c=true)))";
		assertEquals(e, r);

		s = UrlEncodingSerializer.DEFAULT.clone().setExpandedParams(true);
		r = s.serialize(t);
		e = ""
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
	}


	//====================================================================================================
	// Multi-part parameters on beans via @UrlEncoding.expandedParams on class
	//====================================================================================================
	@Test
	public void testMultiPartParametersOnBeansViaAnnotationOnClass() throws Exception {
		UrlEncodingSerializer s;
		DTOs.C t = DTOs.C.create();
		String r;

		s = UrlEncodingSerializer.DEFAULT;
		r = s.serialize(t);
		String e = ""
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

		s = UrlEncodingSerializer.DEFAULT.clone().setExpandedParams(true);
		r = s.serialize(t);
		e = ""
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
	}

	@Test
	public void testMultiPartParametersOnMapOfStringArrays() throws Exception {
		UrlEncodingSerializer s;
		String r;

		Map<String,String[]> t = new LinkedHashMap<String,String[]>();
		t.put("f1", new String[]{"bar"});
		t.put("f2", new String[]{"bar","baz"});
		t.put("f3", new String[]{});
		s = UrlEncodingSerializer.DEFAULT_EXPANDED;
		r = s.serialize(t);
		String e = "f1=bar&f2=bar&f2=baz";
		assertEquals(e, r);
	}
	
	@Test
	public void testParseParameterObjectMap() throws Exception {
		String in = "(name='foo bar')";
		
		ObjectMap r =  UrlEncodingParser.DEFAULT.parseParameter(in, ObjectMap.class);
	
		assertEquals("{name:'foo bar'}", JsonSerializer.DEFAULT_LAX.toString(r));
	}
}