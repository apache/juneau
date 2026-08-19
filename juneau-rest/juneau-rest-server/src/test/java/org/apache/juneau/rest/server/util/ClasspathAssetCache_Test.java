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
package org.apache.juneau.rest.server.util;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;

import org.apache.juneau.commons.utils.*;
import org.junit.jupiter.api.*;

/**
 * Tests {@link ClasspathAssetCache}.
 */
class ClasspathAssetCache_Test {

	/** A resource guaranteed present on this module's own test classpath (this very test's compiled class file). */
	private static final String OWN_CLASS_RESOURCE = "/org/apache/juneau/rest/server/util/ClasspathAssetCache_Test.class";

	/** A second, distinct resource on the same classpath, for cross-resource hash-collision checks. */
	private static final String OTHER_CLASS_RESOURCE = "/org/apache/juneau/rest/server/util/ClasspathAssetCache.class";

	//------------------------------------------------------------------------------------------------------------------
	// a) bytes(...): read-and-cache
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_bytes_returnsTheResourcesActualContent() {
		var cache = new ClasspathAssetCache(ClasspathAssetCache_Test.class);
		var bytes = cache.bytes(OWN_CLASS_RESOURCE);
		assertTrue(bytes.length > 0);
		// A compiled .class file always starts with the JVM class-file magic number.
		assertEquals((byte) 0xCA, bytes[0]);
		assertEquals((byte) 0xFE, bytes[1]);
	}

	@Test void a02_bytes_sameResource_isServedFromCacheOnSecondCall() {
		// computeIfAbsent returns the SAME array instance on a cache hit; a fresh read would return a distinct
		// (if equal-content) array, so reference equality pins that the resource is read at most once.
		var cache = new ClasspathAssetCache(ClasspathAssetCache_Test.class);
		var first = cache.bytes(OWN_CLASS_RESOURCE);
		var second = cache.bytes(OWN_CLASS_RESOURCE);
		assertSame(first, second, "expected the cached byte[] instance, not a freshly-read copy");
	}

	@Test void a03_bytes_distinctResources_areNotConflated() {
		var cache = new ClasspathAssetCache(ClasspathAssetCache_Test.class);
		assertFalse(java.util.Arrays.equals(cache.bytes(OWN_CLASS_RESOURCE), cache.bytes(OTHER_CLASS_RESOURCE)));
	}

	@Test void a04_bytes_missingResource_throwsUncheckedIOException() {
		var cache = new ClasspathAssetCache(ClasspathAssetCache_Test.class);
		assertThrows(UncheckedIOException.class, () -> cache.bytes("/no/such/resource.bin"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// b) hash(...): content-hash cache, backed by ChecksumUtils.hash8
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_hash_matchesChecksumUtilsHash8OfTheSameBytes() {
		var cache = new ClasspathAssetCache(ClasspathAssetCache_Test.class);
		var bytes = cache.bytes(OWN_CLASS_RESOURCE);
		assertEquals(ChecksumUtils.hash8(bytes), cache.hash(OWN_CLASS_RESOURCE));
	}

	@Test void b02_hash_isStableAcrossCalls() {
		var cache = new ClasspathAssetCache(ClasspathAssetCache_Test.class);
		assertEquals(cache.hash(OWN_CLASS_RESOURCE), cache.hash(OWN_CLASS_RESOURCE));
	}

	@Test void b03_hash_distinctResources_doNotCollide() {
		var cache = new ClasspathAssetCache(ClasspathAssetCache_Test.class);
		assertNotEquals(cache.hash(OWN_CLASS_RESOURCE), cache.hash(OTHER_CLASS_RESOURCE));
	}

	//------------------------------------------------------------------------------------------------------------------
	// c) buildVersion(): resolved from the constructor-supplied anchor, "dev" fallback
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_buildVersion_fallsBackToDev_whenAnchorPackageHasNoImplementationVersion() {
		// Every anchor in this unpackaged-classes test run has a null Package#getImplementationVersion() (no
		// manifest), so this pins the documented "dev" fallback directly.
		assertNull(ClasspathAssetCache_Test.class.getPackage().getImplementationVersion(), "test precondition: expected an unpackaged (manifest-less) test run");
		var cache = new ClasspathAssetCache(ClasspathAssetCache_Test.class);
		assertEquals("dev", cache.buildVersion());
	}

	@Test void c02_buildVersion_isConsistentWithDirectPackageLookupOnTheAnchor() {
		// Same anchor, computed two ways: cache.buildVersion() and Package#getImplementationVersion() read
		// directly off the SAME anchor class - equal in either branch (real version or "dev" fallback), so this
		// stays valid even if a future packaged run gives the anchor a real Implementation-Version.
		var cache = new ClasspathAssetCache(ClasspathAssetCache_Test.class);
		var direct = ClasspathAssetCache_Test.class.getPackage().getImplementationVersion();
		assertEquals(direct == null ? "dev" : direct, cache.buildVersion());
	}

	@Test void c03_constructor_storesTheSuppliedAnchor_notTheCachesOwnClass() {
		// Guards the version-anchor design point directly: the cache must resolve buildVersion() from the
		// MIXIN-supplied anchor, not from ClasspathAssetCache's own class/package - a hardcode of `getClass()`
		// (or similar) in place of the constructor argument would flip this to ClasspathAssetCache.class, and
		// this assertion catches that even though both currently resolve to "dev".
		var cache = new ClasspathAssetCache(ClasspathAssetCache_Test.class);
		assertSame(ClasspathAssetCache_Test.class, cache.anchor());
	}

	@Test void c04_constructor_rejectsNullAnchor() {
		assertThrows(NullPointerException.class, () -> new ClasspathAssetCache(null));
	}

	//------------------------------------------------------------------------------------------------------------------
	// d) cacheBuster(...): "?v=<buildVersion>-<hash8>"
	//------------------------------------------------------------------------------------------------------------------

	@Test void d01_cacheBuster_isVEqualsBuildVersionDashHash8() {
		var cache = new ClasspathAssetCache(ClasspathAssetCache_Test.class);
		assertEquals("?v=" + cache.buildVersion() + "-" + cache.hash(OWN_CLASS_RESOURCE), cache.cacheBuster(OWN_CLASS_RESOURCE));
	}

	//------------------------------------------------------------------------------------------------------------------
	// e) wrap(...) / serve(...): cacheable HttpResource with the requested Content-Type/Cache-Control
	//------------------------------------------------------------------------------------------------------------------

	@Test void e01_wrap_setsContentTypeHeader() {
		var cache = new ClasspathAssetCache(ClasspathAssetCache_Test.class);
		var resource = cache.wrap(new byte[]{1, 2, 3}, "image/svg+xml", "max-age=86400, public");
		assertEquals("image/svg+xml", resource.getContentType());
	}

	@Test void e02_wrap_setsCacheControlHeader() {
		var cache = new ClasspathAssetCache(ClasspathAssetCache_Test.class);
		var resource = cache.wrap(new byte[]{1, 2, 3}, "image/svg+xml", "max-age=86400, public");
		assertEquals("max-age=86400, public", cacheControlOf(resource));
	}

	@Test void e03_wrap_bodyBytes_matchTheSuppliedBytes() throws IOException {
		var cache = new ClasspathAssetCache(ClasspathAssetCache_Test.class);
		var expected = new byte[]{1, 2, 3, 4};
		var resource = cache.wrap(expected, "application/octet-stream", "max-age=86400, public");
		assertArrayEquals(expected, writtenBytes(resource));
	}

	@Test void e04_serve_bodyBytes_matchTheClasspathResourcesActualBytes() throws IOException {
		var cache = new ClasspathAssetCache(ClasspathAssetCache_Test.class);
		var resource = cache.serve(OWN_CLASS_RESOURCE, "application/octet-stream", "max-age=86400, public");
		assertArrayEquals(cache.bytes(OWN_CLASS_RESOURCE), writtenBytes(resource));
	}

	@Test void e05_serve_setsContentTypeAndCacheControl() {
		var cache = new ClasspathAssetCache(ClasspathAssetCache_Test.class);
		var resource = cache.serve(OWN_CLASS_RESOURCE, "application/octet-stream", "max-age=3600, public");
		assertEquals("application/octet-stream", resource.getContentType());
		assertEquals("max-age=3600, public", cacheControlOf(resource));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Test helpers
	//------------------------------------------------------------------------------------------------------------------

	private static byte[] writtenBytes(org.apache.juneau.http.HttpBody body) throws IOException {
		var out = new ByteArrayOutputStream();
		body.writeTo(out);
		return out.toByteArray();
	}

	private static String cacheControlOf(org.apache.juneau.http.HttpResource resource) {
		return resource.getHeaders().stream()
			.filter(h -> h.getName().equalsIgnoreCase("Cache-Control"))
			.findFirst()
			.map(org.apache.juneau.http.HttpHeader::getValue)
			.orElseThrow(() -> new AssertionError("no Cache-Control header found"));
	}
}
