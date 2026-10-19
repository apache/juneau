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
package org.apache.juneau.microbench.runtime;

import static org.apache.juneau.commons.utils.Shorts.*;

import java.time.*;
import java.util.*;

/**
 * Mid-size bean graph shared by the runtime micro-benchmarks.
 *
 * <p>
 * One {@link Order} holds a nested {@link Customer} (with two {@link Address} beans), 20 {@link LineItem} beans,
 * string lists, string/number maps, {@link ZonedDateTime} and {@link LocalDate} values and {@link Status} enums.
 * Serialized as JSON it is roughly 4 KB.
 *
 * <p>
 * {@link Order} and {@link Customer} use getters and setters, while {@link LineItem} and {@link Address} use
 * public fields, so both bean-property access paths are on the measured path.
 *
 * @since 10.0.0
 */
public final class BenchBeans {

	/** Number of line items in the graph returned by {@link #order()}. */
	public static final int LINE_ITEMS = 20;

	private BenchBeans() {}

	/** Order state. */
	public enum Status {
		/** New order. */
		PENDING,
		/** Paid. */
		PAID,
		/** Shipped. */
		SHIPPED,
		/** Delivered. */
		DELIVERED,
		/** Cancelled. */
		CANCELLED
	}

	/** Postal address with public fields. */
	public static class Address {
		/** Street. */
		public String street;
		/** City. */
		public String city;
		/** Postal code. */
		public String postalCode;
		/** Country code. */
		public String country;
		/** Default address flag. */
		public boolean primary;
	}

	/** Customer with getters and setters. */
	public static class Customer {
		private long id;
		private String name;
		private String email;
		private LocalDate since;
		private List<Address> addresses;
		private Map<String,String> preferences;

		/** @return The id. */
		public long getId() { return id; }
		/** @param value The new value. */
		public void setId(long value) { id = value; }
		/** @return The name. */
		public String getName() { return name; }
		/** @param value The new value. */
		public void setName(String value) { name = value; }
		/** @return The email. */
		public String getEmail() { return email; }
		/** @param value The new value. */
		public void setEmail(String value) { email = value; }
		/** @return The customer-since date. */
		public LocalDate getSince() { return since; }
		/** @param value The new value. */
		public void setSince(LocalDate value) { since = value; }
		/** @return The addresses. */
		public List<Address> getAddresses() { return addresses; }
		/** @param value The new value. */
		public void setAddresses(List<Address> value) { addresses = value; }
		/** @return The preferences. */
		public Map<String,String> getPreferences() { return preferences; }
		/** @param value The new value. */
		public void setPreferences(Map<String,String> value) { preferences = value; }
	}

	/** Order line with public fields. */
	public static class LineItem {
		/** Stock-keeping unit. */
		public String sku;
		/** Description. */
		public String description;
		/** Quantity. */
		public int quantity;
		/** Unit price. */
		public double unitPrice;
		/** Line status. */
		public Status status;
		/** Free-form tags. */
		public List<String> tags;
	}

	/** Root bean with getters and setters. */
	public static class Order {
		private String id;
		private Status status;
		private ZonedDateTime created;
		private LocalDate shipBy;
		private boolean priority;
		private double total;
		private Customer customer;
		private List<LineItem> items;
		private List<Status> history;
		private Map<String,String> attributes;
		private Map<String,Integer> counters;

		/** @return The id. */
		public String getId() { return id; }
		/** @param value The new value. */
		public void setId(String value) { id = value; }
		/** @return The status. */
		public Status getStatus() { return status; }
		/** @param value The new value. */
		public void setStatus(Status value) { status = value; }
		/** @return The creation time. */
		public ZonedDateTime getCreated() { return created; }
		/** @param value The new value. */
		public void setCreated(ZonedDateTime value) { created = value; }
		/** @return The ship-by date. */
		public LocalDate getShipBy() { return shipBy; }
		/** @param value The new value. */
		public void setShipBy(LocalDate value) { shipBy = value; }
		/** @return The priority flag. */
		public boolean isPriority() { return priority; }
		/** @param value The new value. */
		public void setPriority(boolean value) { priority = value; }
		/** @return The order total. */
		public double getTotal() { return total; }
		/** @param value The new value. */
		public void setTotal(double value) { total = value; }
		/** @return The customer. */
		public Customer getCustomer() { return customer; }
		/** @param value The new value. */
		public void setCustomer(Customer value) { customer = value; }
		/** @return The line items. */
		public List<LineItem> getItems() { return items; }
		/** @param value The new value. */
		public void setItems(List<LineItem> value) { items = value; }
		/** @return The status history. */
		public List<Status> getHistory() { return history; }
		/** @param value The new value. */
		public void setHistory(List<Status> value) { history = value; }
		/** @return The attributes. */
		public Map<String,String> getAttributes() { return attributes; }
		/** @param value The new value. */
		public void setAttributes(Map<String,String> value) { attributes = value; }
		/** @return The counters. */
		public Map<String,Integer> getCounters() { return counters; }
		/** @param value The new value. */
		public void setCounters(Map<String,Integer> value) { counters = value; }
	}

	/**
	 * Builds the benchmark bean graph.
	 *
	 * <p>
	 * The graph is deterministic, so every run and every format serializes the same content.
	 *
	 * @return A new order graph.
	 */
	public static Order order() {
		var home = address("1 Main Street", "Springfield", "12345", "US", true);
		var work = address("200 Market Street", "Shelbyville", "67890", "US", false);

		var customer = new Customer();
		customer.setId(4711L);
		customer.setName("Jane Doe");
		customer.setEmail("jane.doe@example.org");
		customer.setSince(LocalDate.of(2019, 4, 1));
		customer.setAddresses(l(home, work));
		customer.setPreferences(m("language", "en", "currency", "USD", "newsletter", "weekly"));

		List<LineItem> items = los(LINE_ITEMS);
		var statuses = Status.values();
		var total = 0.0;
		for (var i = 0; i < LINE_ITEMS; i++) {
			var li = new LineItem();
			li.sku = "SKU-" + (10000 + i);
			li.description = "Item number " + i + " with a moderately long description";
			li.quantity = 1 + i % 5;
			li.unitPrice = 9.99 + i * 1.25;
			li.status = statuses[i % statuses.length];
			li.tags = l("tag" + i, "group" + (i % 3), "bench");
			total += li.quantity * li.unitPrice;
			items.add(li);
		}

		Map<String,String> attributes = m();
		for (var i = 0; i < 8; i++)
			attributes.put("attr" + i, "value-" + i);
		Map<String,Integer> counters = m();
		for (var i = 0; i < 8; i++)
			counters.put("counter" + i, i * 17);

		var order = new Order();
		order.setId("ORD-2026-000123");
		order.setStatus(Status.PAID);
		order.setCreated(ZonedDateTime.of(2026, 10, 19, 14, 30, 15, 0, ZoneOffset.UTC));
		order.setShipBy(LocalDate.of(2026, 10, 24));
		order.setPriority(true);
		order.setTotal(total);
		order.setCustomer(customer);
		order.setItems(items);
		order.setHistory(l(Status.PENDING, Status.PAID));
		order.setAttributes(attributes);
		order.setCounters(counters);
		return order;
	}

	private static Address address(String street, String city, String postalCode, String country, boolean primary) {
		var a = new Address();
		a.street = street;
		a.city = city;
		a.postalCode = postalCode;
		a.country = country;
		a.primary = primary;
		return a;
	}
}
