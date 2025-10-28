/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.rest.annotation;

import static org.apache.juneau.TestUtils.*;
import static org.apache.juneau.junit.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

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

class RestAnnotation_Test extends TestBase {

	private static final String CNAME = RestAnnotation_Test.class.getName();

	//------------------------------------------------------------------------------------------------------------------
	// Basic tests
	//------------------------------------------------------------------------------------------------------------------

	Rest a1 = RestAnnotation.create()
		.disableContentParam("a")
		.allowedHeaderParams("b")
		.allowedMethodHeaders("c")
		.allowedMethodParams("d")
		.beanStore(BeanStore.class)
		.callLogger(CallLogger.class)
		.children(RestAnnotation_Test.class)
		.clientVersionHeader("e")
		.config("f")
		.consumes("g")
		.converters(RestConverter.class)
		.debug("h")
		.debugEnablement(DebugEnablement.class)
		.debugOn("i")
		.defaultAccept("j")
		.defaultCharset("k")
		.defaultContentType("l")
		.defaultRequestAttributes("m")
		.defaultRequestHeaders("n")
		.defaultResponseHeaders("o")
		.description("p")
		.encoders(Encoder.class)
		.guards(RestGuard.class)
		.maxInput("q")
		.messages("r")
		.on("s")
		.onClass(RestAnnotation_Test.class)
		.parsers(Parser.class)
		.partParser(HttpPartParser.class)
		.partSerializer(HttpPartSerializer.class)
		.path("t")
		.produces("u")
		.renderResponseStackTraces("v")
		.responseProcessors(ResponseProcessor.class)
		.restChildrenClass(RestChildren.class)
		.restOpArgs(RestOpArg.class)
		.restOperationsClass(RestOperations.class)
		.roleGuard("w")
		.rolesDeclared("x")
		.serializers(Serializer.class)
		.siteName("y")
		.staticFiles(StaticFiles.class)
		.swagger(SwaggerAnnotation.DEFAULT)
		.swaggerProvider(BasicSwaggerProvider.class)
		.title("z")
		.uriAuthority("aa")
		.uriContext("bb")
		.uriRelativity("cc")
		.uriResolution("dd")
		.build();

	Rest a2 = RestAnnotation.create()
		.disableContentParam("a")
		.allowedHeaderParams("b")
		.allowedMethodHeaders("c")
		.allowedMethodParams("d")
		.beanStore(BeanStore.class)
		.callLogger(CallLogger.class)
		.children(RestAnnotation_Test.class)
		.clientVersionHeader("e")
		.config("f")
		.consumes("g")
		.converters(RestConverter.class)
		.debug("h")
		.debugEnablement(DebugEnablement.class)
		.debugOn("i")
		.defaultAccept("j")
		.defaultCharset("k")
		.defaultContentType("l")
		.defaultRequestAttributes("m")
		.defaultRequestHeaders("n")
		.defaultResponseHeaders("o")
		.description("p")
		.encoders(Encoder.class)
		.guards(RestGuard.class)
		.maxInput("q")
		.messages("r")
		.on("s")
		.onClass(RestAnnotation_Test.class)
		.parsers(Parser.class)
		.partParser(HttpPartParser.class)
		.partSerializer(HttpPartSerializer.class)
		.path("t")
		.produces("u")
		.renderResponseStackTraces("v")
		.responseProcessors(ResponseProcessor.class)
		.restChildrenClass(RestChildren.class)
		.restOpArgs(RestOpArg.class)
		.restOperationsClass(RestOperations.class)
		.roleGuard("w")
		.rolesDeclared("x")
		.serializers(Serializer.class)
		.siteName("y")
		.staticFiles(StaticFiles.class)
		.swagger(SwaggerAnnotation.DEFAULT)
		.swaggerProvider(BasicSwaggerProvider.class)
		.title("z")
		.uriAuthority("aa")
		.uriContext("bb")
		.uriRelativity("cc")
		.uriResolution("dd")
		.build();

	@Test void a01_basic() {
		assertBean(a1,
			"allowedHeaderParams,allowedMethodHeaders,allowedMethodParams,beanStore,callLogger,children,clientVersionHeader,config,consumes,converters,debug,debugEnablement,debugOn,defaultAccept,defaultCharset,defaultContentType,defaultRequestAttributes,defaultRequestHeaders,defaultResponseHeaders,description,disableContentParam,encoders,guards,maxInput,messages,on,onClass,parsers,partParser,partSerializer,path,produces,renderResponseStackTraces,responseProcessors,restChildrenClass,restOpArgs,restOperationsClass,roleGuard,rolesDeclared,serializers,siteName,staticFiles,swagger{contact{description,email,name,url},description,externalDocs{description,url},license{description,name,url},tags,termsOfService,title,value,version},swaggerProvider,title,uriAuthority,uriContext,uriRelativity,uriResolution",
			"b,c,d,BeanStore,CallLogger,[RestAnnotation_Test],e,f,[g],[RestConverter],h,DebugEnablement,i,j,k,l,[m],[n],[o],[p],a,[Encoder],[RestGuard],q,r,[s],[RestAnnotation_Test],[Parser],HttpPartParser,HttpPartSerializer,t,[u],v,[ResponseProcessor],RestChildren,[RestOpArg],RestOperations,w,x,[Serializer],y,StaticFiles,{{[],,,},[],{[],},{[],,},[],[],[],[],},BasicSwaggerProvider,[z],aa,bb,cc,dd");
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
		var c1 = RestAnnotation.create(C1.class).on(C2.class).build();
		var c2 = RestAnnotation.create("a").on("b").build();

		assertBean(c1, "on", "["+CNAME+"$C1,"+CNAME+"$C2]");
		assertBean(c2, "on", "[a,b]");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Comparison with declared annotations.
	//------------------------------------------------------------------------------------------------------------------

	@Rest(
		disableContentParam="a",
		allowedHeaderParams="b",
		allowedMethodHeaders="c",
		allowedMethodParams="d",
		beanStore=BeanStore.class,
		callLogger=CallLogger.class,
		children=RestAnnotation_Test.class,
		clientVersionHeader="e",
		config="f",
		consumes="g",
		converters=RestConverter.class,
		debug="h",
		debugEnablement=DebugEnablement.class,
		debugOn="i",
		defaultAccept="j",
		defaultCharset="k",
		defaultContentType="l",
		defaultRequestAttributes="m",
		defaultRequestHeaders="n",
		defaultResponseHeaders="o",
		description="p",
		encoders=Encoder.class,
		guards=RestGuard.class,
		maxInput="q",
		messages="r",
		on="s",
		onClass=RestAnnotation_Test.class,
		parsers=Parser.class,
		partParser=HttpPartParser.class,
		partSerializer=HttpPartSerializer.class,
		path="t",
		produces="u",
		renderResponseStackTraces="v",
		responseProcessors=ResponseProcessor.class,
		restChildrenClass=RestChildren.class,
		restOpArgs=RestOpArg.class,
		restOperationsClass=RestOperations.class,
		roleGuard="w",
		rolesDeclared="x",
		serializers=Serializer.class,
		siteName="y",
		staticFiles=StaticFiles.class,
		swagger=@Swagger,
		swaggerProvider=BasicSwaggerProvider.class,
		title="z",
		uriAuthority="aa",
		uriContext="bb",
		uriRelativity="cc",
		uriResolution="dd"
	)
	public static class D1 {}
	Rest d1 = D1.class.getAnnotationsByType(Rest.class)[0];

	@Rest(
		disableContentParam="a",
		allowedHeaderParams="b",
		allowedMethodHeaders="c",
		allowedMethodParams="d",
		beanStore=BeanStore.class,
		callLogger=CallLogger.class,
		children=RestAnnotation_Test.class,
		clientVersionHeader="e",
		config="f",
		consumes="g",
		converters=RestConverter.class,
		debug="h",
		debugEnablement=DebugEnablement.class,
		debugOn="i",
		defaultAccept="j",
		defaultCharset="k",
		defaultContentType="l",
		defaultRequestAttributes="m",
		defaultRequestHeaders="n",
		defaultResponseHeaders="o",
		description="p",
		encoders=Encoder.class,
		guards=RestGuard.class,
		maxInput="q",
		messages="r",
		on="s",
		onClass=RestAnnotation_Test.class,
		parsers=Parser.class,
		partParser=HttpPartParser.class,
		partSerializer=HttpPartSerializer.class,
		path="t",
		produces="u",
		renderResponseStackTraces="v",
		responseProcessors=ResponseProcessor.class,
		restChildrenClass=RestChildren.class,
		restOpArgs=RestOpArg.class,
		restOperationsClass=RestOperations.class,
		roleGuard="w",
		rolesDeclared="x",
		serializers=Serializer.class,
		siteName="y",
		staticFiles=StaticFiles.class,
		swagger=@Swagger,
		swaggerProvider=BasicSwaggerProvider.class,
		title="z",
		uriAuthority="aa",
		uriContext="bb",
		uriRelativity="cc",
		uriResolution="dd"
	)
	public static class D2 {}
	Rest d2 = D2.class.getAnnotationsByType(Rest.class)[0];

	@Test void d01_comparisonWithDeclarativeAnnotations() {
		assertEqualsAll(a1, d1, d2);
		assertNotEqualsAny(a1.hashCode(), 0, -1);
		assertEqualsAll(a1.hashCode(), d1.hashCode(), d2.hashCode());
	}
}