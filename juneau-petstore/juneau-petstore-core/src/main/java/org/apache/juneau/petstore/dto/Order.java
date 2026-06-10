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

/**
 * Order domain bean.
 *
 * <p>
 * Represents an order placed against a {@link Pet}.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauPetstoreOverview">juneau-petstore</a>
 * </ul>
 */
public class Order {

	private long id;
	private long petId;
	private String shipDate;
	private OrderStatus status;

	/**
	 * Returns the order ID.
	 *
	 * @return The order ID.
	 */
	public long getId() {
		return id;
	}

	/**
	 * Sets the order ID.
	 *
	 * @param value The new value.
	 * @return This object.
	 */
	public Order setId(long value) {
		id = value;
		return this;
	}

	/**
	 * Returns the ID of the pet being ordered.
	 *
	 * @return The ID of the pet being ordered.
	 */
	public long getPetId() {
		return petId;
	}

	/**
	 * Sets the ID of the pet being ordered.
	 *
	 * @param value The new value.
	 * @return This object.
	 */
	public Order setPetId(long value) {
		petId = value;
		return this;
	}

	/**
	 * Returns the shipping date as an ISO 8601 string.
	 *
	 * @return The shipping date as an ISO 8601 string.  Can be <jk>null</jk>.
	 */
	public String getShipDate() {
		return shipDate;
	}

	/**
	 * Sets the shipping date.
	 *
	 * @param value The new value.  Can be <jk>null</jk>.
	 * @return This object.
	 */
	public Order setShipDate(String value) {
		shipDate = value;
		return this;
	}

	/**
	 * Returns the lifecycle status of the order.
	 *
	 * @return The lifecycle status of the order.
	 */
	public OrderStatus getStatus() {
		return status;
	}

	/**
	 * Sets the lifecycle status of the order.
	 *
	 * @param value The new value.
	 * @return This object.
	 */
	public Order setStatus(OrderStatus value) {
		status = value;
		return this;
	}
}
