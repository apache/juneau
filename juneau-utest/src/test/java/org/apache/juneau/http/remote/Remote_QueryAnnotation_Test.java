/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.http.remote;

import static org.apache.juneau.TestUtils.*;
import static org.apache.juneau.common.utils.CollectionUtils.*;
import static org.apache.juneau.http.HttpParts.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.math.*;
import java.util.*;
import java.util.concurrent.atomic.*;

import org.apache.http.*;
import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.http.part.*;
import org.apache.juneau.rest.RestRequest;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.client.*;
import org.apache.juneau.rest.mock.*;
import org.apache.juneau.uon.*;
import org.apache.juneau.utest.utils.*;
import org.junit.jupiter.api.*;

class Remote_QueryAnnotation_Test extends TestBase {

	public static class Bean {
		public int f;

		public static Bean create() {
			var b = new Bean();
			b.f = 1;
			return b;
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Basic tests
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class A {
		@RestGet
		public String a(@Query("*") JsonMap m) {
			return m.toString();
		}
	}

	@Remote
	public interface A1 {
		@RemoteOp(path="a") String getX1(@Query("x") int b);
		@RemoteOp(path="a") String getX2(@Query("x") float b);
		@RemoteOp(path="a") String getX3(@Query("x") Bean b);
		@RemoteOp(path="a") String getX4(@Query("*") Bean b);
		@RemoteOp(path="a") String getX5(@Query Bean b);
		@RemoteOp(path="a") String getX6(@Query("x") Bean[] b);
		@RemoteOp(path="a") String getX7(@Query("x") @Schema(cf="uon") Bean[] b);
		@RemoteOp(path="a") String getX8(@Query("x") List<Bean> b);
		@RemoteOp(path="a") String getX9(@Query("x") @Schema(cf="uon") List<Bean> b);
		@RemoteOp(path="a") String getX10(@Query("x") Map<String,Bean> b);
		@RemoteOp(path="a") String getX11(@Query("*") Map<String,Bean> b);
		@RemoteOp(path="a") String getX12(@Query Map<String,Bean> b);
		@RemoteOp(path="a") String getX13(@Query("x") @Schema(cf="uon") Map<String,Bean> b);
		@RemoteOp(path="a") String getX14(@Query() @Schema(f="uon") Map<String,Bean> b);
		@RemoteOp(path="a") String getX15(@Query("*") Reader b);
		@RemoteOp(path="a") String getX16(@Query Reader b);
		@RemoteOp(path="a") String getX17(@Query("*") InputStream b);
		@RemoteOp(path="a") String getX18(@Query InputStream b);
		@RemoteOp(path="a") String getX19(@Query("*") PartList b);
		@RemoteOp(path="a") String getX20(@Query PartList b);
		@RemoteOp(path="a") String getX21(@Query NameValuePair b);
		@RemoteOp(path="a") String getX22(@Query NameValuePair[] b);
		@RemoteOp(path="a") String getX24(@Query String b);
		@RemoteOp(path="a") String getX25(@Query List<NameValuePair> b);
	}

	@Test void a01_objectTypes() {
		var x = remote(A.class,A1.class);
		assertEquals("{x:'1'}",x.getX1(1));
		assertEquals("{x:'1.0'}",x.getX2(1));
		assertEquals("{x:'f=1'}",x.getX3(Bean.create()));
		assertEquals("{f:'1'}",x.getX4(Bean.create()));
		assertEquals("{f:'1'}",x.getX5(Bean.create()));
		assertEquals("{x:'f=1,f=1'}",x.getX6(a(Bean.create(),Bean.create())));
		assertEquals("{x:'@((f=1),(f=1))'}",x.getX7(a(Bean.create(),Bean.create())));
		assertEquals("{x:'f=1,f=1'}",x.getX8(alist(Bean.create(),Bean.create())));
		assertEquals("{x:'@((f=1),(f=1))'}",x.getX9(alist(Bean.create(),Bean.create())));
		assertEquals("{x:'k1=f\\\\=1'}",x.getX10(map("k1",Bean.create())));
		assertEquals("{k1:'f=1'}",x.getX11(map("k1",Bean.create())));
		assertEquals("{k1:'f=1'}",x.getX12(map("k1",Bean.create())));
		assertEquals("{x:'(k1=(f=1))'}",x.getX13(map("k1",Bean.create())));
		assertEquals("{k1:'f=1'}",x.getX14(map("k1",Bean.create())));
		assertEquals("{x:'1'}",x.getX15(reader("x=1")));
		assertEquals("{x:'1'}",x.getX16(reader("x=1")));
		assertEquals("{x:'1'}",x.getX17(inputStream("x=1")));
		assertEquals("{x:'1'}",x.getX18(inputStream("x=1")));
		assertEquals("{foo:'bar'}",x.getX19(parts("foo","bar")));
		assertEquals("{foo:'bar'}",x.getX20(parts("foo","bar")));
		assertEquals("{foo:'bar'}",x.getX21(part("foo","bar")));
		assertEquals("{foo:'bar'}",x.getX22(parts("foo","bar").getAll()));
		assertEquals("{foo:'bar'}",x.getX24("foo=bar"));
		assertEquals("{}",x.getX24(null));
		assertEquals("{foo:'bar'}",x.getX25(alist(part("foo","bar"))));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// @Query(_default/allowEmptyValue)
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class B {
		@RestOp
		public String get(@Query("*") JsonMap m) {
			return m.toString();
		}
	}

	@Remote
	public interface B1 {
		@RemoteOp(path="/") String getX1(@Query("x") @Schema(df="foo") String b);
		@RemoteOp(path="/") String getX2(@Query("x") @Schema(df="foo",aev=true) String b);
		@RemoteOp(path="/") String getX3(@Query("x") @Schema(df="") String b);
		@RemoteOp(path="/") String getX4(@Query("x") @Schema(df="",aev=true) String b);
	}

	@Test void b01_default_aev() {
		var x = remote(B.class,B1.class);
		assertEquals("{x:'foo'}",x.getX1(null));
		assertThrowsWithMessage(Exception.class, "Empty value not allowed.", ()->x.getX1(""));
		assertEquals("{x:'foo'}",x.getX2(null));
		assertEquals("{x:''}",x.getX2(""));
		assertEquals("{}",x.getX3(null));  // Empty string default is not applied (changed in 9.2.0)
		assertThrowsWithMessage(Exception.class, "Empty value not allowed.", ()->x.getX3(""));
		assertEquals("{}",x.getX4(null));  // Empty string default is not applied (changed in 9.2.0)
		assertEquals("{x:''}",x.getX4(""));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// @Query(collectionFormat)
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class C {
		@RestGet
		public String a(@Query("*") JsonMap m) {
			return m.toString();
		}
		@RestGet
		public Reader b(RestRequest req) {
			return reader(req.getQueryString());
		}
	}

	@Remote
	public interface C1 {
		@RemoteOp(path="/a") String getX1(@Query("x") String...b);
		@RemoteOp(path="/b") String getX2(@Query("x") String...b);
		@RemoteOp(path="/a") String getX3(@Query("x") @Schema(cf="csv") String...b);
		@RemoteOp(path="/b") String getX4(@Query("x") @Schema(cf="csv") String...b);
		@RemoteOp(path="/a") String getX5(@Query("x") @Schema(cf="ssv") String...b);
		@RemoteOp(path="/b") String getX6(@Query("x") @Schema(cf="ssv") String...b);
		@RemoteOp(path="/a") String getX7(@Query("x") @Schema(cf="tsv") String...b);
		@RemoteOp(path="/b") String getX8(@Query("x") @Schema(cf="tsv") String...b);
		@RemoteOp(path="/a") String getX9(@Query("x") @Schema(cf="pipes") String...b);
		@RemoteOp(path="/b") String getX10(@Query("x") @Schema(cf="pipes") String...b);
		@RemoteOp(path="/a") String getX11(@Query("x") @Schema(cf="multi") String...b); // Not supported,but should be treated as csv.
		@RemoteOp(path="/b") String getX12(@Query("x") @Schema(cf="multi") String...b); // Not supported,but should be treated as csv.
		@RemoteOp(path="/a") String getX13(@Query("x") @Schema(cf="uon") String...b);
		@RemoteOp(path="/b") String getX14(@Query("x") @Schema(cf="uon") String...b);
	}

	@Test void c01_collectionFormat() {
		var x = remote(C.class,C1.class);
		assertEquals("{x:'foo,bar'}",x.getX1("foo","bar"));
		assertEquals("x=foo%2Cbar",x.getX2("foo","bar"));
		assertEquals("{x:'foo,bar'}",x.getX3("foo","bar"));
		assertEquals("x=foo%2Cbar",x.getX4("foo","bar"));
		assertEquals("{x:'foo bar'}",x.getX5("foo","bar"));
		assertEquals("x=foo+bar",x.getX6("foo","bar"));
		assertEquals("{x:'foo\\tbar'}",x.getX7("foo","bar"));
		assertEquals("x=foo%09bar",x.getX8("foo","bar"));
		assertEquals("{x:'foo|bar'}",x.getX9("foo","bar"));
		assertEquals("x=foo%7Cbar",x.getX10("foo","bar"));
		assertEquals("{x:'foo,bar'}",x.getX11("foo","bar"));
		assertEquals("x=foo%2Cbar",x.getX12("foo","bar"));
		assertEquals("{x:'@(foo,bar)'}",x.getX13("foo","bar"));
		assertEquals("x=%40%28foo%2Cbar%29",x.getX14("foo","bar"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// @Query(maximum,exclusiveMaximum,minimum,exclusiveMinimum)
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class D {
		@RestOp
		public String get(@Query("*") JsonMap m) {
			return m.toString();
		}
	}

	@Remote
	public interface D1 {
		@RemoteOp(path="/") String getX1(@Query("x") @Schema(min="1",max="10") int b);
		@RemoteOp(path="/") String getX2(@Query("x") @Schema(min="1",max="10",emin=false,emax=false) int b);
		@RemoteOp(path="/") String getX3(@Query("x") @Schema(min="1",max="10",emin=true,emax=true) int b);
		@RemoteOp(path="/") String getX4(@Query("x") @Schema(min="1",max="10") short b);
		@RemoteOp(path="/") String getX5(@Query("x") @Schema(min="1",max="10",emin=false,emax=false) short b);
		@RemoteOp(path="/") String getX6(@Query("x") @Schema(min="1",max="10",emin=true,emax=true) short b);
		@RemoteOp(path="/") String getX7(@Query("x") @Schema(min="1",max="10") long b);
		@RemoteOp(path="/") String getX8(@Query("x") @Schema(min="1",max="10",emin=false,emax=false) long b);
		@RemoteOp(path="/") String getX9(@Query("x") @Schema(min="1",max="10",emin=true,emax=true) long b);
		@RemoteOp(path="/") String getX10(@Query("x") @Schema(min="1",max="10") float b);
		@RemoteOp(path="/") String getX11(@Query("x") @Schema(min="1",max="10",emin=false,emax=false) float b);
		@RemoteOp(path="/") String getX12(@Query("x") @Schema(min="1",max="10",emin=true,emax=true) float b);
		@RemoteOp(path="/") String getX13(@Query("x") @Schema(min="1",max="10") double b);
		@RemoteOp(path="/") String getX14(@Query("x") @Schema(min="1",max="10",emin=false,emax=false) double b);
		@RemoteOp(path="/") String getX15(@Query("x") @Schema(min="1",max="10",emin=true,emax=true) double b);
		@RemoteOp(path="/") String getX16(@Query("x") @Schema(min="1",max="10") byte b);
		@RemoteOp(path="/") String getX17(@Query("x") @Schema(min="1",max="10",emin=false,emax=false) byte b);
		@RemoteOp(path="/") String getX18(@Query("x") @Schema(min="1",max="10",emin=true,emax=true) byte b);
		@RemoteOp(path="/") String getX19(@Query("x") @Schema(min="1",max="10") AtomicInteger b);
		@RemoteOp(path="/") String getX20(@Query("x") @Schema(min="1",max="10",emin=false,emax=false) AtomicInteger b);
		@RemoteOp(path="/") String getX21(@Query("x") @Schema(min="1",max="10",emin=true,emax=true) AtomicInteger b);
		@RemoteOp(path="/") String getX22(@Query("x") @Schema(min="1",max="10") BigDecimal b);
		@RemoteOp(path="/") String getX23(@Query("x") @Schema(min="1",max="10",emin=false,emax=false) BigDecimal b);
		@RemoteOp(path="/") String getX24(@Query("x") @Schema(min="1",max="10",emin=true,emax=true) BigDecimal b);
		@RemoteOp(path="/") String getX25(@Query("x") @Schema(min="1",max="10") Integer b);
		@RemoteOp(path="/") String getX26(@Query("x") @Schema(min="1",max="10",emin=false,emax=false) Integer b);
		@RemoteOp(path="/") String getX27(@Query("x") @Schema(min="1",max="10",emin=true,emax=true) Integer b);
		@RemoteOp(path="/") String getX28(@Query("x") @Schema(min="1",max="10") Short b);
		@RemoteOp(path="/") String getX29(@Query("x") @Schema(min="1",max="10",emin=false,emax=false) Short b);
		@RemoteOp(path="/") String getX30(@Query("x") @Schema(min="1",max="10",emin=true,emax=true) Short b);
		@RemoteOp(path="/") String getX31(@Query("x") @Schema(min="1",max="10") Long b);
		@RemoteOp(path="/") String getX32(@Query("x") @Schema(min="1",max="10",emin=false,emax=false) Long b);
		@RemoteOp(path="/") String getX33(@Query("x") @Schema(min="1",max="10",emin=true,emax=true) Long b);
		@RemoteOp(path="/") String getX34(@Query("x") @Schema(min="1",max="10") Float b);
		@RemoteOp(path="/") String getX35(@Query("x") @Schema(min="1",max="10",emin=false,emax=false) Float b);
		@RemoteOp(path="/") String getX36(@Query("x") @Schema(min="1",max="10",emin=true,emax=true) Float b);
		@RemoteOp(path="/") String getX37(@Query("x") @Schema(min="1",max="10") Double b);
		@RemoteOp(path="/") String getX38(@Query("x") @Schema(min="1",max="10",emin=false,emax=false) Double b);
		@RemoteOp(path="/") String getX39(@Query("x") @Schema(min="1",max="10",emin=true,emax=true) Double b);
		@RemoteOp(path="/") String getX40(@Query("x") @Schema(min="1",max="10") Byte b);
		@RemoteOp(path="/") String getX41(@Query("x") @Schema(min="1",max="10",emin=false,emax=false) Byte b);
		@RemoteOp(path="/") String getX42(@Query("x") @Schema(min="1",max="10",emin=true,emax=true) Byte b);
	}

	@Test void d01_min_max_emin_emax() {
		var x = remote(D.class,D1.class);
		assertEquals("{x:'1'}",x.getX1(1));
		assertEquals("{x:'10'}",x.getX1(10));
		assertThrowsWithMessage(Exception.class, "Minimum value not met.", ()->x.getX1(0));
		assertThrowsWithMessage(Exception.class, "Maximum value exceeded.", ()->x.getX1(11));
		assertEquals("{x:'1'}",x.getX2(1));
		assertEquals("{x:'10'}",x.getX2(10));
		assertThrowsWithMessage(Exception.class, "Minimum value not met.", ()->x.getX2(0));
		assertThrowsWithMessage(Exception.class, "Maximum value exceeded.", ()->x.getX2(11));
		assertEquals("{x:'2'}",x.getX3(2));
		assertEquals("{x:'9'}",x.getX3(9));
		assertThrowsWithMessage(Exception.class, "Minimum value not met.", ()->x.getX3(1));
		assertThrowsWithMessage(Exception.class, "Maximum value exceeded.", ()->x.getX3(10));
		assertEquals("{x:'1'}",x.getX4((short)1));
		assertEquals("{x:'10'}",x.getX4((short)10));
		assertThrowsWithMessage(Exception.class, "Minimum value not met.", ()->x.getX4((short)0));
		assertThrowsWithMessage(Exception.class, "Maximum value exceeded.", ()->x.getX4((short)11));
		assertEquals("{x:'1'}",x.getX5((short)1));
		assertEquals("{x:'10'}",x.getX5((short)10));
		assertThrowsWithMessage(Exception.class, "Minimum value not met.", ()->x.getX5((short)0));
		assertThrowsWithMessage(Exception.class, "Maximum value exceeded.", ()->x.getX5((short)11));
		assertEquals("{x:'2'}",x.getX6((short)2));
		assertEquals("{x:'9'}",x.getX6((short)9));
		assertThrowsWithMessage(Exception.class, "Minimum value not met.", ()->x.getX6((short)1));
		assertThrowsWithMessage(Exception.class, "Maximum value exceeded.", ()->x.getX6((short)10));
		assertEquals("{x:'1'}",x.getX7(1L));
		assertEquals("{x:'10'}",x.getX7(10L));
		assertThrowsWithMessage(Exception.class, "Minimum value not met.", ()->x.getX7(0L));
		assertThrowsWithMessage(Exception.class, "Maximum value exceeded.", ()->x.getX7(11L));
		assertEquals("{x:'1'}",x.getX8(1L));
		assertEquals("{x:'10'}",x.getX8(10L));
		assertThrowsWithMessage(Exception.class, "Minimum value not met.", ()->x.getX8(0L));
		assertThrowsWithMessage(Exception.class, "Maximum value exceeded.", ()->x.getX8(11L));
		assertEquals("{x:'2'}",x.getX9(2L));
		assertEquals("{x:'9'}",x.getX9(9L));
		assertThrowsWithMessage(Exception.class, "Minimum value not met.", ()->x.getX9(1L));
		assertThrowsWithMessage(Exception.class, "Maximum value exceeded.", ()->x.getX9(10L));
		assertEquals("{x:'1.0'}",x.getX10(1f));
		assertEquals("{x:'10.0'}",x.getX10(10f));
		assertThrowsWithMessage(Exception.class, "Minimum value not met.", ()->x.getX10(0.9f));
		assertThrowsWithMessage(Exception.class, "Maximum value exceeded.", ()->x.getX10(10.1f));
		assertEquals("{x:'1.0'}",x.getX11(1f));
		assertEquals("{x:'10.0'}",x.getX11(10f));
		assertThrowsWithMessage(Exception.class, "Minimum value not met.", ()->x.getX11(0.9f));
		assertThrowsWithMessage(Exception.class, "Maximum value exceeded.", ()->x.getX11(10.1f));
		assertEquals("{x:'1.1'}",x.getX12(1.1f));
		assertEquals("{x:'9.9'}",x.getX12(9.9f));
		assertThrowsWithMessage(Exception.class, "Minimum value not met.", ()->x.getX12(1f));
		assertThrowsWithMessage(Exception.class, "Maximum value exceeded.", ()->x.getX12(10f));
		assertEquals("{x:'1.0'}",x.getX13(1d));
		assertEquals("{x:'10.0'}",x.getX13(10d));
		assertThrowsWithMessage(Exception.class, "Minimum value not met.", ()->x.getX13(0.9d));
		assertThrowsWithMessage(Exception.class, "Maximum value exceeded.", ()->x.getX13(10.1d));
		assertEquals("{x:'1.0'}",x.getX14(1d));
		assertEquals("{x:'10.0'}",x.getX14(10d));
		assertThrowsWithMessage(Exception.class, "Minimum value not met.", ()->x.getX14(0.9d));
		assertThrowsWithMessage(Exception.class, "Maximum value exceeded.", ()->x.getX14(10.1d));
		assertEquals("{x:'1.1'}",x.getX15(1.1d));
		assertEquals("{x:'9.9'}",x.getX15(9.9d));
		assertThrowsWithMessage(Exception.class, "Minimum value not met.", ()->x.getX15(1d));
		assertThrowsWithMessage(Exception.class, "Maximum value exceeded.", ()->x.getX15(10d));
		assertEquals("{x:'1'}",x.getX16((byte)1));
		assertEquals("{x:'10'}",x.getX16((byte)10));
		assertThrowsWithMessage(Exception.class, "Minimum value not met.", ()->x.getX16((byte)0));
		assertThrowsWithMessage(Exception.class, "Maximum value exceeded.", ()->x.getX16((byte)11));
		assertEquals("{x:'1'}",x.getX17((byte)1));
		assertEquals("{x:'10'}",x.getX17((byte)10));
		assertThrowsWithMessage(Exception.class, "Minimum value not met.", ()->x.getX17((byte)0));
		assertThrowsWithMessage(Exception.class, "Maximum value exceeded.", ()->x.getX17((byte)11));
		assertEquals("{x:'2'}",x.getX18((byte)2));
		assertEquals("{x:'9'}",x.getX18((byte)9));
		assertThrowsWithMessage(Exception.class, "Minimum value not met.", ()->x.getX18((byte)1));
		assertThrowsWithMessage(Exception.class, "Maximum value exceeded.", ()->x.getX18((byte)10));
		assertEquals("{x:'1'}",x.getX19(new AtomicInteger(1)));
		assertEquals("{x:'10'}",x.getX19(new AtomicInteger(10)));
		assertThrowsWithMessage(Exception.class, "Minimum value not met.", ()->x.getX19(new AtomicInteger(0)));
		assertThrowsWithMessage(Exception.class, "Maximum value exceeded.", ()->x.getX19(new AtomicInteger(11)));
		assertEquals("{x:'1'}",x.getX20(new AtomicInteger(1)));
		assertEquals("{x:'10'}",x.getX20(new AtomicInteger(10)));
		assertThrowsWithMessage(Exception.class, "Minimum value not met.", ()->x.getX20(new AtomicInteger(0)));
		assertThrowsWithMessage(Exception.class, "Maximum value exceeded.", ()->x.getX20(new AtomicInteger(11)));
		assertEquals("{x:'2'}",x.getX21(new AtomicInteger(2)));
		assertEquals("{x:'9'}",x.getX21(new AtomicInteger(9)));
		assertThrowsWithMessage(Exception.class, "Minimum value not met.", ()->x.getX21(new AtomicInteger(1)));
		assertThrowsWithMessage(Exception.class, "Maximum value exceeded.", ()->x.getX21(new AtomicInteger(10)));
		assertEquals("{x:'1'}",x.getX22(new BigDecimal(1)));
		assertEquals("{x:'10'}",x.getX22(new BigDecimal(10)));
		assertThrowsWithMessage(Exception.class, "Minimum value not met.", ()->x.getX22(new BigDecimal(0)));
		assertThrowsWithMessage(Exception.class, "Maximum value exceeded.", ()->x.getX22(new BigDecimal(11)));
		assertEquals("{x:'1'}",x.getX23(new BigDecimal(1)));
		assertEquals("{x:'10'}",x.getX23(new BigDecimal(10)));
		assertThrowsWithMessage(Exception.class, "Minimum value not met.", ()->x.getX23(new BigDecimal(0)));
		assertThrowsWithMessage(Exception.class, "Maximum value exceeded.", ()->x.getX23(new BigDecimal(11)));
		assertEquals("{x:'2'}",x.getX24(new BigDecimal(2)));
		assertEquals("{x:'9'}",x.getX24(new BigDecimal(9)));
		assertThrowsWithMessage(Exception.class, "Minimum value not met.", ()->x.getX24(new BigDecimal(1)));
		assertThrowsWithMessage(Exception.class, "Maximum value exceeded.", ()->x.getX24(new BigDecimal(10)));
		assertEquals("{x:'1'}",x.getX25(1));
		assertEquals("{x:'10'}",x.getX25(10));
		assertThrowsWithMessage(Exception.class, "Minimum value not met.", ()->x.getX25(0));
		assertThrowsWithMessage(Exception.class, "Maximum value exceeded.", ()->x.getX25(11));
		assertEquals("{}",x.getX25(null));
		assertEquals("{x:'1'}",x.getX26(1));
		assertEquals("{x:'10'}",x.getX26(10));
		assertThrowsWithMessage(Exception.class, "Minimum value not met.", ()->x.getX26(0));
		assertThrowsWithMessage(Exception.class, "Maximum value exceeded.", ()->x.getX26(11));
		assertEquals("{}",x.getX26(null));
		assertEquals("{x:'2'}",x.getX27(2));
		assertEquals("{x:'9'}",x.getX27(9));
		assertThrowsWithMessage(Exception.class, "Minimum value not met.", ()->x.getX27(1));
		assertThrowsWithMessage(Exception.class, "Maximum value exceeded.", ()->x.getX27(10));
		assertEquals("{}",x.getX27(null));
		assertEquals("{x:'1'}",x.getX28((short)1));
		assertEquals("{x:'10'}",x.getX28((short)10));
		assertThrowsWithMessage(Exception.class, "Minimum value not met.", ()->x.getX28((short)0));
		assertThrowsWithMessage(Exception.class, "Maximum value exceeded.", ()->x.getX28((short)11));
		assertEquals("{}",x.getX28(null));
		assertEquals("{x:'1'}",x.getX29((short)1));
		assertEquals("{x:'10'}",x.getX29((short)10));
		assertThrowsWithMessage(Exception.class, "Minimum value not met.", ()->x.getX29((short)0));
		assertThrowsWithMessage(Exception.class, "Maximum value exceeded.", ()->x.getX29((short)11));
		assertEquals("{}",x.getX29(null));
		assertEquals("{x:'2'}",x.getX30((short)2));
		assertEquals("{x:'9'}",x.getX30((short)9));
		assertThrowsWithMessage(Exception.class, "Minimum value not met.", ()->x.getX30((short)1));
		assertThrowsWithMessage(Exception.class, "Maximum value exceeded.", ()->x.getX30((short)10));
		assertEquals("{}",x.getX30(null));
		assertEquals("{x:'1'}",x.getX31(1L));
		assertEquals("{x:'10'}",x.getX31(10L));
		assertThrowsWithMessage(Exception.class, "Minimum value not met.", ()->x.getX31(0L));
		assertThrowsWithMessage(Exception.class, "Maximum value exceeded.", ()->x.getX31(11L));
		assertEquals("{}",x.getX31(null));
		assertEquals("{x:'1'}",x.getX32(1L));
		assertEquals("{x:'10'}",x.getX32(10L));
		assertThrowsWithMessage(Exception.class, "Minimum value not met.", ()->x.getX32(0L));
		assertThrowsWithMessage(Exception.class, "Maximum value exceeded.", ()->x.getX32(11L));
		assertEquals("{}",x.getX32(null));
		assertEquals("{x:'2'}",x.getX33(2L));
		assertEquals("{x:'9'}",x.getX33(9L));
		assertThrowsWithMessage(Exception.class, "Minimum value not met.", ()->x.getX33(1L));
		assertThrowsWithMessage(Exception.class, "Maximum value exceeded.", ()->x.getX33(10L));
		assertEquals("{}",x.getX33(null));
		assertEquals("{x:'1.0'}",x.getX34(1f));
		assertEquals("{x:'10.0'}",x.getX34(10f));
		assertThrowsWithMessage(Exception.class, "Minimum value not met.", ()->x.getX34(0.9f));
		assertThrowsWithMessage(Exception.class, "Maximum value exceeded.", ()->x.getX34(10.1f));
		assertEquals("{}",x.getX34(null));
		assertEquals("{x:'1.0'}",x.getX35(1f));
		assertEquals("{x:'10.0'}",x.getX35(10f));
		assertThrowsWithMessage(Exception.class, "Minimum value not met.", ()->x.getX35(0.9f));
		assertThrowsWithMessage(Exception.class, "Maximum value exceeded.", ()->x.getX35(10.1f));
		assertEquals("{}",x.getX35(null));
		assertEquals("{x:'1.1'}",x.getX36(1.1f));
		assertEquals("{x:'9.9'}",x.getX36(9.9f));
		assertThrowsWithMessage(Exception.class, "Minimum value not met.", ()->x.getX36(1f));
		assertThrowsWithMessage(Exception.class, "Maximum value exceeded.", ()->x.getX36(10f));
		assertEquals("{}",x.getX36(null));
		assertEquals("{x:'1.0'}",x.getX37(1d));
		assertEquals("{x:'10.0'}",x.getX37(10d));
		assertThrowsWithMessage(Exception.class, "Minimum value not met.", ()->x.getX37(0.9d));
		assertThrowsWithMessage(Exception.class, "Maximum value exceeded.", ()->x.getX37(10.1d));
		assertEquals("{}",x.getX37(null));
		assertEquals("{x:'1.0'}",x.getX38(1d));
		assertEquals("{x:'10.0'}",x.getX38(10d));
		assertThrowsWithMessage(Exception.class, "Minimum value not met.", ()->x.getX38(0.9d));
		assertThrowsWithMessage(Exception.class, "Maximum value exceeded.", ()->x.getX38(10.1d));
		assertEquals("{}",x.getX38(null));
		assertEquals("{x:'1.1'}",x.getX39(1.1d));
		assertEquals("{x:'9.9'}",x.getX39(9.9d));
		assertThrowsWithMessage(Exception.class, "Minimum value not met.", ()->x.getX39(1d));
		assertThrowsWithMessage(Exception.class, "Maximum value exceeded.", ()->x.getX39(10d));
		assertEquals("{}",x.getX39(null));
		assertEquals("{x:'1'}",x.getX40((byte)1));
		assertEquals("{x:'10'}",x.getX40((byte)10));
		assertThrowsWithMessage(Exception.class, "Minimum value not met.", ()->x.getX40((byte)0));
		assertThrowsWithMessage(Exception.class, "Maximum value exceeded.", ()->x.getX40((byte)11));
		assertEquals("{}",x.getX40(null));
		assertEquals("{x:'1'}",x.getX41((byte)1));
		assertEquals("{x:'10'}",x.getX41((byte)10));
		assertThrowsWithMessage(Exception.class, "Minimum value not met.", ()->x.getX41((byte)0));
		assertThrowsWithMessage(Exception.class, "Maximum value exceeded.", ()->x.getX41((byte)11));
		assertEquals("{}",x.getX41(null));
		assertEquals("{x:'2'}",x.getX42((byte)2));
		assertEquals("{x:'9'}",x.getX42((byte)9));
		assertThrowsWithMessage(Exception.class, "Minimum value not met.", ()->x.getX42((byte)1));
		assertThrowsWithMessage(Exception.class, "Maximum value exceeded.", ()->x.getX42((byte)10));
		assertEquals("{}",x.getX42(null));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// @Query(maxItems,minItems,uniqueItems)
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class E {
		@RestOp
		public String get(@Query("*") JsonMap m) {
			return m.toString();
		}
	}

	@Remote
	public interface E1 {
		@RemoteOp(path="/") String getX1(@Query("x") @Schema(cf="pipes",mini=1,maxi=2) String...b);
		@RemoteOp(path="/") String getX2(@Query("x") @Schema(items=@Items(cf="pipes",mini=1,maxi=2)) String[]...b);
		@RemoteOp(path="/") String getX3(@Query("x") @Schema(cf="pipes",ui=false) String...b);
		@RemoteOp(path="/") String getX4(@Query("x") @Schema(items=@Items(cf="pipes",ui=false)) String[]...b);
		@RemoteOp(path="/") String getX5(@Query("x") @Schema(cf="pipes",ui=true) String...b);
		@RemoteOp(path="/") String getX6(@Query("x") @Schema(items=@Items(cf="pipes",ui=true)) String[]...b);
	}

	@Test void e01_mini_maxi_ui() {
		var x = remote(E.class,E1.class);
		assertEquals("{x:'1'}",x.getX1("1"));
		assertEquals("{x:'1|2'}",x.getX1("1","2"));
		assertThrowsWithMessage(Exception.class, "Minimum number of items not met.", x::getX1);
		assertThrowsWithMessage(Exception.class, "Maximum number of items exceeded.", ()->x.getX1("1","2","3"));
		assertEquals("{x:null}",x.getX1((String)null));
		assertEquals("{x:'1'}",x.getX2(a("1")));
		assertEquals("{x:'1|2'}",x.getX2(a("1","2")));
		assertThrowsWithMessage(Exception.class, "Minimum number of items not met.", ()->x.getX2(new String[]{}));
		assertThrowsWithMessage(Exception.class, "Maximum number of items exceeded.", ()->x.getX2(a("1","2","3")));
		assertEquals("{x:null}",x.getX2(a((String)null)));
		assertEquals("{x:'1|1'}",x.getX3("1","1"));
		assertEquals("{x:'1|1'}",x.getX4(a("1","1")));
		assertEquals("{x:'1|2'}",x.getX5("1","2"));
		assertThrowsWithMessage(Exception.class, "Duplicate items not allowed.", ()->x.getX5("1","1"));
		assertEquals("{x:'1|2'}",x.getX6(a("1","2")));
		assertThrowsWithMessage(Exception.class, "Duplicate items not allowed.", ()->x.getX6(a("1","1")));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// @Query(maxLength,minLength,enum)
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class F {
		@RestOp
		public String get(@Query("*") JsonMap m) {
			return m.toString();
		}
	}

	@Remote
	public interface F1 {
		@RemoteOp(path="/") String getX1(@Query("x") @Schema(minl=2,maxl=3) String b);
		@RemoteOp(path="/") String getX2(@Query("x") @Schema(cf="pipes",items=@Items(minl=2,maxl=3)) String...b);
		@RemoteOp(path="/") String getX3(@Query("x") @Schema(e={"foo"}) String b);
		@RemoteOp(path="/") String getX4(@Query("x") @Schema(cf="pipes",items=@Items(e={"foo"})) String...b);
		@RemoteOp(path="/") String getX5(@Query("x") @Schema(p="foo\\d{1,3}") String b);
		@RemoteOp(path="/") String getX6(@Query("x") @Schema(cf="pipes",items=@Items(p="foo\\d{1,3}")) String...b);
	}

	@Test void f01_minl_maxl_enum() {
		var x = remote(F.class,F1.class);
		assertEquals("{x:'12'}",x.getX1("12"));
		assertEquals("{x:'123'}",x.getX1("123"));
		assertThrowsWithMessage(Exception.class, "Minimum length of value not met.", ()->x.getX1("1"));
		assertThrowsWithMessage(Exception.class, "Maximum length of value exceeded.", ()->x.getX1("1234"));
		assertEquals("{}",x.getX1(null));
		assertEquals("{x:'12|34'}",x.getX2("12","34"));
		assertEquals("{x:'123|456'}",x.getX2("123","456"));
		assertThrowsWithMessage(Exception.class, "Minimum length of value not met.", ()->x.getX2("1","2"));
		assertThrowsWithMessage(Exception.class, "Maximum length of value exceeded.", ()->x.getX2("1234","5678"));
		assertEquals("{x:'12|null'}",x.getX2("12",null));
		assertEquals("{x:'foo'}",x.getX3("foo"));
		assertThrowsWithMessage(Exception.class, "Value does not match one of the expected values.  Must be one of the following:  foo", ()->x.getX3("bar"));
		assertEquals("{}",x.getX3(null));
		assertEquals("{x:'foo'}",x.getX4("foo"));
		assertThrowsWithMessage(Exception.class, "Value does not match one of the expected values.  Must be one of the following:  foo", ()->x.getX4("bar"));
		assertEquals("{x:null}",x.getX4((String)null));
		assertEquals("{x:'foo123'}",x.getX5("foo123"));
		assertThrowsWithMessage(Exception.class, "Value does not match expected pattern.  Must match pattern: foo\\d{1,3}", ()->x.getX5("bar"));
		assertEquals("{}",x.getX5(null));
		assertEquals("{x:'foo123'}",x.getX6("foo123"));
		assertThrowsWithMessage(Exception.class, "Value does not match expected pattern.  Must match pattern: foo\\d{1,3}", ()->x.getX6("foo"));
		assertEquals("{x:null}",x.getX6((String)null));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// @Query(multipleOf)
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class G {
		@RestOp
		public String get(@Query("*") JsonMap m) {
			return m.toString();
		}
	}

	@Remote
	public interface G1 {
		@RemoteOp(path="/") String getX1(@Query("x") @Schema(mo="2") int b);
		@RemoteOp(path="/") String getX2(@Query("x") @Schema(mo="2") short b);
		@RemoteOp(path="/") String getX3(@Query("x") @Schema(mo="2") long b);
		@RemoteOp(path="/") String getX4(@Query("x") @Schema(mo="2") float b);
		@RemoteOp(path="/") String getX5(@Query("x") @Schema(mo="2") double b);
		@RemoteOp(path="/") String getX6(@Query("x") @Schema(mo="2") byte b);
		@RemoteOp(path="/") String getX7(@Query("x") @Schema(mo="2") AtomicInteger b);
		@RemoteOp(path="/") String getX8(@Query("x") @Schema(mo="2") BigDecimal b);
		@RemoteOp(path="/") String getX9(@Query("x") @Schema(mo="2") Integer b);
		@RemoteOp(path="/") String getX10(@Query("x") @Schema(mo="2") Short b);
		@RemoteOp(path="/") String getX11(@Query("x") @Schema(mo="2") Long b);
		@RemoteOp(path="/") String getX12(@Query("x") @Schema(mo="2") Float b);
		@RemoteOp(path="/") String getX13(@Query("x") @Schema(mo="2") Double b);
		@RemoteOp(path="/") String getX14(@Query("x") @Schema(mo="2") Byte b);
	}

	@Test void g01_multipleOf() {
		var x = remote(G.class,G1.class);
		assertEquals("{x:'4'}",x.getX1(4));
		assertThrowsWithMessage(Exception.class, "Multiple-of not met.", ()->x.getX1(5));
		assertEquals("{x:'4'}",x.getX2((short)4));
		assertThrowsWithMessage(Exception.class, "Multiple-of not met.", ()->x.getX2((short)5));
		assertEquals("{x:'4'}",x.getX3(4));
		assertThrowsWithMessage(Exception.class, "Multiple-of not met.", ()->x.getX3(5));
		assertEquals("{x:'4.0'}",x.getX4(4));
		assertThrowsWithMessage(Exception.class, "Multiple-of not met.", ()->x.getX4(5));
		assertEquals("{x:'4.0'}",x.getX5(4));
		assertThrowsWithMessage(Exception.class, "Multiple-of not met.", ()->x.getX5(5));
		assertEquals("{x:'4'}",x.getX6((byte)4));
		assertThrowsWithMessage(Exception.class, "Multiple-of not met.", ()->x.getX6((byte)5));
		assertEquals("{x:'4'}",x.getX7(new AtomicInteger(4)));
		assertThrowsWithMessage(Exception.class, "Multiple-of not met.", ()->x.getX7(new AtomicInteger(5)));
		assertEquals("{x:'4'}",x.getX8(new BigDecimal(4)));
		assertThrowsWithMessage(Exception.class, "Multiple-of not met.", ()->x.getX8(new BigDecimal(5)));
		assertEquals("{x:'4'}",x.getX9(4));
		assertThrowsWithMessage(Exception.class, "Multiple-of not met.", ()->x.getX9(5));
		assertEquals("{x:'4'}",x.getX10((short)4));
		assertThrowsWithMessage(Exception.class, "Multiple-of not met.", ()->x.getX10((short)5));
		assertEquals("{x:'4'}",x.getX11(4L));
		assertThrowsWithMessage(Exception.class, "Multiple-of not met.", ()->x.getX11(5L));
		assertEquals("{x:'4.0'}",x.getX12(4f));
		assertThrowsWithMessage(Exception.class, "Multiple-of not met.", ()->x.getX12(5f));
		assertEquals("{x:'4.0'}",x.getX13(4d));
		assertThrowsWithMessage(Exception.class, "Multiple-of not met.", ()->x.getX13(5d));
		assertEquals("{x:'4'}",x.getX14((byte)4));
		assertThrowsWithMessage(Exception.class, "Multiple-of not met.", ()->x.getX14((byte)5));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// @Query(required)
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class H {
		@RestOp
		public String get(@Query("*") JsonMap m) {
			return m.toString();
		}
	}

	@Remote
	public interface H1 {
		@RemoteOp(path="/") String getX1(@Query("x") String b);
		@RemoteOp(path="/") String getX2(@Query("x") @Schema(r=false) String b);
		@RemoteOp(path="/") String getX3(@Query("x") @Schema(r=true) String b);
	}

	@Test void h01_required() {
		var x = remote(H.class,H1.class);
		assertEquals("{}",x.getX1(null));
		assertEquals("{}",x.getX2(null));
		assertEquals("{x:'1'}",x.getX3("1"));
		assertThrowsWithMessage(Exception.class, "Required value not provided.", ()->x.getX3(null));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// @Query(skipIfEmpty)
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class I {
		@RestOp
		public String get(@Query("*") JsonMap m) {
			return m.toString();
		}
	}

	@Remote
	public interface I1 {
		@RemoteOp(path="/") String getX1(@Query("x") @Schema(aev=true) String b);
		@RemoteOp(path="/") String getX2(@Query("x") @Schema(aev=true,sie=false) String b);
		@RemoteOp(path="/") String getX3(@Query("x") @Schema(sie=true) String b);
	}

	@Test void i01_skipIfEmpty() {
		var x = remote(I.class,I1.class);
		assertEquals("{x:''}",x.getX1(""));
		assertEquals("{x:''}",x.getX2(""));
		assertEquals("{}",x.getX3(""));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// @Query(serializer)
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class J {
		@RestOp
		public String get(@Query("*") JsonMap m) {
			return m.toString();
		}
	}

	@Remote
	public interface J1 {
		@RemoteOp(path="/") String getX1(@Query(name="x",serializer=FakeWriterSerializer.X.class) String b);
	}

	@Test void j01_serializer() {
		var x = remote(J.class,J1.class);
		assertEquals("{x:'xXx'}",x.getX1("X"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// RequestBean @Query - Return types
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class K {
		@RestOp
		public String get(RestRequest req) {
			return req.getQueryParams().toString();
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// RequestBean @Query - Simple values
	//-----------------------------------------------------------------------------------------------------------------

	@Remote(path="/")
	public interface K1 {
		@RemoteOp(path="/") String getX1(@Request K1b rb);
		@RemoteOp(path="/") String getX2(@Request(serializer=FakeWriterSerializer.X.class) K1b rb);
	}

	public interface K1a {
		@Query String getA();
		@Query("b") String getX1();
		@Query("c") String getX2();
		@Query("e") @Schema(allowEmptyValue=true) String getX4();
		@Query("f") String getX5();
		@Query("g") String getX6();
		@Query("h") String getX7();
		@Query("i1") @Schema(sie=true) String getX8();
		@Query("i2") @Schema(sie=true) String getX9();
		@Query("i3") @Schema(sie=true) String getX10();
	}

	public static class K1b implements K1a {
		@Override public String getA() { return "a1"; }
		@Override public String getX1() { return "b1"; }
		@Override public String getX2() { return "c1"; }
		@Override public String getX4() { return ""; }
		@Override public String getX5() { return null; }
		@Override public String getX6() { return "true"; }
		@Override public String getX7() { return "123"; }
		@Override public String getX8() { return "foo"; }
		@Override public String getX9() { return ""; }
		@Override public String getX10() { return null; }
	}

	@Test void k01_requestBean_simpleVals() {
		var x1 = remote(K.class,K1.class);
		var x2 = client(K.class).partSerializer(UonSerializer.class).build().getRemote(K1.class);
		assertEquals("{a:'a1',b:'b1',c:'c1',e:'',g:'true',h:'123',i1:'foo'}",x1.getX1(new K1b()));
		assertEquals("{a:'a1',b:'b1',c:'c1',e:'',g:'\\'true\\'',h:'\\'123\\'',i1:'foo'}",x2.getX1(new K1b()));
		assertEquals("{a:'xa1x',b:'xb1x',c:'xc1x',e:'xx',g:'xtruex',h:'x123x',i1:'xfoox'}",x2.getX2(new K1b()));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// RequestBean @Query - Maps
	//-----------------------------------------------------------------------------------------------------------------

	@Remote(path="/")
	public interface K2 {
		@RemoteOp(path="/") String getX1(@Request K2a rb);
		@RemoteOp(path="/") String getX2(@Request(serializer=FakeWriterSerializer.X.class) K2a rb);
	}

	public static class K2a {
		@Query public Map<String,Object> getA() { return mapb(String.class,Object.class).add("a1","v1").add("a2",123).add("a3",null).add("a4","").build(); }
		@Query("*") public Map<String,Object> getB() { return map("b1","true","b2","123","b3","null"); }
		@Query("*") @Schema(allowEmptyValue=true) public Map<String,Object> getC() { return mapb(String.class,Object.class).add("c1","v1").add("c2",123).add("c3",null).add("c4","").build(); }
		@Query("*")
		public Map<String,Object> getD() { return null; }
	}

	@Test void k02_requestBean_maps() {
		var x1 = remote(K.class,K2.class);
		var x2 = client(K.class).partSerializer(UonSerializer.class).build().getRemote(K2.class);
		assertEquals("{a:'a1=v1,a2=123,a3=null,a4=',b1:'true',b2:'123',b3:'null',c1:'v1',c2:'123',c4:''}",x1.getX1(new K2a()));
		assertEquals("{a:'(a1=v1,a2=123,a3=null,a4=\\'\\')',b1:'\\'true\\'',b2:'\\'123\\'',b3:'\\'null\\'',c1:'v1',c2:'123',c4:''}",x2.getX1(new K2a()));
		assertEquals("{a:'x{a1=v1, a2=123, a3=null, a4=}x',b1:'xtruex',b2:'x123x',b3:'xnullx',c1:'xv1x',c2:'x123x',c4:'xx'}",x2.getX2(new K2a()));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// RequestBean @Query - NameValuePairs
	//-----------------------------------------------------------------------------------------------------------------

	@Remote(path="/")
	public interface K3 {
		@RemoteOp(path="/") String getX1(@Request K3a rb);
		@RemoteOp(path="/") String getX2(@Request(serializer=FakeWriterSerializer.X.class) K3a rb);
	}

	public static class K3a {
		@Query() @Schema(aev=true) public PartList getA() { return parts("a1","v1","a2","123","a3",null,"a4",""); }
		@Query("*") public PartList getB() { return parts("b1","true","b2","123","b3","null"); }
		@Query("*") @Schema(aev=true) public PartList getC() { return parts("c1","v1","c2","123","c3",null,"c4",""); }
		@Query("*")
		public PartList getD() { return null; }
		@Query() @Schema(aev=true) public NameValuePair[] getE() { return parts("e1","v1","e2","123","e3",null,"e4","").getAll(); }
	}

	@Test void k03_requestBean_nameValuePairs() {
		var x1 = remote(K.class,K3.class);
		var x2 = client(K.class).partSerializer(UonSerializer.class).build().getRemote(K3.class);
		assertEquals("{a1:'v1',a2:'123',a4:'',b1:'true',b2:'123',b3:'null',c1:'v1',c2:'123',c4:'',e1:'v1',e2:'123',e4:''}",x1.getX1(new K3a()));
		assertEquals("{a1:'v1',a2:'123',a4:'',b1:'true',b2:'123',b3:'null',c1:'v1',c2:'123',c4:'',e1:'v1',e2:'123',e4:''}",x2.getX1(new K3a()));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// RequestBean @Query - CharSequence
	//-----------------------------------------------------------------------------------------------------------------

	@Remote(path="/")
	public interface K4 {
		String get(@Request K4a rb);
	}

	public static class K4a {
		@Query("*") public StringBuilder getA() { return new StringBuilder("foo=bar&baz=qux"); }
	}

	@Test void k04_requestBean_charSequence() {
		var x = remote(K.class,K4.class);
		assertEquals("{foo:'bar',baz:'qux'}",x.get(new K4a()));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// RequestBean @Query - Reader
	//-----------------------------------------------------------------------------------------------------------------

	@Remote(path="/")
	public interface K5 {
		String get(@Request K5a rb);
	}

	public static class K5a {
		@Query("*") public Reader getA() { return reader("foo=bar&baz=qux"); }
	}

	@Test void k05_requestBean_reader() {
		var x = remote(K.class,K5.class);
		assertEquals("{foo:'bar',baz:'qux'}",x.get(new K5a()));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// RequestBean @Query - Collections
	//-----------------------------------------------------------------------------------------------------------------

	@Remote(path="/")
	public interface K6 {
		@RemoteOp(path="/") String getX1(@Request K6a rb);
		@RemoteOp(path="/") String getX2(@Request(serializer=FakeWriterSerializer.X.class) K6a rb);
	}

	public static class K6a {
		@Query public List<Object> getA() { return alist("foo","","true","123","null",true,123,null); }
		@Query("b") public List<Object> getX1() { return alist("foo","","true","123","null",true,123,null); }
		@Query(name="c",serializer=FakeWriterSerializer.X.class) public List<Object> getX2() { return alist("foo","","true","123","null",true,123,null); }
		@Query("d") @Schema(allowEmptyValue=true) public List<Object> getX3() { return alist(); }
		@Query("e") public List<Object> getX4() { return null; }
		@Query("f") public Object[] getX5() { return a("foo","","true","123","null",true,123,null); }
		@Query(name="g",serializer=FakeWriterSerializer.X.class) public Object[] getX6() { return a("foo","","true","123","null",true,123,null); }
		@Query("h") @Schema(allowEmptyValue=true) public Object[] getX7() { return a(); }
		@Query("i") public Object[] getX8() { return null; }
	}

	@Test void k06_requestBean_collections() {
		var x1 = remote(K.class,K6.class);
		var x2 = client(K.class).partSerializer(UonSerializer.class).build().getRemote(K6.class);
		assertEquals("{a:'foo,,true,123,null,true,123,null',b:'foo,,true,123,null,true,123,null',c:'xfoo||true|123|null|true|123|nullx',d:'',f:'foo,,true,123,null,true,123,null',g:'xfoo||true|123|null|true|123|nullx',h:''}", x1.getX1(new K6a()));
		assertEquals("{a:'@(foo,\\'\\',\\'true\\',\\'123\\',\\'null\\',true,123,null)',b:'@(foo,\\'\\',\\'true\\',\\'123\\',\\'null\\',true,123,null)',c:'xfoo||true|123|null|true|123|nullx',d:'@()',f:'@(foo,\\'\\',\\'true\\',\\'123\\',\\'null\\',true,123,null)',g:'xfoo||true|123|null|true|123|nullx',h:'@()'}", x2.getX1(new K6a()));
		assertEquals("{a:'xfoo||true|123|null|true|123|nullx',b:'xfoo||true|123|null|true|123|nullx',c:'xfoo||true|123|null|true|123|nullx',d:'xx',f:'xfoo||true|123|null|true|123|nullx',g:'xfoo||true|123|null|true|123|nullx',h:'xx'}", x2.getX2(new K6a()));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Helper methods.
	//------------------------------------------------------------------------------------------------------------------

	private static PartList parts(String...pairs) {
		return partList(pairs);
	}

	private static NameValuePair part(String key,Object val) {
		return basicPart(key,val);
	}

	private static RestClient.Builder client(Class<?> c) {
		return MockRestClient.create(c);
	}

	private static <T> T remote(Class<?> rest,Class<T> t) {
		return MockRestClient.build(rest).getRemote(t);
	}
}