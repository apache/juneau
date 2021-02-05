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
import org.apache.juneau.http.annotation.Body;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.client.*;
import org.apache.juneau.rest.mock.*;
import org.apache.juneau.serializer.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class Header_Accept_Test {

	//------------------------------------------------------------------------------------------------------------------
	// Setup classes
	//------------------------------------------------------------------------------------------------------------------

	public static class DummySerializer extends WriterSerializer {
		String name;
		DummySerializer(PropertyStore ps, String name, String produces) {
			super(ps, produces, null);
			this.name = name;
		}
		@Override /* Serializer */
		public WriterSerializerSession createSession(SerializerSessionArgs args) {
			return new WriterSerializerSession(args) {
				@Override /* SerializerSession */
				protected void doSerialize(SerializerPipe out, Object o) throws IOException, SerializeException {
					out.getWriter().write(name);
				}
			};
		}
	}

	public static class S1 extends DummySerializer { public S1(PropertyStore ps) {super(ps, "s1", "text/s1");}}
	public static class S2 extends DummySerializer { public S2(PropertyStore ps) {super(ps, "s2", "text/s2");}}
	public static class S3 extends DummySerializer { public S3(PropertyStore ps) {super(ps, "s3", "text/s3");}}

	//------------------------------------------------------------------------------------------------------------------
	// Test that default Accept headers on servlet annotation are picked up.
	//------------------------------------------------------------------------------------------------------------------

	@Rest(
		defaultRequestHeaders={" Accept : text/s2 "},
		serializers={S1.class,S2.class}
	)
	public static class A {
		@RestOp
		public String put(@Body String in) {
			return in;
		}
	}

	@Test
	public void a01_defaultHeadersOnServletAnnotation() throws Exception {
		RestClient a = MockRestClient.buildLax(A.class);
		a.put("/", null)
			.run()
			.assertBody().is("s2");
		a.put("/", null)
			.accept("text/s1")
			.run()
			.assertBody().is("s1");
		a.put("/", null)
			.accept("text/s2")
			.run()
			.assertBody().is("s2");
		a.put("?noTrace=true", null)
			.accept("text/s3")
			.run()
			.assertCode().is(406)
			.assertBody().contains("Unsupported media-type in request header 'Accept': 'text/s3'");
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
		public String put(@Body String in) {
			return in;
		}
	}

	@Test
	public void b01_restMethodWithParsersSerializers() throws Exception {
		RestClient b = MockRestClient.buildLax(B.class);
		b.put("/", null).accept("text/s3").run().assertBody().is("s3");
		b.put("?noTrace=true", null)
			.accept("text/s4")
			.run()
			.assertCode().is(406)
			.assertBody().contains(
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
		@RestOp(serializers={S3.class,Inherit.class})
		public String put(@Body String in) {
			return in;
		}
	}

	@Test
	public void c01_restMethodAddParsersSerializersInherit() throws Exception {
		RestClient c = MockRestClient.buildLax(C.class);
		c.put("/", null)
			.run()
			.assertBody().is("s2");
		c.put("/", null)
			.accept("text/s1")
			.run()
			.assertBody().is("s1");
		c.put("/", null)
			.accept("text/s2")
			.run()
			.assertBody().is("s2");
		c.put("/", null)
			.accept("text/s3")
			.run()
			.assertBody().is("s3");
		c.put("?noTrace=true", null)
			.accept("text/s4")
			.run()
			.assertCode().is(406)
			.assertBody().contains(
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
		public String put(@Body String in) {
			return in;
		}
	}

	@Test
	public void d01_accept_valid() throws Exception {
		RestClient d = MockRestClient.buildLax(D.class);
		// "*/*" should match the first serializer, not the default serializer.
		d.put("/", null).accept("*/*").run().assertBody().is("s1");
		// "text/*" should match the first serializer, not the default serializer.
		d.put("/", null).accept("text/*").run().assertBody().is("s1");
		d.put("/", null).accept("bad/*,text/*").run().assertBody().is("s1");
		d.put("/", null).accept("text/*,bad/*").run().assertBody().is("s1");
		d.put("/", null).accept("text/s1;q=0.5,text/s2").run().assertBody().is("s2");
		d.put("/", null).accept("text/s1,text/s2;q=0.5").run().assertBody().is("s1");
		d.put("?noTrace=true", null)
			.accept("bad/*")
			.run()
			.assertCode().is(406)
			.assertBody().contains(
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
		public String put(@Body String in) {
			return in;
		}
	}

	@Test
	public void e01_restMethodParserSerializerAnnotations() throws Exception {
		RestClient e = MockRestClient.buildLax(E.class);
		e.put("/", null).run().assertBody().is("s3");
		e.put("/", null).accept("text/s3").run().assertBody().is("s3");
		e.put("?noTrace=true", null)
			.accept("text/s1")
			.run()
			.assertCode().is(406)
			.assertBody().contains(
				"Unsupported media-type in request header 'Accept': 'text/s1'",
				"Supported media-types: ['text/s3']"
			);
		e.put("?noTrace=true", null).accept("text/s2").run()
			.assertCode().is(406)
			.assertBody().contains(
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
		@RestOp(defaultRequestHeaders={"Accept: text/s3"}, serializers={Inherit.class, S3.class})
		public String put(@Body String in) {
			return in;
		}
	}

	@Test
	public void f01_restMethodAddParsersSerializersAnnotations() throws Exception {
		RestClient f = MockRestClient.build(F.class);
		f.put("/", null).run().assertBody().is("s3");
		f.put("/", null).accept("text/s1").run().assertBody().is("s1");
		f.put("/", null).accept("text/s2").run().assertBody().is("s2");
		f.put("/", null).accept("text/s3").run().assertBody().is("s3");
	}
}
