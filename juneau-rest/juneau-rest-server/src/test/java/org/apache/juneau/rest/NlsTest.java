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

import static org.apache.juneau.http.HttpMethodName.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.mock.*;
import org.apache.juneau.serializer.*;
import org.junit.*;
import org.junit.runners.*;

/**
 * Tests various aspects of localization support.
 */
@SuppressWarnings({"javadoc"})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class NlsTest {

	//====================================================================================================
	// Test getting an NLS property defined on a class or method.
	//====================================================================================================

	@RestResource(
		serializers={A01.class},
		properties={@Property(name="TestProperty",value="$L{key1}")},
		messages="NlsTest"
	)
	public static class A {
		@RestMethod(name=GET, path="/fromClass")
		public String a01() {
			return null;
		}
		@RestMethod(name=GET, path="/fromMethod",
			properties={@Property(name="TestProperty",value="$L{key2}")}
		)
		public String a02() {
			return null;
		}
	}
	static MockRest a = MockRest.create(A.class);

	public static class A01 extends WriterSerializer {
		public A01(PropertyStore ps) {
			super(ps, "text/plain", null);
		}
		@Override /* Serializer */
		public WriterSerializerSession createSession(SerializerSessionArgs args) {
			return new WriterSerializerSession(args) {
				@Override /* SerializerSession */
				protected void doSerialize(SerializerPipe out, Object o) throws Exception {
					out.getWriter().write(getProperty("TestProperty", String.class));
				}
			};
		}
	}

	@Test
	public void a01_fromClass() throws Exception {
		a.request("GET", "/fromClass").execute().assertBody("value1");
	}

	@Test
	public void a02_fromMethod() throws Exception {
		a.request("GET", "/fromMethod").execute().assertBody("value2");
	}
}
