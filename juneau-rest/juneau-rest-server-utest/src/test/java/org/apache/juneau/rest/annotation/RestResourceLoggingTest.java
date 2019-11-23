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

import java.util.*;
import java.util.stream.*;

import org.apache.juneau.marshall.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.mock2.*;
import org.junit.*;
import org.junit.runners.*;

/**
 * Tests that validate the behavior of @Rest(logging).
 */
@SuppressWarnings({})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class RestResourceLoggingTest {

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

	static MockRest a = MockRest.build(A.class, null);

	@Test
	public void a01_default() throws Exception {
		a.get("/").execute().assertBody("{}");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Level
	//------------------------------------------------------------------------------------------------------------------

	@Rest(logging=@Logging(level="WARNING"))
	public static class B1 {
		@RestMethod(path="b01")
		public String b01(RestRequest req) {
			return string(req.getCallLoggerConfig());
		}
		@RestMethod(path="b02", logging=@Logging(level="SEVERE"))
		public String b02(RestRequest req) {
			return string(req.getCallLoggerConfig());
		}
		@RestMethod(path="b07", logging=@Logging(level="SEVERE"))
		public String b07(RestRequest req) {
			return string(req.getCallLoggerConfig());
		}
	}

	@Rest
	public static class B2 {
		@RestMethod(path="b03")
		public String getB03(RestRequest req) {
			return string(req.getCallLoggerConfig());
		}
		@RestMethod(path="b04", logging=@Logging(level="SEVERE"))
		public String getB04(RestRequest req) {
			return string(req.getCallLoggerConfig());
		}
	}

	public static class B3 extends B1 {
		@Override
		public String b01(RestRequest req) {
			return string(req.getCallLoggerConfig());
		}
		@Override
		public String b02(RestRequest req) {
			return string(req.getCallLoggerConfig());
		}
		@Override
		@RestMethod(logging=@Logging(level="OFF"))
		public String b07(RestRequest req) {
			return string(req.getCallLoggerConfig());
		}
		@RestMethod(path="b08")
		public String b08(RestRequest req) {
			return string(req.getCallLoggerConfig());
		}
		@RestMethod(path="b09", logging=@Logging(level="SEVERE"))
		public String b09(RestRequest req) {
			return string(req.getCallLoggerConfig());
		}
	}

	static MockRest b1 = MockRest.build(B1.class, null);
	static MockRest b2 = MockRest.build(B2.class, null);
	static MockRest b3 = MockRest.build(B3.class, null);

	@Test
	public void b01_logging() throws Exception {
		b1.get("/b01").execute().assertBody("{level:'WARNING'}");
	}
	@Test
	public void b02_logging() throws Exception {
		b1.get("/b02").execute().assertBody("{level:'SEVERE'}");
	}
	@Test
	public void b03_logging() throws Exception {
		b2.get("/b03").execute().assertBody("{}");
	}
	@Test
	public void b04_logging() throws Exception {
		b2.get("/b04").execute().assertBody("{level:'SEVERE'}");
	}
	@Test
	public void b05_logging() throws Exception {
		b3.get("/b01").execute().assertBody("{level:'WARNING'}");
	}
	@Test
	public void b06_logging() throws Exception {
		b3.get("/b02").execute().assertBody("{level:'SEVERE'}");
	}
	@Test
	public void b07_logging() throws Exception {
		b3.get("/b07").execute().assertBody("{level:'OFF'}");
	}
	@Test
	public void b08_logging() throws Exception {
		b3.get("/b08").execute().assertBody("{level:'WARNING'}");
	}
	@Test
	public void b09_logging() throws Exception {
		b3.get("/b09").execute().assertBody("{level:'SEVERE'}");
	}

	//------------------------------------------------------------------------------------------------------------------
	// useStackTraceHashing
	//------------------------------------------------------------------------------------------------------------------

	@Rest(logging=@Logging(useStackTraceHashing="true"))
	public static class C1 {
		@RestMethod(path="c01")
		public String c01(RestRequest req) {
			return string(req.getCallLoggerConfig());
		}
		@RestMethod(path="c02", logging=@Logging(useStackTraceHashing="false"))
		public String c02(RestRequest req) {
			return string(req.getCallLoggerConfig());
		}
		@RestMethod(path="c08", logging=@Logging(useStackTraceHashing="false"))
		public String c08(RestRequest req) {
			return string(req.getCallLoggerConfig());
		}
	}

	@Rest
	public static class C2 {
		@RestMethod(path="c03")
		public String c03(RestRequest req) {
			return string(req.getCallLoggerConfig());
		}
		@RestMethod(path="c04", logging=@Logging(useStackTraceHashing="true"))
		public String c04(RestRequest req) {
			return string(req.getCallLoggerConfig());
		}
		@RestMethod(path="c05", logging=@Logging(useStackTraceHashing="foo"))
		public String c05(RestRequest req) {
			return string(req.getCallLoggerConfig());
		}
	}

	public static class C3 extends C1 {
		@Override
		public String c01(RestRequest req) {
			return string(req.getCallLoggerConfig());
		}
		@Override
		public String c02(RestRequest req) {
			return string(req.getCallLoggerConfig());
		}
		@Override
		@RestMethod(logging=@Logging(useStackTraceHashing="TRUE"))
		public String c08(RestRequest req) {
			return string(req.getCallLoggerConfig());
		}
		@RestMethod(path="c09")
		public String c09(RestRequest req) {
			return string(req.getCallLoggerConfig());
		}
		@RestMethod(path="c10", logging=@Logging(useStackTraceHashing="FALSE"))
		public String c10(RestRequest req) {
			return string(req.getCallLoggerConfig());
		}
	}
	@Rest(logging=@Logging(useStackTraceHashing="foo"))
	public static class C4 {
		@RestMethod(path="c11")
		public String c11(RestRequest req) {
			return string(req.getCallLoggerConfig());
		}
	}

	static MockRest c1 = MockRest.build(C1.class, null);
	static MockRest c2 = MockRest.build(C2.class, null);
	static MockRest c3 = MockRest.build(C3.class, null);
	static MockRest c4 = MockRest.build(C4.class, null);

	@Test
	public void c01_useStackTraceHashing() throws Exception {
		c1.get("/c01").execute().assertBody("{useStackTraceHashing:true}");
	}
	@Test
	public void c02_useStackTraceHashing() throws Exception {
		c1.get("/c02").execute().assertBody("{}");
	}
	@Test
	public void c03_useStackTraceHashing() throws Exception {
		c2.get("/c03").execute().assertBody("{}");
	}
	@Test
	public void c04_useStackTraceHashing() throws Exception {
		c2.get("/c04").execute().assertBody("{useStackTraceHashing:true}");
	}
	@Test
	public void c05_useStackTraceHashing() throws Exception {
		c2.get("/c05").execute().assertBody("{}");
	}
	@Test
	public void c06_useStackTraceHashing() throws Exception {
		c3.get("/c01").execute().assertBody("{useStackTraceHashing:true}");
	}
	@Test
	public void c07_useStackTraceHashing() throws Exception {
		c3.get("/c02").execute().assertBody("{}");
	}
	@Test
	public void c08_useStackTraceHashing() throws Exception {
		c3.get("/c08").execute().assertBody("{useStackTraceHashing:true}");
	}
	@Test
	public void c09_useStackTraceHashing() throws Exception {
		c3.get("/c09").execute().assertBody("{useStackTraceHashing:true}");
	}
	@Test
	public void c10_useStackTraceHashing() throws Exception {
		c3.get("/c10").execute().assertBody("{}");
	}
	@Test
	public void c11_useStackTraceHashing() throws Exception {
		c4.get("/c11").execute().assertBody("{}");
	}

	//------------------------------------------------------------------------------------------------------------------
	// stackTraceHashingTimeout
	//------------------------------------------------------------------------------------------------------------------

	@Rest(logging=@Logging(stackTraceHashingTimeout="1"))
	public static class D1 {
		@RestMethod(path="d01")
		public String d01(RestRequest req) {
			return string(req.getCallLoggerConfig());
		}
		@RestMethod(path="d02", logging=@Logging(stackTraceHashingTimeout="2"))
		public String d02(RestRequest req) {
			return string(req.getCallLoggerConfig());
		}
		@RestMethod(path="d07", logging=@Logging(stackTraceHashingTimeout="3"))
		public String d07(RestRequest req) {
			return string(req.getCallLoggerConfig());
		}
	}

	@Rest
	public static class D2 {
		@RestMethod(path="d03")
		public String d03(RestRequest req) {
			return string(req.getCallLoggerConfig());
		}
		@RestMethod(path="d04", logging=@Logging(stackTraceHashingTimeout="4"))
		public String d04(RestRequest req) {
			return string(req.getCallLoggerConfig());
		}
	}

	public static class D3 extends D1 {
		@Override
		public String d01(RestRequest req) {
			return string(req.getCallLoggerConfig());
		}
		@Override
		public String d02(RestRequest req) {
			return string(req.getCallLoggerConfig());
		}
		@Override
		@RestMethod(logging=@Logging(stackTraceHashingTimeout="5"))
		public String d07(RestRequest req) {
			return string(req.getCallLoggerConfig());
		}
		@RestMethod(path="d08")
		public String d08(RestRequest req) {
			return string(req.getCallLoggerConfig());
		}
		@RestMethod(path="d09", logging=@Logging(stackTraceHashingTimeout="6"))
		public String d09(RestRequest req) {
			return string(req.getCallLoggerConfig());
		}
	}

	static MockRest d1 = MockRest.build(D1.class, null);
	static MockRest d2 = MockRest.build(D2.class, null);
	static MockRest d3 = MockRest.build(D3.class, null);

	@Test
	public void d01_stackTraceHashingTimeout() throws Exception {
		d1.get("/d01").execute().assertBody("{stackTraceHashingTimeout:1}");
	}
	@Test
	public void d02_stackTraceHashingTimeout() throws Exception {
		d1.get("/d02").execute().assertBody("{stackTraceHashingTimeout:2}");
	}
	@Test
	public void d03_stackTraceHashingTimeout() throws Exception {
		d2.get("/d03").execute().assertBody("{}");
	}
	@Test
	public void d04_stackTraceHashingTimeout() throws Exception {
		d2.get("/d04").execute().assertBody("{stackTraceHashingTimeout:4}");
	}
	@Test
	public void d05_stackTraceHashingTimeout() throws Exception {
		d3.get("/d01").execute().assertBody("{stackTraceHashingTimeout:1}");
	}
	@Test
	public void d06_stackTraceHashingTimeout() throws Exception {
		d3.get("/d02").execute().assertBody("{stackTraceHashingTimeout:2}");
	}
	@Test
	public void d07_stackTraceHashingTimeout() throws Exception {
		d3.get("/d07").execute().assertBody("{stackTraceHashingTimeout:5}");
	}
	@Test
	public void d08_stackTraceHashingTimeout() throws Exception {
		d3.get("/d08").execute().assertBody("{stackTraceHashingTimeout:1}");
	}
	@Test
	public void d09_stackTraceHashingTimeout() throws Exception {
		d3.get("/d09").execute().assertBody("{stackTraceHashingTimeout:6}");
	}

	//------------------------------------------------------------------------------------------------------------------
	// noTrace
	//------------------------------------------------------------------------------------------------------------------

	@Rest(logging=@Logging(disabled="true"))
	public static class E1 {
		@RestMethod(path="e01")
		public String e01(RestRequest req) {
			return string(req.getCallLoggerConfig());
		}
		@RestMethod(path="e02", logging=@Logging(disabled="per-request"))
		public String e02(RestRequest req) {
			return string(req.getCallLoggerConfig());
		}
		@RestMethod(path="e08", logging=@Logging(disabled="per-request"))
		public String e08(RestRequest req) {
			return string(req.getCallLoggerConfig());
		}
	}

	@Rest
	public static class E2 {
		@RestMethod(path="e03")
		public String e03(RestRequest req) {
			return string(req.getCallLoggerConfig());
		}
		@RestMethod(path="e04", logging=@Logging(disabled="true"))
		public String e04(RestRequest req) {
			return string(req.getCallLoggerConfig());
		}
		@RestMethod(path="e05", logging=@Logging(disabled="foo"))
		public String e05(RestRequest req) {
			return string(req.getCallLoggerConfig());
		}
	}

	public static class E3 extends E1 {
		@Override
		public String e01(RestRequest req) {
			return string(req.getCallLoggerConfig());
		}
		@Override
		public String e02(RestRequest req) {
			return string(req.getCallLoggerConfig());
		}
		@Override
		@RestMethod(logging=@Logging(disabled="false"))
		public String e08(RestRequest req) {
			return string(req.getCallLoggerConfig());
		}
		@RestMethod(path="e09")
		public String e09(RestRequest req) {
			return string(req.getCallLoggerConfig());
		}
		@RestMethod(path="e10", logging=@Logging(disabled="per-request"))
		public String e10(RestRequest req) {
			return string(req.getCallLoggerConfig());
		}
	}
	@Rest(logging=@Logging(disabled="foo"))
	public static class E4 {
		@RestMethod(path="e11")
		public String e11(RestRequest req) {
			return string(req.getCallLoggerConfig());
		}
	}

	static MockRest e1 = MockRest.build(E1.class, null);
	static MockRest e2 = MockRest.build(E2.class, null);
	static MockRest e3 = MockRest.build(E3.class, null);
	static MockRest e4 = MockRest.build(E4.class, null);

	@Test
	public void e01_noTrace() throws Exception {
		e1.get("/e01").execute().assertBody("{disabled:'TRUE'}");
	}
	@Test
	public void e02_noTrace() throws Exception {
		e1.get("/e02").execute().assertBody("{disabled:'PER_REQUEST'}");
	}
	@Test
	public void e03_noTrace() throws Exception {
		e2.get("/e03").execute().assertBody("{}");
	}
	@Test
	public void e04_noTrace() throws Exception {
		e2.get("/e04").execute().assertBody("{disabled:'TRUE'}");
	}
	@Test
	public void e05_noTrace() throws Exception {
		e2.get("/e05").execute().assertBody("{}");
	}
	@Test
	public void e06_noTrace() throws Exception {
		e3.get("/e01").execute().assertBody("{disabled:'TRUE'}");
	}
	@Test
	public void e07_noTrace() throws Exception {
		e3.get("/e02").execute().assertBody("{disabled:'PER_REQUEST'}");
	}
	@Test
	public void e08_noTrace() throws Exception {
		e3.get("/e08").execute().assertBody("{}");
	}
	@Test
	public void e09_noTrace() throws Exception {
		e3.get("/e09").execute().assertBody("{disabled:'TRUE'}");
	}
	@Test
	public void e10_noTrace() throws Exception {
		e3.get("/e10").execute().assertBody("{disabled:'PER_REQUEST'}");
	}
	@Test
	public void e11_noTrace() throws Exception {
		e4.get("/e11").execute().assertBody("{}");
	}

	//------------------------------------------------------------------------------------------------------------------
	// rules
	//------------------------------------------------------------------------------------------------------------------

	@Rest(logging=@Logging(rules=@LoggingRule(codes="1")))
	public static class F1 {
		@RestMethod(path="f01")
		public String f01(RestRequest req) {
			return string(req.getCallLoggerConfig().getRules());
		}
		@RestMethod(path="f02", logging=@Logging(rules=@LoggingRule(codes="2")))
		public String f02(RestRequest req) {
			return string(req.getCallLoggerConfig().getRules());
		}
		@RestMethod(path="f07", logging=@Logging(rules=@LoggingRule(codes="3")))
		public String f07(RestRequest req) {
			return string(req.getCallLoggerConfig().getRules());
		}
	}

	@Rest
	public static class F2 {
		@RestMethod(path="f03")
		public String f03(RestRequest req) {
			return string(req.getCallLoggerConfig().getRules());
		}
		@RestMethod(path="f04", logging=@Logging(rules=@LoggingRule(codes="4")))
		public String f04(RestRequest req) {
			return string(req.getCallLoggerConfig().getRules());
		}
	}

	public static class F3 extends F1 {
		@Override
		public String f01(RestRequest req) {
			return string(req.getCallLoggerConfig().getRules());
		}
		@Override
		public String f02(RestRequest req) {
			return string(req.getCallLoggerConfig().getRules());
		}
		@Override
		@RestMethod(logging=@Logging(rules=@LoggingRule(codes="5")))
		public String f07(RestRequest req) {
			return string(req.getCallLoggerConfig().getRules());
		}
		@RestMethod(path="f08")
		public String f08(RestRequest req) {
			return string(req.getCallLoggerConfig().getRules());
		}
		@RestMethod(path="f09", logging=@Logging(rules=@LoggingRule(codes="6")))
		public String f09(RestRequest req) {
			return string(req.getCallLoggerConfig().getRules());
		}
	}

	static MockRest f1 = MockRest.build(F1.class, null);
	static MockRest f2 = MockRest.build(F2.class, null);
	static MockRest f3 = MockRest.build(F3.class, null);

	@Test
	public void f01_rules() throws Exception {
		f1.get("/f01").execute().assertBody("{codes:'1'}");
	}
	@Test
	public void f02_rules() throws Exception {
		f1.get("/f02").execute().assertBody("{codes:'2'},{codes:'1'}");
	}
	@Test
	public void f03_rules() throws Exception {
		f2.get("/f03").execute().assertBody("");
	}
	@Test
	public void f04_rules() throws Exception {
		f2.get("/f04").execute().assertBody("{codes:'4'}");
	}
	@Test
	public void f05_rules() throws Exception {
		f3.get("/f01").execute().assertBody("{codes:'1'}");
	}
	@Test
	public void f06_rules() throws Exception {
		f3.get("/f02").execute().assertBody("{codes:'2'},{codes:'1'}");
	}
	@Test
	public void f07_rules() throws Exception {
		f3.get("/f07").execute().assertBody("{codes:'5'},{codes:'3'},{codes:'1'}");
	}
	@Test
	public void f08_rules() throws Exception {
		f3.get("/f08").execute().assertBody("{codes:'1'}");
	}
	@Test
	public void f09_rules() throws Exception {
		f3.get("/f09").execute().assertBody("{codes:'6'},{codes:'1'}");
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
		@RestMethod(path="g01")
		public String g01(RestRequest req) {
			return string(req.getCallLoggerConfig().getRules());
		}
		@RestMethod(path="g02",
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
		public String g02(RestRequest req) {
			return string(req.getCallLoggerConfig().getRules());
		}
	}

	@Rest
	public static class G2 {
		@RestMethod(path="g03",
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
		public String g03(RestRequest req) {
			return string(req.getCallLoggerConfig().getRules());
		}
	}
	static MockRest g1 = MockRest.build(G1.class, null);
	static MockRest g2 = MockRest.build(G2.class, null);

	@Test
	public void g01_rules() throws Exception {
		g1.get("/g01").execute().assertBody("{exceptions:'1',debugOnly:true,level:'WARNING',req:'MEDIUM'}");
	}
	@Test
	public void g02_rules() throws Exception {
		g1.get("/g02").execute().assertBody("{exceptions:'2',debugOnly:true,level:'WARNING',req:'MEDIUM'},{exceptions:'1',debugOnly:true,level:'WARNING',req:'MEDIUM'}");
	}
	@Test
	public void g03_rules() throws Exception {
		g2.get("/g03").execute().assertBody("{exceptions:'3',debugOnly:true,level:'WARNING',req:'MEDIUM'}");
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
	public static class MyRestClass {

		@RestMethod(method="POST", path="foo")
		public String myRestMethod(RestRequest req, RestResponse res) throws Exception {
			res.setStatus(500);
			res.setHeader("Foo", "bar");
			res.setException(new StringIndexOutOfBoundsException());
			return req.getBody().asString();
		}
	}

	static MockRest MY_REST = MockRest.build(MyRestClass.class, null);

	@Test
	public void test() throws Exception {
		MY_REST.post("/foo?foo=bar", "Foo").header("Foo", "bar").execute().assertStatus(500);
		MY_REST.post("/foo?foo=bar", "Foo").header("Foo", "bar").execute().assertStatus(500);
	}
}
