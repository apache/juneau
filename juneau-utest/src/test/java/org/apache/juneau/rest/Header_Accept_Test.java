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

import org.apache.juneau.http.annotation.Content;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.client.*;
import org.apache.juneau.rest.mock.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.testutils.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class Header_Accept_Test {

	//------------------------------------------------------------------------------------------------------------------
	// Setup classes
	//------------------------------------------------------------------------------------------------------------------

	public static class S1 extends MockWriterSerializer {
		public S1(MockWriterSerializer.Builder b) {
			super(b.produces("text/s1").function((s,o) -> "s1"));
		}
	}
	public static class S2 extends MockWriterSerializer {
		public S2(MockWriterSerializer.Builder b) {
			super(b.produces("text/s2").function((s,o) -> "s2"));
		}
	}
	public static class S3 extends MockWriterSerializer {
		public S3(MockWriterSerializer.Builder b) {
			super(b.produces("text/s3").function((s,o) -> "s3"));
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// Test that default Accept headers on servlet annotation are picked up.
	//------------------------------------------------------------------------------------------------------------------

	@Rest(
		defaultRequestHeaders={" Accept : text/s2 "},
		serializers={S1.class,S2.class}
	)
	public static class A {
		@RestOp
		public String put(@Content String in) {
			return in;
		}
	}

	@Test
	public void a01_defaultHeadersOnServletAnnotation() throws Exception {
		RestClient a = MockRestClient.buildLax(A.class);
		a.put("/", null)
			.run()
			.assertContent("s2");
		a.put("/", null)
			.accept("text/s1")
			.run()
			.assertContent("s1");
		a.put("/", null)
			.accept("text/s2")
			.run()
			.assertContent("s2");
		a.put("?noTrace=true", null)
			.accept("text/s3")
			.run()
			.assertStatus(406)
			.assertContent().isContains("Unsupported media-type in request header 'Accept': 'text/s3'");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Test that default Accept headers on servlet annotation are picked up
	// when @RestOp.parsers/serializers annotations are used.
	//------------------------------------------------------------------------------------------------------------------

	@Rest(
		defaultRequestHeaders={" Accept : text/s2 "},
		serializers={S1.class,S2.class}
	)
	public static class B {
		@RestOp(serializers=S3.class)
		public String put(@Content String in) {
			return in;
		}
	}

	@Test
	public void b01_restMethodWithParsersSerializers() throws Exception {
		RestClient b = MockRestClient.buildLax(B.class);
		b.put("/", null).accept("text/s3").run().assertContent("s3");
		b.put("?noTrace=true", null)
			.accept("text/s4")
			.run()
			.assertStatus(406)
			.assertContent().isContains(
				"Unsupported media-type in request header 'Accept': 'text/s4'",
				"Supported media-types: ['text/s3']"
			);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Test that default Accept headers on servlet annotation are picked up
	// when @RestOp.addParsers/addSerializers annotations are used.
	//------------------------------------------------------------------------------------------------------------------

	@Rest(
		defaultRequestHeaders={" Accept : text/s2 "},
		serializers={S1.class,S2.class}
	)
	public static class C {
		@RestOp(serializers={S3.class,SerializerSet.Inherit.class})
		public String put(@Content String in) {
			return in;
		}
	}

	@Test
	public void c01_restMethodAddParsersSerializersInherit() throws Exception {
		RestClient c = MockRestClient.buildLax(C.class);
		c.put("/", null)
			.run()
			.assertContent("s2");
		c.put("/", null)
			.accept("text/s1")
			.run()
			.assertContent("s1");
		c.put("/", null)
			.accept("text/s2")
			.run()
			.assertContent("s2");
		c.put("/", null)
			.accept("text/s3")
			.run()
			.assertContent("s3");
		c.put("?noTrace=true", null)
			.accept("text/s4")
			.run()
			.assertStatus(406)
			.assertContent().isContains(
				"Unsupported media-type in request header 'Accept': 'text/s4'",
				"Supported media-types: ['text/s3','text/s1','text/s2']"
			);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Various Accept incantations.
	//------------------------------------------------------------------------------------------------------------------

	@Rest(
		defaultRequestHeaders={" Accept : text/s2 "},
		serializers={S1.class,S2.class}
	)
	public static class D {
		@RestOp
		public String put(@Content String in) {
			return in;
		}
	}

	@Test
	public void d01_accept_valid() throws Exception {
		RestClient d = MockRestClient.buildLax(D.class);
		// "*/*" should match the first serializer, not the default serializer.
		d.put("/", null).accept("*/*").run().assertContent("s1");
		// "text/*" should match the first serializer, not the default serializer.
		d.put("/", null).accept("text/*").run().assertContent("s1");
		d.put("/", null).accept("bad/*,text/*").run().assertContent("s1");
		d.put("/", null).accept("text/*,bad/*").run().assertContent("s1");
		d.put("/", null).accept("text/s1;q=0.5,text/s2").run().assertContent("s2");
		d.put("/", null).accept("text/s1,text/s2;q=0.5").run().assertContent("s1");
		d.put("?noTrace=true", null)
			.accept("bad/*")
			.run()
			.assertStatus(406)
			.assertContent().isContains(
				"Unsupported media-type in request header 'Accept': 'bad/*'",
				"Supported media-types: ['text/s1','text/s2']"
			);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Test that default Accept headers on method annotation are picked up
	// when @RestOp.parsers/serializers annotations are used.
	//------------------------------------------------------------------------------------------------------------------

	@Rest(
		defaultRequestHeaders={" Accept : text/s2 "},
		serializers={S1.class,S2.class}
	)
	public static class E {
		@RestOp(defaultRequestHeaders={"Accept: text/s3"}, serializers=S3.class)
		public String put(@Content String in) {
			return in;
		}
	}

	@Test
	public void e01_restMethodParserSerializerAnnotations() throws Exception {
		RestClient e = MockRestClient.buildLax(E.class);
		e.put("/", null).run().assertContent("s3");
		e.put("/", null).accept("text/s3").run().assertContent("s3");
		e.put("?noTrace=true", null)
			.accept("text/s1")
			.run()
			.assertStatus(406)
			.assertContent().isContains(
				"Unsupported media-type in request header 'Accept': 'text/s1'",
				"Supported media-types: ['text/s3']"
			);
		e.put("?noTrace=true", null).accept("text/s2").run()
			.assertStatus(406)
			.assertContent().isContains(
				"Unsupported media-type in request header 'Accept': 'text/s2'",
				"Supported media-types: ['text/s3']"
			);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Test that default Accept headers on method annotation are picked up
	// 	when @RestOp.addParsers/addSerializers annotations are used.
	//------------------------------------------------------------------------------------------------------------------

	@Rest(
		defaultRequestHeaders={" Accept : text/s2 "},
		serializers={S1.class,S2.class}
	)
	public static class F {
		@RestOp(defaultRequestHeaders={"Accept: text/s3"}, serializers={SerializerSet.Inherit.class, S3.class})
		public String put(@Content String in) {
			return in;
		}
	}

	@Test
	public void f01_restMethodAddParsersSerializersAnnotations() throws Exception {
		RestClient f = MockRestClient.build(F.class);
		f.put("/", null).run().assertContent("s3");
		f.put("/", null).accept("text/s1").run().assertContent("s1");
		f.put("/", null).accept("text/s2").run().assertContent("s2");
		f.put("/", null).accept("text/s3").run().assertContent("s3");
	}
}
