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

import static org.apache.juneau.assertions.Assertions.*;
import static org.junit.Assert.*;
import org.apache.juneau.*;
import org.apache.juneau.encoders.*;
import org.apache.juneau.rest.guard.*;
import org.apache.juneau.rest.matcher.*;
import org.junit.jupiter.api.*;

class RestDeleteAnnotation_Test extends SimpleTestBase {

	private static final String CNAME = RestDeleteAnnotation_Test.class.getName();

	//------------------------------------------------------------------------------------------------------------------
	// Basic tests
	//------------------------------------------------------------------------------------------------------------------

	RestDelete a1 = RestDeleteAnnotation.create()
		.clientVersion("clientVersion")
		.debug("debug")
		.defaultAccept("defaultAccept")
		.defaultCharset("defaultCharset")
		.defaultRequestQueryData("defaultRequestQueryData")
		.defaultRequestAttributes("defaultRequestAttributes")
		.defaultRequestHeaders("defaultRequestHeaders")
		.defaultResponseHeaders("defaultResponseHeaders")
		.description("description")
		.encoders(Encoder.class)
		.guards(RestGuard.class)
		.matchers(RestMatcher.class)
		.on("on")
		.path("path")
		.roleGuard("roleGuard")
		.rolesDeclared("rolesDeclared")
		.summary("summary")
		.swagger(OpSwaggerAnnotation.DEFAULT)
		.value("value")
		.build();

	RestDelete a2 = RestDeleteAnnotation.create()
		.clientVersion("clientVersion")
		.debug("debug")
		.defaultAccept("defaultAccept")
		.defaultCharset("defaultCharset")
		.defaultRequestQueryData("defaultRequestQueryData")
		.defaultRequestAttributes("defaultRequestAttributes")
		.defaultRequestHeaders("defaultRequestHeaders")
		.defaultResponseHeaders("defaultResponseHeaders")
		.description("description")
		.encoders(Encoder.class)
		.guards(RestGuard.class)
		.matchers(RestMatcher.class)
		.on("on")
		.path("path")
		.roleGuard("roleGuard")
		.rolesDeclared("rolesDeclared")
		.summary("summary")
		.swagger(OpSwaggerAnnotation.DEFAULT)
		.value("value")
		.build();

	@Test void a01_basic() {
		assertJson(a1, ""
			+ "{"
				+ "clientVersion:'clientVersion',"
				+ "debug:'debug',"
				+ "defaultAccept:'defaultAccept',"
				+ "defaultCharset:'defaultCharset',"
				+ "defaultRequestAttributes:['defaultRequestAttributes'],"
				+ "defaultRequestHeaders:['defaultRequestHeaders'],"
				+ "defaultRequestQueryData:['defaultRequestQueryData'],"
				+ "defaultResponseHeaders:['defaultResponseHeaders'],"
				+ "description:['description'],"
				+ "encoders:['org.apache.juneau.encoders.Encoder'],"
				+ "guards:['org.apache.juneau.rest.guard.RestGuard'],"
				+ "matchers:['org.apache.juneau.rest.matcher.RestMatcher'],"
				+ "on:['on'],"
				+ "path:['path'],"
				+ "roleGuard:'roleGuard',"
				+ "rolesDeclared:'rolesDeclared',"
				+ "summary:'summary',"
				+ "swagger:{consumes:[],deprecated:'',description:[],externalDocs:{description:[],url:''},operationId:'',parameters:[],produces:[],responses:[],schemes:[],summary:[],tags:[],value:[]},"
				+ "value:'value'"
			+ "}"
		);
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
		BeanContext bc1 = BeanContext.create().annotations(a1).build();
		BeanContext bc2 = BeanContext.create().annotations(a2).build();
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
		RestDelete c4 = RestDeleteAnnotation.create().on(C1.class.getMethod("m1")).on(C2.class.getMethod("m2")).build();

		assertObject(c4).asJson().isContains("on:['"+CNAME+"$C1.m1()','"+CNAME+"$C2.m2()']");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Comparison with declared annotations.
	//------------------------------------------------------------------------------------------------------------------

	public interface D1 {

		@RestDelete(
			clientVersion="clientVersion",
			debug="debug",
			defaultAccept="defaultAccept",
			defaultCharset="defaultCharset",
			defaultRequestQueryData="defaultRequestQueryData",
			defaultRequestAttributes="defaultRequestAttributes",
			defaultRequestHeaders="defaultRequestHeaders",
			defaultResponseHeaders="defaultResponseHeaders",
			description="description",
			encoders=Encoder.class,
			guards=RestGuard.class,
			matchers=RestMatcher.class,
			on="on",
			path="path",
			roleGuard="roleGuard",
			rolesDeclared="rolesDeclared",
			summary="summary",
			swagger=@OpSwagger,
			value="value"
		)
		void m1();

		@RestDelete(
			clientVersion="clientVersion",
			debug="debug",
			defaultAccept="defaultAccept",
			defaultCharset="defaultCharset",
			defaultRequestQueryData="defaultRequestQueryData",
			defaultRequestAttributes="defaultRequestAttributes",
			defaultRequestHeaders="defaultRequestHeaders",
			defaultResponseHeaders="defaultResponseHeaders",
			description="description",
			encoders=Encoder.class,
			guards=RestGuard.class,
			matchers=RestMatcher.class,
			on="on",
			path="path",
			roleGuard="roleGuard",
			rolesDeclared="rolesDeclared",
			summary="summary",
			swagger=@OpSwagger,
			value="value"
		)
		void m2();
	}

	RestDelete d1, d2;
	{
		try {
			d1 = D1.class.getMethod("m1").getAnnotationsByType(RestDelete.class)[0];
			d2 = D1.class.getMethod("m2").getAnnotationsByType(RestDelete.class)[0];

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