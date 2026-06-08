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
import org.apache.juneau.rest.mock.classic.*;
import org.junit.jupiter.api.*;

/**
 * Validates the {@link FaviconServlet} servlet flavor mounted directly as a top-level servlet.
 *
 * <p>
 * The servlet pins its op at {@code /*} and delegates to a shared flavor-neutral {@link FaviconProvider}
 * worker, so this mirrors {@link FaviconMixin_AsMixin_Test} but exercises the standalone-servlet
 * deployment.
 * Cases:
 * <ul>
 * 	<li>Default favicon (framework-shipped {@code /juneau-favicon.ico}) is served with
 * 		{@code Content-Type: image/x-icon} and the 30-day {@code Cache-Control} header.
 * 	<li>The served body is a non-empty ICO payload.
 * 	<li>A builder-configured delegate (custom bytes + cache-control) flows through the servlet.
 * </ul>
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"resource" // Static MockRestClient fields are a common test pattern; resources are managed by the mock framework.
})
class FaviconServlet_Test extends TestBase {

	private static final MockRestClient c = MockRestClient.buildLax(FaviconServlet.class);

	@Test void a01_defaultFaviconServed() throws Exception {
		c.get("/favicon.ico")
			.run()
			.assertStatus(200)
			.assertHeader("Content-Type").is("image/x-icon")
			.assertHeader("Cache-Control").is("max-age=2592000, public");
	}

	@Test void a02_defaultFaviconBodyIsIco() throws Exception {
		var body = c.get("/favicon.ico")
			.run()
			.assertStatus(200)
			.getContent().asBytes();
		Assertions.assertTrue(body.length > 0, "Default favicon body must be non-empty");
		Assertions.assertEquals(0x00, body[0] & 0xFF, "ICO magic byte 0");
		Assertions.assertEquals(0x00, body[1] & 0xFF, "ICO magic byte 1");
		Assertions.assertEquals(0x01, body[2] & 0xFF, "ICO magic byte 2 (type=ICO)");
	}

	/** Servlet subclass supplying a builder-configured worker (constructor-injected). */
	public static class B extends FaviconServlet {
		private static final long serialVersionUID = 1L;
		public B() {
			super(FaviconProvider.create()
				.bytes(new byte[]{(byte)0xCA, (byte)0xFE, (byte)0xBA, (byte)0xBE})
				.cacheControl("max-age=300, public")
				.build());
		}
	}

	private static final MockRestClient cb = MockRestClient.buildLax(B.class);

	@Test void b01_customDelegateBytesAndCacheControl() throws Exception {
		var body = cb.get("/favicon.ico")
			.run()
			.assertStatus(200)
			.assertHeader("Content-Type").is("image/x-icon")
			.assertHeader("Cache-Control").is("max-age=300, public")
			.getContent().asBytes();
		Assertions.assertEquals(4, body.length);
		Assertions.assertEquals((byte)0xCA, body[0]);
		Assertions.assertEquals((byte)0xFE, body[1]);
		Assertions.assertEquals((byte)0xBA, body[2]);
		Assertions.assertEquals((byte)0xBE, body[3]);
	}
}
