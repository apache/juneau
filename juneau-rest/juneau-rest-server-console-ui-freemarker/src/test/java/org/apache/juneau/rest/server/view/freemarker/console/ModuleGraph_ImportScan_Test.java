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
package org.apache.juneau.rest.server.view.freemarker.console;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.nio.file.*;
import java.util.stream.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Phase 5 module-graph re-check (now with real code): no class under {@code console-ui-freemarker}'s MAIN source
 * imports {@code org.apache.juneau.rest.server.datatables.*}. A chrome-only consumer of this module never drags
 * {@code -datatables} onto the classpath (the {@code console-ui-freemarker-datatables} module is the only one
 * allowed to import that package &mdash; see Phase 7's own module-graph re-check).
 */
class ModuleGraph_ImportScan_Test extends TestBase {

	@Test void a01_mainSource_neverImportsDatatablesPackage() throws IOException {
		var srcMain = new File(System.getProperty("user.dir"), "src/main/java").toPath();
		assertTrue(Files.isDirectory(srcMain), () -> "Expected src/main/java not found: " + srcMain);
		try (Stream<Path> files = Files.walk(srcMain)) {
			var offenders = files
				.filter(p -> p.toString().endsWith(".java"))
				.filter(ModuleGraph_ImportScan_Test::importsDatatables)
				.map(Path::toString)
				.collect(Collectors.toList());
			assertTrue(offenders.isEmpty(), () -> "Found forbidden org.apache.juneau.rest.server.datatables.* import(s) in: " + offenders);
		}
	}

	private static boolean importsDatatables(Path javaFile) {
		try {
			return Files.readString(javaFile).contains("org.apache.juneau.rest.server.datatables");
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
