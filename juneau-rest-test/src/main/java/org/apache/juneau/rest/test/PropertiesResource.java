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

import static java.lang.String.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.serializer.*;

/**
 * JUnit automated testcase resource.
 */
@RestResource(
	path="/testProperties",
	properties={
		@Property(name="A1",value="a1"),
		@Property(name="A2",value="a2"),
		@Property(name="foo",value="bar"),
		@Property(name="bar",value="baz"),
		@Property(name="R1a",value="$R{requestURI}"),
		@Property(name="R1b",value="$R{requestParentURI}"),
		@Property(name="R2",value="$R{foo}"),
		@Property(name="R3",value="$R{$R{foo}}"),
		@Property(name="R4",value="$R{A1}"),
		@Property(name="R5",value="$R{A2}"),
		@Property(name="R6",value="$R{C}"),
	}
)
public class PropertiesResource extends RestServletDefault {
	private static final long serialVersionUID = 1L;

	//====================================================================================================
	// Properties defined on method.
	//====================================================================================================
	@RestMethod(name="GET", path="/testPropertiesDefinedOnMethod",
		properties={
			@Property(name="B1",value="b1"),
			@Property(name="B2",value="b2")
		},
		serializers=PropertySerializer1.class
	)
	public void testPropertiesDefinedOnMethod(RestResponse res) {
		res.setProperty("A2", "c");
		res.setProperty("B2", "c");
		res.setProperty("C", "c");
		res.setOutput(null);
	}

	@Produces("application/json,text/json")
	public static class PropertySerializer1 extends WriterSerializer {

		public PropertySerializer1(PropertyStore propertyStore) {
			super(propertyStore);
		}

		@Override /* Serializer */
		protected void doSerialize(SerializerSession session, Object output) throws Exception {
			session.getWriter().write(format("A1=%s,A2=%s,B1=%s,B2=%s,C=%s,R1a=%s,R1b=%s,R2=%s,R3=%s,R4=%s,R5=%s,R6=%s",
				session.getProperty("A1"), session.getProperty("A2"), session.getProperty("B1"), session.getProperty("B2"), session.getProperty("C"),
				session.getProperty("R1a"), session.getProperty("R1b"), session.getProperty("R2"), session.getProperty("R3"), session.getProperty("R4"), session.getProperty("R5"), session.getProperty("R6")));
		}
	}

	//====================================================================================================
	// Make sure attributes/parameters/headers are available through ctx.getProperties().
	//====================================================================================================
	@RestMethod(name="GET", path="/testProperties/{A}", serializers=PropertySerializer2.class)
	public void testProperties(RestResponse res) {
		res.setOutput(null);
	}

	@Produces("application/json,text/json")
	public static class PropertySerializer2 extends WriterSerializer {

		public PropertySerializer2(PropertyStore propertyStore) {
			super(propertyStore);
		}

		@Override /* Serializer */
		protected void doSerialize(SerializerSession session, Object output) throws Exception {
			session.getWriter().write(format("A=%s,P=%s,H=%s", session.getProperty("A"), session.getProperty("P"), session.getProperty("h")));
		}
	}
}
