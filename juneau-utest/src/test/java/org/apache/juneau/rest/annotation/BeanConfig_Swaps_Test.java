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

import static org.junit.runners.MethodSorters.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.http.annotation.Body;
import org.apache.juneau.http.annotation.Path;
import org.apache.juneau.json.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.rest.client.*;
import org.apache.juneau.rest.mock.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.transform.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class BeanConfig_Swaps_Test {

	//------------------------------------------------------------------------------------------------------------------
	// Basic tests
	//------------------------------------------------------------------------------------------------------------------

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

	@Rest(serializers=SimpleJsonSerializer.class, parsers=JsonParser.class)
	@BeanConfig(swaps={SwapA1.class})
	public static class A2 {}

	@Rest
	@BeanConfig(swaps={SwapA2.class})
	public static class A1 extends A2 {

		@RestGet
		public A a() {
			return new A(); // Should return "A2-1".
		}
		@RestPut
		public A b(@Body A a) {
			return a; // Should return "A2-1".
		}
		@RestPut(path="/c/{a}")
		public A c(@Path("a") A a) {
			return a; // Should return "A2-1".
		}
		@RestGet
		@BeanConfig(swaps={SwapA3.class})
		public A d() {
			return new A(); // Should return "A3-1".
		}
		@RestPut
		@BeanConfig(swaps={SwapA3.class})
		public A e(@Body A a) {
			return a; // Should return "A3-1".
		}
		@RestPut(path="/f/{a}")
		@BeanConfig(swaps={SwapA3.class})
		public A f(@Path("a") A a) {
			return a; // Should return "A3-1".
		}
	}

	@Test
	public void a01_swaps() throws Exception {
		RestClient a = MockRestClient.build(A1.class);
		a.get("/a").json().run().assertBody().is("'A2-0'");
		a.put("/b", "'A2-1'", "application/json").run().assertBody().is("'A2-1'");
		a.put("/c/A2-2", null, "application/json").run().assertBody().is("'A2-2'");
		a.get("/d").json().run().assertBody().is("'A3-0'");
		a.put("/e", "'A3-1'", "application/json").run().assertBody().is("'A3-1'");
		a.put("/f/A3-2", null, "application/json").run().assertBody().is("'A3-2'");
	}
}
