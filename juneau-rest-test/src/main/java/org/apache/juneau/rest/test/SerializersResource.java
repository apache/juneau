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

import static org.apache.juneau.rest.annotation.Inherit.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.serializer.*;

/**
 * JUnit automated testcase resource.
 */
@RestResource(
	path="/testSerializers",
	serializers=SerializersResource.TestSerializerA.class
)
public class SerializersResource extends RestServletDefault {
	private static final long serialVersionUID = 1L;

	@Produces("text/a")
	public static class TestSerializerA extends WriterSerializer {

		public TestSerializerA(PropertyStore propertyStore) {
			super(propertyStore);
		}

		@Override /* Serializer */
		public WriterSerializerSession createSession(SerializerSessionArgs args) {
			return new WriterSerializerSession(args) {

				@Override /* SerializerSession */
				protected void doSerialize(SerializerPipe out, Object o) throws Exception {
					out.getWriter().write("text/a - " + o);
				}
			};
		}
	}

	@Produces("text/b")
	public static class TestSerializerB extends WriterSerializer {

		public TestSerializerB(PropertyStore propertyStore) {
			super(propertyStore);
		}

		@Override /* Serializer */
		public WriterSerializerSession createSession(SerializerSessionArgs args) {
			return new WriterSerializerSession(args) {

				@Override /* SerializerSession */
				protected void doSerialize(SerializerPipe out, Object o) throws Exception {
					out.getWriter().write("text/b - " + o);
				}
			};
		}
	}

	//====================================================================================================
	// Serializer defined on class.
	//====================================================================================================
	@RestMethod(name="GET", path="/testSerializerOnClass")
	public String testSerializerOnClass() {
		return "test1";
	}

	//====================================================================================================
	// Serializer defined on method.
	//====================================================================================================
	@RestMethod(name="GET", path="/testSerializerOnMethod", serializers=TestSerializerB.class)
	public String testSerializerOnMethod() {
		return "test2";
	}

	//====================================================================================================
	// Serializer overridden on method.
	//====================================================================================================
	@RestMethod(name="GET", path="/testSerializerOverriddenOnMethod", serializers={TestSerializerB.class,TestSerializerC.class}, serializersInherit=SERIALIZERS)
	public String testSerializerOverriddenOnMethod() {
		return "test3";
	}

	@Produces("text/a")
	public static class TestSerializerC extends WriterSerializer {

		public TestSerializerC(PropertyStore propertyStore) {
			super(propertyStore);
		}

		@Override /* Serializer */
		public WriterSerializerSession createSession(SerializerSessionArgs args) {
			return new WriterSerializerSession(args) {

				@Override /* SerializerSession */
				protected void doSerialize(SerializerPipe out, Object o) throws Exception {
					out.getWriter().write("text/c - " + o);
				}
			};
		}
	}

	//====================================================================================================
	// Serializer with different Accept than Content-Type.
	//====================================================================================================
	@RestMethod(name="GET", path="/testSerializerWithDifferentMediaTypes", serializers={TestSerializerD.class}, serializersInherit=SERIALIZERS)
	public String testSerializerWithDifferentMediaTypes() {
		return "test4";
	}

	@Produces(value="text/a,text/d",contentType="text/d")
	public static class TestSerializerD extends WriterSerializer {

		public TestSerializerD(PropertyStore propertyStore) {
			super(propertyStore);
		}

		@Override /* Serializer */
		public WriterSerializerSession createSession(SerializerSessionArgs args) {
			return new WriterSerializerSession(args) {

				@Override /* SerializerSession */
				protected void doSerialize(SerializerPipe out, Object o) throws Exception {
					out.getWriter().write("text/d - " + o);
				}
			};
		}
	}

	//====================================================================================================
	// Check for valid 406 error response.
	//====================================================================================================
	@RestMethod(name="GET", path="/test406")
	public String test406() {
		return "test406";
	}
}
