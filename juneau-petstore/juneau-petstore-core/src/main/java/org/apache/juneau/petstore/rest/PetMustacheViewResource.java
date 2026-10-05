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

import org.apache.juneau.commons.inject.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.petstore.service.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.*;
import org.apache.juneau.rest.server.view.*;
import org.apache.juneau.rest.server.view.mustache.*;

/**
 * Petstore Mustache view demo.
 *
 * <p>
 * Demonstrates rendering a templated HTML pet detail page through the {@code juneau-rest-server-view-mustache}
 * bridge.  Lives in {@code juneau-petstore-core} so both the Jetty and Spring Boot deployments inherit it
 * unchanged — Mustache is a pure-classpath engine and renders identically under either embedded container.
 *
 * <p>
 * The {@code @RestGet("/pets/{id}/view")} method returns {@link MustacheView}; the configured
 * {@link MustacheViewRenderer} (registered via {@code @Rest(responseProcessors=...)}) detects the return type,
 * resolves the {@code pet.mustache} template under {@code petstore-templates/} on the classpath, and renders
 * it directly to the response writer.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/MustacheViewSupport">Mustache View Support</a>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauPetstore">juneau-petstore</a>
 * </ul>
 */
@Rest(
	path="/pet-views/mustache",
	title="Pet detail (Mustache)",
	description="Renders a pet detail page through the Mustache view-rendering bridge.",
	responseProcessors={MustacheViewRenderer.class}
)
@SuppressWarnings({
	"java:S110" // Inheritance depth acceptable for example/demo code
})
public class PetMustacheViewResource extends BasicRestServlet {

	private static final long serialVersionUID = 1L;

	/**
	 * The shared store, injected from the parent's bean store (the runner registers one seeded {@code PetStore}).
	 * When this resource is mounted standalone (unit tests), {@link #store()} falls back to a classic-rows store.
	 */
	@SuppressWarnings({
		"java:S2226" // the field is set by the framework's @Bean injection after construction, so it cannot be final.
	})
	@Bean
	private transient PetStore store;

	@SuppressWarnings({
		"java:S2654" // synchronized guards the lazy standalone fallback, racing against the injected value.
	})
	private synchronized PetStore store() {
		if (store == null)
			store = new PetStore();
		return store;
	}

	/**
	 * Provides a {@link MustacheMixin} configured to resolve templates under {@code petstore-templates/} with
	 * an implicit {@code .mustache} suffix — so {@code MustacheView.of("pet")} resolves to the classpath
	 * resource {@code petstore-templates/pet.mustache}.
	 *
	 * @return The mustache mixin bean.
	 */
	@Bean
	public MustacheMixin mustache() {
		return MustacheMixin.create()
			.basePath("/petstore-templates/")
			.templateSuffix(".mustache")
			.build();
	}

	/**
	 * GET /pets/{id}/view — renders a pet detail HTML page through Mustache.
	 *
	 * @param id The pet ID.
	 * @return A {@link MustacheView} bound to the {@code pet} template.
	 * @throws NotFound If no pet with the given ID exists.
	 */
	@RestGet("/pets/{id}/view")
	public View viewPet(@Path("id") long id) {
		var pet = store().getPet(id);
		if (pet == null)
			throw new NotFound("Pet not found: id=%s", id);
		return MustacheView.of("pet").attr("pet", pet);
	}
}
