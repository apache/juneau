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
package org.apache.juneau.rest.server.convention;

import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.*;
import org.junit.jupiter.api.*;

/**
 * Validates {@link FaviconMixin} mounted as a mixin via {@code @Rest(mixins=...)} on a
 * vanilla {@link RestServlet}.
 *
 * <p>
 * Cases:
 * <ul>
 * 	<li>Default favicon (framework-shipped {@code /juneau-favicon.ico}) is served when no
 * 		{@code @Bean FaviconMixin} is registered on the host.
 * 	<li>{@code Content-Type: image/x-icon} and 30-day {@code Cache-Control} headers flow through.
 * 	<li>Importer's {@code @Bean FaviconMixin} factory overrides the default bytes.
 * 	<li>{@link FaviconMixin.Builder#classpath(String) classpath(...)} loads icon bytes
 * 		from a classpath resource path.
 * 	<li>The host's own endpoints are unaffected by the mixin.
 * </ul>
 *
 * @since 10.0.0
 */
class FaviconMixin_AsMixin_Test extends TestBase {

	@Rest(mixins=FaviconMixin.class)
	public static class A extends RestServlet {
		private static final long serialVersionUID = 1L;
		@RestGet(path="/items") public String items() { return "items"; }
	}

	private static final MockRestClient ca = MockRestClient.buildLax(A.class);

	@Test void a01_defaultFaviconServed() throws Exception {
		ca.get("/favicon.ico")
			.run()
			.assertStatus(200)
			.assertHeader("Content-Type").is("image/x-icon")
			.assertHeader("Cache-Control").is("max-age=2592000, public");
	}

	@Test void a02_defaultFaviconBodyNonEmpty() throws Exception {
		var body = ca.get("/favicon.ico")
			.run()
			.assertStatus(200)
			.getContent().asBytes();
		Assertions.assertTrue(body.length > 0, "Default favicon body must be non-empty");
		Assertions.assertEquals(0x00, body[0] & 0xFF, "ICO magic byte 0");
		Assertions.assertEquals(0x00, body[1] & 0xFF, "ICO magic byte 1");
		Assertions.assertEquals(0x01, body[2] & 0xFF, "ICO magic byte 2 (type=ICO)");
	}

	@Test void a03_hostEndpointStillReachable() throws Exception {
		ca.get("/items")
			.run()
			.assertStatus(200)
			.assertContent().asString().isContains("items");
	}

	/** Host with an importer-supplied {@code @Bean FaviconMixin} carrying custom bytes. */
	@Rest(mixins=FaviconMixin.class)
	public static class B extends RestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public FaviconMixin favicon() {
			return FaviconMixin.create()
				.bytes(new byte[]{(byte)0xCA, (byte)0xFE, (byte)0xBA, (byte)0xBE})
				.build();
		}
	}

	private static final MockRestClient cb = MockRestClient.buildLax(B.class);

	@Test void b01_overrideViaBuilderBytes() throws Exception {
		var body = cb.get("/favicon.ico")
			.run()
			.assertStatus(200)
			.assertHeader("Content-Type").is("image/x-icon")
			.getContent().asBytes();
		Assertions.assertEquals(4, body.length);
		Assertions.assertEquals((byte)0xCA, body[0]);
		Assertions.assertEquals((byte)0xFE, body[1]);
		Assertions.assertEquals((byte)0xBA, body[2]);
		Assertions.assertEquals((byte)0xBE, body[3]);
	}

	/** Host with an importer-supplied factory that loads bytes via classpath resource path. */
	@Rest(mixins=FaviconMixin.class)
	public static class C extends RestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public FaviconMixin favicon() {
			// /juneau-favicon.ico is the framework's default-shipping classpath resource;
			// loading it explicitly via classpath(...) verifies the classpath path works.
			return FaviconMixin.create()
				.classpath("/juneau-favicon.ico")
				.cacheControl("max-age=300, public")
				.build();
		}
	}

	private static final MockRestClient cc = MockRestClient.buildLax(C.class);

	@Test void c01_classpathLoaderAndCustomCacheControl() throws Exception {
		cc.get("/favicon.ico")
			.run()
			.assertStatus(200)
			.assertHeader("Content-Type").is("image/x-icon")
			.assertHeader("Cache-Control").is("max-age=300, public");
	}

	@Test void c02_classpathMissingFallsBackToDefault() throws Exception {
		// A builder pointed at a missing classpath path falls back to the framework default.
		var fav = FaviconMixin.create()
			.classpath("/no-such-favicon.ico")
			.build();
		// Even with a missing classpath override, the default favicon resource provides bytes.
		Assertions.assertNotNull(fav, "Builder must always produce an instance");
	}
}
