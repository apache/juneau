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
import org.apache.juneau.rest.mock.*;
import org.apache.juneau.serializer.*;
import org.junit.*;
import org.junit.runners.*;

/**
 * Tests that validate the behavior of @RestMethod(serializers).
 */
@SuppressWarnings({"javadoc"})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class RestResourceSerializersTest {

	//=================================================================================================================
	// Setup
	//=================================================================================================================

	public static class SA extends WriterSerializer {
		public SA(PropertyStore ps) {
			super(ps, "text/a", null);
		}
		@Override /* Serializer */
		public WriterSerializerSession createSession(SerializerSessionArgs args) {
			return new WriterSerializerSession(args) {
				@Override /* SerializerSession */
				protected void doSerialize(SerializerPipe out, Object o) throws Exception {
					out.getWriter().write("text/a - " + o);
				}
			};
		}
	}

	public static class SB extends WriterSerializer {
		public SB(PropertyStore ps) {
			super(ps, "text/b", null);
		}
		@Override /* Serializer */
		public WriterSerializerSession createSession(SerializerSessionArgs args) {
			return new WriterSerializerSession(args) {
				@Override /* SerializerSession */
				protected void doSerialize(SerializerPipe out, Object o) throws Exception {
					out.getWriter().write("text/b - " + o);
				}
			};
		}
	}

	public static class SC extends WriterSerializer {
		public SC(PropertyStore ps) {
			super(ps, "text/a", null);
		}
		@Override /* Serializer */
		public WriterSerializerSession createSession(SerializerSessionArgs args) {
			return new WriterSerializerSession(args) {
				@Override /* SerializerSession */
				protected void doSerialize(SerializerPipe out, Object o) throws Exception {
					out.getWriter().write("text/c - " + o);
				}
			};
		}
	}

	public static class SD extends WriterSerializer {
		public SD(PropertyStore ps) {
			super(ps, "text/d", "text/a,text/d");
		}
		@Override /* Serializer */
		public WriterSerializerSession createSession(SerializerSessionArgs args) {
			return new WriterSerializerSession(args) {

				@Override /* SerializerSession */
				protected void doSerialize(SerializerPipe out, Object o) throws Exception {
					out.getWriter().write("text/d - " + o);
				}
			};
		}
	}

	//=================================================================================================================
	// Basic tests
	//=================================================================================================================
	
	@RestResource(serializers=SA.class)
	public static class A {
		@RestMethod(name=GET, path="/serializerOnClass")
		public String a01() {
			return "test1";
		}
		@RestMethod(name=GET, path="/serializerOnMethod", serializers=SB.class)
		public String a02() {
			return "test2";
		}
		@RestMethod(name=GET, path="/serializerOverriddenOnMethod", serializers={SB.class,SC.class}, inherit="SERIALIZERS")
		public String a03() {
			return "test3";
		}
		@RestMethod(name=GET, path="/serializerWithDifferentMediaTypes", serializers={SD.class}, inherit="SERIALIZERS")
		public String a04() {
			return "test4";
		}
		@RestMethod(name=GET, path="/validErrorResponse")
		public String a05() {
			return "test406";
		}
	}
	static MockRest a = MockRest.create(A.class);
	
	@Test
	public void a01_serializerOnClass() throws Exception {
		a.request("GET", "/serializerOnClass").accept("text/a").execute().assertBody("text/a - test1");
		a.request("GET", "/serializerOnClass?noTrace=true").accept("text/b").execute()
			.assertStatus(406)
			.assertBodyContains(
				"Unsupported media-type in request header 'Accept': 'text/b'",
				"Supported media-types: ['text/a'"
			);
	}
	@Test
	public void a02_serializerOnMethod() throws Exception {
		a.request("GET", "/serializerOnMethod?noTrace=true").accept("text/a").execute()
			.assertStatus(406)
			.assertBodyContains(
				"Unsupported media-type in request header 'Accept': 'text/a'",
				"Supported media-types: ['text/b']"
			);
	}
	@Test
	public void a03_serializerOverriddenOnMethod() throws Exception {
		a.request("GET", "/serializerOverriddenOnMethod").accept("text/a").execute().assertBody("text/c - test3");
		a.request("GET", "/serializerOverriddenOnMethod").accept("text/b").execute().assertBody("text/b - test3");
	}
	@Test
	public void a04_serializerWithDifferentMediaTypes() throws Exception {
		a.request("GET", "/serializerWithDifferentMediaTypes").accept("text/a").execute().assertBody("text/d - test4");
		a.request("GET", "/serializerWithDifferentMediaTypes").accept("text/d").execute().assertBody("text/d - test4");
	}
	@Test
	public void a05_validErrorResponse() throws Exception {
		a.request("GET", "/validErrorResponse?noTrace=true").accept("text/bad").execute()
			.assertStatus(406)
			.assertBodyContains(
				"Unsupported media-type in request header 'Accept': 'text/bad'",
				"Supported media-types: ['text/a"
			);
	}
}
