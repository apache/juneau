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
import java.time.*;
import java.util.function.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.common.internal.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.client.*;
import org.apache.juneau.rest.mock.*;

import static java.time.format.DateTimeFormatter.*;
import static java.time.temporal.ChronoUnit.*;
import static org.apache.juneau.assertions.Assertions.*;
import static org.apache.juneau.http.HttpHeaders.*;

import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class BasicDateHeader_Test {

	private static final String HEADER = "Foo";
	private static final String VALUE = "Sat, 29 Oct 1994 19:43:31 GMT";
	private static final ZonedDateTime PARSED = ZonedDateTime.from(RFC_1123_DATE_TIME.parse(VALUE)).truncatedTo(SECONDS);

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
		c.get().header(dateHeader(HEADER,VALUE)).run().assertContent(VALUE);
		c.get().header(dateHeader(HEADER,VALUE)).run().assertContent(VALUE);
		c.get().header(dateHeader(HEADER,PARSED)).run().assertContent(VALUE);
		c.get().header(dateHeader(HEADER,()->PARSED)).run().assertContent(VALUE);

		// Invalid usage.
		c.get().header(dateHeader(HEADER,(Supplier<ZonedDateTime>)null)).run().assertContent().isEmpty();
		c.get().header(dateHeader(HEADER,()->null)).run().assertContent().isEmpty();
		assertThrown(()->dateHeader("", VALUE)).asMessage().is("Name cannot be empty on header.");
		assertThrown(()->dateHeader(null, VALUE)).asMessage().is("Name cannot be empty on header.");
		assertThrown(()->dateHeader("", PARSED)).asMessage().is("Name cannot be empty on header.");
		assertThrown(()->dateHeader(null, PARSED)).asMessage().is("Name cannot be empty on header.");
		assertThrown(()->dateHeader("", ()->PARSED)).asMessage().is("Name cannot be empty on header.");
		assertThrown(()->dateHeader(null, ()->PARSED)).asMessage().is("Name cannot be empty on header.");
	}

	@Test
	public void a02_asZonedDateTime() throws Exception {
		assertOptional(dateHeader(HEADER,VALUE).asZonedDateTime()).asString().is("1994-10-29T19:43:31Z");
	}

	@Test
	public void a04_assertZonedDateTime() throws Exception {
		dateHeader(HEADER,VALUE).assertZonedDateTime().asString().is("1994-10-29T19:43:31Z");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Helper methods.
	//------------------------------------------------------------------------------------------------------------------

	private static RestClient.Builder client() {
		return MockRestClient.create(A.class);
	}
}
