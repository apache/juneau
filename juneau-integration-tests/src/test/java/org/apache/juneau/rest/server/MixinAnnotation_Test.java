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
package org.apache.juneau.rest.server;

import static org.apache.juneau.TestUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.encoders.*;
import org.apache.juneau.marshall.json.*;
import org.apache.juneau.marshall.uon.*;
import org.apache.juneau.rest.server.auth.*;
import org.apache.juneau.rest.server.converter.*;
import org.apache.juneau.rest.server.logger.*;
import org.apache.juneau.rest.server.processor.*;
import org.junit.jupiter.api.*;

/**
 * Tests for the {@link MixinAnnotation} companion (programmatic builder + synthetic impl) and equivalency with
 * the declarative {@link Mixin @Mixin} annotation form.
 */
@SuppressWarnings({
	"unchecked" // Generic Class<? extends X>[] varargs in the builder slot setters are populated with single literals in tests.
})
class MixinAnnotation_Test extends TestBase {

	public static class FooMixin {}

	private static MixinAnnotation.Builder fullBuilder() {
		return MixinAnnotation.create()
			.type(FooMixin.class)
			.guards(BearerTokenGuard.class)
			.roleGuard("admin")
			.rolesDeclared("admin,user")
			.converters(Traversable.class)
			.encoders(GzipEncoder.class)
			.serializers(JsonSerializer.class)
			.parsers(JsonParser.class)
			.responseProcessors(InputStreamProcessor.class)
			.restOpArgs(AuthArg.class)
			.callLogger(BasicCallLogger.class)
			.partSerializer(UonSerializer.class)
			.partParser(UonParser.class)
			.debug(null)  // null coalesces to DebugAnnotation.DEFAULT
			.messages("Msgs")
			.defaultRequestHeaders("X-A: 1")
			.defaultResponseHeaders("X-B: 2")
			.defaultRequestAttributes("p1: v1")
			.produces("application/json")
			.consumes("application/json")
			.defaultAccept("application/json")
			.defaultContentType("application/json")
			.defaultCharset("utf-8")
			.maxInput("1M")
			.path("/admin")
			.paths("/a", "/b")
			.noInherit("guards");
	}

	Mixin a1 = fullBuilder().build();
	Mixin a2 = fullBuilder().build();

	@Test void a01_stringAccessors() {
		assertEquals(FooMixin.class, a1.type());
		assertEquals("admin", a1.roleGuard());
		assertEquals("admin,user", a1.rolesDeclared());
		assertEquals("Msgs", a1.messages());
		assertEquals("application/json", a1.defaultAccept());
		assertEquals("application/json", a1.defaultContentType());
		assertEquals("utf-8", a1.defaultCharset());
		assertEquals("1M", a1.maxInput());
		assertEquals("/admin", a1.path());
		assertArrayEquals(new String[]{"/a","/b"}, a1.paths());
		assertArrayEquals(new String[]{"guards"}, a1.noInherit());
		assertArrayEquals(new String[]{"X-A: 1"}, a1.defaultRequestHeaders());
		assertArrayEquals(new String[]{"X-B: 2"}, a1.defaultResponseHeaders());
		assertArrayEquals(new String[]{"p1: v1"}, a1.defaultRequestAttributes());
		assertArrayEquals(new String[]{"application/json"}, a1.produces());
		assertArrayEquals(new String[]{"application/json"}, a1.consumes());
	}

	@Test void a01b_classAndAnnotationAccessors() {
		assertEquals(BearerTokenGuard.class, a1.guards()[0]);
		assertEquals(Traversable.class, a1.converters()[0]);
		assertEquals(GzipEncoder.class, a1.encoders()[0]);
		assertEquals(JsonSerializer.class, a1.serializers()[0]);
		assertEquals(JsonParser.class, a1.parsers()[0]);
		assertEquals(InputStreamProcessor.class, a1.responseProcessors()[0]);
		assertEquals(AuthArg.class, a1.restOpArgs()[0]);
		assertEquals(BasicCallLogger.class, a1.callLogger());
		assertEquals(UonSerializer.class, a1.partSerializer());
		assertEquals(UonParser.class, a1.partParser());
		// debug(null) coalesced to the default @Debug.
		assertEquals(DebugAnnotation.DEFAULT, a1.debug());
	}

	@Test void a02_testEquivalency() {
		assertEquals(a2, a1);
		assertNotEqualsAny(a1.hashCode(), 0, -1);
		assertEquals(a1.hashCode(), a2.hashCode());
	}

	@Test void a03_defaultInstance() {
		var d = MixinAnnotation.DEFAULT;
		assertEquals("", d.roleGuard());
		assertEquals("", d.path());
		assertEquals(0, d.guards().length);
		assertEquals(0, d.noInherit().length);
	}

	@Test void a04_debugNonNullValueKept() {
		// The non-null branch of debug(Debug): a supplied @Debug is kept as-is (not coalesced to DEFAULT).
		var dbg = DebugAnnotation.create().value("always").build();
		var m = MixinAnnotation.create().type(FooMixin.class).debug(dbg).build();
		assertEquals(dbg, m.debug());
		assertEquals("always", m.debug().value());
	}

	// Comparison with the declarative @Mixin form.

	@Rest(mixinDefs=@Mixin(
		type=FooMixin.class,
		guards=BearerTokenGuard.class,
		roleGuard="admin",
		rolesDeclared="admin,user",
		converters=Traversable.class,
		encoders=GzipEncoder.class,
		serializers=JsonSerializer.class,
		parsers=JsonParser.class,
		responseProcessors=InputStreamProcessor.class,
		restOpArgs=AuthArg.class,
		callLogger=BasicCallLogger.class,
		partSerializer=UonSerializer.class,
		partParser=UonParser.class,
		messages="Msgs",
		defaultRequestHeaders="X-A: 1",
		defaultResponseHeaders="X-B: 2",
		defaultRequestAttributes="p1: v1",
		produces="application/json",
		consumes="application/json",
		defaultAccept="application/json",
		defaultContentType="application/json",
		defaultCharset="utf-8",
		maxInput="1M",
		path="/admin",
		paths={"/a","/b"},
		noInherit="guards"
	))
	public static class D1 {}

	@Test void d01_comparisonWithDeclarativeAnnotation() {
		// The builder-produced a1 must be equal+hashCode-equal to the declarative @Mixin form (same slots).
		var d1 = D1.class.getAnnotationsByType(Rest.class)[0].mixinDefs()[0];
		assertEquals(a1, d1);
		assertEquals(a1.hashCode(), d1.hashCode());
	}
}
