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
package org.apache.juneau.rest.test;

import static org.apache.juneau.http.HttpMethodName.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.serializer.*;

/**
 * JUnit automated testcase resource.
 */
@RestResource(
	path="/testNlsProperty",
	serializers={NlsPropertyResource.TestSerializer.class},
	properties={
		@Property(name="TestProperty",value="$L{key1}")
	},
	messages="NlsPropertyResource"
)
public class NlsPropertyResource extends RestServlet {
	private static final long serialVersionUID = 1L;

	//====================================================================================================
	// Test getting an NLS property defined on a class.
	//====================================================================================================
	@RestMethod(name=GET, path="/testInheritedFromClass")
	public String testInheritedFromClass() {
		return null;
	}

	//====================================================================================================
	// Test getting an NLS property defined on a method.
	//====================================================================================================
	@RestMethod(name=GET, path="/testInheritedFromMethod",
		properties={
			@Property(name="TestProperty",value="$L{key2}")
		}
	)
	public String testInheritedFromMethod() {
		return null;
	}

	public static class TestSerializer extends WriterSerializer {

		public TestSerializer(PropertyStore ps) {
			super(ps, "text/plain");
		}

		@Override /* Serializer */
		public WriterSerializerSession createSession(SerializerSessionArgs args) {
			return new WriterSerializerSession(args) {

				@Override /* SerializerSession */
				protected void doSerialize(SerializerPipe out, Object o) throws Exception {
					out.getWriter().write(getProperty("TestProperty", String.class));
				}
			};
		}
	}
}
