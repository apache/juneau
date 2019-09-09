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
 * Bean for updating {@link Pet} objects.
 *
 * <ul class='seealso'>
 * 	<li class='extlink'>{@source}
 * </ul>
 */
@Bean(fluentSetters=true, properties="id,name,price,species,tags,status")
public class UpdatePet extends CreatePet {

	@Schema(description="Pet identifier.", minimum="1")
	private long id;

	@Schema(description="Updated pet status.")
	private PetStatus status;

	/**
	 * Constructor.
	 *
	 * @param id The <bc>id</bc> property value.
	 * @param name The <bc>name</bc> property value.
	 * @param price The <bc>price</bc> property value.
	 * @param species The <bc>species</bc> property value.
	 * @param tags The <bc>tags</bc> property value.
	 * @param status The <bc>status</bc> property value.
	 */
	public UpdatePet(long id, String name, float price, Species species, String[] tags, PetStatus status) {
		super(name, price, species, tags);
		this.id = id;
		this.status = status;
	}

	/**
	 * Empty constructor.
	 */
	public UpdatePet() {}

	//-----------------------------------------------------------------------------------------------------------------
	// Bean properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * @return The <bc>id</jc> property value.
	 */
	public long getId() {
		return id;
	}

	/**
	 * @param value The <bc>id</jc> property value.
	 * @return This object (for method chaining).
	 */
	public UpdatePet id(long value) {
		this.id = value;
		return this;
	}

	/**
	 * @return The <bc>status</jc> property value.
	 */
	public PetStatus getStatus() {
		return status;
	}

	/**
	 * @param value The <bc>status</jc> property value.
	 * @return This object (for method chaining).
	 */
	public UpdatePet status(PetStatus value) {
		this.status = value;
		return this;
	}

	@Override
	public UpdatePet name(String value) {
		super.name(value);
		return this;
	}

	@Override
	public UpdatePet price(float value) {
		super.price(value);
		return this;
	}

	@Override
	public UpdatePet species(Species value) {
		super.species(value);
		return this;
	}

	@Override
	public UpdatePet tags(String...value) {
		super.tags(value);
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other
	//-----------------------------------------------------------------------------------------------------------------

	public static UpdatePet example() {
		return new UpdatePet(123, "Doggie", 9.99f, Species.DOG, new String[]{"smart","friendly"}, PetStatus.SOLD);
	}
}
