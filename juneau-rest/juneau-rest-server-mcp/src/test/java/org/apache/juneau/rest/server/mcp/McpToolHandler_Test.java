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

import org.apache.juneau.commons.inject.*;
import org.junit.jupiter.api.*;

/**
 * Coverage for {@link McpToolHandler#of(McpToolSpec, java.util.function.BiFunction)} and the
 * two-abstract-method contract that replaced the misleading {@code @FunctionalInterface} annotation.
 */
class McpToolHandler_Test {

	@Test void a01_interfaceIsNoLongerAnnotatedFunctional() {
		assertNull(McpToolHandler.class.getAnnotation(FunctionalInterface.class));
	}

	@Test void a02_descriptorIsAbstractSoBareLambdasNoLongerCompile() {
		var abstractMethods = 0;
		for (var m : McpToolHandler.class.getDeclaredMethods())
			if (Modifier.isAbstract(m.getModifiers()))
				abstractMethods++;
		assertEquals(2, abstractMethods, "descriptor() and call() must both be abstract - a two-method contract can never be satisfied by a bare lambda.");
	}

	@Test void b01_ofWiresDescriptorAndDelegatesCall() {
		var spec = new McpToolSpec().setName("echo").setDescription("Echoes arguments back.");
		var a = McpToolHandler.of(spec, (arguments, ctx) -> McpToolOutcome.text(String.valueOf(arguments.get("text"))));
		assertSame(spec, a.descriptor());
		var b = a.call(Map.of("text", "hi"), new BasicBeanStore());
		assertEquals("hi", b.getContent().get(0).text());
	}

	@Test void b02_ofRegistersInOneExpressionViaAddTool() {
		var config = new McpServerConfig().addTool(McpToolHandler.of(
			new McpToolSpec().setName("echo"),
			(arguments, ctx) -> McpToolOutcome.text("ok")));
		assertEquals(1, config.getTools().size());
		assertEquals("echo", config.getTools().get(0).descriptor().getName());
	}

	@Test void c01_ofRejectsNullDescriptor() {
		assertThrows(IllegalArgumentException.class, () -> McpToolHandler.of(null, (arguments, ctx) -> McpToolOutcome.text("x")));
	}

	@Test void c02_ofRejectsNullCall() {
		assertThrows(IllegalArgumentException.class, () -> McpToolHandler.of(new McpToolSpec().setName("x"), null));
	}

	@Test void c03_ofRejectsBlankName() {
		assertThrows(IllegalArgumentException.class, () -> McpToolHandler.of(new McpToolSpec(), (arguments, ctx) -> McpToolOutcome.text("x")));
		assertThrows(IllegalArgumentException.class, () -> McpToolHandler.of(new McpToolSpec().setName(" "), (arguments, ctx) -> McpToolOutcome.text("x")));
	}
}
