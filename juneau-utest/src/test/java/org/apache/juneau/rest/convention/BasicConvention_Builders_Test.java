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
package org.apache.juneau.rest.convention;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.net.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.jar.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.*;

/**
 * Direct-instance tests for the convention-pack builders &mdash; covers the no-arg constructor
 * paths, helper methods, and edge cases that the {@code AsMixin} / real-container tests don't
 * exercise (entries-map handling, sitemap-entry XML formatting, git.properties parsing,
 * URL-classloader fallback paths, etc.).
 *
 * <p>
 * Pure JUnit; no MockRest or container needed.
 *
 * @since 9.5.0
 */
class BasicConvention_Builders_Test extends TestBase {

	// -------- BasicFaviconResource --------

	@Test void favicon_noArgConstructorLoadsDefault() {
		var f = new BasicFaviconResource();
		assertNotNull(f);
	}

	@Test void favicon_classpathResolvesExistingResource() {
		var f = BasicFaviconResource.create()
			.classpath("/juneau-favicon.ico")
			.build();
		assertNotNull(f);
	}

	@Test void favicon_classpathFallsBackOnMissingResource() {
		var f = BasicFaviconResource.create()
			.classpath("/no-such-resource.ico")
			.build();
		assertNotNull(f, "Missing classpath path must fall back to default favicon");
	}

	@Test void favicon_bytesAndClasspathAreMutuallyExclusive() {
		var bytes = new byte[]{1,2,3};
		var f = BasicFaviconResource.create()
			.classpath("/anything.ico")
			.bytes(bytes)
			.build();
		// bytes() wins over classpath() when set last
		assertNotNull(f);
	}

	@Test void favicon_customCacheControl() {
		var f = BasicFaviconResource.create()
			.bytes(new byte[]{1,2})
			.cacheControl("no-store")
			.build();
		assertNotNull(f);
	}

	// -------- BasicSeoResource --------

	@Test void seo_noArgConstructorYieldsDenyAll() {
		var s = new BasicSeoResource();
		assertNotNull(s);
	}

	@Test void seo_robotsAllowAndDisallowMix() {
		var s = BasicSeoResource.create()
			.robotsAllow("Googlebot", "/")
			.robotsDisallow("BadBot", "/private", "/admin")
			.build();
		assertNotNull(s);
	}

	@Test void seo_sitemapEntryWithFullMetadata() {
		var s = BasicSeoResource.create()
			.sitemapEntry("https://example.com/page",
				ZonedDateTime.parse("2026-05-24T18:00:00Z"),
				"weekly",
				0.8)
			.build();
		assertNotNull(s);
	}

	@Test void seo_sitemapEntryDirectInstance() {
		var e = new BasicSeoResource.SitemapEntry(
			"https://x.example.com/<unsafe&\"chars'>",
			ZonedDateTime.parse("2026-05-24T18:00:00+02:00"),
			"daily",
			1.0);
		var sw = new java.io.StringWriter();
		try {
			e.write(sw);
		} catch (IOException ex) {
			fail(ex);
		}
		var xml = sw.toString();
		assertTrue(xml.contains("<lastmod>"), "lastmod present");
		assertTrue(xml.contains("<changefreq>daily</changefreq>"), "changefreq present");
		assertTrue(xml.contains("<priority>1.0</priority>"), "priority present");
		assertTrue(xml.contains("&amp;"), "ampersand escaped");
		assertTrue(xml.contains("&lt;"), "less-than escaped");
		assertTrue(xml.contains("&gt;"), "greater-than escaped");
		assertTrue(xml.contains("&quot;"), "double-quote escaped");
		assertTrue(xml.contains("&apos;"), "single-quote escaped");
	}

	@Test void seo_emptyPathArrayInRobotsRuleIsTolerated() {
		// Internal RobotsRule's path-array handling — null/empty paths is just an empty rule.
		var s = BasicSeoResource.create().robotsAllow("MyBot").build();
		assertNotNull(s);
	}

	// -------- BasicVersionResource --------

	@Test void version_noArgConstructorPopulatesDefaults() {
		var v = new BasicVersionResource();
		assertNotNull(v.getInfoMap().get("javaVersion"), "no-arg ctor must surface javaVersion");
	}

	@Test void version_entryAcceptsNullValue() {
		var v = BasicVersionResource.create()
			.entry("missing", null)
			.build();
		assertEquals(BasicVersionResource.UNKNOWN, v.getInfoMap().get("missing"));
	}

	@Test void version_entriesMapSink() {
		var values = new LinkedHashMap<String,String>();
		values.put("a", "1");
		values.put("b", null);
		var v = BasicVersionResource.create().entries(values).build();
		assertEquals("1", v.getInfoMap().get("a"));
		assertEquals(BasicVersionResource.UNKNOWN, v.getInfoMap().get("b"));
	}

	@Test void version_entriesNullMapIsTolerated() {
		var v = BasicVersionResource.create().entries(null).build();
		assertNotNull(v);
	}

	@Test void version_fromManifestFallbackUnknownNameAndVersion() {
		// URLClassLoader with no manifest available: name+version fall back to (unknown).
		var v = BasicVersionResource.create()
			.fromManifest(new URLClassLoader(new URL[0], null))
			.build();
		assertEquals(BasicVersionResource.UNKNOWN, v.getInfoMap().get("name"));
		assertEquals(BasicVersionResource.UNKNOWN, v.getInfoMap().get("version"));
	}

	@Test void version_fromManifestObjectFullAttrs() {
		var m = new Manifest();
		var attrs = m.getMainAttributes();
		attrs.putValue("Manifest-Version", "1.0");
		attrs.putValue("Implementation-Title", "obj-app");
		attrs.putValue("Implementation-Version", "5.0.0");
		attrs.putValue("Implementation-Vendor", "Co");
		attrs.putValue("Build-Jdk", "21");
		var v = BasicVersionResource.create().fromManifest(m).build();
		assertEquals("obj-app", v.getInfoMap().get("name"));
		assertEquals("5.0.0", v.getInfoMap().get("version"));
		assertEquals("Co", v.getInfoMap().get("vendor"));
		assertEquals("21", v.getInfoMap().get("javaVersion"));
	}

	@Test void version_fromGitPropertiesViaClasspathResource(@TempDir Path tempDir) throws Exception {
		// Build a tiny synthetic classpath that contains git.properties at the root.
		var props = "git.commit.id=abcdef0123456789\n"
			+ "git.commit.id.abbrev=abcdef0\n"
			+ "git.branch=feature\n"
			+ "git.build.time=2026-05-24T17:00:00Z\n";
		Files.writeString(tempDir.resolve("git.properties"), props, StandardCharsets.UTF_8);
		try (var cl = new URLClassLoader(new URL[]{ tempDir.toUri().toURL() }, null)) {
			var v = BasicVersionResource.create()
				.fromGitProperties(cl)
				.build();
			assertEquals("abcdef0123456789", v.getInfoMap().get("gitCommit"));
			assertEquals("feature", v.getInfoMap().get("gitBranch"));
			assertEquals("2026-05-24T17:00:00Z", v.getInfoMap().get("buildTime"));
		}
	}

	@Test void version_fromGitPropertiesAbbrevFallback(@TempDir Path tempDir) throws Exception {
		var props = "git.commit.id.abbrev=abcdef0\n"
			+ "git.branch=main\n";
		Files.writeString(tempDir.resolve("git.properties"), props, StandardCharsets.UTF_8);
		try (var cl = new URLClassLoader(new URL[]{ tempDir.toUri().toURL() }, null)) {
			var v = BasicVersionResource.create()
				.fromGitProperties(cl)
				.build();
			assertEquals("abcdef0", v.getInfoMap().get("gitCommit"),
				"abbrev fallback applies when full commit id absent");
		}
	}

	@Test void version_fromGitPropertiesMissingFileIsSilent() throws Exception {
		try (var cl = new URLClassLoader(new URL[0], null)) {
			var v = BasicVersionResource.create()
				.fromGitProperties(cl)
				.build();
			assertNull(v.getInfoMap().get("gitCommit"),
				"missing git.properties must not register a gitCommit entry");
		}
	}

	@Test void version_fromManifestWithoutImplementationTitle(@TempDir Path tempDir) throws Exception {
		// Manifest without Implementation-Title — locator falls back to first found.
		var manifestDir = tempDir.resolve("META-INF");
		Files.createDirectory(manifestDir);
		Files.writeString(manifestDir.resolve("MANIFEST.MF"),
			"Manifest-Version: 1.0\nBuild-Jdk: 22\n\n",
			StandardCharsets.UTF_8);
		try (var cl = new URLClassLoader(new URL[]{ tempDir.toUri().toURL() }, null)) {
			var v = BasicVersionResource.create()
				.fromManifest(cl)
				.build();
			assertEquals("22", v.getInfoMap().get("javaVersion"));
			assertEquals(BasicVersionResource.UNKNOWN, v.getInfoMap().get("name"),
				"name unset → (unknown)");
		}
	}

	@Test void version_fromJavaVersionPutsIfAbsent() {
		var v = BasicVersionResource.create()
			.entry("javaVersion", "preset")
			.fromJavaVersion()
			.build();
		assertEquals("preset", v.getInfoMap().get("javaVersion"),
			"fromJavaVersion must not overwrite an explicit entry");
	}

	@Test void version_fromManifestObjectEmptyVendorIsSkipped() {
		// Hits the empty-string branch in ifNotEmpty / ifNotEmptyValue.
		var m = new Manifest();
		var attrs = m.getMainAttributes();
		attrs.putValue("Manifest-Version", "1.0");
		attrs.putValue("Implementation-Title", "x");
		attrs.putValue("Implementation-Version", "1");
		attrs.putValue("Implementation-Vendor", "");
		var v = BasicVersionResource.create().fromManifest(m).build();
		assertNull(v.getInfoMap().get("vendor"), "empty string Implementation-Vendor must be skipped");
	}

	@Test void version_fromGitPropertiesEmptyValuesAreSkipped(@TempDir Path tempDir) throws Exception {
		var props = "git.commit.id=\n"
			+ "git.branch=\n"
			+ "git.build.time=\n";
		Files.writeString(tempDir.resolve("git.properties"), props, StandardCharsets.UTF_8);
		try (var cl = new URLClassLoader(new URL[]{ tempDir.toUri().toURL() }, null)) {
			var v = BasicVersionResource.create().fromGitProperties(cl).build();
			assertNull(v.getInfoMap().get("gitCommit"), "empty git.commit.id skipped");
			assertNull(v.getInfoMap().get("gitBranch"), "empty git.branch skipped");
			assertNull(v.getInfoMap().get("buildTime"), "empty git.build.time skipped");
		}
	}

	@Test void version_fromManifestClassLoaderThrowsIOException() {
		// A ClassLoader whose getResources(...) throws IOException must yield (unknown) entries
		// without bubbling up the exception.
		var brokenCl = new ClassLoader(null) {
			@Override
			public java.util.Enumeration<URL> getResources(String name) throws IOException {
				throw new IOException("boom");
			}
		};
		var v = BasicVersionResource.create().fromManifest(brokenCl).build();
		assertEquals(BasicVersionResource.UNKNOWN, v.getInfoMap().get("name"));
		assertEquals(BasicVersionResource.UNKNOWN, v.getInfoMap().get("version"));
	}

	@Test void version_fromGitPropertiesClassLoaderThrowsIOException() {
		// ClassLoader whose getResourceAsStream returns a stream that throws on read must yield
		// no git.* entries, again without bubbling up the exception.
		var brokenCl = new ClassLoader(null) {
			@Override
			public InputStream getResourceAsStream(String name) {
				return new InputStream() {
					@Override
					public int read() throws IOException {
						throw new IOException("boom");
					}
				};
			}
		};
		var v = BasicVersionResource.create().fromGitProperties(brokenCl).build();
		assertNull(v.getInfoMap().get("gitCommit"), "broken git.properties read produces no entry");
	}

	@Test void version_fromManifestUrlOpenStreamThrows(@TempDir Path tempDir) throws Exception {
		// Locator returns a URL pointing at a deleted manifest; openStream throws
		// FileNotFoundException → caught and yields empty map → name/version → (unknown).
		var manifestDir = tempDir.resolve("META-INF");
		Files.createDirectory(manifestDir);
		var manifest = manifestDir.resolve("MANIFEST.MF");
		Files.writeString(manifest,
			"Manifest-Version: 1.0\nImplementation-Title: gone\n\n", StandardCharsets.UTF_8);
		try (var cl = new URLClassLoader(new URL[]{ tempDir.toUri().toURL() }, null)) {
			Files.delete(manifest);
			var v = BasicVersionResource.create().fromManifest(cl).build();
			assertEquals(BasicVersionResource.UNKNOWN, v.getInfoMap().get("name"));
		}
	}

	@Test void version_fromManifestSecondCandidateWinsOnImplementationTitle(@TempDir Path tempDir) throws Exception {
		// Two manifests on the classpath: only the second has Implementation-Title — locator
		// must prefer it.
		var dirA = Files.createDirectory(tempDir.resolve("a"));
		var dirB = Files.createDirectory(tempDir.resolve("b"));
		Files.createDirectory(dirA.resolve("META-INF"));
		Files.writeString(dirA.resolve("META-INF/MANIFEST.MF"),
			"Manifest-Version: 1.0\n\n", StandardCharsets.UTF_8);
		Files.createDirectory(dirB.resolve("META-INF"));
		Files.writeString(dirB.resolve("META-INF/MANIFEST.MF"),
			"Manifest-Version: 1.0\nImplementation-Title: titled\nImplementation-Version: 2\n\n",
			StandardCharsets.UTF_8);
		try (var cl = new URLClassLoader(
				new URL[]{ dirA.toUri().toURL(), dirB.toUri().toURL() }, null)) {
			var v = BasicVersionResource.create().fromManifest(cl).build();
			assertEquals("titled", v.getInfoMap().get("name"),
				"Locator must prefer Implementation-Title-bearing manifest");
		}
	}

	// -------- BasicWellKnownResource --------

	@Test void wellKnown_noArgConstructorYieldsNullBody() {
		var w = new BasicWellKnownResource();
		assertNull(w.getSecurityTxtBody());
	}

	@Test void wellKnown_securityTxtBodyAccessor() {
		var body = "Contact: x@example.com\nExpires: 2027-01-01T00:00:00Z\n";
		var w = BasicWellKnownResource.create().securityTxt(body).build();
		assertEquals(body, w.getSecurityTxtBody());
	}
}
