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
package org.apache.juneau.rest.client;

import static org.junit.runners.MethodSorters.*;
import static org.apache.juneau.testutils.StreamUtils.*;

import java.io.*;
import java.util.logging.*;

import org.apache.http.entity.*;
import org.apache.juneau.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.mock.*;
import org.apache.juneau.rest.servlet.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class RestClient_Logging_Test {

	public static class ABean {
		public int f;
		static ABean get() {
			ABean x = new ABean();
			x.f = 1;
			return x;
		}
	}

	private static ABean bean = ABean.get();

	@Rest
	public static class A extends BasicRestObject {
		@RestPost
		public ABean bean(@Content ABean b) {
			return b;
		}
		@RestPost
		public InputStream stream(@Content InputStream b, org.apache.juneau.rest.RestResponse res) {
			res.setHeader("Content-Encoding", "identity");
			return b;
		}
		@RestGet
		public ABean bean() {
			return bean;
		}
	}

	@Test
	public void a01_logToConsole() throws Exception {
		MockConsole c = MockConsole.create();
		MockLogger l = MockLogger.create();

		client().logRequests(DetailLevel.NONE,Level.SEVERE,null).logToConsole().logger(l).console(c).build().post("/bean",bean).complete();
		c.assertContents().is("");
		c.reset();

		client().logRequests(DetailLevel.SIMPLE,Level.SEVERE,null).logToConsole().logger(l).console(c).build().post("/bean",bean).complete();
		c.assertContents().isContains("HTTP POST http://localhost/bean, HTTP/1.1 200");
		c.reset();

		client().logRequests(DetailLevel.FULL,Level.SEVERE,null).logToConsole().logger(l).console(c).build().post("/bean",bean).complete();
		c.assertContents().asTrimmed().isLines(
			"=== HTTP Call (outgoing) ======================================================",
			"=== REQUEST ===",
			"POST http://localhost/bean",
			"---request headers---",
			"	Accept: application/json5",
			"---request entity---",
			"	Content-Type: application/json5",
			"---request content---",
			"{f:1}",
			"=== RESPONSE ===",
			"HTTP/1.1 200 ",
			"---response headers---",
			"	Content-Type: application/json5",
			"---response content---",
			"{f:1}",
			"=== END ======================================================================="
		);
		c.reset();

		client().logRequests(DetailLevel.FULL,Level.SEVERE,null).logToConsole().logger(l).console(c).build().get("/bean").complete();
		c.assertContents().asTrimmed().isLines(
			"=== HTTP Call (outgoing) ======================================================",
			"=== REQUEST ===",
			"GET http://localhost/bean",
			"---request headers---",
			"	Accept: application/json5",
			"=== RESPONSE ===",
			"HTTP/1.1 200 ",
			"---response headers---",
			"	Content-Type: application/json5",
			"---response content---",
			"{f:1}",
			"=== END ======================================================================="
		);
		c.reset();

		clientPlain().logRequests(DetailLevel.FULL,Level.SEVERE,null).logToConsole().logger(l).console(c).build().post("/stream",new InputStreamEntity(inputStream("foo"))).complete();
		c.assertContents().asTrimmed().isLines(
			"=== HTTP Call (outgoing) ======================================================",
			"=== REQUEST ===",
			"POST http://localhost/stream",
			"---request headers---",
			"---request entity---",
			"=== RESPONSE ===",
			"HTTP/1.1 200 ",
			"---response headers---",
			"	Content-Encoding: identity",
			"---response content---",
			"foo",
			"=== END ======================================================================="
		);
		c.reset();

		clientPlain().logRequests(DetailLevel.FULL,Level.SEVERE,(req,res)->false).logToConsole().logger(l).console(c).build().post("/stream",new InputStreamEntity(inputStream("foo"))).complete();
		c.assertContents().isEmpty();
		c.reset();
	}

	@Test
	public void a02_logTo() throws Exception {
		MockLogger l = MockLogger.create();

		client().logRequests(DetailLevel.NONE,Level.SEVERE,null).logToConsole().logger(l).build().post("/bean",bean).complete();
		l.assertContents().is("");
		l.assertRecordCount().is(0);
		l.reset();

		client().logger(l).logRequests(DetailLevel.SIMPLE,Level.WARNING,null).build().post("/bean",bean).complete();
		l.assertLastLevel(Level.WARNING);
		l.assertLastMessage().is("HTTP POST http://localhost/bean, HTTP/1.1 200 ");
		l.assertContents().isContains("WARNING: HTTP POST http://localhost/bean, HTTP/1.1 200");
		l.reset();

		client().logger(l).logRequests(DetailLevel.FULL,Level.WARNING,null).build().post("/bean",bean).complete();
		l.assertLastLevel(Level.WARNING);
		l.assertLastMessage().asTrimmed().isLines(
			"=== HTTP Call (outgoing) ======================================================",
			"=== REQUEST ===",
			"POST http://localhost/bean",
			"---request headers---",
			"	Accept: application/json5",
			"---request entity---",
			"	Content-Type: application/json5",
			"---request content---",
			"{f:1}",
			"=== RESPONSE ===",
			"HTTP/1.1 200 ",
			"---response headers---",
			"	Content-Type: application/json5",
			"---response content---",
			"{f:1}",
			"=== END ======================================================================="
		);
		l.assertContents().asTrimmed().asJavaStrings().isContains(
			"WARNING: ",
			"=== HTTP Call (outgoing) ======================================================",
			"=== REQUEST ===",
			"POST http://localhost/bean",
			"---request headers---",
			"	Accept: application/json5",
			"---request entity---",
			"	Content-Type: application/json5",
			"---request content---",
			"{f:1}",
			"=== RESPONSE ===",
			"HTTP/1.1 200 ",
			"---response headers---",
			"	Content-Type: application/json5",
			"---response content---",
			"{f:1}",
			"=== END ======================================================================="
		);
	}

	public static class A1 extends BasicRestCallInterceptor {
		@Override /* RestCallInterceptor */
		public void onConnect(RestRequest req, RestResponse res) throws Exception {
			super.onConnect(req,res);
			req.log(Level.WARNING,"Foo");
			req.log(Level.WARNING,new RuntimeException(),"Bar");
			res.log(Level.WARNING,"Baz");
			res.log(Level.WARNING,new RuntimeException(),"Qux");
			req.log(Level.WARNING,(Throwable)null,"Quux");
		}
	}

	@Test
	public void a04_other() throws Exception {
		MockLogger ml = MockLogger.create();
		MockConsole mc = MockConsole.create();
		client().logger(ml).interceptors(A1.class).build().post("/bean",bean).complete();
		ml.assertRecordCount().is(5);
		ml.reset();
		client().logger(ml).logToConsole().console(mc).interceptors(A1.class).build().post("/bean",bean).complete();
		ml.assertRecordCount().is(5);
		ml.assertContents().isContains(
			"WARNING: Foo",
			"WARNING: Bar",
			"WARNING: Baz",
			"WARNING: Qux",
			"WARNING: Quux",
			"at org.apache.juneau"
		);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Helper methods.
	//------------------------------------------------------------------------------------------------------------------

	private static RestClient.Builder client() {
		return MockRestClient.create(A.class).json5();
	}

	private static RestClient.Builder clientPlain() {
		return MockRestClient.create(A.class);
	}
}
