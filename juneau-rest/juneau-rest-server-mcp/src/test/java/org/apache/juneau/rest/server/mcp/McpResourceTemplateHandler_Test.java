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

import java.util.*;
import java.util.concurrent.atomic.*;

import org.apache.juneau.commons.inject.*;
import org.junit.jupiter.api.*;

/**
 * Coverage for {@link McpResourceTemplateHandler#of(McpResourceTemplateSpec, McpResourceTemplateHandler.ReadFunction)},
 * the only two-method handler interface that lacked a factory (it never had the {@code @FunctionalInterface}
 * footgun in the first place, since {@link McpResourceTemplateHandler#read(String, Map, BeanStore)} already
 * took three parameters, which no bare lambda satisfying a single-abstract-method interface could implement).
 */
@SuppressWarnings({
	"resource" // Closeable resources in tests are intentionally unassigned; closing is handled by test infrastructure.
})
class McpResourceTemplateHandler_Test {

	@Test void b01_ofWiresDescriptorAndDelegatesRead() {
		var spec = new McpResourceTemplateSpec().setUriTemplate("file:///{name}").setName("t");
		var seenCtx = new AtomicReference<BeanStore>();
		var a = McpResourceTemplateHandler.of(spec, (uri, variables, ctx) -> {
			seenCtx.set(ctx);
			return new McpResourceOutcome().setContents(List.of(McpResourceContents.text(uri, "text/plain", variables.get("name"))));
		});
		assertSame(spec, a.descriptor());
		var ctx = new BasicBeanStore();
		var b = a.read("file:///a", Map.of("name", "a"), ctx);
		assertEquals("a", b.getContents().get(0).text());
		assertSame(ctx, seenCtx.get());
	}

	@Test void b02_ofRegistersInOneExpressionViaAddResourceTemplate() {
		var config = new McpServerConfig().addResourceTemplate(McpResourceTemplateHandler.of(
			new McpResourceTemplateSpec().setUriTemplate("file:///{name}"),
			(uri, variables, ctx) -> new McpResourceOutcome()));
		assertEquals(1, config.getResourceTemplates().size());
		assertEquals("file:///{name}", config.getResourceTemplates().get(0).descriptor().getUriTemplate());
	}

	@Test void c01_ofRejectsNullDescriptor() {
		McpResourceTemplateHandler.ReadFunction read = (uri, variables, ctx) -> new McpResourceOutcome();
		assertThrows(IllegalArgumentException.class, () -> McpResourceTemplateHandler.of(null, read));
	}

	@Test void c02_ofRejectsNullRead() {
		var spec = new McpResourceTemplateSpec().setUriTemplate("file:///{name}");
		assertThrows(IllegalArgumentException.class, () -> McpResourceTemplateHandler.of(spec, null));
	}

	@Test void c03_ofRejectsBlankUriTemplate() {
		McpResourceTemplateHandler.ReadFunction read = (uri, variables, ctx) -> new McpResourceOutcome();
		assertThrows(IllegalArgumentException.class, () -> McpResourceTemplateHandler.of(new McpResourceTemplateSpec(), read));
		var spec = new McpResourceTemplateSpec().setUriTemplate(" ");
		assertThrows(IllegalArgumentException.class, () -> McpResourceTemplateHandler.of(spec, read));
	}
}
