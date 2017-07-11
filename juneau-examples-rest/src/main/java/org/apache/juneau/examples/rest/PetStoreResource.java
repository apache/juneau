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
			StyleMenuItem.class
		},
		links={
			"up: request:/..",
			"options: servlet:/?method=OPTIONS",
			"$W{ContentTypeMenuItem}",
			"$W{StyleMenuItem}",
			"source: $C{Source/gitHub}/org/apache/juneau/examples/rest/PetStoreResource.java"
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

	@Override /* Servlet */
	public synchronized void init(RestConfig config) throws Exception {
		// Load our database from a local JSON file.
		petDB = JsonParser.DEFAULT.parse(getClass().getResourceAsStream("PetStore.json"), LinkedHashMap.class, Integer.class, Pet.class);
		super.init(config);
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
				"up: request:/..",
				"options: servlet:/?method=OPTIONS",
				"$W{QueryMenuItem}",
				"$W{ContentTypeMenuItem}",
				"$W{StyleMenuItem}",
				"source: $C{Source/gitHub}/org/apache/juneau/examples/rest/PetStoreResource.java"
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

		@BeanProperty(swap=DateSwap.RFC2822D.class)  // Renders dates in RFC2822 format.
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
}

