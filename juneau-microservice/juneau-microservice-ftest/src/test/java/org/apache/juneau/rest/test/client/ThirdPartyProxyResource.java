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

import static java.util.Arrays.*;
import static org.junit.Assert.*;
import static org.apache.juneau.http.HttpMethodName.*;
import static org.apache.juneau.rest.testutils.Constants.*;
import static org.apache.juneau.rest.testutils.TestUtils.*;

import java.io.*;
import java.util.*;

import javax.servlet.http.*;

import org.apache.juneau.http.annotation.Body;
import org.apache.juneau.http.annotation.FormData;
import org.apache.juneau.http.annotation.Header;
import org.apache.juneau.http.annotation.Path;
import org.apache.juneau.http.annotation.Query;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.testutils.*;
import org.apache.juneau.serializer.annotation.*;
import org.apache.juneau.utils.*;

/**
 * JUnit automated testcase resource.
 */
@Rest(
	path="/testThirdPartyProxy",
	logging=@Logging(
		disabled="true"
	)
)
@SerializerConfig(addRootType="true",addBeanTypes="true")
@SuppressWarnings({"serial"})
public class ThirdPartyProxyResource extends BasicRestServletJena {

	public static FileWriter logFile;
	static {
		try {
			logFile = new FileWriter("./target/logs/third-party-proxy-resource.txt", false);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@RestHook(HookEvent.START_CALL)
	public static void startCall(HttpServletRequest req) {
		try {
			logFile.append("START["+new Date()+"]-").append(req.getQueryString()).append("\n");
			logFile.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@RestHook(HookEvent.PRE_CALL)
	public static void preCall(HttpServletRequest req) {
		try {
			logFile.append("PRE["+new Date()+"]-").append(req.getQueryString()).append("\n");
			logFile.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@RestHook(HookEvent.POST_CALL)
	public static void postCall(HttpServletRequest req) {
		try {
			logFile.append("POST["+new Date()+"]-").append(req.getQueryString()).append("\n");
			logFile.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@RestHook(HookEvent.END_CALL)
	public static void endCall(HttpServletRequest req) {
		try {
			Throwable e = (Throwable)req.getAttribute("Exception");
			Long execTime = (Long)req.getAttribute("ExecTime");
			logFile.append("END["+new Date()+"]-").append(req.getQueryString()).append(", time=").append(""+execTime).append(", exception=").append(e == null ? null : e.toString()).append("\n");
			logFile.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Header tests
	//-----------------------------------------------------------------------------------------------------------------

	@RestMethod(name=GET, path="/primitiveHeaders")
	public String primitiveHeaders(
			@Header("a") String a,
			@Header("an") String an,
			@Header("b") int b,
			@Header("c") Integer c,
			@Header("cn") Integer cn,
			@Header("d") Boolean d,
			@Header("e") float e,
			@Header("f") Float f
		) throws Exception {

		assertEquals(a, "foo");
		assertNull(an);
		assertEquals(123, b);
		assertEquals(123, (int)c);
		assertNull(cn);
		assertTrue(d);
		assertTrue(1f == e);
		assertTrue(1f == f);
		return "OK";
	}

	@RestMethod(name=GET, path="/primitiveCollectionHeaders")
	public String primitiveCollectionHeaders(
			@Header("a") int[][][] a,
			@Header("b") Integer[][][] b,
			@Header("c") String[][][] c,
			@Header("d") List<Integer> d,
			@Header("e") List<List<List<Integer>>> e,
			@Header("f") List<Integer[][][]> f,
			@Header("g") List<int[][][]> g,
			@Header("h") List<String> h
		) throws Exception {

		assertObjectEquals("[[[1,2],null],null]", a);
		assertObjectEquals("[[[1,null],null],null]", b);
		assertObjectEquals("[[['foo',null],null],null]", c);
		assertObjectEquals("[1,null]", d);
		assertObjectEquals("[[[1,null],null],null]", e);
		assertObjectEquals("[[[[1,null],null],null],null]", f);
		assertObjectEquals("[[[[1,2],null],null],null]", g);
		assertObjectEquals("['foo','bar',null]", h);

		assertClass(Integer.class, d.get(0));
		assertClass(Integer.class, e.get(0).get(0).get(0));
		assertClass(Integer[][][].class, f.get(0));
		assertClass(int[][][].class, g.get(0));

		return "OK";
	}

	@RestMethod(name=GET, path="/beanHeaders")
	public String beanHeaders(
			@Header("a") ABean a,
			@Header("an") ABean an,
			@Header("b") ABean[][][] b,
			@Header("c") List<ABean> c,
			@Header("d") List<ABean[][][]> d,
			@Header("e") Map<String,ABean> e,
			@Header("f") Map<String,List<ABean>> f,
			@Header("g") Map<String,List<ABean[][][]>> g,
			@Header("h") Map<Integer,List<ABean>> h
		) throws Exception {

		assertObjectEquals("{a:1,b:'foo'}", a);
		assertNull(an);
		assertObjectEquals("[[[{a:1,b:'foo'},null],null],null]", b);
		assertObjectEquals("[{a:1,b:'foo'},null]", c);
		assertObjectEquals("[[[[{a:1,b:'foo'},null],null],null],null]", d);
		assertObjectEquals("{foo:{a:1,b:'foo'}}", e);
		assertObjectEquals("{foo:[{a:1,b:'foo'}]}", f);
		assertObjectEquals("{foo:[[[[{a:1,b:'foo'},null],null],null],null]}", g);
		assertObjectEquals("{'1':[{a:1,b:'foo'}]}", h);

		assertClass(ABean.class, c.get(0));
		assertClass(ABean[][][].class, d.get(0));
		assertClass(ABean.class, e.get("foo"));
		assertClass(ABean.class, f.get("foo").get(0));
		assertClass(ABean[][][].class, g.get("foo").get(0));
		assertClass(Integer.class, h.keySet().iterator().next());
		assertClass(ABean.class, h.values().iterator().next().get(0));
		return "OK";
	}

	@RestMethod(name=GET, path="/typedBeanHeaders")
	public String typedBeanHeaders(
			@Header("a") TypedBean a,
			@Header("an") TypedBean an,
			@Header("b") TypedBean[][][] b,
			@Header("c") List<TypedBean> c,
			@Header("d") List<TypedBean[][][]> d,
			@Header("e") Map<String,TypedBean> e,
			@Header("f") Map<String,List<TypedBean>> f,
			@Header("g") Map<String,List<TypedBean[][][]>> g,
			@Header("h") Map<Integer,List<TypedBean>> h
		) throws Exception {

		assertObjectEquals("{_type:'TypedBeanImpl',a:1,b:'foo'}", a);
		assertNull(an);
		assertObjectEquals("[[[{_type:'TypedBeanImpl',a:1,b:'foo'},null],null],null]", b);
		assertObjectEquals("[{_type:'TypedBeanImpl',a:1,b:'foo'},null]", c);
		assertObjectEquals("[[[[{_type:'TypedBeanImpl',a:1,b:'foo'},null],null],null],null]", d);
		assertObjectEquals("{foo:{_type:'TypedBeanImpl',a:1,b:'foo'}}", e);
		assertObjectEquals("{foo:[{_type:'TypedBeanImpl',a:1,b:'foo'}]}", f);
		assertObjectEquals("{foo:[[[[{_type:'TypedBeanImpl',a:1,b:'foo'},null],null],null],null]}", g);
		assertObjectEquals("{'1':[{_type:'TypedBeanImpl',a:1,b:'foo'}]}", h);

		assertClass(TypedBeanImpl.class, a);
		assertClass(TypedBeanImpl.class, b[0][0][0]);
		assertClass(TypedBeanImpl.class, c.get(0));
		assertClass(TypedBeanImpl.class, d.get(0)[0][0][0]);
		assertClass(TypedBeanImpl.class, e.get("foo"));
		assertClass(TypedBeanImpl.class, f.get("foo").get(0));
		assertClass(TypedBeanImpl.class, g.get("foo").get(0)[0][0][0]);
		assertClass(Integer.class, h.keySet().iterator().next());
		assertClass(TypedBeanImpl.class, h.get(1).get(0));

		return "OK";
	}

	@RestMethod(name=GET, path="/swappedPojoHeaders")
	public String swappedPojoHeaders(
			@Header("a") SwappedPojo a,
			@Header("b") SwappedPojo[][][] b,
			@Header("c") Map<SwappedPojo,SwappedPojo> c,
			@Header("d") Map<SwappedPojo,SwappedPojo[][][]> d
		) throws Exception {

		assertObjectEquals("'swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/'", a);
		assertObjectEquals("[[['swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/',null],null],null]", b);
		assertObjectEquals("{'swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/':'swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/'}", c);
		assertObjectEquals("{'swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/':[[['swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/',null],null],null]}", d);

		assertClass(SwappedPojo.class, a);
		assertClass(SwappedPojo.class, b[0][0][0]);
		assertClass(SwappedPojo.class, c.keySet().iterator().next());
		assertClass(SwappedPojo.class, c.values().iterator().next());
		assertClass(SwappedPojo.class, d.keySet().iterator().next());
		assertClass(SwappedPojo.class, d.values().iterator().next()[0][0][0]);

		return "OK";
	}

	@RestMethod(name=GET, path="/implicitSwappedPojoHeaders")
	public String implicitSwappedPojoHeaders(
			@Header("a") ImplicitSwappedPojo a,
			@Header("b") ImplicitSwappedPojo[][][] b,
			@Header("c") Map<ImplicitSwappedPojo,ImplicitSwappedPojo> c,
			@Header("d") Map<ImplicitSwappedPojo,ImplicitSwappedPojo[][][]> d
		) throws Exception {

		assertObjectEquals("'swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/'", a);
		assertObjectEquals("[[['swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/',null],null],null]", b);
		assertObjectEquals("{'swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/':'swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/'}", c);
		assertObjectEquals("{'swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/':[[['swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/',null],null],null]}", d);

		assertClass(ImplicitSwappedPojo.class, a);
		assertClass(ImplicitSwappedPojo.class, b[0][0][0]);
		assertClass(ImplicitSwappedPojo.class, c.keySet().iterator().next());
		assertClass(ImplicitSwappedPojo.class, c.values().iterator().next());
		assertClass(ImplicitSwappedPojo.class, d.keySet().iterator().next());
		assertClass(ImplicitSwappedPojo.class, d.values().iterator().next()[0][0][0]);

		return "OK";
	}

	@RestMethod(name=GET, path="/enumHeaders")
	public String enumHeaders(
			@Header("a") TestEnum a,
			@Header("an") TestEnum an,
			@Header("b") TestEnum[][][] b,
			@Header("c") List<TestEnum> c,
			@Header("d") List<List<List<TestEnum>>> d,
			@Header("e") List<TestEnum[][][]> e,
			@Header("f") Map<TestEnum,TestEnum> f,
			@Header("g") Map<TestEnum,TestEnum[][][]> g,
			@Header("h") Map<TestEnum,List<TestEnum[][][]>> h
		) throws Exception {

		assertEquals(TestEnum.TWO, a);
		assertNull(an);
		assertObjectEquals("[[['TWO',null],null],null]", b);
		assertObjectEquals("['TWO',null]", c);
		assertObjectEquals("[[['TWO',null],null],null]", d);
		assertObjectEquals("[[[['TWO',null],null],null],null]", e);
		assertObjectEquals("{ONE:'TWO'}", f);
		assertObjectEquals("{ONE:[[['TWO',null],null],null]}", g);
		assertObjectEquals("{ONE:[[[['TWO',null],null],null],null]}", h);

		assertClass(TestEnum.class, c.get(0));
		assertClass(TestEnum.class, d.get(0).get(0).get(0));
		assertClass(TestEnum[][][].class, e.get(0));
		assertClass(TestEnum.class, f.keySet().iterator().next());
		assertClass(TestEnum.class, f.values().iterator().next());
		assertClass(TestEnum.class, g.keySet().iterator().next());
		assertClass(TestEnum[][][].class, g.values().iterator().next());
		assertClass(TestEnum.class, h.keySet().iterator().next());
		assertClass(TestEnum[][][].class, h.values().iterator().next().get(0));

		return "OK";
	}

	@RestMethod(name=GET, path="/mapHeader")
	public String mapHeader(
		@Header("a") String a,
		@Header(name="b",allowEmptyValue=true) String b,
		@Header("c") String c
	) throws Exception {

		assertEquals("foo", a);
		assertEquals("", b);
		assertEquals(null, c);

		return "OK";
	}

	@RestMethod(name=GET, path="/beanHeader")
	public String beanHeader(
		@Header("a") String a,
		@Header(name="b",allowEmptyValue=true) String b,
		@Header("c") String c
	) throws Exception {

		assertEquals("foo", a);
		assertEquals("", b);
		assertEquals(null, c);

		return "OK";
	}

	@RestMethod(name=GET, path="/nameValuePairsHeader")
	public String nameValuePairsHeader(
		@Header("a") String a,
		@Header(name="b",allowEmptyValue=true) String b,
		@Header("c") String c
	) throws Exception {

		assertEquals("foo", a);
		assertEquals("", b);
		assertEquals(null, c);

		return "OK";
	}

	@RestMethod(name=GET, path="/headerIfNE1")
	public String headerIfNE1(
		@Header("a") String a
	) throws Exception {

		assertEquals("foo", a);

		return "OK";
	}

	@RestMethod(name=GET, path="/headerIfNE2")
	public String headerIfNE2(
		@Header("a") String a
	) throws Exception {

		assertEquals(null, a);

		return "OK";
	}

	@RestMethod(name=GET, path="/headerIfNEMap")
	public String headerIfNEMap(
		@Header("a") String a,
		@Header("b") String b,
		@Header("c") String c
	) throws Exception {

		assertEquals("foo", a);
		assertEquals(null, b);
		assertEquals(null, c);

		return "OK";
	}

	@RestMethod(name=GET, path="/headerIfNEBean")
	public String headerIfNEBean(
		@Header("a") String a,
		@Header("b") String b,
		@Header("c") String c
	) throws Exception {

		assertEquals("foo", a);
		assertEquals(null, b);
		assertEquals(null, c);

		return "OK";
	}

	@RestMethod(name=GET, path="/headerIfNEnameValuePairs")
	public String headerIfNEnameValuePairs(
		@Header("a") String a,
		@Header("b") String b,
		@Header("c") String c
	) throws Exception {

		assertEquals("foo", a);
		assertEquals(null, b);
		assertEquals(null, c);

		return "OK";
	}


	//-----------------------------------------------------------------------------------------------------------------
	// Query tests
	//-----------------------------------------------------------------------------------------------------------------

	@RestMethod(name=GET, path="/primitiveQueries")
	public String primitiveQueries(
			@Query("a") String a,
			@Query("an") String an,
			@Query("b") int b,
			@Query("c") Integer c,
			@Query("cn") Integer cn,
			@Query("d") Boolean d,
			@Query("e") float e,
			@Query("f") Float f
		) throws Exception {

		assertEquals(a, "foo");
		assertNull(an);
		assertEquals(123, b);
		assertEquals(123, (int)c);
		assertNull(cn);
		assertTrue(d);
		assertTrue(1f == e);
		assertTrue(1f == f);
		return "OK";
	}

	@RestMethod(name=GET, path="/primitiveCollectionQueries")
	public String primitiveCollectionQueries(
			@Query("a") int[][][] a,
			@Query("b") Integer[][][] b,
			@Query("c") String[][][] c,
			@Query("d") List<Integer> d,
			@Query("e") List<List<List<Integer>>> e,
			@Query("f") List<Integer[][][]> f,
			@Query("g") List<int[][][]> g,
			@Query("h") List<String> h
		) throws Exception {

		assertObjectEquals("[[[1,2],null],null]", a);
		assertObjectEquals("[[[1,null],null],null]", b);
		assertObjectEquals("[[['foo',null],null],null]", c);
		assertObjectEquals("[1,null]", d);
		assertObjectEquals("[[[1,null],null],null]", e);
		assertObjectEquals("[[[[1,null],null],null],null]", f);
		assertObjectEquals("[[[[1,2],null],null],null]", g);
		assertObjectEquals("['foo','bar',null]", h);

		assertClass(Integer.class, d.get(0));
		assertClass(Integer.class, e.get(0).get(0).get(0));
		assertClass(Integer[][][].class, f.get(0));
		assertClass(int[][][].class, g.get(0));

		return "OK";
	}

	@RestMethod(name=GET, path="/beanQueries")
	public String beanQueries(
			@Query("a") ABean a,
			@Query("an") ABean an,
			@Query("b") ABean[][][] b,
			@Query("c") List<ABean> c,
			@Query("d") List<ABean[][][]> d,
			@Query("e") Map<String,ABean> e,
			@Query("f") Map<String,List<ABean>> f,
			@Query("g") Map<String,List<ABean[][][]>> g,
			@Query("h") Map<Integer,List<ABean>> h
		) throws Exception {

		assertObjectEquals("{a:1,b:'foo'}", a);
		assertNull(an);
		assertObjectEquals("[[[{a:1,b:'foo'},null],null],null]", b);
		assertObjectEquals("[{a:1,b:'foo'},null]", c);
		assertObjectEquals("[[[[{a:1,b:'foo'},null],null],null],null]", d);
		assertObjectEquals("{foo:{a:1,b:'foo'}}", e);
		assertObjectEquals("{foo:[{a:1,b:'foo'}]}", f);
		assertObjectEquals("{foo:[[[[{a:1,b:'foo'},null],null],null],null]}", g);
		assertObjectEquals("{'1':[{a:1,b:'foo'}]}", h);

		assertClass(ABean.class, c.get(0));
		assertClass(ABean[][][].class, d.get(0));
		assertClass(ABean.class, e.get("foo"));
		assertClass(ABean.class, f.get("foo").get(0));
		assertClass(ABean[][][].class, g.get("foo").get(0));
		assertClass(Integer.class, h.keySet().iterator().next());
		assertClass(ABean.class, h.values().iterator().next().get(0));
		return "OK";
	}

	@RestMethod(name=GET, path="/typedBeanQueries")
	public String typedBeanQueries(
			@Query("a") TypedBean a,
			@Query("an") TypedBean an,
			@Query("b") TypedBean[][][] b,
			@Query("c") List<TypedBean> c,
			@Query("d") List<TypedBean[][][]> d,
			@Query("e") Map<String,TypedBean> e,
			@Query("f") Map<String,List<TypedBean>> f,
			@Query("g") Map<String,List<TypedBean[][][]>> g,
			@Query("h") Map<Integer,List<TypedBean>> h
		) throws Exception {

		assertObjectEquals("{_type:'TypedBeanImpl',a:1,b:'foo'}", a);
		assertNull(an);
		assertObjectEquals("[[[{_type:'TypedBeanImpl',a:1,b:'foo'},null],null],null]", b);
		assertObjectEquals("[{_type:'TypedBeanImpl',a:1,b:'foo'},null]", c);
		assertObjectEquals("[[[[{_type:'TypedBeanImpl',a:1,b:'foo'},null],null],null],null]", d);
		assertObjectEquals("{foo:{_type:'TypedBeanImpl',a:1,b:'foo'}}", e);
		assertObjectEquals("{foo:[{_type:'TypedBeanImpl',a:1,b:'foo'}]}", f);
		assertObjectEquals("{foo:[[[[{_type:'TypedBeanImpl',a:1,b:'foo'},null],null],null],null]}", g);
		assertObjectEquals("{'1':[{_type:'TypedBeanImpl',a:1,b:'foo'}]}", h);

		assertClass(TypedBeanImpl.class, a);
		assertClass(TypedBeanImpl.class, b[0][0][0]);
		assertClass(TypedBeanImpl.class, c.get(0));
		assertClass(TypedBeanImpl.class, d.get(0)[0][0][0]);
		assertClass(TypedBeanImpl.class, e.get("foo"));
		assertClass(TypedBeanImpl.class, f.get("foo").get(0));
		assertClass(TypedBeanImpl.class, g.get("foo").get(0)[0][0][0]);
		assertClass(Integer.class, h.keySet().iterator().next());
		assertClass(TypedBeanImpl.class, h.get(1).get(0));

		return "OK";
	}

	@RestMethod(name=GET, path="/swappedPojoQueries")
	public String swappedPojoQueries(
			@Query("a") SwappedPojo a,
			@Query("b") SwappedPojo[][][] b,
			@Query("c") Map<SwappedPojo,SwappedPojo> c,
			@Query("d") Map<SwappedPojo,SwappedPojo[][][]> d
		) throws Exception {

		assertObjectEquals("'swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/'", a);
		assertObjectEquals("[[['swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/',null],null],null]", b);
		assertObjectEquals("{'swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/':'swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/'}", c);
		assertObjectEquals("{'swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/':[[['swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/',null],null],null]}", d);

		assertClass(SwappedPojo.class, a);
		assertClass(SwappedPojo.class, b[0][0][0]);
		assertClass(SwappedPojo.class, c.keySet().iterator().next());
		assertClass(SwappedPojo.class, c.values().iterator().next());
		assertClass(SwappedPojo.class, d.keySet().iterator().next());
		assertClass(SwappedPojo.class, d.values().iterator().next()[0][0][0]);

		return "OK";
	}

	@RestMethod(name=GET, path="/implicitSwappedPojoQueries")
	public String implicitSwappedPojoQueries(
			@Query("a") ImplicitSwappedPojo a,
			@Query("b") ImplicitSwappedPojo[][][] b,
			@Query("c") Map<ImplicitSwappedPojo,ImplicitSwappedPojo> c,
			@Query("d") Map<ImplicitSwappedPojo,ImplicitSwappedPojo[][][]> d
		) throws Exception {

		assertObjectEquals("'swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/'", a);
		assertObjectEquals("[[['swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/',null],null],null]", b);
		assertObjectEquals("{'swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/':'swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/'}", c);
		assertObjectEquals("{'swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/':[[['swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/',null],null],null]}", d);

		assertClass(ImplicitSwappedPojo.class, a);
		assertClass(ImplicitSwappedPojo.class, b[0][0][0]);
		assertClass(ImplicitSwappedPojo.class, c.keySet().iterator().next());
		assertClass(ImplicitSwappedPojo.class, c.values().iterator().next());
		assertClass(ImplicitSwappedPojo.class, d.keySet().iterator().next());
		assertClass(ImplicitSwappedPojo.class, d.values().iterator().next()[0][0][0]);

		return "OK";
	}

	@RestMethod(name=GET, path="/enumQueries")
	public String enumQueries(
			@Query("a") TestEnum a,
			@Query("an") TestEnum an,
			@Query("b") TestEnum[][][] b,
			@Query("c") List<TestEnum> c,
			@Query("d") List<List<List<TestEnum>>> d,
			@Query("e") List<TestEnum[][][]> e,
			@Query("f") Map<TestEnum,TestEnum> f,
			@Query("g") Map<TestEnum,TestEnum[][][]> g,
			@Query("h") Map<TestEnum,List<TestEnum[][][]>> h
		) throws Exception {

		assertEquals(TestEnum.TWO, a);
		assertNull(an);
		assertObjectEquals("[[['TWO',null],null],null]", b);
		assertObjectEquals("['TWO',null]", c);
		assertObjectEquals("[[['TWO',null],null],null]", d);
		assertObjectEquals("[[[['TWO',null],null],null],null]", e);
		assertObjectEquals("{ONE:'TWO'}", f);
		assertObjectEquals("{ONE:[[['TWO',null],null],null]}", g);
		assertObjectEquals("{ONE:[[[['TWO',null],null],null],null]}", h);

		assertClass(TestEnum.class, c.get(0));
		assertClass(TestEnum.class, d.get(0).get(0).get(0));
		assertClass(TestEnum[][][].class, e.get(0));
		assertClass(TestEnum.class, f.keySet().iterator().next());
		assertClass(TestEnum.class, f.values().iterator().next());
		assertClass(TestEnum.class, g.keySet().iterator().next());
		assertClass(TestEnum[][][].class, g.values().iterator().next());
		assertClass(TestEnum.class, h.keySet().iterator().next());
		assertClass(TestEnum[][][].class, h.values().iterator().next().get(0));

		return "OK";
	}

	@RestMethod(name=GET, path="/stringQuery1")
	public String stringQuery1(
			@Query("a") int a,
			@Query("b") String b
		) throws Exception {

		assertEquals(1, a);
		assertEquals("foo", b);

		return "OK";
	}

	@RestMethod(name=GET, path="/stringQuery2")
	public String stringQuery2(
			@Query("a") int a,
			@Query("b") String b
		) throws Exception {

		assertEquals(1, a);
		assertEquals("foo", b);

		return "OK";
	}

	@RestMethod(name=GET, path="/mapQuery")
	public String mapQuery(
			@Query("a") int a,
			@Query("b") String b
		) throws Exception {

		assertEquals(1, a);
		assertEquals("foo", b);

		return "OK";
	}

	@RestMethod(name=GET, path="/beanQuery")
	public String beanQuery(
			@Query("a") String a,
			@Query(name="b",allowEmptyValue=true) String b,
			@Query("c") String c
		) throws Exception {

		assertEquals("foo", a);
		assertEquals("", b);
		assertEquals(null, c);

		return "OK";
	}

	@RestMethod(name=GET, path="/nameValuePairsQuery")
	public String nameValuePairsQuery(
		@Query("a") String a,
		@Query(name="b",allowEmptyValue=true) String b,
		@Query("c") String c
	) throws Exception {

		assertEquals("foo", a);
		assertEquals("", b);
		assertEquals(null, c);

		return "OK";
	}

	@RestMethod(name=GET, path="/queryIfNE1")
	public String queryIfNE1(
		@Query("a") String a
	) throws Exception {

		assertEquals("foo", a);

		return "OK";
	}

	@RestMethod(name=GET, path="/queryIfNE2")
	public String queryIfNE2(
		@Query("q") String a
	) throws Exception {

		assertEquals(null, a);

		return "OK";
	}

	@RestMethod(name=GET, path="/queryIfNEMap")
	public String queryIfNEMap(
		@Query("a") String a,
		@Query("b") String b,
		@Query("c") String c
	) throws Exception {

		assertEquals("foo", a);
		assertEquals(null, b);
		assertEquals(null, c);

		return "OK";
	}

	@RestMethod(name=GET, path="/queryIfNEBean")
	public String queryIfNEBean(
		@Query("a") String a,
		@Query("b") String b,
		@Query("c") String c
	) throws Exception {

		assertEquals("foo", a);
		assertEquals(null, b);
		assertEquals(null, c);

		return "OK";
	}

	@RestMethod(name=GET, path="/queryIfNEnameValuePairs")
	public String queryIfNEnameValuePairs(
		@Query("a") String a,
		@Query("b") String b,
		@Query("c") String c
	) throws Exception {

		assertEquals("foo", a);
		assertEquals(null, b);
		assertEquals(null, c);

		return "OK";
	}


	//-----------------------------------------------------------------------------------------------------------------
	// FormData tests
	//-----------------------------------------------------------------------------------------------------------------

	@RestMethod(name=POST, path="/primitiveFormData")
	public String primitiveFormData(
			@FormData("a") String a,
			@FormData("an") String an,
			@FormData("b") int b,
			@FormData("c") Integer c,
			@FormData("cn") Integer cn,
			@FormData("d") Boolean d,
			@FormData("e") float e,
			@FormData("f") Float f
		) throws Exception {

		assertEquals("foo", a);
		assertNull(an);
		assertEquals(123, b);
		assertEquals(123, (int)c);
		assertNull(cn);
		assertTrue(d);
		assertTrue(1f == e);
		assertTrue(1f == f);
		return "OK";
	}

	@RestMethod(name=POST, path="/primitiveCollectionFormData")
	public String primitiveCollectionFormData(
			@FormData("a") int[][][] a,
			@FormData("b") Integer[][][] b,
			@FormData("c") String[][][] c,
			@FormData("d") List<Integer> d,
			@FormData("e") List<List<List<Integer>>> e,
			@FormData("f") List<Integer[][][]> f,
			@FormData("g") List<int[][][]> g,
			@FormData("h") List<String> h
		) throws Exception {

		assertObjectEquals("[[[1,2],null],null]", a);
		assertObjectEquals("[[[1,null],null],null]", b);
		assertObjectEquals("[[['foo',null],null],null]", c);
		assertObjectEquals("[1,null]", d);
		assertObjectEquals("[[[1,null],null],null]", e);
		assertObjectEquals("[[[[1,null],null],null],null]", f);
		assertObjectEquals("[[[[1,2],null],null],null]", g);
		assertObjectEquals("['foo','bar',null]", h);

		assertClass(Integer.class, d.get(0));
		assertClass(Integer.class, e.get(0).get(0).get(0));
		assertClass(Integer[][][].class, f.get(0));
		assertClass(int[][][].class, g.get(0));

		return "OK";
	}

	@RestMethod(name=POST, path="/beanFormData")
	public String beanFormData(
			@FormData("a") ABean a,
			@FormData("an") ABean an,
			@FormData("b") ABean[][][] b,
			@FormData("c") List<ABean> c,
			@FormData("d") List<ABean[][][]> d,
			@FormData("e") Map<String,ABean> e,
			@FormData("f") Map<String,List<ABean>> f,
			@FormData("g") Map<String,List<ABean[][][]>> g,
			@FormData("h") Map<Integer,List<ABean>> h
		) throws Exception {

		assertObjectEquals("{a:1,b:'foo'}", a);
		assertNull(an);
		assertObjectEquals("[[[{a:1,b:'foo'},null],null],null]", b);
		assertObjectEquals("[{a:1,b:'foo'},null]", c);
		assertObjectEquals("[[[[{a:1,b:'foo'},null],null],null],null]", d);
		assertObjectEquals("{foo:{a:1,b:'foo'}}", e);
		assertObjectEquals("{foo:[{a:1,b:'foo'}]}", f);
		assertObjectEquals("{foo:[[[[{a:1,b:'foo'},null],null],null],null]}", g);
		assertObjectEquals("{'1':[{a:1,b:'foo'}]}", h);

		assertClass(ABean.class, c.get(0));
		assertClass(ABean[][][].class, d.get(0));
		assertClass(ABean.class, e.get("foo"));
		assertClass(ABean.class, f.get("foo").get(0));
		assertClass(ABean[][][].class, g.get("foo").get(0));
		assertClass(Integer.class, h.keySet().iterator().next());
		assertClass(ABean.class, h.values().iterator().next().get(0));
		return "OK";
	}

	@RestMethod(name=POST, path="/typedBeanFormData")
	public String typedBeanFormData(
			@FormData("a") TypedBean a,
			@FormData("an") TypedBean an,
			@FormData("b") TypedBean[][][] b,
			@FormData("c") List<TypedBean> c,
			@FormData("d") List<TypedBean[][][]> d,
			@FormData("e") Map<String,TypedBean> e,
			@FormData("f") Map<String,List<TypedBean>> f,
			@FormData("g") Map<String,List<TypedBean[][][]>> g,
			@FormData("h") Map<Integer,List<TypedBean>> h
		) throws Exception {

		assertObjectEquals("{_type:'TypedBeanImpl',a:1,b:'foo'}", a);
		assertNull(an);
		assertObjectEquals("[[[{_type:'TypedBeanImpl',a:1,b:'foo'},null],null],null]", b);
		assertObjectEquals("[{_type:'TypedBeanImpl',a:1,b:'foo'},null]", c);
		assertObjectEquals("[[[[{_type:'TypedBeanImpl',a:1,b:'foo'},null],null],null],null]", d);
		assertObjectEquals("{foo:{_type:'TypedBeanImpl',a:1,b:'foo'}}", e);
		assertObjectEquals("{foo:[{_type:'TypedBeanImpl',a:1,b:'foo'}]}", f);
		assertObjectEquals("{foo:[[[[{_type:'TypedBeanImpl',a:1,b:'foo'},null],null],null],null]}", g);
		assertObjectEquals("{'1':[{_type:'TypedBeanImpl',a:1,b:'foo'}]}", h);

		assertClass(TypedBeanImpl.class, a);
		assertClass(TypedBeanImpl.class, b[0][0][0]);
		assertClass(TypedBeanImpl.class, c.get(0));
		assertClass(TypedBeanImpl.class, d.get(0)[0][0][0]);
		assertClass(TypedBeanImpl.class, e.get("foo"));
		assertClass(TypedBeanImpl.class, f.get("foo").get(0));
		assertClass(TypedBeanImpl.class, g.get("foo").get(0)[0][0][0]);
		assertClass(Integer.class, h.keySet().iterator().next());
		assertClass(TypedBeanImpl.class, h.get(1).get(0));

		return "OK";
	}

	@RestMethod(name=POST, path="/swappedPojoFormData")
	public String swappedPojoFormData(
			@FormData("a") SwappedPojo a,
			@FormData("b") SwappedPojo[][][] b,
			@FormData("c") Map<SwappedPojo,SwappedPojo> c,
			@FormData("d") Map<SwappedPojo,SwappedPojo[][][]> d
		) throws Exception {

		assertObjectEquals("'swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/'", a);
		assertObjectEquals("[[['swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/',null],null],null]", b);
		assertObjectEquals("{'swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/':'swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/'}", c);
		assertObjectEquals("{'swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/':[[['swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/',null],null],null]}", d);

		assertClass(SwappedPojo.class, a);
		assertClass(SwappedPojo.class, b[0][0][0]);
		assertClass(SwappedPojo.class, c.keySet().iterator().next());
		assertClass(SwappedPojo.class, c.values().iterator().next());
		assertClass(SwappedPojo.class, d.keySet().iterator().next());
		assertClass(SwappedPojo.class, d.values().iterator().next()[0][0][0]);

		return "OK";
	}

	@RestMethod(name=POST, path="/implicitSwappedPojoFormData")
	public String implicitSwappedPojoFormData(
			@FormData("a") ImplicitSwappedPojo a,
			@FormData("b") ImplicitSwappedPojo[][][] b,
			@FormData("c") Map<ImplicitSwappedPojo,ImplicitSwappedPojo> c,
			@FormData("d") Map<ImplicitSwappedPojo,ImplicitSwappedPojo[][][]> d
		) throws Exception {

		assertObjectEquals("'swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/'", a);
		assertObjectEquals("[[['swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/',null],null],null]", b);
		assertObjectEquals("{'swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/':'swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/'}", c);
		assertObjectEquals("{'swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/':[[['swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/',null],null],null]}", d);

		assertClass(ImplicitSwappedPojo.class, a);
		assertClass(ImplicitSwappedPojo.class, b[0][0][0]);
		assertClass(ImplicitSwappedPojo.class, c.keySet().iterator().next());
		assertClass(ImplicitSwappedPojo.class, c.values().iterator().next());
		assertClass(ImplicitSwappedPojo.class, d.keySet().iterator().next());
		assertClass(ImplicitSwappedPojo.class, d.values().iterator().next()[0][0][0]);

		return "OK";
	}

	@RestMethod(name=POST, path="/enumFormData")
	public String enumFormData(
			@FormData("a") TestEnum a,
			@FormData("an") TestEnum an,
			@FormData("b") TestEnum[][][] b,
			@FormData("c") List<TestEnum> c,
			@FormData("d") List<List<List<TestEnum>>> d,
			@FormData("e") List<TestEnum[][][]> e,
			@FormData("f") Map<TestEnum,TestEnum> f,
			@FormData("g") Map<TestEnum,TestEnum[][][]> g,
			@FormData("h") Map<TestEnum,List<TestEnum[][][]>> h
		) throws Exception {

		assertEquals(TestEnum.TWO, a);
		assertNull(an);
		assertObjectEquals("[[['TWO',null],null],null]", b);
		assertObjectEquals("['TWO',null]", c);
		assertObjectEquals("[[['TWO',null],null],null]", d);
		assertObjectEquals("[[[['TWO',null],null],null],null]", e);
		assertObjectEquals("{ONE:'TWO'}", f);
		assertObjectEquals("{ONE:[[['TWO',null],null],null]}", g);
		assertObjectEquals("{ONE:[[[['TWO',null],null],null],null]}", h);

		assertClass(TestEnum.class, c.get(0));
		assertClass(TestEnum.class, d.get(0).get(0).get(0));
		assertClass(TestEnum[][][].class, e.get(0));
		assertClass(TestEnum.class, f.keySet().iterator().next());
		assertClass(TestEnum.class, f.values().iterator().next());
		assertClass(TestEnum.class, g.keySet().iterator().next());
		assertClass(TestEnum[][][].class, g.values().iterator().next());
		assertClass(TestEnum.class, h.keySet().iterator().next());
		assertClass(TestEnum[][][].class, h.values().iterator().next().get(0));

		return "OK";
	}

	@RestMethod(name=POST, path="/mapFormData")
	public String mapFormData(
		@FormData("a") String a,
		@FormData(name="b",allowEmptyValue=true) String b,
		@FormData("c") String c
	) throws Exception {

		assertEquals("foo", a);
		assertEquals("", b);
		assertEquals(null, c);

		return "OK";
	}

	@RestMethod(name=POST, path="/beanFormData2")
	public String beanFormData(
		@FormData("a") String a,
		@FormData(name="b",allowEmptyValue=true) String b,
		@FormData("c") String c
	) throws Exception {

		assertEquals("foo", a);
		assertEquals("", b);
		assertEquals(null, c);

		return "OK";
	}

	@RestMethod(name=POST, path="/nameValuePairsFormData")
	public String nameValuePairsFormData(
		@FormData("a") String a,
		@FormData(name="b",allowEmptyValue=true) String b,
		@FormData("c") String c
	) throws Exception {

		assertEquals("foo", a);
		assertEquals("", b);
		//assertEquals(null, c);  // This is impossible to represent.

		return "OK";
	}

	@RestMethod(name=POST, path="/formDataIfNE1")
	public String formDataIfNE1(
		@FormData("a") String a
	) throws Exception {

		assertEquals("foo", a);

		return "OK";
	}

	@RestMethod(name=POST, path="/formDataIfNE2")
	public String formDataIfNE2(
		@FormData("a") String a
	) throws Exception {

		assertEquals(null, a);

		return "OK";
	}

	@RestMethod(name=POST, path="/formDataIfNEMap")
	public String formDataIfNEMap(
		@FormData("a") String a,
		@FormData("b") String b,
		@FormData("c") String c
	) throws Exception {

		assertEquals("foo", a);
		assertEquals(null, b);
		assertEquals(null, c);

		return "OK";
	}

	@RestMethod(name=POST, path="/formDataIfNEBean")
	public String formDataIfNEBean(
		@FormData("a") String a,
		@FormData("b") String b,
		@FormData("c") String c
	) throws Exception {

		assertEquals("foo", a);
		assertEquals(null, b);
		assertEquals(null, c);

		return "OK";
	}

	@RestMethod(name=POST, path="/formDataIfNENameValuePairs")
	public String formDataIfNENameValuePairs(
		@FormData("a") String a,
		@FormData("b") String b,
		@FormData("c") String c
	) throws Exception {

		assertEquals("foo", a);
		assertEquals(null, b);
		assertEquals(null, c);

		return "OK";
	}


	//-----------------------------------------------------------------------------------------------------------------
	// Path tests
	//-----------------------------------------------------------------------------------------------------------------

	@RestMethod(name=POST, path="/pathVars1/{a}/{b}")
	public String pathVars1(
		@Path("a") int a,
		@Path("b") String b
		) throws Exception {

		assertEquals(1, a);
		assertEquals("foo", b);

		return "OK";
	}


	@RestMethod(name=POST, path="/pathVars2/{a}/{b}")
	public String pathVars2(
		@Path("a") int a,
		@Path("b") String b
		) throws Exception {

		assertEquals(1, a);
		assertEquals("foo", b);

		return "OK";
	}

	@RestMethod(name=POST, path="/pathVars3/{a}/{b}")
	public String pathVars3(
		@Path("a") int a,
		@Path("b") String b
		) throws Exception {

		assertEquals(1, a);
		assertEquals("foo", b);

		return "OK";
	}

	//-----------------------------------------------------------------------------------------------------------------
	// @Request tests
	//-----------------------------------------------------------------------------------------------------------------

	@RestMethod(name=POST, path="/reqBeanPath/{a}/{b}")
	public String reqBeanPath(
		@Path("a") int a,
		@Path("b") String b
		) throws Exception {

		assertEquals(1, a);
		assertEquals("foo", b);

		return "OK";
	}

	@RestMethod(name=POST, path="/reqBeanQuery")
	public String reqBeanQuery(
		@Query("a") int a,
		@Query("b") String b
		) throws Exception {

		assertEquals(1, a);
		assertEquals("foo", b);

		return "OK";
	}

	@RestMethod(name=POST, path="/reqBeanQueryIfNE")
	public String reqBeanQueryIfNE(
		@Query("a") String a,
		@Query("b") String b,
		@Query("c") String c
		) throws Exception {

		assertEquals("foo", a);
		assertNull(b);
		assertNull(c);

		return "OK";
	}

	@RestMethod(name=POST, path="/reqBeanFormData")
	public String reqBeanFormData(
		@FormData("a") int a,
		@FormData("b") String b
		) throws Exception {

		assertEquals(1, a);
		assertEquals("foo", b);

		return "OK";
	}

	@RestMethod(name=POST, path="/reqBeanFormDataIfNE")
	public String reqBeanFormDataIfNE(
		@FormData("a") String a,
		@FormData("b") String b,
		@FormData("c") String c
		) throws Exception {

		assertEquals("foo", a);
		assertNull(b);
		assertNull(c);

		return "OK";
	}

	@RestMethod(name=POST, path="/reqBeanHeader")
	public String reqBeanHeader(
		@Header("a") int a,
		@Header("b") String b
		) throws Exception {

		assertEquals(1, a);
		assertEquals("foo", b);

		return "OK";
	}

	@RestMethod(name=POST, path="/reqBeanHeaderIfNE")
	public String reqBeanHeaderIfNE(
		@Header("a") String a,
		@Header("b") String b,
		@Header("c") String c
		) throws Exception {

		assertEquals("foo", a);
		assertNull(b);
		assertNull(c);

		return "OK";
	}
	//-----------------------------------------------------------------------------------------------------------------
	// Test return types.
	//-----------------------------------------------------------------------------------------------------------------

	// Various primitives

	@RestMethod(name=GET, path="/returnVoid")
	public void returnVoid() {
	}

	@RestMethod(name=GET, path="/returnInteger")
	public Integer returnInteger() {
		return 1;
	}

	@RestMethod(name=GET, path="/returnInt")
	public int returnInt() {
		return 1;
	}

	@RestMethod(name=GET, path="/returnBoolean")
	public boolean returnBoolean() {
		return true;
	}

	@RestMethod(name=GET, path="/returnFloat")
	public float returnFloat() {
		return 1f;
	}

	@RestMethod(name=GET, path="/returnFloatObject")
	public Float returnFloatObject() {
		return 1f;
	}

	@RestMethod(name=GET, path="/returnString")
	public String returnString() {
		return "foobar";
	}

	@RestMethod(name=GET, path="/returnNullString")
	public String returnNullString() {
		return null;
	}

	@RestMethod(name=GET, path="/returnInt3dArray")
	public int[][][] returnInt3dArray() {
		return new int[][][]{{{1,2},null},null};
	}

	@RestMethod(name=GET, path="/returnInteger3dArray")
	public Integer[][][] returnInteger3dArray() {
		return new Integer[][][]{{{1,null},null},null};
	}

	@RestMethod(name=GET, path="/returnString3dArray")
	public String[][][] returnString3dArray() {
		return new String[][][]{{{"foo","bar",null},null},null};
	}

	@RestMethod(name=GET, path="/returnIntegerList")
	public List<Integer> returnIntegerList() {
		return asList(new Integer[]{1,null});
	}

	@RestMethod(name=GET, path="/returnInteger3dList")
	public List<List<List<Integer>>> returnInteger3dList() {
		return AList.of(AList.of(AList.of(1,null),null),null);
	}

	@RestMethod(name=GET, path="/returnInteger1d3dList")
	public List<Integer[][][]> returnInteger1d3dList() {
		return AList.of(new Integer[][][]{{{1,null},null},null},null);
	}

	@RestMethod(name=GET, path="/returnInt1d3dList")
	public List<int[][][]> returnInt1d3dList() {
		return AList.of(new int[][][]{{{1,2},null},null},null);
	}

	@RestMethod(name=GET, path="/returnStringList")
	public List<String> returnStringList() {
		return asList(new String[]{"foo","bar",null});
	}

	// Beans

	@RestMethod(name=GET, path="/returnBean")
	public ABean returnBean() {
		return new ABean().init();
	}

	@RestMethod(name=GET, path="/returnBean3dArray")
	public ABean[][][] returnBean3dArray() {
		return new ABean[][][]{{{new ABean().init(),null},null},null};
	}

	@RestMethod(name=GET, path="/returnBeanList")
	public List<ABean> returnBeanList() {
		return asList(new ABean().init());
	}

	@RestMethod(name=GET, path="/returnBean1d3dList")
	public List<ABean[][][]> returnBean1d3dList() {
		return AList.of(new ABean[][][]{{{new ABean().init(),null},null},null},null);
	}

	@RestMethod(name=GET, path="/returnBeanMap")
	public Map<String,ABean> returnBeanMap() {
		return AMap.of("foo",new ABean().init());
	}

	@RestMethod(name=GET, path="/returnBeanListMap")
	public Map<String,List<ABean>> returnBeanListMap() {
		return AMap.of("foo",asList(new ABean().init()));
	}

	@RestMethod(name=GET, path="/returnBean1d3dListMap")
	public Map<String,List<ABean[][][]>> returnBean1d3dListMap() {
		return AMap.of("foo", AList.of(new ABean[][][]{{{new ABean().init(),null},null},null},null));
	}

	@RestMethod(name=GET, path="/returnBeanListMapIntegerKeys")
	public Map<Integer,List<ABean>> returnBeanListMapIntegerKeys() {
		return AMap.of(1,asList(new ABean().init()));
	}

	// Typed beans

	@RestMethod(name=GET, path="/returnTypedBean")
	public TypedBean returnTypedBean() {
		return new TypedBeanImpl().init();
	}

	@RestMethod(name=GET, path="/returnTypedBean3dArray")
	public TypedBean[][][] returnTypedBean3dArray() {
		return new TypedBean[][][]{{{new TypedBeanImpl().init(),null},null},null};
	}

	@RestMethod(name=GET, path="/returnTypedBeanList")
	public List<TypedBean> returnTypedBeanList() {
		return asList((TypedBean)new TypedBeanImpl().init());
	}

	@RestMethod(name=GET, path="/returnTypedBean1d3dList")
	public List<TypedBean[][][]> returnTypedBean1d3dList() {
		return AList.of(new TypedBean[][][]{{{new TypedBeanImpl().init(),null},null},null},null);
	}

	@RestMethod(name=GET, path="/returnTypedBeanMap")
	public Map<String,TypedBean> returnTypedBeanMap() {
		return AMap.of("foo",new TypedBeanImpl().init());
	}

	@RestMethod(name=GET, path="/returnTypedBeanListMap")
	public Map<String,List<TypedBean>> returnTypedBeanListMap() {
		return AMap.of("foo",asList((TypedBean)new TypedBeanImpl().init()));
	}

	@RestMethod(name=GET, path="/returnTypedBean1d3dListMap")
	public Map<String,List<TypedBean[][][]>> returnTypedBean1d3dListMap() {
		return AMap.of("foo", AList.of(new TypedBean[][][]{{{new TypedBeanImpl().init(),null},null},null},null));
	}

	@RestMethod(name=GET, path="/returnTypedBeanListMapIntegerKeys")
	public Map<Integer,List<TypedBean>> returnTypedBeanListMapIntegerKeys() {
		return AMap.of(1,asList((TypedBean)new TypedBeanImpl().init()));
	}

	// Swapped POJOs

	@RestMethod(name=GET, path="/returnSwappedPojo")
	public SwappedPojo returnSwappedPojo() {
		return new SwappedPojo();
	}

	@RestMethod(name=GET, path="/returnSwappedPojo3dArray")
	public SwappedPojo[][][] returnSwappedPojo3dArray() {
		return new SwappedPojo[][][]{{{new SwappedPojo(),null},null},null};
	}

	@RestMethod(name=GET, path="/returnSwappedPojoMap")
	public Map<SwappedPojo,SwappedPojo> returnSwappedPojoMap() {
		return AMap.of(new SwappedPojo(),new SwappedPojo());
	}

	@RestMethod(name=GET, path="/returnSwappedPojo3dMap")
	public Map<SwappedPojo,SwappedPojo[][][]> returnSwappedPojo3dMap() {
		return AMap.of(new SwappedPojo(),new SwappedPojo[][][]{{{new SwappedPojo(),null},null},null});
	}

	// Implicit swapped POJOs

	@RestMethod(name=GET, path="/returnImplicitSwappedPojo")
	public ImplicitSwappedPojo returnImplicitSwappedPojo() {
		return new ImplicitSwappedPojo();
	}

	@RestMethod(name=GET, path="/returnImplicitSwappedPojo3dArray")
	public ImplicitSwappedPojo[][][] returnImplicitSwappedPojo3dArray() {
		return new ImplicitSwappedPojo[][][]{{{new ImplicitSwappedPojo(),null},null},null};
	}

	@RestMethod(name=GET, path="/returnImplicitSwappedPojoMap")
	public Map<ImplicitSwappedPojo,ImplicitSwappedPojo> returnImplicitSwappedPojoMap() {
		return AMap.of(new ImplicitSwappedPojo(),new ImplicitSwappedPojo());
	}

	@RestMethod(name=GET, path="/returnImplicitSwappedPojo3dMap")
	public Map<ImplicitSwappedPojo,ImplicitSwappedPojo[][][]> returnImplicitSwappedPojo3dMap() {
		return AMap.of(new ImplicitSwappedPojo(),new ImplicitSwappedPojo[][][]{{{new ImplicitSwappedPojo(),null},null},null});
	}

	// Enums

	@RestMethod(name=GET, path="/returnEnum")
	public TestEnum returnEnum() {
		return TestEnum.TWO;
	}

	@RestMethod(name=GET, path="/returnEnum3d")
	public TestEnum[][][] returnEnum3d() {
		return new TestEnum[][][]{{{TestEnum.TWO,null},null},null};
	}

	@RestMethod(name=GET, path="/returnEnumList")
	public List<TestEnum> returnEnumList() {
		return AList.of(TestEnum.TWO,null);
	}

	@RestMethod(name=GET, path="/returnEnum3dList")
	public List<List<List<TestEnum>>> returnEnum3dList() {
		return AList.of(AList.of(AList.of(TestEnum.TWO,null),null),null);
	}

	@RestMethod(name=GET, path="/returnEnum1d3dList")
	public List<TestEnum[][][]> returnEnum1d3dList() {
		return AList.of(new TestEnum[][][]{{{TestEnum.TWO,null},null},null},null);
	}

	@RestMethod(name=GET, path="/returnEnumMap")
	public Map<TestEnum,TestEnum> returnEnumMap() {
		return AMap.of(TestEnum.ONE,TestEnum.TWO);
	}

	@RestMethod(name=GET, path="/returnEnum3dArrayMap")
	public Map<TestEnum,TestEnum[][][]> returnEnum3dArrayMap() {
		return AMap.of(TestEnum.ONE,new TestEnum[][][]{{{TestEnum.TWO,null},null},null});
	}

	@RestMethod(name=GET, path="/returnEnum1d3dListMap")
	public Map<TestEnum,List<TestEnum[][][]>> returnEnum1d3dListMap() {
		return AMap.of(TestEnum.ONE,AList.of(new TestEnum[][][]{{{TestEnum.TWO,null},null},null},null));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Test parameters
	//-----------------------------------------------------------------------------------------------------------------

	// Various primitives

	@RestMethod(name=POST, path="/setInt")
	public void setInt(@Body int x) {
		assertEquals(1, x);
	}

	@RestMethod(name=POST, path="/setInteger")
	public void setInteger(@Body Integer x) {
		assertEquals((Integer)1, x);
	}

	@RestMethod(name=POST, path="/setBoolean")
	public void setBoolean(@Body boolean x) {
		assertTrue(x);
	}

	@RestMethod(name=POST, path="/setFloat")
	public void setFloat(@Body float x) {
		assertTrue(1f == x);
	}

	@RestMethod(name=POST, path="/setFloatObject")
	public void setFloatObject(@Body Float x) {
		assertTrue(1f == x);
	}

	@RestMethod(name=POST, path="/setString")
	public void setString(@Body String x) {
		assertEquals("foo", x);
	}

	@RestMethod(name=POST, path="/setNullString")
	public void setNullString(@Body String x) {
		assertNull(x);
	}

	@RestMethod(name=POST, path="/setInt3dArray")
	public String setInt3dArray(@Body int[][][] x) {
		return ""+x[0][0][0];
	}

	@RestMethod(name=POST, path="/setInteger3dArray")
	public void setInteger3dArray(@Body Integer[][][] x) {
		assertObjectEquals("[[[1,null],null],null]", x);
	}

	@RestMethod(name=POST, path="/setString3dArray")
	public void setString3dArray(@Body String[][][] x) {
		assertObjectEquals("[[['foo',null],null],null]", x);
	}

	@RestMethod(name=POST, path="/setIntegerList")
	public void setIntegerList(@Body List<Integer> x) {
		assertObjectEquals("[1,null]", x);
		assertClass(Integer.class, x.get(0));
	}

	@RestMethod(name=POST, path="/setInteger3dList")
	public void setInteger3dList(@Body List<List<List<Integer>>> x) {
		assertObjectEquals("[[[1,null],null],null]", x);
		assertClass(Integer.class, x.get(0).get(0).get(0));
	}

	@RestMethod(name=POST, path="/setInteger1d3dList")
	public void setInteger1d3dList(@Body List<Integer[][][]> x) {
		assertObjectEquals("[[[[1,null],null],null],null]", x);
		assertClass(Integer[][][].class, x.get(0));
		assertClass(Integer.class, x.get(0)[0][0][0]);
	}

	@RestMethod(name=POST, path="/setInt1d3dList")
	public void setInt1d3dList(@Body List<int[][][]> x) {
		assertObjectEquals("[[[[1,2],null],null],null]", x);
		assertClass(int[][][].class, x.get(0));
	}

	@RestMethod(name=POST, path="/setStringList")
	public void setStringList(@Body List<String> x) {
		assertObjectEquals("['foo','bar',null]", x);
	}

	// Beans

	@RestMethod(name=POST, path="/setBean")
	public void setBean(@Body ABean x) {
		assertObjectEquals("{a:1,b:'foo'}", x);
	}

	@RestMethod(name=POST, path="/setBean3dArray")
	public void setBean3dArray(@Body ABean[][][] x) {
		assertObjectEquals("[[[{a:1,b:'foo'},null],null],null]", x);
	}

	@RestMethod(name=POST, path="/setBeanList")
	public void setBeanList(@Body List<ABean> x) {
		assertObjectEquals("[{a:1,b:'foo'}]", x);
	}

	@RestMethod(name=POST, path="/setBean1d3dList")
	public void setBean1d3dList(@Body List<ABean[][][]> x) {
		assertObjectEquals("[[[[{a:1,b:'foo'},null],null],null],null]", x);
	}

	@RestMethod(name=POST, path="/setBeanMap")
	public void setBeanMap(@Body Map<String,ABean> x) {
		assertObjectEquals("{foo:{a:1,b:'foo'}}", x);
	}

	@RestMethod(name=POST, path="/setBeanListMap")
	public void setBeanListMap(@Body Map<String,List<ABean>> x) {
		assertObjectEquals("{foo:[{a:1,b:'foo'}]}", x);
	}

	@RestMethod(name=POST, path="/setBean1d3dListMap")
	public void setBean1d3dListMap(@Body Map<String,List<ABean[][][]>> x) {
		assertObjectEquals("{foo:[[[[{a:1,b:'foo'},null],null],null],null]}", x);
	}

	@RestMethod(name=POST, path="/setBeanListMapIntegerKeys")
	public void setBeanListMapIntegerKeys(@Body Map<Integer,List<ABean>> x) {
		assertObjectEquals("{'1':[{a:1,b:'foo'}]}", x);  // Note: JsonSerializer serializes key as string.
		assertClass(Integer.class, x.keySet().iterator().next());
	}

	// Typed beans

	@RestMethod(name=POST, path="/setTypedBean")
	public void setTypedBean(@Body TypedBean x) {
		assertObjectEquals("{_type:'TypedBeanImpl',a:1,b:'foo'}", x);
		assertClass(TypedBeanImpl.class, x);
	}

	@RestMethod(name=POST, path="/setTypedBean3dArray")
	public void setTypedBean3dArray(@Body TypedBean[][][] x) {
		assertObjectEquals("[[[{_type:'TypedBeanImpl',a:1,b:'foo'},null],null],null]", x);
		assertClass(TypedBeanImpl.class, x[0][0][0]);
	}

	@RestMethod(name=POST, path="/setTypedBeanList")
	public void setTypedBeanList(@Body List<TypedBean> x) {
		assertObjectEquals("[{_type:'TypedBeanImpl',a:1,b:'foo'}]", x);
		assertClass(TypedBeanImpl.class, x.get(0));
	}

	@RestMethod(name=POST, path="/setTypedBean1d3dList")
	public void setTypedBean1d3dList(@Body List<TypedBean[][][]> x) {
		assertObjectEquals("[[[[{_type:'TypedBeanImpl',a:1,b:'foo'},null],null],null],null]", x);
		assertClass(TypedBeanImpl.class, x.get(0)[0][0][0]);
	}

	@RestMethod(name=POST, path="/setTypedBeanMap")
	public void setTypedBeanMap(@Body Map<String,TypedBean> x) {
		assertObjectEquals("{foo:{_type:'TypedBeanImpl',a:1,b:'foo'}}", x);
		assertClass(TypedBeanImpl.class, x.get("foo"));
	}

	@RestMethod(name=POST, path="/setTypedBeanListMap")
	public void setTypedBeanListMap(@Body Map<String,List<TypedBean>> x) {
		assertObjectEquals("{foo:[{_type:'TypedBeanImpl',a:1,b:'foo'}]}", x);
		assertClass(TypedBeanImpl.class, x.get("foo").get(0));
	}

	@RestMethod(name=POST, path="/setTypedBean1d3dListMap")
	public void setTypedBean1d3dListMap(@Body Map<String,List<TypedBean[][][]>> x) {
		assertObjectEquals("{foo:[[[[{_type:'TypedBeanImpl',a:1,b:'foo'},null],null],null],null]}", x);
		assertClass(TypedBeanImpl.class, x.get("foo").get(0)[0][0][0]);
	}

	@RestMethod(name=POST, path="/setTypedBeanListMapIntegerKeys")
	public void setTypedBeanListMapIntegerKeys(@Body Map<Integer,List<TypedBean>> x) {
		assertObjectEquals("{'1':[{_type:'TypedBeanImpl',a:1,b:'foo'}]}", x);  // Note: JsonSerializer serializes key as string.
		assertClass(TypedBeanImpl.class, x.get(1).get(0));
	}

	// Swapped POJOs

	@RestMethod(name=POST, path="/setSwappedPojo")
	public void setSwappedPojo(@Body SwappedPojo x) {
		assertTrue(x.wasUnswapped);
	}

	@RestMethod(name=POST, path="/setSwappedPojo3dArray")
	public void setSwappedPojo3dArray(@Body SwappedPojo[][][] x) {
		assertObjectEquals("[[['"+SWAP+"',null],null],null]", x);
		assertTrue(x[0][0][0].wasUnswapped);
	}

	@RestMethod(name=POST, path="/setSwappedPojoMap")
	public void setSwappedPojoMap(@Body Map<SwappedPojo,SwappedPojo> x) {
		assertObjectEquals("{'"+SWAP+"':'"+SWAP+"'}", x);
		Map.Entry<SwappedPojo,SwappedPojo> e = x.entrySet().iterator().next();
		assertTrue(e.getKey().wasUnswapped);
		assertTrue(e.getValue().wasUnswapped);
	}

	@RestMethod(name=POST, path="/setSwappedPojo3dMap")
	public void setSwappedPojo3dMap(@Body Map<SwappedPojo,SwappedPojo[][][]> x) {
		assertObjectEquals("{'"+SWAP+"':[[['"+SWAP+"',null],null],null]}", x);
		Map.Entry<SwappedPojo,SwappedPojo[][][]> e = x.entrySet().iterator().next();
		assertTrue(e.getKey().wasUnswapped);
		assertTrue(e.getValue()[0][0][0].wasUnswapped);
	}

	// Implicit swapped POJOs

	@RestMethod(name=POST, path="/setImplicitSwappedPojo")
	public void setImplicitSwappedPojo(@Body ImplicitSwappedPojo x) {
		assertTrue(x.wasUnswapped);
	}

	@RestMethod(name=POST, path="/setImplicitSwappedPojo3dArray")
	public void setImplicitSwappedPojo3dArray(@Body ImplicitSwappedPojo[][][] x) {
		assertObjectEquals("[[['"+SWAP+"',null],null],null]", x);
		assertTrue(x[0][0][0].wasUnswapped);
	}

	@RestMethod(name=POST, path="/setImplicitSwappedPojoMap")
	public void setImplicitSwappedPojoMap(@Body Map<ImplicitSwappedPojo,ImplicitSwappedPojo> x) {
		assertObjectEquals("{'"+SWAP+"':'"+SWAP+"'}", x);
		Map.Entry<ImplicitSwappedPojo,ImplicitSwappedPojo> e = x.entrySet().iterator().next();
		assertTrue(e.getKey().wasUnswapped);
		assertTrue(e.getValue().wasUnswapped);
	}

	@RestMethod(name=POST, path="/setImplicitSwappedPojo3dMap")
	public void setImplicitSwappedPojo3dMap(@Body Map<ImplicitSwappedPojo,ImplicitSwappedPojo[][][]> x) {
		assertObjectEquals("{'"+SWAP+"':[[['"+SWAP+"',null],null],null]}", x);
		Map.Entry<ImplicitSwappedPojo,ImplicitSwappedPojo[][][]> e = x.entrySet().iterator().next();
		assertTrue(e.getKey().wasUnswapped);
		assertTrue(e.getValue()[0][0][0].wasUnswapped);
	}

	// Enums

	@RestMethod(name=POST, path="/setEnum")
	public void setEnum(@Body TestEnum x) {
		assertEquals(TestEnum.TWO, x);
	}

	@RestMethod(name=POST, path="/setEnum3d")
	public void setEnum3d(@Body TestEnum[][][] x) {
		assertObjectEquals("[[['TWO',null],null],null]", x);
	}

	@RestMethod(name=POST, path="/setEnumList")
	public void setEnumList(@Body List<TestEnum> x) {
		assertObjectEquals("['TWO',null]", x);
		assertClass(TestEnum.class, x.get(0));
	}

	@RestMethod(name=POST, path="/setEnum3dList")
	public void setEnum3dList(@Body List<List<List<TestEnum>>> x) {
		assertObjectEquals("[[['TWO',null],null],null]", x);
		assertClass(TestEnum.class, x.get(0).get(0).get(0));
	}

	@RestMethod(name=POST, path="/setEnum1d3dList")
	public void setEnum1d3dList(@Body List<TestEnum[][][]> x) {
		assertObjectEquals("[[[['TWO',null],null],null],null]", x);
		assertClass(TestEnum[][][].class, x.get(0));
	}

	@RestMethod(name=POST, path="/setEnumMap")
	public void setEnumMap(@Body Map<TestEnum,TestEnum> x) {
		assertObjectEquals("{ONE:'TWO'}", x);
		Map.Entry<TestEnum,TestEnum> e = x.entrySet().iterator().next();
		assertClass(TestEnum.class, e.getKey());
		assertClass(TestEnum.class, e.getValue());
	}

	@RestMethod(name=POST, path="/setEnum3dArrayMap")
	public void setEnum3dArrayMap(@Body Map<TestEnum,TestEnum[][][]> x) {
		assertObjectEquals("{ONE:[[['TWO',null],null],null]}", x);
		Map.Entry<TestEnum,TestEnum[][][]> e = x.entrySet().iterator().next();
		assertClass(TestEnum.class, e.getKey());
		assertClass(TestEnum[][][].class, e.getValue());
	}

	@RestMethod(name=POST, path="/setEnum1d3dListMap")
	public void setEnum1d3dListMap(@Body Map<TestEnum,List<TestEnum[][][]>> x) {
		assertObjectEquals("{ONE:[[[['TWO',null],null],null],null]}", x);
		Map.Entry<TestEnum,List<TestEnum[][][]>> e = x.entrySet().iterator().next();
		assertClass(TestEnum.class, e.getKey());
		assertClass(TestEnum[][][].class, e.getValue().get(0));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// PartFormatter tests
	//-----------------------------------------------------------------------------------------------------------------

	@RestMethod(name=POST, path="/partFormatters/{p1}")
	public String partFormatter(
		@Path("p1") String p1,
		@Header("h1") String h1,
		@Query("q1") String q1,
		@FormData("f1") String f1
	) throws Exception {

		assertEquals("dummy-1", p1);
		assertEquals("dummy-2", h1);
		assertEquals("dummy-3", q1);
		assertEquals("dummy-4", f1);

		return "OK";
	}

	//-----------------------------------------------------------------------------------------------------------------
	// @RemoteMethod(returns=HTTP_STATUS)
	//-----------------------------------------------------------------------------------------------------------------

	@RestMethod(name=GET, path="/httpStatusReturn200")
	public void httpStatusReturn200(RestResponse res) {
		res.setStatus(200);
	}

	@RestMethod(name=GET, path="/httpStatusReturn404")
	public void httpStatusReturn404(RestResponse res) {
		res.setStatus(404);
	}
}
