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

import static org.apache.juneau.bean.html5.HtmlBuilder.*;

import java.util.*;

import org.apache.juneau.bean.html5.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.petstore.service.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.beans.*;
import org.apache.juneau.rest.server.servlet.*;

/**
 * Petstore-flavored demo of Juneau's HTML5 bean-builder DSL.
 *
 * <p>
 * Ports the deleted {@code juneau-examples-rest} {@code HtmlBeansResource} demo, swapping its generic
 * div/form/table samples for hand-built HTML5 fragments rendered from live petstore data — a "pet card"
 * {@link Div} and an all-pets {@link Table} — using {@link org.apache.juneau.bean.html5.HtmlBuilder} static
 * factory methods instead of relying on the framework's automatic HTML-doc view.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/HtmlBeans">Using with HTML Beans</a>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauPetstore">juneau-petstore</a>
 * </ul>
 */
@Rest(
	path="/petstore-html",
	title="Petstore HTML5 bean builder",
	description="Hand-built HTML5 div/table fragments rendered from live petstore data."
)
@SuppressWarnings({
	"java:S110" // Inheritance depth acceptable for example/demo code
})
public class PetHtmlResource extends BasicRestServlet {

	private static final long serialVersionUID = 1L;

	private final transient PetStore store = new PetStore();

	/**
	 * Lists the child endpoints.
	 *
	 * @return Descriptive links to the child endpoints.
	 */
	@RestGet("/")
	public ResourceDescriptions getChildDescriptions() {
		return ResourceDescriptions
			.create()
			.append("table", "All pets, rendered as a hand-built HTML5 table")
			.append("card/1", "A single pet, rendered as a hand-built HTML5 'pet card' div");
	}

	/**
	 * Renders a single pet as a hand-built HTML5 "pet card" div.
	 *
	 * @param id The pet ID.
	 * @return A div containing the pet's name, species, price, and status.
	 * @throws NotFound If no pet with the given ID exists.
	 */
	@RestGet("/card/{id}")
	public Div getPetCard(@Path("id") long id) {
		var pet = store.getPet(id);
		if (pet == null)
			throw new NotFound("Pet not found: id={0}", id);
		return div(
			p(b(pet.getName())),
			p("Species: " + pet.getSpecies()),
			p("Price: $" + pet.getPrice()),
			p("Status: " + pet.getStatus())
		);
	}

	/**
	 * Renders all pets as a hand-built HTML5 table.
	 *
	 * @return A table with one row per pet (name, species, price, status).
	 */
	@RestGet("/table")
	public Table getPetTable() {
		var rows = new ArrayList<>();
		rows.add(tr(th("Name"), th("Species"), th("Price"), th("Status")));
		for (var pet : store.getPets())
			rows.add(tr(td(pet.getName()), td(pet.getSpecies()), td(pet.getPrice()), td(pet.getStatus())));
		return table(rows.toArray());
	}
}
