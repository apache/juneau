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

import java.io.*;
import java.time.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.http.annotation.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.client.*;
import org.apache.juneau.rest.mock.*;
import org.apache.juneau.testutils.*;

import static org.apache.juneau.assertions.Assertions.*;
import static org.apache.juneau.http.header.StandardHttpHeaders.*;

import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class BasicDateHeader_Test {

	private static final String HEADER = "Foo";
	private static final String VALUE = "Sat, 29 Oct 1994 19:43:31 GMT";

	@Rest
	public static class A {
		@RestOp
		public StringReader get(@Header(name=HEADER,multi=true) String[] h) {
			return new StringReader(h == null ? "null" : StringUtils.join(h, '|'));
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// Method tests
	//------------------------------------------------------------------------------------------------------------------

	@Test
	public void a01_basic() throws Exception {
		RestClient c = client().build();

		c.get().header(dateHeader(null,(Object)null)).run().assertBody().isEmpty();
		c.get().header(dateHeader("","*")).run().assertBody().isEmpty();
		c.get().header(dateHeader(HEADER,(Object)null)).run().assertBody().isEmpty();
		c.get().header(dateHeader(null,"*")).run().assertBody().isEmpty();

		c.get().header(dateHeader(null,()->null)).run().assertBody().isEmpty();
		c.get().header(dateHeader(HEADER,(Supplier<?>)null)).run().assertBody().isEmpty();
		c.get().header(dateHeader(null,(Supplier<?>)null)).run().assertBody().isEmpty();

		c.get().header(dateHeader(HEADER,VALUE)).run().assertBody().is(VALUE);
		c.get().header(dateHeader(HEADER,()->VALUE)).run().assertBody().is(VALUE);

		c.get().header(dateHeader(HEADER,()->null)).run().assertBody().isEmpty();

		c.get().header(dateHeader(HEADER,ZonedDateTime.parse("1994-10-29T19:43:31Z"))).run().assertBody().is("Sat, 29 Oct 1994 19:43:31 GMT");
		c.get().header(dateHeader(HEADER,GregorianCalendar.from(ZonedDateTime.parse("1994-10-29T19:43:31Z")))).run().assertBody().is("Sat, 29 Oct 1994 19:43:31 GMT");
	}

	@Test
	public void a02_asCalendar() throws Exception {
		assertObject(dateHeader(HEADER,VALUE).asCalendar()).asString(Calendar.class, x->calendarString(x)).is("1994-10-29T19:43:31Z");
		assertObject(header(HEADER,null).asCalendar()).doesNotExist();
	}

	@Test
	public void a03_asDate() throws Exception {
		assertObject(dateHeader(HEADER,VALUE).asDate()).asString().contains("1994");
		assertObject(header(HEADER,null).asDate()).doesNotExist();
	}

	@Test
	public void a04_assertZonedDateTime() throws Exception {
		dateHeader(HEADER,VALUE).assertZonedDateTime().asString().is("1994-10-29T19:43:31Z");
		header(HEADER,null).assertZonedDateTime().doesNotExist();
	}

	//------------------------------------------------------------------------------------------------------------------
	// Helper methods.
	//------------------------------------------------------------------------------------------------------------------

	private static String calendarString(Calendar c) {
		return CalendarUtils.serialize(c, CalendarUtils.Format.ISO8601_DTZ, null, null);
	}

	private static BasicDateHeader header(String name, Object value) {
		return new BasicDateHeader(name, value);
	}

	private static RestClientBuilder client() {
		return MockRestClient.create(A.class);
	}
}
