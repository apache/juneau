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

import org.apache.juneau.commons.inject.Bean;
import org.apache.juneau.http.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.petstore.service.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.*;
import org.apache.juneau.rest.server.view.*;
import org.apache.juneau.rest.server.view.freemarker.*;

/**
 * Petstore FreeMarker view demo.
 *
 * <p>
 * Pure-classpath analogue of {@link PetMustacheViewResource}: the FreeMarker bridge is engine-agnostic and
 * renders identically under both the Jetty and Spring Boot deployments.  Resolves
 * {@code petstore-templates/pet.ftlh} via the configured {@link FreemarkerMixin}.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/FreemarkerViewSupport">FreeMarker View Support</a>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauPetstoreOverview">juneau-petstore</a>
 * </ul>
 */
@Rest(
	path="/pet-views/freemarker",
	title="Pet detail (FreeMarker)",
	description="Renders a pet detail page through the FreeMarker view-rendering bridge.",
	responseProcessors={FreemarkerViewRenderer.class}
)
@SuppressWarnings({
	"java:S110" // Inheritance depth acceptable for example/demo code
})
public class PetFreemarkerViewResource extends BasicRestServlet {

	private static final long serialVersionUID = 1L;

	private final PetStore store = new PetStore();

	/**
	 * Provides a {@link FreemarkerMixin} configured to resolve templates under {@code petstore-templates/}.
	 *
	 * @return The freemarker mixin bean.
	 */
	@Bean
	public FreemarkerMixin freemarker() {
		return FreemarkerMixin.create()
			.basePath("/petstore-templates/")
			.build();
	}

	/**
	 * GET /pets/{id}/view — renders a pet detail HTML page through FreeMarker.
	 *
	 * @param id The pet ID.
	 * @return A {@link FreemarkerView} bound to the {@code pet.ftlh} template.
	 * @throws NotFound If no pet with the given ID exists.
	 */
	@RestGet("/pets/{id}/view")
	public View viewPet(@Path("id") long id) {
		var pet = store.getPet(id);
		if (pet == null)
			throw new NotFound("Pet not found: id={0}", id);
		return FreemarkerView.of("pet.ftlh").attr("pet", pet);
	}
}
