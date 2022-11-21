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
package org.apache.juneau.http.header;

import static org.junit.runners.MethodSorters.*;
import static org.apache.juneau.testutils.StreamUtils.*;

import java.io.*;
import java.util.function.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.common.internal.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.client.*;
import org.apache.juneau.rest.mock.*;

import static org.apache.juneau.assertions.Assertions.*;
import static org.apache.juneau.http.HttpHeaders.*;

import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class BasicIntegerHeader_Test {

	private static final String HEADER = "Foo";
	private static final String VALUE = "123";
	private static final Integer PARSED = 123;

	@Rest
	public static class A {
		@RestOp
		public StringReader get(@Header(name=HEADER) @Schema(cf="multi") String[] h) {
			return reader(h == null ? "null" : StringUtils.join(h, '|'));
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// Method tests
	//------------------------------------------------------------------------------------------------------------------

	@Test
	public void a01_basic() throws Exception {
		RestClient c = client().build();

		// Normal usage.
		c.get().header(integerHeader(HEADER,VALUE)).run().assertContent(VALUE);
		c.get().header(integerHeader(HEADER,VALUE)).run().assertContent(VALUE);
		c.get().header(integerHeader(HEADER,PARSED)).run().assertContent(VALUE);
		c.get().header(integerHeader(HEADER,()->PARSED)).run().assertContent(VALUE);

		// Invalid usage.
		c.get().header(integerHeader(HEADER,(Supplier<Integer>)null)).run().assertContent().isEmpty();
		assertThrown(()->integerHeader(HEADER,"foo")).asMessage().is("Value 'foo' could not be parsed as an integer.");
		assertThrown(()->integerHeader("", VALUE)).asMessage().is("Name cannot be empty on header.");
		assertThrown(()->integerHeader(null, VALUE)).asMessage().is("Name cannot be empty on header.");
		assertThrown(()->integerHeader("", PARSED)).asMessage().is("Name cannot be empty on header.");
		assertThrown(()->integerHeader(null, PARSED)).asMessage().is("Name cannot be empty on header.");
		assertThrown(()->integerHeader("", ()->PARSED)).asMessage().is("Name cannot be empty on header.");
		assertThrown(()->integerHeader(null, ()->PARSED)).asMessage().is("Name cannot be empty on header.");
	}

	@Test
	public void a02_assertInteger() throws Exception {
		integerHeader(HEADER,1).assertInteger().is(1);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Helper methods.
	//------------------------------------------------------------------------------------------------------------------

	private static RestClient.Builder client() {
		return MockRestClient.create(A.class);
	}
}
