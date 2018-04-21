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

import org.apache.juneau.annotation.*;

/**
 * Bean for creating {@link Pet} objects.
 */
public class PetCreate {

	private final String name;
	private final float price;
	private final String species;
	private final String[] tags;
	
	@BeanConstructor(properties="name,price,species,tags")
	public PetCreate(String name, float price, String species, String[] tags) {
		this.name = name;
		this.price = price;
		this.species = species;
		this.tags = tags;
	}
	
	public static PetCreate example() {
		return new PetCreate("Doggie", 9.99f, "doc", new String[] {"friendly","cute"});
	}

	public String getName() {
		return name;
	}

	public float getPrice() {
		return price;
	}

	public String getSpecies() {
		return species;
	}

	public String[] getTags() {
		return tags;
	}
}
