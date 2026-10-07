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
package org.apache.juneau.rest.validation;

import org.apache.juneau.*;
import org.apache.juneau.commons.bean.*;
import org.apache.juneau.http.*;
import org.apache.juneau.marshall.json.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.junit.jupiter.api.*;

/**
 * Tests that a missing {@code @BeanProp(required=true)} property in a request body produces 400 (WORK-J0585).
 */
class RestRequiredProperty_Test extends TestBase {

	public static class Order {
		@BeanProp(required=true)
		public String sku;
		public int quantity;
	}

	public record Line(@BeanProp(required=true) String sku, int quantity) {}

	@Rest(serializers=JsonSerializer.class, parsers=JsonParser.class, defaultAccept="application/json")
	public static class A {
		@RestPost("/order")
		public String submit(@Content Order order) {
			return order.sku;
		}

		@RestPost("/line")
		public String line(@Content Line line) {
			return line.sku();
		}
	}

	@Test
	void a01_missingRequired_400() throws Exception {
		MockRestClient.buildLax(A.class).post("/order", "{\"quantity\":1}")
			.contentType("application/json")
			.run()
			.assertStatus(400)
			.assertContent().isContains("Could not convert request content");
	}

	@Test
	void a02_present_200() throws Exception {
		MockRestClient.buildLax(A.class).post("/order", "{\"sku\":\"X1\",\"quantity\":1}")
			.contentType("application/json")
			.run()
			.assertStatus(200)
			.assertContent("\"X1\"");
	}

	@Test
	void a03_record_400NotConstructorError() throws Exception {
		MockRestClient.buildLax(A.class).post("/line", "{\"quantity\":1}")
			.contentType("application/json")
			.run()
			.assertStatus(400);
	}
}
