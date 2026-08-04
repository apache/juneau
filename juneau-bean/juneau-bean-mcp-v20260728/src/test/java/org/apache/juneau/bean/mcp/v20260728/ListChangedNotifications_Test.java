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
package org.apache.juneau.bean.mcp.v20260728;

import static org.junit.jupiter.api.Assertions.*;

import java.util.stream.*;

import org.apache.juneau.marshall.json.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

class ListChangedNotifications_Test {

	static Stream<Class<?>> types() {
		return Stream.of(ResourcesListChangedNotification.class, ToolsListChangedNotification.class,
			PromptsListChangedNotification.class);
	}

	@ParameterizedTest
	@MethodSource("types")
	void a01_extendsRequestParams(Class<?> type) {
		assertTrue(RequestParams.class.isAssignableFrom(type), () -> type.getName() + " must extend RequestParams");
	}

	@ParameterizedTest
	@MethodSource("types")
	void a02_declaresNoMembersBeyondInheritedMeta(Class<?> type) {
		assertEquals(0, type.getDeclaredFields().length, () -> type.getName() + " must declare no own fields");
	}

	@Test void a03_metaRoundTrip_resourcesListChanged() throws Exception {
		roundTripMeta(new ResourcesListChangedNotification());
	}

	@Test void a04_metaRoundTrip_toolsListChanged() throws Exception {
		roundTripMeta(new ToolsListChangedNotification());
	}

	@Test void a05_metaRoundTrip_promptsListChanged() throws Exception {
		roundTripMeta(new PromptsListChangedNotification());
	}

	private static void roundTripMeta(Object bean) throws Exception {
		bean.getClass().getMethod("setMeta", RequestMeta.class)
			.invoke(bean, new RequestMeta().setProtocolVersion("2026-07-28"));
		var json = JsonSerializer.DEFAULT.write(bean);
		assertTrue(json.contains("\"_meta\":{"), () -> bean.getClass().getSimpleName() + " must nest _meta: " + json);
		var copy = JsonParser.DEFAULT.read(json, bean.getClass());
		assertEquals(json, JsonSerializer.DEFAULT.write(copy));
	}
}
