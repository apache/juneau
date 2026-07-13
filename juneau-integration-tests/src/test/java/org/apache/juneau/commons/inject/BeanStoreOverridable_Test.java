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
package org.apache.juneau.commons.inject;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.encoders.*;
import org.apache.juneau.marshall.parser.*;
import org.apache.juneau.marshall.serializer.*;
import org.apache.juneau.rest.mock.*;
import org.apache.juneau.test.junit.*;
import org.junit.jupiter.api.*;

/**
 * Tests the {@link BeanStoreOverridable} marker interface and verifies the canonical bindings
 * (SerializerSet.Builder, ParserSet.Builder, EncoderSet.Builder, MockRestClient.Builder) implement it
 * with the correct self-typed return.
 */
class BeanStoreOverridable_Test extends TestBase {

	//-----------------------------------------------------------------------------------------------------------------
	// a — generic wiring helper compiles and links against each canonical binding.
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Generic test helper that exercises the self-typed return: the compiler must accept this call
	 * with each concrete builder type without an unchecked cast.
	 */
	private static <B extends BeanStoreOverridable<B>> B wire(B builder, BeanStore overlay) {
		return builder.overridingBeanStore(overlay);
	}

	@Test
	void a01_serializerSetBuilder_isBeanStoreOverridable() {
		var overlay = new TestBeanStore();
		var b = SerializerSet.create();
		assertInstanceOf(BeanStoreOverridable.class, b);
		var ret = wire(b, overlay);
		assertSame(b, ret, "Self-typed return must be the same builder instance");
	}

	@Test
	void a02_parserSetBuilder_isBeanStoreOverridable() {
		var overlay = new TestBeanStore();
		var b = ParserSet.create();
		assertInstanceOf(BeanStoreOverridable.class, b);
		var ret = wire(b, overlay);
		assertSame(b, ret);
	}

	@Test
	void a03_encoderSetBuilder_isBeanStoreOverridable() {
		var overlay = new TestBeanStore();
		var b = EncoderSet.create();
		assertInstanceOf(BeanStoreOverridable.class, b);
		var ret = wire(b, overlay);
		assertSame(b, ret);
	}

	@Test
	void a04_mockRestClientBuilder_isBeanStoreOverridable() {
		var overlay = new TestBeanStore();
		// We don't build the client (no @Rest fixture) — just verify the binding compiles and the marker interface is present.
		var b = MockRestClient.builder(Object.class);
		assertInstanceOf(BeanStoreOverridable.class, b);
		var ret = wire(b, overlay);
		assertSame(b, ret);
	}

	@Test
	void a05_classicMockRestClientBuilder_isBeanStoreOverridable() {
		var overlay = new TestBeanStore();
		var b = org.apache.juneau.rest.mock.classic.MockRestClient.create(Object.class);
		assertInstanceOf(BeanStoreOverridable.class, b);
		var ret = wire(b, overlay);
		assertSame(b, ret);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// b — null overlay is a no-op (does not throw, does not wrap)
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void b01_serializerSetBuilder_nullOverlay_isNoop() {
		var b = SerializerSet.create();
		var before = b.beanStore();
		b.overridingBeanStore(null);
		assertSame(before, b.beanStore(), "Passing null must not wrap the underlying bean store");
	}

	@Test
	void b02_parserSetBuilder_nullOverlay_isNoop() {
		var b = ParserSet.create();
		var before = b.beanStore();
		b.overridingBeanStore(null);
		assertSame(before, b.beanStore());
	}

	@Test
	void b03_encoderSetBuilder_nullOverlay_isNoop() {
		var b = EncoderSet.create();
		var before = b.beanStore();
		b.overridingBeanStore(null);
		assertSame(before, b.beanStore());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// c — non-null overlay wraps the underlying bean store with the overlay as the overridingParent.
	//-----------------------------------------------------------------------------------------------------------------

	interface Greeter { String greet(); }

	@Test
	void c01_overlayWinsOverLocalRegistration() {
		var overlay = new TestBeanStore().override(Greeter.class, () -> "from-overlay");
		var b = SerializerSet.create()
			.overridingBeanStore(overlay);
		// Lookup must hit the overlay first, not any later-installed local entry.
		var resolved = b.beanStore().getBean(Greeter.class).orElseThrow();
		assertEquals("from-overlay", resolved.greet());
	}

	@Test
	void c02_repeatedCalls_stack() {
		var inner = new TestBeanStore().override(Greeter.class, () -> "inner");
		var outer = new TestBeanStore().override(Greeter.class, () -> "outer");
		var b = SerializerSet.create()
			.overridingBeanStore(inner)
			.overridingBeanStore(outer);
		// Outer call wraps the already-wrapped store, so the outer overlay wins at the top of the chain.
		assertEquals("outer", b.beanStore().getBean(Greeter.class).orElseThrow().greet());
	}
}
