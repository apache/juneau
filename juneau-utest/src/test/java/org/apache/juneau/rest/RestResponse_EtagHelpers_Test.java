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
package org.apache.juneau.rest;

import static org.junit.jupiter.api.Assertions.*;

import java.time.*;

import org.apache.juneau.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.mock.classic.*;
import org.junit.jupiter.api.*;

/**
 * Tests for the conditional-GET response helpers on {@link RestResponse}:
 * {@code eTag(...)}, {@code lastModified(...)}, and {@code cacheControl(...)}.
 */
class RestResponse_EtagHelpers_Test extends TestBase {

	//-----------------------------------------------------------------------------------------------------------------
	// A: eTag(...) — strong vs weak vs wildcard wire-format
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class A {
		@RestGet("/strongString")
		public String strongString(RestResponse res) {
			res.eTag("\"v42\"");
			return "ok";
		}

		@RestGet("/weakString")
		public String weakString(RestResponse res) {
			res.eTag("W/\"v42\"");
			return "ok";
		}

		@RestGet("/wildcardString")
		public String wildcardString(RestResponse res) {
			res.eTag("*");
			return "ok";
		}

		@RestGet("/strongTyped")
		public String strongTyped(RestResponse res) {
			res.eTag(EntityTag.of("\"v42\""));
			return "ok";
		}

		@RestGet("/weakTyped")
		public String weakTyped(RestResponse res) {
			res.eTag(EntityTag.of("W/\"v42\""));
			return "ok";
		}
	}

	@Test void a01_strongStringEmitsQuoted() throws Exception {
		var a = MockRestClient.buildLax(A.class);
		a.get("/strongString").run().assertStatus(200).assertHeader("ETag").is("\"v42\"");
	}

	@Test void a02_weakStringEmitsWeakPrefix() throws Exception {
		var a = MockRestClient.buildLax(A.class);
		a.get("/weakString").run().assertStatus(200).assertHeader("ETag").is("W/\"v42\"");
	}

	@Test void a03_wildcardStringEmitsStar() throws Exception {
		var a = MockRestClient.buildLax(A.class);
		a.get("/wildcardString").run().assertStatus(200).assertHeader("ETag").is("*");
	}

	@Test void a04_strongTypedEmitsQuoted() throws Exception {
		var a = MockRestClient.buildLax(A.class);
		a.get("/strongTyped").run().assertStatus(200).assertHeader("ETag").is("\"v42\"");
	}

	@Test void a05_weakTypedEmitsWeakPrefix() throws Exception {
		var a = MockRestClient.buildLax(A.class);
		a.get("/weakTyped").run().assertStatus(200).assertHeader("ETag").is("W/\"v42\"");
	}

	@Test void a06_malformedStringRejected() {
		// Direct call into EntityTag.of validates - bare unquoted tags throw.
		assertThrows(IllegalArgumentException.class, () -> EntityTag.of("v42"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// B: lastModified(...) — IMF-fixdate formatting, instant vs zoned, UTC conversion, truncation
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class B {
		@RestGet("/fromInstant")
		public String fromInstant(RestResponse res) {
			res.lastModified(Instant.parse("2026-05-22T00:00:00Z"));
			return "ok";
		}

		@RestGet("/fromZonedUtc")
		public String fromZonedUtc(RestResponse res) {
			res.lastModified(ZonedDateTime.parse("2026-05-22T00:00:00Z"));
			return "ok";
		}

		@RestGet("/fromZonedOffset")
		public String fromZonedOffset(RestResponse res) {
			// 02:30 +02:30 = 00:00 UTC ⇒ same wire result as fromZonedUtc.
			res.lastModified(ZonedDateTime.parse("2026-05-22T02:30:00+02:30"));
			return "ok";
		}

		@RestGet("/truncatesFraction")
		public String truncatesFraction(RestResponse res) {
			res.lastModified(Instant.parse("2026-05-22T00:00:00.987654321Z"));
			return "ok";
		}
	}

	@Test void b01_instantFormattedAsImfFixdate() throws Exception {
		var a = MockRestClient.buildLax(B.class);
		a.get("/fromInstant").run().assertStatus(200)
			.assertHeader("Last-Modified").is("Fri, 22 May 2026 00:00:00 GMT");
	}

	@Test void b02_zonedUtcFormattedAsImfFixdate() throws Exception {
		var a = MockRestClient.buildLax(B.class);
		a.get("/fromZonedUtc").run().assertStatus(200)
			.assertHeader("Last-Modified").is("Fri, 22 May 2026 00:00:00 GMT");
	}

	@Test void b03_zonedOffsetConvertedToUtc() throws Exception {
		var a = MockRestClient.buildLax(B.class);
		a.get("/fromZonedOffset").run().assertStatus(200)
			.assertHeader("Last-Modified").is("Fri, 22 May 2026 00:00:00 GMT");
	}

	@Test void b04_fractionalSecondsTruncated() throws Exception {
		var a = MockRestClient.buildLax(B.class);
		a.get("/truncatesFraction").run().assertStatus(200)
			.assertHeader("Last-Modified").is("Fri, 22 May 2026 00:00:00 GMT");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// C: cacheControl(...) — string vs builder
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class C {
		@RestGet("/fromString")
		public String fromString(RestResponse res) {
			res.cacheControl("public, max-age=3600");
			return "ok";
		}

		@RestGet("/fromBuilder")
		public String fromBuilder(RestResponse res) {
			res.cacheControl(CacheControlBuilder.create().publicCache().maxAge(3600).mustRevalidate());
			return "ok";
		}

		@RestGet("/privateNoStore")
		public String privateNoStore(RestResponse res) {
			res.cacheControl(CacheControlBuilder.create().privateCache().noStore());
			return "ok";
		}

		@RestGet("/immutable")
		public String immutable(RestResponse res) {
			res.cacheControl(CacheControlBuilder.create().publicCache().maxAge(31536000L).immutable());
			return "ok";
		}
	}

	@Test void c01_stringFormVerbatim() throws Exception {
		var a = MockRestClient.buildLax(C.class);
		a.get("/fromString").run().assertStatus(200).assertHeader("Cache-Control").is("public, max-age=3600");
	}

	@Test void c02_builderFormJoinedCommaSeparated() throws Exception {
		var a = MockRestClient.buildLax(C.class);
		// Builder emits a stable order: cacheability → boolean directives → numeric directives → extensions.
		a.get("/fromBuilder").run().assertStatus(200)
			.assertHeader("Cache-Control").is("public, must-revalidate, max-age=3600");
	}

	@Test void c03_builderPrivateNoStore() throws Exception {
		var a = MockRestClient.buildLax(C.class);
		a.get("/privateNoStore").run().assertStatus(200)
			.assertHeader("Cache-Control").is("private, no-store");
	}

	@Test void c04_builderImmutableMaxAge() throws Exception {
		var a = MockRestClient.buildLax(C.class);
		// Booleans before numbers in the stable emission order.
		a.get("/immutable").run().assertStatus(200)
			.assertHeader("Cache-Control").is("public, immutable, max-age=31536000");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// D: CacheControlBuilder unit checks — directives, ordering, error paths
	//-----------------------------------------------------------------------------------------------------------------

	@Test void d01_emptyBuilderEmitsEmptyString() {
		assertEquals("", CacheControlBuilder.create().build());
	}

	@Test void d02_publicOverridesPrivate() {
		var s = CacheControlBuilder.create().privateCache().publicCache().build();
		assertEquals("public", s);
	}

	@Test void d03_privateOverridesPublic() {
		var s = CacheControlBuilder.create().publicCache().privateCache().build();
		assertEquals("private", s);
	}

	@Test void d04_allBooleanDirectives() {
		// Stable emission order: public/private, then boolean directives in source order.
		var s = CacheControlBuilder.create()
			.publicCache()
			.noCache().noStore().noTransform()
			.mustRevalidate().proxyRevalidate()
			.immutable()
			.build();
		assertEquals("public, no-cache, no-store, no-transform, must-revalidate, proxy-revalidate, immutable", s);
	}

	@Test void d05_maxAgeAndSMaxAge() {
		var s = CacheControlBuilder.create()
			.maxAge(60)
			.sMaxAge(120)
			.build();
		assertEquals("max-age=60, s-maxage=120", s);
	}

	@Test void d06_maxAgeFromDuration() {
		var s = CacheControlBuilder.create()
			.maxAge(Duration.ofMinutes(10))
			.sMaxAge(Duration.ofMinutes(15))
			.build();
		assertEquals("max-age=600, s-maxage=900", s);
	}

	@Test void d07_staleWhileRevalidateAndStaleIfError() {
		var s = CacheControlBuilder.create()
			.staleWhileRevalidate(30)
			.staleIfError(60)
			.build();
		assertEquals("stale-while-revalidate=30, stale-if-error=60", s);
	}

	@Test void d08_extensionAppendedAtEnd() {
		var s = CacheControlBuilder.create()
			.publicCache().maxAge(3600)
			.extension("community=\"UCI\"")
			.build();
		assertEquals("public, max-age=3600, community=\"UCI\"", s);
	}

	@Test void d09_extensionRejectsBlank() {
		assertThrows(IllegalArgumentException.class, () -> CacheControlBuilder.create().extension(""));
		assertThrows(IllegalArgumentException.class, () -> CacheControlBuilder.create().extension("   "));
	}

	@Test void d10_extensionRejectsNull() {
		assertThrows(IllegalArgumentException.class, () -> CacheControlBuilder.create().extension(null));
	}

	@Test void d11_negativeMaxAgeRejected() {
		assertThrows(IllegalArgumentException.class, () -> CacheControlBuilder.create().maxAge(-1));
		assertThrows(IllegalArgumentException.class, () -> CacheControlBuilder.create().sMaxAge(-1));
		assertThrows(IllegalArgumentException.class, () -> CacheControlBuilder.create().staleWhileRevalidate(-1));
		assertThrows(IllegalArgumentException.class, () -> CacheControlBuilder.create().staleIfError(-1));
	}

	@Test void d12_nullDurationRejected() {
		assertThrows(IllegalArgumentException.class, () -> CacheControlBuilder.create().maxAge((Duration)null));
		assertThrows(IllegalArgumentException.class, () -> CacheControlBuilder.create().sMaxAge((Duration)null));
	}

	@Test void d13_toHeaderReturnsCacheControlBean() {
		var h = CacheControlBuilder.create().publicCache().maxAge(60).toHeader();
		assertNotNull(h);
		assertEquals("Cache-Control", h.getName());
		assertEquals("public, max-age=60", h.getValue());
	}

	@Test void d14_toStringMatchesBuild() {
		var b = CacheControlBuilder.create().publicCache().maxAge(60);
		assertEquals(b.build(), b.toString());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// E: Null-argument guards on RestResponse helpers (cover the assertArgNotNull branches).
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class E {
		@RestGet("/etagNullString") public String etagNullString(RestResponse res) {
			res.eTag((String)null); return "ok";
		}
		@RestGet("/etagNullTyped") public String etagNullTyped(RestResponse res) {
			res.eTag((EntityTag)null); return "ok";
		}
		@RestGet("/lmNullInstant") public String lmNullInstant(RestResponse res) {
			res.lastModified((Instant)null); return "ok";
		}
		@RestGet("/lmNullZdt") public String lmNullZdt(RestResponse res) {
			res.lastModified((ZonedDateTime)null); return "ok";
		}
		@RestGet("/ccNullString") public String ccNullString(RestResponse res) {
			res.cacheControl((String)null); return "ok";
		}
		@RestGet("/ccNullBuilder") public String ccNullBuilder(RestResponse res) {
			res.cacheControl((CacheControlBuilder)null); return "ok";
		}
	}

	@Test void e01_nullEtagStringYields500() throws Exception {
		// EntityTag.of(null) returns null, then assertArgNotNull triggers IllegalArgumentException → 500.
		var a = MockRestClient.buildLax(E.class);
		a.get("/etagNullString").run().assertStatus(500);
	}

	@Test void e02_nullEtagTypedYields500() throws Exception {
		var a = MockRestClient.buildLax(E.class);
		a.get("/etagNullTyped").run().assertStatus(500);
	}

	@Test void e03_nullInstantYields500() throws Exception {
		var a = MockRestClient.buildLax(E.class);
		a.get("/lmNullInstant").run().assertStatus(500);
	}

	@Test void e04_nullZdtYields500() throws Exception {
		var a = MockRestClient.buildLax(E.class);
		a.get("/lmNullZdt").run().assertStatus(500);
	}

	@Test void e05_nullCacheControlStringYields500() throws Exception {
		var a = MockRestClient.buildLax(E.class);
		a.get("/ccNullString").run().assertStatus(500);
	}

	@Test void e06_nullCacheControlBuilderYields500() throws Exception {
		var a = MockRestClient.buildLax(E.class);
		a.get("/ccNullBuilder").run().assertStatus(500);
	}
}
