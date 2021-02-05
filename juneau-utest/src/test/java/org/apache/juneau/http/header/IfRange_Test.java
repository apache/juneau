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

import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.*;
import static org.apache.juneau.assertions.Assertions.*;
import static org.apache.juneau.http.header.IfRange.*;

import java.io.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.http.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.client.*;
import org.apache.juneau.rest.mock.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class IfRange_Test {

	private static final String HEADER = "If-Range";
	private static final String ETAG_VALUE = "\"foo\"";
	private static final String ETAG_VALUE2 = "W/\"foo\"";
	private static final Calendar CALENDAR_VALUE = new GregorianCalendar(TimeZone.getTimeZone("Z"));
	static {
		CALENDAR_VALUE.set(2000,11,31,12,34,56);
	}

	@Rest
	public static class A {
		@RestOp
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
		c.get().header(new IfRange((String)null)).run().assertBody().isEmpty();
		c.get().header(of((Object)null)).run().assertBody().isEmpty();
		c.get().header(of((Supplier<?>)null)).run().assertBody().isEmpty();
		c.get().header(of(()->null)).run().assertBody().isEmpty();
		c.get().header(of(ETAG_VALUE)).run().assertBody().is(ETAG_VALUE);
		c.get().header(of(ETAG_VALUE)).run().assertBody().is(ETAG_VALUE);
		c.get().header(of(new StringBuilder(ETAG_VALUE))).run().assertBody().is(ETAG_VALUE);
		c.get().header(of(()->ETAG_VALUE)).run().assertBody().is(ETAG_VALUE);
		c.get().header(of(ETAG_VALUE2)).run().assertBody().is(ETAG_VALUE2);
		c.get().header(of(ETAG_VALUE2)).run().assertBody().is(ETAG_VALUE2);
		c.get().header(of(new StringBuilder(ETAG_VALUE2))).run().assertBody().is(ETAG_VALUE2);
		c.get().header(of(()->ETAG_VALUE2)).run().assertBody().is(ETAG_VALUE2);
		c.get().header(of(CALENDAR_VALUE)).run().assertBody().is("Sun, 31 Dec 2000 12:34:56 GMT");
		c.get().header(of(CALENDAR_VALUE)).run().assertBody().is("Sun, 31 Dec 2000 12:34:56 GMT");
		c.get().header(of(()->CALENDAR_VALUE)).run().assertBody().is("Sun, 31 Dec 2000 12:34:56 GMT");
		c.get().header(new IfRange(ETAG_VALUE)).run().assertBody().is(ETAG_VALUE);
	}

	@Test
	public void a02_asEntityTag() throws Exception {
		EntityTag x = of(ETAG_VALUE).asEntityTag();
		assertString(x).is("\"foo\"");
		assertNull(of(()->null).asEntityTag());
		assertNull(of(()->CALENDAR_VALUE).asEntityTag());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Helper methods.
	//------------------------------------------------------------------------------------------------------------------

	private static RestClientBuilder client() {
		return MockRestClient.create(A.class);
	}
}
