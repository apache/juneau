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
package org.apache.juneau.rest.server.view.jsp;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Unit tests for {@link JspDispatcher} and its {@link JspDispatcher.Builder}.
 *
 * <p>
 * Covers the builder's default/explicit base-path plumbing, plus the defensive
 * {@code basePath == null} guard in {@link JspDispatcher.Builder#build() build()} -- unreachable through
 * the public {@link JspDispatcher.Builder#basePath(String) basePath(...)} setter (which normalizes
 * {@code null}/blank to the default), so this test reaches it directly via the package-private
 * {@code basePath} field, exercising the guard as a defense against a future subclass or same-package
 * caller that mutates the field without going through the setter.
 *
 * @since 10.0.0
 */
class JspDispatcher_Test extends TestBase {

	@Test void a01_defaultBasePath() {
		var d = JspDispatcher.create().build();
		assertEquals(JspDispatcher.DEFAULT_BASE_PATH, d.getBasePath());
	}

	@Test void a02_explicitBasePath() {
		var d = JspDispatcher.create().basePath("/WEB-INF/views").build();
		assertEquals("/WEB-INF/views", d.getBasePath());
	}

	@Test void a03_blankBasePathResetsToDefault() {
		var d = JspDispatcher.create().basePath("   ").build();
		assertEquals(JspDispatcher.DEFAULT_BASE_PATH, d.getBasePath());
	}

	@Test void a04_builderBasePathFieldNullDirectly_buildThrows() {
		var b = JspDispatcher.create();
		b.basePath = null; // Bypasses the basePath(...) setter's null-normalization (package-private field).
		assertThrows(IllegalArgumentException.class, b::build);
	}
}
