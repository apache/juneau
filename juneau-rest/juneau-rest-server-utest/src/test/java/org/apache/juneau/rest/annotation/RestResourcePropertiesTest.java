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
import static org.apache.juneau.http.HttpMethodName.*;

import java.io.IOException;

import org.apache.juneau.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.mock2.*;
import org.apache.juneau.serializer.*;
import org.junit.*;
import org.junit.runners.*;

/**
 * Tests that validate the behavior of @RestResource(properties).
 */
@SuppressWarnings({"javadoc"})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class RestResourcePropertiesTest {

	//=================================================================================================================
	// Basic tests
	//=================================================================================================================

	@RestResource(
		path="/p1",
		attrs={
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

		@RestMethod(name=GET, path="/p2",
			attrs={"B1: b1", "B2:b"},
			serializers=A01.class
		)
		public void testPropertiesDefinedOnMethod(RestResponse res) {
			res.attr("A2", "c");
			res.attr("B2", "c");
			res.attr("C", "c");
			res.setOutput(null);
		}

		public static class A01 extends WriterSerializer {
			public A01(PropertyStore ps) {
				super(ps, "text/plain", "*/*");
			}
			@Override /* Serializer */
			public WriterSerializerSession createSession(SerializerSessionArgs args) {
				return new WriterSerializerSession(args) {
					@Override /* SerializerSession */
					protected void doSerialize(SerializerPipe out, Object o) throws IOException, SerializeException {
						out.getWriter().write(format("A1=%s,A2=%s,B1=%s,B2=%s,C=%s,R1a=%s,R1b=%s,R2=%s,R3=%s,R4=%s,R5=%s,R6=%s",
							getProperty("A1"), getProperty("A2"), getProperty("B1"), getProperty("B2"), getProperty("C"),
							getProperty("R1a"), getProperty("R1b"), getProperty("R2"), getProperty("R3"), getProperty("R4"), getProperty("R5"), getProperty("R6")));
					}
				};
			}
		}
	}
	static MockRest a = MockRest.build(A.class);

	@Test
	public void a01() throws Exception {
		a.get("/p2").accept("text/plain").execute().assertBody("A1=a1,A2=c,B1=b1,B2=c,C=c,R1a=/p2,R1b=/,R2=bar,R3=,R4=a1,R5=c,R6=c");
	}
}
