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

import static org.apache.juneau.http.header.BasicEntityTagArrayHeader.*;

import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class BasicEntityTagArrayHeader_Test {


	private static final String HEADER = "Foo";

	@Rest
	public static class A {
		@RestOp
		public StringReader get(@Header(name=HEADER,multi=true,allowEmptyValue=true) String[] h) {
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

		c.get().header(of(HEADER,"\"foo\"")).run().assertBody().is("\"foo\"");
		c.get().header(of(HEADER,()->"\"foo\"")).run().assertBody().is("\"foo\"");

		c.get().header(of(HEADER,()->null)).run().assertBody().isEmpty();

		c.get().header(of(HEADER,"\"foo\",\"bar\"")).run().assertBody().is("\"foo\",\"bar\"");
		c.get().header(of(HEADER,()->"\"foo\",\"bar\"")).run().assertBody().is("\"foo\",\"bar\"");

		c.get().header(of(HEADER,AList.of("\"foo\"","\"bar\""))).run().assertBody().is("\"foo\",\"bar\"");
		c.get().header(of(HEADER,()->AList.of("\"foo\"","\"bar\""))).run().assertBody().is("\"foo\",\"bar\"");

		EntityTag[] x1 = new EntityTag[]{EntityTag.of("\"foo\""),EntityTag.of("\"bar\"")};
		c.get().header(of(HEADER,x1)).run().assertBody().is("\"foo\",\"bar\"");
		c.get().header(of(HEADER,()->x1)).run().assertBody().is("\"foo\",\"bar\"");

		String[] x2 = {"\"foo\"","\"bar\""};
		c.get().header(of(HEADER,x2)).run().assertBody().is("\"foo\",\"bar\"");
		c.get().header(of(HEADER,()->x2)).run().assertBody().is("\"foo\",\"bar\"");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Helper methods.
	//------------------------------------------------------------------------------------------------------------------

	private static RestClientBuilder client() {
		return MockRestClient.create(A.class);
	}
}
