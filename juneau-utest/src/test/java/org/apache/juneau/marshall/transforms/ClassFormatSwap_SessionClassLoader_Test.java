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
package org.apache.juneau.marshall.transforms;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

import java.net.*;
import java.nio.file.*;

import javax.tools.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.json.*;
import org.junit.jupiter.api.*;

/**
 * Verifies that {@link org.apache.juneau.marshall.swaps.ClassFormatSwap#unswap} consults the session-installed
 * classloader before falling back to the thread-context classloader (FINISHED-138).
 *
 * <p>
 * Three scenarios are exercised:
 * <ol>
 *   <li>Happy path — session CL resolves a class that is invisible to the thread-context CL.</li>
 *   <li>Fallback — no session CL is set; the thread-context CL resolves the class as before.</li>
 *   <li>Isolation — when a session CL is set, the thread-context CL is NOT consulted as a fallback,
 *       so a class that lives only in the thread-context CL cannot be resolved through a session CL
 *       that does not have it.</li>
 * </ol>
 */
class ClassFormatSwap_SessionClassLoader_Test extends TestBase {

	/**
	 * FQCN of the class compiled into the isolated temp directory.  This class does not exist on the
	 * test classpath and is therefore invisible to the thread-context classloader.
	 */
	private static final String ISOLATED_FQCN = "org.apache.juneau.marshall.test.isolated.IsolatedBean";

	/** URLs pointing at the isolated class output directory — passed to the session CL. */
	private static URL[] isolatedUrls;

	/** Temp directory that holds the compiled isolated class. */
	private static Path tempDir;

	/**
	 * Compiles a tiny {@code IsolatedBean} class into a fresh temp directory using the system Java
	 * compiler so it is invisible to the test classpath / thread-context classloader.
	 */
	@BeforeAll
	static void compileIsolatedBean() throws Exception {
		var compiler = ToolProvider.getSystemJavaCompiler();
		assumeTrue(compiler != null, "System Java compiler unavailable — skipping session-CL tests");

		tempDir = Files.createTempDirectory("juneau-cltest-");
		var srcPkg = tempDir.resolve(Path.of("src", "org", "apache", "juneau", "test", "isolated"));
		Files.createDirectories(srcPkg);
		var srcFile = srcPkg.resolve("IsolatedBean.java");
		Files.writeString(srcFile, "package org.apache.juneau.marshall.test.isolated; public class IsolatedBean {}");

		var outDir = tempDir.resolve("classes");
		Files.createDirectories(outDir);

		var exitCode = compiler.run(null, null, null, "-d", outDir.toString(), srcFile.toString());
		assertEquals(0, exitCode, "IsolatedBean compilation failed");

		isolatedUrls = new URL[]{outDir.toUri().toURL()};
	}

	@AfterAll
	static void cleanupTempDir() {
		if (tempDir != null) {
			try {
				try (var walk = Files.walk(tempDir)) {
					walk.sorted(java.util.Comparator.reverseOrder())
						.map(Path::toFile)
						.forEach(java.io.File::delete);
				}
			} catch (Exception ignored) { // HTT
			}
		}
	}

	//====================================================================================================
	// a01 — happy path: session CL resolves IsolatedBean; thread-context CL cannot
	//====================================================================================================

	/**
	 * When a session classloader is installed that contains {@code IsolatedBean}, parsing its FQCN
	 * succeeds and the returned {@code Class} is loaded by the session CL.
	 */
	@Test void a01_sessionCL_resolvesClassInvisibleToThreadContextCL() throws Exception {
		// Confirm thread-context CL cannot find IsolatedBean — our baseline.
		assertThrows(Exception.class,
			() -> JsonParser.create().build().parse("\"" + ISOLATED_FQCN + "\"", Class.class),
			"Precondition: thread-context CL must NOT resolve IsolatedBean"
		);

		// With the session CL pointing at the compiled class, parsing must succeed.
		try (var sessionCL = new URLClassLoader(isolatedUrls, null)) {
			var parser = JsonParser.create().classLoader(sessionCL).build();
			var result = parser.parse("\"" + ISOLATED_FQCN + "\"", Class.class);

			assertNotNull(result);
			assertEquals(ISOLATED_FQCN, result.getName());
			// The class was loaded by the session CL, not the thread-context CL.
			assertSame(sessionCL, result.getClassLoader(),
				"Class must be defined by the session CL, not the thread-context CL");
		}
	}

	//====================================================================================================
	// a02 — fallback: no session CL → thread-context CL used (historical behavior preserved)
	//====================================================================================================

	/**
	 * When no session classloader is configured, the thread-context classloader is still used as the
	 * fallback — preserving the historical behavior for existing callers.
	 */
	@Test void a02_noSessionCL_fallsBackToThreadContextCL() throws Exception {
		// JsonParser itself is on the test classpath and therefore resolvable via thread-context CL.
		var fqcn = JsonParser.class.getName();
		var parser = JsonParser.create().build();

		var result = parser.parse("\"" + fqcn + "\"", Class.class);

		assertNotNull(result);
		assertEquals(fqcn, result.getName());
	}

	//====================================================================================================
	// a03 — isolation: session CL is set but cannot see the target → no silent thread-CL fallback
	//====================================================================================================

	/**
	 * When a session classloader is explicitly set, the thread-context classloader is NOT consulted
	 * as a fallback.  A class that lives only on the test classpath cannot be found through an empty
	 * session CL, even though the thread-context CL would resolve it.
	 *
	 * <p>
	 * This proves the session CL is genuinely consulted first — not the thread CL — and that there is
	 * no silent double-lookup that would degrade to the old behavior when the session CL comes up empty.
	 */
	@Test void a03_sessionCL_set_noFallbackToThreadContextCL() throws Exception {
		// JsonParser lives on the test classpath (thread-context CL sees it).
		// An empty URLClassLoader with null parent only delegates to the bootstrap CL
		// (JDK core classes only) — it cannot see non-JDK test classpath classes.
		var fqcn = JsonParser.class.getName();

		try (var emptyCL = new URLClassLoader(new URL[0], null)) {
			var parser = JsonParser.create().classLoader(emptyCL).build();

			// Must fail: session CL cannot find JsonParser, and there is no thread-CL fallback.
			assertThrows(Exception.class,
				() -> parser.parse("\"" + fqcn + "\"", Class.class),
				"Should fail — session CL is set but cannot find " + fqcn
					+ "; thread-context CL must NOT be consulted as a fallback"
			);
		}
	}
}
