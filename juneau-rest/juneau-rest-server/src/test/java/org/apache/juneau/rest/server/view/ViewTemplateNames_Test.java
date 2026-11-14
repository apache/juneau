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
package org.apache.juneau.rest.server.view;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Unit tests for {@link ViewTemplateNames}.
 *
 * @since 10.0.0
 */
class ViewTemplateNames_Test extends TestBase {

	@Test void a01_isPlainName_acceptsOrdinaryNames() {
		assertTrue(ViewTemplateNames.isPlainName(null));
		assertTrue(ViewTemplateNames.isPlainName("about.ftlh"));
		assertTrue(ViewTemplateNames.isPlainName("nested/inner.mustache"));
	}

	@Test void a02_isPlainName_rejectsSchemesAndSeparators() {
		assertFalse(ViewTemplateNames.isPlainName("file:/etc/hosts"));
		assertFalse(ViewTemplateNames.isPlainName("http://host/x"));
		assertFalse(ViewTemplateNames.isPlainName("a\\b.ftl"));
		assertFalse(ViewTemplateNames.isPlainName("a\u0000b.ftl"));
		assertFalse(ViewTemplateNames.isPlainName("a\nb.ftl"));
		assertFalse(ViewTemplateNames.isPlainName("a\u007fb.ftl"));
	}

	@Test void b01_hasAllowedExtension() {
		assertTrue(ViewTemplateNames.hasAllowedExtension("a.ftlh", ".ftlh", ".ftl"));
		assertTrue(ViewTemplateNames.hasAllowedExtension("A.FTL", ".ftlh", ".ftl"));
		assertFalse(ViewTemplateNames.hasAllowedExtension("a.properties", ".ftlh", ".ftl"));
		assertFalse(ViewTemplateNames.hasAllowedExtension("a", ".ftl"));
	}

	@Test void c01_requirePlainName() {
		assertDoesNotThrow(() -> ViewTemplateNames.requirePlainName("about.ftlh"));
		assertDoesNotThrow(() -> ViewTemplateNames.requirePlainName(null));
		assertThrows(IllegalArgumentException.class, () -> ViewTemplateNames.requirePlainName("file:x"));
	}
}
