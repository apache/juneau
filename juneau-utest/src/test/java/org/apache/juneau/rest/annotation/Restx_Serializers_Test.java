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

import org.apache.juneau.collections.*;
import org.apache.juneau.rest.RestResponse;
import org.apache.juneau.rest.client.*;
import org.apache.juneau.rest.mock.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.testutils.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class Restx_Serializers_Test {

	//------------------------------------------------------------------------------------------------------------------
	// Basic tests
	//------------------------------------------------------------------------------------------------------------------

	public static class SA extends MockWriterSerializer {
		public SA(MockWriterSerializer.Builder b) {
			super(b.produces("text/a").function((s,o)->"text/a - "+o));
		}
	}

	public static class SB extends MockWriterSerializer {
		public SB(MockWriterSerializer.Builder b) {
			super(b.produces("text/b").function((s,o)->"text/b - "+o));
		}
	}

	public static class SC extends MockWriterSerializer {
		public SC(MockWriterSerializer.Builder b) {
			super(b.produces("text/a").function((s,o)->"text/c - "+o));
		}
	}

	public static class SD extends MockWriterSerializer {
		public SD(MockWriterSerializer.Builder b) {
			super(b.produces("text/d").accept("text/a,text/d").function((s,o)->"text/d - "+o));
		}
	}

	@Rest(serializers=SA.class)
	public static class A {
		@RestGet
		public String a() {
			return "test1";
		}
		@RestGet(serializers=SB.class)
		public String b() {
			return "test2";
		}
		@RestGet(serializers={SB.class,SC.class,SerializerSet.Inherit.class})
		public String c() {
			return "test3";
		}
		@RestGet(serializers={SD.class,SerializerSet.Inherit.class})
		public String d() {
			return "test4";
		}
		@RestGet
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

	public static class DummySerializer extends MockWriterSerializer {
		public DummySerializer(MockWriterSerializer.Builder b, String produces) {
			super(b.produces(produces));
		}
	}

	public static class S1 extends DummySerializer{ public S1(MockWriterSerializer.Builder b) {super(b, "text/s1");} }
	public static class S2 extends DummySerializer{ public S2(MockWriterSerializer.Builder b) {super(b, "text/s2");} }
	public static class S3 extends DummySerializer{ public S3(MockWriterSerializer.Builder b) {super(b, "text/s3");} }
	public static class S4 extends DummySerializer{ public S4(MockWriterSerializer.Builder b) {super(b, "text/s4");} }
	public static class S5 extends DummySerializer{ public S5(MockWriterSerializer.Builder b) {super(b, "text/s5");} }

	@Rest(serializers={S1.class,S2.class})
	public static class B {}

	@Rest(serializers={S3.class,S4.class})
	public static class B1 extends B {}

	@Rest
	public static class B2 extends B1 {
		@RestGet
		public JsonList a(RestResponse res) {
			// Should show ['text/s3','text/s4','text/s1','text/s2']
			return JsonList.of(res.getOpContext().getSupportedAcceptTypes());
		}
		@RestGet(serializers=S5.class)
		public JsonList b(RestResponse res) {
			// Should show ['text/s5']
			return JsonList.of(res.getOpContext().getSupportedAcceptTypes());
		}
		@RestGet(serializers={S5.class,SerializerSet.Inherit.class})
		public JsonList c(RestResponse res) {
			// Should show ['text/s5','text/s3','text/s4','text/s1','text/s2']
			return JsonList.of(res.getOpContext().getSupportedAcceptTypes());
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
