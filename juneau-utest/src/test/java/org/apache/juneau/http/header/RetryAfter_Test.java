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
import static java.time.format.DateTimeFormatter.*;
import static java.time.temporal.ChronoUnit.*;
import static org.apache.juneau.assertions.Assertions.*;
import static org.apache.juneau.http.HttpHeaders.*;
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
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class RetryAfter_Test {

	private static final String HEADER = "Retry-After";
	private static final String VALUE1 = "123";
	private static final String VALUE2 = "Sat, 29 Oct 1994 19:43:31 GMT";
	private static final Integer PARSED1 = 123;
	private static final ZonedDateTime PARSED2 = ZonedDateTime.from(RFC_1123_DATE_TIME.parse(VALUE2)).truncatedTo(SECONDS);

	@Rest
	public static class A {
		@RestOp
		public StringReader get(@Header(name=HEADER) @Schema(cf="multi") String[] h) {
			return reader(h == null ? "null" : StringUtils.join(h, ','));
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// Method tests
	//------------------------------------------------------------------------------------------------------------------

	@Test
	public void a01_basic() throws Exception {
		RestClient c = client().build();

		// Normal usage.
		c.get().header(retryAfter(VALUE1)).run().assertContent(VALUE1);
		c.get().header(retryAfter(VALUE1)).run().assertContent(VALUE1);
		c.get().header(retryAfter(PARSED1)).run().assertContent(VALUE1);
		c.get().header(retryAfter(()->PARSED1)).run().assertContent(VALUE1);

		c.get().header(retryAfter(VALUE2)).run().assertContent(VALUE2);
		c.get().header(retryAfter(VALUE2)).run().assertContent(VALUE2);
		c.get().header(retryAfter(PARSED2)).run().assertContent(VALUE2);
		c.get().header(retryAfter(()->PARSED2)).run().assertContent(VALUE2);

		// Invalid usage.
		c.get().header(retryAfter((String)null)).run().assertContent().isEmpty();
		c.get().header(retryAfter((ZonedDateTime)null)).run().assertContent().isEmpty();
		c.get().header(retryAfter((Supplier<?>)null)).run().assertContent().isEmpty();
		c.get().header(retryAfter(()->null)).run().assertContent().isEmpty();
	}

	@Test
	public void a02_asZonedDateTime() throws Exception {
		assertObject(retryAfter(PARSED2).asZonedDateTime().get().toString()).is("1994-10-29T19:43:31Z");
	}

	@Test
	public void a03_asInt() throws Exception {
		assertOptional(retryAfter(123).asInteger()).is(123);
		assertOptional(new RetryAfter((String)null).asInteger()).isNull();
		assertOptional(retryAfter(()->null).asInteger()).isNull();
	}

	//------------------------------------------------------------------------------------------------------------------
	// Helper methods.
	//------------------------------------------------------------------------------------------------------------------

	private static RestClient.Builder client() {
		return MockRestClient.create(A.class);
	}
}
