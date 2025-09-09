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

import static org.apache.juneau.TestUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.encoders.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.rest.converter.*;
import org.apache.juneau.rest.guard.*;
import org.apache.juneau.rest.matcher.*;
import org.apache.juneau.serializer.*;
import org.junit.jupiter.api.*;

class RestPutAnnotation_Test extends SimpleTestBase {

	private static final String CNAME = RestPutAnnotation_Test.class.getName();

	//------------------------------------------------------------------------------------------------------------------
	// Basic tests
	//------------------------------------------------------------------------------------------------------------------

	RestPut a1 = RestPutAnnotation.create()
		.clientVersion("a")
		.consumes("b")
		.converters(RestConverter.class)
		.debug("c")
		.defaultAccept("d")
		.defaultCharset("e")
		.defaultContentType("f")
		.defaultRequestFormData("g")
		.defaultRequestQueryData("h")
		.defaultRequestAttributes("i")
		.defaultRequestHeaders("j")
		.defaultResponseHeaders("k")
		.description("l")
		.encoders(Encoder.class)
		.guards(RestGuard.class)
		.matchers(RestMatcher.class)
		.maxInput("m")
		.on("n")
		.parsers(Parser.class)
		.path("o")
		.produces("p")
		.roleGuard("q")
		.rolesDeclared("r")
		.serializers(Serializer.class)
		.summary("s")
		.swagger(OpSwaggerAnnotation.DEFAULT)
		.value("t")
		.build();

	RestPut a2 = RestPutAnnotation.create()
		.clientVersion("a")
		.consumes("b")
		.converters(RestConverter.class)
		.debug("c")
		.defaultAccept("d")
		.defaultCharset("e")
		.defaultContentType("f")
		.defaultRequestFormData("g")
		.defaultRequestQueryData("h")
		.defaultRequestAttributes("i")
		.defaultRequestHeaders("j")
		.defaultResponseHeaders("k")
		.description("l")
		.encoders(Encoder.class)
		.guards(RestGuard.class)
		.matchers(RestMatcher.class)
		.maxInput("m")
		.on("n")
		.parsers(Parser.class)
		.path("o")
		.produces("p")
		.roleGuard("q")
		.rolesDeclared("r")
		.serializers(Serializer.class)
		.summary("s")
		.swagger(OpSwaggerAnnotation.DEFAULT)
		.value("t")
		.build();

	@Test void a01_basic() {
		assertBean(a1,
			"clientVersion,consumes,converters,debug,defaultAccept,defaultCharset,defaultContentType,defaultRequestAttributes,defaultRequestFormData,defaultRequestHeaders,defaultRequestQueryData,defaultResponseHeaders,description,encoders,guards,matchers,maxInput,on,parsers,path,produces,roleGuard,rolesDeclared,serializers,summary,swagger{consumes,deprecated,description,externalDocs{description,url},operationId,parameters,produces,responses,schemes,summary,tags,value},value",
			"a,[b],[RestConverter],c,d,e,f,[i],[g],[j],[h],[k],[l],[Encoder],[RestGuard],[RestMatcher],m,[n],[Parser],[o],[p],q,r,[Serializer],s,{[],,[],{[],},,[],[],[],[],[],[],[]},t");
	}

	@Test void a02_testEquivalency() {
		assertEquals(a2, a1);
		assertNotEqualsAny(a1.hashCode(), 0, -1);
		assertEquals(a1.hashCode(), a2.hashCode());
	}

	//------------------------------------------------------------------------------------------------------------------
	// PropertyStore equivalency.
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_testEquivalencyInPropertyStores() {
		var bc1 = BeanContext.create().annotations(a1).build();
		var bc2 = BeanContext.create().annotations(a2).build();
		assertSame(bc1, bc2);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Other methods.
	//------------------------------------------------------------------------------------------------------------------

	public static class C1 {
		public int f1;
		public void m1() {}  // NOSONAR
	}
	public static class C2 {
		public int f2;
		public void m2() {}  // NOSONAR
	}

	@Test void c01_otherMethods() throws Exception {
		var c4 = RestPutAnnotation.create().on(C1.class.getMethod("m1")).on(C2.class.getMethod("m2")).build();

		assertBean(c4, "on", "["+CNAME+"$C1.m1(),"+CNAME+"$C2.m2()]");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Comparison with declared annotations.
	//------------------------------------------------------------------------------------------------------------------

	public interface D1 {

		@RestPut(
			clientVersion="a",
			consumes="b",
			converters=RestConverter.class,
			debug="c",
			defaultAccept="d",
			defaultCharset="e",
			defaultContentType="f",
			defaultRequestFormData="g",
			defaultRequestQueryData="h",
			defaultRequestAttributes="i",
			defaultRequestHeaders="j",
			defaultResponseHeaders="k",
			description="l",
			encoders=Encoder.class,
			guards=RestGuard.class,
			matchers=RestMatcher.class,
			maxInput="m",
			on="n",
			parsers=Parser.class,
			path="o",
			produces="p",
			roleGuard="q",
			rolesDeclared="r",
			serializers=Serializer.class,
			summary="s",
			swagger=@OpSwagger,
			value="t"
		)
		void m1();

		@RestPut(
			clientVersion="a",
			consumes="b",
			converters=RestConverter.class,
			debug="c",
			defaultAccept="d",
			defaultCharset="e",
			defaultContentType="f",
			defaultRequestFormData="g",
			defaultRequestQueryData="h",
			defaultRequestAttributes="i",
			defaultRequestHeaders="j",
			defaultResponseHeaders="k",
			description="l",
			encoders=Encoder.class,
			guards=RestGuard.class,
			matchers=RestMatcher.class,
			maxInput="m",
			on="n",
			parsers=Parser.class,
			path="o",
			produces="p",
			roleGuard="q",
			rolesDeclared="r",
			serializers=Serializer.class,
			summary="s",
			swagger=@OpSwagger,
			value="t"
		)
		void m2();
	}

	RestPut d1, d2;
	{
		try {
			d1 = D1.class.getMethod("m1").getAnnotationsByType(RestPut.class)[0];
			d2 = D1.class.getMethod("m2").getAnnotationsByType(RestPut.class)[0];

		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Test void d01_comparisonWithDeclarativeAnnotations() {
		assertEqualsAll(a1, d1, d2);
		assertNotEqualsAny(a1.hashCode(), 0, -1);
		assertEqualsAll(a1.hashCode(), d1.hashCode(), d2.hashCode());
	}
}