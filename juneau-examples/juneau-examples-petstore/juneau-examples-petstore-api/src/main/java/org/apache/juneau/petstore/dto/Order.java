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

import static javax.persistence.TemporalType.*;

import java.util.*;

import javax.persistence.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.html.annotation.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.jsonschema.annotation.*;
import org.apache.juneau.transforms.*;

/**
 * Order bean.
 *
 * <ul class='seealso'>
 * 	<li class='extlink'>{@source}
 * </ul>
 */
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
	@Swap(TemporalDateSwap.IsoLocalDate.class)
	private Date shipDate;

	/**
	 * Applies the specified create data to this order.
	 *
	 * @param o The create data to apply.
	 * @return This object.
	 */
	public Order apply(CreateOrder o) {
		this.petId = o.getPetId();
		this.username = o.getUsername();
		return this;
	}

	/**
	 * Applies the specified order this order.
	 *
	 * @param o The order to apply.
	 * @return This object.
	 */
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

	/**
	 * @return The <bc>id</bc> property value.
	 */
	public long getId() {
		return id;
	}

	/**
	 * @param value The <bc>id</bc> property value.
	 * @return This object (for method chaining).
	 */
	public Order id(long value) {
		this.id = value;
		return this;
	}

	/**
	 * @return The <bc>shipDate</bc> property value.
	 */
	public Date getShipDate() {
		return shipDate;
	}

	/**
	 * @param value The <bc>shipDate</bc> property value.
	 * @return This object (for method chaining).
	 */
	public Order shipDate(Date value) {
		this.shipDate = value;
		return this;
	}

	/**
	 * @return The <bc>status</bc> property value.
	 */
	public OrderStatus getStatus() {
		return status;
	}

	/**
	 * @param value The <bc>status</bc> property value.
	 * @return This object (for method chaining).
	 */
	public Order status(OrderStatus value) {
		this.status = value;
		return this;
	}

	/**
	 * @return The <bc>petId</bc> property value.
	 */
	public long getPetId() {
		return petId;
	}

	/**
	 * @param value The <bc>petId</bc> property value.
	 * @return This object (for method chaining).
	 */
	public Order petId(long value) {
		this.petId = value;;
		return this;
	}

	/**
	 * @return The <bc>username</bc> property value.
	 */
	public String getUsername() {
		return username;
	}

	/**
	 * @param value The <bc>username</bc> property value.
	 * @return This object (for method chaining).
	 */
	public Order username(String value) {
		this.username = value;
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * This shows an example generated from a static method.
	 *
	 * @return The example POJO.
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
