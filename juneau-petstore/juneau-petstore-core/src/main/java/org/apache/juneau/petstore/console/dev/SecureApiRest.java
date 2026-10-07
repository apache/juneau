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
package org.apache.juneau.petstore.console.dev;

import java.security.*;
import java.util.*;

import org.apache.juneau.commons.inject.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.petstore.auth.*;
import org.apache.juneau.petstore.console.*;
import org.apache.juneau.petstore.dto.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.auth.*;
import org.apache.juneau.rest.server.guard.*;

/**
 * The bearer-token protected API behind the Secure page.
 *
 * <p>
 * Every op is gated by a fail-closed {@link BearerTokenGuard} backed by {@link StubBearerTokenValidator}: no token
 * or an unknown token is a {@code 401} with a {@code WWW-Authenticate: Bearer realm="petstore"} challenge.  On
 * success the resolved {@link Principal} reaches ops as an {@link Auth @Auth} parameter.
 *
 * <p>
 * <b>Why a guard rather than a filter chain?</b>  {@code AuthFilterChain} composes several mechanisms and is
 * fail-open by design, so a downstream guard decides.  With one mechanism, the op-level guard is the whole story.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// One bean gates every op on the resource.</jc>
 * 	<ja>@Bean</ja>
 * 	<jk>public</jk> RestGuardList guards(BeanStore <jv>bs</jv>) {
 * 		<jk>return</jk> RestGuardList.<jsm>create</jsm>(<jv>bs</jv>)
 * 			.append(BearerTokenGuard.<jsm>create</jsm>().realm(<js>"petstore"</js>).validator(<jk>new</jk> StubBearerTokenValidator()).build())
 * 			.build();
 * 	}
 *
 * 	<jc>// curl -H 'Authorization: Bearer petstore-user' http://localhost:10000/console/dev/secure/api/pets</jc>
 * </p>
 */
@Rest(path="/api", title="Secure API")
@SuppressWarnings({
	"java:S110" // Inheritance depth comes from the BasicRestServlet hierarchy, not this page.
})
public class SecureApiRest extends PetstoreConsolePage {

	private static final long serialVersionUID = 1L;

	/**
	 * Gates every op on this resource with a bearer-token guard.
	 *
	 * @param bs The bean store.
	 * @return The guard list.
	 */
	@Bean
	public RestGuardList guards(BeanStore bs) {
		return RestGuardList.create(bs)
			.append(BearerTokenGuard.create().realm("petstore").validator(new StubBearerTokenValidator()).build())
			.build();
	}

	/** @return All pets. */
	@RestGet(path="/pets")
	public Collection<Pet> pets() {
		return store().getPets();
	}

	/**
	 * @param id The pet id.
	 * @return The pet.
	 * @throws NotFound If there is no such pet.
	 */
	@RestGet(path="/pets/{id}")
	public Pet pet(@Path("id") long id) {
		var pet = store().getPet(id);
		if (pet == null)
			throw new NotFound("Unknown pet '%s'", id);
		return pet;
	}

	/**
	 * @param caller The authenticated principal (never <jk>null</jk>: the guard rejects anonymous calls first).
	 * @return {@code {name: <principal>}}.
	 */
	@RestGet(path="/whoami")
	public Map<String,String> whoami(@Auth Principal caller) {
		return Map.of("name", caller.getName());
	}
}
