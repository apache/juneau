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

import java.io.*;
import java.time.*;
import java.util.function.*;

import org.apache.juneau.http.annotation.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.client.*;
import org.apache.juneau.rest.mock.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class IfRange_Test {

	private static final String HEADER = "If-Range";
	private static final String VALUE1 = "\"foo\"";
	private static final String VALUE2 = "W/\"foo\"";
	private static final String VALUE3 = "Sat, 29 Oct 1994 19:43:31 GMT";
	private static final EntityTag PARSED1 = EntityTag.of(VALUE1);
	private static final EntityTag PARSED2 = EntityTag.of(VALUE2);
	private static final ZonedDateTime PARSED3 = ZonedDateTime.from(RFC_1123_DATE_TIME.parse(VALUE3)).truncatedTo(SECONDS);

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

		// Normal usage.
		c.get().header(ifRange(VALUE1)).run().assertBody().is(VALUE1);
		c.get().header(ifRange(VALUE1)).run().assertBody().is(VALUE1);
		c.get().header(ifRange(PARSED1)).run().assertBody().is(VALUE1);
		c.get().header(ifRange(()->PARSED1)).run().assertBody().is(VALUE1);

		c.get().header(ifRange(VALUE2)).run().assertBody().is(VALUE2);
		c.get().header(ifRange(VALUE2)).run().assertBody().is(VALUE2);
		c.get().header(ifRange(PARSED2)).run().assertBody().is(VALUE2);
		c.get().header(ifRange(()->PARSED2)).run().assertBody().is(VALUE2);

		c.get().header(ifRange(VALUE3)).run().assertBody().is(VALUE3);
		c.get().header(ifRange(VALUE3)).run().assertBody().is(VALUE3);
		c.get().header(ifRange(PARSED3)).run().assertBody().is(VALUE3);
		c.get().header(ifRange(()->PARSED3)).run().assertBody().is(VALUE3);

		// Invalid usage.
		c.get().header(ifRange((String)null)).run().assertBody().isEmpty();
		c.get().header(ifRange((Supplier<ZonedDateTime>)null)).run().assertBody().isEmpty();
		c.get().header(ifRange(()->null)).run().assertBody().isEmpty();
	}

	@Test
	public void a02_asEntityTag() throws Exception {
		EntityTag x = ifRange(VALUE1).asEntityTag().get();
		assertString(x).is("\"foo\"");
		assertObject(ifRange(()->null).asEntityTag()).isNull();
		assertObject(ifRange(()->PARSED3).asEntityTag()).isNull();
	}

	//------------------------------------------------------------------------------------------------------------------
	// Helper methods.
	//------------------------------------------------------------------------------------------------------------------

	private static RestClientBuilder client() {
		return MockRestClient.create(A.class);
	}
}
