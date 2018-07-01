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

import static org.apache.juneau.http.HttpMethodName.*;

import org.apache.juneau.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.json.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.rest.mock.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.transform.*;
import org.junit.*;
import org.junit.runners.*;

/**
 * Tests that validate the behavior of @RestResource(pojoSwaps).
 */
@SuppressWarnings({"javadoc"})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class RestResourcePojoSwapsTest {

	//=================================================================================================================
	// Basic tests
	//=================================================================================================================

	public static class A {
		public int f1;
	}

	public static class SwapA1 extends StringSwap<A> {
		@Override /* PojoSwap */
		public String swap(BeanSession session, A a) throws SerializeException {
			return "A1-" + a.f1;
		}
		@Override /* PojoSwap */
		public A unswap(BeanSession session, String in, ClassMeta<?> hint) throws ParseException {
			if (! in.startsWith("A1"))
				throw new RuntimeException("Invalid input for SwapA1!");
			A a = new A();
			a.f1 = Integer.parseInt(in.substring(3));
			return a;
		}
	}

	public static class SwapA2 extends StringSwap<A> {
		@Override /* PojoSwap */
		public String swap(BeanSession session, A a) throws SerializeException {
			return "A2-" + a.f1;
		}
		@Override /* PojoSwap */
		public A unswap(BeanSession session, String in, ClassMeta<?> hint) throws ParseException {
			if (! in.startsWith("A2"))
				throw new RuntimeException("Invalid input for SwapA2!");
			A a = new A();
			a.f1 = Integer.parseInt(in.substring(3));
			return a;
		}
	}

	public static class SwapA3 extends StringSwap<A> {
		@Override /* PojoSwap */
		public String swap(BeanSession session, A a) throws SerializeException {
			return "A3-" + a.f1;
		}
		@Override /* PojoSwap */
		public A unswap(BeanSession session, String in, ClassMeta<?> hint) throws ParseException {
			if (! in.startsWith("A3"))
				throw new RuntimeException("Invalid input for SwapA3!");
			A a = new A();
			a.f1 = Integer.parseInt(in.substring(3));
			return a;
		}
	}

	@RestResource(pojoSwaps={SwapA1.class}, serializers=JsonSerializer.Simple.class, parsers=JsonParser.class)
	public static class A01_Parent {}

	@RestResource(pojoSwaps={SwapA2.class})
	public static class A01 extends A01_Parent {

		@RestMethod(name=GET, path="/classTransformOverridesParentClassTransform")
		public A a01a() {
			return new A(); // Should return "A2-1".
		}
		@RestMethod(name=PUT, path="/classTransformOverridesParentClassTransform")
		public A a01b(@Body A a) {
			return a; // Should return "A2-1".
		}
		@RestMethod(name=PUT, path="/classTransformOverridesParentClassTransform/{a}")
		public A a01c(@Path("a") A a) {
			return a; // Should return "A2-1".
		}
		@RestMethod(name=GET, path="/methodTransformOverridesClassTransform", pojoSwaps={SwapA3.class})
		public A a02a() {
			return new A(); // Should return "A3-1".
		}
		@RestMethod(name=PUT, path="/methodTransformOverridesClassTransform", pojoSwaps={SwapA3.class})
		public A a02b(@Body A a) {
			return a; // Should return "A3-1".
		}
		@RestMethod(name=PUT, path="/methodTransformOverridesClassTransform/{a}", pojoSwaps={SwapA3.class})
		public A a02c(@Path("a") A a) {
			return a; // Should return "A3-1".
		}
	}
	static MockRest a = MockRest.create(A01.class);

	@Test
	public void a01_classTransformOverridesParentClassTransform() throws Exception {
		a.get("/classTransformOverridesParentClassTransform").json().execute().assertBody("'A2-0'");
		a.put("/classTransformOverridesParentClassTransform", "'A2-1'").json().execute().assertBody("'A2-1'");
		a.put("/classTransformOverridesParentClassTransform/A2-2", null).json().execute().assertBody("'A2-2'");
	}

	@Test
	public void a02_methodTransformOverridesClassTransform() throws Exception {
		a.get("/methodTransformOverridesClassTransform").json().execute().assertBody("'A3-0'");
		a.put("/methodTransformOverridesClassTransform", "'A3-1'").json().execute().assertBody("'A3-1'");
		a.put("/methodTransformOverridesClassTransform/A3-2", null).json().execute().assertBody("'A3-2'");
	}
}
