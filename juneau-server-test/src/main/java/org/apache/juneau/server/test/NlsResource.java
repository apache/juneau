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
package org.apache.juneau.server.test;

import org.apache.juneau.server.*;
import org.apache.juneau.server.annotation.*;
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
		label="Test1.a",
		description="Test1.b"
	)
	public static class Test1 extends RestServletDefault {

		@RestMethod(
			name="POST", path="/{a}",
			description="Test1.c",
			input={
				@Var(category="attr", name="a", description="Test1.d"),
				@Var(category="param", name="b", description="Test1.e"),
				@Var(category="content", description="Test1.f"),
				@Var(category="header", name="D", description="Test1.g"),
				@Var(category="attr", name="a2", description="Test1.h"),
				@Var(category="param", name="b2", description="Test1.i"),
				@Var(category="header", name="D2", description="Test1.j"),
				@Var(category="foo", name="bar", description="Test1.k"),
			},
			responses={
				@Response(200),
				@Response(value=201,
					description="Test1.l",
					output={
						@Var(category="foo", name="bar", description="Test1.m"),
					}
				)
			}
		)
		public String test1(@Attr("a") String a, @Param("b") String b, @Content String c, @Header("D") String d,
				@Attr("e") String e, @Param("f") String f, @Header("g") String g) {
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
			name="POST", path="/{a}"
		)
		public String test2(@Attr("a") String a, @Param("b") String b, @Content String c, @Header("D") String d,
				@Attr("e") String e, @Param("f") String f, @Header("g") String g) {
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
			name="POST", path="/{a}"
		)
		public String test3(@Attr("a") String a, @Param("b") String b, @Content String c, @Header("D") String d,
				@Attr("e") String e, @Param("f") String f, @Header("g") String g) {
			return null;
		}

		@RestMethod(
			name="GET", path="/"
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
			name="POST", path="/{a}"
		)
		public String test4(@Attr("a") String a, @Param("b") String b, @Content String c, @Header("D") String d,
				@Attr("e") String e, @Param("f") String f, @Header("g") String g) {
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
			name="POST", path="/{a}"
		)
		public String test5(@Attr("a") String a, @Param("b") String b, @Content String c, @Header("D") String d,
				@Attr("e") String e, @Param("f") String f, @Header("g") String g) {
			return null;
		}
	}

	//====================================================================================================
	// test6 - Pull labels from annotations only, but annotations contain variables.
	//====================================================================================================
	@RestResource(
		path="/test6",
		messages="NlsResource",
		label="$L{foo}",
		description="$L{foo}"
	)
	public static class Test6 extends RestServletDefault {

		@RestMethod(
			name="POST", path="/{a}",
			description="$L{foo}",
			input={
				@Var(category="attr", name="a", description="$L{foo}"),
				@Var(category="param", name="b", description="$L{foo}"),
				@Var(category="content", description="$L{foo}"),
				@Var(category="header", name="D", description="$L{foo}"),
				@Var(category="attr", name="a2", description="$L{foo}"),
				@Var(category="param", name="b2", description="$L{foo}"),
				@Var(category="header", name="D2", description="$L{foo}"),
				@Var(category="foo", name="bar", description="$L{foo}"),
			},
			responses={
				@Response(200),
				@Response(value=201,
					description="$L{foo}",
					output={
						@Var(category="foo", name="bar", description="$L{foo}"),
					}
				)
			}
		)
		public String test6(@Attr("a") String a, @Param("b") String b, @Content String c, @Header("D") String d,
				@Attr("e") String e, @Param("f") String f, @Header("g") String g) {
			return null;
		}
	}
}
