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

import java.io.IOException;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.RestResponse;
import org.apache.juneau.rest.client.*;
import org.apache.juneau.rest.mock2.*;
import org.apache.juneau.serializer.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class Restx_Serializers_Test {

	//------------------------------------------------------------------------------------------------------------------
	// Basic tests
	//------------------------------------------------------------------------------------------------------------------

	public static class SA extends WriterSerializer {
		public SA(PropertyStore ps) {
			super(ps, "text/a", null);
		}
		@Override /* Serializer */
		public WriterSerializerSession createSession(SerializerSessionArgs args) {
			return new WriterSerializerSession(args) {
				@Override /* SerializerSession */
				protected void doSerialize(SerializerPipe out, Object o) throws IOException, SerializeException {
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
				protected void doSerialize(SerializerPipe out, Object o) throws IOException, SerializeException {
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
				protected void doSerialize(SerializerPipe out, Object o) throws IOException, SerializeException {
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
				protected void doSerialize(SerializerPipe out, Object o) throws IOException, SerializeException {
					out.getWriter().write("text/d - " + o);
				}
			};
		}
	}

	@Rest(serializers=SA.class)
	public static class A {
		@RestMethod
		public String a() {
			return "test1";
		}
		@RestMethod(serializers=SB.class)
		public String b() {
			return "test2";
		}
		@RestMethod(serializers={SB.class,SC.class,Inherit.class})
		public String c() {
			return "test3";
		}
		@RestMethod(serializers={SD.class,Inherit.class})
		public String d() {
			return "test4";
		}
		@RestMethod
		public String e() {
			return "test406";
		}
	}

	@Test
	public void a01_basic() throws Exception {
		RestClient a = MockRestClient.buildLax(A.class);
		a.get("/a")
			.accept("text/a")
			.run()
			.assertBody().is("text/a - test1");
		a.get("/a?noTrace=true")
			.accept("text/b")
			.run()
			.assertCode().is(406)
			.assertBody().contains(
				"Unsupported media-type in request header 'Accept': 'text/b'",
				"Supported media-types: ['text/a'"
			);
		a.get("/b?noTrace=true")
			.accept("text/a")
			.run()
			.assertCode().is(406)
			.assertBody().contains(
				"Unsupported media-type in request header 'Accept': 'text/a'",
				"Supported media-types: ['text/b']"
			);
		a.get("/c")
			.accept("text/a")
			.run()
			.assertBody().is("text/c - test3");
		a.get("/c")
			.accept("text/b")
			.run()
			.assertBody().is("text/b - test3");
		a.get("/d")
			.accept("text/a")
			.run()
			.assertBody().is("text/d - test4");
		a.get("/d")
			.accept("text/d")
			.run()
			.assertBody().is("text/d - test4");
		a.get("/e?noTrace=true")
			.accept("text/bad")
			.run()
			.assertCode().is(406)
			.assertBody().contains(
				"Unsupported media-type in request header 'Accept': 'text/bad'",
				"Supported media-types: ['text/a"
			);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Test serializer inheritance.
	//------------------------------------------------------------------------------------------------------------------

	public static class DummySerializer extends WriterSerializer {
		public DummySerializer(String produces) {
			super(PropertyStore.DEFAULT, produces, null);
		}
		@Override /* Serializer */
		public WriterSerializerSession createSession(SerializerSessionArgs args) {
			return new WriterSerializerSession(args) {
				@Override /* SerializerSession */
				protected void doSerialize(SerializerPipe out, Object o) throws IOException, SerializeException {
					out.getWriter().write(o.toString());
				}
			};
		}
	}

	public static class S1 extends DummySerializer{ public S1(PropertyStore ps) {super("text/s1");} }
	public static class S2 extends DummySerializer{ public S2(PropertyStore ps) {super("text/s2");} }
	public static class S3 extends DummySerializer{ public S3(PropertyStore ps) {super("text/s3");} }
	public static class S4 extends DummySerializer{ public S4(PropertyStore ps) {super("text/s4");} }
	public static class S5 extends DummySerializer{ public S5(PropertyStore ps) {super("text/s5");} }

	@Rest(serializers={S1.class,S2.class})
	public static class B {}

	@Rest(serializers={S3.class,S4.class,Inherit.class})
	public static class B1 extends B {}

	@Rest
	public static class B2 extends B1 {
		@RestMethod
		public OList a(RestResponse res) {
			// Should show ['text/s3','text/s4','text/s1','text/s2']
			return OList.ofAll(res.getSupportedMediaTypes());
		}
		@RestMethod(serializers=S5.class)
		public OList b(RestResponse res) {
			// Should show ['text/s5']
			return OList.ofAll(res.getSupportedMediaTypes());
		}
		@RestMethod(serializers={S5.class,Inherit.class})
		public OList c(RestResponse res) {
			// Should show ['text/s5','text/s3','text/s4','text/s1','text/s2']
			return OList.ofAll(res.getSupportedMediaTypes());
		}
	}

	@Test
	public void b01_inheritence() throws Exception {
		RestClient b = MockRestClient.build(B2.class);
		b.get("/a").run().assertBody().is("['text/s3','text/s4','text/s1','text/s2']");
		b.get("/b").run().assertBody().is("['text/s5']");
		b.get("/c").run().assertBody().is("['text/s5','text/s3','text/s4','text/s1','text/s2']");
	}
}
