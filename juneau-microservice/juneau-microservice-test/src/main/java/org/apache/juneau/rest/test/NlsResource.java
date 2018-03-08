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

import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.utils.*;

/**
 * JUnit automated testcase resource.
 */
@RestResource(
	path="/testNls",
	children={
		NlsResource.Test1.class,
		NlsResource.Test2.class,
		NlsResource.Test3.class,
		NlsResource.Test4.class,
		NlsResource.Test5.class,
		NlsResource.Test6.class
	}
)
@SuppressWarnings({"serial"})
public class NlsResource extends BasicRestServletGroup {
	private static final long serialVersionUID = 1L;

	//====================================================================================================
	// test1 - Pull labels from annotations only.
	//====================================================================================================
	@RestResource(
		path="/test1",
		messages="NlsResource",
		title="Test1.a",
		description="Test1.b"
	)
	public static class Test1 extends BasicRestServlet {

		@RestMethod(
			name=POST, path="/{a}",
			description="Test1.c",
			swagger= {
				"parameters:[",
					"{name:'a',in:'path',type:'string',description:'Test1.d'},",
					"{name:'b',in:'query',type:'string',description:'Test1.e'},",
					"{in:'body',type:'string',description:'Test1.f'},",
					"{name:'D',in:'header',type:'string',description:'Test1.g'},",
					"{name:'a2',in:'path',type:'string',description:'Test1.h'},",
					"{name:'b2',in:'query',type:'string',description:'Test1.i'},",
					"{name:'D2',in:'header',type:'string',description:'Test1.j'}",
				"],",
				"responses:{",
					"200: {description:'OK'},",
					"201: {description:'Test1.l',headers:{bar:{description:'Test1.m',type:'string'}}}",
				"}"
			}
		)
		public String test1(@Path("a") String a, @Query("b") String b, @Body String c, @Header("D") String d,
				@Path("e") String e, @Query("f") String f, @Header("g") String g) {
			return null;
		}
	}

	//====================================================================================================
	// test2 - Pull labels from resource bundles only - simple keys.
	//====================================================================================================
	@RestResource(
		path="/test2",
		messages="NlsResource"
	)
	public static class Test2 extends BasicRestServlet {

		@RestMethod(
			name=POST, path="/{a}"
		)
		public String test2(@Path("a") String a, @Query("b") String b, @Body String c, @Header("D") String d,
				@Path("e") String e, @Query("f") String f, @Header("g") String g) {
			return null;
		}
	}

	//====================================================================================================
	// test3 - Pull labels from resource bundles only - keys with class names.
	//====================================================================================================
	@RestResource(
		path="/test3",
		messages="NlsResource"
	)
	public static class Test3 extends BasicRestServlet {

		@RestMethod(
			name=POST, path="/{a}"
		)
		public String test3(@Path("a") String a, @Query("b") String b, @Body String c, @Header("D") String d,
				@Path("e") String e, @Query("f") String f, @Header("g") String g) {
			return null;
		}

		@RestMethod(
			name=GET, path="/"
		)
		public Object test3a(MessageBundle mb) {
			return mb;
		}
	}

	//====================================================================================================
	// test4 - Pull labels from resource bundles only.  Values have localized variables to resolve.
	//====================================================================================================
	@RestResource(
		path="/test4",
		messages="NlsResource"
	)
	public static class Test4 extends BasicRestServlet {

		@RestMethod(
			name=POST, path="/{a}"
		)
		public String test4(@Path("a") String a, @Query("b") String b, @Body String c, @Header("D") String d,
				@Path("e") String e, @Query("f") String f, @Header("g") String g) {
			return null;
		}
	}

	//====================================================================================================
	// test5 - Pull labels from resource bundles only.  Values have request variables to resolve.
	//====================================================================================================
	@RestResource(
		path="/test5",
		messages="NlsResource"
	)
	public static class Test5 extends BasicRestServlet {

		@RestMethod(
			name=POST, path="/{a}"
		)
		public String test5(@Path("a") String a, @Query("b") String b, @Body String c, @Header("D") String d,
				@Path("e") String e, @Query("f") String f, @Header("g") String g) {
			return null;
		}
	}

	//====================================================================================================
	// test6 - Pull labels from annotations only, but annotations contain variables.
	//====================================================================================================
	@RestResource(
		path="/test6",
		messages="NlsResource",
		title="$L{foo}",
		description="$L{foo}"
	)
	public static class Test6 extends BasicRestServlet {

		@RestMethod(
			name=POST, path="/{a}",
			description="$L{foo}",
			swagger= {
				"parameters:[",
					"{name:'a',in:'path',description:'$L{foo}'},",
					"{name:'b',in:'query',description:'$L{foo}'},",
					"{in:'body',description:'$L{foo}'},",
					"{name:'D',in:'header',description:'$L{foo}'},",
					"{name:'a2',in:'path',description:'$L{foo}'},",
					"{name:'b2',in:'query',description:'$L{foo}'},",
					"{name:'D2',in:'header',description:'$L{foo}'}",
				"],",
				"responses:{",
					"200: {description:'OK'},",
					"201: {description:'$L{foo}',headers:{bar:{description:'$L{foo}',type:'string'}}}",
				"}"
			}
		)
		public String test6(@Path("a") String a, @Query("b") String b, @Body String c, @Header("D") String d,
				@Path("e") String e, @Query("f") String f, @Header("g") String g) {
			return null;
		}
	}
}
