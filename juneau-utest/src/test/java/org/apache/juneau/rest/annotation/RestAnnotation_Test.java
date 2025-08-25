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

import static org.junit.Assert.*;
import static org.apache.juneau.TestUtils.*;

import org.apache.juneau.*;
import org.apache.juneau.cp.*;
import org.apache.juneau.encoders.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.arg.*;
import org.apache.juneau.rest.converter.*;
import org.apache.juneau.rest.debug.*;
import org.apache.juneau.rest.guard.*;
import org.apache.juneau.rest.logger.*;
import org.apache.juneau.rest.processor.*;
import org.apache.juneau.rest.staticfile.*;
import org.apache.juneau.rest.swagger.*;
import org.apache.juneau.serializer.*;
import org.junit.jupiter.api.*;

class RestAnnotation_Test extends SimpleTestBase {

	private static final String CNAME = RestAnnotation_Test.class.getName();

	//------------------------------------------------------------------------------------------------------------------
	// Basic tests
	//------------------------------------------------------------------------------------------------------------------

	Rest a1 = RestAnnotation.create()
		.disableContentParam("disableContentParam")
		.allowedHeaderParams("allowedHeaderParams")
		.allowedMethodHeaders("allowedMethodHeaders")
		.allowedMethodParams("allowedMethodParams")
		.beanStore(BeanStore.class)
		.callLogger(CallLogger.class)
		.children(RestAnnotation_Test.class)
		.clientVersionHeader("clientVersionHeader")
		.config("config")
		.consumes("consumes")
		.converters(RestConverter.class)
		.debug("debug")
		.debugEnablement(DebugEnablement.class)
		.debugOn("debugOn")
		.defaultAccept("defaultAccept")
		.defaultCharset("defaultCharset")
		.defaultContentType("defaultContentType")
		.defaultRequestAttributes("defaultRequestAttributes")
		.defaultRequestHeaders("defaultRequestHeaders")
		.defaultResponseHeaders("defaultResponseHeaders")
		.description("description")
		.encoders(Encoder.class)
		.guards(RestGuard.class)
		.maxInput("maxInput")
		.messages("messages")
		.on("on")
		.onClass(RestAnnotation_Test.class)
		.parsers(Parser.class)
		.partParser(HttpPartParser.class)
		.partSerializer(HttpPartSerializer.class)
		.path("path")
		.produces("produces")
		.renderResponseStackTraces("renderResponseStackTraces")
		.responseProcessors(ResponseProcessor.class)
		.restChildrenClass(RestChildren.class)
		.restOpArgs(RestOpArg.class)
		.restOperationsClass(RestOperations.class)
		.roleGuard("roleGuard")
		.rolesDeclared("rolesDeclared")
		.serializers(Serializer.class)
		.siteName("siteName")
		.staticFiles(StaticFiles.class)
		.swagger(SwaggerAnnotation.DEFAULT)
		.swaggerProvider(BasicSwaggerProvider.class)
		.title("title")
		.uriAuthority("uriAuthority")
		.uriContext("uriContext")
		.uriRelativity("uriRelativity")
		.uriResolution("uriResolution")
		.build();

	Rest a2 = RestAnnotation.create()
		.disableContentParam("disableContentParam")
		.allowedHeaderParams("allowedHeaderParams")
		.allowedMethodHeaders("allowedMethodHeaders")
		.allowedMethodParams("allowedMethodParams")
		.beanStore(BeanStore.class)
		.callLogger(CallLogger.class)
		.children(RestAnnotation_Test.class)
		.clientVersionHeader("clientVersionHeader")
		.config("config")
		.consumes("consumes")
		.converters(RestConverter.class)
		.debug("debug")
		.debugEnablement(DebugEnablement.class)
		.debugOn("debugOn")
		.defaultAccept("defaultAccept")
		.defaultCharset("defaultCharset")
		.defaultContentType("defaultContentType")
		.defaultRequestAttributes("defaultRequestAttributes")
		.defaultRequestHeaders("defaultRequestHeaders")
		.defaultResponseHeaders("defaultResponseHeaders")
		.description("description")
		.encoders(Encoder.class)
		.guards(RestGuard.class)
		.maxInput("maxInput")
		.messages("messages")
		.on("on")
		.onClass(RestAnnotation_Test.class)
		.parsers(Parser.class)
		.partParser(HttpPartParser.class)
		.partSerializer(HttpPartSerializer.class)
		.path("path")
		.produces("produces")
		.renderResponseStackTraces("renderResponseStackTraces")
		.responseProcessors(ResponseProcessor.class)
		.restChildrenClass(RestChildren.class)
		.restOpArgs(RestOpArg.class)
		.restOperationsClass(RestOperations.class)
		.roleGuard("roleGuard")
		.rolesDeclared("rolesDeclared")
		.serializers(Serializer.class)
		.siteName("siteName")
		.staticFiles(StaticFiles.class)
		.swagger(SwaggerAnnotation.DEFAULT)
		.swaggerProvider(BasicSwaggerProvider.class)
		.title("title")
		.uriAuthority("uriAuthority")
		.uriContext("uriContext")
		.uriRelativity("uriRelativity")
		.uriResolution("uriResolution")
		.build();

	@Test void a01_basic() {
		assertJson(a1, ""
			+ "{"
				+ "allowedHeaderParams:'allowedHeaderParams',"
				+ "allowedMethodHeaders:'allowedMethodHeaders',"
				+ "allowedMethodParams:'allowedMethodParams',"
				+ "beanStore:'org.apache.juneau.cp.BeanStore',"
				+ "callLogger:'org.apache.juneau.rest.logger.CallLogger',"
				+ "children:['"+CNAME+"'],"
				+ "clientVersionHeader:'clientVersionHeader',"
				+ "config:'config',"
				+ "consumes:['consumes'],"
				+ "converters:['org.apache.juneau.rest.converter.RestConverter'],"
				+ "debug:'debug',"
				+ "debugEnablement:'org.apache.juneau.rest.debug.DebugEnablement',"
				+ "debugOn:'debugOn',"
				+ "defaultAccept:'defaultAccept',"
				+ "defaultCharset:'defaultCharset',"
				+ "defaultContentType:'defaultContentType',"
				+ "defaultRequestAttributes:['defaultRequestAttributes'],"
				+ "defaultRequestHeaders:['defaultRequestHeaders'],"
				+ "defaultResponseHeaders:['defaultResponseHeaders'],"
				+ "description:['description'],"
				+ "disableContentParam:'disableContentParam',"
				+ "encoders:['org.apache.juneau.encoders.Encoder'],"
				+ "guards:['org.apache.juneau.rest.guard.RestGuard'],"
				+ "maxInput:'maxInput',"
				+ "messages:'messages',"
				+ "on:['on'],"
				+ "onClass:['"+CNAME+"'],"
				+ "parsers:['org.apache.juneau.parser.Parser'],"
				+ "partParser:'org.apache.juneau.httppart.HttpPartParser',"
				+ "partSerializer:'org.apache.juneau.httppart.HttpPartSerializer',"
				+ "path:'path',"
				+ "produces:['produces'],"
				+ "renderResponseStackTraces:'renderResponseStackTraces',"
				+ "responseProcessors:['org.apache.juneau.rest.processor.ResponseProcessor'],"
				+ "restChildrenClass:'org.apache.juneau.rest.RestChildren',"
				+ "restOpArgs:['org.apache.juneau.rest.arg.RestOpArg'],"
				+ "restOperationsClass:'org.apache.juneau.rest.RestOperations',"
				+ "roleGuard:'roleGuard',"
				+ "rolesDeclared:'rolesDeclared',"
				+ "serializers:['org.apache.juneau.serializer.Serializer'],"
				+ "siteName:'siteName',"
				+ "staticFiles:'org.apache.juneau.rest.staticfile.StaticFiles',"
				+ "swagger:{contact:{description:[],email:'',name:'',url:''},description:[],externalDocs:{description:[],url:''},license:{description:[],name:'',url:''},tags:[],termsOfService:[],title:[],value:[],version:''},"
				+ "swaggerProvider:'org.apache.juneau.rest.swagger.BasicSwaggerProvider',"
				+ "title:['title'],"
				+ "uriAuthority:'uriAuthority',"
				+ "uriContext:'uriContext',"
				+ "uriRelativity:'uriRelativity',"
				+ "uriResolution:'uriResolution'"
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

	@Test void c01_otherMethods() {
		Rest c1 = RestAnnotation.create(C1.class).on(C2.class).build();
		Rest c2 = RestAnnotation.create("a").on("b").build();

		assertJsonContains(c1, "on:['"+CNAME+"$C1','"+CNAME+"$C2']");
		assertJsonContains(c2, "on:['a','b']");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Comparison with declared annotations.
	//------------------------------------------------------------------------------------------------------------------

	@Rest(
		disableContentParam="disableContentParam",
		allowedHeaderParams="allowedHeaderParams",
		allowedMethodHeaders="allowedMethodHeaders",
		allowedMethodParams="allowedMethodParams",
		beanStore=BeanStore.class,
		callLogger=CallLogger.class,
		children=RestAnnotation_Test.class,
		clientVersionHeader="clientVersionHeader",
		config="config",
		consumes="consumes",
		converters=RestConverter.class,
		debug="debug",
		debugEnablement=DebugEnablement.class,
		debugOn="debugOn",
		defaultAccept="defaultAccept",
		defaultCharset="defaultCharset",
		defaultContentType="defaultContentType",
		defaultRequestAttributes="defaultRequestAttributes",
		defaultRequestHeaders="defaultRequestHeaders",
		defaultResponseHeaders="defaultResponseHeaders",
		description="description",
		encoders=Encoder.class,
		guards=RestGuard.class,
		maxInput="maxInput",
		messages="messages",
		on="on",
		onClass=RestAnnotation_Test.class,
		parsers=Parser.class,
		partParser=HttpPartParser.class,
		partSerializer=HttpPartSerializer.class,
		path="path",
		produces="produces",
		renderResponseStackTraces="renderResponseStackTraces",
		responseProcessors=ResponseProcessor.class,
		restChildrenClass=RestChildren.class,
		restOpArgs=RestOpArg.class,
		restOperationsClass=RestOperations.class,
		roleGuard="roleGuard",
		rolesDeclared="rolesDeclared",
		serializers=Serializer.class,
		siteName="siteName",
		staticFiles=StaticFiles.class,
		swagger=@Swagger,
		swaggerProvider=BasicSwaggerProvider.class,
		title="title",
		uriAuthority="uriAuthority",
		uriContext="uriContext",
		uriRelativity="uriRelativity",
		uriResolution="uriResolution"
	)
	public static class D1 {}
	Rest d1 = D1.class.getAnnotationsByType(Rest.class)[0];

	@Rest(
		disableContentParam="disableContentParam",
		allowedHeaderParams="allowedHeaderParams",
		allowedMethodHeaders="allowedMethodHeaders",
		allowedMethodParams="allowedMethodParams",
		beanStore=BeanStore.class,
		callLogger=CallLogger.class,
		children=RestAnnotation_Test.class,
		clientVersionHeader="clientVersionHeader",
		config="config",
		consumes="consumes",
		converters=RestConverter.class,
		debug="debug",
		debugEnablement=DebugEnablement.class,
		debugOn="debugOn",
		defaultAccept="defaultAccept",
		defaultCharset="defaultCharset",
		defaultContentType="defaultContentType",
		defaultRequestAttributes="defaultRequestAttributes",
		defaultRequestHeaders="defaultRequestHeaders",
		defaultResponseHeaders="defaultResponseHeaders",
		description="description",
		encoders=Encoder.class,
		guards=RestGuard.class,
		maxInput="maxInput",
		messages="messages",
		on="on",
		onClass=RestAnnotation_Test.class,
		parsers=Parser.class,
		partParser=HttpPartParser.class,
		partSerializer=HttpPartSerializer.class,
		path="path",
		produces="produces",
		renderResponseStackTraces="renderResponseStackTraces",
		responseProcessors=ResponseProcessor.class,
		restChildrenClass=RestChildren.class,
		restOpArgs=RestOpArg.class,
		restOperationsClass=RestOperations.class,
		roleGuard="roleGuard",
		rolesDeclared="rolesDeclared",
		serializers=Serializer.class,
		siteName="siteName",
		staticFiles=StaticFiles.class,
		swagger=@Swagger,
		swaggerProvider=BasicSwaggerProvider.class,
		title="title",
		uriAuthority="uriAuthority",
		uriContext="uriContext",
		uriRelativity="uriRelativity",
		uriResolution="uriResolution"
	)
	public static class D2 {}
	Rest d2 = D2.class.getAnnotationsByType(Rest.class)[0];

	@Test void d01_comparisonWithDeclarativeAnnotations() {
		assertEqualsAll(a1, d1, d2);
		assertNotEqualsAny(a1.hashCode(), 0, -1);
		assertEqualsAll(a1.hashCode(), d1.hashCode(), d2.hashCode());
	}
}