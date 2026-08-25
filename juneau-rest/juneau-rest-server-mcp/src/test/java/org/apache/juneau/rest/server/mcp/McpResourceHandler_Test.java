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
package org.apache.juneau.rest.server.mcp;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;

import org.apache.juneau.commons.inject.*;
import org.junit.jupiter.api.*;

/**
 * Coverage for {@link McpResourceHandler#of(McpResourceSpec, java.util.function.BiFunction)} and the
 * two-abstract-method contract that replaced the misleading {@code @FunctionalInterface} annotation.
 */
@SuppressWarnings({
	"resource" // Closeable resources in tests are intentionally unassigned; closing is handled by test infrastructure.
})
class McpResourceHandler_Test {

	@Test void a01_interfaceIsNoLongerAnnotatedFunctional() {
		assertNull(McpResourceHandler.class.getAnnotation(FunctionalInterface.class));
	}

	@Test void a02_descriptorIsAbstractSoBareLambdasNoLongerCompile() {
		var abstractMethods = 0;
		for (var m : McpResourceHandler.class.getDeclaredMethods())
			if (Modifier.isAbstract(m.getModifiers()))
				abstractMethods++;
		assertEquals(2, abstractMethods, "descriptor() and read() must both be abstract - a two-method contract can never be satisfied by a bare lambda.");
	}

	@Test void b01_ofWiresDescriptorAndDelegatesRead() {
		var spec = new McpResourceSpec().setUri("file:///a").setName("a");
		var seenCtx = new AtomicReference<BeanStore>();
		var a = McpResourceHandler.of(spec, (uri, ctx) -> { seenCtx.set(ctx); return new McpResourceOutcome().setContents(List.of(McpResourceContents.text(uri, "text/plain", "ok"))); });
		assertSame(spec, a.descriptor());
		var ctx = new BasicBeanStore();
		var b = a.read("file:///a", ctx);
		assertEquals("ok", b.getContents().get(0).text());
		assertSame(ctx, seenCtx.get());
	}

	@Test void b02_ofRegistersInOneExpressionViaAddResource() {
		var config = new McpServerConfig().addResource(McpResourceHandler.of(
			new McpResourceSpec().setUri("file:///a"),
			(uri, ctx) -> new McpResourceOutcome()));
		assertEquals(1, config.getResources().size());
		assertEquals("file:///a", config.getResources().get(0).descriptor().getUri());
	}

	@Test void c01_ofRejectsNullDescriptor() {
		BiFunction<String,BeanStore,McpResourceOutcome> read = (uri, ctx) -> new McpResourceOutcome();
		assertThrows(IllegalArgumentException.class, () -> McpResourceHandler.of(null, read));
	}

	@Test void c02_ofRejectsNullRead() {
		var spec = new McpResourceSpec().setUri("file:///a");
		assertThrows(IllegalArgumentException.class, () -> McpResourceHandler.of(spec, null));
	}

	@Test void c03_ofRejectsBlankUri() {
		BiFunction<String,BeanStore,McpResourceOutcome> read = (uri, ctx) -> new McpResourceOutcome();
		var spec1 = new McpResourceSpec();
		assertThrows(IllegalArgumentException.class, () -> McpResourceHandler.of(spec1, read));
		var spec2 = new McpResourceSpec().setUri(" ");
		assertThrows(IllegalArgumentException.class, () -> McpResourceHandler.of(spec2, read));
	}
}
