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

import static org.junit.runners.MethodSorters.*;

import java.io.IOException;

import org.apache.juneau.*;
import org.apache.juneau.dto.swagger.Swagger;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.client.*;
import org.apache.juneau.rest.mock.*;
import org.apache.juneau.serializer.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class Nls_Test {

	//------------------------------------------------------------------------------------------------------------------
	// Test getting an NLS property defined on a class or method.
	//------------------------------------------------------------------------------------------------------------------

	@Rest(
		serializers={A1.class},
		defaultRequestAttributes={"TestProperty:$L{key1}"},
		messages="NlsTest"
	)
	public static class A {
		@RestOp
		public String a() {
			return null;
		}
		@RestOp(
			defaultRequestAttributes={"TestProperty:$L{key2}"}
		)
		public String b() {
			return null;
		}
	}
	public static class A1 extends WriterSerializer {
		public A1(PropertyStore ps) {
			super(ps, "text/plain", null);
		}
		@Override /* Serializer */
		public WriterSerializerSession createSession(SerializerSessionArgs args) {
			return new WriterSerializerSession(args) {
				@Override /* SerializerSession */
				protected void doSerialize(SerializerPipe out, Object o) throws IOException, SerializeException {
					out.getWriter().write(getProperty("TestProperty", String.class));
				}
			};
		}
	}

	@Test
	public void a01_basic() throws Exception {
		RestClient a = MockRestClient.build(A.class);
		a.get("/a").run().assertBody().is("value1");
		a.get("/b").run().assertBody().is("value2");
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

	@Test
	public void b01_optionsPageWithoutNls() throws Exception {
		MockRestClient b = MockRestClient.build(B.class);
		b.options("/").run().assertBody().contains("foo");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Test Missing resource bundles.
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class C {
		@RestOp
		public String a(RestRequest req) {
			// Missing resource bundle should cause {!!x} string.
			return req.getMessage("bad", 1, 2, 3);
		}
	}

	@Test
	public void c01_missingResourceBundle() throws Exception {
		MockRestClient c = MockRestClient.build(C.class);
		c.get("/a").run().assertBody().is("{!bad}");
	}
}
