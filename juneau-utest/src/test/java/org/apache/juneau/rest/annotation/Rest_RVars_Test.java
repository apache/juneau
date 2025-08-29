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
package org.apache.juneau.rest.annotation;

import static java.lang.String.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.mock.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.utest.utils.*;
import org.junit.jupiter.api.*;

class Rest_RVars_Test extends SimpleTestBase {

	//------------------------------------------------------------------------------------------------------------------
	// Basic tests
	//------------------------------------------------------------------------------------------------------------------

	@Rest(
		path="/p1",
		defaultRequestAttributes={
			"A1: a1",
			"A2: a2",
			"foo: bar",
			"bar: baz",
			"R1a: $R{requestURI}",
			"R1b: $R{requestParentURI}",
			"R2: $R{foo}",
			"R3: $R{$R{foo}}",
			"R4: $R{A1}",
			"R5: $R{A2}",
			"R6: $R{C}",
		}
	)
	public static class A {

		@RestGet(
			path="/p2",
			defaultRequestAttributes={"B1: b1", "B2:b"},
			serializers=A1.class
		)
		public void a(RestResponse res) {
			res.setAttribute("A2", "c");
			res.setAttribute("B2", "c");
			res.setAttribute("C", "c");
			res.setContent(null);
		}

		public static class A1 extends FakeWriterSerializer {
			public A1(FakeWriterSerializer.Builder b) {
				super(b.produces("text/plain").accept("*/*").function((s,o) -> out(s)));
			}
			public static String out(SerializerSession s) {
				var sp = s.getSessionProperties();
				return format("A1=%s,A2=%s,B1=%s,B2=%s,C=%s,R1a=%s,R1b=%s,R2=%s,R3=%s,R4=%s,R5=%s,R6=%s",
					sp.get("A1",null), sp.get("A2",null), sp.get("B1",null), sp.get("B2",null), sp.get("C",null),
					sp.get("R1a",null), sp.get("R1b",null), sp.get("R2",null), sp.get("R3",null), sp.get("R4",null), sp.get("R5",null), sp.get("R6",null));
			}
		}
	}

	@Test void a01_basic() throws Exception {
		var a = MockRestClient.build(A.class);
		a.get("/p2").accept("text/plain").run().assertContent("A1=a1,A2=c,B1=b1,B2=c,C=c,R1a=/p1/p2,R1b=/p1,R2=bar,R3=,R4=a1,R5=a2,R6=");
	}
}
