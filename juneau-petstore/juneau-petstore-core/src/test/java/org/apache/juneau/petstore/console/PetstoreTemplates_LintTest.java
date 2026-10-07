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
package org.apache.juneau.petstore.console;

import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.server.view.freemarker.console.*;
import org.junit.jupiter.api.*;

/** Every console template validates with zero findings. */
class PetstoreTemplates_LintTest extends TestBase {

	private static Path moduleRoot() {
		for (var p : List.of(Path.of(""), Path.of("juneau-petstore/juneau-petstore-core")))
			if (Files.isDirectory(p.resolve("src/main/resources/org/apache/juneau/petstore/console/templates")))
				return p;
		var basedir = System.getProperty("basedir");
		assertNotNull(basedir, "could not locate juneau-petstore-core; the lint gate would scan nothing and 'pass'");
		return Path.of(basedir);
	}

	@Test void a01_noFindings() {
		var root = moduleRoot().resolve("src/main/resources/org/apache/juneau/petstore/console/templates");
		var findings = ConsoleTemplateValidator.create()
			.templateRoot(root)
			.classpathRoot(PetstoreConsolePage.TEMPLATES)
			.chromeTemplate("base.ftlh")
			.validateAll();
		assertEmpty(findings);
	}

	@Test void a02_scansTheRealTree() {
		// A misconfigured root would scan nothing and pass.  A bad fixture (templateRoot) must be found, with the real
		// base.ftlh resolved from the classpath root.
		var root = moduleRoot().resolve("src/test/resources/org/apache/juneau/petstore/console/failloud");
		var findings = ConsoleTemplateValidator.create()
			.templateRoot(root)
			.classpathRoot(PetstoreConsolePage.TEMPLATES)
			.chromeTemplate("base.ftlh")
			.validateAll();
		assertNotEmpty(findings);
	}
}
