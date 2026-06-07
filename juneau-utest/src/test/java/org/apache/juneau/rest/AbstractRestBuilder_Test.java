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
package org.apache.juneau.rest;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.encoders.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.json.*;
import org.apache.juneau.rest.servlet.*;
import org.junit.jupiter.api.*;

/**
 * Coverage tests for {@link AbstractRestBuilder} fluent setters.
 *
 * <p>Each setter forwards into the synthetic {@code @Rest} override-bag annotation; these tests assert that
 * every fluent setter (a) records the value on the synthetic annotation returned by {@link AbstractRestBuilder#toRestAnnotation()}
 * and (b) returns the builder for chaining.
 */
class AbstractRestBuilder_Test extends TestBase {
	public static class R extends RestResource {}

	private static RestResource.DefaultBuilder<R> b() {
		return RestResource.builder(R.class);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Identity & mounting
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_path() {
		var b = b().path("/foo");
		assertEquals("/foo", b.toRestAnnotation().path());
	}

	@Test void a02_paths() {
		var b = b().paths("/a", "/b");
		assertArrayEquals(new String[]{"/a", "/b"}, b.toRestAnnotation().paths());
	}

	@Test void a03_children() {
		var b = b().children(R.class);
		assertArrayEquals(new Class<?>[]{R.class}, b.toRestAnnotation().children());
	}

	@Test void a04_mixins() {
		var b = b().mixins(R.class);
		assertArrayEquals(new Class<?>[]{R.class}, b.toRestAnnotation().mixins());
	}

	@Test void a05_uriAuthority() {
		assertEquals("https://x", b().uriAuthority("https://x").toRestAnnotation().uriAuthority());
	}

	@Test void a06_uriContext() {
		assertEquals("/ctx", b().uriContext("/ctx").toRestAnnotation().uriContext());
	}

	@Test void a07_uriRelativity() {
		assertEquals("RESOURCE", b().uriRelativity("RESOURCE").toRestAnnotation().uriRelativity());
	}

	@Test void a08_uriResolution() {
		assertEquals("ROOT_RELATIVE", b().uriResolution("ROOT_RELATIVE").toRestAnnotation().uriResolution());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Marshalling
	//------------------------------------------------------------------------------------------------------------------

	@Test
	@SuppressWarnings({
		"unchecked" // Class<? extends Serializer>[] varargs; generic array creation is safe here.
	})
	void b01_serializers() {
		var b = b().serializers(JsonSerializer.class);
		assertArrayEquals(new Class<?>[]{JsonSerializer.class}, b.toRestAnnotation().serializers());
	}

	@Test void b02_parsers() {
		var b = b().parsers(JsonParser.class);
		assertArrayEquals(new Class<?>[]{JsonParser.class}, b.toRestAnnotation().parsers());
	}

	@Test
	@SuppressWarnings({
		"unchecked" // Class<? extends Encoder>[] varargs; generic array creation is safe here.
	})
	void b03_encoders() {
		var b = b().encoders(IdentityEncoder.class);
		assertArrayEquals(new Class<?>[]{IdentityEncoder.class}, b.toRestAnnotation().encoders());
	}

	@Test void b04_partSerializer() {
		var b = b().partSerializer(SimplePartSerializer.class);
		assertEquals(SimplePartSerializer.class, b.toRestAnnotation().partSerializer());
	}

	@Test void b05_partParser() {
		var b = b().partParser(SimplePartParser.class);
		assertEquals(SimplePartParser.class, b.toRestAnnotation().partParser());
	}

	@Test void b06_consumes() {
		var b = b().consumes("application/json");
		assertArrayEquals(new String[]{"application/json"}, b.toRestAnnotation().consumes());
	}

	@Test void b07_produces() {
		var b = b().produces("application/json");
		assertArrayEquals(new String[]{"application/json"}, b.toRestAnnotation().produces());
	}

	@Test
	@SuppressWarnings({
		"unchecked" // Class<? extends ResponseProcessor>[] varargs; generic array creation is safe here.
	})
	void b08_responseProcessors() {
		var b = b().responseProcessors();
		assertEquals(0, b.toRestAnnotation().responseProcessors().length);
	}

	@Test void b09_allowedSerializerOptions() {
		var b = b().allowedSerializerOptions("useWhitespace");
		assertArrayEquals(new String[]{"useWhitespace"}, b.toRestAnnotation().allowedSerializerOptions());
	}

	@Test void b10_allowedParserOptions() {
		var b = b().allowedParserOptions("strict");
		assertArrayEquals(new String[]{"strict"}, b.toRestAnnotation().allowedParserOptions());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Request behavior
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_allowedHeaderParams() {
		assertEquals("X-Foo", b().allowedHeaderParams("X-Foo").toRestAnnotation().allowedHeaderParams());
	}

	@Test void c02_allowedMethodHeaders() {
		assertEquals("X-Method", b().allowedMethodHeaders("X-Method").toRestAnnotation().allowedMethodHeaders());
	}

	@Test void c03_allowedMethodParams() {
		assertEquals("HEAD", b().allowedMethodParams("HEAD").toRestAnnotation().allowedMethodParams());
	}

	@Test void c04_clientVersionHeader() {
		assertEquals("X-Client-Version", b().clientVersionHeader("X-Client-Version").toRestAnnotation().clientVersionHeader());
	}

	@Test void c05_defaultAccept() {
		assertEquals("application/json", b().defaultAccept("application/json").toRestAnnotation().defaultAccept());
	}

	@Test void c06_defaultContentType() {
		assertEquals("application/xml", b().defaultContentType("application/xml").toRestAnnotation().defaultContentType());
	}

	@Test void c07_defaultCharset() {
		assertEquals("UTF-8", b().defaultCharset("UTF-8").toRestAnnotation().defaultCharset());
	}

	@Test void c08_defaultRequestAttributes() {
		var b = b().defaultRequestAttributes("k:v");
		assertArrayEquals(new String[]{"k:v"}, b.toRestAnnotation().defaultRequestAttributes());
	}

	@Test void c09_defaultRequestHeaders() {
		var b = b().defaultRequestHeaders("X-Foo: bar");
		assertArrayEquals(new String[]{"X-Foo: bar"}, b.toRestAnnotation().defaultRequestHeaders());
	}

	@Test void c10_defaultResponseHeaders() {
		var b = b().defaultResponseHeaders("X-Bar: baz");
		assertArrayEquals(new String[]{"X-Bar: baz"}, b.toRestAnnotation().defaultResponseHeaders());
	}

	@Test void c11_disableContentParam() {
		assertEquals("true", b().disableContentParam("true").toRestAnnotation().disableContentParam());
	}

	@Test void c12_maxInput() {
		assertEquals("1M", b().maxInput("1M").toRestAnnotation().maxInput());
	}

	@Test
	@SuppressWarnings({
		"unchecked" // Class<? extends RestOpArg>[] varargs; generic array creation is safe here.
	})
	void c13_restOpArgs() {
		var b = b().restOpArgs();
		assertEquals(0, b.toRestAnnotation().restOpArgs().length);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Security
	//------------------------------------------------------------------------------------------------------------------

	@Test
	@SuppressWarnings({
		"unchecked" // Class<? extends Guard>[] varargs; generic array creation is safe here.
	})
	void d01_guards() {
		var b = b().guards();
		assertEquals(0, b.toRestAnnotation().guards().length);
	}

	@Test void d02_roleGuard() {
		assertEquals("ADMIN", b().roleGuard("ADMIN").toRestAnnotation().roleGuard());
	}

	@Test void d03_rolesDeclared() {
		assertEquals("ADMIN,USER", b().rolesDeclared("ADMIN,USER").toRestAnnotation().rolesDeclared());
	}

	@Test
	@SuppressWarnings({
		"unchecked" // Class<? extends RestConverter>[] varargs; generic array creation is safe here.
	})
	void d04_converters() {
		var b = b().converters();
		assertEquals(0, b.toRestAnnotation().converters().length);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Lifecycle / perf
	//------------------------------------------------------------------------------------------------------------------

	@Test void e01_eagerInit() {
		assertEquals("true", b().eagerInit("true").toRestAnnotation().eagerInit());
	}

	@Test void e02_lazyChildren() {
		assertEquals("true", b().lazyChildren("true").toRestAnnotation().lazyChildren());
	}

	@Test void e03_virtualThreads() {
		assertEquals("true", b().virtualThreads("true").toRestAnnotation().virtualThreads());
	}

	@Test void e04_asyncTimeoutMillis() {
		assertEquals("5000", b().asyncTimeoutMillis("5000").toRestAnnotation().asyncTimeoutMillis());
	}

	@Test void e05_asyncCompletionExecutor() {
		assertEquals("execBean", b().asyncCompletionExecutor("execBean").toRestAnnotation().asyncCompletionExecutor());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Observability / logging / errors
	//------------------------------------------------------------------------------------------------------------------

	@Test void f01_callLogger() {
		// Using class assignment is verified by direct annotation read.
		var b = b().callLogger(org.apache.juneau.rest.logger.BasicCallLogger.class);
		assertEquals(org.apache.juneau.rest.logger.BasicCallLogger.class, b.toRestAnnotation().callLogger());
	}

	@Test void f02_debug() {
		assertEquals("true", b().debug("true").toRestAnnotation().debug().value());
	}

	@Test void f03_observability() {
		assertEquals("true", b().observability("true").toRestAnnotation().observability());
	}

	@Test void f04_renderResponseStackTraces() {
		assertEquals("true", b().renderResponseStackTraces("true").toRestAnnotation().renderResponseStackTraces());
	}

	@Test void f05_problemDetails() {
		assertEquals("on", b().problemDetails("on").toRestAnnotation().problemDetails());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Docs / metadata / i18n / static files
	//------------------------------------------------------------------------------------------------------------------

	@Test void g01_title() {
		var b = b().title("Hello");
		assertArrayEquals(new String[]{"Hello"}, b.toRestAnnotation().title());
	}

	@Test void g02_description() {
		var b = b().description("Desc");
		assertArrayEquals(new String[]{"Desc"}, b.toRestAnnotation().description());
	}

	@Test void g03_siteName() {
		assertEquals("MySite", b().siteName("MySite").toRestAnnotation().siteName());
	}

	@Test void g04_swaggerProvider() {
		var b = b().swaggerProvider(org.apache.juneau.rest.swagger.BasicSwaggerProvider.class);
		assertEquals(org.apache.juneau.rest.swagger.BasicSwaggerProvider.class, b.toRestAnnotation().swaggerProvider());
	}

	@Test void g05_openApiProvider() {
		var b = b().openApiProvider(org.apache.juneau.rest.openapi.BasicOpenApiProvider.class);
		assertEquals(org.apache.juneau.rest.openapi.BasicOpenApiProvider.class, b.toRestAnnotation().openApiProvider());
	}

	@Test void g06_messages() {
		assertEquals("nls/Bundle", b().messages("nls/Bundle").toRestAnnotation().messages());
	}

	@Test void g07_config() {
		assertEquals("config.cfg", b().config("config.cfg").toRestAnnotation().config());
	}

	@Test void g08_staticFiles() {
		var b = b().staticFiles(org.apache.juneau.rest.staticfile.BasicStaticFiles.class);
		assertEquals(org.apache.juneau.rest.staticfile.BasicStaticFiles.class, b.toRestAnnotation().staticFiles());
	}

	@Test void g09_noInherit() {
		var b = b().noInherit("path");
		assertArrayEquals(new String[]{"path"}, b.toRestAnnotation().noInherit());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Programmatic-only knob & escape hatch
	//------------------------------------------------------------------------------------------------------------------

	@Test void h01_mdcAsyncPropagation_setTrue() {
		var b = b().mdcAsyncPropagation(true);
		assertEquals(Boolean.TRUE, b.getMdcAsyncPropagation());
	}

	@Test void h02_mdcAsyncPropagation_setFalse() {
		var b = b().mdcAsyncPropagation(false);
		assertEquals(Boolean.FALSE, b.getMdcAsyncPropagation());
	}

	@Test void h03_mdcAsyncPropagation_default_isNull() {
		assertNull(b().getMdcAsyncPropagation());
	}

	@Test void h04_set() {
		var b = b().set("k1", "v1").set("k2", 42);
		assertEquals("v1", b.getExtras().get("k1"));
		assertEquals(42, b.getExtras().get("k2"));
	}

	@Test void h05_set_throwsOnNullKey() {
		var b = b();
		assertThrows(IllegalArgumentException.class, () -> b.set(null, "x"));
	}

	@Test void h06_extras_unmodifiable() {
		var b = b().set("k", "v");
		var x = b.getExtras();
		assertThrows(UnsupportedOperationException.class, () -> x.put("k2", "v2"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Resource-type and self() identity
	//------------------------------------------------------------------------------------------------------------------

	@Test void i01_getResourceType() {
		assertEquals(R.class, b().getResourceType());
	}

	@Test void i02_self_returnsBuilder() {
		// All setters should chain via self(); confirm chaining works across many setters.
		var b = b()
			.path("/p")
			.title("T")
			.description("D")
			.allowedHeaderParams("h")
			.consumes("application/json")
			.produces("application/json");
		assertNotNull(b);
		var anno = b.toRestAnnotation();
		assertEquals("/p", anno.path());
		assertArrayEquals(new String[]{"T"}, anno.title());
	}

	@Test void i03_constructor_nullResourceType_throws() {
		assertThrows(IllegalArgumentException.class, () -> RestResource.builder(null));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Build: createResource paths (no-arg ctor / RestBuilder<?> ctor)
	//------------------------------------------------------------------------------------------------------------------

	public static class NoArgCtor extends RestResource {
		public NoArgCtor() {}
	}

	@Test void j01_build_noArgCtor() {
		var r = RestResource.builder(NoArgCtor.class).build();
		assertNotNull(r);
	}

	public static class BuilderCtor extends RestResource {
		public BuilderCtor(RestBuilder<?> b) {
			super(b);
		}
	}

	@Test void j02_build_builderCtor() {
		var b = RestResource.builder(BuilderCtor.class).path("/p");
		var r = b.build();
		assertSame(b, r.getRestBuilder());
	}

	public static class NoCtorResource extends RestResource {
		// Has only a constructor that takes an int — no no-arg, no RestBuilder<?> ctor.
		// (The implicit super-class default ctor isn't available because we declared a non-default ctor here.)
		public NoCtorResource(int x) { /* unused */ }
	}

	@Test void j03_build_noUsableCtor_throws() {
		// Builder.build() invokes createResource(), which must throw IllegalStateException for a class with no
		// no-arg or RestBuilder<?> ctor.
		assertThrows(IllegalStateException.class, () -> RestResource.builder(NoCtorResource.class).build());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Resource type cast for compile-time covariance check
	//------------------------------------------------------------------------------------------------------------------

	@Test void k01_chain_returnsConcreteBuilderType() {
		// Verify that AbstractRestBuilder fluent setters return SELF (DefaultBuilder<R> here).
		RestResource.DefaultBuilder<R> b = b().path("/x").siteName("y").title("z");
		assertNotNull(b);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Sanity: extras keep insertion order (LinkedHashMap)
	//------------------------------------------------------------------------------------------------------------------

	@Test void l01_extras_preserveInsertionOrder() {
		var b = b().set("a", 1).set("b", 2).set("c", 3);
		var keys = new ArrayList<>(b.getExtras().keySet());
		assertEquals(List.of("a", "b", "c"), keys);
	}
}
