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
import java.util.*;

import org.junit.jupiter.api.*;

/**
 * Enforces the standing Juneau convention that every new public type carries a class-level
 * {@code <h5 class='section'>Example:</h5>} Javadoc block and {@code @since 10.0.0}, for exactly the top-level
 * public types the C3 page-builder work created. Reads source text, not compiled Javadoc (doc comments do not
 * survive into {@code .class} files); a renamed or moved file fails loudly so the audit cannot silently go stale.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jsm>assertHasExampleBlock</jsm>(<jv>juneauRoot</jv>.resolve(<js>"juneau-rest/.../TableSpec.java"</js>));
 * </p>
 *
 * @since 10.0.0
 */
class PublicTypes_Javadoc_Test {

	/** @return The juneau checkout root: the nearest ancestor of the working directory holding {@code juneau-bom}. */
	private static Path juneauRoot() {
		var cwd = Path.of("").toAbsolutePath();
		var p = cwd;
		while (p != null && ! Files.exists(p.resolve("juneau-bom")))
			p = p.getParent();
		if (p == null)
			throw new IllegalStateException("Could not locate the juneau checkout root above '" + cwd + "'.");
		return p;
	}

	private static final String VIEWS = "juneau-rest/juneau-rest-server-views/src/main/java/org/apache/juneau/rest/server/views/";
	private static final String CUIF = "juneau-rest/juneau-rest-server-console-ui-freemarker/src/main/java/org/apache/juneau/rest/server/view/freemarker/console/";

	private static final List<String> NEW_PUBLIC_TYPE_FILES = List.of(
		VIEWS + "RibbonItem.java", VIEWS + "RowDetail.java", VIEWS + "QuickStat.java", VIEWS + "TableSpec.java",
		CUIF + "PageSpec.java", CUIF + "BadgeDirectiveModel.java", CUIF + "FactsDirectiveModel.java");

	private static void assertHasExampleBlock(Path file) throws IOException {
		assertTrue(Files.exists(file), () -> "Missing file, update NEW_PUBLIC_TYPE_FILES: '" + file + "'.");
		var text = Files.readString(file);
		var typeIdx = text.indexOf("public final class");
		if (typeIdx < 0)
			typeIdx = text.indexOf("public class");
		assertTrue(typeIdx >= 0, () -> "No public type declaration found in '" + file + "'.");
		var javadoc = text.substring(0, typeIdx);
		assertTrue(javadoc.contains("<h5 class='section'>Example:</h5>"),
			() -> "'" + file + "' is missing the class-level <h5 class='section'>Example:</h5> Javadoc block.");
		assertTrue(javadoc.contains("@since 10.0.0"),
			() -> "'" + file + "' is missing @since 10.0.0 on its class-level Javadoc.");
	}

	@Test void everyNewPublicType_hasExampleBlock() throws Exception {
		var root = juneauRoot();
		for (var rel : NEW_PUBLIC_TYPE_FILES)
			assertHasExampleBlock(root.resolve(rel));
	}
}
