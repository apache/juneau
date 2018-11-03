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
package org.apache.juneau.examples.rest.petstore.dto;

import static javax.persistence.EnumType.*;

import java.util.*;

import javax.persistence.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.html.*;
import org.apache.juneau.html.annotation.*;
import org.apache.juneau.jsonschema.annotation.*;
import org.apache.juneau.serializer.*;

/**
 * Pet bean.
 */
@Bean(typeName="Pet", fluentSetters=true, properties="id,species,name,tags,price,status,photo")
@Entity(name="PetstorePet")
public class Pet {

	@Column @Id @GeneratedValue
	@Schema(description="Unique identifier for this pet.")
	@Html(link="servlet:/pet/{id}")
	private long id;

	@Column(length=50)
	@Schema(description="Pet name.", minLength=3, maxLength=50)
	private String name;

	@Column
	@Schema(description="Price of pet.", maximum="999.99")
	@Html(render=PriceRender.class)
	private float price;

	@Column
	@Schema(description="Pet species.")
	private Species species;

	@ElementCollection @OrderColumn
	@Schema(description="Pet attributes.", example="friendly,smart")
	private List<String> tags;

	@Column @Enumerated(STRING)
	@Schema(description="Pet species.")
	private PetStatus status;

	@Column
	@Schema(description="Photo URL.")
	@URI
	private String photo;

	public Pet apply(CreatePet x) {
		this.name = x.getName();
		this.price = x.getPrice();
		this.species = x.getSpecies();
		this.tags = x.getTags() == null ? null : Arrays.asList(x.getTags());
		this.photo = x.getPhoto();
		return this;
	}

	public Pet apply(UpdatePet x) {
		this.id = x.getId();
		this.name = x.getName();
		this.price = x.getPrice();
		this.species = x.getSpecies();
		this.tags = Arrays.asList(x.getTags());
		this.status = x.getStatus();
		this.photo = x.getPhoto();
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Bean properties
	//-----------------------------------------------------------------------------------------------------------------

	public long getId() {
		return id;
	}

	public Pet id(long id) {
		this.id = id;
		return this;
	}

	public String getName() {
		return name;
	}

	public Pet name(String name) {
		this.name = name;
		return this;
	}

	public float getPrice() {
		return price;
	}

	public Pet price(float price) {
		this.price = price;
		return this;
	}

	public Species getSpecies() {
		return species;
	}

	public Pet species(Species species) {
		this.species = species;
		return this;
	}

	public List<String> getTags() {
		return tags;
	}

	public Pet tags(List<String> tags) {
		this.tags = tags;
		return this;
	}

	public Pet tags(String...tags) {
		this.tags = Arrays.asList(tags);
		return this;
	}

	public PetStatus getStatus() {
		return status;
	}

	public Pet status(PetStatus status) {
		this.status = status;
		return this;
	}

	public String getPhoto() {
		return photo;
	}

	public Pet photo(String photo) {
		this.photo = photo;
		return this;
	}

	public boolean hasStatus(PetStatus...statuses) {
		for (PetStatus status : statuses)
			if (this.status == status)
				return true;
		return false;
	}

	public boolean hasTag(String...tags) {
		for (String tag : tags)
			for (String t : this.tags)
				if (t.equals(tag))
					return true;
		return false;
	}

	public java.net.URI getEdit() {
		return java.net.URI.create("servlet:/pet/edit/{id}");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * This shows an example generated from a static method.
	 */
	@Example
	public static Pet example() {
		return new Pet()
			.id(123)
			.species(Species.DOG)
			.name("Doggie")
			.tags("friendly","smart")
			.status(PetStatus.AVAILABLE);
	}


	public static final class PriceRender extends HtmlRender<Float> {
		@Override
		public Object getContent(SerializerSession session, Float value) {
			return value == null ? null : String.format("$%.2f", value);
		}
	}
}
