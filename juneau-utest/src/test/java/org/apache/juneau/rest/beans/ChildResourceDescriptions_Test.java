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
package org.apache.juneau.rest.beans;

import static org.apache.juneau.junit.bct.BctAssertions.*;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class ChildResourceDescriptions_Test extends TestBase {

	@Test void a01_basic() {
		ChildResourceDescriptions x = new ChildResourceDescriptions();
		assertNotNull(x);
		assertEmpty(x);
	}

	@Test void a02_appendWithDescription() {
		ChildResourceDescriptions x = new ChildResourceDescriptions();
		x.append("child1", "Description 1");
		assertSize(1, x);
		assertEquals("child1", x.get(0).getName());
		assertEquals("Description 1", x.get(0).getDescription());
	}

	@Test void a03_appendWithUri() {
		ChildResourceDescriptions x = new ChildResourceDescriptions();
		x.append("child1", "/api/child1", "Child 1 API");
		assertSize(1, x);
		assertEquals("child1", x.get(0).getName());
		assertEquals("/api/child1", x.get(0).getUri());
		assertEquals("Child 1 API", x.get(0).getDescription());
	}

	@Test void a04_fluentSetters() {
		ChildResourceDescriptions x = new ChildResourceDescriptions();

		// Test append(String, String) returns same instance for fluent chaining
		assertSame(x, x.append("resource1", "Resource 1"));
		assertSize(1, x);

		// Test append(String, String, String) returns same instance
		assertSame(x, x.append("resource2", "/api/resource2", "Resource 2 API"));
		assertSize(2, x);
	}

	@Test void a05_fluentChaining() {
		// Test multiple fluent calls can be chained
		ChildResourceDescriptions x = new ChildResourceDescriptions()
			.append("users", "User management")
			.append("products", "/api/products", "Product catalog")
			.append("orders", "Order processing");

		assertSize(3, x);
		assertEquals("users", x.get(0).getName());
		assertEquals("User management", x.get(0).getDescription());
		assertEquals("products", x.get(1).getName());
		assertEquals("/api/products", x.get(1).getUri());
		assertEquals("Product catalog", x.get(1).getDescription());
		assertEquals("orders", x.get(2).getName());
		assertEquals("Order processing", x.get(2).getDescription());
	}

	@Test void a06_multipleResources() {
		// Test building a list of child resources
		ChildResourceDescriptions x = new ChildResourceDescriptions()
			.append("child1", "First child resource")
			.append("child2", "/child2", "Second child resource")
			.append("child3", "Third child resource");

		assertSize(3, x);
		assertNotNull(x.get(0));
		assertNotNull(x.get(1));
		assertNotNull(x.get(2));
	}
}