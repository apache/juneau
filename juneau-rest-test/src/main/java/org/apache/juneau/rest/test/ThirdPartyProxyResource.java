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

import static java.util.Arrays.*;
import static org.apache.juneau.rest.test.TestUtils.*;
import static org.apache.juneau.rest.test.pojos.Constants.*;
import static org.junit.Assert.*;

import java.util.*;

import org.apache.juneau.microservice.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.test.pojos.*;
import org.apache.juneau.utils.*;

/**
 * JUnit automated testcase resource.
 */
@RestResource(
	path="/testThirdPartyProxy"
)
@SuppressWarnings("serial")
public class ThirdPartyProxyResource extends ResourceJena {

	//--------------------------------------------------------------------------------
	// Header tests
	//--------------------------------------------------------------------------------

	@RestMethod(name="GET", path="/primitiveHeaders")
	public String primitiveHeaders(
			@Header("h1") String h1,
			@Header("h1n") String h1n,
			@Header("h2") int h2,
			@Header("h3") Integer h3,
			@Header("h3n") Integer h3n,
			@Header("h4") Boolean h4,
			@Header("h5") float h5,
			@Header("h6") Float h6
		) throws Exception {

		assertEquals(h1, "foo");
		assertNull(h1n);
		assertEquals(123, h2);
		assertEquals(123, (int)h3);
		assertNull(h3n);
		assertTrue(h4);
		assertTrue(1f == h5);
		assertTrue(1f == h6);
		return "OK";
	}

	@RestMethod(name="GET", path="/primitiveCollectionHeaders")
	public String primitiveCollectionHeaders(
			@Header("h1") int[][][] h1,
			@Header("h2") Integer[][][] h2,
			@Header("h3") String[][][] h3,
			@Header("h4") List<Integer> h4,
			@Header("h5") List<List<List<Integer>>> h5,
			@Header("h6") List<Integer[][][]> h6,
			@Header("h7") List<int[][][]> h7,
			@Header("h8") List<String> h8
		) throws Exception {

		assertObjectEquals("[[[1,2],null],null]", h1);
		assertObjectEquals("[[[1,null],null],null]", h2);
		assertObjectEquals("[[['foo',null],null],null]", h3);
		assertObjectEquals("[1,null]", h4);
		assertObjectEquals("[[[1,null],null],null]", h5);
		assertObjectEquals("[[[[1,null],null],null],null]", h6);
		assertObjectEquals("[[[[1,2],null],null],null]", h7);
		assertObjectEquals("['foo','bar',null]", h8);

		assertClass(Integer.class, h4.get(0));
		assertClass(Integer.class, h5.get(0).get(0).get(0));
		assertClass(Integer[][][].class, h6.get(0));
		assertClass(int[][][].class, h7.get(0));

		return "OK";
	}

	@RestMethod(name="GET", path="/beanHeaders")
	public String beanHeaders(
			@Header("h1") ABean h1,
			@Header("h1n") ABean h1n,
			@Header("h2") ABean[][][] h2,
			@Header("h3") List<ABean> h3,
			@Header("h4") List<ABean[][][]> h4,
			@Header("h5") Map<String,ABean> h5,
			@Header("h6") Map<String,List<ABean>> h6,
			@Header("h7") Map<String,List<ABean[][][]>> h7,
			@Header("h8") Map<Integer,List<ABean>> h8
		) throws Exception {

		assertObjectEquals("{a:1,b:'foo'}", h1);
		assertNull(h1n);
		assertObjectEquals("[[[{a:1,b:'foo'},null],null],null]", h2);
		assertObjectEquals("[{a:1,b:'foo'},null]", h3);
		assertObjectEquals("[[[[{a:1,b:'foo'},null],null],null],null]", h4);
		assertObjectEquals("{foo:{a:1,b:'foo'}}", h5);
		assertObjectEquals("{foo:[{a:1,b:'foo'}]}", h6);
		assertObjectEquals("{foo:[[[[{a:1,b:'foo'},null],null],null],null]}", h7);
		assertObjectEquals("{'1':[{a:1,b:'foo'}]}", h8);

		assertClass(ABean.class, h3.get(0));
		assertClass(ABean[][][].class, h4.get(0));
		assertClass(ABean.class, h5.get("foo"));
		assertClass(ABean.class, h6.get("foo").get(0));
		assertClass(ABean[][][].class, h7.get("foo").get(0));
		assertClass(Integer.class, h8.keySet().iterator().next());
		assertClass(ABean.class, h8.values().iterator().next().get(0));
		return "OK";
	}

	@RestMethod(name="GET", path="/typedBeanHeaders")
	public String typedBeanHeaders(
			@Header("h1") TypedBean h1,
			@Header("h1n") TypedBean h1n,
			@Header("h2") TypedBean[][][] h2,
			@Header("h3") List<TypedBean> h3,
			@Header("h4") List<TypedBean[][][]> h4,
			@Header("h5") Map<String,TypedBean> h5,
			@Header("h6") Map<String,List<TypedBean>> h6,
			@Header("h7") Map<String,List<TypedBean[][][]>> h7,
			@Header("h8") Map<Integer,List<TypedBean>> h8
		) throws Exception {

		assertObjectEquals("{_type:'TypedBeanImpl',a:1,b:'foo'}", h1);
		assertNull(h1n);
		assertObjectEquals("[[[{_type:'TypedBeanImpl',a:1,b:'foo'},null],null],null]", h2);
		assertObjectEquals("[{_type:'TypedBeanImpl',a:1,b:'foo'},null]", h3);
		assertObjectEquals("[[[[{_type:'TypedBeanImpl',a:1,b:'foo'},null],null],null],null]", h4);
		assertObjectEquals("{foo:{_type:'TypedBeanImpl',a:1,b:'foo'}}", h5);
		assertObjectEquals("{foo:[{_type:'TypedBeanImpl',a:1,b:'foo'}]}", h6);
		assertObjectEquals("{foo:[[[[{_type:'TypedBeanImpl',a:1,b:'foo'},null],null],null],null]}", h7);
		assertObjectEquals("{'1':[{_type:'TypedBeanImpl',a:1,b:'foo'}]}", h8);

		assertClass(TypedBeanImpl.class, h1);
		assertClass(TypedBeanImpl.class, h2[0][0][0]);
		assertClass(TypedBeanImpl.class, h3.get(0));
		assertClass(TypedBeanImpl.class, h4.get(0)[0][0][0]);
		assertClass(TypedBeanImpl.class, h5.get("foo"));
		assertClass(TypedBeanImpl.class, h6.get("foo").get(0));
		assertClass(TypedBeanImpl.class, h7.get("foo").get(0)[0][0][0]);
		assertClass(Integer.class, h8.keySet().iterator().next());
		assertClass(TypedBeanImpl.class, h8.get(1).get(0));

		return "OK";
	}

	@RestMethod(name="GET", path="/swappedPojoHeaders")
	public String swappedPojoHeaders(
			@Header("h1") SwappedPojo h1,
			@Header("h2") SwappedPojo[][][] h2,
			@Header("h3") Map<SwappedPojo,SwappedPojo> h3,
			@Header("h4") Map<SwappedPojo,SwappedPojo[][][]> h4
		) throws Exception {

		assertObjectEquals("'swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/'", h1);
		assertObjectEquals("[[['swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/',null],null],null]", h2);
		assertObjectEquals("{'swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/':'swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/'}", h3);
		assertObjectEquals("{'swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/':[[['swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/',null],null],null]}", h4);

		assertClass(SwappedPojo.class, h1);
		assertClass(SwappedPojo.class, h2[0][0][0]);
		assertClass(SwappedPojo.class, h3.keySet().iterator().next());
		assertClass(SwappedPojo.class, h3.values().iterator().next());
		assertClass(SwappedPojo.class, h4.keySet().iterator().next());
		assertClass(SwappedPojo.class, h4.values().iterator().next()[0][0][0]);

		return "OK";
	}

	@RestMethod(name="GET", path="/implicitSwappedPojoHeaders")
	public String implicitSwappedPojoHeaders(
			@Header("h1") ImplicitSwappedPojo h1,
			@Header("h2") ImplicitSwappedPojo[][][] h2,
			@Header("h3") Map<ImplicitSwappedPojo,ImplicitSwappedPojo> h3,
			@Header("h4") Map<ImplicitSwappedPojo,ImplicitSwappedPojo[][][]> h4
		) throws Exception {

		assertObjectEquals("'swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/'", h1);
		assertObjectEquals("[[['swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/',null],null],null]", h2);
		assertObjectEquals("{'swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/':'swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/'}", h3);
		assertObjectEquals("{'swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/':[[['swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/',null],null],null]}", h4);

		assertClass(ImplicitSwappedPojo.class, h1);
		assertClass(ImplicitSwappedPojo.class, h2[0][0][0]);
		assertClass(ImplicitSwappedPojo.class, h3.keySet().iterator().next());
		assertClass(ImplicitSwappedPojo.class, h3.values().iterator().next());
		assertClass(ImplicitSwappedPojo.class, h4.keySet().iterator().next());
		assertClass(ImplicitSwappedPojo.class, h4.values().iterator().next()[0][0][0]);

		return "OK";
	}

	@RestMethod(name="GET", path="/enumHeaders")
	public String enumHeaders(
			@Header("h1") TestEnum h1,
			@Header("h1n") TestEnum h1n,
			@Header("h2") TestEnum[][][] h2,
			@Header("h3") List<TestEnum> h3,
			@Header("h4") List<List<List<TestEnum>>> h4,
			@Header("h5") List<TestEnum[][][]> h5,
			@Header("h6") Map<TestEnum,TestEnum> h6,
			@Header("h7") Map<TestEnum,TestEnum[][][]> h7,
			@Header("h8") Map<TestEnum,List<TestEnum[][][]>> h8
		) throws Exception {

		assertEquals(TestEnum.TWO, h1);
		assertNull(h1n);
		assertObjectEquals("[[['TWO',null],null],null]", h2);
		assertObjectEquals("['TWO',null]", h3);
		assertObjectEquals("[[['TWO',null],null],null]", h4);
		assertObjectEquals("[[[['TWO',null],null],null],null]", h5);
		assertObjectEquals("{ONE:'TWO'}", h6);
		assertObjectEquals("{ONE:[[['TWO',null],null],null]}", h7);
		assertObjectEquals("{ONE:[[[['TWO',null],null],null],null]}", h8);

		assertClass(TestEnum.class, h3.get(0));
		assertClass(TestEnum.class, h4.get(0).get(0).get(0));
		assertClass(TestEnum[][][].class, h5.get(0));
		assertClass(TestEnum.class, h6.keySet().iterator().next());
		assertClass(TestEnum.class, h6.values().iterator().next());
		assertClass(TestEnum.class, h7.keySet().iterator().next());
		assertClass(TestEnum[][][].class, h7.values().iterator().next());
		assertClass(TestEnum.class, h8.keySet().iterator().next());
		assertClass(TestEnum[][][].class, h8.values().iterator().next().get(0));

		return "OK";
	}

	//--------------------------------------------------------------------------------
	// Query tests
	//--------------------------------------------------------------------------------

	@RestMethod(name="GET", path="/primitiveQueries")
	public String primitiveQueries(
			@Query("h1") String h1,
			@Query("h1n") String h1n,
			@Query("h2") int h2,
			@Query("h3") Integer h3,
			@Query("h3n") Integer h3n,
			@Query("h4") Boolean h4,
			@Query("h5") float h5,
			@Query("h6") Float h6
		) throws Exception {

		assertEquals(h1, "foo");
		assertNull(h1n);
		assertEquals(123, h2);
		assertEquals(123, (int)h3);
		assertNull(h3n);
		assertTrue(h4);
		assertTrue(1f == h5);
		assertTrue(1f == h6);
		return "OK";
	}

	@RestMethod(name="GET", path="/primitiveCollectionQueries")
	public String primitiveCollectionQueries(
			@Query("h1") int[][][] h1,
			@Query("h2") Integer[][][] h2,
			@Query("h3") String[][][] h3,
			@Query("h4") List<Integer> h4,
			@Query("h5") List<List<List<Integer>>> h5,
			@Query("h6") List<Integer[][][]> h6,
			@Query("h7") List<int[][][]> h7,
			@Query("h8") List<String> h8
		) throws Exception {

		assertObjectEquals("[[[1,2],null],null]", h1);
		assertObjectEquals("[[[1,null],null],null]", h2);
		assertObjectEquals("[[['foo',null],null],null]", h3);
		assertObjectEquals("[1,null]", h4);
		assertObjectEquals("[[[1,null],null],null]", h5);
		assertObjectEquals("[[[[1,null],null],null],null]", h6);
		assertObjectEquals("[[[[1,2],null],null],null]", h7);
		assertObjectEquals("['foo','bar',null]", h8);

		assertClass(Integer.class, h4.get(0));
		assertClass(Integer.class, h5.get(0).get(0).get(0));
		assertClass(Integer[][][].class, h6.get(0));
		assertClass(int[][][].class, h7.get(0));

		return "OK";
	}

	@RestMethod(name="GET", path="/beanQueries")
	public String beanQueries(
			@Query("h1") ABean h1,
			@Query("h1n") ABean h1n,
			@Query("h2") ABean[][][] h2,
			@Query("h3") List<ABean> h3,
			@Query("h4") List<ABean[][][]> h4,
			@Query("h5") Map<String,ABean> h5,
			@Query("h6") Map<String,List<ABean>> h6,
			@Query("h7") Map<String,List<ABean[][][]>> h7,
			@Query("h8") Map<Integer,List<ABean>> h8
		) throws Exception {

		assertObjectEquals("{a:1,b:'foo'}", h1);
		assertNull(h1n);
		assertObjectEquals("[[[{a:1,b:'foo'},null],null],null]", h2);
		assertObjectEquals("[{a:1,b:'foo'},null]", h3);
		assertObjectEquals("[[[[{a:1,b:'foo'},null],null],null],null]", h4);
		assertObjectEquals("{foo:{a:1,b:'foo'}}", h5);
		assertObjectEquals("{foo:[{a:1,b:'foo'}]}", h6);
		assertObjectEquals("{foo:[[[[{a:1,b:'foo'},null],null],null],null]}", h7);
		assertObjectEquals("{'1':[{a:1,b:'foo'}]}", h8);

		assertClass(ABean.class, h3.get(0));
		assertClass(ABean[][][].class, h4.get(0));
		assertClass(ABean.class, h5.get("foo"));
		assertClass(ABean.class, h6.get("foo").get(0));
		assertClass(ABean[][][].class, h7.get("foo").get(0));
		assertClass(Integer.class, h8.keySet().iterator().next());
		assertClass(ABean.class, h8.values().iterator().next().get(0));
		return "OK";
	}

	@RestMethod(name="GET", path="/typedBeanQueries")
	public String typedBeanQueries(
			@Query("h1") TypedBean h1,
			@Query("h1n") TypedBean h1n,
			@Query("h2") TypedBean[][][] h2,
			@Query("h3") List<TypedBean> h3,
			@Query("h4") List<TypedBean[][][]> h4,
			@Query("h5") Map<String,TypedBean> h5,
			@Query("h6") Map<String,List<TypedBean>> h6,
			@Query("h7") Map<String,List<TypedBean[][][]>> h7,
			@Query("h8") Map<Integer,List<TypedBean>> h8
		) throws Exception {

		assertObjectEquals("{_type:'TypedBeanImpl',a:1,b:'foo'}", h1);
		assertNull(h1n);
		assertObjectEquals("[[[{_type:'TypedBeanImpl',a:1,b:'foo'},null],null],null]", h2);
		assertObjectEquals("[{_type:'TypedBeanImpl',a:1,b:'foo'},null]", h3);
		assertObjectEquals("[[[[{_type:'TypedBeanImpl',a:1,b:'foo'},null],null],null],null]", h4);
		assertObjectEquals("{foo:{_type:'TypedBeanImpl',a:1,b:'foo'}}", h5);
		assertObjectEquals("{foo:[{_type:'TypedBeanImpl',a:1,b:'foo'}]}", h6);
		assertObjectEquals("{foo:[[[[{_type:'TypedBeanImpl',a:1,b:'foo'},null],null],null],null]}", h7);
		assertObjectEquals("{'1':[{_type:'TypedBeanImpl',a:1,b:'foo'}]}", h8);

		assertClass(TypedBeanImpl.class, h1);
		assertClass(TypedBeanImpl.class, h2[0][0][0]);
		assertClass(TypedBeanImpl.class, h3.get(0));
		assertClass(TypedBeanImpl.class, h4.get(0)[0][0][0]);
		assertClass(TypedBeanImpl.class, h5.get("foo"));
		assertClass(TypedBeanImpl.class, h6.get("foo").get(0));
		assertClass(TypedBeanImpl.class, h7.get("foo").get(0)[0][0][0]);
		assertClass(Integer.class, h8.keySet().iterator().next());
		assertClass(TypedBeanImpl.class, h8.get(1).get(0));

		return "OK";
	}

	@RestMethod(name="GET", path="/swappedPojoQueries")
	public String swappedPojoQueries(
			@Query("h1") SwappedPojo h1,
			@Query("h2") SwappedPojo[][][] h2,
			@Query("h3") Map<SwappedPojo,SwappedPojo> h3,
			@Query("h4") Map<SwappedPojo,SwappedPojo[][][]> h4
		) throws Exception {

		assertObjectEquals("'swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/'", h1);
		assertObjectEquals("[[['swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/',null],null],null]", h2);
		assertObjectEquals("{'swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/':'swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/'}", h3);
		assertObjectEquals("{'swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/':[[['swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/',null],null],null]}", h4);

		assertClass(SwappedPojo.class, h1);
		assertClass(SwappedPojo.class, h2[0][0][0]);
		assertClass(SwappedPojo.class, h3.keySet().iterator().next());
		assertClass(SwappedPojo.class, h3.values().iterator().next());
		assertClass(SwappedPojo.class, h4.keySet().iterator().next());
		assertClass(SwappedPojo.class, h4.values().iterator().next()[0][0][0]);

		return "OK";
	}

	@RestMethod(name="GET", path="/implicitSwappedPojoQueries")
	public String implicitSwappedPojoQueries(
			@Query("h1") ImplicitSwappedPojo h1,
			@Query("h2") ImplicitSwappedPojo[][][] h2,
			@Query("h3") Map<ImplicitSwappedPojo,ImplicitSwappedPojo> h3,
			@Query("h4") Map<ImplicitSwappedPojo,ImplicitSwappedPojo[][][]> h4
		) throws Exception {

		assertObjectEquals("'swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/'", h1);
		assertObjectEquals("[[['swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/',null],null],null]", h2);
		assertObjectEquals("{'swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/':'swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/'}", h3);
		assertObjectEquals("{'swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/':[[['swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/',null],null],null]}", h4);

		assertClass(ImplicitSwappedPojo.class, h1);
		assertClass(ImplicitSwappedPojo.class, h2[0][0][0]);
		assertClass(ImplicitSwappedPojo.class, h3.keySet().iterator().next());
		assertClass(ImplicitSwappedPojo.class, h3.values().iterator().next());
		assertClass(ImplicitSwappedPojo.class, h4.keySet().iterator().next());
		assertClass(ImplicitSwappedPojo.class, h4.values().iterator().next()[0][0][0]);

		return "OK";
	}

	@RestMethod(name="GET", path="/enumQueries")
	public String enumQueries(
			@Query("h1") TestEnum h1,
			@Query("h1n") TestEnum h1n,
			@Query("h2") TestEnum[][][] h2,
			@Query("h3") List<TestEnum> h3,
			@Query("h4") List<List<List<TestEnum>>> h4,
			@Query("h5") List<TestEnum[][][]> h5,
			@Query("h6") Map<TestEnum,TestEnum> h6,
			@Query("h7") Map<TestEnum,TestEnum[][][]> h7,
			@Query("h8") Map<TestEnum,List<TestEnum[][][]>> h8
		) throws Exception {

		assertEquals(TestEnum.TWO, h1);
		assertNull(h1n);
		assertObjectEquals("[[['TWO',null],null],null]", h2);
		assertObjectEquals("['TWO',null]", h3);
		assertObjectEquals("[[['TWO',null],null],null]", h4);
		assertObjectEquals("[[[['TWO',null],null],null],null]", h5);
		assertObjectEquals("{ONE:'TWO'}", h6);
		assertObjectEquals("{ONE:[[['TWO',null],null],null]}", h7);
		assertObjectEquals("{ONE:[[[['TWO',null],null],null],null]}", h8);

		assertClass(TestEnum.class, h3.get(0));
		assertClass(TestEnum.class, h4.get(0).get(0).get(0));
		assertClass(TestEnum[][][].class, h5.get(0));
		assertClass(TestEnum.class, h6.keySet().iterator().next());
		assertClass(TestEnum.class, h6.values().iterator().next());
		assertClass(TestEnum.class, h7.keySet().iterator().next());
		assertClass(TestEnum[][][].class, h7.values().iterator().next());
		assertClass(TestEnum.class, h8.keySet().iterator().next());
		assertClass(TestEnum[][][].class, h8.values().iterator().next().get(0));

		return "OK";
	}

	@RestMethod(name="GET", path="/stringQuery1")
	public String stringQuery1(
			@Query("a") int a,
			@Query("b") String b
		) throws Exception {

		assertEquals(1, a);
		assertEquals("foo", b);

		return "OK";
	}

	@RestMethod(name="GET", path="/stringQuery2")
	public String stringQuery2(
			@Query("a") int a,
			@Query("b") String b
		) throws Exception {

		assertEquals(1, a);
		assertEquals("foo", b);

		return "OK";
	}

	@RestMethod(name="GET", path="/mapQuery")
	public String mapQuery(
			@Query("a") int a,
			@Query("b") String b
		) throws Exception {

		assertEquals(1, a);
		assertEquals("foo", b);

		return "OK";
	}

	@RestMethod(name="GET", path="/beanQuery")
	public String beanQuery(
			@Query("a") int a,
			@Query("b") String b
		) throws Exception {

		assertEquals(1, a);
		assertEquals("foo", b);

		return "OK";
	}


	//--------------------------------------------------------------------------------
	// FormData tests
	//--------------------------------------------------------------------------------

	@RestMethod(name="POST", path="/primitiveFormData")
	public String primitiveFormData(
			@FormData("h1") String h1,
			@FormData("h1n") String h1n,
			@FormData("h2") int h2,
			@FormData("h3") Integer h3,
			@FormData("h3n") Integer h3n,
			@FormData("h4") Boolean h4,
			@FormData("h5") float h5,
			@FormData("h6") Float h6
		) throws Exception {

		assertEquals("foo", h1);
		assertNull(h1n);
		assertEquals(123, h2);
		assertEquals(123, (int)h3);
		assertNull(h3n);
		assertTrue(h4);
		assertTrue(1f == h5);
		assertTrue(1f == h6);
		return "OK";
	}

	@RestMethod(name="POST", path="/primitiveCollectionFormData")
	public String primitiveCollectionFormData(
			@FormData("h1") int[][][] h1,
			@FormData("h2") Integer[][][] h2,
			@FormData("h3") String[][][] h3,
			@FormData("h4") List<Integer> h4,
			@FormData("h5") List<List<List<Integer>>> h5,
			@FormData("h6") List<Integer[][][]> h6,
			@FormData("h7") List<int[][][]> h7,
			@FormData("h8") List<String> h8
		) throws Exception {

		assertObjectEquals("[[[1,2],null],null]", h1);
		assertObjectEquals("[[[1,null],null],null]", h2);
		assertObjectEquals("[[['foo',null],null],null]", h3);
		assertObjectEquals("[1,null]", h4);
		assertObjectEquals("[[[1,null],null],null]", h5);
		assertObjectEquals("[[[[1,null],null],null],null]", h6);
		assertObjectEquals("[[[[1,2],null],null],null]", h7);
		assertObjectEquals("['foo','bar',null]", h8);

		assertClass(Integer.class, h4.get(0));
		assertClass(Integer.class, h5.get(0).get(0).get(0));
		assertClass(Integer[][][].class, h6.get(0));
		assertClass(int[][][].class, h7.get(0));

		return "OK";
	}

	@RestMethod(name="POST", path="/beanFormData")
	public String beanFormData(
			@FormData("h1") ABean h1,
			@FormData("h1n") ABean h1n,
			@FormData("h2") ABean[][][] h2,
			@FormData("h3") List<ABean> h3,
			@FormData("h4") List<ABean[][][]> h4,
			@FormData("h5") Map<String,ABean> h5,
			@FormData("h6") Map<String,List<ABean>> h6,
			@FormData("h7") Map<String,List<ABean[][][]>> h7,
			@FormData("h8") Map<Integer,List<ABean>> h8
		) throws Exception {

		assertObjectEquals("{a:1,b:'foo'}", h1);
		assertNull(h1n);
		assertObjectEquals("[[[{a:1,b:'foo'},null],null],null]", h2);
		assertObjectEquals("[{a:1,b:'foo'},null]", h3);
		assertObjectEquals("[[[[{a:1,b:'foo'},null],null],null],null]", h4);
		assertObjectEquals("{foo:{a:1,b:'foo'}}", h5);
		assertObjectEquals("{foo:[{a:1,b:'foo'}]}", h6);
		assertObjectEquals("{foo:[[[[{a:1,b:'foo'},null],null],null],null]}", h7);
		assertObjectEquals("{'1':[{a:1,b:'foo'}]}", h8);

		assertClass(ABean.class, h3.get(0));
		assertClass(ABean[][][].class, h4.get(0));
		assertClass(ABean.class, h5.get("foo"));
		assertClass(ABean.class, h6.get("foo").get(0));
		assertClass(ABean[][][].class, h7.get("foo").get(0));
		assertClass(Integer.class, h8.keySet().iterator().next());
		assertClass(ABean.class, h8.values().iterator().next().get(0));
		return "OK";
	}

	@RestMethod(name="POST", path="/typedBeanFormData")
	public String typedBeanFormData(
			@FormData("h1") TypedBean h1,
			@FormData("h1n") TypedBean h1n,
			@FormData("h2") TypedBean[][][] h2,
			@FormData("h3") List<TypedBean> h3,
			@FormData("h4") List<TypedBean[][][]> h4,
			@FormData("h5") Map<String,TypedBean> h5,
			@FormData("h6") Map<String,List<TypedBean>> h6,
			@FormData("h7") Map<String,List<TypedBean[][][]>> h7,
			@FormData("h8") Map<Integer,List<TypedBean>> h8
		) throws Exception {

		assertObjectEquals("{_type:'TypedBeanImpl',a:1,b:'foo'}", h1);
		assertNull(h1n);
		assertObjectEquals("[[[{_type:'TypedBeanImpl',a:1,b:'foo'},null],null],null]", h2);
		assertObjectEquals("[{_type:'TypedBeanImpl',a:1,b:'foo'},null]", h3);
		assertObjectEquals("[[[[{_type:'TypedBeanImpl',a:1,b:'foo'},null],null],null],null]", h4);
		assertObjectEquals("{foo:{_type:'TypedBeanImpl',a:1,b:'foo'}}", h5);
		assertObjectEquals("{foo:[{_type:'TypedBeanImpl',a:1,b:'foo'}]}", h6);
		assertObjectEquals("{foo:[[[[{_type:'TypedBeanImpl',a:1,b:'foo'},null],null],null],null]}", h7);
		assertObjectEquals("{'1':[{_type:'TypedBeanImpl',a:1,b:'foo'}]}", h8);

		assertClass(TypedBeanImpl.class, h1);
		assertClass(TypedBeanImpl.class, h2[0][0][0]);
		assertClass(TypedBeanImpl.class, h3.get(0));
		assertClass(TypedBeanImpl.class, h4.get(0)[0][0][0]);
		assertClass(TypedBeanImpl.class, h5.get("foo"));
		assertClass(TypedBeanImpl.class, h6.get("foo").get(0));
		assertClass(TypedBeanImpl.class, h7.get("foo").get(0)[0][0][0]);
		assertClass(Integer.class, h8.keySet().iterator().next());
		assertClass(TypedBeanImpl.class, h8.get(1).get(0));

		return "OK";
	}

	@RestMethod(name="POST", path="/swappedPojoFormData")
	public String swappedPojoFormData(
			@FormData("h1") SwappedPojo h1,
			@FormData("h2") SwappedPojo[][][] h2,
			@FormData("h3") Map<SwappedPojo,SwappedPojo> h3,
			@FormData("h4") Map<SwappedPojo,SwappedPojo[][][]> h4
		) throws Exception {

		assertObjectEquals("'swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/'", h1);
		assertObjectEquals("[[['swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/',null],null],null]", h2);
		assertObjectEquals("{'swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/':'swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/'}", h3);
		assertObjectEquals("{'swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/':[[['swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/',null],null],null]}", h4);

		assertClass(SwappedPojo.class, h1);
		assertClass(SwappedPojo.class, h2[0][0][0]);
		assertClass(SwappedPojo.class, h3.keySet().iterator().next());
		assertClass(SwappedPojo.class, h3.values().iterator().next());
		assertClass(SwappedPojo.class, h4.keySet().iterator().next());
		assertClass(SwappedPojo.class, h4.values().iterator().next()[0][0][0]);

		return "OK";
	}

	@RestMethod(name="POST", path="/implicitSwappedPojoFormData")
	public String implicitSwappedPojoFormData(
			@FormData("h1") ImplicitSwappedPojo h1,
			@FormData("h2") ImplicitSwappedPojo[][][] h2,
			@FormData("h3") Map<ImplicitSwappedPojo,ImplicitSwappedPojo> h3,
			@FormData("h4") Map<ImplicitSwappedPojo,ImplicitSwappedPojo[][][]> h4
		) throws Exception {

		assertObjectEquals("'swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/'", h1);
		assertObjectEquals("[[['swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/',null],null],null]", h2);
		assertObjectEquals("{'swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/':'swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/'}", h3);
		assertObjectEquals("{'swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/':[[['swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/',null],null],null]}", h4);

		assertClass(ImplicitSwappedPojo.class, h1);
		assertClass(ImplicitSwappedPojo.class, h2[0][0][0]);
		assertClass(ImplicitSwappedPojo.class, h3.keySet().iterator().next());
		assertClass(ImplicitSwappedPojo.class, h3.values().iterator().next());
		assertClass(ImplicitSwappedPojo.class, h4.keySet().iterator().next());
		assertClass(ImplicitSwappedPojo.class, h4.values().iterator().next()[0][0][0]);

		return "OK";
	}

	@RestMethod(name="POST", path="/enumFormData")
	public String enumFormData(
			@FormData("h1") TestEnum h1,
			@FormData("h1n") TestEnum h1n,
			@FormData("h2") TestEnum[][][] h2,
			@FormData("h3") List<TestEnum> h3,
			@FormData("h4") List<List<List<TestEnum>>> h4,
			@FormData("h5") List<TestEnum[][][]> h5,
			@FormData("h6") Map<TestEnum,TestEnum> h6,
			@FormData("h7") Map<TestEnum,TestEnum[][][]> h7,
			@FormData("h8") Map<TestEnum,List<TestEnum[][][]>> h8
		) throws Exception {

		assertEquals(TestEnum.TWO, h1);
		assertNull(h1n);
		assertObjectEquals("[[['TWO',null],null],null]", h2);
		assertObjectEquals("['TWO',null]", h3);
		assertObjectEquals("[[['TWO',null],null],null]", h4);
		assertObjectEquals("[[[['TWO',null],null],null],null]", h5);
		assertObjectEquals("{ONE:'TWO'}", h6);
		assertObjectEquals("{ONE:[[['TWO',null],null],null]}", h7);
		assertObjectEquals("{ONE:[[[['TWO',null],null],null],null]}", h8);

		assertClass(TestEnum.class, h3.get(0));
		assertClass(TestEnum.class, h4.get(0).get(0).get(0));
		assertClass(TestEnum[][][].class, h5.get(0));
		assertClass(TestEnum.class, h6.keySet().iterator().next());
		assertClass(TestEnum.class, h6.values().iterator().next());
		assertClass(TestEnum.class, h7.keySet().iterator().next());
		assertClass(TestEnum[][][].class, h7.values().iterator().next());
		assertClass(TestEnum.class, h8.keySet().iterator().next());
		assertClass(TestEnum[][][].class, h8.values().iterator().next().get(0));

		return "OK";
	}


	//--------------------------------------------------------------------------------
	// Path tests
	//--------------------------------------------------------------------------------

	@RestMethod(name="POST", path="/pathVars1/{a}/{b}")
	public String pathVars1(
		@Path("a") int a,
		@Path("b") String b
		) throws Exception {

		assertEquals(1, a);
		assertEquals("foo", b);

		return "OK";
	}


	@RestMethod(name="POST", path="/pathVars2/{a}/{b}")
	public String pathVars2(
		@Path("a") int a,
		@Path("b") String b
		) throws Exception {

		assertEquals(1, a);
		assertEquals("foo", b);

		return "OK";
	}

	@RestMethod(name="POST", path="/pathVars3/{a}/{b}")
	public String pathVars3(
		@Path("a") int a,
		@Path("b") String b
		) throws Exception {

		assertEquals(1, a);
		assertEquals("foo", b);

		return "OK";
	}


	//--------------------------------------------------------------------------------
	// Test return types.
	//--------------------------------------------------------------------------------

	// Various primitives

	@RestMethod(name="GET", path="/returnVoid")
	public void returnVoid() {
	}

	@RestMethod(name="GET", path="/returnInteger")
	public Integer returnInteger() {
		return 1;
	}

	@RestMethod(name="GET", path="/returnInt")
	public int returnInt() {
		return 1;
	}

	@RestMethod(name="GET", path="/returnBoolean")
	public boolean returnBoolean() {
		return true;
	}

	@RestMethod(name="GET", path="/returnFloat")
	public float returnFloat() {
		return 1f;
	}

	@RestMethod(name="GET", path="/returnFloatObject")
	public Float returnFloatObject() {
		return 1f;
	}

	@RestMethod(name="GET", path="/returnString")
	public String returnString() {
		return "foobar";
	}

	@RestMethod(name="GET", path="/returnNullString")
	public String returnNullString() {
		return null;
	}

	@RestMethod(name="GET", path="/returnInt3dArray")
	public int[][][] returnInt3dArray() {
		return new int[][][]{{{1,2},null},null};
	}

	@RestMethod(name="GET", path="/returnInteger3dArray")
	public Integer[][][] returnInteger3dArray() {
		return new Integer[][][]{{{1,null},null},null};
	}

	@RestMethod(name="GET", path="/returnString3dArray")
	public String[][][] returnString3dArray() {
		return new String[][][]{{{"foo","bar",null},null},null};
	}

	@RestMethod(name="GET", path="/returnIntegerList")
	public List<Integer> returnIntegerList() {
		return asList(new Integer[]{1,null});
	}

	@RestMethod(name="GET", path="/returnInteger3dList")
	public List<List<List<Integer>>> returnInteger3dList() {
		return new AList<List<List<Integer>>>()
		.append(
			new AList<List<Integer>>()
			.append(
				new AList<Integer>().append(1).append(null)
			)
			.append(null)
		)
		.append(null);
	}

	@RestMethod(name="GET", path="/returnInteger1d3dList")
	public List<Integer[][][]> returnInteger1d3dList() {
		return new AList<Integer[][][]>().append(new Integer[][][]{{{1,null},null},null}).append(null);
	}

	@RestMethod(name="GET", path="/returnInt1d3dList")
	public List<int[][][]> returnInt1d3dList() {
		return new AList<int[][][]>().append(new int[][][]{{{1,2},null},null}).append(null);
	}

	@RestMethod(name="GET", path="/returnStringList")
	public List<String> returnStringList() {
		return asList(new String[]{"foo","bar",null});
	}

	// Beans

	@RestMethod(name="GET", path="/returnBean")
	public ABean returnBean() {
		return new ABean().init();
	}

	@RestMethod(name="GET", path="/returnBean3dArray")
	public ABean[][][] returnBean3dArray() {
		return new ABean[][][]{{{new ABean().init(),null},null},null};
	}

	@RestMethod(name="GET", path="/returnBeanList")
	public List<ABean> returnBeanList() {
		return asList(new ABean().init());
	}

	@RestMethod(name="GET", path="/returnBean1d3dList")
	public List<ABean[][][]> returnBean1d3dList() {
		return new AList<ABean[][][]>().append(new ABean[][][]{{{new ABean().init(),null},null},null}).append(null);
	}

	@RestMethod(name="GET", path="/returnBeanMap")
	public Map<String,ABean> returnBeanMap() {
		return new AMap<String,ABean>().append("foo",new ABean().init());
	}

	@RestMethod(name="GET", path="/returnBeanListMap")
	public Map<String,List<ABean>> returnBeanListMap() {
		return new AMap<String,List<ABean>>().append("foo",asList(new ABean().init()));
	}

	@RestMethod(name="GET", path="/returnBean1d3dListMap")
	public Map<String,List<ABean[][][]>> returnBean1d3dListMap() {
		return new AMap<String,List<ABean[][][]>>().append("foo", new AList<ABean[][][]>().append(new ABean[][][]{{{new ABean().init(),null},null},null}).append(null));
	}

	@RestMethod(name="GET", path="/returnBeanListMapIntegerKeys")
	public Map<Integer,List<ABean>> returnBeanListMapIntegerKeys() {
		return new AMap<Integer,List<ABean>>().append(1,asList(new ABean().init()));
	}

	// Typed beans

	@RestMethod(name="GET", path="/returnTypedBean")
	public TypedBean returnTypedBean() {
		return new TypedBeanImpl().init();
	}

	@RestMethod(name="GET", path="/returnTypedBean3dArray")
	public TypedBean[][][] returnTypedBean3dArray() {
		return new TypedBean[][][]{{{new TypedBeanImpl().init(),null},null},null};
	}

	@RestMethod(name="GET", path="/returnTypedBeanList")
	public List<TypedBean> returnTypedBeanList() {
		return asList((TypedBean)new TypedBeanImpl().init());
	}

	@RestMethod(name="GET", path="/returnTypedBean1d3dList")
	public List<TypedBean[][][]> returnTypedBean1d3dList() {
		return new AList<TypedBean[][][]>().append(new TypedBean[][][]{{{new TypedBeanImpl().init(),null},null},null}).append(null);
	}

	@RestMethod(name="GET", path="/returnTypedBeanMap")
	public Map<String,TypedBean> returnTypedBeanMap() {
		return new AMap<String,TypedBean>().append("foo",new TypedBeanImpl().init());
	}

	@RestMethod(name="GET", path="/returnTypedBeanListMap")
	public Map<String,List<TypedBean>> returnTypedBeanListMap() {
		return new AMap<String,List<TypedBean>>().append("foo",asList((TypedBean)new TypedBeanImpl().init()));
	}

	@RestMethod(name="GET", path="/returnTypedBean1d3dListMap")
	public Map<String,List<TypedBean[][][]>> returnTypedBean1d3dListMap() {
		return new AMap<String,List<TypedBean[][][]>>().append("foo", new AList<TypedBean[][][]>().append(new TypedBean[][][]{{{new TypedBeanImpl().init(),null},null},null}).append(null));
	}

	@RestMethod(name="GET", path="/returnTypedBeanListMapIntegerKeys")
	public Map<Integer,List<TypedBean>> returnTypedBeanListMapIntegerKeys() {
		return new AMap<Integer,List<TypedBean>>().append(1,asList((TypedBean)new TypedBeanImpl().init()));
	}

	// Swapped POJOs

	@RestMethod(name="GET", path="/returnSwappedPojo")
	public SwappedPojo returnSwappedPojo() {
		return new SwappedPojo();
	}

	@RestMethod(name="GET", path="/returnSwappedPojo3dArray")
	public SwappedPojo[][][] returnSwappedPojo3dArray() {
		return new SwappedPojo[][][]{{{new SwappedPojo(),null},null},null};
	}

	@RestMethod(name="GET", path="/returnSwappedPojoMap")
	public Map<SwappedPojo,SwappedPojo> returnSwappedPojoMap() {
		return new AMap<SwappedPojo,SwappedPojo>().append(new SwappedPojo(), new SwappedPojo());
	}

	@RestMethod(name="GET", path="/returnSwappedPojo3dMap")
	public Map<SwappedPojo,SwappedPojo[][][]> returnSwappedPojo3dMap() {
		return new AMap<SwappedPojo,SwappedPojo[][][]>().append(new SwappedPojo(), new SwappedPojo[][][]{{{new SwappedPojo(),null},null},null});
	}

	// Implicit swapped POJOs

	@RestMethod(name="GET", path="/returnImplicitSwappedPojo")
	public ImplicitSwappedPojo returnImplicitSwappedPojo() {
		return new ImplicitSwappedPojo();
	}

	@RestMethod(name="GET", path="/returnImplicitSwappedPojo3dArray")
	public ImplicitSwappedPojo[][][] returnImplicitSwappedPojo3dArray() {
		return new ImplicitSwappedPojo[][][]{{{new ImplicitSwappedPojo(),null},null},null};
	}

	@RestMethod(name="GET", path="/returnImplicitSwappedPojoMap")
	public Map<ImplicitSwappedPojo,ImplicitSwappedPojo> returnImplicitSwappedPojoMap() {
		return new AMap<ImplicitSwappedPojo,ImplicitSwappedPojo>().append(new ImplicitSwappedPojo(), new ImplicitSwappedPojo());
	}

	@RestMethod(name="GET", path="/returnImplicitSwappedPojo3dMap")
	public Map<ImplicitSwappedPojo,ImplicitSwappedPojo[][][]> returnImplicitSwappedPojo3dMap() {
		return new AMap<ImplicitSwappedPojo,ImplicitSwappedPojo[][][]>().append(new ImplicitSwappedPojo(), new ImplicitSwappedPojo[][][]{{{new ImplicitSwappedPojo(),null},null},null});
	}

	// Enums

	@RestMethod(name="GET", path="/returnEnum")
	public TestEnum returnEnum() {
		return TestEnum.TWO;
	}

	@RestMethod(name="GET", path="/returnEnum3d")
	public TestEnum[][][] returnEnum3d() {
		return new TestEnum[][][]{{{TestEnum.TWO,null},null},null};
	}

	@RestMethod(name="GET", path="/returnEnumList")
	public List<TestEnum> returnEnumList() {
		return new AList<TestEnum>().append(TestEnum.TWO).append(null);
	}

	@RestMethod(name="GET", path="/returnEnum3dList")
	public List<List<List<TestEnum>>> returnEnum3dList() {
		return new AList<List<List<TestEnum>>>()
		.append(
			new AList<List<TestEnum>>()
			.append(
				new AList<TestEnum>().append(TestEnum.TWO).append(null)
			)
			.append(null)
		.append(null)
		);
	}

	@RestMethod(name="GET", path="/returnEnum1d3dList")
	public List<TestEnum[][][]> returnEnum1d3dList() {
		return new AList<TestEnum[][][]>().append(new TestEnum[][][]{{{TestEnum.TWO,null},null},null}).append(null);
	}

	@RestMethod(name="GET", path="/returnEnumMap")
	public Map<TestEnum,TestEnum> returnEnumMap() {
		return new AMap<TestEnum,TestEnum>().append(TestEnum.ONE,TestEnum.TWO);
	}

	@RestMethod(name="GET", path="/returnEnum3dArrayMap")
	public Map<TestEnum,TestEnum[][][]> returnEnum3dArrayMap() {
		return new AMap<TestEnum,TestEnum[][][]>().append(TestEnum.ONE, new TestEnum[][][]{{{TestEnum.TWO,null},null},null});
	}

	@RestMethod(name="GET", path="/returnEnum1d3dListMap")
	public Map<TestEnum,List<TestEnum[][][]>> returnEnum1d3dListMap() {
		return new AMap<TestEnum,List<TestEnum[][][]>>().append(TestEnum.ONE, new AList<TestEnum[][][]>().append(new TestEnum[][][]{{{TestEnum.TWO,null},null},null}).append(null));
	}

	//--------------------------------------------------------------------------------
	// Test parameters
	//--------------------------------------------------------------------------------

	// Various primitives

	@RestMethod(name="POST", path="/setInt")
	public void setInt(@Body int x) {
		assertEquals(1, x);
	}

	@RestMethod(name="POST", path="/setInteger")
	public void setInteger(@Body Integer x) {
		assertEquals((Integer)1, x);
	}

	@RestMethod(name="POST", path="/setBoolean")
	public void setBoolean(@Body boolean x) {
		assertTrue(x);
	}

	@RestMethod(name="POST", path="/setFloat")
	public void setFloat(@Body float x) {
		assertTrue(1f == x);
	}

	@RestMethod(name="POST", path="/setFloatObject")
	public void setFloatObject(@Body Float x) {
		assertTrue(1f == x);
	}

	@RestMethod(name="POST", path="/setString")
	public void setString(@Body String x) {
		assertEquals("foo", x);
	}

	@RestMethod(name="POST", path="/setNullString")
	public void setNullString(@Body String x) {
		assertNull(x);
	}

	@RestMethod(name="POST", path="/setInt3dArray")
	public void setInt3dArray(@Body int[][][] x) {
		assertObjectEquals("[[[1,2],null],null]", x);
	}

	@RestMethod(name="POST", path="/setInteger3dArray")
	public void setInteger3dArray(@Body Integer[][][] x) {
		assertObjectEquals("[[[1,null],null],null]", x);
	}

	@RestMethod(name="POST", path="/setString3dArray")
	public void setString3dArray(@Body String[][][] x) {
		assertObjectEquals("[[['foo',null],null],null]", x);
	}

	@RestMethod(name="POST", path="/setIntegerList")
	public void setIntegerList(@Body List<Integer> x) {
		assertObjectEquals("[1,null]", x);
		assertClass(Integer.class, x.get(0));
	}

	@RestMethod(name="POST", path="/setInteger3dList")
	public void setInteger3dList(@Body List<List<List<Integer>>> x) {
		assertObjectEquals("[[[1,null],null],null]", x);
		assertClass(Integer.class, x.get(0).get(0).get(0));
	}

	@RestMethod(name="POST", path="/setInteger1d3dList")
	public void setInteger1d3dList(@Body List<Integer[][][]> x) {
		assertObjectEquals("[[[[1,null],null],null],null]", x);
		assertClass(Integer[][][].class, x.get(0));
		assertClass(Integer.class, x.get(0)[0][0][0]);
	}

	@RestMethod(name="POST", path="/setInt1d3dList")
	public void setInt1d3dList(@Body List<int[][][]> x) {
		assertObjectEquals("[[[[1,2],null],null],null]", x);
		assertClass(int[][][].class, x.get(0));
	}

	@RestMethod(name="POST", path="/setStringList")
	public void setStringList(@Body List<String> x) {
		assertObjectEquals("['foo','bar',null]", x);
	}

	// Beans

	@RestMethod(name="POST", path="/setBean")
	public void setBean(@Body ABean x) {
		assertObjectEquals("{a:1,b:'foo'}", x);
	}

	@RestMethod(name="POST", path="/setBean3dArray")
	public void setBean3dArray(@Body ABean[][][] x) {
		assertObjectEquals("[[[{a:1,b:'foo'},null],null],null]", x);
	}

	@RestMethod(name="POST", path="/setBeanList")
	public void setBeanList(@Body List<ABean> x) {
		assertObjectEquals("[{a:1,b:'foo'}]", x);
	}

	@RestMethod(name="POST", path="/setBean1d3dList")
	public void setBean1d3dList(@Body List<ABean[][][]> x) {
		assertObjectEquals("[[[[{a:1,b:'foo'},null],null],null],null]", x);
	}

	@RestMethod(name="POST", path="/setBeanMap")
	public void setBeanMap(@Body Map<String,ABean> x) {
		assertObjectEquals("{foo:{a:1,b:'foo'}}", x);
	}

	@RestMethod(name="POST", path="/setBeanListMap")
	public void setBeanListMap(@Body Map<String,List<ABean>> x) {
		assertObjectEquals("{foo:[{a:1,b:'foo'}]}", x);
	}

	@RestMethod(name="POST", path="/setBean1d3dListMap")
	public void setBean1d3dListMap(@Body Map<String,List<ABean[][][]>> x) {
		assertObjectEquals("{foo:[[[[{a:1,b:'foo'},null],null],null],null]}", x);
	}

	@RestMethod(name="POST", path="/setBeanListMapIntegerKeys")
	public void setBeanListMapIntegerKeys(@Body Map<Integer,List<ABean>> x) {
		assertObjectEquals("{'1':[{a:1,b:'foo'}]}", x);  // Note: JsonSerializer serializes key as string.
		assertClass(Integer.class, x.keySet().iterator().next());
	}

	// Typed beans

	@RestMethod(name="POST", path="/setTypedBean")
	public void setTypedBean(@Body TypedBean x) {
		assertObjectEquals("{_type:'TypedBeanImpl',a:1,b:'foo'}", x);
		assertClass(TypedBeanImpl.class, x);
	}

	@RestMethod(name="POST", path="/setTypedBean3dArray")
	public void setTypedBean3dArray(@Body TypedBean[][][] x) {
		assertObjectEquals("[[[{_type:'TypedBeanImpl',a:1,b:'foo'},null],null],null]", x);
		assertClass(TypedBeanImpl.class, x[0][0][0]);
	}

	@RestMethod(name="POST", path="/setTypedBeanList")
	public void setTypedBeanList(@Body List<TypedBean> x) {
		assertObjectEquals("[{_type:'TypedBeanImpl',a:1,b:'foo'}]", x);
		assertClass(TypedBeanImpl.class, x.get(0));
	}

	@RestMethod(name="POST", path="/setTypedBean1d3dList")
	public void setTypedBean1d3dList(@Body List<TypedBean[][][]> x) {
		assertObjectEquals("[[[[{_type:'TypedBeanImpl',a:1,b:'foo'},null],null],null],null]", x);
		assertClass(TypedBeanImpl.class, x.get(0)[0][0][0]);
	}

	@RestMethod(name="POST", path="/setTypedBeanMap")
	public void setTypedBeanMap(@Body Map<String,TypedBean> x) {
		assertObjectEquals("{foo:{_type:'TypedBeanImpl',a:1,b:'foo'}}", x);
		assertClass(TypedBeanImpl.class, x.get("foo"));
	}

	@RestMethod(name="POST", path="/setTypedBeanListMap")
	public void setTypedBeanListMap(@Body Map<String,List<TypedBean>> x) {
		assertObjectEquals("{foo:[{_type:'TypedBeanImpl',a:1,b:'foo'}]}", x);
		assertClass(TypedBeanImpl.class, x.get("foo").get(0));
	}

	@RestMethod(name="POST", path="/setTypedBean1d3dListMap")
	public void setTypedBean1d3dListMap(@Body Map<String,List<TypedBean[][][]>> x) {
		assertObjectEquals("{foo:[[[[{_type:'TypedBeanImpl',a:1,b:'foo'},null],null],null],null]}", x);
		assertClass(TypedBeanImpl.class, x.get("foo").get(0)[0][0][0]);
	}

	@RestMethod(name="POST", path="/setTypedBeanListMapIntegerKeys")
	public void setTypedBeanListMapIntegerKeys(@Body Map<Integer,List<TypedBean>> x) {
		assertObjectEquals("{'1':[{_type:'TypedBeanImpl',a:1,b:'foo'}]}", x);  // Note: JsonSerializer serializes key as string.
		assertClass(TypedBeanImpl.class, x.get(1).get(0));
	}

	// Swapped POJOs

	@RestMethod(name="POST", path="/setSwappedPojo")
	public void setSwappedPojo(@Body SwappedPojo x) {
		assertTrue(x.wasUnswapped);
	}

	@RestMethod(name="POST", path="/setSwappedPojo3dArray")
	public void setSwappedPojo3dArray(@Body SwappedPojo[][][] x) {
		assertObjectEquals("[[['"+SWAP+"',null],null],null]", x);
		assertTrue(x[0][0][0].wasUnswapped);
	}

	@RestMethod(name="POST", path="/setSwappedPojoMap")
	public void setSwappedPojoMap(@Body Map<SwappedPojo,SwappedPojo> x) {
		assertObjectEquals("{'"+SWAP+"':'"+SWAP+"'}", x);
		Map.Entry<SwappedPojo,SwappedPojo> e = x.entrySet().iterator().next();
		assertTrue(e.getKey().wasUnswapped);
		assertTrue(e.getValue().wasUnswapped);
	}

	@RestMethod(name="POST", path="/setSwappedPojo3dMap")
	public void setSwappedPojo3dMap(@Body Map<SwappedPojo,SwappedPojo[][][]> x) {
		assertObjectEquals("{'"+SWAP+"':[[['"+SWAP+"',null],null],null]}", x);
		Map.Entry<SwappedPojo,SwappedPojo[][][]> e = x.entrySet().iterator().next();
		assertTrue(e.getKey().wasUnswapped);
		assertTrue(e.getValue()[0][0][0].wasUnswapped);
	}

	// Implicit swapped POJOs

	@RestMethod(name="POST", path="/setImplicitSwappedPojo")
	public void setImplicitSwappedPojo(@Body ImplicitSwappedPojo x) {
		assertTrue(x.wasUnswapped);
	}

	@RestMethod(name="POST", path="/setImplicitSwappedPojo3dArray")
	public void setImplicitSwappedPojo3dArray(@Body ImplicitSwappedPojo[][][] x) {
		assertObjectEquals("[[['"+SWAP+"',null],null],null]", x);
		assertTrue(x[0][0][0].wasUnswapped);
	}

	@RestMethod(name="POST", path="/setImplicitSwappedPojoMap")
	public void setImplicitSwappedPojoMap(@Body Map<ImplicitSwappedPojo,ImplicitSwappedPojo> x) {
		assertObjectEquals("{'"+SWAP+"':'"+SWAP+"'}", x);
		Map.Entry<ImplicitSwappedPojo,ImplicitSwappedPojo> e = x.entrySet().iterator().next();
		assertTrue(e.getKey().wasUnswapped);
		assertTrue(e.getValue().wasUnswapped);
	}

	@RestMethod(name="POST", path="/setImplicitSwappedPojo3dMap")
	public void setImplicitSwappedPojo3dMap(@Body Map<ImplicitSwappedPojo,ImplicitSwappedPojo[][][]> x) {
		assertObjectEquals("{'"+SWAP+"':[[['"+SWAP+"',null],null],null]}", x);
		Map.Entry<ImplicitSwappedPojo,ImplicitSwappedPojo[][][]> e = x.entrySet().iterator().next();
		assertTrue(e.getKey().wasUnswapped);
		assertTrue(e.getValue()[0][0][0].wasUnswapped);
	}

	// Enums

	@RestMethod(name="POST", path="/setEnum")
	public void setEnum(@Body TestEnum x) {
		assertEquals(TestEnum.TWO, x);
	}

	@RestMethod(name="POST", path="/setEnum3d")
	public void setEnum3d(@Body TestEnum[][][] x) {
		assertObjectEquals("[[['TWO',null],null],null]", x);
	}

	@RestMethod(name="POST", path="/setEnumList")
	public void setEnumList(@Body List<TestEnum> x) {
		assertObjectEquals("['TWO',null]", x);
		assertClass(TestEnum.class, x.get(0));
	}

	@RestMethod(name="POST", path="/setEnum3dList")
	public void setEnum3dList(@Body List<List<List<TestEnum>>> x) {
		assertObjectEquals("[[['TWO',null],null,null]]", x);
		assertClass(TestEnum.class, x.get(0).get(0).get(0));
	}

	@RestMethod(name="POST", path="/setEnum1d3dList")
	public void setEnum1d3dList(@Body List<TestEnum[][][]> x) {
		assertObjectEquals("[[[['TWO',null],null],null],null]", x);
		assertClass(TestEnum[][][].class, x.get(0));
	}

	@RestMethod(name="POST", path="/setEnumMap")
	public void setEnumMap(@Body Map<TestEnum,TestEnum> x) {
		assertObjectEquals("{ONE:'TWO'}", x);
		Map.Entry<TestEnum,TestEnum> e = x.entrySet().iterator().next();
		assertClass(TestEnum.class, e.getKey());
		assertClass(TestEnum.class, e.getValue());
	}

	@RestMethod(name="POST", path="/setEnum3dArrayMap")
	public void setEnum3dArrayMap(@Body Map<TestEnum,TestEnum[][][]> x) {
		assertObjectEquals("{ONE:[[['TWO',null],null],null]}", x);
		Map.Entry<TestEnum,TestEnum[][][]> e = x.entrySet().iterator().next();
		assertClass(TestEnum.class, e.getKey());
		assertClass(TestEnum[][][].class, e.getValue());
	}

	@RestMethod(name="POST", path="/setEnum1d3dListMap")
	public void setEnum1d3dListMap(@Body Map<TestEnum,List<TestEnum[][][]>> x) {
		assertObjectEquals("{ONE:[[[['TWO',null],null],null],null]}", x);
		Map.Entry<TestEnum,List<TestEnum[][][]>> e = x.entrySet().iterator().next();
		assertClass(TestEnum.class, e.getKey());
		assertClass(TestEnum[][][].class, e.getValue().get(0));
	}
}
