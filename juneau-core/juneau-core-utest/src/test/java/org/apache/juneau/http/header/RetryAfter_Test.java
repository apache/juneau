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
import static org.apache.juneau.assertions.Assertions.*;

import static org.apache.juneau.http.header.RetryAfter.*;

import java.io.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.http.annotation.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.client.*;
import org.apache.juneau.rest.mock.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class RetryAfter_Test {

	private static final String HEADER = "Retry-After";
	private static final String INT_VALUE = "123";
	private static final Calendar CALENDAR_VALUE = new GregorianCalendar(TimeZone.getTimeZone("Z"));
	static {
		CALENDAR_VALUE.set(2000,11,31,12,34,56);
	}

	@Rest
	public static class A {
		@RestMethod
		public StringReader get(@Header(name=HEADER,multi=true) String[] h) {
			return new StringReader(h == null ? "null" : StringUtils.join(h, ','));
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// Method tests
	//------------------------------------------------------------------------------------------------------------------

	@Test
	public void a01_basic() throws Exception {
		RestClient c = client().build();

		c.get().header(of((String)null)).run().assertBody().isEmpty();
		c.get().header(of((Object)null)).run().assertBody().isEmpty();
		c.get().header(of((Supplier<?>)null)).run().assertBody().isEmpty();
		c.get().header(of(()->null)).run().assertBody().isEmpty();
		c.get().header(of(INT_VALUE)).run().assertBody().is(INT_VALUE);
		c.get().header(of(INT_VALUE)).run().assertBody().is(INT_VALUE);
		c.get().header(of(new StringBuilder(INT_VALUE))).run().assertBody().is(INT_VALUE);
		c.get().header(of(()->INT_VALUE)).run().assertBody().is(INT_VALUE);
		c.get().header(of(CALENDAR_VALUE)).run().assertBody().stderr().is("Sun, 31 Dec 2000 12:34:56 GMT");
		c.get().header(of(()->CALENDAR_VALUE)).run().assertBody().is("Sun, 31 Dec 2000 12:34:56 GMT");
		c.get().header(new RetryAfter(INT_VALUE)).run().assertBody().is(INT_VALUE);
	}

	@Test
	public void a02_asZonedDateTime() throws Exception {
		assertObject(of(CALENDAR_VALUE).asZonedDateTime().toString()).stderr().is("2000-12-31T12:34:56Z[GMT]");
	}

	@Test
	public void a03_asInt() throws Exception {
		assertObject(of(123).asInt()).is(123);
		assertObject(new RetryAfter((String)null).asInt()).is(-1);
		assertObject(of(()->null).asInt()).is(-1);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Helper methods.
	//------------------------------------------------------------------------------------------------------------------

	private static RestClientBuilder client() {
		return MockRestClient.create(A.class);
	}
}
