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

import static javax.persistence.TemporalType.*;

import java.util.*;

import javax.persistence.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.html.annotation.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.jsonschema.annotation.*;
import org.apache.juneau.transforms.*;

@Bean(fluentSetters=true, properties="id,petId,username,status,shipDate")
@Example("{id:123,petId:456,shipDate:'2012-12-21',status:'APPROVED'}")
@Entity(name="PetstoreOrder")
public class Order {

	@Column @Id @GeneratedValue
	@Schema(description="Unique identifier for this order.")
	@Html(link="servlet:/store/order/{id}")
	private long id;

	@Column
	@Schema(description="Pet unique identifier.")
	@Html(link="servlet:/pet/{id}")
	private long petId;

	@Column(length=20)
	@Schema(description="User who created this order.", minLength=3, maxLength=20)
	@Html(link="servlet:/user/{username}")
	private String username;

	@Column
	@Enumerated(EnumType.STRING)
	@Schema(description="The current order status.")
	private OrderStatus status;

	@Column @Temporal(TIMESTAMP)
	@Schema(description="The ship date for this order.", format="date-time")
	@Swap(DateSwap.ISO8601D.class)
	private Date shipDate;

	public Order apply(CreateOrder o) {
		this.petId = o.getPetId();
		this.username = o.getUsername();
		return this;
	}

	public Order apply(Order o) {
		this.id = o.getId();
		this.petId = o.getPetId();
		this.username = o.getUsername();
		this.status = o.getStatus();
		this.shipDate = o.getShipDate();
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Bean properties
	//-----------------------------------------------------------------------------------------------------------------

	public long getId() {
		return id;
	}

	public Order id(long id) {
		this.id = id;
		return this;
	}

	public Date getShipDate() {
		return shipDate;
	}

	public Order shipDate(Date value) {
		this.shipDate = value;
		return this;
	}

	public OrderStatus getStatus() {
		return status;
	}

	public Order status(OrderStatus value) {
		this.status = value;
		return this;
	}

	public long getPetId() {
		return petId;
	}

	public Order petId(long value) {
		this.petId = value;;
		return this;
	}

	public String getUsername() {
		return username;
	}

	public Order username(String value) {
		this.username = value;
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * This shows an example generated from a static method.
	 */
	@Example
	public static Order example() {
		return new Order()
			.id(123)
			.username("sampleuser")
			.petId(456)
			.status(OrderStatus.APPROVED)
			.shipDate(DateUtils.parseISO8601("2020-10-10"))
		;
	}
}
