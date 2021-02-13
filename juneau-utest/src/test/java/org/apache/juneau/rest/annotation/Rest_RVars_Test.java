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
import static org.junit.runners.MethodSorters.*;

import java.io.IOException;

import org.apache.juneau.*;
import org.apache.juneau.rest.RestResponse;
import org.apache.juneau.rest.client.*;
import org.apache.juneau.rest.mock.*;
import org.apache.juneau.serializer.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class Rest_RVars_Test {

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
			res.attr("A2", "c");
			res.attr("B2", "c");
			res.attr("C", "c");
			res.setOutput(null);
		}

		public static class A1 extends WriterSerializer {
			public A1(ContextProperties cp) {
				super(cp, "text/plain", "*/*");
			}
			@Override /* Serializer */
			public WriterSerializerSession createSession(SerializerSessionArgs args) {
				return new WriterSerializerSession(args) {
					@Override /* SerializerSession */
					protected void doSerialize(SerializerPipe out, Object o) throws IOException, SerializeException {
						SessionProperties sp = getSessionProperties();
						out.getWriter().write(format("A1=%s,A2=%s,B1=%s,B2=%s,C=%s,R1a=%s,R1b=%s,R2=%s,R3=%s,R4=%s,R5=%s,R6=%s",
							sp.get("A1").orElse(null), sp.get("A2").orElse(null), sp.get("B1").orElse(null), sp.get("B2").orElse(null), sp.get("C").orElse(null),
							sp.get("R1a").orElse(null), sp.get("R1b").orElse(null), sp.get("R2").orElse(null), sp.get("R3").orElse(null), sp.get("R4").orElse(null), sp.get("R5").orElse(null), sp.get("R6").orElse(null)));
					}
				};
			}
		}
	}

	@Test
	public void a01_basic() throws Exception {
		RestClient a = MockRestClient.build(A.class);
		a.get("/p2").accept("text/plain").run().assertBody().is("A1=a1,A2=c,B1=b1,B2=c,C=c,R1a=/p1/p2,R1b=/p1,R2=bar,R3=,R4=a1,R5=c,R6=c");
	}
}
