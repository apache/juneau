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

import static org.apache.juneau.http.header.Connection.*;

import java.io.*;
import java.util.function.*;

import org.apache.juneau.http.annotation.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.client.*;
import org.apache.juneau.rest.mock.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class Connection_Test {

	private static final String HEADER = "Connection";
	private static final String VALUE = "foo";

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

		c.get().header(of((String)null)).run().assertBody().is("Keep-Alive");
		c.get().header(of((Object)null)).run().assertBody().is("Keep-Alive");
		c.get().header(of((Supplier<?>)null)).run().assertBody().is("Keep-Alive");
		c.get().header(of(()->null)).run().assertBody().is("Keep-Alive");
		c.get().header(of(VALUE)).run().assertBody().is(VALUE);
		c.get().header(of(VALUE)).run().assertBody().is(VALUE);
		c.get().header(of(new StringBuilder(VALUE))).run().assertBody().is(VALUE);
		c.get().header(of(()->VALUE)).run().assertBody().is(VALUE);
		c.get().header(new Connection(VALUE)).run().assertBody().is(VALUE);
	}

	@Test
	public void a02_other() throws Exception {
		assertTrue(of("Keep-Alive").isKeepAlive());
		assertTrue(of("keep-alive").isKeepAlive());
		assertFalse(of("foo").isKeepAlive());
		assertTrue(of("Close").isClose());
		assertTrue(of("close").isClose());
		assertFalse(of("foo").isClose());
		assertTrue(of("Upgrade").isUpgrade());
		assertTrue(of("upgrade").isUpgrade());
		assertFalse(of("foo").isUpgrade());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Helper methods.
	//------------------------------------------------------------------------------------------------------------------

	private static RestClientBuilder client() {
		return MockRestClient.create(A.class);
	}
}
