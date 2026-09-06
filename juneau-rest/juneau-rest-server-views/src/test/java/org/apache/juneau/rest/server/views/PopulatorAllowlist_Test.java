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
package org.apache.juneau.rest.server.views;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Java name-allowlist for region populators, plus a source-level guard on the class Javadoc's honest-limit warning.
 */
class PopulatorAllowlist_Test extends TestBase {

	@Test void a01_builtinAccepted() {
		PopulatorAllowlist.assertAllowed("default", null);
	}

	@Test void a02_optedInCustomAccepted() {
		PopulatorAllowlist.assertAllowed("myWidget", Set.of("myWidget"));
	}

	@Test void a03_unknownRejected() {
		var e = assertThrows(IllegalArgumentException.class,
			() -> PopulatorAllowlist.assertAllowed("evil", null));
		assertTrue(e.getMessage().contains("evil"), e::getMessage);
	}

	@Test void a04_customRejectedWithoutOptIn() {
		assertThrows(IllegalArgumentException.class,
			() -> PopulatorAllowlist.assertAllowed("myWidget", null));
	}

	@Test void a05_nullAndBlankRejected() {
		assertThrows(IllegalArgumentException.class, () -> PopulatorAllowlist.assertAllowed(null, null));
		assertThrows(IllegalArgumentException.class, () -> PopulatorAllowlist.assertAllowed("", null));
		assertThrows(IllegalArgumentException.class, () -> PopulatorAllowlist.assertAllowed("  ", null));
	}

	@Test void a06_nullAllowedNamesTolerated() {
		PopulatorAllowlist.assertAllowed("default", null);
	}

	@Test void a07_builtinIds_isExactlyDefault() {
		assertEquals(Set.of("default"), PopulatorAllowlist.BUILTIN_IDS);
	}

	/** Locates {@code PopulatorAllowlist.java}'s own source on disk, the same way {@code locateHarness()} does in
	 * {@link ViewsJs_ContractVersion_Test}. */
	private static Path locateSource() {
		var basedir = System.getProperty("basedir");
		if (basedir != null) {
			var p = Path.of(basedir, "src/main/java/org/apache/juneau/rest/server/views/PopulatorAllowlist.java");
			if (Files.isRegularFile(p)) return p.toAbsolutePath().normalize();
		}
		for (var rel : List.of(
				"src/main/java/org/apache/juneau/rest/server/views/PopulatorAllowlist.java",
				"juneau-rest/juneau-rest-server-views/src/main/java/org/apache/juneau/rest/server/views/PopulatorAllowlist.java")) {
			var p = Path.of(rel);
			if (Files.isRegularFile(p)) return p.toAbsolutePath().normalize();
		}
		return null;
	}

	/**
	 * Guards the honest-limit warning in {@link PopulatorAllowlist}'s class Javadoc against silent deletion by a
	 * future editor.  Unlike {@link SinkRenderAllowlist}, a populator allowlist controls only which name is
	 * resolved and nothing about what the resolved populator then does &mdash; a fact that is easy to lose once the
	 * class exists and looks, on its shape alone, exactly like a real security boundary.  This test reads the
	 * class's own source file off disk and fails if either load-bearing phrase is missing, so deleting the warning
	 * (rather than merely editing code around it) breaks the build.
	 */
	@Test void a08_classJavadoc_keepsTheHonestLimitWarning() throws IOException {
		var src = locateSource();
		assertNotNull(src, "could not locate PopulatorAllowlist.java on disk to check its class Javadoc");
		var body = Files.readString(src, StandardCharsets.UTF_8);
		var classStart = body.indexOf("public final class PopulatorAllowlist");
		assertTrue(classStart > 0, () -> "could not find 'public final class PopulatorAllowlist' in:\n" + src);
		var javadoc = body.substring(0, classStart).toLowerCase(Locale.ROOT);
		assertTrue(javadoc.contains("authoring-discipline") || javadoc.contains("authoring discipline"),
			() -> "PopulatorAllowlist's class Javadoc must keep stating that this allowlist is an authoring-discipline "
				+ "control, not a security boundary. A future edit removed that language - restore it so nobody "
				+ "transfers SinkRenderAllowlist's security value onto this class by mistake.");
		assertTrue(javadoc.contains("not a security boundary"),
			() -> "PopulatorAllowlist's class Javadoc must keep stating that this allowlist is NOT a security "
				+ "boundary. A future edit removed that language - restore it so nobody transfers "
				+ "SinkRenderAllowlist's security value onto this class by mistake.");
	}
}
