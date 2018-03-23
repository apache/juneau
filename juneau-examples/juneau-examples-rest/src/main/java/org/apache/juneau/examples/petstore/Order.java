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
package org.apache.juneau.examples.petstore;

import org.apache.juneau.annotation.*;

@Bean(fluentSetters=true)
public class Order {
	private long id, petId;
	private int quantity;
	private String shipDate;
	private OrderStatus status;
	
	public long getId() {
		return id;
	}
	
	public Order id(long id) {
		this.id = id;
		return this;
	}
	
	public long getPetId() {
		return petId;
	}
	
	public Order petId(long petId) {
		this.petId = petId;
		return this;
	}
	
	public int getQuantity() {
		return quantity;
	}
	
	public Order quantity(int quantity) {
		this.quantity = quantity;
		return this;
	}
	
	public String getShipDate() {
		return shipDate;
	}
	
	public Order shipDate(String shipDate) {
		this.shipDate = shipDate;
		return this;
	}
	
	public OrderStatus getStatus() {
		return status;
	}
	
	public Order status(OrderStatus status) {
		this.status = status;
		return this;
	}
}
