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
import static org.junit.runners.MethodSorters.*;

import org.apache.juneau.*;
import org.apache.juneau.encoders.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.serializer.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
@SuppressWarnings("unchecked")
public class RestMethodAnnotation_Test {

	private static final String CNAME = RestMethodAnnotation_Test.class.getName();

	//------------------------------------------------------------------------------------------------------------------
	// Basic tests
	//------------------------------------------------------------------------------------------------------------------

	RestMethod a1 = RestMethodAnnotation.create()
		.clientVersion("clientVersion")
		.consumes("consumes")
		.context(RestMethodContext.class)
		.converters(RestConverter.class)
		.debug("debug")
		.defaultAccept("defaultAccept")
		.defaultCharset("defaultCharset")
		.defaultContentType("defaultContentType")
		.defaultFormData("defaultFormData")
		.defaultQuery("defaultQuery")
		.defaultRequestAttributes("defaultRequestAttributes")
		.defaultRequestHeaders("defaultRequestHeaders")
		.defaultResponseHeaders("defaultResponseHeaders")
		.description("description")
		.encoders(Encoder.class)
		.guards(RestGuard.class)
		.matchers(RestMatcher.class)
		.maxInput("maxInput")
		.method("method")
		.on("on")
		.parsers(Parser.class)
		.path("path")
		.priority(1)
		.produces("produces")
		.roleGuard("roleGuard")
		.rolesDeclared("rolesDeclared")
		.serializers(Serializer.class)
		.summary("summary")
		.swagger(MethodSwaggerAnnotation.DEFAULT)
		.value("value")
		.build();

	RestMethod a2 = RestMethodAnnotation.create()
		.clientVersion("clientVersion")
		.consumes("consumes")
		.context(RestMethodContext.class)
		.converters(RestConverter.class)
		.debug("debug")
		.defaultAccept("defaultAccept")
		.defaultCharset("defaultCharset")
		.defaultContentType("defaultContentType")
		.defaultFormData("defaultFormData")
		.defaultQuery("defaultQuery")
		.defaultRequestAttributes("defaultRequestAttributes")
		.defaultRequestHeaders("defaultRequestHeaders")
		.defaultResponseHeaders("defaultResponseHeaders")
		.description("description")
		.encoders(Encoder.class)
		.guards(RestGuard.class)
		.matchers(RestMatcher.class)
		.maxInput("maxInput")
		.method("method")
		.on("on")
		.parsers(Parser.class)
		.path("path")
		.priority(1)
		.produces("produces")
		.roleGuard("roleGuard")
		.rolesDeclared("rolesDeclared")
		.serializers(Serializer.class)
		.summary("summary")
		.swagger(MethodSwaggerAnnotation.DEFAULT)
		.value("value")
		.build();

	@Test
	public void a01_basic() {
		assertObject(a1).json().is(""
			+ "{"
				+ "clientVersion:'clientVersion',"
				+ "consumes:['consumes'],"
				+ "context:'org.apache.juneau.rest.RestMethodContext',"
				+ "converters:['org.apache.juneau.rest.RestConverter'],"
				+ "debug:'debug',"
				+ "defaultAccept:'defaultAccept',"
				+ "defaultCharset:'defaultCharset',"
				+ "defaultContentType:'defaultContentType',"
				+ "defaultFormData:['defaultFormData'],"
				+ "defaultQuery:['defaultQuery'],"
				+ "defaultRequestAttributes:['defaultRequestAttributes'],"
				+ "defaultRequestHeaders:['defaultRequestHeaders'],"
				+ "defaultResponseHeaders:['defaultResponseHeaders'],"
				+ "description:['description'],"
				+ "encoders:['org.apache.juneau.encoders.Encoder'],"
				+ "guards:['org.apache.juneau.rest.RestGuard'],"
				+ "matchers:['org.apache.juneau.rest.RestMatcher'],"
				+ "maxInput:'maxInput',"
				+ "method:'method',"
				+ "on:['on'],"
				+ "parsers:['org.apache.juneau.parser.Parser'],"
				+ "path:['path'],"
				+ "priority:1,"
				+ "produces:['produces'],"
				+ "roleGuard:'roleGuard',"
				+ "rolesDeclared:'rolesDeclared',"
				+ "serializers:['org.apache.juneau.serializer.Serializer'],"
				+ "summary:'summary',"
				+ "swagger:{consumes:[],deprecated:'',description:[],externalDocs:{description:[],url:'',value:[]},operationId:'',parameters:[],produces:[],responses:[],schemes:[],summary:[],tags:[],value:[]},"
				+ "value:'value'"
			+ "}"
		);
	}

	@Test
	public void a02_testEquivalency() {
		assertObject(a1).is(a2);
		assertInteger(a1.hashCode()).is(a2.hashCode()).isNotAny(0,-1);
	}

	//------------------------------------------------------------------------------------------------------------------
	// PropertyStore equivalency.
	//------------------------------------------------------------------------------------------------------------------

	@Test
	public void b01_testEquivalencyInPropertyStores() {
		BeanContext bc1 = BeanContext.create().annotations(a1).build();
		BeanContext bc2 = BeanContext.create().annotations(a2).build();
		assertTrue(bc1 == bc2);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Other methods.
	//------------------------------------------------------------------------------------------------------------------

	public static class C1 {
		public int f1;
		public void m1() {}
	}
	public static class C2 {
		public int f2;
		public void m2() {}
	}

	@Test
	public void c01_otherMethods() throws Exception {
		RestMethod c4 = RestMethodAnnotation.create().on(C1.class.getMethod("m1")).on(C2.class.getMethod("m2")).build();

		assertObject(c4).json().contains("on:['"+CNAME+"$C1.m1()','"+CNAME+"$C2.m2()']");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Comparison with declared annotations.
	//------------------------------------------------------------------------------------------------------------------

	@RestMethod(
		clientVersion="clientVersion",
		consumes="consumes",
		context=RestMethodContext.class,
		converters=RestConverter.class,
		debug="debug",
		defaultAccept="defaultAccept",
		defaultCharset="defaultCharset",
		defaultContentType="defaultContentType",
		defaultFormData="defaultFormData",
		defaultQuery="defaultQuery",
		defaultRequestAttributes="defaultRequestAttributes",
		defaultRequestHeaders="defaultRequestHeaders",
		defaultResponseHeaders="defaultResponseHeaders",
		description="description",
		encoders=Encoder.class,
		guards=RestGuard.class,
		matchers=RestMatcher.class,
		maxInput="maxInput",
		method="method",
		on="on",
		parsers=Parser.class,
		path="path",
		priority=1,
		produces="produces",
		roleGuard="roleGuard",
		rolesDeclared="rolesDeclared",
		serializers=Serializer.class,
		summary="summary",
		swagger=@MethodSwagger,
		value="value"
	)
	public static class D1 {}
	RestMethod d1 = D1.class.getAnnotationsByType(RestMethod.class)[0];

	@RestMethod(
		clientVersion="clientVersion",
		consumes="consumes",
		context=RestMethodContext.class,
		converters=RestConverter.class,
		debug="debug",
		defaultAccept="defaultAccept",
		defaultCharset="defaultCharset",
		defaultContentType="defaultContentType",
		defaultFormData="defaultFormData",
		defaultQuery="defaultQuery",
		defaultRequestAttributes="defaultRequestAttributes",
		defaultRequestHeaders="defaultRequestHeaders",
		defaultResponseHeaders="defaultResponseHeaders",
		description="description",
		encoders=Encoder.class,
		guards=RestGuard.class,
		matchers=RestMatcher.class,
		maxInput="maxInput",
		method="method",
		on="on",
		parsers=Parser.class,
		path="path",
		priority=1,
		produces="produces",
		roleGuard="roleGuard",
		rolesDeclared="rolesDeclared",
		serializers=Serializer.class,
		summary="summary",
		swagger=@MethodSwagger,
		value="value"
	)
	public static class D2 {}
	RestMethod d2 = D2.class.getAnnotationsByType(RestMethod.class)[0];

	@Test
	public void d01_comparisonWithDeclarativeAnnotations() {
		assertObject(d1).is(d2).is(a1);
		assertInteger(d1.hashCode()).is(d2.hashCode()).is(a1.hashCode()).isNotAny(0,-1);
	}
}
