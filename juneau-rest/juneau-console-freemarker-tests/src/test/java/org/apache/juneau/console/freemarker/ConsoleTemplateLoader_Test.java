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
package org.apache.juneau.console.freemarker;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.console.freemarker.ConsoleFreemarkerMixin.*;
import org.junit.jupiter.api.*;

/**
 * Tests that the console template loader only resolves names inside the console template package.
 *
 * @since 10.0.0
 */
class ConsoleTemplateLoader_Test extends TestBase {

	private final ConsoleTemplateLoader loader = new ConsoleTemplateLoader(getClass().getClassLoader());

	@Test void a01_consoleTemplatesResolve() throws Exception {
		var src = loader.findTemplateSource(ConsoleFreemarkerMixin.BASE_TEMPLATE_PATH);
		assertNotNull(src);
		loader.closeTemplateSource(src);
	}

	@Test void a02_otherClasspathNamesDoNotResolve() throws Exception {
		assertNull(loader.findTemplateSource(""));
		assertNull(loader.findTemplateSource("META-INF/MANIFEST.MF"));
		assertNull(loader.findTemplateSource("org/apache/juneau/Version.class"));
		assertNull(loader.findTemplateSource("java/lang/Object.class"));
	}

	@Test void a03_parentSegmentsAreRejected() throws Exception {
		assertNull(loader.findTemplateSource("org/apache/juneau/console/../../../../META-INF/MANIFEST.MF"));
	}
}
