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
import org.apache.juneau.rest.mock2.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
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

	static MockRestClient a = MockRestClient.build(A.class);

	@Test
	public void a01_default() throws Exception {
		a.get("/").run().assertBody().is("{}");
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

	static MockRestClient b1 = MockRestClient.build(B1.class);
	static MockRestClient b2 = MockRestClient.build(B2.class);
	static MockRestClient b3 = MockRestClient.build(B3.class);

	@Test
	public void b01_logging() throws Exception {
		b1.get("/b01").run().assertBody().is("{level:'WARNING'}");
	}
	@Test
	public void b02_logging() throws Exception {
		b1.get("/b02").run().assertBody().is("{level:'SEVERE'}");
	}
	@Test
	public void b03_logging() throws Exception {
		b2.get("/b03").run().assertBody().is("{}");
	}
	@Test
	public void b04_logging() throws Exception {
		b2.get("/b04").run().assertBody().is("{level:'SEVERE'}");
	}
	@Test
	public void b05_logging() throws Exception {
		b3.get("/b01").run().assertBody().is("{level:'WARNING'}");
	}
	@Test
	public void b06_logging() throws Exception {
		b3.get("/b02").run().assertBody().is("{level:'SEVERE'}");
	}
	@Test
	public void b07_logging() throws Exception {
		b3.get("/b07").run().assertBody().is("{level:'OFF'}");
	}
	@Test
	public void b08_logging() throws Exception {
		b3.get("/b08").run().assertBody().is("{level:'WARNING'}");
	}
	@Test
	public void b09_logging() throws Exception {
		b3.get("/b09").run().assertBody().is("{level:'SEVERE'}");
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

	static MockRestClient c1 = MockRestClient.build(C1.class);
	static MockRestClient c2 = MockRestClient.build(C2.class);
	static MockRestClient c3 = MockRestClient.build(C3.class);
	static MockRestClient c4 = MockRestClient.build(C4.class);

	@Test
	public void c01_useStackTraceHashing() throws Exception {
		c1.get("/c01").run().assertBody().is("{useStackTraceHashing:true}");
	}
	@Test
	public void c02_useStackTraceHashing() throws Exception {
		c1.get("/c02").run().assertBody().is("{}");
	}
	@Test
	public void c03_useStackTraceHashing() throws Exception {
		c2.get("/c03").run().assertBody().is("{}");
	}
	@Test
	public void c04_useStackTraceHashing() throws Exception {
		c2.get("/c04").run().assertBody().is("{useStackTraceHashing:true}");
	}
	@Test
	public void c05_useStackTraceHashing() throws Exception {
		c2.get("/c05").run().assertBody().is("{}");
	}
	@Test
	public void c06_useStackTraceHashing() throws Exception {
		c3.get("/c01").run().assertBody().is("{useStackTraceHashing:true}");
	}
	@Test
	public void c07_useStackTraceHashing() throws Exception {
		c3.get("/c02").run().assertBody().is("{}");
	}
	@Test
	public void c08_useStackTraceHashing() throws Exception {
		c3.get("/c08").run().assertBody().is("{useStackTraceHashing:true}");
	}
	@Test
	public void c09_useStackTraceHashing() throws Exception {
		c3.get("/c09").run().assertBody().is("{useStackTraceHashing:true}");
	}
	@Test
	public void c10_useStackTraceHashing() throws Exception {
		c3.get("/c10").run().assertBody().is("{}");
	}
	@Test
	public void c11_useStackTraceHashing() throws Exception {
		c4.get("/c11").run().assertBody().is("{}");
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

	static MockRestClient d1 = MockRestClient.build(D1.class);
	static MockRestClient d2 = MockRestClient.build(D2.class);
	static MockRestClient d3 = MockRestClient.build(D3.class);

	@Test
	public void d01_stackTraceHashingTimeout() throws Exception {
		d1.get("/d01").run().assertBody().is("{stackTraceHashingTimeout:1}");
	}
	@Test
	public void d02_stackTraceHashingTimeout() throws Exception {
		d1.get("/d02").run().assertBody().is("{stackTraceHashingTimeout:2}");
	}
	@Test
	public void d03_stackTraceHashingTimeout() throws Exception {
		d2.get("/d03").run().assertBody().is("{}");
	}
	@Test
	public void d04_stackTraceHashingTimeout() throws Exception {
		d2.get("/d04").run().assertBody().is("{stackTraceHashingTimeout:4}");
	}
	@Test
	public void d05_stackTraceHashingTimeout() throws Exception {
		d3.get("/d01").run().assertBody().is("{stackTraceHashingTimeout:1}");
	}
	@Test
	public void d06_stackTraceHashingTimeout() throws Exception {
		d3.get("/d02").run().assertBody().is("{stackTraceHashingTimeout:2}");
	}
	@Test
	public void d07_stackTraceHashingTimeout() throws Exception {
		d3.get("/d07").run().assertBody().is("{stackTraceHashingTimeout:5}");
	}
	@Test
	public void d08_stackTraceHashingTimeout() throws Exception {
		d3.get("/d08").run().assertBody().is("{stackTraceHashingTimeout:1}");
	}
	@Test
	public void d09_stackTraceHashingTimeout() throws Exception {
		d3.get("/d09").run().assertBody().is("{stackTraceHashingTimeout:6}");
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

	static MockRestClient e1 = MockRestClient.build(E1.class);
	static MockRestClient e2 = MockRestClient.build(E2.class);
	static MockRestClient e3 = MockRestClient.build(E3.class);
	static MockRestClient e4 = MockRestClient.build(E4.class);

	@Test
	public void e01_noTrace() throws Exception {
		e1.get("/e01").run().assertBody().is("{disabled:'TRUE'}");
	}
	@Test
	public void e02_noTrace() throws Exception {
		e1.get("/e02").run().assertBody().is("{disabled:'PER_REQUEST'}");
	}
	@Test
	public void e03_noTrace() throws Exception {
		e2.get("/e03").run().assertBody().is("{}");
	}
	@Test
	public void e04_noTrace() throws Exception {
		e2.get("/e04").run().assertBody().is("{disabled:'TRUE'}");
	}
	@Test
	public void e05_noTrace() throws Exception {
		e2.get("/e05").run().assertBody().is("{}");
	}
	@Test
	public void e06_noTrace() throws Exception {
		e3.get("/e01").run().assertBody().is("{disabled:'TRUE'}");
	}
	@Test
	public void e07_noTrace() throws Exception {
		e3.get("/e02").run().assertBody().is("{disabled:'PER_REQUEST'}");
	}
	@Test
	public void e08_noTrace() throws Exception {
		e3.get("/e08").run().assertBody().is("{}");
	}
	@Test
	public void e09_noTrace() throws Exception {
		e3.get("/e09").run().assertBody().is("{disabled:'TRUE'}");
	}
	@Test
	public void e10_noTrace() throws Exception {
		e3.get("/e10").run().assertBody().is("{disabled:'PER_REQUEST'}");
	}
	@Test
	public void e11_noTrace() throws Exception {
		e4.get("/e11").run().assertBody().is("{}");
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

	static MockRestClient f1 = MockRestClient.build(F1.class);
	static MockRestClient f2 = MockRestClient.build(F2.class);
	static MockRestClient f3 = MockRestClient.build(F3.class);

	@Test
	public void f01_rules() throws Exception {
		f1.get("/f01").run().assertBody().is("{codes:'1'}");
	}
	@Test
	public void f02_rules() throws Exception {
		f1.get("/f02").run().assertBody().is("{codes:'2'},{codes:'1'}");
	}
	@Test
	public void f03_rules() throws Exception {
		f2.get("/f03").run().assertBody().is("");
	}
	@Test
	public void f04_rules() throws Exception {
		f2.get("/f04").run().assertBody().is("{codes:'4'}");
	}
	@Test
	public void f05_rules() throws Exception {
		f3.get("/f01").run().assertBody().is("{codes:'1'}");
	}
	@Test
	public void f06_rules() throws Exception {
		f3.get("/f02").run().assertBody().is("{codes:'2'},{codes:'1'}");
	}
	@Test
	public void f07_rules() throws Exception {
		f3.get("/f07").run().assertBody().is("{codes:'5'},{codes:'3'},{codes:'1'}");
	}
	@Test
	public void f08_rules() throws Exception {
		f3.get("/f08").run().assertBody().is("{codes:'1'}");
	}
	@Test
	public void f09_rules() throws Exception {
		f3.get("/f09").run().assertBody().is("{codes:'6'},{codes:'1'}");
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
	static MockRestClient g1 = MockRestClient.build(G1.class);
	static MockRestClient g2 = MockRestClient.build(G2.class);

	@Test
	public void g01_rules() throws Exception {
		g1.get("/g01").run().assertBody().is("{exceptions:'1',debugOnly:true,level:'WARNING',req:'MEDIUM'}");
	}
	@Test
	public void g02_rules() throws Exception {
		g1.get("/g02").run().assertBody().is("{exceptions:'2',debugOnly:true,level:'WARNING',req:'MEDIUM'},{exceptions:'1',debugOnly:true,level:'WARNING',req:'MEDIUM'}");
	}
	@Test
	public void g03_rules() throws Exception {
		g2.get("/g03").run().assertBody().is("{exceptions:'3',debugOnly:true,level:'WARNING',req:'MEDIUM'}");
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

	static MockRestClient MY_REST = MockRestClient.build(MyRestClass.class);

	@Test
	public void test() throws Exception {
		MY_REST.post("/foo?foo=bar", "Foo")
			.header("Foo", "bar")
			.run()
			.assertStatus().is(500);
		MY_REST.post("/foo?foo=bar", "Foo")
			.header("Foo", "bar")
			.run()
			.assertStatus().is(500);
	}
}
