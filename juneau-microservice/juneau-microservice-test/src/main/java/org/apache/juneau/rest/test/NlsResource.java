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
public class NlsResource extends RestServletGroupDefault {
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
	public static class Test1 extends RestServletDefault {

		@RestMethod(
			name=POST, path="/{a}",
			description="Test1.c",
			swagger=@MethodSwagger(
				parameters={
					@Parameter(in="path", name="a", description="Test1.d"),
					@Parameter(in="query", name="b", description="Test1.e"),
					@Parameter(in="body", description="Test1.f"),
					@Parameter(in="header", name="D", description="Test1.g"),
					@Parameter(in="path", name="a2", description="Test1.h"),
					@Parameter(in="query", name="b2", description="Test1.i"),
					@Parameter(in="header", name="D2", description="Test1.j"),
				},
				responses={
					@Response(200),
					@Response(value=201,
						description="Test1.l",
						headers={
							@Parameter(in="foo", name="bar", description="Test1.m"),
						}
					)
				}
			)
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
	public static class Test2 extends RestServletDefault {

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
	public static class Test3 extends RestServletDefault {

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
		public Object test3a(@Messages MessageBundle mb) {
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
	public static class Test4 extends RestServletDefault {

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
	public static class Test5 extends RestServletDefault {

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
	public static class Test6 extends RestServletDefault {

		@RestMethod(
			name=POST, path="/{a}",
			description="$L{foo}",
			swagger=@MethodSwagger(
				parameters={
					@Parameter(in="path", name="a", description="$L{foo}"),
					@Parameter(in="query", name="b", description="$L{foo}"),
					@Parameter(in="body", description="$L{foo}"),
					@Parameter(in="header", name="D", description="$L{foo}"),
					@Parameter(in="path", name="a2", description="$L{foo}"),
					@Parameter(in="query", name="b2", description="$L{foo}"),
					@Parameter(in="header", name="D2", description="$L{foo}")
				},
				responses={
					@Response(200),
					@Response(value=201,
						description="$L{foo}",
						headers={
							@Parameter(in="foo", name="bar", description="$L{foo}"),
						}
					)
				}
			)
		)
		public String test6(@Path("a") String a, @Query("b") String b, @Body String c, @Header("D") String d,
				@Path("e") String e, @Query("f") String f, @Header("g") String g) {
			return null;
		}
	}
}
