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
package org.apache.juneau.commons.settings;

import static org.apache.juneau.commons.utils.Utils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.jar.*;

import org.apache.juneau.commons.runtime.*;
import org.junit.jupiter.api.*;
import org.apache.juneau.commons.TestBase;

@SuppressWarnings({
	"java:S5976" // Explicit per-case tests are clearer than a single parameterized test for these distinct property-source scenarios.
})
class PropertySources_Test extends TestBase {

	//====================================================================================================
	// PropertyLookupResult
	//====================================================================================================

	@Test
	void a01_result_missing_isNotPresent() {
		assertFalse(PropertyLookupResult.missing().isPresent());
	}

	@Test
	void a02_result_missing_returnsSingleton() {
		assertSame(PropertyLookupResult.missing(), PropertyLookupResult.missing());
	}

	@Test
	void a03_result_present_withOptionalValue() {
		var r = PropertyLookupResult.present(opt("hello"));
		assertTrue(r.isPresent());
		assertEquals("hello", r.value().orElse(null));
	}

	@Test
	void a04_result_present_withOptionalEmpty() {
		var r = PropertyLookupResult.present(opte());
		assertTrue(r.isPresent());
		assertFalse(r.value().isPresent());
	}

	@Test
	void a05_result_present_withStringValue() {
		// Exercises the present(String) overload directly (not via present(Optional))
		var r = PropertyLookupResult.present("world");
		assertTrue(r.isPresent());
		assertEquals("world", r.value().orElse(null));
	}

	@Test
	void a06_result_present_withNullString() {
		var r = PropertyLookupResult.present((String) null);
		assertTrue(r.isPresent());
		assertFalse(r.value().isPresent());
	}

	@Test
	void a07_result_present_nullOptional_throws() {
		assertThrows(NullPointerException.class, () -> PropertyLookupResult.present((Optional<String>) null));
	}

	//====================================================================================================
	// PropertySourceProvider — default order()
	//====================================================================================================

	@Test
	void b01_provider_defaultOrder() {
		PropertySourceProvider provider = () -> null;
		assertEquals(0, provider.order());
	}

	//====================================================================================================
	// ArgsPropertySource
	//====================================================================================================

	@Test
	void c01_args_nullSupplier_missing() {
		var src = new ArgsPropertySource(() -> null);
		assertFalse(src.get("anything").isPresent());
	}

	@Test
	void c02_args_positionalIndex_found() {
		var src = new ArgsPropertySource(() -> new Args("pos0 pos1"));
		var r = src.get("0");
		assertTrue(r.isPresent());
		assertEquals("pos0", r.value().orElse(null));
	}

	@Test
	void c03_args_positionalIndex_outOfBounds_missing() {
		var src = new ArgsPropertySource(() -> new Args("pos0"));
		assertFalse(src.get("5").isPresent());
	}

	@Test
	void c04_args_namedOption_found() {
		var src = new ArgsPropertySource(() -> new Args("-port 8080"));
		var r = src.get("port");
		assertTrue(r.isPresent());
		assertEquals("8080", r.value().orElse(null));
	}

	@Test
	void c05_args_namedOption_missing() {
		var src = new ArgsPropertySource(() -> new Args("-port 8080"));
		assertFalse(src.get("host").isPresent());
	}

	@Test
	void c06_args_multiValueOption_joinedWithComma() {
		var src = new ArgsPropertySource(() -> new Args("-tag a -tag b -tag c"));
		var r = src.get("tag");
		assertTrue(r.isPresent());
		assertEquals("a,b,c", r.value().orElse(null));
	}

	@Test
	void c07_createDefaultArgs_sunJavaCommand_withArgs() {
		System.setProperty("sun.java.command", "com.example.Main --port 9090");
		try {
			var args = ArgsPropertySource.createDefaultArgs();
			assertNotNull(args);
			assertTrue(args.get("port").isPresent());
		} finally {
			System.clearProperty("sun.java.command");
		}
	}

	@Test
	void c08_createDefaultArgs_sunJavaCommand_noArgs() {
		// sun.java.command with no space → empty args string
		System.setProperty("sun.java.command", "com.example.Main");
		try {
			var args = ArgsPropertySource.createDefaultArgs();
			assertNotNull(args);
		} finally {
			System.clearProperty("sun.java.command");
		}
	}

	@Test
	void c09_createDefaultArgs_juneauArgsFallback() {
		System.clearProperty("sun.java.command");
		System.setProperty("juneau.args", "--env prod");
		try {
			var args = ArgsPropertySource.createDefaultArgs();
			assertNotNull(args);
			assertTrue(args.get("env").isPresent());
		} finally {
			System.clearProperty("juneau.args");
		}
	}

	@Test
	void c10_createDefaultArgs_bothAbsent() {
		System.clearProperty("sun.java.command");
		System.clearProperty("juneau.args");
		assertNotNull(ArgsPropertySource.createDefaultArgs());
	}

	//====================================================================================================
	// DotenvPropertySource
	//====================================================================================================

	@Test
	void d01_dotenv_missingFile_returnsEmpty() {
		var src = new DotenvPropertySource(Paths.get("nonexistent.env.file.xyz"));
		assertFalse(src.get("ANY_KEY").isPresent());
	}

	@Test
	void d02_dotenv_presentKey_found() throws IOException {
		var tmp = writeTempDotenv("MY_KEY=hello\n");
		try {
			var src = new DotenvPropertySource(tmp);
			var r = src.get("MY_KEY");
			assertTrue(r.isPresent());
			assertEquals("hello", r.value().orElse(null));
		} finally {
			Files.deleteIfExists(tmp);
		}
	}

	@Test
	void d03_dotenv_missingKey_missing() throws IOException {
		var tmp = writeTempDotenv("OTHER_KEY=value\n");
		try {
			var src = new DotenvPropertySource(tmp);
			assertFalse(src.get("MY_KEY").isPresent());
		} finally {
			Files.deleteIfExists(tmp);
		}
	}

	@Test
	void d04_dotenv_doubleQuotedValue() throws IOException {
		var tmp = writeTempDotenv("KEY=\"quoted value\"\n");
		try {
			var src = new DotenvPropertySource(tmp);
			assertEquals("quoted value", src.get("KEY").value().orElse(null));
		} finally {
			Files.deleteIfExists(tmp);
		}
	}

	@Test
	void d05_dotenv_singleQuotedValue() throws IOException {
		var tmp = writeTempDotenv("KEY='single'\n");
		try {
			var src = new DotenvPropertySource(tmp);
			assertEquals("single", src.get("KEY").value().orElse(null));
		} finally {
			Files.deleteIfExists(tmp);
		}
	}

	@Test
	void d06_dotenv_commentsAndBlankLinesIgnored() throws IOException {
		var tmp = writeTempDotenv("# comment\n\nKEY=value\n");
		try {
			var src = new DotenvPropertySource(tmp);
			assertEquals("value", src.get("KEY").value().orElse(null));
			assertFalse(src.get("# comment").isPresent());
		} finally {
			Files.deleteIfExists(tmp);
		}
	}

	@Test
	void d07_dotenv_lineWithNoEquals_ignored() throws IOException {
		var tmp = writeTempDotenv("INVALID_LINE\nGOOD=ok\n");
		try {
			var src = new DotenvPropertySource(tmp);
			assertFalse(src.get("INVALID_LINE").isPresent());
			assertEquals("ok", src.get("GOOD").value().orElse(null));
		} finally {
			Files.deleteIfExists(tmp);
		}
	}

	@Test
	void d08_dotenv_resolvePath_fromSystemProperty() throws IOException {
		var tmp = writeTempDotenv("DOTENV_SYS=found\n");
		System.setProperty("juneau.dotenv.path", tmp.toString());
		try {
			var src = new DotenvPropertySource();
			assertEquals("found", src.get("DOTENV_SYS").value().orElse(null));
		} finally {
			System.clearProperty("juneau.dotenv.path");
			Files.deleteIfExists(tmp);
		}
	}

	@Test
	void d09_dotenv_defaultConstructor_doesNotThrow() {
		// Default path ".env" may not exist; just verify no exception.
		System.clearProperty("juneau.dotenv.path");
		var src = new DotenvPropertySource();
		assertNotNull(src.get("ANYTHING"));
	}

	@Test
	void d10_dotenv_emptySystemProperty_fallsToDefault() {
		// Empty string value → same as absent → falls through to default ".env" path.
		System.setProperty("juneau.dotenv.path", "");
		try {
			var src = new DotenvPropertySource();
			assertNotNull(src.get("ANY_KEY")); // no exception, missing is fine
		} finally {
			System.clearProperty("juneau.dotenv.path");
		}
	}

	@Test
	void d11_dotenv_unmatchedQuotes_preservedAsIs() throws IOException {
		// Value starts with `"` but doesn't end with it → kept as-is (no strip).
		var tmp = writeTempDotenv("KEY=\"unmatched\n");
		try {
			var src = new DotenvPropertySource(tmp);
			var r = src.get("KEY");
			assertTrue(r.isPresent());
			// The value ends with a newline-stripped but unmatched quote is kept verbatim.
			assertTrue(r.value().orElse("").startsWith("\""));
		} finally {
			Files.deleteIfExists(tmp);
		}
	}

	@Test
	void d12_dotenv_singleCharValue_quotedBothEnds() throws IOException {
		// Edge: value is exactly one char between single quotes, e.g. KEY='' → empty string.
		var tmp = writeTempDotenv("KEY=''\n");
		try {
			var src = new DotenvPropertySource(tmp);
			assertEquals("", src.get("KEY").value().orElse("x"));
		} finally {
			Files.deleteIfExists(tmp);
		}
	}

	private static Path writeTempDotenv(String content) throws IOException {
		var tmp = Files.createTempFile("juneau-test-", ".env");
		Files.writeString(tmp, content);
		return tmp;
	}

	//====================================================================================================
	// ManifestFilePropertySource
	//====================================================================================================

	@Test
	void e01_manifest_nullSupplier_missing() {
		var src = new ManifestFilePropertySource(() -> null);
		assertFalse(src.get("Main-Class").isPresent());
	}

	@Test
	void e02_manifest_presentKey_found() {
		var mf = buildManifest("Main-Class", "com.example.Main");
		var src = new ManifestFilePropertySource(() -> mf);
		var r = src.get("Main-Class");
		assertTrue(r.isPresent());
		assertEquals("com.example.Main", r.value().orElse(null));
	}

	@Test
	void e03_manifest_missingKey_missing() {
		var mf = buildManifest("Main-Class", "com.example.Main");
		var src = new ManifestFilePropertySource(() -> mf);
		assertFalse(src.get("Implementation-Version").isPresent());
	}

	@Test
	void e04_manifest_createDefault_doesNotThrow() {
		var src = ManifestFilePropertySource.createDefault();
		assertNotNull(src);
		assertNotNull(src.get("Main-Class"));
	}

	private static ManifestFile buildManifest(String key, String value) {
		var manifest = new Manifest();
		manifest.getMainAttributes().putValue(key, value);
		return new ManifestFile(manifest);
	}
}
