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
package org.apache.juneau.petstore.rest;

import java.security.*;
import java.util.*;

import org.apache.juneau.commons.inject.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.petstore.auth.*;
import org.apache.juneau.petstore.dto.*;
import org.apache.juneau.petstore.service.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.auth.*;
import org.apache.juneau.rest.server.guard.*;
import org.apache.juneau.rest.server.servlet.*;

/**
 * Secure analogue of {@link PetStoreResource} demonstrating Juneau's {@link BearerTokenGuard} +
 * {@link StubBearerTokenValidator} pattern.
 *
 * <p>
 * Mounted at {@code /petstore-secure/*} and gated by a {@link BearerTokenGuard} that delegates to a
 * {@link StubBearerTokenValidator}.  Any request lacking a valid {@code Authorization: Bearer <token>} header
 * receives {@code 401} with a {@code WWW-Authenticate: Bearer realm="petstore"} challenge.  On success, the
 * resolved {@link Principal} is stashed on the request and surfaces through {@link Auth @Auth Principal} op
 * parameters — see {@link #whoami(Principal)}.
 *
 * <p>
 * <b>Why guard rather than chain?</b>  Juneau also ships an {@code AuthFilterChain} for composing multiple
 * mechanisms (JWT bearer, API key, OAuth, SAML, OIDC).  The chain is fail-open by design — a request that
 * matches the chain's path pattern but presents no recognised credentials is passed through unchanged so a
 * downstream guard can decide.  For a single-mechanism demo, the op-level {@code BearerTokenGuard} (fail-closed
 * by default) is the right surface.  In a real multi-mechanism deployment you'd compose
 * {@code AuthFilterChain.create(...).append(filterA, "/secure/*").append(filterB, "/secure/*").build()} and
 * mount it via {@code @Bean AuthFilterChain} (auto-mounted by {@code JettyServerComponent}) or a Spring
 * {@code FilterRegistrationBean}, then add this guard for fail-closed enforcement.
 *
 * <p>
 * The CRUD surface is intentionally a thin subset of {@link PetStoreResource} — the demo is the auth gate, not
 * the surface.  If you need richer auth integration (role-based guards, multi-mechanism chains, server-side
 * OAuth2 / SAML / OIDC-RP), consult the {@code juneau-rest-server-auth-*} modules.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link BearerTokenGuard}
 * 	<li class='jc'>{@link AuthFilterChain}
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/AuthGuards">AuthN Guards</a>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/AuthFilterFramework">AuthN Filter Framework</a>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauPetstoreOverview">juneau-petstore</a>
 * </ul>
 */
@Rest(
	path="/petstore-secure",
	title="Petstore (secure)",
	description="Auth-gated subset of the petstore CRUD surface; bearer-token-protected via AuthFilterChain."
)
@SuppressWarnings({
	"java:S110" // Inheritance depth acceptable for example/demo code
})
public class PetSecureResource extends BasicRestServlet {

	private static final long serialVersionUID = 1L;

	private final PetStore store = new PetStore();

	/**
	 * Provides the {@link RestGuardList} that gates every op on this resource with a
	 * {@link BearerTokenGuard} — fail-closed bearer-token enforcement.
	 *
	 * @param bs The bean store (Juneau supplies this automatically).
	 * @return The guard list applied to all ops on this resource.
	 */
	@Bean
	public RestGuardList guards(BeanStore bs) {
		return RestGuardList.create(bs)
			.append(
				BearerTokenGuard.create()
					.realm("petstore")
					.validator(new StubBearerTokenValidator())
					.build())
			.build();
	}

	/**
	 * Lists all pets.  Accessible only with a valid bearer token.
	 *
	 * @return All pets.
	 */
	@RestGet("/pets")
	public Collection<Pet> getPets() {
		return store.getPets();
	}

	/**
	 * Retrieves a pet by ID.  Accessible only with a valid bearer token.
	 *
	 * @param id The pet ID.
	 * @return The pet.
	 * @throws NotFound If no pet with the given ID exists.
	 */
	@RestGet("/pets/{id}")
	public Pet getPet(@Path("id") long id) {
		var pet = store.getPet(id);
		if (pet == null)
			throw new NotFound("Pet not found: id={0}", id);
		return pet;
	}

	/**
	 * Returns the name of the authenticated principal — the simplest possible "auth worked" probe.
	 *
	 * @param caller The principal stashed on the request by the {@code AuthFilterChain}.  Never {@code null} —
	 * 	if no caller is authenticated the chain rejects with {@code 401} before this op runs.
	 * @return A small map carrying the principal's display name.
	 */
	@RestGet("/whoami")
	public Map<String,String> whoami(@Auth Principal caller) {
		return Map.of("name", caller.getName());
	}
}
