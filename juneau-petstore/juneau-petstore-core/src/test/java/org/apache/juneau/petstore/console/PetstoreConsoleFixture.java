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
package org.apache.juneau.petstore.console;

import org.apache.juneau.commons.inject.*;
import org.apache.juneau.petstore.console.data.*;
import org.apache.juneau.petstore.rest.*;
import org.apache.juneau.petstore.service.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.*;

/**
 * Test host mirroring the runners: the {@code /petstore} API plus the {@code /console} tree over one seeded store.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jk>var</jk> <jv>html</jv> = PetstoreConsoleFixture.<jsm>page</jsm>(PetstoreConsoleFixture.<jsm>client</jsm>(), <js>"/console/store"</js>);
 * </p>
 */
@SuppressWarnings({
	"resource" // client()/rawClient() return Closeables owned by the caller, and page() consumes its response; Eclipse JDT @Owning warning is by design.
})
public final class PetstoreConsoleFixture {

	/** The host group. */
	@Rest(children={PetStoreResource.class, PetstoreConsoleResource.class})
	public static class Host extends BasicRestServletGroup {
		private static final long serialVersionUID = 1L;

		/** @return The seeded store shared by every child. */
		@Bean public PetStore petStore() {
			return PetstoreSeed.create().populate(new PetStore(PetstoreSeed.DEFAULT_CLOCK));
		}
	}

	private PetstoreConsoleFixture() {}

	/**
	 * @return A lax client over a host.  Contexts may be cached per class, so mutating tests must use ids they create.
	 */
	public static MockRestClient client() {
		return MockRestClient.create(Host.class).noTrace().ignoreErrors().build();
	}

	/**
	 * @return Like {@link #client()} but without redirect following, so 3xx responses can be asserted.
	 */
	public static MockRestClient rawClient() {
		return MockRestClient.create(Host.class).noTrace().ignoreErrors().disableRedirectHandling().build();
	}

	/**
	 * @param c The client.
	 * @param path The console path, e.g. {@code "/console/store"}.
	 * @return The rendered page HTML, status asserted 200.
	 * @throws Exception On request failure.
	 */
	public static String page(MockRestClient c, String path) throws Exception {
		return c.get(path).accept("text/html").run().assertStatus(200).getContent().asString();
	}
}
