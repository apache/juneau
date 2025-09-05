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
package org.apache.juneau.rest.test.client;

import static org.apache.juneau.TestUtils.*;
import static org.apache.juneau.utest.utils.Constants.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.util.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.logger.*;
import org.apache.juneau.rest.servlet.*;
import org.apache.juneau.serializer.annotation.*;
import org.apache.juneau.testutils.pojos.*;

import jakarta.servlet.http.*;

/**
 * JUnit automated testcase resource.
 */
@Rest(
	path="/testThirdPartyProxy",
	callLogger=BasicDisabledCallLogger.class
)
@SerializerConfig(addRootType="true",addBeanTypes="true")
@SuppressWarnings({"serial"})
public class ThirdPartyProxyResource extends BasicRestServlet {

	public static FileWriter logFile;
	static {
		try {
			new File("./target/logs").mkdirs();
			logFile = new FileWriter("./target/logs/third-party-proxy-resource.txt", false);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@RestStartCall
	public static void startCall(HttpServletRequest req) {
		try {
			logFile.append("START["+new Date()+"]-").append(req.getQueryString()).append("\n");
			logFile.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@RestPreCall
	public static void preCall(HttpServletRequest req) {
		try {
			logFile.append("PRE["+new Date()+"]-").append(req.getQueryString()).append("\n");
			logFile.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@RestPostCall
	public static void postCall(HttpServletRequest req) {
		try {
			logFile.append("POST["+new Date()+"]-").append(req.getQueryString()).append("\n");
			logFile.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@RestEndCall
	public static void endCall(HttpServletRequest req) {
		try {
			var e = (Throwable)req.getAttribute("Exception");
			var execTime = (Long)req.getAttribute("ExecTime");
			logFile.append("END["+new Date()+"]-").append(req.getQueryString()).append(", time=").append(""+execTime).append(", exception=").append(e == null ? null : e.toString()).append("\n");
			logFile.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Header tests
	//-----------------------------------------------------------------------------------------------------------------

	@RestGet(path="/primitiveHeaders")
	public String primitiveHeaders(
			@Header("a") String a,
			@Header("an") String an,
			@Header("b") int b,
			@Header("c") Integer c,
			@Header("cn") Integer cn,
			@Header("d") Boolean d,
			@Header("e") float e,
			@Header("f") Float f
		) {

		assertEquals("foo", a);
		assertNull(an);
		assertEquals(123, b);
		assertEquals(123, (int)c);
		assertNull(cn);
		assertTrue(d);
		assertEquals(1f, e, 0.1f);
		assertEquals(1f, f, 0.1f);
		return "OK";
	}

	@RestGet(path="/primitiveCollectionHeaders")
	public String primitiveCollectionHeaders(
			@Header("a") int[][][] a,
			@Header("b") Integer[][][] b,
			@Header("c") String[][][] c,
			@Header("d") List<Integer> d,
			@Header("e") List<List<List<Integer>>> e,
			@Header("f") List<Integer[][][]> f,
			@Header("g") List<int[][][]> g,
			@Header("h") List<String> h
		) {

		assertJson("[[[1,2],null],null]", a);
		assertJson("[[[1,null],null],null]", b);
		assertJson("[[['foo',null],null],null]", c);
		assertJson("[1,null]", d);
		assertJson("[[[1,null],null],null]", e);
		assertJson("[[[[1,null],null],null],null]", f);
		assertJson("[[[[1,2],null],null],null]", g);
		assertJson("['foo','bar',null]", h);

		assertInstanceOf(Integer.class, d.get(0));
		assertInstanceOf(Integer.class, e.get(0).get(0).get(0));
		assertInstanceOf(Integer[][][].class, f.get(0));
		assertInstanceOf(int[][][].class, g.get(0));

		return "OK";
	}

	@RestGet(path="/beanHeaders")
	public String beanHeaders(
			@Header(name="a") @Schema(cf="uon") ABean a,
			@Header(name="an") @Schema(cf="uon") ABean an,
			@Header(name="b") @Schema(cf="uon") ABean[][][] b,
			@Header(name="c") @Schema(cf="uon") List<ABean> c,
			@Header(name="d") @Schema(cf="uon") List<ABean[][][]> d,
			@Header(name="e") @Schema(cf="uon") Map<String,ABean> e,
			@Header(name="f") @Schema(cf="uon") Map<String,List<ABean>> f,
			@Header(name="g") @Schema(cf="uon") Map<String,List<ABean[][][]>> g,
			@Header(name="h") @Schema(cf="uon") Map<Integer,List<ABean>> h
		) {

		assertJson("{a:1,b:'foo'}", a);
		assertNull(an);
		assertJson("[[[{a:1,b:'foo'},null],null],null]", b);
		assertJson("[{a:1,b:'foo'},null]", c);
		assertJson("[[[[{a:1,b:'foo'},null],null],null],null]", d);
		assertJson("{foo:{a:1,b:'foo'}}", e);
		assertJson("{foo:[{a:1,b:'foo'}]}", f);
		assertJson("{foo:[[[[{a:1,b:'foo'},null],null],null],null]}", g);
		assertJson("{'1':[{a:1,b:'foo'}]}", h);

		assertInstanceOf(ABean.class, c.get(0));
		assertInstanceOf(ABean[][][].class, d.get(0));
		assertInstanceOf(ABean.class, e.get("foo"));
		assertInstanceOf(ABean.class, f.get("foo").get(0));
		assertInstanceOf(ABean[][][].class, g.get("foo").get(0));
		assertInstanceOf(Integer.class, h.keySet().iterator().next());
		assertInstanceOf(ABean.class, h.values().iterator().next().get(0));
		return "OK";
	}

	@RestGet(path="/typedBeanHeaders")
	public String typedBeanHeaders(
			@Header("a") @Schema(cf="uon") TypedBean a,
			@Header("an") @Schema(cf="uon") TypedBean an,
			@Header("b") @Schema(cf="uon") TypedBean[][][] b,
			@Header("c") @Schema(cf="uon") List<TypedBean> c,
			@Header("d") @Schema(cf="uon") List<TypedBean[][][]> d,
			@Header("e") @Schema(cf="uon") Map<String,TypedBean> e,
			@Header("f") @Schema(cf="uon") Map<String,List<TypedBean>> f,
			@Header("g") @Schema(cf="uon") Map<String,List<TypedBean[][][]>> g,
			@Header("h") @Schema(cf="uon") Map<Integer,List<TypedBean>> h
		) {

		assertJson("{a:1,b:'foo'}", a);
		assertNull(an);
		assertJson("[[[{a:1,b:'foo'},null],null],null]", b);
		assertJson("[{a:1,b:'foo'},null]", c);
		assertJson("[[[[{a:1,b:'foo'},null],null],null],null]", d);
		assertJson("{foo:{a:1,b:'foo'}}", e);
		assertJson("{foo:[{a:1,b:'foo'}]}", f);
		assertJson("{foo:[[[[{a:1,b:'foo'},null],null],null],null]}", g);
		assertJson("{'1':[{a:1,b:'foo'}]}", h);

		assertInstanceOf(TypedBeanImpl.class, a);
		assertInstanceOf(TypedBeanImpl.class, b[0][0][0]);
		assertInstanceOf(TypedBeanImpl.class, c.get(0));
		assertInstanceOf(TypedBeanImpl.class, d.get(0)[0][0][0]);
		assertInstanceOf(TypedBeanImpl.class, e.get("foo"));
		assertInstanceOf(TypedBeanImpl.class, f.get("foo").get(0));
		assertInstanceOf(TypedBeanImpl.class, g.get("foo").get(0)[0][0][0]);
		assertInstanceOf(Integer.class, h.keySet().iterator().next());
		assertInstanceOf(TypedBeanImpl.class, h.get(1).get(0));

		return "OK";
	}

	@RestGet(path="/swappedObjectHeaders")
	public String swappedObjectHeaders(
			@Header("a") @Schema(cf="uon") SwappedObject a,
			@Header("b") @Schema(cf="uon") SwappedObject[][][] b,
			@Header("c") @Schema(cf="uon") Map<SwappedObject,SwappedObject> c,
			@Header("d") @Schema(cf="uon") Map<SwappedObject,SwappedObject[][][]> d
		) {

		assertJson("'swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/'", a);
		assertJson("[[['swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/',null],null],null]", b);
		assertJson("{'swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/':'swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/'}", c);
		assertJson("{'swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/':[[['swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/',null],null],null]}", d);

		assertInstanceOf(SwappedObject.class, a);
		assertInstanceOf(SwappedObject.class, b[0][0][0]);
		assertInstanceOf(SwappedObject.class, c.keySet().iterator().next());
		assertInstanceOf(SwappedObject.class, c.values().iterator().next());
		assertInstanceOf(SwappedObject.class, d.keySet().iterator().next());
		assertInstanceOf(SwappedObject.class, d.values().iterator().next()[0][0][0]);

		return "OK";
	}

	@RestGet(path="/implicitSwappedObjectHeaders")
	public String implicitSwappedObjectHeaders(
			@Header("a") @Schema(cf="uon") ImplicitSwappedObject a,
			@Header("b") @Schema(cf="uon") ImplicitSwappedObject[][][] b,
			@Header("c") @Schema(cf="uon") Map<ImplicitSwappedObject,ImplicitSwappedObject> c,
			@Header("d") @Schema(cf="uon") Map<ImplicitSwappedObject,ImplicitSwappedObject[][][]> d
		) {

		assertJson("'swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/'", a);
		assertJson("[[['swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/',null],null],null]", b);
		assertJson("{'swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/':'swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/'}", c);
		assertJson("{'swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/':[[['swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/',null],null],null]}", d);

		assertInstanceOf(ImplicitSwappedObject.class, a);
		assertInstanceOf(ImplicitSwappedObject.class, b[0][0][0]);
		assertInstanceOf(ImplicitSwappedObject.class, c.keySet().iterator().next());
		assertInstanceOf(ImplicitSwappedObject.class, c.values().iterator().next());
		assertInstanceOf(ImplicitSwappedObject.class, d.keySet().iterator().next());
		assertInstanceOf(ImplicitSwappedObject.class, d.values().iterator().next()[0][0][0]);

		return "OK";
	}

	@RestGet(path="/enumHeaders")
	public String enumHeaders(
			@Header("a") @Schema(cf="uon") TestEnum a,
			@Header("an") @Schema(cf="uon") TestEnum an,
			@Header("b") @Schema(cf="uon") TestEnum[][][] b,
			@Header("c") @Schema(cf="uon") List<TestEnum> c,
			@Header("d") @Schema(cf="uon") List<List<List<TestEnum>>> d,
			@Header("e") @Schema(cf="uon") List<TestEnum[][][]> e,
			@Header("f") @Schema(cf="uon") Map<TestEnum,TestEnum> f,
			@Header("g") @Schema(cf="uon") Map<TestEnum,TestEnum[][][]> g,
			@Header("h") @Schema(cf="uon") Map<TestEnum,List<TestEnum[][][]>> h
		) {

		assertEquals(TestEnum.TWO, a);
		assertNull(an);
		assertJson("[[['TWO',null],null],null]", b);
		assertJson("['TWO',null]", c);
		assertJson("[[['TWO',null],null],null]", d);
		assertJson("[[[['TWO',null],null],null],null]", e);
		assertJson("{ONE:'TWO'}", f);
		assertJson("{ONE:[[['TWO',null],null],null]}", g);
		assertJson("{ONE:[[[['TWO',null],null],null],null]}", h);

		assertInstanceOf(TestEnum.class, c.get(0));
		assertInstanceOf(TestEnum.class, d.get(0).get(0).get(0));
		assertInstanceOf(TestEnum[][][].class, e.get(0));
		assertInstanceOf(TestEnum.class, f.keySet().iterator().next());
		assertInstanceOf(TestEnum.class, f.values().iterator().next());
		assertInstanceOf(TestEnum.class, g.keySet().iterator().next());
		assertInstanceOf(TestEnum[][][].class, g.values().iterator().next());
		assertInstanceOf(TestEnum.class, h.keySet().iterator().next());
		assertInstanceOf(TestEnum[][][].class, h.values().iterator().next().get(0));

		return "OK";
	}

	@RestGet(path="/mapHeader")
	public String mapHeader(
		@Header("a") String a,
		@Header(name="b") @Schema(allowEmptyValue=true) String b,
		@Header("c") String c
	) {

		assertEquals("foo", a);
		assertEquals("", b);
		assertEquals(null, c);

		return "OK";
	}

	@RestGet(path="/beanHeader")
	public String beanHeader(
		@Header("a") String a,
		@Header(name="b") @Schema(allowEmptyValue=true) String b,
		@Header("c") String c
	) {

		assertEquals("foo", a);
		assertEquals("", b);
		assertEquals(null, c);

		return "OK";
	}

	@RestGet(path="/headerList")
	public String headerList(
		@Header("a") String a,
		@Header(name="b") @Schema(allowEmptyValue=true) String b,
		@Header("c") String c
	) {

		assertEquals("foo", a);
		assertEquals("", b);
		assertEquals(null, c);

		return "OK";
	}

	@RestGet(path="/headerIfNE1")
	public String headerIfNE1(
		@Header("a") String a
	) {

		assertEquals("foo", a);

		return "OK";
	}

	@RestGet(path="/headerIfNE2")
	public String headerIfNE2(
		@Header("a") String a
	) {

		assertEquals(null, a);

		return "OK";
	}

	@RestGet(path="/headerIfNEMap")
	public String headerIfNEMap(
		@Header("a") String a,
		@Header("b") String b,
		@Header("c") String c
	) {

		assertEquals("foo", a);
		assertEquals(null, b);
		assertEquals(null, c);

		return "OK";
	}

	@RestGet(path="/headerIfNEBean")
	public String headerIfNEBean(
		@Header("a") String a,
		@Header("b") String b,
		@Header("c") String c
	) {

		assertEquals("foo", a);
		assertEquals(null, b);
		assertEquals(null, c);

		return "OK";
	}

	@RestGet(path="/headerIfNEnameValuePairs")
	public String headerIfNEnameValuePairs(
		@Header("a") String a,
		@Header("b") String b,
		@Header("c") String c
	) {

		assertEquals("foo", a);
		assertEquals(null, b);
		assertEquals(null, c);

		return "OK";
	}


	//-----------------------------------------------------------------------------------------------------------------
	// Query tests
	//-----------------------------------------------------------------------------------------------------------------

	@RestGet(path="/primitiveQueries")
	public String primitiveQueries(
			@Query("a") String a,
			@Query("an") String an,
			@Query("b") int b,
			@Query("c") Integer c,
			@Query("cn") Integer cn,
			@Query("d") Boolean d,
			@Query("e") float e,
			@Query("f") Float f
		) {

		assertEquals("foo", a);
		assertNull(an);
		assertEquals(123, b);
		assertEquals(123, (int)c);
		assertNull(cn);
		assertTrue(d);
		assertEquals(1f, e, 0.1f);
		assertEquals(1f, f, 0.1f);
		return "OK";
	}

	@RestGet(path="/primitiveCollectionQueries")
	public String primitiveCollectionQueries(
			@Query("a") int[][][] a,
			@Query("b") Integer[][][] b,
			@Query("c") String[][][] c,
			@Query("d") List<Integer> d,
			@Query("e") List<List<List<Integer>>> e,
			@Query("f") List<Integer[][][]> f,
			@Query("g") List<int[][][]> g,
			@Query("h") List<String> h
		) {

		assertJson("[[[1,2],null],null]", a);
		assertJson("[[[1,null],null],null]", b);
		assertJson("[[['foo',null],null],null]", c);
		assertJson("[1,null]", d);
		assertJson("[[[1,null],null],null]", e);
		assertJson("[[[[1,null],null],null],null]", f);
		assertJson("[[[[1,2],null],null],null]", g);
		assertJson("['foo','bar',null]", h);

		assertInstanceOf(Integer.class, d.get(0));
		assertInstanceOf(Integer.class, e.get(0).get(0).get(0));
		assertInstanceOf(Integer[][][].class, f.get(0));
		assertInstanceOf(int[][][].class, g.get(0));

		return "OK";
	}

	@RestGet(path="/beanQueries")
	public String beanQueries(
			@Query("a") @Schema(cf="uon") ABean a,
			@Query("an") @Schema(cf="uon") ABean an,
			@Query("b") @Schema(cf="uon") ABean[][][] b,
			@Query("c") @Schema(cf="uon") List<ABean> c,
			@Query("d") @Schema(cf="uon") List<ABean[][][]> d,
			@Query("e") @Schema(cf="uon") Map<String,ABean> e,
			@Query("f") @Schema(cf="uon") Map<String,List<ABean>> f,
			@Query("g") @Schema(cf="uon") Map<String,List<ABean[][][]>> g,
			@Query("h") @Schema(cf="uon") Map<Integer,List<ABean>> h
		) {

		assertJson("{a:1,b:'foo'}", a);
		assertNull(an);
		assertJson("[[[{a:1,b:'foo'},null],null],null]", b);
		assertJson("[{a:1,b:'foo'},null]", c);
		assertJson("[[[[{a:1,b:'foo'},null],null],null],null]", d);
		assertJson("{foo:{a:1,b:'foo'}}", e);
		assertJson("{foo:[{a:1,b:'foo'}]}", f);
		assertJson("{foo:[[[[{a:1,b:'foo'},null],null],null],null]}", g);
		assertJson("{'1':[{a:1,b:'foo'}]}", h);

		assertInstanceOf(ABean.class, c.get(0));
		assertInstanceOf(ABean[][][].class, d.get(0));
		assertInstanceOf(ABean.class, e.get("foo"));
		assertInstanceOf(ABean.class, f.get("foo").get(0));
		assertInstanceOf(ABean[][][].class, g.get("foo").get(0));
		assertInstanceOf(Integer.class, h.keySet().iterator().next());
		assertInstanceOf(ABean.class, h.values().iterator().next().get(0));
		return "OK";
	}

	@RestGet(path="/typedBeanQueries")
	public String typedBeanQueries(
			@Query("a") @Schema(cf="uon") TypedBean a,
			@Query("an") @Schema(cf="uon") TypedBean an,
			@Query("b") @Schema(cf="uon") TypedBean[][][] b,
			@Query("c") @Schema(cf="uon") List<TypedBean> c,
			@Query("d") @Schema(cf="uon") List<TypedBean[][][]> d,
			@Query("e") @Schema(cf="uon") Map<String,TypedBean> e,
			@Query("f") @Schema(cf="uon") Map<String,List<TypedBean>> f,
			@Query("g") @Schema(cf="uon") Map<String,List<TypedBean[][][]>> g,
			@Query("h") @Schema(cf="uon") Map<Integer,List<TypedBean>> h
		) {

		assertJson("{a:1,b:'foo'}", a);
		assertNull(an);
		assertJson("[[[{a:1,b:'foo'},null],null],null]", b);
		assertJson("[{a:1,b:'foo'},null]", c);
		assertJson("[[[[{a:1,b:'foo'},null],null],null],null]", d);
		assertJson("{foo:{a:1,b:'foo'}}", e);
		assertJson("{foo:[{a:1,b:'foo'}]}", f);
		assertJson("{foo:[[[[{a:1,b:'foo'},null],null],null],null]}", g);
		assertJson("{'1':[{a:1,b:'foo'}]}", h);

		assertInstanceOf(TypedBeanImpl.class, a);
		assertInstanceOf(TypedBeanImpl.class, b[0][0][0]);
		assertInstanceOf(TypedBeanImpl.class, c.get(0));
		assertInstanceOf(TypedBeanImpl.class, d.get(0)[0][0][0]);
		assertInstanceOf(TypedBeanImpl.class, e.get("foo"));
		assertInstanceOf(TypedBeanImpl.class, f.get("foo").get(0));
		assertInstanceOf(TypedBeanImpl.class, g.get("foo").get(0)[0][0][0]);
		assertInstanceOf(Integer.class, h.keySet().iterator().next());
		assertInstanceOf(TypedBeanImpl.class, h.get(1).get(0));

		return "OK";
	}

	@RestGet(path="/swappedObjectQueries")
	public String swappedObjectQueries(
			@Query("a") @Schema(cf="uon") SwappedObject a,
			@Query("b") @Schema(cf="uon") SwappedObject[][][] b,
			@Query("c") @Schema(cf="uon") Map<SwappedObject,SwappedObject> c,
			@Query("d") @Schema(cf="uon") Map<SwappedObject,SwappedObject[][][]> d
		) {

		assertJson("'swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/'", a);
		assertJson("[[['swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/',null],null],null]", b);
		assertJson("{'swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/':'swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/'}", c);
		assertJson("{'swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/':[[['swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/',null],null],null]}", d);

		assertInstanceOf(SwappedObject.class, a);
		assertInstanceOf(SwappedObject.class, b[0][0][0]);
		assertInstanceOf(SwappedObject.class, c.keySet().iterator().next());
		assertInstanceOf(SwappedObject.class, c.values().iterator().next());
		assertInstanceOf(SwappedObject.class, d.keySet().iterator().next());
		assertInstanceOf(SwappedObject.class, d.values().iterator().next()[0][0][0]);

		return "OK";
	}

	@RestGet(path="/implicitSwappedObjectQueries")
	public String implicitSwappedObjectQueries(
			@Query("a") @Schema(cf="uon") ImplicitSwappedObject a,
			@Query("b") @Schema(cf="uon") ImplicitSwappedObject[][][] b,
			@Query("c") @Schema(cf="uon") Map<ImplicitSwappedObject,ImplicitSwappedObject> c,
			@Query("d") @Schema(cf="uon") Map<ImplicitSwappedObject,ImplicitSwappedObject[][][]> d
		) {

		assertJson("'swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/'", a);
		assertJson("[[['swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/',null],null],null]", b);
		assertJson("{'swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/':'swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/'}", c);
		assertJson("{'swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/':[[['swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/',null],null],null]}", d);

		assertInstanceOf(ImplicitSwappedObject.class, a);
		assertInstanceOf(ImplicitSwappedObject.class, b[0][0][0]);
		assertInstanceOf(ImplicitSwappedObject.class, c.keySet().iterator().next());
		assertInstanceOf(ImplicitSwappedObject.class, c.values().iterator().next());
		assertInstanceOf(ImplicitSwappedObject.class, d.keySet().iterator().next());
		assertInstanceOf(ImplicitSwappedObject.class, d.values().iterator().next()[0][0][0]);

		return "OK";
	}

	@RestGet(path="/enumQueries")
	public String enumQueries(
			@Query("a") @Schema(cf="uon") TestEnum a,
			@Query("an") @Schema(cf="uon") TestEnum an,
			@Query("b") @Schema(cf="uon") TestEnum[][][] b,
			@Query("c") @Schema(cf="uon") List<TestEnum> c,
			@Query("d") @Schema(cf="uon") List<List<List<TestEnum>>> d,
			@Query("e") @Schema(cf="uon") List<TestEnum[][][]> e,
			@Query("f") @Schema(cf="uon") Map<TestEnum,TestEnum> f,
			@Query("g") @Schema(cf="uon") Map<TestEnum,TestEnum[][][]> g,
			@Query("h") @Schema(cf="uon") Map<TestEnum,List<TestEnum[][][]>> h
		) {

		assertEquals(TestEnum.TWO, a);
		assertNull(an);
		assertJson("[[['TWO',null],null],null]", b);
		assertJson("['TWO',null]", c);
		assertJson("[[['TWO',null],null],null]", d);
		assertJson("[[[['TWO',null],null],null],null]", e);
		assertJson("{ONE:'TWO'}", f);
		assertJson("{ONE:[[['TWO',null],null],null]}", g);
		assertJson("{ONE:[[[['TWO',null],null],null],null]}", h);

		assertInstanceOf(TestEnum.class, c.get(0));
		assertInstanceOf(TestEnum.class, d.get(0).get(0).get(0));
		assertInstanceOf(TestEnum[][][].class, e.get(0));
		assertInstanceOf(TestEnum.class, f.keySet().iterator().next());
		assertInstanceOf(TestEnum.class, f.values().iterator().next());
		assertInstanceOf(TestEnum.class, g.keySet().iterator().next());
		assertInstanceOf(TestEnum[][][].class, g.values().iterator().next());
		assertInstanceOf(TestEnum.class, h.keySet().iterator().next());
		assertInstanceOf(TestEnum[][][].class, h.values().iterator().next().get(0));

		return "OK";
	}

	@RestGet(path="/stringQuery1")
	public String stringQuery1(
			@Query("a") int a,
			@Query("b") String b
		) {

		assertEquals(1, a);
		assertEquals("foo", b);

		return "OK";
	}

	@RestGet(path="/stringQuery2")
	public String stringQuery2(
			@Query("a") int a,
			@Query("b") String b
		) {

		assertEquals(1, a);
		assertEquals("foo", b);

		return "OK";
	}

	@RestGet(path="/mapQuery")
	public String mapQuery(
			@Query("a") int a,
			@Query("b") String b
		) {

		assertEquals(1, a);
		assertEquals("foo", b);

		return "OK";
	}

	@RestGet(path="/beanQuery")
	public String beanQuery(
			@Query("a") String a,
			@Query("b") @Schema(allowEmptyValue=true) String b,
			@Query("c") String c
		) {

		assertEquals("foo", a);
		assertEquals("", b);
		assertEquals(null, c);

		return "OK";
	}

	@RestGet(path="/partListQuery")
	public String partListQuery(
		@Query("a") String a,
		@Query("b") @Schema(allowEmptyValue=true) String b,
		@Query("c") String c
	) {

		assertEquals("foo", a);
		assertEquals("", b);
		assertEquals(null, c);

		return "OK";
	}

	@RestGet(path="/queryIfNE1")
	public String queryIfNE1(
		@Query("a") String a
	) {

		assertEquals("foo", a);

		return "OK";
	}

	@RestGet(path="/queryIfNE2")
	public String queryIfNE2(
		@Query("q") String a
	) {

		assertEquals(null, a);

		return "OK";
	}

	@RestGet(path="/queryIfNEMap")
	public String queryIfNEMap(
		@Query("a") String a,
		@Query("b") String b,
		@Query("c") String c
	) {

		assertEquals("foo", a);
		assertEquals(null, b);
		assertEquals(null, c);

		return "OK";
	}

	@RestGet(path="/queryIfNEBean")
	public String queryIfNEBean(
		@Query("a") String a,
		@Query("b") String b,
		@Query("c") String c
	) {

		assertEquals("foo", a);
		assertEquals(null, b);
		assertEquals(null, c);

		return "OK";
	}

	@RestGet(path="/queryIfNEnameValuePairs")
	public String queryIfNEnameValuePairs(
		@Query("a") String a,
		@Query("b") String b,
		@Query("c") String c
	) {

		assertEquals("foo", a);
		assertEquals(null, b);
		assertEquals(null, c);

		return "OK";
	}


	//-----------------------------------------------------------------------------------------------------------------
	// FormData tests
	//-----------------------------------------------------------------------------------------------------------------

	@RestPost(path="/primitiveFormData")
	public String primitiveFormData(
			@FormData("a") String a,
			@FormData("an") String an,
			@FormData("b") int b,
			@FormData("c") Integer c,
			@FormData("cn") Integer cn,
			@FormData("d") Boolean d,
			@FormData("e") float e,
			@FormData("f") Float f
		) {

		assertEquals("foo", a);
		assertNull(an);
		assertEquals(123, b);
		assertEquals(123, (int)c);
		assertNull(cn);
		assertTrue(d);
		assertEquals(1f, e, 0.1f);
		assertEquals(1f, f, 0.1f);
		return "OK";
	}

	@RestPost(path="/primitiveCollectionFormData")
	public String primitiveCollectionFormData(
			@FormData("a") int[][][] a,
			@FormData("b") Integer[][][] b,
			@FormData("c") String[][][] c,
			@FormData("d") List<Integer> d,
			@FormData("e") List<List<List<Integer>>> e,
			@FormData("f") List<Integer[][][]> f,
			@FormData("g") List<int[][][]> g,
			@FormData("h") List<String> h
		) {

		assertJson("[[[1,2],null],null]", a);
		assertJson("[[[1,null],null],null]", b);
		assertJson("[[['foo',null],null],null]", c);
		assertJson("[1,null]", d);
		assertJson("[[[1,null],null],null]", e);
		assertJson("[[[[1,null],null],null],null]", f);
		assertJson("[[[[1,2],null],null],null]", g);
		assertJson("['foo','bar',null]", h);

		assertInstanceOf(Integer.class, d.get(0));
		assertInstanceOf(Integer.class, e.get(0).get(0).get(0));
		assertInstanceOf(Integer[][][].class, f.get(0));
		assertInstanceOf(int[][][].class, g.get(0));

		return "OK";
	}

	@RestPost(path="/beanFormData")
	public String beanFormData(
			@FormData("a") @Schema(cf="uon") ABean a,
			@FormData("an") @Schema(cf="uon") ABean an,
			@FormData("b") @Schema(cf="uon") ABean[][][] b,
			@FormData("c") @Schema(cf="uon") List<ABean> c,
			@FormData("d") @Schema(cf="uon") List<ABean[][][]> d,
			@FormData("e") @Schema(cf="uon") Map<String,ABean> e,
			@FormData("f") @Schema(cf="uon") Map<String,List<ABean>> f,
			@FormData("g") @Schema(cf="uon") Map<String,List<ABean[][][]>> g,
			@FormData("h") @Schema(cf="uon") Map<Integer,List<ABean>> h
		) {

		assertJson("{a:1,b:'foo'}", a);
		assertNull(an);
		assertJson("[[[{a:1,b:'foo'},null],null],null]", b);
		assertJson("[{a:1,b:'foo'},null]", c);
		assertJson("[[[[{a:1,b:'foo'},null],null],null],null]", d);
		assertJson("{foo:{a:1,b:'foo'}}", e);
		assertJson("{foo:[{a:1,b:'foo'}]}", f);
		assertJson("{foo:[[[[{a:1,b:'foo'},null],null],null],null]}", g);
		assertJson("{'1':[{a:1,b:'foo'}]}", h);

		assertInstanceOf(ABean.class, c.get(0));
		assertInstanceOf(ABean[][][].class, d.get(0));
		assertInstanceOf(ABean.class, e.get("foo"));
		assertInstanceOf(ABean.class, f.get("foo").get(0));
		assertInstanceOf(ABean[][][].class, g.get("foo").get(0));
		assertInstanceOf(Integer.class, h.keySet().iterator().next());
		assertInstanceOf(ABean.class, h.values().iterator().next().get(0));
		return "OK";
	}

	@RestPost(path="/typedBeanFormData")
	public String typedBeanFormData(
			@FormData("a") @Schema(cf="uon") TypedBean a,
			@FormData("an") @Schema(cf="uon") TypedBean an,
			@FormData("b") @Schema(cf="uon") TypedBean[][][] b,
			@FormData("c") @Schema(cf="uon") List<TypedBean> c,
			@FormData("d") @Schema(cf="uon") List<TypedBean[][][]> d,
			@FormData("e") @Schema(cf="uon") Map<String,TypedBean> e,
			@FormData("f") @Schema(cf="uon") Map<String,List<TypedBean>> f,
			@FormData("g") @Schema(cf="uon") Map<String,List<TypedBean[][][]>> g,
			@FormData("h") @Schema(cf="uon") Map<Integer,List<TypedBean>> h
		) {

		assertJson("{a:1,b:'foo'}", a);
		assertNull(an);
		assertJson("[[[{a:1,b:'foo'},null],null],null]", b);
		assertJson("[{a:1,b:'foo'},null]", c);
		assertJson("[[[[{a:1,b:'foo'},null],null],null],null]", d);
		assertJson("{foo:{a:1,b:'foo'}}", e);
		assertJson("{foo:[{a:1,b:'foo'}]}", f);
		assertJson("{foo:[[[[{a:1,b:'foo'},null],null],null],null]}", g);
		assertJson("{'1':[{a:1,b:'foo'}]}", h);

		assertInstanceOf(TypedBeanImpl.class, a);
		assertInstanceOf(TypedBeanImpl.class, b[0][0][0]);
		assertInstanceOf(TypedBeanImpl.class, c.get(0));
		assertInstanceOf(TypedBeanImpl.class, d.get(0)[0][0][0]);
		assertInstanceOf(TypedBeanImpl.class, e.get("foo"));
		assertInstanceOf(TypedBeanImpl.class, f.get("foo").get(0));
		assertInstanceOf(TypedBeanImpl.class, g.get("foo").get(0)[0][0][0]);
		assertInstanceOf(Integer.class, h.keySet().iterator().next());
		assertInstanceOf(TypedBeanImpl.class, h.get(1).get(0));

		return "OK";
	}

	@RestPost(path="/swappedObjectFormData")
	public String swappedObjectFormData(
			@FormData("a") @Schema(cf="uon") SwappedObject a,
			@FormData("b") @Schema(cf="uon") SwappedObject[][][] b,
			@FormData("c") @Schema(cf="uon") Map<SwappedObject,SwappedObject> c,
			@FormData("d") @Schema(cf="uon") Map<SwappedObject,SwappedObject[][][]> d
		) {

		assertJson("'swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/'", a);
		assertJson("[[['swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/',null],null],null]", b);
		assertJson("{'swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/':'swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/'}", c);
		assertJson("{'swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/':[[['swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/',null],null],null]}", d);

		assertInstanceOf(SwappedObject.class, a);
		assertInstanceOf(SwappedObject.class, b[0][0][0]);
		assertInstanceOf(SwappedObject.class, c.keySet().iterator().next());
		assertInstanceOf(SwappedObject.class, c.values().iterator().next());
		assertInstanceOf(SwappedObject.class, d.keySet().iterator().next());
		assertInstanceOf(SwappedObject.class, d.values().iterator().next()[0][0][0]);

		return "OK";
	}

	@RestPost(path="/implicitSwappedObjectFormData")
	public String implicitSwappedObjectFormData(
			@FormData("a") @Schema(cf="uon") ImplicitSwappedObject a,
			@FormData("b") @Schema(cf="uon") ImplicitSwappedObject[][][] b,
			@FormData("c") @Schema(cf="uon") Map<ImplicitSwappedObject,ImplicitSwappedObject> c,
			@FormData("d") @Schema(cf="uon") Map<ImplicitSwappedObject,ImplicitSwappedObject[][][]> d
		) {

		assertJson("'swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/'", a);
		assertJson("[[['swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/',null],null],null]", b);
		assertJson("{'swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/':'swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/'}", c);
		assertJson("{'swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/':[[['swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/',null],null],null]}", d);

		assertInstanceOf(ImplicitSwappedObject.class, a);
		assertInstanceOf(ImplicitSwappedObject.class, b[0][0][0]);
		assertInstanceOf(ImplicitSwappedObject.class, c.keySet().iterator().next());
		assertInstanceOf(ImplicitSwappedObject.class, c.values().iterator().next());
		assertInstanceOf(ImplicitSwappedObject.class, d.keySet().iterator().next());
		assertInstanceOf(ImplicitSwappedObject.class, d.values().iterator().next()[0][0][0]);

		return "OK";
	}

	@RestPost(path="/enumFormData")
	public String enumFormData(
			@FormData("a") @Schema(cf="uon") TestEnum a,
			@FormData("an") @Schema(cf="uon") TestEnum an,
			@FormData("b") @Schema(cf="uon") TestEnum[][][] b,
			@FormData("c") @Schema(cf="uon") List<TestEnum> c,
			@FormData("d") @Schema(cf="uon") List<List<List<TestEnum>>> d,
			@FormData("e") @Schema(cf="uon") List<TestEnum[][][]> e,
			@FormData("f") @Schema(cf="uon") Map<TestEnum,TestEnum> f,
			@FormData("g") @Schema(cf="uon") Map<TestEnum,TestEnum[][][]> g,
			@FormData("h") @Schema(cf="uon") Map<TestEnum,List<TestEnum[][][]>> h
		) {

		assertEquals(TestEnum.TWO, a);
		assertNull(an);
		assertJson("[[['TWO',null],null],null]", b);
		assertJson("['TWO',null]", c);
		assertJson("[[['TWO',null],null],null]", d);
		assertJson("[[[['TWO',null],null],null],null]", e);
		assertJson("{ONE:'TWO'}", f);
		assertJson("{ONE:[[['TWO',null],null],null]}", g);
		assertJson("{ONE:[[[['TWO',null],null],null],null]}", h);

		assertInstanceOf(TestEnum.class, c.get(0));
		assertInstanceOf(TestEnum.class, d.get(0).get(0).get(0));
		assertInstanceOf(TestEnum[][][].class, e.get(0));
		assertInstanceOf(TestEnum.class, f.keySet().iterator().next());
		assertInstanceOf(TestEnum.class, f.values().iterator().next());
		assertInstanceOf(TestEnum.class, g.keySet().iterator().next());
		assertInstanceOf(TestEnum[][][].class, g.values().iterator().next());
		assertInstanceOf(TestEnum.class, h.keySet().iterator().next());
		assertInstanceOf(TestEnum[][][].class, h.values().iterator().next().get(0));

		return "OK";
	}

	@RestPost(path="/mapFormData")
	public String mapFormData(
		@FormData("a") String a,
		@FormData("b") @Schema(aev=true) String b,
		@FormData("c") String c
	) {

		assertEquals("foo", a);
		assertEquals("", b);
		assertEquals(null, c);

		return "OK";
	}

	@RestPost(path="/beanFormData2")
	public String beanFormData(
		@FormData("a") String a,
		@FormData("b") @Schema(aev=true) String b,
		@FormData("c") String c
	) {

		assertEquals("foo", a);
		assertEquals("", b);
		assertEquals(null, c);

		return "OK";
	}

	@RestPost(path="/partListFormData")
	public String partListFormData(
		@FormData("a") String a,
		@FormData("b") @Schema(aev=true) String b,
		@FormData("c") String c
	) {

		assertEquals("foo", a);
		assertEquals("", b);
		//assertEquals(null, c);  // This is impossible to represent.

		return "OK";
	}

	@RestPost(path="/formDataIfNE1")
	public String formDataIfNE1(
		@FormData("a") String a
	) {

		assertEquals("foo", a);

		return "OK";
	}

	@RestPost(path="/formDataIfNE2")
	public String formDataIfNE2(
		@FormData("a") String a
	) {

		assertEquals(null, a);

		return "OK";
	}

	@RestPost(path="/formDataIfNEMap")
	public String formDataIfNEMap(
		@FormData("a") String a,
		@FormData("b") String b,
		@FormData("c") String c
	) {

		assertEquals("foo", a);
		assertEquals(null, b);
		assertEquals(null, c);

		return "OK";
	}

	@RestPost(path="/formDataIfNEBean")
	public String formDataIfNEBean(
		@FormData("a") String a,
		@FormData("b") String b,
		@FormData("c") String c
	) {

		assertEquals("foo", a);
		assertEquals(null, b);
		assertEquals(null, c);

		return "OK";
	}

	@RestPost(path="/formDataIfNENameValuePairs")
	public String formDataIfNENameValuePairs(
		@FormData("a") String a,
		@FormData("b") String b,
		@FormData("c") String c
	) {

		assertEquals("foo", a);
		assertEquals(null, b);
		assertEquals(null, c);

		return "OK";
	}


	//-----------------------------------------------------------------------------------------------------------------
	// Path tests
	//-----------------------------------------------------------------------------------------------------------------

	@RestPost(path="/pathVars1/{a}/{b}")
	public String pathVars1(
		@Path("a") int a,
		@Path("b") String b
		) {

		assertEquals(1, a);
		assertEquals("foo", b);

		return "OK";
	}


	@RestPost(path="/pathVars2/{a}/{b}")
	public String pathVars2(
		@Path("a") int a,
		@Path("b") String b
		) {

		assertEquals(1, a);
		assertEquals("foo", b);

		return "OK";
	}

	@RestPost(path="/pathVars3/{a}/{b}")
	public String pathVars3(
		@Path("a") int a,
		@Path("b") String b
		) {

		assertEquals(1, a);
		assertEquals("foo", b);

		return "OK";
	}

	//-----------------------------------------------------------------------------------------------------------------
	// @Request tests
	//-----------------------------------------------------------------------------------------------------------------

	@RestPost(path="/reqBeanPath/{a}/{b}")
	public String reqBeanPath(
		@Path("a") int a,
		@Path("b") String b
		) {

		assertEquals(1, a);
		assertEquals("foo", b);

		return "OK";
	}

	@RestPost(path="/reqBeanQuery")
	public String reqBeanQuery(
		@Query("a") int a,
		@Query("b") String b
		) {

		assertEquals(1, a);
		assertEquals("foo", b);

		return "OK";
	}

	@RestPost(path="/reqBeanQueryIfNE")
	public String reqBeanQueryIfNE(
		@Query("a") String a,
		@Query("b") String b,
		@Query("c") String c
		) {

		assertEquals("foo", a);
		assertNull(b);
		assertNull(c);

		return "OK";
	}

	@RestPost(path="/reqBeanFormData")
	public String reqBeanFormData(
		@FormData("a") int a,
		@FormData("b") String b
		) {

		assertEquals(1, a);
		assertEquals("foo", b);

		return "OK";
	}

	@RestPost(path="/reqBeanFormDataIfNE")
	public String reqBeanFormDataIfNE(
		@FormData("a") String a,
		@FormData("b") String b,
		@FormData("c") String c
		) {

		assertEquals("foo", a);
		assertNull(b);
		assertNull(c);

		return "OK";
	}

	@RestPost(path="/reqBeanHeader")
	public String reqBeanHeader(
		@Header("a") int a,
		@Header("b") String b
		) {

		assertEquals(1, a);
		assertEquals("foo", b);

		return "OK";
	}

	@RestPost(path="/reqBeanHeaderIfNE")
	public String reqBeanHeaderIfNE(
		@Header("a") String a,
		@Header("b") String b,
		@Header("c") String c
		) {

		assertEquals("foo", a);
		assertNull(b);
		assertNull(c);

		return "OK";
	}
	//-----------------------------------------------------------------------------------------------------------------
	// Test return types.
	//-----------------------------------------------------------------------------------------------------------------

	// Various primitives

	@RestGet(path="/returnVoid")
	public void returnVoid() {}  // NOSONAR

	@RestGet(path="/returnInteger")
	public Integer returnInteger() {
		return 1;
	}

	@RestGet(path="/returnInt")
	public int returnInt() {
		return 1;
	}

	@RestGet(path="/returnBoolean")
	public boolean returnBoolean() {
		return true;
	}

	@RestGet(path="/returnFloat")
	public float returnFloat() {
		return 1f;
	}

	@RestGet(path="/returnFloatObject")
	public Float returnFloatObject() {
		return 1f;
	}

	@RestGet(path="/returnString")
	public String returnString() {
		return "foobar";
	}

	@RestGet(path="/returnNullString")
	public String returnNullString() {
		return null;
	}

	@RestGet(path="/returnInt3dArray")
	public int[][][] returnInt3dArray() {
		return new int[][][]{{{1,2},null},null};
	}

	@RestGet(path="/returnInteger3dArray")
	public Integer[][][] returnInteger3dArray() {
		return new Integer[][][]{{{1,null},null},null};
	}

	@RestGet(path="/returnString3dArray")
	public String[][][] returnString3dArray() {
		return new String[][][]{{{"foo","bar",null},null},null};
	}

	@RestGet(path="/returnIntegerList")
	public List<Integer> returnIntegerList() {
		return alist(new Integer[]{1,null});
	}

	@RestGet(path="/returnInteger3dList")
	public List<List<List<Integer>>> returnInteger3dList() {
		return list(list(list(1,null),null),null);
	}

	@RestGet(path="/returnInteger1d3dList")
	public List<Integer[][][]> returnInteger1d3dList() {
		return list(new Integer[][][]{{{1,null},null},null},null);
	}

	@RestGet(path="/returnInt1d3dList")
	public List<int[][][]> returnInt1d3dList() {
		return list(new int[][][]{{{1,2},null},null},null);
	}

	@RestGet(path="/returnStringList")
	public List<String> returnStringList() {
		return alist(new String[]{"foo","bar",null});
	}

	// Beans

	@RestGet(path="/returnBean")
	public ABean returnBean() {
		return ABean.get();
	}

	@RestGet(path="/returnBean3dArray")
	public ABean[][][] returnBean3dArray() {
		return new ABean[][][]{{{ABean.get(),null},null},null};
	}

	@RestGet(path="/returnBeanList")
	public List<ABean> returnBeanList() {
		return alist(ABean.get());
	}

	@RestGet(path="/returnBean1d3dList")
	public List<ABean[][][]> returnBean1d3dList() {
		return alist(new ABean[][][]{{{ABean.get(),null},null},null},null);
	}

	@RestGet(path="/returnBeanMap")
	public Map<String,ABean> returnBeanMap() {
		return map("foo",ABean.get());
	}

	@RestGet(path="/returnBeanListMap")
	public Map<String,List<ABean>> returnBeanListMap() {
		return map("foo",alist(ABean.get()));
	}

	@RestGet(path="/returnBean1d3dListMap")
	public Map<String,List<ABean[][][]>> returnBean1d3dListMap() {
		return map("foo", alist(new ABean[][][]{{{ABean.get(),null},null},null},null));
	}

	@RestGet(path="/returnBeanListMapIntegerKeys")
	public Map<Integer,List<ABean>> returnBeanListMapIntegerKeys() {
		return map(1,alist(ABean.get()));
	}

	// Typed beans

	@RestGet(path="/returnTypedBean")
	public TypedBean returnTypedBean() {
		return TypedBeanImpl.get();
	}

	@RestGet(path="/returnTypedBean3dArray")
	public TypedBean[][][] returnTypedBean3dArray() {
		return new TypedBean[][][]{{{TypedBeanImpl.get(),null},null},null};
	}

	@RestGet(path="/returnTypedBeanList")
	public List<TypedBean> returnTypedBeanList() {
		return alist((TypedBean)TypedBeanImpl.get());
	}

	@RestGet(path="/returnTypedBean1d3dList")
	public List<TypedBean[][][]> returnTypedBean1d3dList() {
		return list(new TypedBean[][][]{{{TypedBeanImpl.get(),null},null},null},null);
	}

	@RestGet(path="/returnTypedBeanMap")
	public Map<String,TypedBean> returnTypedBeanMap() {
		return map("foo",TypedBeanImpl.get());
	}

	@RestGet(path="/returnTypedBeanListMap")
	public Map<String,List<TypedBean>> returnTypedBeanListMap() {
		return map("foo",alist((TypedBean)TypedBeanImpl.get()));
	}

	@RestGet(path="/returnTypedBean1d3dListMap")
	public Map<String,List<TypedBean[][][]>> returnTypedBean1d3dListMap() {
		return map("foo", list(new TypedBean[][][]{{{TypedBeanImpl.get(),null},null},null},null));
	}

	@RestGet(path="/returnTypedBeanListMapIntegerKeys")
	public Map<Integer,List<TypedBean>> returnTypedBeanListMapIntegerKeys() {
		return map(1,alist((TypedBean)TypedBeanImpl.get()));
	}

	// Swapped POJOs

	@RestGet(path="/returnSwappedObject")
	public SwappedObject returnSwappedObject() {
		return new SwappedObject();
	}

	@RestGet(path="/returnSwappedObject3dArray")
	public SwappedObject[][][] returnSwappedObject3dArray() {
		return new SwappedObject[][][]{{{new SwappedObject(),null},null},null};
	}

	@RestGet(path="/returnSwappedObjectMap")
	public Map<SwappedObject,SwappedObject> returnSwappedObjectMap() {
		return map(new SwappedObject(),new SwappedObject());
	}

	@RestGet(path="/returnSwappedObject3dMap")
	public Map<SwappedObject,SwappedObject[][][]> returnSwappedObject3dMap() {
		return map(new SwappedObject(),new SwappedObject[][][]{{{new SwappedObject(),null},null},null});
	}

	// Implicit swapped POJOs

	@RestGet(path="/returnImplicitSwappedObject")
	public ImplicitSwappedObject returnImplicitSwappedObject() {
		return new ImplicitSwappedObject();
	}

	@RestGet(path="/returnImplicitSwappedObject3dArray")
	public ImplicitSwappedObject[][][] returnImplicitSwappedObject3dArray() {
		return new ImplicitSwappedObject[][][]{{{new ImplicitSwappedObject(),null},null},null};
	}

	@RestGet(path="/returnImplicitSwappedObjectMap")
	public Map<ImplicitSwappedObject,ImplicitSwappedObject> returnImplicitSwappedObjectMap() {
		return map(new ImplicitSwappedObject(),new ImplicitSwappedObject());
	}

	@RestGet(path="/returnImplicitSwappedObject3dMap")
	public Map<ImplicitSwappedObject,ImplicitSwappedObject[][][]> returnImplicitSwappedObject3dMap() {
		return map(new ImplicitSwappedObject(),new ImplicitSwappedObject[][][]{{{new ImplicitSwappedObject(),null},null},null});
	}

	// Enums

	@RestGet(path="/returnEnum")
	public TestEnum returnEnum() {
		return TestEnum.TWO;
	}

	@RestGet(path="/returnEnum3d")
	public TestEnum[][][] returnEnum3d() {
		return new TestEnum[][][]{{{TestEnum.TWO,null},null},null};
	}

	@RestGet(path="/returnEnumList")
	public List<TestEnum> returnEnumList() {
		return alist(TestEnum.TWO,null);
	}

	@RestGet(path="/returnEnum3dList")
	public List<List<List<TestEnum>>> returnEnum3dList() {
		return alist(alist(alist(TestEnum.TWO,null),null),null);
	}

	@RestGet(path="/returnEnum1d3dList")
	public List<TestEnum[][][]> returnEnum1d3dList() {
		return alist(new TestEnum[][][]{{{TestEnum.TWO,null},null},null},null);
	}

	@RestGet(path="/returnEnumMap")
	public Map<TestEnum,TestEnum> returnEnumMap() {
		return map(TestEnum.ONE,TestEnum.TWO);
	}

	@RestGet(path="/returnEnum3dArrayMap")
	public Map<TestEnum,TestEnum[][][]> returnEnum3dArrayMap() {
		return map(TestEnum.ONE,new TestEnum[][][]{{{TestEnum.TWO,null},null},null});
	}

	@RestGet(path="/returnEnum1d3dListMap")
	public Map<TestEnum,List<TestEnum[][][]>> returnEnum1d3dListMap() {
		return map(TestEnum.ONE,alist(new TestEnum[][][]{{{TestEnum.TWO,null},null},null},null));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Test parameters
	//-----------------------------------------------------------------------------------------------------------------

	// Various primitives

	@RestPost(path="/setInt")
	public void setInt(@Content int v) {
		assertEquals(1, v);
	}

	@RestPost(path="/setInteger")
	public void setInteger(@Content Integer v) {
		assertEquals((Integer)1, v);
	}

	@RestPost(path="/setBoolean")
	public void setBoolean(@Content boolean v) {
		assertTrue(v);
	}

	@RestPost(path="/setFloat")
	public void setFloat(@Content float v) {
		assertEquals(1f, v, 0.1f);
	}

	@RestPost(path="/setFloatObject")
	public void setFloatObject(@Content Float v) {
		assertEquals(1f, v, 0.1f);
	}

	@RestPost(path="/setString")
	public void setString(@Content String v) {
		assertEquals("foo", v);
	}

	@RestPost(path="/setNullString")
	public void setNullString(@Content String v) {
		assertNull(v);
	}

	@RestPost(path="/setInt3dArray")
	public String setInt3dArray(@Content int[][][] v) {
		return ""+v[0][0][0];
	}

	@RestPost(path="/setInteger3dArray")
	public void setInteger3dArray(@Content Integer[][][] v) {
		assertJson("[[[1,null],null],null]", v);
	}

	@RestPost(path="/setString3dArray")
	public void setString3dArray(@Content String[][][] v) {
		assertJson("[[['foo',null],null],null]", v);
	}

	@RestPost(path="/setIntegerList")
	public void setIntegerList(@Content List<Integer> v) {
		assertJson("[1,null]", v);
		assertInstanceOf(Integer.class, v.get(0));
	}

	@RestPost(path="/setInteger3dList")
	public void setInteger3dList(@Content List<List<List<Integer>>> v) {
		assertJson("[[[1,null],null],null]", v);
		assertInstanceOf(Integer.class, v.get(0).get(0).get(0));
	}

	@RestPost(path="/setInteger1d3dList")
	public void setInteger1d3dList(@Content List<Integer[][][]> v) {
		assertJson("[[[[1,null],null],null],null]", v);
		assertInstanceOf(Integer[][][].class, v.get(0));
		assertInstanceOf(Integer.class, v.get(0)[0][0][0]);
	}

	@RestPost(path="/setInt1d3dList")
	public void setInt1d3dList(@Content List<int[][][]> v) {
		assertJson("[[[[1,2],null],null],null]", v);
		assertInstanceOf(int[][][].class, v.get(0));
	}

	@RestPost(path="/setStringList")
	public void setStringList(@Content List<String> v) {
		assertJson("['foo','bar',null]", v);
	}

	// Beans

	@RestPost(path="/setBean")
	public void setBean(@Content ABean v) {
		assertJson("{a:1,b:'foo'}", v);
	}

	@RestPost(path="/setBean3dArray")
	public void setBean3dArray(@Content ABean[][][] v) {
		assertJson("[[[{a:1,b:'foo'},null],null],null]", v);
	}

	@RestPost(path="/setBeanList")
	public void setBeanList(@Content List<ABean> v) {
		assertJson("[{a:1,b:'foo'}]", v);
	}

	@RestPost(path="/setBean1d3dList")
	public void setBean1d3dList(@Content List<ABean[][][]> v) {
		assertJson("[[[[{a:1,b:'foo'},null],null],null],null]", v);
	}

	@RestPost(path="/setBeanMap")
	public void setBeanMap(@Content Map<String,ABean> v) {
		assertJson("{foo:{a:1,b:'foo'}}", v);
	}

	@RestPost(path="/setBeanListMap")
	public void setBeanListMap(@Content Map<String,List<ABean>> v) {
		assertJson("{foo:[{a:1,b:'foo'}]}", v);
	}

	@RestPost(path="/setBean1d3dListMap")
	public void setBean1d3dListMap(@Content Map<String,List<ABean[][][]>> v) {
		assertJson("{foo:[[[[{a:1,b:'foo'},null],null],null],null]}", v);
	}

	@RestPost(path="/setBeanListMapIntegerKeys")
	public void setBeanListMapIntegerKeys(@Content Map<Integer,List<ABean>> v) {
		assertJson("{'1':[{a:1,b:'foo'}]}", v);  // Note: JsonSerializer serializes key as string.
		assertInstanceOf(Integer.class, v.keySet().iterator().next());
	}

	// Typed beans

	@RestPost(path="/setTypedBean")
	public void setTypedBean(@Content TypedBean v) {
		assertJson("{a:1,b:'foo'}", v);
		assertInstanceOf(TypedBeanImpl.class, v);
	}

	@RestPost(path="/setTypedBean3dArray")
	public void setTypedBean3dArray(@Content TypedBean[][][] v) {
		assertJson("[[[{a:1,b:'foo'},null],null],null]", v);
		assertInstanceOf(TypedBeanImpl.class, v[0][0][0]);
	}

	@RestPost(path="/setTypedBeanList")
	public void setTypedBeanList(@Content List<TypedBean> v) {
		assertJson("[{a:1,b:'foo'}]", v);
		assertInstanceOf(TypedBeanImpl.class, v.get(0));
	}

	@RestPost(path="/setTypedBean1d3dList")
	public void setTypedBean1d3dList(@Content List<TypedBean[][][]> v) {
		assertJson("[[[[{a:1,b:'foo'},null],null],null],null]", v);
		assertInstanceOf(TypedBeanImpl.class, v.get(0)[0][0][0]);
	}

	@RestPost(path="/setTypedBeanMap")
	public void setTypedBeanMap(@Content Map<String,TypedBean> v) {
		assertJson("{foo:{a:1,b:'foo'}}", v);
		assertInstanceOf(TypedBeanImpl.class, v.get("foo"));
	}

	@RestPost(path="/setTypedBeanListMap")
	public void setTypedBeanListMap(@Content Map<String,List<TypedBean>> v) {
		assertJson("{foo:[{a:1,b:'foo'}]}", v);
		assertInstanceOf(TypedBeanImpl.class, v.get("foo").get(0));
	}

	@RestPost(path="/setTypedBean1d3dListMap")
	public void setTypedBean1d3dListMap(@Content Map<String,List<TypedBean[][][]>> v) {
		assertJson("{foo:[[[[{a:1,b:'foo'},null],null],null],null]}", v);
		assertInstanceOf(TypedBeanImpl.class, v.get("foo").get(0)[0][0][0]);
	}

	@RestPost(path="/setTypedBeanListMapIntegerKeys")
	public void setTypedBeanListMapIntegerKeys(@Content Map<Integer,List<TypedBean>> v) {
		assertJson("{'1':[{a:1,b:'foo'}]}", v);  // Note: JsonSerializer serializes key as string.
		assertInstanceOf(TypedBeanImpl.class, v.get(1).get(0));
	}

	// Swapped POJOs

	@RestPost(path="/setSwappedObject")
	public void setSwappedObject(@Content SwappedObject v) {
		assertTrue(v.wasUnswapped);
	}

	@RestPost(path="/setSwappedObject3dArray")
	public void setSwappedObject3dArray(@Content SwappedObject[][][] v) {
		assertJson("[[['"+SWAP+"',null],null],null]", v);
		assertTrue(v[0][0][0].wasUnswapped);
	}

	@RestPost(path="/setSwappedObjectMap")
	public void setSwappedObjectMap(@Content Map<SwappedObject,SwappedObject> v) {
		assertJson("{'"+SWAP+"':'"+SWAP+"'}", v);
		var e = v.entrySet().iterator().next();
		assertTrue(e.getKey().wasUnswapped);
		assertTrue(e.getValue().wasUnswapped);
	}

	@RestPost(path="/setSwappedObject3dMap")
	public void setSwappedObject3dMap(@Content Map<SwappedObject,SwappedObject[][][]> v) {
		assertJson("{'"+SWAP+"':[[['"+SWAP+"',null],null],null]}", v);
		var e = v.entrySet().iterator().next();
		assertTrue(e.getKey().wasUnswapped);
		assertTrue(e.getValue()[0][0][0].wasUnswapped);
	}

	// Implicit swapped POJOs

	@RestPost(path="/setImplicitSwappedObject")
	public void setImplicitSwappedObject(@Content ImplicitSwappedObject v) {
		assertTrue(v.wasUnswapped);
	}

	@RestPost(path="/setImplicitSwappedObject3dArray")
	public void setImplicitSwappedObject3dArray(@Content ImplicitSwappedObject[][][] v) {
		assertJson("[[['"+SWAP+"',null],null],null]", v);
		assertTrue(v[0][0][0].wasUnswapped);
	}

	@RestPost(path="/setImplicitSwappedObjectMap")
	public void setImplicitSwappedObjectMap(@Content Map<ImplicitSwappedObject,ImplicitSwappedObject> v) {
		assertJson("{'"+SWAP+"':'"+SWAP+"'}", v);
		var e = v.entrySet().iterator().next();
		assertTrue(e.getKey().wasUnswapped);
		assertTrue(e.getValue().wasUnswapped);
	}

	@RestPost(path="/setImplicitSwappedObject3dMap")
	public void setImplicitSwappedObject3dMap(@Content Map<ImplicitSwappedObject,ImplicitSwappedObject[][][]> v) {
		assertJson("{'"+SWAP+"':[[['"+SWAP+"',null],null],null]}", v);
		var e = v.entrySet().iterator().next();
		assertTrue(e.getKey().wasUnswapped);
		assertTrue(e.getValue()[0][0][0].wasUnswapped);
	}

	// Enums

	@RestPost(path="/setEnum")
	public void setEnum(@Content TestEnum v) {
		assertEquals(TestEnum.TWO, v);
	}

	@RestPost(path="/setEnum3d")
	public void setEnum3d(@Content TestEnum[][][] v) {
		assertJson("[[['TWO',null],null],null]", v);
	}

	@RestPost(path="/setEnumList")
	public void setEnumList(@Content List<TestEnum> v) {
		assertJson("['TWO',null]", v);
		assertInstanceOf(TestEnum.class, v.get(0));
	}

	@RestPost(path="/setEnum3dList")
	public void setEnum3dList(@Content List<List<List<TestEnum>>> v) {
		assertJson("[[['TWO',null],null],null]", v);
		assertInstanceOf(TestEnum.class, v.get(0).get(0).get(0));
	}

	@RestPost(path="/setEnum1d3dList")
	public void setEnum1d3dList(@Content List<TestEnum[][][]> v) {
		assertJson("[[[['TWO',null],null],null],null]", v);
		assertInstanceOf(TestEnum[][][].class, v.get(0));
	}

	@RestPost(path="/setEnumMap")
	public void setEnumMap(@Content Map<TestEnum,TestEnum> v) {
		assertJson("{ONE:'TWO'}", v);
		var e = v.entrySet().iterator().next();
		assertInstanceOf(TestEnum.class, e.getKey());
		assertInstanceOf(TestEnum.class, e.getValue());
	}

	@RestPost(path="/setEnum3dArrayMap")
	public void setEnum3dArrayMap(@Content Map<TestEnum,TestEnum[][][]> v) {
		assertJson("{ONE:[[['TWO',null],null],null]}", v);
		var e = v.entrySet().iterator().next();
		assertInstanceOf(TestEnum.class, e.getKey());
		assertInstanceOf(TestEnum[][][].class, e.getValue());
	}

	@RestPost(path="/setEnum1d3dListMap")
	public void setEnum1d3dListMap(@Content Map<TestEnum,List<TestEnum[][][]>> v) {
		assertJson("{ONE:[[[['TWO',null],null],null],null]}", v);
		var e = v.entrySet().iterator().next();
		assertInstanceOf(TestEnum.class, e.getKey());
		assertInstanceOf(TestEnum[][][].class, e.getValue().get(0));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// PartFormatter tests
	//-----------------------------------------------------------------------------------------------------------------

	@RestPost(path="/partFormatters/{p1}")
	public String partFormatter(
		@Path("p1") String p1,
		@Header("h1") String h1,
		@Query("q1") String q1,
		@FormData("f1") String f1
	) {

		assertEquals("dummy-1", p1);
		assertEquals("dummy-2", h1);
		assertEquals("dummy-3", q1);
		assertEquals("dummy-4", f1);

		return "OK";
	}

	//-----------------------------------------------------------------------------------------------------------------
	// @RemoteOp(returns=HTTP_STATUS)
	//-----------------------------------------------------------------------------------------------------------------

	@RestGet(path="/httpStatusReturn200")
	public void httpStatusReturn200(RestResponse res) {
		res.setStatus(200);
	}

	@RestGet(path="/httpStatusReturn404")
	public void httpStatusReturn404(RestResponse res) {
		res.setStatus(404);
	}
}
