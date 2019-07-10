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
package org.apache.juneau.rest;

import static org.junit.Assert.*;
import static org.apache.juneau.rest.RestCallLoggingDetail.*;
import static java.util.logging.Level.*;

import java.util.logging.*;
import java.util.regex.*;

import org.apache.juneau.internal.*;
import org.apache.juneau.rest.mock2.*;
import org.junit.*;
import org.junit.runners.*;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class BasicRestCallLoggerTest {

	static class TestLogger extends Logger {
		Level level;
		String msg;
		Throwable t;

		protected TestLogger() {
			super(null, null);
		}

		@Override
		public void log(Level level, String msg, Throwable t) {
			this.level = level;
			this.msg = msg;
			this.t = t;
		}

		public void check(Level level, String msg, boolean hasThrowable) {
			boolean isNot = (msg != null && msg.length() > 0 && msg.charAt(0) == '!');
			if (isNot)
				msg = msg.substring(1);
			if (msg != null && msg.indexOf('*') != -1) {
				Pattern p = StringUtils.getMatchPattern(msg, Pattern.DOTALL);
				boolean eq = p.matcher(this.msg).matches();
				if (isNot ? eq : ! eq)
					fail("Message text didn't match [2].\nExpected=["+msg+"]\nActual=["+this.msg+"]");
			} else {
				boolean eq = StringUtils.isEquals(this.msg, msg);
				if (isNot ? eq : ! eq)
					fail("Message text didn't match [1].\nExpected=["+msg+"]\nActual=["+this.msg+"]");
			}

			assertEquals("Message level didn't match.", level, this.level);
			if (hasThrowable && t == null)
				fail("Throwable not present");
			if (t != null && ! hasThrowable)
				fail("Throwable present.");
		}
	}

	private RestCallLoggerConfig.Builder config() {
		return RestCallLoggerConfig.create();
	}

	private RestCallLoggerConfig wrapped(RestCallLoggerConfig config) {
		return RestCallLoggerConfig.create().parent(config).build();
	}

	private BasicRestCallLogger logger(Logger l) {
		return new BasicRestCallLogger(null, l);
	}

	private RestCallLoggerRule.Builder rule() {
		return RestCallLoggerRule.create();
	}

	private MockServletRequest req() {
		return MockServletRequest.create();
	}

	private MockServletResponse res() {
		return MockServletResponse.create();
	}

	private MockServletResponse res(int status) {
		return MockServletResponse.create().status(status);
	}

	//------------------------------------------------------------------------------------------------------------------
	// No logging
	//------------------------------------------------------------------------------------------------------------------


	@Test
	public void a01a_noRules() {
		RestCallLoggerConfig lc = config().build();
		RestCallLoggerConfig lcw = wrapped(lc);

		TestLogger tc = new TestLogger();
		BasicRestCallLogger cl = logger(tc);

		MockServletRequest req = req();
		MockServletResponse res = res();

		cl.log(lc, req, res);
		tc.check(null, null, false);

		cl.log(lcw, req, res);
		tc.check(null, null, false);
	}

	@Test
	public void a02_levelOff() {
		RestCallLoggerConfig lc =
			config()
			.rules(
				rule().codes("*").level(OFF).build()
			)
			.build();
		RestCallLoggerConfig lcw = wrapped(lc);

		TestLogger tc = new TestLogger();
		BasicRestCallLogger cl = logger(tc);
		MockServletRequest req = req();
		MockServletResponse res = res();

		cl.log(lc, req, res);
		tc.check(null, null, false);

		cl.log(lcw, req, res);
		tc.check(null, null, false);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Basic logging
	//------------------------------------------------------------------------------------------------------------------

	@Test
	public void b01_short_short() {
		RestCallLoggerConfig lc =
			config()
			.rules(
				rule().codes("*").req(SHORT).res(SHORT).build()
			)
			.build();
		RestCallLoggerConfig lcw = wrapped(lc);

		TestLogger tc = new TestLogger();
		BasicRestCallLogger cl = logger(tc);
		MockServletRequest req = req().uri("/foo").query("bar", "baz");
		MockServletResponse res = res(200);

		cl.log(lc, req, res);
		tc.check(INFO, "[200] HTTP GET /foo", false);

		cl.log(lcw, req, res);
		tc.check(INFO, "[200] HTTP GET /foo", false);
	}

	@Test
	public void b02_short_short_default() {
		RestCallLoggerConfig lc =
			config()
			.rules(
				rule().codes("*").build()
			)
			.build();
		RestCallLoggerConfig lcw = wrapped(lc);

		TestLogger tc = new TestLogger();
		BasicRestCallLogger cl = logger(tc);
		MockServletRequest req = req().uri("/foo").query("bar", "baz");
		MockServletResponse res = res(200);

		cl.log(lc, req, res);
		tc.check(INFO, "[200] HTTP GET /foo", false);

		cl.log(lcw, req, res);
		tc.check(INFO, "[200] HTTP GET /foo", false);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Stack trace hashing.
	//------------------------------------------------------------------------------------------------------------------

	@Test
	public void c01_stackTraceHashing_on() {
		RestCallLoggerConfig lc =
			config()
			.rules(
				rule().codes("*").req(SHORT).res(SHORT).build()
			)
			.useStackTraceHashing()
			.build();
		RestCallLoggerConfig lcw = wrapped(lc);

		TestLogger tc = new TestLogger();
		BasicRestCallLogger cl = logger(tc).resetStackTraces();
		Exception e = new StringIndexOutOfBoundsException();
		MockServletRequest req = req().uri("/foo").query("bar", "baz").attribute("Exception", e);
		MockServletResponse res = res(200);

		cl.log(lc, req, res);
		tc.check(INFO, "[200,*.1] HTTP GET /foo", true);
		cl.log(lc, req, res);
		tc.check(INFO, "[200,*.2] HTTP GET /foo", false);

		cl.resetStackTraces();

		cl.log(lcw, req, res);
		tc.check(INFO, "[200,*.1] HTTP GET /foo", true);
		cl.log(lcw, req, res);
		tc.check(INFO, "[200,*.2] HTTP GET /foo", false);
	}

	@Test
	public void c02_stackTraceHashing_true() {

		RestCallLoggerConfig lc =
			config()
			.rules(
				rule().codes("*").req(SHORT).res(SHORT).build()
			)
			.useStackTraceHashing(true)
			.build();
		RestCallLoggerConfig lcw = wrapped(lc);

		TestLogger tc = new TestLogger();
		BasicRestCallLogger cl = logger(tc).resetStackTraces();
		Exception e = new StringIndexOutOfBoundsException();
		MockServletRequest req = req().uri("/foo").query("bar", "baz").attribute("Exception", e);
		MockServletResponse res = res(200);

		cl.log(lc, req, res);
		tc.check(INFO, "[200,*.1] HTTP GET /foo", true);
		cl.log(lc, req, res);
		tc.check(INFO, "[200,*.2] HTTP GET /foo", false);

		cl.resetStackTraces();

		cl.log(lcw, req, res);
		tc.check(INFO, "[200,*.1] HTTP GET /foo", true);
		cl.log(lcw, req, res);
		tc.check(INFO, "[200,*.2] HTTP GET /foo", false);
	}

	@Test
	public void c03_stackTraceHashing_false() {
		RestCallLoggerConfig lc =
			config()
			.rules(
				rule().codes("*").req(SHORT).res(SHORT).build()
			)
			.useStackTraceHashing(false)
			.build();
		RestCallLoggerConfig lcw = wrapped(lc);

		TestLogger tc = new TestLogger();
		BasicRestCallLogger cl = logger(tc).resetStackTraces();
		Exception e = new StringIndexOutOfBoundsException();
		MockServletRequest req = req().uri("/foo").query("bar", "baz").attribute("Exception", e);
		MockServletResponse res = res(200);

		cl.log(lc, req, res);
		tc.check(INFO, "[200] HTTP GET /foo", true);
		cl.log(lc, req, res);
		tc.check(INFO, "[200] HTTP GET /foo", true);

		cl.resetStackTraces();

		cl.log(lcw, req, res);
		tc.check(INFO, "[200] HTTP GET /foo", true);
		cl.log(lcw, req, res);
		tc.check(INFO, "[200] HTTP GET /foo", true);
	}

	@Test
	public void c04_stackTraceHashing_default() {
		RestCallLoggerConfig lc =
			config()
			.rules(
				rule().codes("*").req(SHORT).res(SHORT).build()
			)
			.build();
		RestCallLoggerConfig lcw = wrapped(lc);

		TestLogger tc = new TestLogger();
		BasicRestCallLogger cl = logger(tc).resetStackTraces();
		Exception e = new StringIndexOutOfBoundsException();
		MockServletRequest req = req().uri("/foo").query("bar", "baz").attribute("Exception", e);
		MockServletResponse res = res(200);

		cl.log(lc, req, res);
		tc.check(INFO, "[200] HTTP GET /foo", true);
		cl.log(lc, req, res);
		tc.check(INFO, "[200] HTTP GET /foo", true);

		cl.resetStackTraces();

		cl.log(lcw, req, res);
		tc.check(INFO, "[200] HTTP GET /foo", true);
		cl.log(lcw, req, res);
		tc.check(INFO, "[200] HTTP GET /foo", true);
	}

	@Test
	public void c05_stackTraceHashing_null() {
		RestCallLoggerConfig lc =
			config()
			.rules(
				rule().codes("*").req(SHORT).res(SHORT).build()
			)
			.useStackTraceHashing(null)
			.build();
		RestCallLoggerConfig lcw = wrapped(lc);

		TestLogger tc = new TestLogger();
		BasicRestCallLogger cl = logger(tc).resetStackTraces();
		Exception e = new StringIndexOutOfBoundsException();
		MockServletRequest req = req().uri("/foo").query("bar", "baz").attribute("Exception", e);
		MockServletResponse res = res(200);

		cl.log(lc, req, res);
		tc.check(INFO, "[200] HTTP GET /foo", true);
		cl.log(lc, req, res);
		tc.check(INFO, "[200] HTTP GET /foo", true);

		cl.resetStackTraces();

		cl.log(lcw, req, res);
		tc.check(INFO, "[200] HTTP GET /foo", true);
		cl.log(lcw, req, res);
		tc.check(INFO, "[200] HTTP GET /foo", true);
	}

	@Test
	public void c06_stackTraceHashing_timeout_on() {
		RestCallLoggerConfig lc =
			config()
			.rules(
				rule().codes("*").req(SHORT).res(SHORT).build()
			)
			.useStackTraceHashing()
			.stackTraceHashingTimeout(100000)
			.build();
		RestCallLoggerConfig lcw = wrapped(lc);

		TestLogger tc = new TestLogger();
		BasicRestCallLogger cl = logger(tc).resetStackTraces();
		Exception e = new StringIndexOutOfBoundsException();
		MockServletRequest req = req().uri("/foo").query("bar", "baz").attribute("Exception", e);
		MockServletResponse res = res(200);

		cl.log(lc, req, res);
		tc.check(INFO, "[200,*.1] HTTP GET /foo", true);
		cl.log(lc, req, res);
		tc.check(INFO, "[200,*.2] HTTP GET /foo", false);

		cl.resetStackTraces();

		cl.log(lcw, req, res);
		tc.check(INFO, "[200,*.1] HTTP GET /foo", true);
		cl.log(lcw, req, res);
		tc.check(INFO, "[200,*.2] HTTP GET /foo", false);
	}

	@Test
	public void c07_stackTraceHashing_timeout_off() {
		RestCallLoggerConfig lc =
			config()
			.rules(
				rule().codes("*").req(SHORT).res(SHORT).build()
			)
			.useStackTraceHashing()
			.stackTraceHashingTimeout(-1)
			.build();
		RestCallLoggerConfig lcw = wrapped(lc);

		TestLogger tc = new TestLogger();
		BasicRestCallLogger cl = logger(tc).resetStackTraces();
		Exception e = new StringIndexOutOfBoundsException();
		MockServletRequest req = req().uri("/foo").query("bar", "baz").attribute("Exception", e);
		MockServletResponse res = res(200);

		cl.log(lc, req, res);
		tc.check(INFO, "[200,*.1] HTTP GET /foo", true);
		cl.log(lc, req, res);
		tc.check(INFO, "[200,*.1] HTTP GET /foo", true);

		cl.resetStackTraces();

		cl.log(lcw, req, res);
		tc.check(INFO, "[200,*.1] HTTP GET /foo", true);
		cl.log(lcw, req, res);
		tc.check(INFO, "[200,*.1] HTTP GET /foo", true);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Various logging options.
	//------------------------------------------------------------------------------------------------------------------

	@Test
	public void d01a_requestLength_on() {
		RestCallLoggerConfig lc =
			config()
			.rules(
				rule().codes("*").req(MEDIUM).build()
			)
			.build();
		RestCallLoggerConfig lcw = wrapped(lc);

		TestLogger tc = new TestLogger();
		BasicRestCallLogger cl = logger(tc).resetStackTraces();
		MockServletRequest req = req().attribute("RequestBody", "foo".getBytes());
		MockServletResponse res = res(200);

		cl.log(lc, req, res);
		tc.check(INFO, "*\tRequest length: 3 bytes\n*", false);

		cl.log(lcw, req, res);
		tc.check(INFO, "*\tRequest length: 3 bytes\n*", false);
	}

	@Test
	public void d01b_requestLength_off() {
		RestCallLoggerConfig lc =
			config()
			.rules(
				rule().codes("*").req(SHORT).build()
			)
			.build();
		RestCallLoggerConfig lcw = wrapped(lc);

		TestLogger tc = new TestLogger();
		BasicRestCallLogger cl = logger(tc).resetStackTraces();
		MockServletRequest req = req().attribute("RequestBody", "foo".getBytes());
		MockServletResponse res = res(200);

		cl.log(lc, req, res);
		tc.check(INFO, "!*\tRequest length: 3 bytes\n*", false);

		cl.log(lcw, req, res);
		tc.check(INFO, "!*\tRequest length: 3 bytes\n*", false);
	}

	@Test
	public void d01c_requestLength_none() {
		RestCallLoggerConfig lc =
			config()
			.rules(
				rule().codes("*").req(LONG).build()
			)
			.build();
		RestCallLoggerConfig lcw = wrapped(lc);

		TestLogger tc = new TestLogger();
		BasicRestCallLogger cl = logger(tc).resetStackTraces();
		MockServletRequest req = req();
		MockServletResponse res = res(200);

		cl.log(lc, req, res);
		tc.check(INFO, "!*\tRequest length: 3 bytes\n*", false);

		cl.log(lcw, req, res);
		tc.check(INFO, "!*\tRequest length: 3 bytes\n*", false);
	}

	@Test
	public void d02a_responseCode_on() {
		RestCallLoggerConfig lc =
			config()
			.rules(
				rule().codes("*").res(MEDIUM).build()
			)
			.build();
		RestCallLoggerConfig lcw = wrapped(lc);

		TestLogger tc = new TestLogger();
		BasicRestCallLogger cl = logger(tc).resetStackTraces();
		MockServletRequest req = req();
		MockServletResponse res = res(200);

		cl.log(lc, req, res);
		tc.check(INFO, "*\tResponse code: 200\n*", false);

		cl.log(lcw, req, res);
		tc.check(INFO, "*\tResponse code: 200\n*", false);
	}

	@Test
	public void d02b_responseCode_off() {
		RestCallLoggerConfig lc =
			config()
			.rules(
				rule().codes("*").res(SHORT).build()
			)
			.build();
		RestCallLoggerConfig lcw = wrapped(lc);

		TestLogger tc = new TestLogger();
		BasicRestCallLogger cl = logger(tc).resetStackTraces();
		MockServletRequest req = req();
		MockServletResponse res = res(200);

		cl.log(lc, req, res);
		tc.check(INFO, "!*\tResponse code: 200\n*", false);

		cl.log(lcw, req, res);
		tc.check(INFO, "!*\tResponse code: 200\n*", false);
	}

	@Test
	public void d03a_responseLength_on() {
		RestCallLoggerConfig lc =
			config()
			.rules(
				rule().codes("*").res(MEDIUM).build()
			)
			.build();
		RestCallLoggerConfig lcw = wrapped(lc);

		TestLogger tc = new TestLogger();
		BasicRestCallLogger cl = logger(tc).resetStackTraces();
		MockServletRequest req = req().attribute("ResponseBody", "foo".getBytes());
		MockServletResponse res = res(200);

		cl.log(lc, req, res);
		tc.check(INFO, "*\tResponse length: 3 bytes\n*", false);

		cl.log(lcw, req, res);
		tc.check(INFO, "*\tResponse length: 3 bytes\n*", false);
	}

	@Test
	public void d03b_responseLength_off() {
		RestCallLoggerConfig lc =
			config()
			.rules(
				rule().codes("*").res(SHORT).build()
			)
			.build();
		RestCallLoggerConfig lcw = wrapped(lc);

		TestLogger tc = new TestLogger();
		BasicRestCallLogger cl = logger(tc).resetStackTraces();
		MockServletRequest req = req().attribute("ResponseBody", "foo".getBytes());
		MockServletResponse res = res(200);

		cl.log(lc, req, res);
		tc.check(INFO, "!*\tResponse length: 3 bytes\n*", false);

		cl.log(lcw, req, res);
		tc.check(INFO, "!*\tResponse length: 3 bytes\n*", false);
	}

	@Test
	public void d03c_responseLength_nonef() {
		RestCallLoggerConfig lc =
			config()
			.rules(
				rule().codes("*").res(MEDIUM).build()
			)
			.build();
		RestCallLoggerConfig lcw = wrapped(lc);

		TestLogger tc = new TestLogger();
		BasicRestCallLogger cl = logger(tc).resetStackTraces();
		MockServletRequest req = req();
		MockServletResponse res = res(200);

		cl.log(lc, req, res);
		tc.check(INFO, "!*\tResponse length: 3 bytes\n*", false);

		cl.log(lcw, req, res);
		tc.check(INFO, "!*\tResponse length: 3 bytes\n*", false);
	}

	@Test
	public void d04a_execTime_on() {
		RestCallLoggerConfig lc =
			config()
			.rules(
				rule().codes("*").res(MEDIUM).build()
			)
			.build();
		RestCallLoggerConfig lcw = wrapped(lc);

		TestLogger tc = new TestLogger();
		BasicRestCallLogger cl = logger(tc).resetStackTraces();
		MockServletRequest req = req().attribute("ExecTime", 123l);
		MockServletResponse res = res(200);

		cl.log(lc, req, res);
		tc.check(INFO, "*\tExec time: 123ms\n*", false);

		cl.log(lcw, req, res);
		tc.check(INFO, "*\tExec time: 123ms\n*", false);
	}

	@Test
	public void d04b_execTime_off() {
		RestCallLoggerConfig lc =
			config()
			.rules(
				rule().codes("*").res(SHORT).build()
			)
			.build();
		RestCallLoggerConfig lcw = wrapped(lc);

		TestLogger tc = new TestLogger();
		BasicRestCallLogger cl = logger(tc).resetStackTraces();
		MockServletRequest req = req().attribute("ExecTime", 123l);
		MockServletResponse res = res(200);

		cl.log(lc, req, res);
		tc.check(INFO, "!*\tExec time: 123ms\n*", false);

		cl.log(lcw, req, res);
		tc.check(INFO, "!*\tExec time: 123ms\n*", false);
	}

	@Test
	public void d04c_execTime_none() {
		RestCallLoggerConfig lc =
			config()
			.rules(
				rule().codes("*").res(MEDIUM).build()
			)
			.build();
		RestCallLoggerConfig lcw = wrapped(lc);

		TestLogger tc = new TestLogger();
		BasicRestCallLogger cl = logger(tc).resetStackTraces();
		MockServletRequest req = req();
		MockServletResponse res = res(200);

		cl.log(lc, req, res);
		tc.check(INFO, "!*\tExec time: 123ms\n*", false);

		cl.log(lcw, req, res);
		tc.check(INFO, "!*\tExec time: 123ms\n*", false);
	}

	@Test
	public void d05a_requestHeaders_on() {
		RestCallLoggerConfig lc =
			config()
			.rules(
				rule().codes("*").req(MEDIUM).build()
			)
			.build();
		RestCallLoggerConfig lcw = wrapped(lc);

		TestLogger tc = new TestLogger();
		BasicRestCallLogger cl = logger(tc).resetStackTraces();
		MockServletRequest req = req().header("Foo", "bar");
		MockServletResponse res = res(200);

		cl.log(lc, req, res);
		tc.check(INFO, "*---Request Headers---\n\tFoo: bar\n*", false);

		cl.log(lcw, req, res);
		tc.check(INFO, "*---Request Headers---\n\tFoo: bar\n*", false);
	}

	@Test
	public void d05b_requestHeaders_off() {
		RestCallLoggerConfig lc =
			config()
			.rules(
				rule().codes("*").req(SHORT).build()
			)
			.build();
		RestCallLoggerConfig lcw = wrapped(lc);

		TestLogger tc = new TestLogger();
		BasicRestCallLogger cl = logger(tc).resetStackTraces();
		MockServletRequest req = req().header("Foo", "bar");
		MockServletResponse res = res(200);

		cl.log(lc, req, res);
		tc.check(INFO, "!*---Request Headers---*", false);

		cl.log(lcw, req, res);
		tc.check(INFO, "!*---Request Headers---*", false);
	}

	@Test
	public void d06a_responseHeaders_on() {
		RestCallLoggerConfig lc =
			config()
			.rules(
				rule().codes("*").res(MEDIUM).build()
			)
			.build();
		RestCallLoggerConfig lcw = wrapped(lc);

		TestLogger tc = new TestLogger();
		BasicRestCallLogger cl = logger(tc).resetStackTraces();
		MockServletRequest req = req();
		MockServletResponse res = res(200).header("Foo", "bar");;

		cl.log(lc, req, res);
		tc.check(INFO, "*---Response Headers---\n\tFoo: bar\n*", false);

		cl.log(lcw, req, res);
		tc.check(INFO, "*---Response Headers---\n\tFoo: bar\n*", false);
	}

	@Test
	public void d06b_responseHeaders_off() {
		RestCallLoggerConfig lc =
			config()
			.rules(
				rule().codes("*").res(SHORT).build()
			)
			.build();
		RestCallLoggerConfig lcw = wrapped(lc);

		TestLogger tc = new TestLogger();
		BasicRestCallLogger cl = logger(tc).resetStackTraces();
		MockServletRequest req = req();
		MockServletResponse res = res(200).header("Foo", "bar");;

		cl.log(lc, req, res);
		tc.check(INFO, "!*---Response Headers---*", false);

		cl.log(lcw, req, res);
		tc.check(INFO, "!*---Response Headers---*", false);
	}

	@Test
	public void d07a_requestBody_on() {
		RestCallLoggerConfig lc =
			config()
			.rules(
				rule().codes("*").req(LONG).build()
			)
			.build();
		RestCallLoggerConfig lcw = wrapped(lc);

		TestLogger tc = new TestLogger();
		BasicRestCallLogger cl = logger(tc).resetStackTraces();
		MockServletRequest req = req().attribute("RequestBody", "foo".getBytes());
		MockServletResponse res = res(200);

		cl.log(lc, req, res);
		tc.check(INFO, "*---Request Body UTF-8---\nfoo\n*", false);
		tc.check(INFO, "*---Request Body Hex---\n66 6F 6F\n*", false);

		cl.log(lcw, req, res);
		tc.check(INFO, "*---Request Body UTF-8---\nfoo\n*", false);
		tc.check(INFO, "*---Request Body Hex---\n66 6F 6F\n*", false);
	}

	@Test
	public void d07b_requestBody_off() {
		RestCallLoggerConfig lc =
			config()
			.rules(
				rule().codes("*").req(MEDIUM).build()
			)
			.build();
		RestCallLoggerConfig lcw = wrapped(lc);

		TestLogger tc = new TestLogger();
		BasicRestCallLogger cl = logger(tc).resetStackTraces();
		MockServletRequest req = req().attribute("RequestBody", "foo".getBytes());
		MockServletResponse res = res(200);

		cl.log(lc, req, res);
		tc.check(INFO, "!*---Request Body UTF-8---\nfoo\n*", false);
		tc.check(INFO, "!*---Request Body Hex---\n66 6F 6F\n*", false);

		cl.log(lcw, req, res);
		tc.check(INFO, "!*---Request Body UTF-8---\nfoo\n*", false);
		tc.check(INFO, "!*---Request Body Hex---\n66 6F 6F\n*", false);
	}

	@Test
	public void d07c_requestBody_none() {
		RestCallLoggerConfig lc =
			config()
			.rules(
				rule().codes("*").req(LONG).build()
			)
			.build();
		RestCallLoggerConfig lcw = wrapped(lc);

		TestLogger tc = new TestLogger();
		BasicRestCallLogger cl = logger(tc).resetStackTraces();
		MockServletRequest req = req();
		MockServletResponse res = res(200);

		cl.log(lc, req, res);
		tc.check(INFO, "!*---Request Body UTF-8---\nfoo\n*", false);
		tc.check(INFO, "!*---Request Body Hex---\n66 6F 6F\n*", false);

		cl.log(lcw, req, res);
		tc.check(INFO, "!*---Request Body UTF-8---\nfoo\n*", false);
		tc.check(INFO, "!*---Request Body Hex---\n66 6F 6F\n*", false);
	}

	@Test
	public void d08a_responseBody_on() {
		RestCallLoggerConfig lc =
			config()
			.rules(
				rule().codes("*").res(LONG).build()
			)
			.build();
		RestCallLoggerConfig lcw = wrapped(lc);

		TestLogger tc = new TestLogger();
		BasicRestCallLogger cl = logger(tc).resetStackTraces();
		MockServletRequest req = req().attribute("ResponseBody", "foo".getBytes());
		MockServletResponse res = res(200);

		cl.log(lc, req, res);
		tc.check(INFO, "*---Response Body UTF-8---\nfoo\n*", false);
		tc.check(INFO, "*---Response Body Hex---\n66 6F 6F\n*", false);

		cl.log(lcw, req, res);
		tc.check(INFO, "*---Response Body UTF-8---\nfoo\n*", false);
		tc.check(INFO, "*---Response Body Hex---\n66 6F 6F\n*", false);
	}

	@Test
	public void d08b_responseBody_off() {
		RestCallLoggerConfig lc =
			config()
			.rules(
				rule().codes("*").res(MEDIUM).build()
			)
			.build();
		RestCallLoggerConfig lcw = wrapped(lc);

		TestLogger tc = new TestLogger();
		BasicRestCallLogger cl = logger(tc).resetStackTraces();
		MockServletRequest req = req().attribute("ResponseBody", "foo".getBytes());
		MockServletResponse res = res(200);

		cl.log(lc, req, res);
		tc.check(INFO, "!*---Response Body UTF-8---\nfoo\n*", false);
		tc.check(INFO, "!*---Response Body Hex---\n66 6F 6F\n*", false);

		cl.log(lcw, req, res);
		tc.check(INFO, "!*---Response Body UTF-8---\nfoo\n*", false);
		tc.check(INFO, "!*---Response Body Hex---\n66 6F 6F\n*", false);
	}

	@Test
	public void d08c_responseBody_none() {
		RestCallLoggerConfig lc =
			config()
			.rules(
				rule().codes("*").res(LONG).build()
			)
			.build();
		RestCallLoggerConfig lcw = wrapped(lc);

		TestLogger tc = new TestLogger();
		BasicRestCallLogger cl = logger(tc).resetStackTraces();
		MockServletRequest req = req();
		MockServletResponse res = res(200);

		cl.log(lc, req, res);
		tc.check(INFO, "!*---Response Body UTF-8---\nfoo\n*", false);
		tc.check(INFO, "!*---Response Body Hex---\n66 6F 6F\n*", false);

		cl.log(lcw, req, res);
		tc.check(INFO, "!*---Response Body UTF-8---\nfoo\n*", false);
		tc.check(INFO, "!*---Response Body Hex---\n66 6F 6F\n*", false);
	}
}
