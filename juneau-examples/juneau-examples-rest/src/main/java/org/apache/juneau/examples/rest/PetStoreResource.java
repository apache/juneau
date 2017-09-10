// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau.examples.rest;

import static org.apache.juneau.dto.html5.HtmlBuilder.*;
import static org.apache.juneau.rest.annotation.HookEvent.*;

import java.util.*;
import java.util.Map;

import org.apache.juneau.annotation.*;
import org.apache.juneau.dto.html5.*;
import org.apache.juneau.html.*;
import org.apache.juneau.html.annotation.Html;
import org.apache.juneau.json.*;
import org.apache.juneau.microservice.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.annotation.Body;
import org.apache.juneau.rest.converters.*;
import org.apache.juneau.rest.widget.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.transforms.*;

/**
 * Sample REST resource that renders summary and detail views of the same bean.
 */
@RestResource(
	title="Pet Store",
	description="An example of a typical REST resource where beans are rendered in summary and details views.",
	path="/petstore",
	htmldoc=@HtmlDoc(
		widgets={
			ContentTypeMenuItem.class,
			StyleMenuItem.class,
			PetStoreResource.AddPet.class
		},
		links={
			"up: request:/..",
			"options: servlet:/?method=OPTIONS",
			"$W{ContentTypeMenuItem}",
			"$W{StyleMenuItem}",
			"source: $C{Source/gitHub}/org/apache/juneau/examples/rest/$R{servletClassSimple}.java",
			"$W{AddPet}"
		},
		aside={
			"<div style='max-width:400px' class='text'>",
			"	<p>This page shows a standard REST resource that renders bean summaries and details.</p>",
			"	<p>It shows how different properties can be rendered on the same bean in different views.</p>",
			"	<p>It also shows examples of HtmlRender classes and @BeanProperty(format) annotations.</p>",
			"	<p>It also shows how the Queryable converter and query widget can be used to create searchable interfaces.</p>",
			"</div>"
		}
	)
)
public class PetStoreResource extends ResourceJena {
	private static final long serialVersionUID = 1L;

	// Our database.
	private Map<Integer,Pet> petDB;

	/**
	 * Initializes the pet store database.
	 * 
	 * @param RestConfig config The resource config.
	 * @throws Exception
	 */
	@RestHook(INIT) 
	public void initDatabase(RestConfig config) throws Exception {
		// Load our database from a local JSON file.
		petDB = JsonParser.DEFAULT.parse(getClass().getResourceAsStream("PetStore.json"), LinkedHashMap.class, Integer.class, Pet.class);
	}

	// Exclude the 'breed' and 'getsAlongWith' properties from the beans.
	@RestMethod(
		name="GET",
		path="/",
		summary="The complete list of pets in the store",
		bpx="Pet: breed,getsAlongWith",
		
		// Add our converter for POJO query support.
		converters=Queryable.class,
		
		// Add our menu items in the nav links.
		htmldoc=@HtmlDoc(
			
			widgets={
				QueryMenuItem.class,
				ContentTypeMenuItem.class,
				StyleMenuItem.class
			},

			links={
				"INHERIT",  // Inherit links from class.
				"[2]:$W{QueryMenuItem}"  // Insert QUERY link in position 2.
			}
		)
	)
	public Collection<Pet> getPets() {
		return petDB.values();
	}

	// Shows all bean properties.
	@RestMethod(name="GET", path="/{id}", summary="Pet details")
	public Pet getPet(@Path("id") Integer id) {
		return petDB.get(id);
	}

	@RestMethod(name="POST", path="/")
	public Redirect addPet(@Body Pet pet) throws Exception {
		this.petDB.put(pet.id, pet);
		return new Redirect("servlet:/");
	}
	
	// Our bean class.
	public static class Pet {

		@Html(link="servlet:/{id}")  // Creates a hyperlink in HTML view.
		@NameProperty                // Links the parent key to this bean.
		public int id;

		public String name;
		public Kind kind;
		public String breed;
		public List<Kind> getsAlongWith;

		@BeanProperty(format="$%.2f")  // Renders price in dollars.
		public float price;

		@Swap(DateSwap.ISO8601D.class)  // Renders dates in ISO8601 format.
		public Date birthDate;

		public int getAge() {
			Calendar c = new GregorianCalendar();
			c.setTime(birthDate);
			return new GregorianCalendar().get(Calendar.YEAR) - c.get(Calendar.YEAR);
		}
	}

	@Html(render=KindRender.class)  // Render as an icon in HTML.
	public static enum Kind {
		CAT, DOG, BIRD, FISH, MOUSE, RABBIT, SNAKE
	}

	public static class KindRender extends HtmlRender<Kind> {
		@Override
		public Object getContent(SerializerSession session, Kind value) {
			return new Img().src("servlet:/htdocs/"+value.toString().toLowerCase()+".png");
		}
		@Override
		public String getStyle(SerializerSession session, Kind value) {
			return "background-color:#FDF2E9";
		}
	}
	
	/**
	 * Renders the "ADD" menu item.
	 */
	public class AddPet extends MenuItemWidget {

		@Override
		public String getLabel(RestRequest req) throws Exception {
			return "add";
		}

		@Override
		public Object getContent(RestRequest req) throws Exception {
			return div(
				form().id("form").action("servlet:/").method("POST").children(
					table(
						tr(
							th("ID:"),
							td(input().name("id").type("number").value(getNextAvailableId())),
							td(new Tooltip("(?)", "A unique identifer for the pet.", br(), "Must not conflict with existing IDs"))
						),
						tr(
							th("Name:"),
							td(input().name("name").type("text")),
							td(new Tooltip("(?)", "The name of the pet.", br(), "e.g. 'Fluffy'")) 
						),
						tr(
							th("Kind:"),
							td(
								select().name("kind").children(
									option("CAT"), option("DOG"), option("BIRD"), option("FISH"), option("MOUSE"), option("RABBIT"), option("SNAKE")
								)
							),
							td(new Tooltip("(?)", "The kind of animal.")) 
						),
						tr(
							th("Breed:"),
							td(input().name("breed").type("text")),
							td(new Tooltip("(?)", "The breed of animal.", br(), "Can be any arbitrary text")) 
						),
						tr(
							th("Gets along with:"),
							td(input().name("getsAlongWith").type("text")),
							td(new Tooltip("(?)", "A comma-delimited list of other animal types that this animal gets along with.")) 
						),
						tr(
							th("Price:"),
							td(input().name("price").type("number").placeholder("1.0").step("0.01").min(1).max(100)),
							td(new Tooltip("(?)", "The price to charge for this pet.")) 
						),
						tr(
							th("Birthdate:"),
							td(input().name("birthDate").type("date")),
							td(new Tooltip("(?)", "The pets birthday.")) 
						),
						tr(
							td().colspan(2).style("text-align:right").children(
								button("reset", "Reset"),
								button("button","Cancel").onclick("window.location.href='/'"),
								button("submit", "Submit")
							)
						)
					).style("white-space:nowrap")
				)
			);
		}
	}
	
	private int getNextAvailableId() {
		int i = 100;
		for (Integer k : petDB.keySet())
			i = Math.max(i, k);
		return i+1;
	}
}

