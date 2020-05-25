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
package org.apache.juneau.utils;

import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.*;
import static org.apache.juneau.internal.HttpUtils.*;

import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class HttpUtilsTest {

	public interface A {
		void doGet();
		void doGET();
		void doPOST();
		void doFOO();
		void getFoo();
		void postFoo();
		void get();
		void post();
		void createFoo();
	}

	//====================================================================================================
	// getHttpMethod()
	//====================================================================================================

	@Test
	public void testGetHttpMethod_detect_nodefault() throws Exception {
		assertEquals("GET", detectHttpMethod(A.class.getMethod("doGet"), true, null));
		assertEquals("GET", detectHttpMethod(A.class.getMethod("doGET"), true, null));
		assertEquals("POST", detectHttpMethod(A.class.getMethod("doPOST"), true, null));
		assertEquals(null, detectHttpMethod(A.class.getMethod("doFOO"), true, null));
		assertEquals("GET", detectHttpMethod(A.class.getMethod("getFoo"), true, null));
		assertEquals("POST", detectHttpMethod(A.class.getMethod("postFoo"), true, null));
		assertEquals("GET", detectHttpMethod(A.class.getMethod("get"), true, null));
		assertEquals("POST", detectHttpMethod(A.class.getMethod("post"), true, null));
		assertEquals(null, detectHttpMethod(A.class.getMethod("createFoo"), true, null));
	}

	@Test
	public void testGetHttpMethod_detect_default() throws Exception {
		assertEquals("GET", detectHttpMethod(A.class.getMethod("doGet"), true, "DELETE"));
		assertEquals("GET", detectHttpMethod(A.class.getMethod("doGET"), true, "DELETE"));
		assertEquals("POST", detectHttpMethod(A.class.getMethod("doPOST"), true, "DELETE"));
		assertEquals("DELETE", detectHttpMethod(A.class.getMethod("doFOO"), true, "DELETE"));
		assertEquals("GET", detectHttpMethod(A.class.getMethod("getFoo"), true, "DELETE"));
		assertEquals("POST", detectHttpMethod(A.class.getMethod("postFoo"), true, "DELETE"));
		assertEquals("GET", detectHttpMethod(A.class.getMethod("get"), true, "DELETE"));
		assertEquals("POST", detectHttpMethod(A.class.getMethod("post"), true, "DELETE"));
		assertEquals("DELETE", detectHttpMethod(A.class.getMethod("createFoo"), true, "DELETE"));
	}

	@Test
	public void testGetHttpMethod_nodetect_nodefault() throws Exception {
		assertEquals(null, detectHttpMethod(A.class.getMethod("doGet"), false, null));
		assertEquals(null, detectHttpMethod(A.class.getMethod("doGET"), false, null));
		assertEquals(null, detectHttpMethod(A.class.getMethod("doPOST"), false, null));
		assertEquals(null, detectHttpMethod(A.class.getMethod("doFOO"), false, null));
		assertEquals(null, detectHttpMethod(A.class.getMethod("getFoo"), false, null));
		assertEquals(null, detectHttpMethod(A.class.getMethod("postFoo"), false, null));
		assertEquals(null, detectHttpMethod(A.class.getMethod("get"), false, null));
		assertEquals(null, detectHttpMethod(A.class.getMethod("post"), false, null));
		assertEquals(null, detectHttpMethod(A.class.getMethod("createFoo"), false, null));
	}

	@Test
	public void testGetHttpMethod_nodetect_default() throws Exception {
		assertEquals("DELETE", detectHttpMethod(A.class.getMethod("doGet"), false, "DELETE"));
		assertEquals("DELETE", detectHttpMethod(A.class.getMethod("doGET"), false, "DELETE"));
		assertEquals("DELETE", detectHttpMethod(A.class.getMethod("doPOST"), false, "DELETE"));
		assertEquals("DELETE", detectHttpMethod(A.class.getMethod("doFOO"), false, "DELETE"));
		assertEquals("DELETE", detectHttpMethod(A.class.getMethod("getFoo"), false, "DELETE"));
		assertEquals("DELETE", detectHttpMethod(A.class.getMethod("postFoo"), false, "DELETE"));
		assertEquals("DELETE", detectHttpMethod(A.class.getMethod("get"), false, "DELETE"));
		assertEquals("DELETE", detectHttpMethod(A.class.getMethod("post"), false, "DELETE"));
		assertEquals("DELETE", detectHttpMethod(A.class.getMethod("createFoo"), false, "DELETE"));
	}

	//====================================================================================================
	// getRestPath()
	//====================================================================================================

	public interface B1 {
		void doGet();
		void doGET();
		void doPOST();
		void doFOO();
		void getFoo();
		void postFoo();
		void get();
		void post();
		void createFoo();
	}

	public interface B2 {
		void doGet(int x, A y);
		void doGET(int x, A y);
		void doPOST(int x, A y);
		void doFOO(int x, A y);
		void getFoo(int x, A y);
		void postFoo(int x, A y);
		void get(int x, A y);
		void post(int x, A y);
		void createFoo(int x, A y);
	}

	@Test
	public void testGetHttpPath_noargs_detect() throws Exception {
		assertEquals("/", detectHttpPath(B1.class.getMethod("doGet"), true));
		assertEquals("/", detectHttpPath(B1.class.getMethod("doGET"), true));
		assertEquals("/", detectHttpPath(B1.class.getMethod("doPOST"), true));
		assertEquals("/doFOO", detectHttpPath(B1.class.getMethod("doFOO"), true));
		assertEquals("/foo", detectHttpPath(B1.class.getMethod("getFoo"), true));
		assertEquals("/foo", detectHttpPath(B1.class.getMethod("postFoo"), true));
		assertEquals("/", detectHttpPath(B1.class.getMethod("get"), true));
		assertEquals("/", detectHttpPath(B1.class.getMethod("post"), true));
		assertEquals("/createFoo", detectHttpPath(B1.class.getMethod("createFoo"), true));
	}

	@Test
	public void testGetHttpPath_noargs_nodetect() throws Exception {
		assertEquals("/doGet", detectHttpPath(B1.class.getMethod("doGet"), false));
		assertEquals("/doGET", detectHttpPath(B1.class.getMethod("doGET"), false));
		assertEquals("/doPOST", detectHttpPath(B1.class.getMethod("doPOST"), false));
		assertEquals("/doFOO", detectHttpPath(B1.class.getMethod("doFOO"), false));
		assertEquals("/getFoo", detectHttpPath(B1.class.getMethod("getFoo"), false));
		assertEquals("/postFoo", detectHttpPath(B1.class.getMethod("postFoo"), false));
		assertEquals("/get", detectHttpPath(B1.class.getMethod("get"), false));
		assertEquals("/post", detectHttpPath(B1.class.getMethod("post"), false));
		assertEquals("/createFoo", detectHttpPath(B1.class.getMethod("createFoo"), false));
	}

	@Test
	public void testGetHttpPath_args_detect() throws Exception {
		assertEquals("/", detectHttpPath(B2.class.getMethod("doGet",int.class,A.class), true));
		assertEquals("/", detectHttpPath(B2.class.getMethod("doGET",int.class,A.class), true));
		assertEquals("/", detectHttpPath(B2.class.getMethod("doPOST",int.class,A.class), true));
		assertEquals("/doFOO", detectHttpPath(B2.class.getMethod("doFOO",int.class,A.class), true));
		assertEquals("/foo", detectHttpPath(B2.class.getMethod("getFoo",int.class,A.class), true));
		assertEquals("/foo", detectHttpPath(B2.class.getMethod("postFoo",int.class,A.class), true));
		assertEquals("/", detectHttpPath(B2.class.getMethod("get",int.class,A.class), true));
		assertEquals("/", detectHttpPath(B2.class.getMethod("post",int.class,A.class), true));
		assertEquals("/createFoo", detectHttpPath(B2.class.getMethod("createFoo",int.class,A.class), true));
	}

	@Test
	public void testGetHttpPath_args_nodetect() throws Exception {
		assertEquals("/doGet", detectHttpPath(B2.class.getMethod("doGet",int.class,A.class), false));
		assertEquals("/doGET", detectHttpPath(B2.class.getMethod("doGET",int.class,A.class), false));
		assertEquals("/doPOST", detectHttpPath(B2.class.getMethod("doPOST",int.class,A.class), false));
		assertEquals("/doFOO", detectHttpPath(B2.class.getMethod("doFOO",int.class,A.class), false));
		assertEquals("/getFoo", detectHttpPath(B2.class.getMethod("getFoo",int.class,A.class), false));
		assertEquals("/postFoo", detectHttpPath(B2.class.getMethod("postFoo",int.class,A.class), false));
		assertEquals("/get", detectHttpPath(B2.class.getMethod("get",int.class,A.class), false));
		assertEquals("/post", detectHttpPath(B2.class.getMethod("post",int.class,A.class), false));
		assertEquals("/createFoo", detectHttpPath(B2.class.getMethod("createFoo",int.class,A.class), false));
	}
}
