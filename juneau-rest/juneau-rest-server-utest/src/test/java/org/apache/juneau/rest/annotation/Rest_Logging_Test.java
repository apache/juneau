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

import java.util.*;
import java.util.stream.*;

import org.apache.juneau.marshall.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.RestRequest;
import org.apache.juneau.rest.RestResponse;
import org.apache.juneau.rest.client.*;
import org.apache.juneau.rest.mock.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class Rest_Logging_Test {

	private static String string(RestCallLoggerConfig config) {
		return SimpleJson.DEFAULT.toString(config);
	}
	private static String string(Collection<RestCallLoggerRule> rules) {
		return rules.stream().map(x -> x.toString()).collect(Collectors.joining(","));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Default logger config
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class A  {
		@RestMethod
		public String get(RestRequest req) {
			return string(req.getCallLoggerConfig());
		}
	}

	@Test
	public void a01_default() throws Exception {
		RestClient a = MockRestClient.build(A.class);
		a.get("/").run().assertBody().is("{}");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Level
	//------------------------------------------------------------------------------------------------------------------

	@Rest(logging=@Logging(level="WARNING"))
	public static class B1 {
		@RestMethod
		public String a(RestRequest req) {
			return string(req.getCallLoggerConfig());
		}
		@RestMethod(logging=@Logging(level="SEVERE"))
		public String b(RestRequest req) {
			return string(req.getCallLoggerConfig());
		}
		@RestMethod(logging=@Logging(level="SEVERE"))
		public String e(RestRequest req) {
			return string(req.getCallLoggerConfig());
		}
	}

	@Rest
	public static class B2 {
		@RestMethod
		public String c(RestRequest req) {
			return string(req.getCallLoggerConfig());
		}
		@RestMethod(logging=@Logging(level="SEVERE"))
		public String d(RestRequest req) {
			return string(req.getCallLoggerConfig());
		}
	}

	public static class B3 extends B1 {
		@Override
		public String a(RestRequest req) {
			return string(req.getCallLoggerConfig());
		}
		@Override
		public String b(RestRequest req) {
			return string(req.getCallLoggerConfig());
		}
		@Override
		@RestMethod(logging=@Logging(level="OFF"))
		public String e(RestRequest req) {
			return string(req.getCallLoggerConfig());
		}
		@RestMethod
		public String f(RestRequest req) {
			return string(req.getCallLoggerConfig());
		}
		@RestMethod(logging=@Logging(level="SEVERE"))
		public String g(RestRequest req) {
			return string(req.getCallLoggerConfig());
		}
	}

	@Test
	public void b01_logging() throws Exception {
		RestClient b1 = MockRestClient.build(B1.class);
		b1.get("/a").run().assertBody().is("{level:'WARNING'}");
		b1.get("/b").run().assertBody().is("{level:'SEVERE'}");

		RestClient b2 = MockRestClient.build(B2.class);
		b2.get("/c").run().assertBody().is("{}");
		b2.get("/d").run().assertBody().is("{level:'SEVERE'}");

		RestClient b3 = MockRestClient.build(B3.class);
		b3.get("/a").run().assertBody().is("{level:'WARNING'}");
		b3.get("/b").run().assertBody().is("{level:'SEVERE'}");
		b3.get("/e").run().assertBody().is("{level:'OFF'}");
		b3.get("/f").run().assertBody().is("{level:'WARNING'}");
		b3.get("/g").run().assertBody().is("{level:'SEVERE'}");
	}

	//------------------------------------------------------------------------------------------------------------------
	// useStackTraceHashing
	//------------------------------------------------------------------------------------------------------------------

	@Rest(logging=@Logging(useStackTraceHashing="true"))
	public static class C1 {
		@RestMethod
		public String a(RestRequest req) {
			return string(req.getCallLoggerConfig());
		}
		@RestMethod(logging=@Logging(useStackTraceHashing="false"))
		public String b(RestRequest req) {
			return string(req.getCallLoggerConfig());
		}
		@RestMethod(logging=@Logging(useStackTraceHashing="false"))
		public String f(RestRequest req) {
			return string(req.getCallLoggerConfig());
		}
	}

	@Rest
	public static class C2 {
		@RestMethod
		public String c(RestRequest req) {
			return string(req.getCallLoggerConfig());
		}
		@RestMethod(logging=@Logging(useStackTraceHashing="true"))
		public String d(RestRequest req) {
			return string(req.getCallLoggerConfig());
		}
		@RestMethod(logging=@Logging(useStackTraceHashing="foo"))
		public String e(RestRequest req) {
			return string(req.getCallLoggerConfig());
		}
	}

	public static class C3 extends C1 {
		@Override
		public String a(RestRequest req) {
			return string(req.getCallLoggerConfig());
		}
		@Override
		public String b(RestRequest req) {
			return string(req.getCallLoggerConfig());
		}
		@Override
		@RestMethod(logging=@Logging(useStackTraceHashing="TRUE"))
		public String f(RestRequest req) {
			return string(req.getCallLoggerConfig());
		}
		@RestMethod
		public String g(RestRequest req) {
			return string(req.getCallLoggerConfig());
		}
		@RestMethod(logging=@Logging(useStackTraceHashing="FALSE"))
		public String h(RestRequest req) {
			return string(req.getCallLoggerConfig());
		}
	}
	@Rest(logging=@Logging(useStackTraceHashing="foo"))
	public static class C4 {
		@RestMethod(path="i")
		public String i(RestRequest req) {
			return string(req.getCallLoggerConfig());
		}
	}

	@Test
	public void c01_useStackTraceHashing() throws Exception {
		RestClient c1 = MockRestClient.build(C1.class);
		c1.get("/a").run().assertBody().is("{useStackTraceHashing:true}");
		c1.get("/b").run().assertBody().is("{}");

		RestClient c2 = MockRestClient.build(C2.class);
		c2.get("/c").run().assertBody().is("{}");
		c2.get("/d").run().assertBody().is("{useStackTraceHashing:true}");
		c2.get("/e").run().assertBody().is("{}");

		RestClient c3 = MockRestClient.build(C3.class);
		c3.get("/a").run().assertBody().is("{useStackTraceHashing:true}");
		c3.get("/b").run().assertBody().is("{}");
		c3.get("/f").run().assertBody().is("{useStackTraceHashing:true}");
		c3.get("/g").run().assertBody().is("{useStackTraceHashing:true}");
		c3.get("/h").run().assertBody().is("{}");

		RestClient c4 = MockRestClient.build(C4.class);
		c4.get("/i").run().assertBody().is("{}");
	}

	//------------------------------------------------------------------------------------------------------------------
	// stackTraceHashingTimeout
	//------------------------------------------------------------------------------------------------------------------

	@Rest(logging=@Logging(stackTraceHashingTimeout="1"))
	public static class D1 {
		@RestMethod
		public String a(RestRequest req) {
			return string(req.getCallLoggerConfig());
		}
		@RestMethod(logging=@Logging(stackTraceHashingTimeout="2"))
		public String b(RestRequest req) {
			return string(req.getCallLoggerConfig());
		}
		@RestMethod(logging=@Logging(stackTraceHashingTimeout="3"))
		public String e(RestRequest req) {
			return string(req.getCallLoggerConfig());
		}
	}

	@Rest
	public static class D2 {
		@RestMethod
		public String c(RestRequest req) {
			return string(req.getCallLoggerConfig());
		}
		@RestMethod(logging=@Logging(stackTraceHashingTimeout="4"))
		public String d(RestRequest req) {
			return string(req.getCallLoggerConfig());
		}
	}

	public static class D3 extends D1 {
		@Override
		public String a(RestRequest req) {
			return string(req.getCallLoggerConfig());
		}
		@Override
		public String b(RestRequest req) {
			return string(req.getCallLoggerConfig());
		}
		@Override
		@RestMethod(logging=@Logging(stackTraceHashingTimeout="5"))
		public String e(RestRequest req) {
			return string(req.getCallLoggerConfig());
		}
		@RestMethod
		public String f(RestRequest req) {
			return string(req.getCallLoggerConfig());
		}
		@RestMethod(logging=@Logging(stackTraceHashingTimeout="6"))
		public String g(RestRequest req) {
			return string(req.getCallLoggerConfig());
		}
	}


	@Test
	public void d01_stackTraceHashingTimeout() throws Exception {
		RestClient d1 = MockRestClient.build(D1.class);
		d1.get("/a").run().assertBody().is("{stackTraceHashingTimeout:1}");
		d1.get("/b").run().assertBody().is("{stackTraceHashingTimeout:2}");

		RestClient d2 = MockRestClient.build(D2.class);
		d2.get("/c").run().assertBody().is("{}");
		d2.get("/d").run().assertBody().is("{stackTraceHashingTimeout:4}");

		RestClient d3 = MockRestClient.build(D3.class);
		d3.get("/a").run().assertBody().is("{stackTraceHashingTimeout:1}");
		d3.get("/b").run().assertBody().is("{stackTraceHashingTimeout:2}");
		d3.get("/e").run().assertBody().is("{stackTraceHashingTimeout:5}");
		d3.get("/f").run().assertBody().is("{stackTraceHashingTimeout:1}");
		d3.get("/g").run().assertBody().is("{stackTraceHashingTimeout:6}");
	}

	//------------------------------------------------------------------------------------------------------------------
	// noTrace
	//------------------------------------------------------------------------------------------------------------------

	@Rest(logging=@Logging(disabled="true"))
	public static class E1 {
		@RestMethod
		public String a(RestRequest req) {
			return string(req.getCallLoggerConfig());
		}
		@RestMethod(logging=@Logging(disabled="per-request"))
		public String b(RestRequest req) {
			return string(req.getCallLoggerConfig());
		}
		@RestMethod(logging=@Logging(disabled="per-request"))
		public String f(RestRequest req) {
			return string(req.getCallLoggerConfig());
		}
	}

	@Rest
	public static class E2 {
		@RestMethod
		public String c(RestRequest req) {
			return string(req.getCallLoggerConfig());
		}
		@RestMethod(logging=@Logging(disabled="true"))
		public String d(RestRequest req) {
			return string(req.getCallLoggerConfig());
		}
		@RestMethod(logging=@Logging(disabled="foo"))
		public String e(RestRequest req) {
			return string(req.getCallLoggerConfig());
		}
	}

	public static class E3 extends E1 {
		@Override
		public String a(RestRequest req) {
			return string(req.getCallLoggerConfig());
		}
		@Override
		public String b(RestRequest req) {
			return string(req.getCallLoggerConfig());
		}
		@Override
		@RestMethod(logging=@Logging(disabled="false"))
		public String f(RestRequest req) {
			return string(req.getCallLoggerConfig());
		}
		@RestMethod
		public String g(RestRequest req) {
			return string(req.getCallLoggerConfig());
		}
		@RestMethod(logging=@Logging(disabled="per-request"))
		public String h(RestRequest req) {
			return string(req.getCallLoggerConfig());
		}
	}
	@Rest(logging=@Logging(disabled="foo"))
	public static class E4 {
		@RestMethod
		public String i(RestRequest req) {
			return string(req.getCallLoggerConfig());
		}
	}

	@Test
	public void e01_noTrace() throws Exception {
		RestClient e1 = MockRestClient.build(E1.class);
		e1.get("/a").run().assertBody().is("{disabled:'TRUE'}");
		e1.get("/b").run().assertBody().is("{disabled:'PER_REQUEST'}");

		RestClient e2 = MockRestClient.build(E2.class);
		e2.get("/c").run().assertBody().is("{}");
		e2.get("/d").run().assertBody().is("{disabled:'TRUE'}");
		e2.get("/e").run().assertBody().is("{}");

		RestClient e3 = MockRestClient.build(E3.class);
		e3.get("/a").run().assertBody().is("{disabled:'TRUE'}");
		e3.get("/b").run().assertBody().is("{disabled:'PER_REQUEST'}");
		e3.get("/f").run().assertBody().is("{}");
		e3.get("/g").run().assertBody().is("{disabled:'TRUE'}");
		e3.get("/h").run().assertBody().is("{disabled:'PER_REQUEST'}");

		RestClient e4 = MockRestClient.build(E4.class);
		e4.get("/i").run().assertBody().is("{}");
	}

	//------------------------------------------------------------------------------------------------------------------
	// rules
	//------------------------------------------------------------------------------------------------------------------

	@Rest(logging=@Logging(rules=@LoggingRule(codes="1")))
	public static class F1 {
		@RestMethod
		public String a(RestRequest req) {
			return string(req.getCallLoggerConfig().getRules());
		}
		@RestMethod(logging=@Logging(rules=@LoggingRule(codes="2")))
		public String b(RestRequest req) {
			return string(req.getCallLoggerConfig().getRules());
		}
		@RestMethod(logging=@Logging(rules=@LoggingRule(codes="3")))
		public String e(RestRequest req) {
			return string(req.getCallLoggerConfig().getRules());
		}
	}

	@Rest
	public static class F2 {
		@RestMethod
		public String c(RestRequest req) {
			return string(req.getCallLoggerConfig().getRules());
		}
		@RestMethod(logging=@Logging(rules=@LoggingRule(codes="4")))
		public String d(RestRequest req) {
			return string(req.getCallLoggerConfig().getRules());
		}
	}

	public static class F3 extends F1 {
		@Override
		public String a(RestRequest req) {
			return string(req.getCallLoggerConfig().getRules());
		}
		@Override
		public String b(RestRequest req) {
			return string(req.getCallLoggerConfig().getRules());
		}
		@Override
		@RestMethod(logging=@Logging(rules=@LoggingRule(codes="5")))
		public String e(RestRequest req) {
			return string(req.getCallLoggerConfig().getRules());
		}
		@RestMethod
		public String f(RestRequest req) {
			return string(req.getCallLoggerConfig().getRules());
		}
		@RestMethod(logging=@Logging(rules=@LoggingRule(codes="6")))
		public String g(RestRequest req) {
			return string(req.getCallLoggerConfig().getRules());
		}
	}

	@Test
	public void f01_rules() throws Exception {
		RestClient f1 = MockRestClient.build(F1.class);
		f1.get("/a").run().assertBody().is("{codes:'1'}");
		f1.get("/b").run().assertBody().is("{codes:'2'},{codes:'1'}");

		RestClient f2 = MockRestClient.build(F2.class);
		f2.get("/c").run().assertBody().is("");
		f2.get("/d").run().assertBody().is("{codes:'4'}");

		RestClient f3 = MockRestClient.build(F3.class);
		f3.get("/a").run().assertBody().is("{codes:'1'}");
		f3.get("/b").run().assertBody().is("{codes:'2'},{codes:'1'}");
		f3.get("/e").run().assertBody().is("{codes:'5'},{codes:'3'},{codes:'1'}");
		f3.get("/f").run().assertBody().is("{codes:'1'}");
		f3.get("/g").run().assertBody().is("{codes:'6'},{codes:'1'}");
	}

	//------------------------------------------------------------------------------------------------------------------
	// rules
	//------------------------------------------------------------------------------------------------------------------

	@Rest(
		logging=@Logging(
			rules=@LoggingRule(
				exceptions="1",
				debugOnly="true",
				level="WARNING",
				req="MEDIUM",
				res="LARGE"
			)
		)
	)
	public static class G1 {
		@RestMethod
		public String a(RestRequest req) {
			return string(req.getCallLoggerConfig().getRules());
		}
		@RestMethod(
			logging=@Logging(
				rules=@LoggingRule(
					exceptions="2",
					debugOnly="true",
					level="WARNING",
					req="MEDIUM",
					res="LARGE"
				)
			)
		)
		public String b(RestRequest req) {
			return string(req.getCallLoggerConfig().getRules());
		}
	}

	@Rest
	public static class G2 {
		@RestMethod(
			logging=@Logging(
				rules=@LoggingRule(
					exceptions="3",
					debugOnly="true",
					level="WARNING",
					req="MEDIUM",
					res="LARGE"
				)
			)
		)
		public String a(RestRequest req) {
			return string(req.getCallLoggerConfig().getRules());
		}
	}

	@Test
	public void g01_rules() throws Exception {
		RestClient g1 = MockRestClient.build(G1.class);
		g1.get("/b").run().assertBody().is("{exceptions:'2',debugOnly:true,level:'WARNING',req:'MEDIUM'},{exceptions:'1',debugOnly:true,level:'WARNING',req:'MEDIUM'}");

		RestClient g2 = MockRestClient.build(G2.class);
		g2.get("/a").run().assertBody().is("{exceptions:'3',debugOnly:true,level:'WARNING',req:'MEDIUM'}");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Examples
	//------------------------------------------------------------------------------------------------------------------

	@Rest(
		debug="always",
		logging=@Logging(
			useStackTraceHashing="true",
			rules={
				@LoggingRule(codes=">=500", level="off", req="short", res="short")
			}
		)
	)
	public static class H {

		@RestMethod(method="POST", path="foo")
		public String a(RestRequest req, RestResponse res) throws Exception {
			res.setStatus(500);
			res.setHeader("Foo", "bar");
			res.setException(new StringIndexOutOfBoundsException());
			return req.getBody().asString();
		}
	}


	@Test
	public void h01_examples() throws Exception {
		RestClient MY_REST = MockRestClient.buildLax(H.class);
		MY_REST.post("/foo?foo=bar", "Foo")
			.header("Foo", "bar")
			.run()
			.assertCode().is(500);
		MY_REST.post("/foo?foo=bar", "Foo")
			.header("Foo", "bar")
			.run()
			.assertCode().is(500);
	}
}
