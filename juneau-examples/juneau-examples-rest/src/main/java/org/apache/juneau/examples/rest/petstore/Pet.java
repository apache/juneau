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
package org.apache.juneau.examples.rest.petstore;

import java.util.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.html.annotation.*;
import org.apache.juneau.xml.annotation.*;

/**
 * Pet bean.
 */
@Bean(typeName="Pet", fluentSetters=true, properties="id,species,name,photoUrls,tags,price,status")
public class Pet {
	private long id;
	private float price;
	private Species species;
	private String name;
	private List<String> photoUrls;
	private List<Tag> tags;
	private PetStatus status;
	
	// This shows an example generated from a static method.
	@Example
	public static Pet example() {
		return new Pet()
			.id(123)
			.species(Species.example())
			.name("Doggie")
			.tags(Tag.example())
			.status(PetStatus.AVAILABLE);
	}

	@Html(link="servlet:/pet/{id}")
	public long getId() {
		return id;
	}

	public Pet id(long id) {
		this.id = id;
		return this;
	}
	
	public Species getSpecies() {
		return species;
	}
	
	public Pet species(Species species) {
		this.species = species;
		return this;
	}
	
	public String getName() {
		return name;
	}
	
	public Pet name(String name) {
		this.name = name;
		return this;
	}
	
	@URI
	@Xml(childName="photoUrl")
	public List<String> getPhotoUrls() {
		return photoUrls;
	}
	
	public Pet photoUrls(List<String> photoUrls) {
		this.photoUrls = photoUrls;
		return this;
	}
	
	public List<Tag> getTags() {
		return tags;
	}

	public Pet tags(List<Tag> tags) {
		this.tags = tags;
		return this;
	}
	
	public Pet tags(Tag...tags) {
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
	
	public boolean hasStatus(PetStatus...statuses) {
		for (PetStatus status : statuses)
			if (this.status == status)
				return true;
		return false;
	}

	public boolean hasTag(String...tags) {
		for (String tag : tags)
			for (Tag t : this.tags)
				if (t.getName().equals(tag))
					return true;
		return false;
	}
	
	@BeanProperty(format="$%.2f")  // Renders price in dollars.
	public float getPrice() {
		return price;
	}
	
	public Pet price(float price) {
		this.price = price;
		return this;
	}
}
