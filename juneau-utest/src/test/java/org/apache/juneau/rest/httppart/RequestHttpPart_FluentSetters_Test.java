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
package org.apache.juneau.rest.httppart;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class RequestHttpPart_FluentSetters_Test extends TestBase {

	@Test
	public void a01_RequestFormParam_def() throws Exception {
		// Test that def() returns correct type for fluent chaining
		var x = new RequestFormParam(null, "test", null);
		assertSame(x, x.def("default"));
		assertEquals("default", x.asString().orElse(null));

		// Test fluent chaining with other methods
		var y = new RequestFormParam(null, "test", null)
			.def("default-value")
			.schema(null);
		assertInstanceOf(RequestFormParam.class, y);
		assertEquals("default-value", y.asString().orElse(null));
	}

	@Test
	public void a02_RequestHeader_def() throws Exception {
		// Test that def() returns correct type for fluent chaining
		var x = new RequestHeader(null, "X-Missing", null);
		assertSame(x, x.def("default"));
		assertEquals("default", x.asString().orElse(null));

		// Test fluent chaining with other methods
		var y = new RequestHeader(null, "X-Missing2", null)
			.def("default-header")
			.schema(null);
		assertInstanceOf(RequestHeader.class, y);
		assertEquals("default-header", y.asString().orElse(null));
	}

	@Test
	public void a03_RequestPathParam_def() throws Exception {
		// Test that def() returns correct type for fluent chaining
		var x = new RequestPathParam(null, "param", null);
		assertSame(x, x.def("default"));
		assertEquals("default", x.asString().orElse(null));

		// Test fluent chaining with other methods
		var y = new RequestPathParam(null, "param2", null)
			.def("default-path")
			.schema(null);
		assertInstanceOf(RequestPathParam.class, y);
		assertEquals("default-path", y.asString().orElse(null));
	}

	@Test
	public void a04_RequestQueryParam_def() throws Exception {
		// Test that def() returns correct type for fluent chaining
		var x = new RequestQueryParam(null, "missing", null);
		assertSame(x, x.def("default"));
		assertEquals("default", x.asString().orElse(null));

		// Test fluent chaining with other methods
		var y = new RequestQueryParam(null, "missing2", null)
			.def("default-query")
			.schema(null);
		assertInstanceOf(RequestQueryParam.class, y);
		assertEquals("default-query", y.asString().orElse(null));
	}

	@Test
	public void a05_def_withExistingValue() throws Exception {
		// Test that def() does not override existing values
		var x = new RequestHeader(null, "X-Test", "existing");
		x.def("default");
		assertEquals("existing", x.asString().orElse(null));
	}
}