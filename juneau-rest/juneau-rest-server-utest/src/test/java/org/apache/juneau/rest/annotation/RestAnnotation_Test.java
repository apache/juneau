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
import org.apache.juneau.cp.*;
import org.apache.juneau.encoders.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.serializer.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
@SuppressWarnings("unchecked")
public class RestAnnotation_Test {

	private static final String CNAME = RestAnnotation_Test.class.getName();

	//------------------------------------------------------------------------------------------------------------------
	// Basic tests
	//------------------------------------------------------------------------------------------------------------------

	Rest a1 = RestAnnotation.create()
		.disableAllowBodyParam("disableAllowBodyParam")
		.allowedHeaderParams("allowedHeaderParams")
		.allowedMethodHeaders("allowedMethodHeaders")
		.allowedMethodParams("allowedMethodParams")
		.callLogger(BasicRestCallLogger.class)
		.children(RestAnnotation_Test.class)
		.classpathResourceFinder(BasicResourceFinder.class)
		.clientVersionHeader("clientVersionHeader")
		.config("config")
		.consumes("consumes")
		.context(RestContext.class)
		.converters(RestConverter.class)
		.debug("debug")
		.debugOn("debugOn")
		.defaultAccept("defaultAccept")
		.defaultCharset("defaultCharset")
		.defaultContentType("defaultContentType")
		.description("description")
		.encoders(Encoder.class)
		.flags("flags")
		.guards(RestGuard.class)
		.infoProvider(BasicRestInfoProvider.class)
		.logging(LoggingAnnotation.DEFAULT)
		.maxInput("maxInput")
		.messages("messages")
		.mimeTypes("mimeTypes")
		.on("on")
		.onClass(RestAnnotation_Test.class)
		.paramResolvers(RestMethodParam.class)
		.parsers(Parser.class)
		.partParser(HttpPartParser.class)
		.partSerializer(HttpPartSerializer.class)
		.path("path")
		.produces("produces")
		.properties()
		.renderResponseStackTraces("renderResponseStackTraces")
		.reqAttrs("reqAttrs")
		.reqHeaders("reqHeaders")
		.resHeaders("resHeaders")
		.resourceResolver(RestResourceResolver.class)
		.responseHandlers(ResponseHandler.class)
		.roleGuard("roleGuard")
		.rolesDeclared("rolesDeclared")
		.serializers(Serializer.class)
		.siteName("siteName")
		.staticFileResponseHeaders("staticFileResponseHeaders")
		.staticFiles("staticFiles")
		.swagger(ResourceSwaggerAnnotation.DEFAULT)
		.title("title")
		.uriAuthority("uriAuthority")
		.uriContext("uriContext")
		.uriRelativity("uriRelativity")
		.uriResolution("uriResolution")
		.disableClasspathResourceCaching("disableClasspathResourceCaching")
		.build();

	Rest a2 = RestAnnotation.create()
		.disableAllowBodyParam("disableAllowBodyParam")
		.allowedHeaderParams("allowedHeaderParams")
		.allowedMethodHeaders("allowedMethodHeaders")
		.allowedMethodParams("allowedMethodParams")
		.callLogger(BasicRestCallLogger.class)
		.children(RestAnnotation_Test.class)
		.classpathResourceFinder(BasicResourceFinder.class)
		.clientVersionHeader("clientVersionHeader")
		.config("config")
		.consumes("consumes")
		.context(RestContext.class)
		.converters(RestConverter.class)
		.debug("debug")
		.debugOn("debugOn")
		.defaultAccept("defaultAccept")
		.defaultCharset("defaultCharset")
		.defaultContentType("defaultContentType")
		.description("description")
		.encoders(Encoder.class)
		.flags("flags")
		.guards(RestGuard.class)
		.infoProvider(BasicRestInfoProvider.class)
		.logging(LoggingAnnotation.DEFAULT)
		.maxInput("maxInput")
		.messages("messages")
		.mimeTypes("mimeTypes")
		.on("on")
		.onClass(RestAnnotation_Test.class)
		.paramResolvers(RestMethodParam.class)
		.parsers(Parser.class)
		.partParser(HttpPartParser.class)
		.partSerializer(HttpPartSerializer.class)
		.path("path")
		.produces("produces")
		.properties()
		.renderResponseStackTraces("renderResponseStackTraces")
		.reqAttrs("reqAttrs")
		.reqHeaders("reqHeaders")
		.resHeaders("resHeaders")
		.resourceResolver(RestResourceResolver.class)
		.responseHandlers(ResponseHandler.class)
		.roleGuard("roleGuard")
		.rolesDeclared("rolesDeclared")
		.serializers(Serializer.class)
		.siteName("siteName")
		.staticFileResponseHeaders("staticFileResponseHeaders")
		.staticFiles("staticFiles")
		.swagger(ResourceSwaggerAnnotation.DEFAULT)
		.title("title")
		.uriAuthority("uriAuthority")
		.uriContext("uriContext")
		.uriRelativity("uriRelativity")
		.uriResolution("uriResolution")
		.disableClasspathResourceCaching("disableClasspathResourceCaching")
		.build();

	@Test
	public void a01_basic() {
		assertObject(a1).stderr().json().is(""
			+ "{"
				+ "allowedHeaderParams:'allowedHeaderParams',"
				+ "allowedMethodHeaders:'allowedMethodHeaders',"
				+ "allowedMethodParams:'allowedMethodParams',"
				+ "callLogger:'org.apache.juneau.rest.BasicRestCallLogger',"
				+ "children:['"+CNAME+"'],"
				+ "classpathResourceFinder:'org.apache.juneau.cp.BasicResourceFinder',"
				+ "clientVersionHeader:'clientVersionHeader',"
				+ "config:'config',"
				+ "consumes:['consumes'],"
				+ "context:'org.apache.juneau.rest.RestContext',"
				+ "converters:['org.apache.juneau.rest.RestConverter'],"
				+ "debug:'debug',"
				+ "debugOn:'debugOn',"
				+ "defaultAccept:'defaultAccept',"
				+ "defaultCharset:'defaultCharset',"
				+ "defaultContentType:'defaultContentType',"
				+ "description:['description'],"
				+ "disableAllowBodyParam:'disableAllowBodyParam',"
				+ "disableClasspathResourceCaching:'disableClasspathResourceCaching',"
				+ "encoders:['org.apache.juneau.encoders.Encoder'],"
				+ "flags:['flags'],"
				+ "guards:['org.apache.juneau.rest.RestGuard'],"
				+ "infoProvider:'org.apache.juneau.rest.BasicRestInfoProvider',"
				+ "logging:{disabled:'',level:'',rules:[],stackTraceHashingTimeout:'',useStackTraceHashing:''},"
				+ "maxInput:'maxInput',"
				+ "messages:'messages',"
				+ "mimeTypes:['mimeTypes'],"
				+ "on:['on'],"
				+ "onClass:['"+CNAME+"'],"
				+ "paramResolvers:['org.apache.juneau.rest.RestMethodParam'],"
				+ "parsers:['org.apache.juneau.parser.Parser'],"
				+ "partParser:'org.apache.juneau.httppart.HttpPartParser',"
				+ "partSerializer:'org.apache.juneau.httppart.HttpPartSerializer',"
				+ "path:'path',"
				+ "produces:['produces'],"
				+ "properties:[],"
				+ "renderResponseStackTraces:'renderResponseStackTraces',"
				+ "reqAttrs:['reqAttrs'],"
				+ "reqHeaders:['reqHeaders'],"
				+ "resHeaders:['resHeaders'],"
				+ "resourceResolver:'org.apache.juneau.rest.RestResourceResolver',"
				+ "responseHandlers:['org.apache.juneau.rest.ResponseHandler'],"
				+ "roleGuard:'roleGuard',"
				+ "rolesDeclared:'rolesDeclared',"
				+ "serializers:['org.apache.juneau.serializer.Serializer'],"
				+ "siteName:'siteName',"
				+ "staticFileResponseHeaders:['staticFileResponseHeaders'],"
				+ "staticFiles:['staticFiles'],"
				+ "swagger:{contact:{email:'',name:'',url:'',value:[]},description:[],externalDocs:{description:[],url:'',value:[]},license:{name:'',url:'',value:[]},tags:[],termsOfService:[],title:[],value:[],version:''},"
				+ "title:['title'],"
				+ "uriAuthority:'uriAuthority',"
				+ "uriContext:'uriContext',"
				+ "uriRelativity:'uriRelativity',"
				+ "uriResolution:'uriResolution'"
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
		Rest c1 = RestAnnotation.create(C1.class).on(C2.class).build();
		Rest c2 = RestAnnotation.create("a").on("b").build();

		assertObject(c1).json().contains("on:['"+CNAME+"$C1','"+CNAME+"$C2']");
		assertObject(c2).json().contains("on:['a','b']");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Comparison with declared annotations.
	//------------------------------------------------------------------------------------------------------------------

	@Rest(
		disableAllowBodyParam="disableAllowBodyParam",
		allowedHeaderParams="allowedHeaderParams",
		allowedMethodHeaders="allowedMethodHeaders",
		allowedMethodParams="allowedMethodParams",
		callLogger=BasicRestCallLogger.class,
		children=RestAnnotation_Test.class,
		classpathResourceFinder=BasicResourceFinder.class,
		clientVersionHeader="clientVersionHeader",
		config="config",
		consumes="consumes",
		context=RestContext.class,
		converters=RestConverter.class,
		debug="debug",
		debugOn="debugOn",
		defaultAccept="defaultAccept",
		defaultCharset="defaultCharset",
		defaultContentType="defaultContentType",
		description="description",
		encoders=Encoder.class,
		flags="flags",
		guards=RestGuard.class,
		infoProvider=BasicRestInfoProvider.class,
		logging=@Logging,
		maxInput="maxInput",
		messages="messages",
		mimeTypes="mimeTypes",
		on="on",
		onClass=RestAnnotation_Test.class,
		paramResolvers=RestMethodParam.class,
		parsers=Parser.class,
		partParser=HttpPartParser.class,
		partSerializer=HttpPartSerializer.class,
		path="path",
		produces="produces",
		properties={},
		renderResponseStackTraces="renderResponseStackTraces",
		reqAttrs="reqAttrs",
		reqHeaders="reqHeaders",
		resHeaders="resHeaders",
		resourceResolver=RestResourceResolver.class,
		responseHandlers=ResponseHandler.class,
		roleGuard="roleGuard",
		rolesDeclared="rolesDeclared",
		serializers=Serializer.class,
		siteName="siteName",
		staticFileResponseHeaders="staticFileResponseHeaders",
		staticFiles="staticFiles",
		swagger=@ResourceSwagger,
		title="title",
		uriAuthority="uriAuthority",
		uriContext="uriContext",
		uriRelativity="uriRelativity",
		uriResolution="uriResolution",
		disableClasspathResourceCaching="disableClasspathResourceCaching"
	)
	public static class D1 {}
	Rest d1 = D1.class.getAnnotationsByType(Rest.class)[0];

	@Rest(
		disableAllowBodyParam="disableAllowBodyParam",
		allowedHeaderParams="allowedHeaderParams",
		allowedMethodHeaders="allowedMethodHeaders",
		allowedMethodParams="allowedMethodParams",
		callLogger=BasicRestCallLogger.class,
		children=RestAnnotation_Test.class,
		classpathResourceFinder=BasicResourceFinder.class,
		clientVersionHeader="clientVersionHeader",
		config="config",
		consumes="consumes",
		context=RestContext.class,
		converters=RestConverter.class,
		debug="debug",
		debugOn="debugOn",
		defaultAccept="defaultAccept",
		defaultCharset="defaultCharset",
		defaultContentType="defaultContentType",
		description="description",
		encoders=Encoder.class,
		flags="flags",
		guards=RestGuard.class,
		infoProvider=BasicRestInfoProvider.class,
		logging=@Logging,
		maxInput="maxInput",
		messages="messages",
		mimeTypes="mimeTypes",
		on="on",
		onClass=RestAnnotation_Test.class,
		paramResolvers=RestMethodParam.class,
		parsers=Parser.class,
		partParser=HttpPartParser.class,
		partSerializer=HttpPartSerializer.class,
		path="path",
		produces="produces",
		properties={},
		renderResponseStackTraces="renderResponseStackTraces",
		reqAttrs="reqAttrs",
		reqHeaders="reqHeaders",
		resHeaders="resHeaders",
		resourceResolver=RestResourceResolver.class,
		responseHandlers=ResponseHandler.class,
		roleGuard="roleGuard",
		rolesDeclared="rolesDeclared",
		serializers=Serializer.class,
		siteName="siteName",
		staticFileResponseHeaders="staticFileResponseHeaders",
		staticFiles="staticFiles",
		swagger=@ResourceSwagger,
		title="title",
		uriAuthority="uriAuthority",
		uriContext="uriContext",
		uriRelativity="uriRelativity",
		uriResolution="uriResolution",
		disableClasspathResourceCaching="disableClasspathResourceCaching"
	)
	public static class D2 {}
	Rest d2 = D2.class.getAnnotationsByType(Rest.class)[0];

	@Test
	public void d01_comparisonWithDeclarativeAnnotations() {
		assertObject(d1).is(d2).is(a1);
		assertInteger(d1.hashCode()).is(d2.hashCode()).is(a1.hashCode()).isNotAny(0,-1);
	}
}
