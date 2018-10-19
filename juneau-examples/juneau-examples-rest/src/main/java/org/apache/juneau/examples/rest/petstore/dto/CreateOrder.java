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

/**
 * Bean for creating {@link Order} objects.
 */
@Bean(fluentSetters=true, properties="petId,username")
public class CreateOrder {

	private long petId;
	private String username;

	/**
	 * Optional constructor.
	 */
	@BeanConstructor(properties="petId,username")
	public CreateOrder(long petId, String username) {
		this.petId = petId;
		this.username = username;
	}

	/**
	 * Constructor needed by JPA.
	 */
	public CreateOrder() {}

	//-----------------------------------------------------------------------------------------------------------------
	// Bean properties
	//-----------------------------------------------------------------------------------------------------------------

	public long getPetId() {
		return petId;
	}

	public CreateOrder petId(long value) {
		this.petId = value;
		return this;
	}

	public String getUsername() {
		return username;
	}

	public CreateOrder username(String value) {
		this.username = value;
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Used to populate Swagger examples.
	 * Example is inferred from the method name.
	 */
	public static CreateOrder example() {
		return new CreateOrder(123, "sampleuser");
	}
}
