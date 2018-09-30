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

import static org.apache.juneau.http.HttpMethodName.*;

import org.apache.juneau.http.annotation.*;
import org.apache.juneau.json.*;
import org.apache.juneau.rest.mock.*;
import org.junit.*;
import org.junit.runners.*;

/**
 * Tests inheritance of annotations from interfaces.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@SuppressWarnings("javadoc")
public class AnnotationInheritanceTest {

	//=================================================================================================================
	// @Body on parameter
	//=================================================================================================================

	@RestResource(serializers=SimpleJsonSerializer.class, parsers=JsonParser.class, defaultAccept="text/json")
	public static interface IA {
		@RestMethod(name=PUT, path="/a01")
		public String a01(@Body String b);

		@RestMethod(name=GET, path="/a02")
		public String a02(@Query("foo") String b);

		@RestMethod(name=GET, path="/a03")
		public String a03(@Header("foo") String b);
	}

	public static class A implements IA {

		@Override
		public String a01(String b) {
			return b;
		}

		@Override
		public String a02(String b) {
			return b;
		}

		@Override
		public String a03(String b) {
			return b;
		}
	}

	private static MockRest a = MockRest.create(A.class);

	@Test
	public void a01_inherited_Body() throws Exception {
		a.put("/a01", "'foo'").json().execute().assertBody("'foo'");
	}
	@Test
	public void a02_inherited_Query() throws Exception {
		a.get("/a02").query("foo", "bar").json().execute().assertBody("'bar'");
	}
	@Test
	public void a03_inherited_Header() throws Exception {
		a.get("/a03").header("foo", "bar").json().execute().assertBody("'bar'");
	}
}
