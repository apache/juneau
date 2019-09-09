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
 *
 * <ul class='seealso'>
 * 	<li class='extlink'>{@source}
 * </ul>
 */
@Bean(typeName="Pet", fluentSetters=true, properties="id,species,name,tags,price,status")
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

	/**
	 * Applies the specified data to this object.
	 *
	 * @param x The data to apply.
	 * @return This object.
	 */
	public Pet apply(CreatePet x) {
		this.name = x.getName();
		this.price = x.getPrice();
		this.species = x.getSpecies();
		this.tags = x.getTags() == null ? null : Arrays.asList(x.getTags());
		return this;
	}

	/**
	 * Applies the specified data to this object.
	 *
	 * @param x The data to apply.
	 * @return This object.
	 */
	public Pet apply(UpdatePet x) {
		this.id = x.getId();
		this.name = x.getName();
		this.price = x.getPrice();
		this.species = x.getSpecies();
		this.tags = Arrays.asList(x.getTags());
		this.status = x.getStatus();
		return this;
	}

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
	public Pet id(long value) {
		this.id = value;
		return this;
	}

	/**
	 * @return The <bc>name</jc> property value.
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param value The <bc>name</jc> property value.
	 * @return This object (for method chaining).
	 */
	public Pet name(String value) {
		this.name = value;
		return this;
	}

	/**
	 * @return The <bc>price</jc> property value.
	 */
	public float getPrice() {
		return price;
	}

	/**
	 * @param value The <bc>price</jc> property value.
	 * @return This object (for method chaining).
	 */
	public Pet price(float value) {
		this.price = value;
		return this;
	}

	/**
	 * @return The <bc>species</jc> property value.
	 */
	public Species getSpecies() {
		return species;
	}

	/**
	 * @param value The <bc>species</jc> property value.
	 * @return This object (for method chaining).
	 */
	public Pet species(Species value) {
		this.species = value;
		return this;
	}

	/**
	 * @return The <bc>tags</jc> property value.
	 */
	public List<String> getTags() {
		return tags;
	}

	/**
	 * @param value The <bc>tags</jc> property value.
	 * @return This object (for method chaining).
	 */
	public Pet tags(List<String> value) {
		this.tags = value;
		return this;
	}

	/**
	 * @param value The <bc>tags</jc> property value.
	 * @return This object (for method chaining).
	 */
	public Pet tags(String...value) {
		this.tags = Arrays.asList(value);
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
	public Pet status(PetStatus value) {
		this.status = value;
		return this;
	}

	/**
	 * @param statuses The statuses to match against.
	 * @return <jk>true</jk> if this pet matches at least one of the specified statuses.
	 */
	public boolean hasStatus(PetStatus...statuses) {
		for (PetStatus status : statuses)
			if (this.status == status)
				return true;
		return false;
	}

	/**
	 * @param tags The tags to match against.
	 * @return <jk>true</jk> if this pet matches at least one of the specified tags.
	 */
	public boolean hasTag(String...tags) {
		for (String tag : tags)
			for (String t : this.tags)
				if (t.equals(tag))
					return true;
		return false;
	}

	/**
	 * @return Edit page link.
	 */
	public java.net.URI getEdit() {
		return java.net.URI.create("servlet:/pet/edit/{id}");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * This shows an example generated from a static method.
	 *
	 * @return POJO example.
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

	/**
	 * Used to control format of prices in HTML view.
	 */
	public static final class PriceRender extends HtmlRender<Float> {
		@Override
		public Object getContent(SerializerSession session, Float value) {
			return value == null ? null : String.format("$%.2f", value);
		}
	}
}
