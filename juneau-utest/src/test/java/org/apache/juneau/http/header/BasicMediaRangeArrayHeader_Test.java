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
import java.util.function.*;

import org.apache.juneau.collections.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.client.*;
import org.apache.juneau.rest.mock.*;

import static org.apache.juneau.assertions.Assertions.*;
import static org.apache.juneau.http.header.BasicMediaRangeArrayHeader.of;

import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class BasicMediaRangeArrayHeader_Test {


	private static final String HEADER = "Foo";

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

		c.get().header(of(null,(Object)null)).run().assertBody().isEmpty();
		c.get().header(of("","*")).run().assertBody().isEmpty();
		c.get().header(of(HEADER,(Object)null)).run().assertBody().isEmpty();
		c.get().header(of(null,"*")).run().assertBody().isEmpty();

		c.get().header(of(null,()->null)).run().assertBody().isEmpty();
		c.get().header(of(HEADER,(Supplier<?>)null)).run().assertBody().isEmpty();
		c.get().header(of(null,(Supplier<?>)null)).run().assertBody().isEmpty();
		c.get().header(of(HEADER,()->null)).run().assertBody().isEmpty();

		c.get().header(new BasicMediaRangeArrayHeader(HEADER,null)).run().assertBody().isEmpty();
		c.get().header(new BasicMediaRangeArrayHeader(HEADER,((Supplier<?>)()->null))).run().assertBody().isEmpty();

		c.get().header(of(HEADER,"foo/bar;x=1")).run().assertBody().is("foo/bar;x=1");

		c.get().header(of(HEADER,MediaRanges.of("foo/bar;x=1"))).run().assertBody().is("foo/bar;x=1");
	}

	@Test
	public void a02_match() throws Exception {
		assertInteger(Accept.of("text/foo").match(AList.of(MediaType.of("text/foo")))).is(0);
		assertInteger(Accept.of("text/foo").match(AList.of(MediaType.of("text/bar")))).is(-1);
		assertInteger(new Accept((String)null).match(AList.of(MediaType.of("text/bar")))).is(-1);
		assertInteger(Accept.of("text/foo").match(AList.of(MediaType.of(null)))).is(-1);
		assertInteger(Accept.of("text/foo").match(null)).is(-1);
	}

	@Test
	public void a03_getRange() throws Exception {
		assertString(Accept.of("text/foo").getRange(0)).is("text/foo");
		assertString(Accept.of("text/foo").getRange(1)).isNull();
		assertString(Accept.of("text/foo").getRange(-1)).isNull();
		assertString(new Accept((String)null).getRange(0)).isNull();
	}

	@Test
	public void a04_hasSubtypePart() throws Exception {
		assertBoolean(Accept.of("text/foo").hasSubtypePart("foo")).isTrue();
		assertBoolean(Accept.of("text/foo").hasSubtypePart("bar")).isFalse();
		assertBoolean(Accept.of("text/foo").hasSubtypePart(null)).isFalse();
		assertBoolean(new Accept((String)null).hasSubtypePart("foo")).isFalse();
	}

	@Test
	public void a05_getRanges() throws Exception {
		assertObject(Accept.of("text/foo,text/bar").getRanges()).asJson().is("['text/foo','text/bar']");
		assertObject(new Accept((String)null).getRanges()).asJson().is("[]");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Helper methods.
	//------------------------------------------------------------------------------------------------------------------

	private static RestClientBuilder client() {
		return MockRestClient.create(A.class);
	}
}
