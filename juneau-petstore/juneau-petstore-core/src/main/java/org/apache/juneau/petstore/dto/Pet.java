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
package org.apache.juneau.petstore.dto;

import java.util.*;

/**
 * Pet domain bean.
 *
 * <p>
 * Represents a pet for sale in the petstore.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauPetstore">juneau-petstore</a>
 * </ul>
 */
public class Pet {

	private long id;
	private Species species;
	private String name;
	private float price;
	private List<String> tags;
	private PetStatus status;
	private String photo;

	/**
	 * Returns the pet ID.
	 *
	 * @return The pet ID.
	 */
	public long getId() {
		return id;
	}

	/**
	 * Sets the pet ID.
	 *
	 * @param value The new value.
	 * @return This object.
	 */
	public Pet setId(long value) {
		id = value;
		return this;
	}

	/**
	 * Returns the species of the pet.
	 *
	 * @return The species of the pet.
	 */
	public Species getSpecies() {
		return species;
	}

	/**
	 * Sets the species of the pet.
	 *
	 * @param value The new value.
	 * @return This object.
	 */
	public Pet setSpecies(Species value) {
		species = value;
		return this;
	}

	/**
	 * Returns the name of the pet.
	 *
	 * @return The name of the pet.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Sets the name of the pet.
	 *
	 * @param value The new value.
	 * @return This object.
	 */
	public Pet setName(String value) {
		name = value;
		return this;
	}

	/**
	 * Returns the price of the pet.
	 *
	 * @return The price of the pet.
	 */
	public float getPrice() {
		return price;
	}

	/**
	 * Sets the price of the pet.
	 *
	 * @param value The new value.
	 * @return This object.
	 */
	public Pet setPrice(float value) {
		price = value;
		return this;
	}

	/**
	 * Returns the tags associated with the pet.
	 *
	 * @return The tags associated with the pet.  Can be <jk>null</jk>.
	 */
	public List<String> getTags() {
		return tags;
	}

	/**
	 * Sets the tags associated with the pet.
	 *
	 * @param value The new value.  Can be <jk>null</jk>.
	 * @return This object.
	 */
	public Pet setTags(List<String> value) {
		tags = value;
		return this;
	}

	/**
	 * Returns the lifecycle status of the pet.
	 *
	 * @return The lifecycle status of the pet.
	 */
	public PetStatus getStatus() {
		return status;
	}

	/**
	 * Sets the lifecycle status of the pet.
	 *
	 * @param value The new value.
	 * @return This object.
	 */
	public Pet setStatus(PetStatus value) {
		status = value;
		return this;
	}

	/**
	 * Returns the URI of the pet photo.
	 *
	 * @return The URI of the pet photo.  Can be <jk>null</jk>.
	 */
	public String getPhoto() {
		return photo;
	}

	/**
	 * Sets the URI of the pet photo.
	 *
	 * @param value The new value.  Can be <jk>null</jk>.
	 * @return This object.
	 */
	public Pet setPhoto(String value) {
		photo = value;
		return this;
	}
}
