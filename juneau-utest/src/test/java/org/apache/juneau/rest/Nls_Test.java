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
package org.apache.juneau.rest;

import org.apache.juneau.*;
import org.apache.juneau.bean.swagger.Swagger;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.client.*;
import org.apache.juneau.rest.mock.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.utest.utils.*;
import org.junit.jupiter.api.*;

class Nls_Test extends SimpleTestBase {

	//------------------------------------------------------------------------------------------------------------------
	// Test getting an NLS property defined on a class or method.
	//------------------------------------------------------------------------------------------------------------------

	@Rest(
		serializers={A1.class},
		defaultRequestAttributes={"TestProperty:$L{key1}"},
		messages="NlsTest"
	)
	public static class A {
		@RestGet
		public String a() {
			return null;
		}
		@RestGet(
			defaultRequestAttributes={"TestProperty:$L{key2}"}
		)
		public String b() {
			return null;
		}
	}
	public static class A1 extends FakeWriterSerializer {
		public A1(FakeWriterSerializer.Builder builder) {
			super(builder.accept("*/*").function((s,o)->out(s)));
		}

		public static String out(SerializerSession s) {
			return s.getSessionProperties().getString("TestProperty",null);
		}
	}

	@Test void a01_basic() throws Exception {
		RestClient a = MockRestClient.build(A.class);
		a.get("/a").run().assertContent("value1");
		a.get("/b").run().assertContent("value2");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Test OPTIONS pages without NLS
	//------------------------------------------------------------------------------------------------------------------

	@Rest(title="test")
	public static class B {
		@RestOp(description="foo")
		public Swagger options(RestRequest req) {
			// Should get to the options page without errors
			return req.getSwagger().orElse(null);
		}
	}

	@Test void b01_optionsPageWithoutNls() throws Exception {
		MockRestClient b = MockRestClient.build(B.class);
		b.options("/").run().assertContent().isContains("foo");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Test Missing resource bundles.
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class C {
		@RestGet
		public String a(RestRequest req) {
			// Missing resource bundle should cause {!!x} string.
			return req.getMessage("bad", 1, 2, 3);
		}
	}

	@Test void c01_missingResourceBundle() throws Exception {
		MockRestClient c = MockRestClient.build(C.class);
		c.get("/a").run().assertContent("{!bad}");
	}
}