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
package org.apache.juneau.petstore.dto;

import org.apache.juneau.annotation.*;
import org.apache.juneau.jsonschema.annotation.*;

/**
 * Bean for creating {@link Pet} objects.
 *
 * <ul class='seealso'>
 * 	<li class='extlink'>{@source}
 * </ul>
 */
@Bean(fluentSetters=true, properties="name,price,species,tags")
public class CreatePet {

	@Schema(description="Pet name.", minLength=3, maxLength=50)
	private String name;

	@Schema(description="Price of pet.", maximum="999.99")
	private float price;

	@Schema(description="Pet species.")
	private Species species;

	@Schema(description="Pet attributes.", example="friendly,smart")
	private String[] tags;

	/**
	 * Constructor.
	 *
	 * @param name The <bc>name</bc> property value.
	 * @param price The <bc>price</bc> property value.
	 * @param species The <bc>species</bc> property value.
	 * @param tags The <bc>tags</bc> property value.
	 * @param photo The <bc>photo</bc> property value.
	 */
	public CreatePet(String name, float price, Species species, String[] tags) {
		this.name = name;
		this.price = price;
		this.species = species;
		this.tags = tags;
	}

	/**
	 * Empty constructor.
	 */
	public CreatePet() {}

	//-----------------------------------------------------------------------------------------------------------------
	// Bean properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * @return The <bc>name</bc> property value.
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param value The <bc>name</bc> property value.
	 * @return This object (for method chaining).
	 */
	public CreatePet name(String value) {
		this.name = value;
		return this;
	}

	/**
	 * @return The <bc>price</bc> property value.
	 */
	public float getPrice() {
		return price;
	}

	/**
	 * @param value The <bc>price</bc> property value.
	 * @return This object (for method chaining).
	 */
	public CreatePet price(float value) {
		this.price = value;
		return this;
	}

	/**
	 * @return The <bc>species</bc> property value.
	 */
	public Species getSpecies() {
		return species;
	}

	/**
	 * @param value The <bc>species</bc> property value.
	 * @return This object (for method chaining).
	 */
	public CreatePet species(Species value) {
		this.species = value;
		return this;
	}

	/**
	 * @return The <bc>tags</bc> property value.
	 */
	public String[] getTags() {
		return tags;
	}

	/**
	 * @param value The <bc>tags</bc> property value.
	 * @return This object (for method chaining).
	 */
	public CreatePet tags(String...value) {
		this.tags = value;
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * @return An example POJO.
	 */
	public static CreatePet example() {
		return new CreatePet("Doggie", 9.99f, Species.DOG, new String[]{"smart","friendly"});
	}
}
