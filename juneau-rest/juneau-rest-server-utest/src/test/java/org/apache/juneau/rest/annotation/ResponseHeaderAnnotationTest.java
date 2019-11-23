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
package org.apache.juneau.rest.annotation;

import static org.apache.juneau.rest.testutils.TestUtils.*;
import static org.junit.Assert.*;

import org.apache.juneau.*;
import org.apache.juneau.dto.swagger.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.rest.mock2.*;
import org.junit.*;
import org.junit.runners.*;

/**
 * Tests related to @ResponseHeader annotation.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ResponseHeaderAnnotationTest {

	//=================================================================================================================
	// Basic tests
	//=================================================================================================================

	@Rest
	public static class A {
		@RestMethod
		public void a01(Value<A01> h) {
			h.set(new A01());
		}
		@RestMethod
		public void a02(@ResponseHeader(name="Foo") Value<String> h) {
			h.set("foo");
		}
		@RestMethod
		public void a03(@ResponseHeader(name="Bar") Value<A01> h) {
			h.set(new A01());
		}
	}

	@ResponseHeader(name="Foo")
	public static class A01 {
		@Override
		public String toString() {return "foo";}
	}

	static MockRest a = MockRest.build(A.class);

	@Test
	public void a01_valueOnParameterPojo() throws Exception {
		a.get("/a01").execute().assertStatus(200).assertHeader("Foo", "foo");
	}
	@Test
	public void a02_valueOnParameterString() throws Exception {
		a.get("/a02").execute().assertStatus(200).assertHeader("Foo", "foo");
	}
	@Test
	public void a03_valueOnParameterOverrideName() throws Exception {
		a.get("/a03").execute().assertStatus(200).assertHeader("Bar", "foo");
	}

	//=================================================================================================================
	// @ResponseHeader on POJO
	//=================================================================================================================

	@Rest
	public static class SA {

		@ResponseHeader(
			name="H",
			description="a",
			type="string"
		)
		public static class SA01 {}
		@RestMethod
		public void sa01(Value<SA01> h) {}

		@ResponseHeader(
			name="H",
			api={
				"description:'a',",
				"type:'string'"
			}
		)
		public static class SA02 {}
		@RestMethod
		public void sa02(Value<SA02> h) {}

		@ResponseHeader(
			name="H",
			api={
				"description:'b',",
				"type:'number'"
			},
			description="a",
			type="string"
		)
		public static class SA03 {}
		@RestMethod
		public void sa03(Value<SA03> h) {}

		@ResponseHeader(name="H", code=100)
		public static class SA04 {}
		@RestMethod
		public void sa04(Value<SA04> h) {}

		@ResponseHeader(name="H", code={100,101})
		public static class SA05 {}
		@RestMethod
		public void sa05(Value<SA05> h) {}

		@ResponseHeader(name="H", description="a")
		public static class SA07 {}
		@RestMethod
		public void sa07(Value<SA07> h) {}

		@ResponseHeader("H")
		public static class SA08 {}
		@RestMethod
		public void sa08(Value<SA08> h) {}
	}

	static Swagger sa = getSwagger(SA.class);

	@Test
	public void sa01_ResponseHeader_onPojo_basic() throws Exception {
		HeaderInfo x = sa.getResponseInfo("/sa01","get",200).getHeader("H");
		assertEquals("a", x.getDescription());
		assertEquals("string", x.getType());
	}
	@Test
	public void sa02_ResponseHeader_onPojo_api() throws Exception {
		HeaderInfo x = sa.getResponseInfo("/sa02","get",200).getHeader("H");
		assertEquals("a", x.getDescription());
		assertEquals("string", x.getType());
	}
	@Test
	public void sa03_ResponseHeader_onPojo_mixed() throws Exception {
		HeaderInfo x = sa.getResponseInfo("/sa03","get",200).getHeader("H");
		assertEquals("a", x.getDescription());
		assertEquals("string", x.getType());
	}
	@Test
	public void sa04_ResponseHeader_onPojo_code() throws Exception {
		HeaderInfo x = sa.getResponseInfo("/sa04","get",100).getHeader("H");
		assertNotNull(x);
	}
	@Test
	public void sa05_ResponseHeader_onPojo_codes() throws Exception {
		Operation x = sa.getOperation("/sa05","get");
		assertNotNull(x.getResponse(100).getHeader("H"));
		assertNotNull(x.getResponse(101).getHeader("H"));
	}
	@Test
	public void sa07_ResponseHeader_onPojo_nocode() throws Exception {
		HeaderInfo x = sa.getResponseInfo("/sa07","get",200).getHeader("H");
		assertEquals("a", x.getDescription());
	}
	@Test
	public void sa08_ResponseHeader_onPojo_value() throws Exception {
		HeaderInfo x = sa.getResponseInfo("/sa08","get",200).getHeader("H");
		assertNotNull(x);
	}

	//=================================================================================================================
	// @ResponseHeader on parameter
	//=================================================================================================================

	@Rest
	public static class SB {

		public static class SB01 {}
		@RestMethod
		public void sb01(
			@ResponseHeader(
				name="H",
				description="a",
				type="string"
			) Value<SB01> h) {}

		public static class SB02 {}
		@RestMethod
		public void sb02(
			@ResponseHeader(
				name="H",
				api={
					"description:'a',",
					"type:'string'"
				}
			) Value<SB02> h) {}

		public static class SB03 {}
		@RestMethod
		public void sb03(
			@ResponseHeader(
				name="H",
				api={
					"description:'b',",
					"type:'number'"
				},
				description="a",
				type="string"
			) Value<SB03> h) {}

		public static class SB04 {}
		@RestMethod
		public void sb04(@ResponseHeader(name="H", code=100) Value<SB04> h) {}

		public static class SB05 {}
		@RestMethod
		public void sb05(@ResponseHeader(name="H", code={100,101}) Value<SB05> h) {}

		public static class SB07 {}
		@RestMethod
		public void sb07(@ResponseHeader(name="H", description="a") Value<SB07> h) {}

		public static class SB08 {}
		@RestMethod
		public void sb08(@ResponseHeader("H") Value<SB08> h) {}
	}

	static Swagger sb = getSwagger(SB.class);

	@Test
	public void sb01_ResponseHeader_onPojo_basic() throws Exception {
		HeaderInfo x = sb.getResponseInfo("/sb01","get",200).getHeader("H");
		assertEquals("a", x.getDescription());
		assertEquals("string", x.getType());
	}
	@Test
	public void sb02_ResponseHeader_onPojo_api() throws Exception {
		HeaderInfo x = sb.getResponseInfo("/sb02","get",200).getHeader("H");
		assertEquals("a", x.getDescription());
		assertEquals("string", x.getType());
	}
	@Test
	public void sb03_ResponseHeader_onPojo_mixed() throws Exception {
		HeaderInfo x = sb.getResponseInfo("/sb03","get",200).getHeader("H");
		assertEquals("a", x.getDescription());
		assertEquals("string", x.getType());
	}
	@Test
	public void sb04_ResponseHeader_onPojo_code() throws Exception {
		HeaderInfo x = sb.getResponseInfo("/sb04","get",100).getHeader("H");
		assertNotNull(x);
	}
	@Test
	public void sb05_ResponseHeader_onPojo_codes() throws Exception {
		Operation x = sb.getOperation("/sb05","get");
		assertNotNull(x.getResponse(100).getHeader("H"));
		assertNotNull(x.getResponse(101).getHeader("H"));
	}
	@Test
	public void sb07_ResponseHeader_onPojo_nocode() throws Exception {
		HeaderInfo x = sb.getResponseInfo("/sb07","get",200).getHeader("H");
		assertEquals("a", x.getDescription());
	}
	@Test
	public void sb08_ResponseHeader_onPojo_value() throws Exception {
		HeaderInfo x = sb.getResponseInfo("/sb08","get",200).getHeader("H");
		assertNotNull(x);
	}
}
