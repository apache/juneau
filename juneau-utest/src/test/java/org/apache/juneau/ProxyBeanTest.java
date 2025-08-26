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
package org.apache.juneau;

import static org.junit.Assert.*;

import org.apache.juneau.html.*;
import org.apache.juneau.json.*;
import org.apache.juneau.msgpack.*;
import org.apache.juneau.oapi.*;
import org.apache.juneau.uon.*;
import org.apache.juneau.urlencoding.*;
import org.apache.juneau.xml.*;
import org.junit.jupiter.api.*;

class ProxyBeanTest extends SimpleTestBase {

	//====================================================================================================
	// testBasic
	//====================================================================================================

	public interface A {
		void setFoo(int foo);
		int getFoo();
	}

	@Test void a01_basic() throws Exception {
		A a = JsonParser.DEFAULT.parse("{foo:1}", A.class);
		assertEquals(1, a.getFoo());
		a = XmlParser.DEFAULT.parse("<object><foo>1</foo></object>", A.class);
		assertEquals(1, a.getFoo());
		a = UonParser.DEFAULT.parse("(foo=1)", A.class);
		assertEquals(1, a.getFoo());
		a = UrlEncodingParser.DEFAULT.parse("foo=1", A.class);
		assertEquals(1, a.getFoo());
		a = MsgPackParser.DEFAULT.parse("81A3666F6F01", A.class);
		assertEquals(1, a.getFoo());
		a = HtmlParser.DEFAULT.parse("<table><tr><td>foo</td><td>1</td></tr></table>", A.class);
		assertEquals(1, a.getFoo());
		a = OpenApiParser.DEFAULT.parse("foo=1", A.class);
		assertEquals(1, a.getFoo());
	}
}