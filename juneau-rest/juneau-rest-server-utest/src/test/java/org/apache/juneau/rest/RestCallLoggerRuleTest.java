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

import java.util.logging.*;

import org.apache.juneau.json.*;
import org.apache.juneau.parser.*;
import org.junit.*;
import org.junit.runners.*;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class RestCallLoggerRuleTest {

	static final Throwable T1 = new IndexOutOfBoundsException();
	static final Throwable T2 = new NoSuchMethodError();

	//------------------------------------------------------------------------------------------------------------------
	// Status code matching
	//------------------------------------------------------------------------------------------------------------------

	@Test
	public void a01_matchingCodes_ignoreOtherFields() {
		RestCallLoggerRule r = RestCallLoggerRule.create().codes("200").build();
		assertTrue(r.matches(200, true, null));
		assertTrue(r.matches(200, false, null));
		assertTrue(r.matches(200, true, T1));
		assertFalse(r.matches(201, true, null));
		assertFalse(r.matches(199, true, null));
		assertFalse(r.matches(201, false, null));
		assertFalse(r.matches(199, false, null));
	}

	@Test
	public void a02_matchingCodes_singleValue() {
		RestCallLoggerRule r = RestCallLoggerRule.create().codes("200").build();
		assertTrue(r.matches(200, true, null));
		assertFalse(r.matches(201, true, null));
		assertFalse(r.matches(199, true, null));
	}

	@Test
	public void a03_matchingCodes_range() {
		RestCallLoggerRule r = RestCallLoggerRule.create().codes("200-299").build();
		assertTrue(r.matches(200, true, null));
		assertTrue(r.matches(201, true, null));
		assertTrue(r.matches(299, true, null));
		assertFalse(r.matches(199, true, null));
		assertFalse(r.matches(300, true, null));
	}

	@Test
	public void a04_matchingCodes_openEnded() {
		RestCallLoggerRule r = RestCallLoggerRule.create().codes(">=200").build();
		assertTrue(r.matches(200, true, null));
		assertTrue(r.matches(201, true, null));
		assertTrue(r.matches(299, true, null));
		assertTrue(r.matches(300, true, null));
		assertFalse(r.matches(199, true, null));
	}

	@Test
	public void a05_matchingCodes_matchAll() {
		RestCallLoggerRule r = RestCallLoggerRule.create().codes("*").build();
		assertTrue(r.matches(200, true, null));
	}

	@Test
	public void a06_matchingCodes_null() {
		RestCallLoggerRule r = RestCallLoggerRule.create().codes(null).build();
		assertTrue(r.matches(200, true, null));
	}

	@Test
	public void a07_matchingCodes_empty() {
		RestCallLoggerRule r = RestCallLoggerRule.create().codes("").build();
		assertTrue(r.matches(200, true, null));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Exception matching
	//------------------------------------------------------------------------------------------------------------------

	@Test
	public void b01_matchingException_ignoreOtherFields() {
		RestCallLoggerRule r = RestCallLoggerRule.create().exceptions("IndexOutOfBoundsException").build();
		assertTrue(r.matches(200, true, T1));
		assertTrue(r.matches(200, false, T1));
		assertTrue(r.matches(200, true, T1));
		assertFalse(r.matches(201, true, null));
		assertFalse(r.matches(199, true, null));
		assertFalse(r.matches(201, false, null));
		assertFalse(r.matches(199, false, null));
	}

	@Test
	public void b02_matchingException_simpleClassName() {
		RestCallLoggerRule r = RestCallLoggerRule.create().exceptions("IndexOutOfBoundsException").build();
		assertTrue(r.matches(200, true, T1));
		assertFalse(r.matches(200, true, T2));
	}

	@Test
	public void b03_matchingException_fullClassName() {
		RestCallLoggerRule r = RestCallLoggerRule.create().exceptions("java.lang.IndexOutOfBoundsException").build();
		assertTrue(r.matches(200, true, T1));
		assertFalse(r.matches(200, true, T2));
	}

	@Test
	public void b04_matchingException_simpleClassName_pattern() {
		RestCallLoggerRule r = RestCallLoggerRule.create().exceptions("IndexOutOfBounds*").build();
		assertTrue(r.matches(200, true, T1));
		assertFalse(r.matches(200, true, T2));
	}

	@Test
	public void b05_matchingException_fullClassName_pattern() {
		RestCallLoggerRule r = RestCallLoggerRule.create().exceptions("java.lang.IndexOutOfBounds*").build();
		assertTrue(r.matches(200, true, T1));
		assertFalse(r.matches(200, true, T2));
	}

	@Test
	public void b06_matchingException_null() {
		RestCallLoggerRule r = RestCallLoggerRule.create().exceptions(null).build();
		assertTrue(r.matches(200, true, null));
		assertTrue(r.matches(201, false, T1));
	}

	@Test
	public void b07_matchingException_empty() {
		RestCallLoggerRule r = RestCallLoggerRule.create().exceptions("").build();
		assertTrue(r.matches(200, true, null));
		assertTrue(r.matches(201, false, T1));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Debug-only matching
	//------------------------------------------------------------------------------------------------------------------

	@Test
	public void c01_debugOnly_true() {
		RestCallLoggerRule r = RestCallLoggerRule.create().debugOnly().build();
		assertTrue(r.matches(200, true, T1));
		assertFalse(r.matches(200, false, T1));
	}

	@Test
	public void c02_debugOnly_false() {
		RestCallLoggerRule r = RestCallLoggerRule.create().debugOnly(false).build();
		assertTrue(r.matches(200, true, T1));
		assertTrue(r.matches(200, false, T1));
	}

	@Test
	public void c03_debugOnly_null() {
		RestCallLoggerRule r = RestCallLoggerRule.create().debugOnly(null).build();
		assertTrue(r.matches(200, true, T1));
		assertTrue(r.matches(200, false, T1));
	}

	@Test
	public void c04_debugOnly_default() {
		RestCallLoggerRule r = RestCallLoggerRule.create().debugOnly(null).build();
		assertTrue(r.matches(200, true, T1));
		assertTrue(r.matches(200, false, T1));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Level
	//------------------------------------------------------------------------------------------------------------------

	@Test
	public void d01_level() {
		RestCallLoggerRule r = RestCallLoggerRule.create().level(Level.WARNING).build();
		assertEquals(Level.WARNING, r.getLevel());
	}

	@Test
	public void d02_level_null() {
		RestCallLoggerRule r = RestCallLoggerRule.create().level(null).build();
		assertNull(r.getLevel());
	}

	@Test
	public void d03_level_default() {
		RestCallLoggerRule r = RestCallLoggerRule.create().build();
		assertNull(r.getLevel());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Request detail
	//------------------------------------------------------------------------------------------------------------------

	@Test
	public void e01_reqDetail_small() {
		RestCallLoggerRule r = RestCallLoggerRule.create().req(RestCallLoggingDetail.SHORT).build();
		assertEquals(RestCallLoggingDetail.SHORT, r.getReqDetail());
	}

	@Test
	public void e02_reqDetail_medium() {
		RestCallLoggerRule r = RestCallLoggerRule.create().req(RestCallLoggingDetail.MEDIUM).build();
		assertEquals(RestCallLoggingDetail.MEDIUM, r.getReqDetail());
	}

	@Test
	public void e03_reqDetail_large() {
		RestCallLoggerRule r = RestCallLoggerRule.create().req(RestCallLoggingDetail.LONG).build();
		assertEquals(RestCallLoggingDetail.LONG, r.getReqDetail());
	}

	@Test
	public void e05_reqDetail_null() {
		RestCallLoggerRule r = RestCallLoggerRule.create().req(null).build();
		assertEquals(RestCallLoggingDetail.SHORT, r.getReqDetail());
	}

	@Test
	public void e06_reqDetail_default() {
		RestCallLoggerRule r = RestCallLoggerRule.create().build();
		assertEquals(RestCallLoggingDetail.SHORT, r.getReqDetail());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Response detail
	//------------------------------------------------------------------------------------------------------------------

	@Test
	public void f01_resDetail_small() {
		RestCallLoggerRule r = RestCallLoggerRule.create().res(RestCallLoggingDetail.SHORT).build();
		assertEquals(RestCallLoggingDetail.SHORT, r.getResDetail());
	}

	@Test
	public void f02_resDetail_medium() {
		RestCallLoggerRule r = RestCallLoggerRule.create().res(RestCallLoggingDetail.MEDIUM).build();
		assertEquals(RestCallLoggingDetail.MEDIUM, r.getResDetail());
	}

	@Test
	public void f03_resDetail_large() {
		RestCallLoggerRule r = RestCallLoggerRule.create().res(RestCallLoggingDetail.LONG).build();
		assertEquals(RestCallLoggingDetail.LONG, r.getResDetail());
	}

	@Test
	public void f05_resDetail_null() {
		RestCallLoggerRule r = RestCallLoggerRule.create().res(null).build();
		assertEquals(RestCallLoggingDetail.SHORT, r.getResDetail());
	}

	@Test
	public void f06_resDetail_default() {
		RestCallLoggerRule r = RestCallLoggerRule.create().build();
		assertEquals(RestCallLoggingDetail.SHORT, r.getResDetail());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Verbose
	//------------------------------------------------------------------------------------------------------------------

	@Test
	public void f01_verbose_true() {
		RestCallLoggerRule r = RestCallLoggerRule.create().verbose().build();
		assertEquals(RestCallLoggingDetail.LONG, r.getReqDetail());
		assertEquals(RestCallLoggingDetail.LONG, r.getResDetail());
	}

	@Test
	public void f02_verbose_false() {
		RestCallLoggerRule r = RestCallLoggerRule.create().verbose(false).build();
		assertEquals(RestCallLoggingDetail.SHORT, r.getReqDetail());
		assertEquals(RestCallLoggingDetail.SHORT, r.getResDetail());
	}

	@Test
	public void f03_verbose_null() {
		RestCallLoggerRule r = RestCallLoggerRule.create().verbose(null).build();
		assertEquals(RestCallLoggingDetail.SHORT, r.getReqDetail());
		assertEquals(RestCallLoggingDetail.SHORT, r.getResDetail());
	}

	@Test
	public void f04_verbose_true_override() {
		RestCallLoggerRule r = RestCallLoggerRule.create().verbose(true).req(RestCallLoggingDetail.SHORT).res(RestCallLoggingDetail.SHORT).build();
		assertEquals(RestCallLoggingDetail.LONG, r.getReqDetail());
		assertEquals(RestCallLoggingDetail.LONG, r.getResDetail());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Bean instantiation
	//------------------------------------------------------------------------------------------------------------------

	@Test
	public void g01_beanInstantiation_defaultValues() throws ParseException {
		RestCallLoggerRule r = JsonParser.DEFAULT.parse("{}", RestCallLoggerRule.class);
		assertEquals("{matchAll:true,req:'SHORT',res:'SHORT'}", r.toString());
	}

	@Test
	public void g02_beanInstantiation_allValues() throws ParseException {
		RestCallLoggerRule r = JsonParser.DEFAULT.parse("{codes:'100-200',exceptions:'Foo*',level:'WARNING',req:'LONG',res:'LONG',debugOnly:'true'}", RestCallLoggerRule.class);
		assertEquals("{codes:'100-200',exceptions:'Foo*',debugOnly:true,level:'WARNING',req:'LONG',res:'LONG'}", r.toString());
	}
}
