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
import org.apache.juneau.dto.swagger.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.mock2.*;
import org.apache.juneau.serializer.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class NlsTest {

	//=================================================================================================================
	// Test getting an NLS property defined on a class or method.
	//=================================================================================================================

	@Rest(
		serializers={A01.class},
		reqAttrs={"TestProperty:$L{key1}"},
		messages="NlsTest"
	)
	public static class A {
		@RestMethod
		public String a01() {
			return null;
		}
		@RestMethod(
			reqAttrs={"TestProperty:$L{key2}"}
		)
		public String a02() {
			return null;
		}
	}
	static MockRestClient a = MockRestClient.build(A.class);

	public static class A01 extends WriterSerializer {
		public A01(PropertyStore ps) {
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
	public void a01_fromClass() throws Exception {
		a.get("/a01").run().assertBody().is("value1");
	}

	@Test
	public void a02_fromMethod() throws Exception {
		a.get("/a02").run().assertBody().is("value2");
	}

	//=================================================================================================================
	// Test OPTIONS pages without NLS
	//=================================================================================================================

	@Rest(title="test")
	public static class B {
		@RestMethod(description="foo")
		public Swagger options(RestRequest req) {
			// Should get to the options page without errors
			return req.getSwagger();
		}
	}
	static MockRestClient b = MockRestClient.build(B.class);

	@Test
	public void b01_optionsPageWithoutNls() throws Exception {
		b.options("/").run().assertBody().contains("foo");
	}

	//=================================================================================================================
	// Test Missing resource bundles.
	//=================================================================================================================

	@Rest
	public static class C {
		@RestMethod
		public String test(RestRequest req) {
			// Missing resource bundle should cause {!!x} string.
			return req.getMessage("bad", 1, 2, 3);
		}
	}
	static MockRestClient c = MockRestClient.build(C.class);

	@Test
	public void c01_missingResourceBundle() throws Exception {
		c.get("/test").run().assertBody().is("{!bad}");
	}
}
