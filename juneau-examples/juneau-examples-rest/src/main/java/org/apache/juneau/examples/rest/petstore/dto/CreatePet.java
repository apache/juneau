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

import org.apache.juneau.annotation.*;
import org.apache.juneau.jsonschema.annotation.*;

/**
 * Bean for creating {@link Pet} objects.
 *
 * <h5 class='section'>See Also:</h5>
 * <ul class='doctree'>
 * 	<li class='extlink'>{@source}
 * </ul>
 */
@Bean(fluentSetters=true, properties="name,price,species,tags,photo")
public class CreatePet {

	@Schema(description="Pet name.", minLength=3, maxLength=50)
	private String name;

	@Schema(description="Price of pet.", maximum="999.99")
	private float price;

	@Schema(description="Pet species.")
	private Species species;

	@Schema(description="Pet attributes.", example="friendly,smart")
	private String[] tags;

	@Schema(description="Photo URL.")
	@URI
	private String photo;

	/**
	 * Constructor.
	 */
	public CreatePet(String name, float price, Species species, String[] tags, String photo) {
		this.name = name;
		this.price = price;
		this.species = species;
		this.tags = tags;
		this.photo = photo;
	}

	/**
	 * Empty constructor.
	 */
	public CreatePet() {}

	//-----------------------------------------------------------------------------------------------------------------
	// Bean properties
	//-----------------------------------------------------------------------------------------------------------------

	public String getName() {
		return name;
	}

	public CreatePet name(String value) {
		this.name = value;
		return this;
	}

	public float getPrice() {
		return price;
	}

	public CreatePet price(float value) {
		this.price = value;
		return this;
	}

	public Species getSpecies() {
		return species;
	}

	public CreatePet species(Species value) {
		this.species = value;
		return this;
	}

	public String[] getTags() {
		return tags;
	}

	public CreatePet tags(String...value) {
		this.tags = value;
		return this;
	}

	public String getPhoto() {
		return photo;
	}

	public CreatePet photo(String photo) {
		this.photo = photo;
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other
	//-----------------------------------------------------------------------------------------------------------------

	public static CreatePet example() {
		return new CreatePet("Doggie", 9.99f, Species.DOG, new String[]{"smart","friendly"}, null);
	}
}
