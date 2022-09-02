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
package org.apache.juneau.http.remote;

import static org.apache.juneau.assertions.Assertions.*;
import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import static org.junit.runners.MethodSorters.*;
import static org.apache.juneau.http.HttpParts.*;
import static org.apache.juneau.internal.CollectionUtils.*;
import static org.apache.juneau.testutils.StreamUtils.*;

import java.io.*;
import java.math.*;
import java.util.*;
import java.util.concurrent.atomic.*;

import org.apache.http.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.http.part.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.client.*;
import org.apache.juneau.rest.mock.*;
import org.apache.juneau.rest.RestRequest;
import org.apache.juneau.testutils.*;
import org.apache.juneau.uon.*;
import org.junit.*;

@SuppressWarnings({"resource"})
@FixMethodOrder(NAME_ASCENDING)
public class Remote_QueryAnnotation_Test {

	public static class Bean {
		public int f;

		public static Bean create() {
			Bean b = new Bean();
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
	public static interface A1 {
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

	@Test
	public void a01_objectTypes() throws Exception {
		A1 x = remote(A.class,A1.class);
		assertEquals("{x:'1'}",x.getX1(1));
		assertEquals("{x:'1.0'}",x.getX2(1));
		assertEquals("{x:'f=1'}",x.getX3(Bean.create()));
		assertEquals("{f:'1'}",x.getX4(Bean.create()));
		assertEquals("{f:'1'}",x.getX5(Bean.create()));
		assertEquals("{x:'f=1,f=1'}",x.getX6(new Bean[]{Bean.create(),Bean.create()}));
		assertEquals("{x:'@((f=1),(f=1))'}",x.getX7(new Bean[]{Bean.create(),Bean.create()}));
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
	public static interface B1 {
		@RemoteOp(path="/") String getX1(@Query("x") @Schema(df="foo") String b);
		@RemoteOp(path="/") String getX2(@Query("x") @Schema(df="foo",aev=true) String b);
		@RemoteOp(path="/") String getX3(@Query("x") @Schema(df="") String b);
		@RemoteOp(path="/") String getX4(@Query("x") @Schema(df="",aev=true) String b);
	}

	@Test
	public void b01_default_aev() throws Exception {
		B1 x = remote(B.class,B1.class);
		assertEquals("{x:'foo'}",x.getX1(null));
		assertThrown(()->x.getX1("")).asMessages().isContains("Empty value not allowed.");
		assertEquals("{x:'foo'}",x.getX2(null));
		assertEquals("{x:''}",x.getX2(""));
		assertEquals("{x:''}",x.getX3(null));
		assertThrown(()->x.getX3("")).asMessages().isContains("Empty value not allowed.");
		assertEquals("{x:''}",x.getX4(null));
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
	public static interface C1 {
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

	@Test
	public void c01_collectionFormat() throws Exception {
		C1 x = remote(C.class,C1.class);
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
	public static interface D1 {
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

	@Test
	public void d01_min_max_emin_emax() throws Exception {
		D1 x = remote(D.class,D1.class);
		assertEquals("{x:'1'}",x.getX1(1));
		assertEquals("{x:'10'}",x.getX1(10));
		assertThrown(()->x.getX1(0)).asMessages().isContains("Minimum value not met.");
		assertThrown(()->x.getX1(11)).asMessages().isContains("Maximum value exceeded.");
		assertEquals("{x:'1'}",x.getX2(1));
		assertEquals("{x:'10'}",x.getX2(10));
		assertThrown(()->x.getX2(0)).asMessages().isContains("Minimum value not met.");
		assertThrown(()->x.getX2(11)).asMessages().isContains("Maximum value exceeded.");
		assertEquals("{x:'2'}",x.getX3(2));
		assertEquals("{x:'9'}",x.getX3(9));
		assertThrown(()->x.getX3(1)).asMessages().isContains("Minimum value not met.");
		assertThrown(()->x.getX3(10)).asMessages().isContains("Maximum value exceeded.");
		assertEquals("{x:'1'}",x.getX4((short)1));
		assertEquals("{x:'10'}",x.getX4((short)10));
		assertThrown(()->x.getX4((short)0)).asMessages().isContains("Minimum value not met.");
		assertThrown(()->x.getX4((short)11)).asMessages().isContains("Maximum value exceeded.");
		assertEquals("{x:'1'}",x.getX5((short)1));
		assertEquals("{x:'10'}",x.getX5((short)10));
		assertThrown(()->x.getX5((short)0)).asMessages().isContains("Minimum value not met.");
		assertThrown(()->x.getX5((short)11)).asMessages().isContains("Maximum value exceeded.");
		assertEquals("{x:'2'}",x.getX6((short)2));
		assertEquals("{x:'9'}",x.getX6((short)9));
		assertThrown(()->x.getX6((short)1)).asMessages().isContains("Minimum value not met.");
		assertThrown(()->x.getX6((short)10)).asMessages().isContains("Maximum value exceeded.");
		assertEquals("{x:'1'}",x.getX7(1l));
		assertEquals("{x:'10'}",x.getX7(10l));
		assertThrown(()->x.getX7(0l)).asMessages().isContains("Minimum value not met.");
		assertThrown(()->x.getX7(11l)).asMessages().isContains("Maximum value exceeded.");
		assertEquals("{x:'1'}",x.getX8(1l));
		assertEquals("{x:'10'}",x.getX8(10l));
		assertThrown(()->x.getX8(0l)).asMessages().isContains("Minimum value not met.");
		assertThrown(()->x.getX8(11l)).asMessages().isContains("Maximum value exceeded.");
		assertEquals("{x:'2'}",x.getX9(2l));
		assertEquals("{x:'9'}",x.getX9(9l));
		assertThrown(()->x.getX9(1l)).asMessages().isContains("Minimum value not met.");
		assertThrown(()->x.getX9(10l)).asMessages().isContains("Maximum value exceeded.");
		assertEquals("{x:'1.0'}",x.getX10(1f));
		assertEquals("{x:'10.0'}",x.getX10(10f));
		assertThrown(()->x.getX10(0.9f)).asMessages().isContains("Minimum value not met.");
		assertThrown(()->x.getX10(10.1f)).asMessages().isContains("Maximum value exceeded.");
		assertEquals("{x:'1.0'}",x.getX11(1f));
		assertEquals("{x:'10.0'}",x.getX11(10f));
		assertThrown(()->x.getX11(0.9f)).asMessages().isContains("Minimum value not met.");
		assertThrown(()->x.getX11(10.1f)).asMessages().isContains("Maximum value exceeded.");
		assertEquals("{x:'1.1'}",x.getX12(1.1f));
		assertEquals("{x:'9.9'}",x.getX12(9.9f));
		assertThrown(()->x.getX12(1f)).asMessages().isContains("Minimum value not met.");
		assertThrown(()->x.getX12(10f)).asMessages().isContains("Maximum value exceeded.");
		assertEquals("{x:'1.0'}",x.getX13(1d));
		assertEquals("{x:'10.0'}",x.getX13(10d));
		assertThrown(()->x.getX13(0.9d)).asMessages().isContains("Minimum value not met.");
		assertThrown(()->x.getX13(10.1d)).asMessages().isContains("Maximum value exceeded.");
		assertEquals("{x:'1.0'}",x.getX14(1d));
		assertEquals("{x:'10.0'}",x.getX14(10d));
		assertThrown(()->x.getX14(0.9d)).asMessages().isContains("Minimum value not met.");
		assertThrown(()->x.getX14(10.1d)).asMessages().isContains("Maximum value exceeded.");
		assertEquals("{x:'1.1'}",x.getX15(1.1d));
		assertEquals("{x:'9.9'}",x.getX15(9.9d));
		assertThrown(()->x.getX15(1d)).asMessages().isContains("Minimum value not met.");
		assertThrown(()->x.getX15(10d)).asMessages().isContains("Maximum value exceeded.");
		assertEquals("{x:'1'}",x.getX16((byte)1));
		assertEquals("{x:'10'}",x.getX16((byte)10));
		assertThrown(()->x.getX16((byte)0)).asMessages().isContains("Minimum value not met.");
		assertThrown(()->x.getX16((byte)11)).asMessages().isContains("Maximum value exceeded.");
		assertEquals("{x:'1'}",x.getX17((byte)1));
		assertEquals("{x:'10'}",x.getX17((byte)10));
		assertThrown(()->x.getX17((byte)0)).asMessages().isContains("Minimum value not met.");
		assertThrown(()->x.getX17((byte)11)).asMessages().isContains("Maximum value exceeded.");
		assertEquals("{x:'2'}",x.getX18((byte)2));
		assertEquals("{x:'9'}",x.getX18((byte)9));
		assertThrown(()->x.getX18((byte)1)).asMessages().isContains("Minimum value not met.");
		assertThrown(()->x.getX18((byte)10)).asMessages().isContains("Maximum value exceeded.");
		assertEquals("{x:'1'}",x.getX19(new AtomicInteger(1)));
		assertEquals("{x:'10'}",x.getX19(new AtomicInteger(10)));
		assertThrown(()->x.getX19(new AtomicInteger(0))).asMessages().isContains("Minimum value not met.");
		assertThrown(()->x.getX19(new AtomicInteger(11))).asMessages().isContains("Maximum value exceeded.");
		assertEquals("{x:'1'}",x.getX20(new AtomicInteger(1)));
		assertEquals("{x:'10'}",x.getX20(new AtomicInteger(10)));
		assertThrown(()->x.getX20(new AtomicInteger(0))).asMessages().isContains("Minimum value not met.");
		assertThrown(()->x.getX20(new AtomicInteger(11))).asMessages().isContains("Maximum value exceeded.");
		assertEquals("{x:'2'}",x.getX21(new AtomicInteger(2)));
		assertEquals("{x:'9'}",x.getX21(new AtomicInteger(9)));
		assertThrown(()->x.getX21(new AtomicInteger(1))).asMessages().isContains("Minimum value not met.");
		assertThrown(()->x.getX21(new AtomicInteger(10))).asMessages().isContains("Maximum value exceeded.");
		assertEquals("{x:'1'}",x.getX22(new BigDecimal(1)));
		assertEquals("{x:'10'}",x.getX22(new BigDecimal(10)));
		assertThrown(()->x.getX22(new BigDecimal(0))).asMessages().isContains("Minimum value not met.");
		assertThrown(()->x.getX22(new BigDecimal(11))).asMessages().isContains("Maximum value exceeded.");
		assertEquals("{x:'1'}",x.getX23(new BigDecimal(1)));
		assertEquals("{x:'10'}",x.getX23(new BigDecimal(10)));
		assertThrown(()->x.getX23(new BigDecimal(0))).asMessages().isContains("Minimum value not met.");
		assertThrown(()->x.getX23(new BigDecimal(11))).asMessages().isContains("Maximum value exceeded.");
		assertEquals("{x:'2'}",x.getX24(new BigDecimal(2)));
		assertEquals("{x:'9'}",x.getX24(new BigDecimal(9)));
		assertThrown(()->x.getX24(new BigDecimal(1))).asMessages().isContains("Minimum value not met.");
		assertThrown(()->x.getX24(new BigDecimal(10))).asMessages().isContains("Maximum value exceeded.");
		assertEquals("{x:'1'}",x.getX25(1));
		assertEquals("{x:'10'}",x.getX25(10));
		assertThrown(()->x.getX25(0)).asMessages().isContains("Minimum value not met.");
		assertThrown(()->x.getX25(11)).asMessages().isContains("Maximum value exceeded.");
		assertEquals("{}",x.getX25(null));
		assertEquals("{x:'1'}",x.getX26(1));
		assertEquals("{x:'10'}",x.getX26(10));
		assertThrown(()->x.getX26(0)).asMessages().isContains("Minimum value not met.");
		assertThrown(()->x.getX26(11)).asMessages().isContains("Maximum value exceeded.");
		assertEquals("{}",x.getX26(null));
		assertEquals("{x:'2'}",x.getX27(2));
		assertEquals("{x:'9'}",x.getX27(9));
		assertThrown(()->x.getX27(1)).asMessages().isContains("Minimum value not met.");
		assertThrown(()->x.getX27(10)).asMessages().isContains("Maximum value exceeded.");
		assertEquals("{}",x.getX27(null));
		assertEquals("{x:'1'}",x.getX28((short)1));
		assertEquals("{x:'10'}",x.getX28((short)10));
		assertThrown(()->x.getX28((short)0)).asMessages().isContains("Minimum value not met.");
		assertThrown(()->x.getX28((short)11)).asMessages().isContains("Maximum value exceeded.");
		assertEquals("{}",x.getX28(null));
		assertEquals("{x:'1'}",x.getX29((short)1));
		assertEquals("{x:'10'}",x.getX29((short)10));
		assertThrown(()->x.getX29((short)0)).asMessages().isContains("Minimum value not met.");
		assertThrown(()->x.getX29((short)11)).asMessages().isContains("Maximum value exceeded.");
		assertEquals("{}",x.getX29(null));
		assertEquals("{x:'2'}",x.getX30((short)2));
		assertEquals("{x:'9'}",x.getX30((short)9));
		assertThrown(()->x.getX30((short)1)).asMessages().isContains("Minimum value not met.");
		assertThrown(()->x.getX30((short)10)).asMessages().isContains("Maximum value exceeded.");
		assertEquals("{}",x.getX30(null));
		assertEquals("{x:'1'}",x.getX31(1l));
		assertEquals("{x:'10'}",x.getX31(10l));
		assertThrown(()->x.getX31(0l)).asMessages().isContains("Minimum value not met.");
		assertThrown(()->x.getX31(11l)).asMessages().isContains("Maximum value exceeded.");
		assertEquals("{}",x.getX31(null));
		assertEquals("{x:'1'}",x.getX32(1l));
		assertEquals("{x:'10'}",x.getX32(10l));
		assertThrown(()->x.getX32(0l)).asMessages().isContains("Minimum value not met.");
		assertThrown(()->x.getX32(11l)).asMessages().isContains("Maximum value exceeded.");
		assertEquals("{}",x.getX32(null));
		assertEquals("{x:'2'}",x.getX33(2l));
		assertEquals("{x:'9'}",x.getX33(9l));
		assertThrown(()->x.getX33(1l)).asMessages().isContains("Minimum value not met.");
		assertThrown(()->x.getX33(10l)).asMessages().isContains("Maximum value exceeded.");
		assertEquals("{}",x.getX33(null));
		assertEquals("{x:'1.0'}",x.getX34(1f));
		assertEquals("{x:'10.0'}",x.getX34(10f));
		assertThrown(()->x.getX34(0.9f)).asMessages().isContains("Minimum value not met.");
		assertThrown(()->x.getX34(10.1f)).asMessages().isContains("Maximum value exceeded.");
		assertEquals("{}",x.getX34(null));
		assertEquals("{x:'1.0'}",x.getX35(1f));
		assertEquals("{x:'10.0'}",x.getX35(10f));
		assertThrown(()->x.getX35(0.9f)).asMessages().isContains("Minimum value not met.");
		assertThrown(()->x.getX35(10.1f)).asMessages().isContains("Maximum value exceeded.");
		assertEquals("{}",x.getX35(null));
		assertEquals("{x:'1.1'}",x.getX36(1.1f));
		assertEquals("{x:'9.9'}",x.getX36(9.9f));
		assertThrown(()->x.getX36(1f)).asMessages().isContains("Minimum value not met.");
		assertThrown(()->x.getX36(10f)).asMessages().isContains("Maximum value exceeded.");
		assertEquals("{}",x.getX36(null));
		assertEquals("{x:'1.0'}",x.getX37(1d));
		assertEquals("{x:'10.0'}",x.getX37(10d));
		assertThrown(()->x.getX37(0.9d)).asMessages().isContains("Minimum value not met.");
		assertThrown(()->x.getX37(10.1d)).asMessages().isContains("Maximum value exceeded.");
		assertEquals("{}",x.getX37(null));
		assertEquals("{x:'1.0'}",x.getX38(1d));
		assertEquals("{x:'10.0'}",x.getX38(10d));
		assertThrown(()->x.getX38(0.9d)).asMessages().isContains("Minimum value not met.");
		assertThrown(()->x.getX38(10.1d)).asMessages().isContains("Maximum value exceeded.");
		assertEquals("{}",x.getX38(null));
		assertEquals("{x:'1.1'}",x.getX39(1.1d));
		assertEquals("{x:'9.9'}",x.getX39(9.9d));
		assertThrown(()->x.getX39(1d)).asMessages().isContains("Minimum value not met.");
		assertThrown(()->x.getX39(10d)).asMessages().isContains("Maximum value exceeded.");
		assertEquals("{}",x.getX39(null));
		assertEquals("{x:'1'}",x.getX40((byte)1));
		assertEquals("{x:'10'}",x.getX40((byte)10));
		assertThrown(()->x.getX40((byte)0)).asMessages().isContains("Minimum value not met.");
		assertThrown(()->x.getX40((byte)11)).asMessages().isContains("Maximum value exceeded.");
		assertEquals("{}",x.getX40(null));
		assertEquals("{x:'1'}",x.getX41((byte)1));
		assertEquals("{x:'10'}",x.getX41((byte)10));
		assertThrown(()->x.getX41((byte)0)).asMessages().isContains("Minimum value not met.");
		assertThrown(()->x.getX41((byte)11)).asMessages().isContains("Maximum value exceeded.");
		assertEquals("{}",x.getX41(null));
		assertEquals("{x:'2'}",x.getX42((byte)2));
		assertEquals("{x:'9'}",x.getX42((byte)9));
		assertThrown(()->x.getX42((byte)1)).asMessages().isContains("Minimum value not met.");
		assertThrown(()->x.getX42((byte)10)).asMessages().isContains("Maximum value exceeded.");
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
	public static interface E1 {
		@RemoteOp(path="/") String getX1(@Query("x") @Schema(cf="pipes",mini=1,maxi=2) String...b);
		@RemoteOp(path="/") String getX2(@Query("x") @Schema(items=@Items(cf="pipes",mini=1,maxi=2)) String[]...b);
		@RemoteOp(path="/") String getX3(@Query("x") @Schema(cf="pipes",ui=false) String...b);
		@RemoteOp(path="/") String getX4(@Query("x") @Schema(items=@Items(cf="pipes",ui=false)) String[]...b);
		@RemoteOp(path="/") String getX5(@Query("x") @Schema(cf="pipes",ui=true) String...b);
		@RemoteOp(path="/") String getX6(@Query("x") @Schema(items=@Items(cf="pipes",ui=true)) String[]...b);
	}

	@Test
	public void e01_mini_maxi_ui() throws Exception {
		E1 x = remote(E.class,E1.class);
		assertEquals("{x:'1'}",x.getX1("1"));
		assertEquals("{x:'1|2'}",x.getX1("1","2"));
		assertThrown(()->x.getX1()).asMessages().isContains("Minimum number of items not met.");
		assertThrown(()->x.getX1("1","2","3")).asMessages().isContains("Maximum number of items exceeded.");
		assertEquals("{x:null}",x.getX1((String)null));
		assertEquals("{x:'1'}",x.getX2(new String[]{"1"}));
		assertEquals("{x:'1|2'}",x.getX2(new String[]{"1","2"}));
		assertThrown(()->x.getX2(new String[]{})).asMessages().isContains("Minimum number of items not met.");
		assertThrown(()->x.getX2(new String[]{"1","2","3"})).asMessages().isContains("Maximum number of items exceeded.");
		assertEquals("{x:null}",x.getX2(new String[]{null}));
		assertEquals("{x:'1|1'}",x.getX3("1","1"));
		assertEquals("{x:'1|1'}",x.getX4(new String[]{"1","1"}));
		assertEquals("{x:'1|2'}",x.getX5("1","2"));
		assertThrown(()->x.getX5("1","1")).asMessages().isContains("Duplicate items not allowed.");
		assertEquals("{x:'1|2'}",x.getX6(new String[]{"1","2"}));
		assertThrown(()->x.getX6(new String[]{"1","1"})).asMessages().isContains("Duplicate items not allowed.");
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
	public static interface F1 {
		@RemoteOp(path="/") String getX1(@Query("x") @Schema(minl=2,maxl=3) String b);
		@RemoteOp(path="/") String getX2(@Query("x") @Schema(cf="pipes",items=@Items(minl=2,maxl=3)) String...b);
		@RemoteOp(path="/") String getX3(@Query("x") @Schema(e={"foo"}) String b);
		@RemoteOp(path="/") String getX4(@Query("x") @Schema(cf="pipes",items=@Items(e={"foo"})) String...b);
		@RemoteOp(path="/") String getX5(@Query("x") @Schema(p="foo\\d{1,3}") String b);
		@RemoteOp(path="/") String getX6(@Query("x") @Schema(cf="pipes",items=@Items(p="foo\\d{1,3}")) String...b);
	}

	@Test
	public void f01_minl_maxl_enum() throws Exception {
		F1 x = remote(F.class,F1.class);
		assertEquals("{x:'12'}",x.getX1("12"));
		assertEquals("{x:'123'}",x.getX1("123"));
		assertThrown(()->x.getX1("1")).asMessages().isContains("Minimum length of value not met.");
		assertThrown(()->x.getX1("1234")).asMessages().isContains("Maximum length of value exceeded.");
		assertEquals("{}",x.getX1(null));
		assertEquals("{x:'12|34'}",x.getX2("12","34"));
		assertEquals("{x:'123|456'}",x.getX2("123","456"));
		assertThrown(()->x.getX2("1","2")).asMessages().isContains("Minimum length of value not met.");
		assertThrown(()->x.getX2("1234","5678")).asMessages().isContains("Maximum length of value exceeded.");
		assertEquals("{x:'12|null'}",x.getX2("12",null));
		assertEquals("{x:'foo'}",x.getX3("foo"));
		assertThrown(()->x.getX3("bar")).asMessages().isContains("Value does not match one of the expected values.  Must be one of the following:  foo");
		assertEquals("{}",x.getX3(null));
		assertEquals("{x:'foo'}",x.getX4("foo"));
		assertThrown(()->x.getX4("bar")).asMessages().isContains("Value does not match one of the expected values.  Must be one of the following:  foo");
		assertEquals("{x:null}",x.getX4((String)null));
		assertEquals("{x:'foo123'}",x.getX5("foo123"));
		assertThrown(()->x.getX5("bar")).asMessages().isContains("Value does not match expected pattern.  Must match pattern: foo\\d{1,3}");
		assertEquals("{}",x.getX5(null));
		assertEquals("{x:'foo123'}",x.getX6("foo123"));
		assertThrown(()->x.getX6("foo")).asMessages().isContains("Value does not match expected pattern.  Must match pattern: foo\\d{1,3}");
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
	public static interface G1 {
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

	@Test
	public void g01_multipleOf() throws Exception {
		G1 x = remote(G.class,G1.class);
		assertEquals("{x:'4'}",x.getX1(4));
		assertThrown(()->x.getX1(5)).asMessages().isContains("Multiple-of not met.");
		assertEquals("{x:'4'}",x.getX2((short)4));
		assertThrown(()->x.getX2((short)5)).asMessages().isContains("Multiple-of not met.");
		assertEquals("{x:'4'}",x.getX3(4));
		assertThrown(()->x.getX3(5)).asMessages().isContains("Multiple-of not met.");
		assertEquals("{x:'4.0'}",x.getX4(4));
		assertThrown(()->x.getX4(5)).asMessages().isContains("Multiple-of not met.");
		assertEquals("{x:'4.0'}",x.getX5(4));
		assertThrown(()->x.getX5(5)).asMessages().isContains("Multiple-of not met.");
		assertEquals("{x:'4'}",x.getX6((byte)4));
		assertThrown(()->x.getX6((byte)5)).asMessages().isContains("Multiple-of not met.");
		assertEquals("{x:'4'}",x.getX7(new AtomicInteger(4)));
		assertThrown(()->x.getX7(new AtomicInteger(5))).asMessages().isContains("Multiple-of not met.");
		assertEquals("{x:'4'}",x.getX8(new BigDecimal(4)));
		assertThrown(()->x.getX8(new BigDecimal(5))).asMessages().isContains("Multiple-of not met.");
		assertEquals("{x:'4'}",x.getX9(4));
		assertThrown(()->x.getX9(5)).asMessages().isContains("Multiple-of not met.");
		assertEquals("{x:'4'}",x.getX10((short)4));
		assertThrown(()->x.getX10((short)5)).asMessages().isContains("Multiple-of not met.");
		assertEquals("{x:'4'}",x.getX11(4l));
		assertThrown(()->x.getX11(5l)).asMessages().isContains("Multiple-of not met.");
		assertEquals("{x:'4.0'}",x.getX12(4f));
		assertThrown(()->x.getX12(5f)).asMessages().isContains("Multiple-of not met.");
		assertEquals("{x:'4.0'}",x.getX13(4d));
		assertThrown(()->x.getX13(5d)).asMessages().isContains("Multiple-of not met.");
		assertEquals("{x:'4'}",x.getX14((byte)4));
		assertThrown(()->x.getX14((byte)5)).asMessages().isContains("Multiple-of not met.");
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
	public static interface H1 {
		@RemoteOp(path="/") String getX1(@Query("x") String b);
		@RemoteOp(path="/") String getX2(@Query("x") @Schema(r=false) String b);
		@RemoteOp(path="/") String getX3(@Query("x") @Schema(r=true) String b);
	}

	@Test
	public void h01_required() throws Exception {
		H1 x = remote(H.class,H1.class);
		assertEquals("{}",x.getX1(null));
		assertEquals("{}",x.getX2(null));
		assertEquals("{x:'1'}",x.getX3("1"));
		assertThrown(()->x.getX3(null)).asMessages().isContains("Required value not provided.");
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
	public static interface I1 {
		@RemoteOp(path="/") String getX1(@Query("x") @Schema(aev=true) String b);
		@RemoteOp(path="/") String getX2(@Query("x") @Schema(aev=true,sie=false) String b);
		@RemoteOp(path="/") String getX3(@Query("x") @Schema(sie=true) String b);
	}

	@Test
	public void i01_skipIfEmpty() throws Exception {
		I1 x = remote(I.class,I1.class);
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
	public static interface J1 {
		@RemoteOp(path="/") String getX1(@Query(name="x",serializer=MockWriterSerializer.X.class) String b);
	}

	@Test
	public void j01_serializer() throws Exception {
		J1 x = remote(J.class,J1.class);
		assertEquals("{x:'xXx'}",x.getX1("X"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// RequestBean @Query - Return types
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class K {
		@RestOp
		public String get(RestRequest req) throws Exception {
			return req.getQueryParams().toString();
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// RequestBean @Query - Simple values
	//-----------------------------------------------------------------------------------------------------------------

	@Remote(path="/")
	public static interface K1 {
		@RemoteOp(path="/") String getX1(@Request K1b rb);
		@RemoteOp(path="/") String getX2(@Request(serializer=MockWriterSerializer.X.class) K1b rb);
	}

	public static interface K1a {
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

	@Test
	public void k01_requestBean_simpleVals() throws Exception {
		K1 x1 = remote(K.class,K1.class);
		K1 x2 = client(K.class).partSerializer(UonSerializer.class).build().getRemote(K1.class);
		assertEquals("{a:'a1',b:'b1',c:'c1',e:'',g:'true',h:'123',i1:'foo'}",x1.getX1(new K1b()));
		assertEquals("{a:'a1',b:'b1',c:'c1',e:'',g:'\\'true\\'',h:'\\'123\\'',i1:'foo'}",x2.getX1(new K1b()));
		assertEquals("{a:'xa1x',b:'xb1x',c:'xc1x',e:'xx',g:'xtruex',h:'x123x',i1:'xfoox'}",x2.getX2(new K1b()));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// RequestBean @Query - Maps
	//-----------------------------------------------------------------------------------------------------------------

	@Remote(path="/")
	public static interface K2 {
		@RemoteOp(path="/") String getX1(@Request K2a rb);
		@RemoteOp(path="/") String getX2(@Request(serializer=MockWriterSerializer.X.class) K2a rb);
	}

	public static class K2a {
		@Query
		public Map<String,Object> getA() {
			return mapBuilder(String.class,Object.class).add("a1","v1").add("a2",123).add("a3",null).add("a4","").build();
		}
		@Query("*")
		public Map<String,Object> getB() {
			return map("b1","true","b2","123","b3","null");
		}
		@Query("*") @Schema(allowEmptyValue=true)
		public Map<String,Object> getC() {
			return mapBuilder(String.class,Object.class).add("c1","v1").add("c2",123).add("c3",null).add("c4","").build();
		}
		@Query("*")
		public Map<String,Object> getD() {
			return null;
		}
	}

	@Test
	public void k02_requestBean_maps() throws Exception {
		K2 x1 = remote(K.class,K2.class);
		K2 x2 = client(K.class).partSerializer(UonSerializer.class).build().getRemote(K2.class);
		assertEquals("{a:'a1=v1,a2=123,a3=null,a4=',b1:'true',b2:'123',b3:'null',c1:'v1',c2:'123',c4:''}",x1.getX1(new K2a()));
		assertEquals("{a:'(a1=v1,a2=123,a3=null,a4=\\'\\')',b1:'\\'true\\'',b2:'\\'123\\'',b3:'\\'null\\'',c1:'v1',c2:'123',c4:''}",x2.getX1(new K2a()));
		assertEquals("{a:'x{a1=v1, a2=123, a3=null, a4=}x',b1:'xtruex',b2:'x123x',b3:'xnullx',c1:'xv1x',c2:'x123x',c4:'xx'}",x2.getX2(new K2a()));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// RequestBean @Query - NameValuePairs
	//-----------------------------------------------------------------------------------------------------------------

	@Remote(path="/")
	public static interface K3 {
		@RemoteOp(path="/") String getX1(@Request K3a rb);
		@RemoteOp(path="/") String getX2(@Request(serializer=MockWriterSerializer.X.class) K3a rb);
	}

	public static class K3a {
		@Query() @Schema(aev=true)
		public PartList getA() {
			return parts("a1","v1","a2","123","a3",null,"a4","");
		}
		@Query("*")
		public PartList getB() {
			return parts("b1","true","b2","123","b3","null");
		}
		@Query("*") @Schema(aev=true)
		public PartList getC() {
			return parts("c1","v1","c2","123","c3",null,"c4","");
		}
		@Query("*")
		public PartList getD() {
			return null;
		}
		@Query() @Schema(aev=true)
		public NameValuePair[] getE() {
			return parts("e1","v1","e2","123","e3",null,"e4","").getAll();
		}
	}

	@Test
	public void k03_requestBean_nameValuePairs() throws Exception {
		K3 x1 = remote(K.class,K3.class);
		K3 x2 = client(K.class).partSerializer(UonSerializer.class).build().getRemote(K3.class);
		assertEquals("{a1:'v1',a2:'123',a4:'',b1:'true',b2:'123',b3:'null',c1:'v1',c2:'123',c4:'',e1:'v1',e2:'123',e4:''}",x1.getX1(new K3a()));
		assertEquals("{a1:'v1',a2:'123',a4:'',b1:'true',b2:'123',b3:'null',c1:'v1',c2:'123',c4:'',e1:'v1',e2:'123',e4:''}",x2.getX1(new K3a()));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// RequestBean @Query - CharSequence
	//-----------------------------------------------------------------------------------------------------------------

	@Remote(path="/")
	public static interface K4 {
		String get(@Request K4a rb);
	}

	public static class K4a {
		@Query("*")
		public StringBuilder getA() {
			return new StringBuilder("foo=bar&baz=qux");
		}
	}

	@Test
	public void k04_requestBean_charSequence() throws Exception {
		K4 x = remote(K.class,K4.class);
		assertEquals("{foo:'bar',baz:'qux'}",x.get(new K4a()));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// RequestBean @Query - Reader
	//-----------------------------------------------------------------------------------------------------------------

	@Remote(path="/")
	public static interface K5 {
		String get(@Request K5a rb);
	}

	public static class K5a {
		@Query("*")
		public Reader getA() {
			return reader("foo=bar&baz=qux");
		}
	}

	@Test
	public void k05_requestBean_reader() throws Exception {
		K5 x = remote(K.class,K5.class);
		assertEquals("{foo:'bar',baz:'qux'}",x.get(new K5a()));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// RequestBean @Query - Collections
	//-----------------------------------------------------------------------------------------------------------------

	@Remote(path="/")
	public static interface K6 {
		@RemoteOp(path="/") String getX1(@Request K6a rb);
		@RemoteOp(path="/") String getX2(@Request(serializer=MockWriterSerializer.X.class) K6a rb);
	}

	public static class K6a {
		@Query
		public List<Object> getA() {
			return alist("foo","","true","123","null",true,123,null);
		}
		@Query("b")
		public List<Object> getX1() {
			return alist("foo","","true","123","null",true,123,null);
		}
		@Query(name="c",serializer=MockWriterSerializer.X.class)
		public List<Object> getX2() {
			return alist("foo","","true","123","null",true,123,null);
		}
		@Query("d") @Schema(allowEmptyValue=true)
		public List<Object> getX3() {
			return alist();
		}
		@Query("e")
		public List<Object> getX4() {
			return null;
		}
		@Query("f")
		public Object[] getX5() {
			return new Object[]{"foo","","true","123","null",true,123,null};
		}
		@Query(name="g",serializer=MockWriterSerializer.X.class)
		public Object[] getX6() {
			return new Object[]{"foo","","true","123","null",true,123,null};
		}
		@Query("h") @Schema(allowEmptyValue=true)
		public Object[] getX7() {
			return new Object[]{};
		}
		@Query("i")
		public Object[] getX8() {
			return null;
		}
	}

	@Test
	public void k06_requestBean_collections() throws Exception {
		K6 x1 = remote(K.class,K6.class);
		K6 x2 = client(K.class).partSerializer(UonSerializer.class).build().getRemote(K6.class);
		assertString(x1.getX1(new K6a())).is("{a:'foo,,true,123,null,true,123,null',b:'foo,,true,123,null,true,123,null',c:'xfoo||true|123|null|true|123|nullx',d:'',f:'foo,,true,123,null,true,123,null',g:'xfoo||true|123|null|true|123|nullx',h:''}");
		assertString(x2.getX1(new K6a())).is("{a:'@(foo,\\'\\',\\'true\\',\\'123\\',\\'null\\',true,123,null)',b:'@(foo,\\'\\',\\'true\\',\\'123\\',\\'null\\',true,123,null)',c:'xfoo||true|123|null|true|123|nullx',d:'@()',f:'@(foo,\\'\\',\\'true\\',\\'123\\',\\'null\\',true,123,null)',g:'xfoo||true|123|null|true|123|nullx',h:'@()'}");
		assertString(x2.getX2(new K6a())).is("{a:'xfoo||true|123|null|true|123|nullx',b:'xfoo||true|123|null|true|123|nullx',c:'xfoo||true|123|null|true|123|nullx',d:'xx',f:'xfoo||true|123|null|true|123|nullx',g:'xfoo||true|123|null|true|123|nullx',h:'xx'}");
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
