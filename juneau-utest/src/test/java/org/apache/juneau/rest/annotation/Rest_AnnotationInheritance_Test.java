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

import static org.junit.runners.MethodSorters.*;

import org.apache.juneau.http.annotation.Content;
import org.apache.juneau.http.annotation.Header;
import org.apache.juneau.http.annotation.Query;
import org.apache.juneau.json.*;
import org.apache.juneau.rest.client.*;
import org.apache.juneau.rest.mock.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class Rest_AnnotationInheritance_Test {

	//------------------------------------------------------------------------------------------------------------------
	// @Body on parameter
	//------------------------------------------------------------------------------------------------------------------

	@Rest(serializers=Json5Serializer.class, parsers=JsonParser.class, defaultAccept="text/json")
	public static interface IA {
		@RestPut
		public String a(@Content String b);
		@RestGet
		public String b(@Query("foo") String b);
		@RestGet
		public String c(@Header("foo") String b);
	}

	public static class A implements IA {
		@Override
		public String a(String b) {
			return b;
		}
		@Override
		public String b(String b) {
			return b;
		}
		@Override
		public String c(String b) {
			return b;
		}
	}

	@Test
	public void a01_inheritedFromInterface() throws Exception {
		RestClient a = MockRestClient.build(A.class);
		a.put("/a", "foo").json().run().assertContent("'foo'");
		a.get("/b").queryData("foo", "bar").json().run().assertContent("'bar'");
		a.get("/c").header("foo", "bar").json().run().assertContent("'bar'");
	}
}
