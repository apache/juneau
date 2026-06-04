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

import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.servlet.*;
import org.junit.jupiter.api.*;

/**
 * Validates {@link WellKnownMixin} mounted as a mixin via {@code @Rest(mixins=...)} on a
 * vanilla {@link RestServlet}.
 *
 * <p>
 * Cases:
 * <ul>
 * 	<li>{@code /.well-known/security.txt} returns {@code 404 Not Found} when no body is configured
 * 		(per RFC 9116 default — no placeholder).
 * 	<li>Importer's {@code @Bean WellKnownMixin} factory drives the body content.
 * 	<li>{@code Content-Type: text/plain; charset=UTF-8} when a body is configured.
 * </ul>
 *
 * @since 10.0.0
 */
class WellKnownMixin_AsMixin_Test extends TestBase {

	@Rest(mixins=WellKnownMixin.class)
	public static class A extends RestServlet {
		private static final long serialVersionUID = 1L;
		@RestGet(path="/items") public String items() { return "items"; }
	}

	private static final MockRestClient ca = MockRestClient.buildLax(A.class);

	@Test void a01_noBodyConfiguredReturns404() throws Exception {
		ca.get("/.well-known/security.txt").run().assertStatus(404);
	}

	@Test void a02_hostEndpointStillReachable() throws Exception {
		ca.get("/items").run().assertStatus(200).assertContent().asString().isContains("items");
	}

	/** Host that configures a security.txt body via @Bean factory. */
	@Rest(mixins=WellKnownMixin.class)
	public static class B extends RestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public WellKnownMixin wellKnown() {
			return WellKnownMixin.create()
				.securityTxt("Contact: security@example.com\nExpires: 2027-01-01T00:00:00Z\n")
				.build();
		}
	}

	private static final MockRestClient cb = MockRestClient.buildLax(B.class);

	@Test void b01_configuredBodyIsServed() throws Exception {
		cb.get("/.well-known/security.txt")
			.run()
			.assertStatus(200)
			.assertHeader("Content-Type").isContains("text/plain")
			.assertContent().asString().is("Contact: security@example.com\nExpires: 2027-01-01T00:00:00Z\n");
	}
}
