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
 * Coverage for {@link McpPromptHandler#of(McpPromptSpec, java.util.function.BiFunction)} and the
 * two-abstract-method contract that replaced the misleading {@code @FunctionalInterface} annotation.
 */
@SuppressWarnings({
	"resource" // Closeable resources in tests are intentionally unassigned; closing is handled by test infrastructure.
})
class McpPromptHandler_Test {

	@Test void a01_interfaceIsNoLongerAnnotatedFunctional() {
		assertNull(McpPromptHandler.class.getAnnotation(FunctionalInterface.class));
	}

	@Test void a02_descriptorIsAbstractSoBareLambdasNoLongerCompile() {
		var abstractMethods = 0;
		for (var m : McpPromptHandler.class.getDeclaredMethods())
			if (Modifier.isAbstract(m.getModifiers()))
				abstractMethods++;
		assertEquals(2, abstractMethods, "descriptor() and get() must both be abstract - a two-method contract can never be satisfied by a bare lambda.");
	}

	@Test void b01_ofWiresDescriptorAndDelegatesGet() {
		var spec = new McpPromptSpec().setName("greet").setDescription("Greets the caller.");
		var seenCtx = new AtomicReference<BeanStore>();
		var a = McpPromptHandler.of(spec, (arguments, ctx) -> { seenCtx.set(ctx); return new McpPromptOutcome().setDescription(String.valueOf(arguments.get("who"))); });
		assertSame(spec, a.descriptor());
		var ctx = new BasicBeanStore();
		var b = a.get(Map.of("who", "Alice"), ctx);
		assertEquals("Alice", b.getDescription());
		assertSame(ctx, seenCtx.get());
	}

	@Test void b02_ofRegistersInOneExpressionViaAddPrompt() {
		var config = new McpServerConfig().addPrompt(McpPromptHandler.of(
			new McpPromptSpec().setName("greet"),
			(arguments, ctx) -> new McpPromptOutcome()));
		assertEquals(1, config.getPrompts().size());
		assertEquals("greet", config.getPrompts().get(0).descriptor().getName());
	}

	@Test void c01_ofRejectsNullDescriptor() {
		BiFunction<Map<String,Object>,BeanStore,McpPromptOutcome> get = (arguments, ctx) -> new McpPromptOutcome();
		assertThrows(IllegalArgumentException.class, () -> McpPromptHandler.of(null, get));
	}

	@Test void c02_ofRejectsNullGet() {
		var spec = new McpPromptSpec().setName("x");
		assertThrows(IllegalArgumentException.class, () -> McpPromptHandler.of(spec, null));
	}

	@Test void c03_ofRejectsBlankName() {
		BiFunction<Map<String,Object>,BeanStore,McpPromptOutcome> get = (arguments, ctx) -> new McpPromptOutcome();
		assertThrows(IllegalArgumentException.class, () -> McpPromptHandler.of(new McpPromptSpec(), get));
		var spec = new McpPromptSpec().setName(" ");
		assertThrows(IllegalArgumentException.class, () -> McpPromptHandler.of(spec, get));
	}
}
