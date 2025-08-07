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

import static org.apache.juneau.http.HttpParts.*;
import static org.apache.juneau.internal.CollectionUtils.*;
import static org.apache.juneau.utest.utils.Utils2.*;
import static org.junit.Assert.*;
import java.io.*;
import java.math.*;
import java.util.*;
import java.util.concurrent.atomic.*;

import org.apache.http.*;
import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.http.annotation.Header;
import org.apache.juneau.http.part.*;
import org.apache.juneau.rest.RestRequest;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.client.*;
import org.apache.juneau.rest.mock.*;
import org.apache.juneau.uon.*;
import org.apache.juneau.urlencoding.*;
import org.apache.juneau.utest.utils.*;
import org.junit.jupiter.api.*;

class Remote_FormDataAnnotation_Test extends SimpleTestBase {

	public static class Bean {
		public int f;
		public static Bean create() {
			Bean b = new Bean();
			b.f = 1;
			return b;
		}
	}

	public static class Bean2 extends Bean {
		public static Bean2 create() {
			Bean2 b = new Bean2();
			b.f = 1;
			return b;
		}
		@Override
		public String toString() {
			return UrlEncodingSerializer.DEFAULT.toString(this);
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Basic tests
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class A {
		@RestPost
		public String a(@FormData("*") JsonMap m, @Header("Content-Type") String ct) {
			assertEquals("application/x-www-form-urlencoded", ct);
			return m.toString();
		}
	}

	@Remote
	public interface A1 {
		@RemotePost(path="a") String x1(@FormData("x") int b);
		@RemotePost(path="a") String x2(@FormData("x") float b);
		@RemotePost(path="a") String x3(@FormData("x") Bean b);
		@RemotePost(path="a") String x4(@FormData("*") Bean b);
		@RemotePost(path="a") String x5(@FormData Bean b);
		@RemotePost(path="a") String x6(@FormData("x") Bean[] b);
		@RemotePost(path="a") String x7(@FormData("x") @Schema(cf="uon") Bean[] b);
		@RemotePost(path="a") String x8(@FormData("x") List<Bean> b);
		@RemotePost(path="a") String x9(@FormData("x") @Schema(cf="uon") List<Bean> b);
		@RemotePost(path="a") String x10(@FormData("x") Map<String,Bean> b);
		@RemotePost(path="a") String x11(@FormData("*") Map<String,Bean> b);
		@RemotePost(path="a") String x12(@FormData Map<String,Bean> b);
		@RemotePost(path="a") String x13(@FormData("x") @Schema(f="uon") Map<String,Bean> b);
		@RemotePost(path="a") String x14(@FormData @Schema(f="uon") Map<String,Bean> b);
		@RemotePost(path="a") String x15(@FormData("*") Reader b);
		@RemotePost(path="a") String x16(@FormData Reader b);
		@RemotePost(path="a") String x17(@FormData("*") InputStream b);
		@RemotePost(path="a") String x18(@FormData InputStream b);
		@RemotePost(path="a") String x19(@FormData("*") PartList b);
		@RemotePost(path="a") String x20(@FormData PartList b);
		@RemotePost(path="a") String x21(@FormData NameValuePair b);
		@RemotePost(path="a") String x22(@FormData String b);
		@RemotePost(path="a") String x23(@FormData InputStream b);
		@RemotePost(path="a") String x24(@FormData Reader b);
		@RemotePost(path="a") String x25(@FormData Bean2 b);
		@RemotePost(path="a") String x26(@FormData List<NameValuePair> b);
	}

	@Test void a01_objectTypes() {
		A1 x = MockRestClient.build(A.class).getRemote(A1.class);
		assertEquals("{x:'1'}",x.x1(1));
		assertEquals("{x:'1.0'}",x.x2(1));
		assertEquals("{x:'f=1'}",x.x3(Bean.create()));
		assertEquals("{f:'1'}",x.x4(Bean.create()));
		assertEquals("{f:'1'}",x.x5(Bean.create()));
		assertEquals("{x:'f=1,f=1'}",x.x6(new Bean[]{Bean.create(),Bean.create()}));
		assertEquals("{x:'@((f=1),(f=1))'}",x.x7(new Bean[]{Bean.create(),Bean.create()}));
		assertEquals("{x:'f=1,f=1'}",x.x8(alist(Bean.create(),Bean.create())));
		assertEquals("{x:'@((f=1),(f=1))'}",x.x9(alist(Bean.create(),Bean.create())));
		assertEquals("{x:'k1=f\\\\=1'}",x.x10(map("k1",Bean.create())));
		assertEquals("{k1:'f=1'}",x.x11(map("k1",Bean.create())));
		assertEquals("{k1:'f=1'}",x.x12(map("k1",Bean.create())));
		assertEquals("{x:'k1=f\\\\=1'}",x.x13(map("k1",Bean.create())));
		assertEquals("{k1:'f=1'}",x.x14(map("k1",Bean.create())));
		assertEquals("{x:'1'}",x.x15(reader("x=1")));
		assertEquals("{x:'1'}",x.x16(reader("x=1")));
		assertEquals("{x:'1'}",x.x17(inputStream("x=1")));
		assertEquals("{x:'1'}",x.x18(inputStream("x=1")));
		assertEquals("{foo:'bar'}",x.x19(parts("foo","bar")));
		assertEquals("{foo:'bar'}",x.x20(parts("foo","bar")));
		assertEquals("{foo:'bar'}",x.x21(part("foo","bar")));
		assertEquals("{foo:'bar'}",x.x22("foo=bar"));
		assertEquals("{}",x.x22(null));
		assertEquals("{foo:'bar'}",x.x23(inputStream("foo=bar")));
		assertEquals("{foo:'bar'}",x.x24(reader("foo=bar")));
		assertEquals("{f:'1'}",x.x25(Bean2.create()));
		assertEquals("{foo:'bar'}",x.x26(alist(part("foo","bar"))));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// @FormData(_default/allowEmptyValue)
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class B {
		@RestOp
		public String post(@FormData("*") JsonMap m) {
			return m.toString();
		}
	}

	@Remote
	public interface B1 {
		@RemoteOp(path="/") String postX1(@FormData("x") @Schema(df="foo") String b);
		@RemoteOp(path="/") String postX2(@FormData("x") @Schema(df="foo",aev=true) String b);
		@RemoteOp(path="/") String postX3(@FormData("x") @Schema(df="") String b);
		@RemoteOp(path="/") String postX4(@FormData("x") @Schema(df="",aev=true) String b);
	}

	@Test void b01_default_allowEmptyValue() {
		B1 x = remote(B.class,B1.class);
		assertEquals("{x:'foo'}",x.postX1(null));
		assertThrowsWithMessage(Exception.class, "Empty value not allowed.", ()->x.postX1(""));
		assertEquals("{x:'foo'}",x.postX2(null));
		assertEquals("{x:''}",x.postX2(""));
		assertEquals("{x:''}",x.postX3(null));
		assertThrowsWithMessage(Exception.class, "Empty value not allowed.", ()->x.postX3(""));
		assertEquals("{x:''}",x.postX4(null));
		assertEquals("{x:''}",x.postX4(""));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// @FormData(collectionFormat)
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class C {
		@RestPost
		public String a(@FormData("*") JsonMap m) {
			return m.toString();
		}
		@RestPost
		public Reader b(@Content Reader b) {
			return b;
		}
	}

	@Remote
	public interface C1 {
		@RemoteOp(path="/a") String postX1(@FormData("x") String...b);
		@RemoteOp(path="/b") String postX2(@FormData("x") String...b);
		@RemoteOp(path="/a") String postX3(@FormData("x") @Schema(cf="csv") String...b);
		@RemoteOp(path="/b") String postX4(@FormData("x") @Schema(cf="csv") String...b);
		@RemoteOp(path="/a") String postX5(@FormData("x") @Schema(cf="ssv") String...b);
		@RemoteOp(path="/b") String postX6(@FormData("x") @Schema(cf="ssv") String...b);
		@RemoteOp(path="/a") String postX7(@FormData("x") @Schema(cf="tsv") String...b);
		@RemoteOp(path="/b") String postX8(@FormData("x") @Schema(cf="tsv") String...b);
		@RemoteOp(path="/a") String postX9(@FormData("x") @Schema(cf="pipes") String...b);
		@RemoteOp(path="/b") String postX10(@FormData("x") @Schema(cf="pipes") String...b);
		@RemoteOp(path="/a") String postX11(@FormData("x") @Schema(cf="multi") String...b); // Not supported,but should be treated as csv.
		@RemoteOp(path="/b") String postX12(@FormData("x") @Schema(cf="multi") String...b); // Not supported,but should be treated as csv.
		@RemoteOp(path="/a") String postX13(@FormData("x") @Schema(cf="uon") String...b);
		@RemoteOp(path="/b") String postX14(@FormData("x") @Schema(cf="uon") String...b);
	}

	@Test void c01_collectionFormat() {
		C1 x = remote(C.class,C1.class);
		assertEquals("{x:'foo,bar'}",x.postX1("foo","bar"));
		assertEquals("x=foo%2Cbar",x.postX2("foo","bar"));
		assertEquals("{x:'foo,bar'}",x.postX3("foo","bar"));
		assertEquals("x=foo%2Cbar",x.postX4("foo","bar"));
		assertEquals("{x:'foo bar'}",x.postX5("foo","bar"));
		assertEquals("x=foo+bar",x.postX6("foo","bar"));
		assertEquals("{x:'foo\\tbar'}",x.postX7("foo","bar"));
		assertEquals("x=foo%09bar",x.postX8("foo","bar"));
		assertEquals("{x:'foo|bar'}",x.postX9("foo","bar"));
		assertEquals("x=foo%7Cbar",x.postX10("foo","bar"));
		assertEquals("{x:'foo,bar'}",x.postX11("foo","bar"));
		assertEquals("x=foo%2Cbar",x.postX12("foo","bar"));
		assertEquals("{x:'@(foo,bar)'}",x.postX13("foo","bar"));
		assertEquals("x=%40%28foo%2Cbar%29",x.postX14("foo","bar"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// @FormData(maximum,exclusiveMaximum,minimum,exclusiveMinimum)
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class D {
		@RestOp
		public String post(@FormData("*") JsonMap m) {
			return m.toString();
		}
	}

	@Remote
	public interface D1 {
		@RemoteOp(path="/") String postX1(@FormData("x") @Schema(min="1",max="10") int b);
		@RemoteOp(path="/") String postX2(@FormData("x") @Schema(min="1",max="10",emin=false,emax=false) int b);
		@RemoteOp(path="/") String postX3(@FormData("x") @Schema(min="1",max="10",emin=true,emax=true) int b);
		@RemoteOp(path="/") String postX4(@FormData("x") @Schema(min="1",max="10",emin=true,emax=true) int b);
		@RemoteOp(path="/") String postX5(@FormData("x") @Schema(min="1",max="10") short b);
		@RemoteOp(path="/") String postX6(@FormData("x") @Schema(min="1",max="10",emin=false,emax=false) short b);
		@RemoteOp(path="/") String postX7(@FormData("x") @Schema(min="1",max="10",emin=true,emax=true) short b);
		@RemoteOp(path="/") String postX8(@FormData("x") @Schema(min="1",max="10") long b);
		@RemoteOp(path="/") String postX9(@FormData("x") @Schema(min="1",max="10",emin=false,emax=false) long b);
		@RemoteOp(path="/") String postX10(@FormData("x") @Schema(min="1",max="10",emin=true,emax=true) long b);
		@RemoteOp(path="/") String postX11(@FormData("x") @Schema(min="1",max="10") float b);
		@RemoteOp(path="/") String postX12(@FormData("x") @Schema(min="1",max="10",emin=false,emax=false) float b);
		@RemoteOp(path="/") String postX13(@FormData("x") @Schema(min="1",max="10",emin=true,emax=true) float b);
		@RemoteOp(path="/") String postX14(@FormData("x") @Schema(min="1",max="10") double b);
		@RemoteOp(path="/") String postX15(@FormData("x") @Schema(min="1",max="10",emin=false,emax=false) double b);
		@RemoteOp(path="/") String postX16(@FormData("x") @Schema(min="1",max="10",emin=true,emax=true) double b);
		@RemoteOp(path="/") String postX17(@FormData("x") @Schema(min="1",max="10") byte b);
		@RemoteOp(path="/") String postX18(@FormData("x") @Schema(min="1",max="10",emin=false,emax=false) byte b);
		@RemoteOp(path="/") String postX19(@FormData("x") @Schema(min="1",max="10",emin=true,emax=true) byte b);
		@RemoteOp(path="/") String postX20(@FormData("x") @Schema(min="1",max="10") AtomicInteger b);
		@RemoteOp(path="/") String postX21(@FormData("x") @Schema(min="1",max="10",emin=false,emax=false) AtomicInteger b);
		@RemoteOp(path="/") String postX22(@FormData("x") @Schema(min="1",max="10",emin=true,emax=true) AtomicInteger b);
		@RemoteOp(path="/") String postX23(@FormData("x") @Schema(min="1",max="10") BigDecimal b);
		@RemoteOp(path="/") String postX24(@FormData("x") @Schema(min="1",max="10",emin=false,emax=false) BigDecimal b);
		@RemoteOp(path="/") String postX25(@FormData("x") @Schema(min="1",max="10",emin=true,emax=true) BigDecimal b);
		@RemoteOp(path="/") String postX26(@FormData("x") @Schema(min="1",max="10") Integer b);
		@RemoteOp(path="/") String postX27(@FormData("x") @Schema(min="1",max="10",emin=false,emax=false) Integer b);
		@RemoteOp(path="/") String postX28(@FormData("x") @Schema(min="1",max="10",emin=true,emax=true) Integer b);
		@RemoteOp(path="/") String postX29(@FormData("x") @Schema(min="1",max="10") Short b);
		@RemoteOp(path="/") String postX30(@FormData("x") @Schema(min="1",max="10",emin=false,emax=false) Short b);
		@RemoteOp(path="/") String postX31(@FormData("x") @Schema(min="1",max="10",emin=true,emax=true) Short b);
		@RemoteOp(path="/") String postX32(@FormData("x") @Schema(min="1",max="10") Long b);
		@RemoteOp(path="/") String postX33(@FormData("x") @Schema(min="1",max="10",emin=false,emax=false) Long b);
		@RemoteOp(path="/") String postX34(@FormData("x") @Schema(min="1",max="10",emin=true,emax=true) Long b);
		@RemoteOp(path="/") String postX35(@FormData("x") @Schema(min="1",max="10") Float b);
		@RemoteOp(path="/") String postX36(@FormData("x") @Schema(min="1",max="10",emin=false,emax=false) Float b);
		@RemoteOp(path="/") String postX37(@FormData("x") @Schema(min="1",max="10",emin=true,emax=true) Float b);
		@RemoteOp(path="/") String postX38(@FormData("x") @Schema(min="1",max="10") Double b);
		@RemoteOp(path="/") String postX39(@FormData("x") @Schema(min="1",max="10",emin=false,emax=false) Double b);
		@RemoteOp(path="/") String postX40(@FormData("x") @Schema(min="1",max="10",emin=true,emax=true) Double b);
		@RemoteOp(path="/") String postX41(@FormData("x") @Schema(min="1",max="10") Byte b);
		@RemoteOp(path="/") String postX42(@FormData("x") @Schema(min="1",max="10",emin=false,emax=false) Byte b);
		@RemoteOp(path="/") String postX43(@FormData("x") @Schema(min="1",max="10",emin=true,emax=true) Byte b);
	}

	@Test void d01_min_max_emin_emax() {
		D1 x = remote(D.class,D1.class);
		assertEquals("{x:'1'}",x.postX1(1));
		assertEquals("{x:'10'}",x.postX1(10));
		assertThrowsWithMessage(Exception.class, "Minimum value not met.", ()->x.postX1(0));
		assertThrowsWithMessage(Exception.class, "Maximum value exceeded.", ()->x.postX1(11));
		assertEquals("{x:'1'}",x.postX2(1));
		assertEquals("{x:'10'}",x.postX2(10));
		assertThrowsWithMessage(Exception.class, "Minimum value not met.", ()->x.postX2(0));
		assertThrowsWithMessage(Exception.class, "Maximum value exceeded.", ()->x.postX2(11));
		assertEquals("{x:'2'}",x.postX3(2));
		assertEquals("{x:'9'}",x.postX3(9));
		assertThrowsWithMessage(Exception.class, "Minimum value not met.", ()->x.postX3(1));
		assertThrowsWithMessage(Exception.class, "Maximum value exceeded.", ()->x.postX3(10));
		assertEquals("{x:'1'}",x.postX5((short)1));
		assertEquals("{x:'10'}",x.postX5((short)10));
		assertThrowsWithMessage(Exception.class, "Minimum value not met.", ()->x.postX5((short)0));
		assertThrowsWithMessage(Exception.class, "Maximum value exceeded.", ()->x.postX5((short)11));
		assertEquals("{x:'1'}",x.postX6((short)1));
		assertEquals("{x:'10'}",x.postX6((short)10));
		assertThrowsWithMessage(Exception.class, "Minimum value not met.", ()->x.postX6((short)0));
		assertThrowsWithMessage(Exception.class, "Maximum value exceeded.", ()->x.postX6((short)11));
		assertEquals("{x:'2'}",x.postX7((short)2));
		assertEquals("{x:'9'}",x.postX7((short)9));
		assertThrowsWithMessage(Exception.class, "Minimum value not met.", ()->x.postX7((short)1));
		assertThrowsWithMessage(Exception.class, "Maximum value exceeded.", ()->x.postX7((short)10));
		assertEquals("{x:'1'}",x.postX8(1L));
		assertEquals("{x:'10'}",x.postX8(10L));
		assertThrowsWithMessage(Exception.class, "Minimum value not met.", ()->x.postX8(0L));
		assertThrowsWithMessage(Exception.class, "Maximum value exceeded.", ()->x.postX8(11L));
		assertEquals("{x:'1'}",x.postX9(1L));
		assertEquals("{x:'10'}",x.postX9(10L));
		assertThrowsWithMessage(Exception.class, "Minimum value not met.", ()->x.postX9(0L));
		assertThrowsWithMessage(Exception.class, "Maximum value exceeded.", ()->x.postX9(11L));
		assertEquals("{x:'2'}",x.postX10(2L));
		assertEquals("{x:'9'}",x.postX10(9L));
		assertThrowsWithMessage(Exception.class, "Minimum value not met.", ()->x.postX10(1L));
		assertThrowsWithMessage(Exception.class, "Maximum value exceeded.", ()->x.postX10(10L));
		assertEquals("{x:'1.0'}",x.postX11(1f));
		assertEquals("{x:'10.0'}",x.postX11(10f));
		assertThrowsWithMessage(Exception.class, "Minimum value not met.", ()->x.postX11(0.9f));
		assertThrowsWithMessage(Exception.class, "Maximum value exceeded.", ()->x.postX11(10.1f));
		assertEquals("{x:'1.0'}",x.postX12(1f));
		assertEquals("{x:'10.0'}",x.postX12(10f));
		assertThrowsWithMessage(Exception.class, "Minimum value not met.", ()->x.postX12(0.9f));
		assertThrowsWithMessage(Exception.class, "Maximum value exceeded.", ()->x.postX12(10.1f));
		assertEquals("{x:'1.1'}",x.postX13(1.1f));
		assertEquals("{x:'9.9'}",x.postX13(9.9f));
		assertThrowsWithMessage(Exception.class, "Minimum value not met.", ()->x.postX13(1f));
		assertThrowsWithMessage(Exception.class, "Maximum value exceeded.", ()->x.postX13(10f));
		assertEquals("{x:'1.0'}",x.postX14(1d));
		assertEquals("{x:'10.0'}",x.postX14(10d));
		assertThrowsWithMessage(Exception.class, "Minimum value not met.", ()->x.postX14(0.9d));
		assertThrowsWithMessage(Exception.class, "Maximum value exceeded.", ()->x.postX14(10.1d));
		assertEquals("{x:'1.0'}",x.postX15(1d));
		assertEquals("{x:'10.0'}",x.postX15(10d));
		assertThrowsWithMessage(Exception.class, "Minimum value not met.", ()->x.postX15(0.9d));
		assertThrowsWithMessage(Exception.class, "Maximum value exceeded.", ()->x.postX15(10.1d));
		assertEquals("{x:'1.1'}",x.postX16(1.1d));
		assertEquals("{x:'9.9'}",x.postX16(9.9d));
		assertThrowsWithMessage(Exception.class, "Minimum value not met.", ()->x.postX16(1d));
		assertThrowsWithMessage(Exception.class, "Maximum value exceeded.", ()->x.postX16(10d));
		assertEquals("{x:'1'}",x.postX17((byte)1));
		assertEquals("{x:'10'}",x.postX17((byte)10));
		assertThrowsWithMessage(Exception.class, "Minimum value not met.", ()->x.postX17((byte)0));
		assertThrowsWithMessage(Exception.class, "Maximum value exceeded.", ()->x.postX17((byte)11));
		assertEquals("{x:'1'}",x.postX18((byte)1));
		assertEquals("{x:'10'}",x.postX18((byte)10));
		assertThrowsWithMessage(Exception.class, "Minimum value not met.", ()->x.postX18((byte)0));
		assertThrowsWithMessage(Exception.class, "Maximum value exceeded.", ()->x.postX18((byte)11));
		assertEquals("{x:'2'}",x.postX19((byte)2));
		assertEquals("{x:'9'}",x.postX19((byte)9));
		assertThrowsWithMessage(Exception.class, "Minimum value not met.", ()->x.postX19((byte)1));
		assertThrowsWithMessage(Exception.class, "Maximum value exceeded.", ()->x.postX19((byte)10));
		assertEquals("{x:'1'}",x.postX20(new AtomicInteger(1)));
		assertEquals("{x:'10'}",x.postX20(new AtomicInteger(10)));
		assertThrowsWithMessage(Exception.class, "Minimum value not met.", ()->x.postX20(new AtomicInteger(0)));
		assertThrowsWithMessage(Exception.class, "Maximum value exceeded.", ()->x.postX20(new AtomicInteger(11)));
		assertEquals("{x:'1'}",x.postX21(new AtomicInteger(1)));
		assertEquals("{x:'10'}",x.postX21(new AtomicInteger(10)));
		assertThrowsWithMessage(Exception.class, "Minimum value not met.", ()->x.postX21(new AtomicInteger(0)));
		assertThrowsWithMessage(Exception.class, "Maximum value exceeded.", ()->x.postX21(new AtomicInteger(11)));
		assertEquals("{x:'2'}",x.postX22(new AtomicInteger(2)));
		assertEquals("{x:'9'}",x.postX22(new AtomicInteger(9)));
		assertThrowsWithMessage(Exception.class, "Minimum value not met.", ()->x.postX22(new AtomicInteger(1)));
		assertThrowsWithMessage(Exception.class, "Maximum value exceeded.", ()->x.postX22(new AtomicInteger(10)));
		assertEquals("{x:'1'}",x.postX23(new BigDecimal(1)));
		assertEquals("{x:'10'}",x.postX23(new BigDecimal(10)));
		assertThrowsWithMessage(Exception.class, "Minimum value not met.", ()->x.postX23(new BigDecimal(0)));
		assertThrowsWithMessage(Exception.class, "Maximum value exceeded.", ()->x.postX23(new BigDecimal(11)));
		assertEquals("{x:'1'}",x.postX24(new BigDecimal(1)));
		assertEquals("{x:'10'}",x.postX24(new BigDecimal(10)));
		assertThrowsWithMessage(Exception.class, "Minimum value not met.", ()->x.postX24(new BigDecimal(0)));
		assertThrowsWithMessage(Exception.class, "Maximum value exceeded.", ()->x.postX24(new BigDecimal(11)));
		assertEquals("{x:'2'}",x.postX25(new BigDecimal(2)));
		assertEquals("{x:'9'}",x.postX25(new BigDecimal(9)));
		assertThrowsWithMessage(Exception.class, "Minimum value not met.", ()->x.postX25(new BigDecimal(1)));
		assertThrowsWithMessage(Exception.class, "Maximum value exceeded.", ()->x.postX25(new BigDecimal(10)));
		assertEquals("{x:'1'}",x.postX26(1));
		assertEquals("{x:'10'}",x.postX26(10));
		assertThrowsWithMessage(Exception.class, "Minimum value not met.", ()->x.postX26(0));
		assertThrowsWithMessage(Exception.class, "Maximum value exceeded.", ()->x.postX26(11));
		assertEquals("{}",x.postX26(null));
		assertEquals("{x:'1'}",x.postX27(1));
		assertEquals("{x:'10'}",x.postX27(10));
		assertThrowsWithMessage(Exception.class, "Minimum value not met.", ()->x.postX27(0));
		assertThrowsWithMessage(Exception.class, "Maximum value exceeded.", ()->x.postX27(11));
		assertEquals("{}",x.postX27(null));
		assertEquals("{x:'2'}",x.postX28(2));
		assertEquals("{x:'9'}",x.postX28(9));
		assertThrowsWithMessage(Exception.class, "Minimum value not met.", ()->x.postX28(1));
		assertThrowsWithMessage(Exception.class, "Maximum value exceeded.", ()->x.postX28(10));
		assertEquals("{}",x.postX28(null));
		assertEquals("{x:'1'}",x.postX29((short)1));
		assertEquals("{x:'10'}",x.postX29((short)10));
		assertThrowsWithMessage(Exception.class, "Minimum value not met.", ()->x.postX29((short)0));
		assertThrowsWithMessage(Exception.class, "Maximum value exceeded.", ()->x.postX29((short)11));
		assertEquals("{}",x.postX29(null));
		assertEquals("{x:'1'}",x.postX30((short)1));
		assertEquals("{x:'10'}",x.postX30((short)10));
		assertThrowsWithMessage(Exception.class, "Minimum value not met.", ()->x.postX30((short)0));
		assertThrowsWithMessage(Exception.class, "Maximum value exceeded.", ()->x.postX30((short)11));
		assertEquals("{}",x.postX30(null));
		assertEquals("{x:'2'}",x.postX31((short)2));
		assertEquals("{x:'9'}",x.postX31((short)9));
		assertThrowsWithMessage(Exception.class, "Minimum value not met.", ()->x.postX31((short)1));
		assertThrowsWithMessage(Exception.class, "Maximum value exceeded.", ()->x.postX31((short)10));
		assertEquals("{}",x.postX31(null));
		assertEquals("{x:'1'}",x.postX32(1L));
		assertEquals("{x:'10'}",x.postX32(10L));
		assertThrowsWithMessage(Exception.class, "Minimum value not met.", ()->x.postX32(0L));
		assertThrowsWithMessage(Exception.class, "Maximum value exceeded.", ()->x.postX32(11L));
		assertEquals("{}",x.postX32(null));
		assertEquals("{x:'1'}",x.postX33(1L));
		assertEquals("{x:'10'}",x.postX33(10L));
		assertThrowsWithMessage(Exception.class, "Minimum value not met.", ()->x.postX33(0L));
		assertThrowsWithMessage(Exception.class, "Maximum value exceeded.", ()->x.postX33(11L));
		assertEquals("{}",x.postX33(null));
		assertEquals("{x:'2'}",x.postX34(2L));
		assertEquals("{x:'9'}",x.postX34(9L));
		assertThrowsWithMessage(Exception.class, "Minimum value not met.", ()->x.postX34(1L));
		assertThrowsWithMessage(Exception.class, "Maximum value exceeded.", ()->x.postX34(10L));
		assertEquals("{}",x.postX34(null));
		assertEquals("{x:'1.0'}",x.postX35(1f));
		assertEquals("{x:'10.0'}",x.postX35(10f));
		assertThrowsWithMessage(Exception.class, "Minimum value not met.", ()->x.postX35(0.9f));
		assertThrowsWithMessage(Exception.class, "Maximum value exceeded.", ()->x.postX35(10.1f));
		assertEquals("{}",x.postX35(null));
		assertEquals("{x:'1.0'}",x.postX36(1f));
		assertEquals("{x:'10.0'}",x.postX36(10f));
		assertThrowsWithMessage(Exception.class, "Minimum value not met.", ()->x.postX36(0.9f));
		assertThrowsWithMessage(Exception.class, "Maximum value exceeded.", ()->x.postX36(10.1f));
		assertEquals("{}",x.postX36(null));
		assertEquals("{x:'1.1'}",x.postX37(1.1f));
		assertEquals("{x:'9.9'}",x.postX37(9.9f));
		assertThrowsWithMessage(Exception.class, "Minimum value not met.", ()->x.postX37(1f));
		assertThrowsWithMessage(Exception.class, "Maximum value exceeded.", ()->x.postX37(10f));
		assertEquals("{}",x.postX37(null));
		assertEquals("{x:'1.0'}",x.postX38(1d));
		assertEquals("{x:'10.0'}",x.postX38(10d));
		assertThrowsWithMessage(Exception.class, "Minimum value not met.", ()->x.postX38(0.9d));
		assertThrowsWithMessage(Exception.class, "Maximum value exceeded.", ()->x.postX38(10.1d));
		assertEquals("{}",x.postX38(null));
		assertEquals("{x:'1.0'}",x.postX39(1d));
		assertEquals("{x:'10.0'}",x.postX39(10d));
		assertThrowsWithMessage(Exception.class, "Minimum value not met.", ()->x.postX39(0.9d));
		assertThrowsWithMessage(Exception.class, "Maximum value exceeded.", ()->x.postX39(10.1d));
		assertEquals("{}",x.postX39(null));
		assertEquals("{x:'1.1'}",x.postX40(1.1d));
		assertEquals("{x:'9.9'}",x.postX40(9.9d));
		assertThrowsWithMessage(Exception.class, "Minimum value not met.", ()->x.postX40(1d));
		assertThrowsWithMessage(Exception.class, "Maximum value exceeded.", ()->x.postX40(10d));
		assertEquals("{}",x.postX40(null));
		assertEquals("{x:'1'}",x.postX41((byte)1));
		assertEquals("{x:'10'}",x.postX41((byte)10));
		assertThrowsWithMessage(Exception.class, "Minimum value not met.", ()->x.postX41((byte)0));
		assertThrowsWithMessage(Exception.class, "Maximum value exceeded.", ()->x.postX41((byte)11));
		assertEquals("{}",x.postX41(null));
		assertEquals("{x:'1'}",x.postX42((byte)1));
		assertEquals("{x:'10'}",x.postX42((byte)10));
		assertThrowsWithMessage(Exception.class, "Minimum value not met.", ()->x.postX42((byte)0));
		assertThrowsWithMessage(Exception.class, "Maximum value exceeded.", ()->x.postX42((byte)11));
		assertEquals("{}",x.postX42(null));
		assertEquals("{x:'2'}",x.postX43((byte)2));
		assertEquals("{x:'9'}",x.postX43((byte)9));
		assertThrowsWithMessage(Exception.class, "Minimum value not met.", ()->x.postX43((byte)1));
		assertThrowsWithMessage(Exception.class, "Maximum value exceeded.", ()->x.postX43((byte)10));
		assertEquals("{}",x.postX43(null));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// @FormData(maxItems) @Schema(minItems,uniqueItems)
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class E {
		@RestOp
		public String post(@FormData("*") JsonMap m) {
			return m.toString();
		}
	}

	@Remote
	public interface E1 {
		@RemoteOp(path="/") String postX1(@FormData("x") @Schema(cf="pipes", mini=1,maxi=2) String...b);
		@RemoteOp(path="/") String postX2(@FormData("x") @Schema(items=@Items(cf="pipes", mini=1,maxi=2)) String[]...b);
		@RemoteOp(path="/") String postX3(@FormData("x") @Schema(cf="pipes",ui=false) String...b);
		@RemoteOp(path="/") String postX4(@FormData("x") @Schema(items=@Items(cf="pipes",ui=false)) String[]...b);
		@RemoteOp(path="/") String postX5(@FormData("x") @Schema(cf="pipes",ui=true) String...b);
		@RemoteOp(path="/") String postX6(@FormData("x") @Schema(items=@Items(cf="pipes",ui=true)) String[]...b);
	}

	@Test void e01_mini_maxi_ui() {
		E1 x = remote(E.class,E1.class);
		assertEquals("{x:'1'}",x.postX1("1"));
		assertEquals("{x:'1|2'}",x.postX1("1","2"));
		assertThrowsWithMessage(Exception.class, "Minimum number of items not met.", x::postX1);
		assertThrowsWithMessage(Exception.class, "Maximum number of items exceeded.", ()->x.postX1("1","2","3"));
		assertEquals("{x:null}",x.postX1((String)null));
		assertEquals("{x:'1'}",x.postX2(new String[]{"1"}));
		assertEquals("{x:'1|2'}",x.postX2(new String[]{"1","2"}));
		assertThrowsWithMessage(Exception.class, "Minimum number of items not met.", ()->x.postX2(new String[]{}));
		assertThrowsWithMessage(Exception.class, "Maximum number of items exceeded.", ()->x.postX2(new String[]{"1","2","3"}));
		assertEquals("{x:null}",x.postX2(new String[]{null}));
		assertEquals("{x:'1|1'}",x.postX3("1","1"));
		assertEquals("{x:'1|1'}",x.postX4(new String[]{"1","1"}));
		assertEquals("{x:'1|2'}",x.postX5("1","2"));
		assertThrowsWithMessage(Exception.class, "Duplicate items not allowed.", ()->x.postX5("1","1"));
		assertEquals("{x:'1|2'}",x.postX6(new String[]{"1","2"}));
		assertThrowsWithMessage(Exception.class, "Duplicate items not allowed.", ()->x.postX6(new String[]{"1","1"}));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// @FormData(maxLength) @Schema(minLength,enum,pattern)
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class F {
		@RestOp
		public String post(@FormData("*") JsonMap m) {
			return m.toString();
		}
	}

	@Remote
	public interface F1 {
		@RemoteOp(path="/") String postX1(@FormData("x") @Schema(minl=2,maxl=3) String b);
		@RemoteOp(path="/") String postX2(@FormData("x") @Schema(cf="pipes",items=@Items(minl=2,maxl=3)) String...b);
		@RemoteOp(path="/") String postX3(@FormData("x") @Schema(e={"foo"}) String b);
		@RemoteOp(path="/") String postX4(@FormData("x") @Schema(cf="pipes",items=@Items(e={"foo"})) String...b);
		@RemoteOp(path="/") String postX5(@FormData("x") @Schema(p="foo\\d{1,3}") String b);
		@RemoteOp(path="/") String postX6(@FormData("x") @Schema(cf="pipes",items=@Items(p="foo\\d{1,3}")) String...b);
	}

	@Test void f01_minl_maxl_enum_pattern() {
		F1 x = remote(F.class,F1.class);
		assertEquals("{x:'12'}",x.postX1("12"));
		assertEquals("{x:'123'}",x.postX1("123"));
		assertThrowsWithMessage(Exception.class, "Minimum length of value not met.", ()->x.postX1("1"));
		assertThrowsWithMessage(Exception.class, "Maximum length of value exceeded.", ()->x.postX1("1234"));
		assertEquals("{}",x.postX1(null));
		assertEquals("{x:'12|34'}",x.postX2("12","34"));
		assertEquals("{x:'123|456'}",x.postX2("123","456"));
		assertThrowsWithMessage(Exception.class, "Minimum length of value not met.", ()->x.postX2("1","2"));
		assertThrowsWithMessage(Exception.class, "Maximum length of value exceeded.", ()->x.postX2("1234","5678"));
		assertEquals("{x:'12|null'}",x.postX2("12",null));
		assertEquals("{x:'foo'}",x.postX3("foo"));
		assertThrowsWithMessage(Exception.class, "Value does not match one of the expected values.  Must be one of the following:  foo", ()->x.postX3("bar"));
		assertEquals("{}",x.postX3(null));
		assertEquals("{x:'foo'}",x.postX4("foo"));
		assertThrowsWithMessage(Exception.class, "Value does not match one of the expected values.  Must be one of the following:  foo", ()->x.postX4("bar"));
		assertEquals("{x:null}",x.postX4((String)null));
		assertEquals("{x:'foo123'}",x.postX5("foo123"));
		assertThrowsWithMessage(Exception.class, "Value does not match expected pattern.  Must match pattern: foo\\d{1,3}", ()->x.postX5("bar"));
		assertEquals("{}",x.postX5(null));
		assertEquals("{x:'foo123'}",x.postX6("foo123"));
		assertThrowsWithMessage(Exception.class, "Value does not match expected pattern.  Must match pattern: foo\\d{1,3}", ()->x.postX6("foo"));
		assertEquals("{x:null}",x.postX6((String)null));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// @FormData(multipleOf)
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class G {
		@RestOp
		public String post(@FormData("*") JsonMap m) {
			return m.toString();
		}
	}

	@Remote
	public interface G1 {
		@RemoteOp(path="/") String postX1(@FormData("x") @Schema(mo="2") int b);
		@RemoteOp(path="/") String postX2(@FormData("x") @Schema(mo="2") short b);
		@RemoteOp(path="/") String postX3(@FormData("x") @Schema(mo="2") long b);
		@RemoteOp(path="/") String postX4(@FormData("x") @Schema(mo="2") float b);
		@RemoteOp(path="/") String postX5(@FormData("x") @Schema(mo="2") double b);
		@RemoteOp(path="/") String postX6(@FormData("x") @Schema(mo="2") byte b);
		@RemoteOp(path="/") String postX7(@FormData("x") @Schema(mo="2") AtomicInteger b);
		@RemoteOp(path="/") String postX8(@FormData("x") @Schema(mo="2") BigDecimal b);
		@RemoteOp(path="/") String postX9(@FormData("x") @Schema(mo="2") Integer b);
		@RemoteOp(path="/") String postX10(@FormData("x") @Schema(mo="2") Short b);
		@RemoteOp(path="/") String postX11(@FormData("x") @Schema(mo="2") Long b);
		@RemoteOp(path="/") String postX12(@FormData("x") @Schema(mo="2") Float b);
		@RemoteOp(path="/") String postX13(@FormData("x") @Schema(mo="2") Double b);
		@RemoteOp(path="/") String postX14(@FormData("x") @Schema(mo="2") Byte b);
	}

	@Test void g01_multipleOf() {
		G1 x = remote(G.class,G1.class);
		assertEquals("{x:'4'}",x.postX1(4));
		assertThrowsWithMessage(Exception.class, "Multiple-of not met.", ()->x.postX1(5));
		assertEquals("{x:'4'}",x.postX2((short)4));
		assertThrowsWithMessage(Exception.class, "Multiple-of not met.", ()->x.postX2((short)5));
		assertEquals("{x:'4'}",x.postX3(4));
		assertThrowsWithMessage(Exception.class, "Multiple-of not met.", ()->x.postX3(5));
		assertEquals("{x:'4.0'}",x.postX4(4));
		assertThrowsWithMessage(Exception.class, "Multiple-of not met.", ()->x.postX4(5));
		assertEquals("{x:'4.0'}",x.postX5(4));
		assertThrowsWithMessage(Exception.class, "Multiple-of not met.", ()->x.postX5(5));
		assertEquals("{x:'4'}",x.postX6((byte)4));
		assertThrowsWithMessage(Exception.class, "Multiple-of not met.", ()->x.postX6((byte)5));
		assertEquals("{x:'4'}",x.postX7(new AtomicInteger(4)));
		assertThrowsWithMessage(Exception.class, "Multiple-of not met.", ()->x.postX7(new AtomicInteger(5)));
		assertEquals("{x:'4'}",x.postX8(new BigDecimal(4)));
		assertThrowsWithMessage(Exception.class, "Multiple-of not met.", ()->x.postX8(new BigDecimal(5)));
		assertEquals("{x:'4'}",x.postX9(4));
		assertThrowsWithMessage(Exception.class, "Multiple-of not met.", ()->x.postX9(5));
		assertEquals("{x:'4'}",x.postX10((short)4));
		assertThrowsWithMessage(Exception.class, "Multiple-of not met.", ()->x.postX10((short)5));
		assertEquals("{x:'4'}",x.postX11(4L));
		assertThrowsWithMessage(Exception.class, "Multiple-of not met.", ()->x.postX11(5L));
		assertEquals("{x:'4.0'}",x.postX12(4f));
		assertThrowsWithMessage(Exception.class, "Multiple-of not met.", ()->x.postX12(5f));
		assertEquals("{x:'4.0'}",x.postX13(4d));
		assertThrowsWithMessage(Exception.class, "Multiple-of not met.", ()->x.postX13(5d));
		assertEquals("{x:'4'}",x.postX14((byte)4));
		assertThrowsWithMessage(Exception.class, "Multiple-of not met.", ()->x.postX14((byte)5));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// @FormData(required)
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class H {
		@RestOp
		public String post(@FormData("*") JsonMap m) {
			return m.toString();
		}
	}

	@Remote
	public interface H1 {
		@RemoteOp(path="/") String postX1(@FormData("x") String b);
		@RemoteOp(path="/") String postX2(@FormData("x") @Schema(r=false) String b);
		@RemoteOp(path="/") String postX3(@FormData("x") @Schema(r=true) String b);
	}

	@Test void h01_required() {
		H1 x = remote(H.class,H1.class);
		assertEquals("{}",x.postX1(null));
		assertEquals("{}",x.postX2(null));
		assertEquals("{x:'1'}",x.postX3("1"));
		assertThrowsWithMessage(Exception.class, "Required value not provided.", ()->x.postX3(null));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// @FormData(skipIfEmpty)
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class I {
		@RestOp
		public String post(@FormData("*") JsonMap m) {
			return m.toString();
		}
	}

	@Remote
	public interface I1 {
		@RemoteOp(path="/") String postX1(@FormData("x") @Schema(aev=true) String b);
		@RemoteOp(path="/") String postX2(@FormData("x") @Schema(aev=true,sie=false) String b);
		@RemoteOp(path="/") String postX3(@FormData("x") @Schema(sie=true) String b);
	}

	@Test void i01_skipIfEmpty() {
		I1 x = remote(I.class,I1.class);
		assertEquals("{x:''}",x.postX1(""));
		assertEquals("{x:''}",x.postX2(""));
		assertEquals("{}",x.postX3(""));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// @FormData(serializer)
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class J {
		@RestOp
		public String post(@FormData("*") JsonMap m) {
			return m.toString();
		}
	}

	@Remote
	public interface J1 {
		@RemoteOp(path="/") String postX1(@FormData(name="x",serializer=FakeWriterSerializer.X.class) String b);
	}

	@Test void j01_serializer() {
		J1 x = remote(J.class,J1.class);
		assertEquals("{x:'xXx'}",x.postX1("X"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// RequestBean @FormData
	//-----------------------------------------------------------------------------------------------------------------

	@Rest(parsers=UrlEncodingParser.class)
	public static class K {
		@RestOp
		public String post(RestRequest req) throws Exception {
			return req.getFormParams().toString();
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// RequestBean @FormData, Simple values
	//-----------------------------------------------------------------------------------------------------------------

	@Remote(path="/")
	public interface K1 {
		@RemoteOp(path="/") String postX1(@Request K1a rb);
		@RemoteOp(path="/") String postX2(@Request(serializer=FakeWriterSerializer.X.class) K1a rb);
	}

	public static class K1a {
		@FormData
		public String getA() {
			return "a1";
		}
		@FormData("b")
		public String getX1() {
			return "b1";
		}
		@FormData("c")
		public String getX2() {
			return "c1";
		}
		@FormData("e") @Schema(aev=true)
		public String getX4() {
			return "";
		}
		@FormData("f")
		public String getX5() {
			return null;
		}
		@FormData("g")
		public String getX6() {
			return "true";
		}
		@FormData("h")
		public String getX7() {
			return "123";
		}
		@FormData("i1") @Schema(sie=true)
		public String getX8() {
			return "foo";
		}
		@FormData("i2") @Schema(sie=true)
		public String getX9() {
			return "";
		}
		@FormData("i3") @Schema(sie=true)
		public String getX10() {
			return null;
		}
	}

	@Test void k01_requestBean_simpleVals() {
		K1 x1 = remote(K.class,K1.class);
		K1 x2 = client(K.class).partSerializer(UonSerializer.class).build().getRemote(K1.class);
		assertEquals("{a:'a1',b:'b1',c:'c1',e:'',g:'true',h:'123',i1:'foo'}",x1.postX1(new K1a()));
		assertEquals("{a:'a1',b:'b1',c:'c1',e:'',g:'\\'true\\'',h:'\\'123\\'',i1:'foo'}",x2.postX1(new K1a()));
		assertEquals("{a:'xa1x',b:'xb1x',c:'xc1x',e:'xx',g:'xtruex',h:'x123x',i1:'xfoox'}",x2.postX2(new K1a()));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// RequestBean @FormData, Maps
	//-----------------------------------------------------------------------------------------------------------------

	@Remote(path="/")
	public interface K2 {
		@RemoteOp(path="/") String postX1(@Request K2a rb);
		@RemoteOp(path="/") String postX2(@Request(serializer=FakeWriterSerializer.X.class) K2a rb);
	}

	public static class K2a {
		@FormData
		public Map<String,Object> getA() {
			return mapBuilder(String.class,Object.class).add("a1","v1").add("a2",123).add("a3",null).add("a4","").build();
		}
		@FormData("*")
		public Map<String,Object> getB() {
			return map("b1","true","b2","123","b3","null");
		}
		@FormData("*") @Schema(aev=true)
		public Map<String,Object> getC() {
			return mapBuilder(String.class,Object.class).add("c1","v1").add("c2",123).add("c3",null).add("c4","").build();
		}
		@FormData("*")
		public Map<String,Object> getD() {
			return null;
		}
	}

	@Test void k02_requestBean_maps() {
		K2 x1 = remote(K.class,K2.class);
		K2 x2 = client(K.class).partSerializer(UonSerializer.class).build().getRemote(K2.class);
		assertEquals("{a:'a1=v1,a2=123,a3=null,a4=',b1:'true',b2:'123',b3:'null',c1:'v1',c2:'123',c4:''}",x1.postX1(new K2a()));
		assertEquals("{a:'(a1=v1,a2=123,a3=null,a4=\\'\\')',b1:'\\'true\\'',b2:'\\'123\\'',b3:'\\'null\\'',c1:'v1',c2:'123',c4:''}",x2.postX1(new K2a()));
		assertEquals("{a:'x{a1=v1, a2=123, a3=null, a4=}x',b1:'xtruex',b2:'x123x',b3:'xnullx',c1:'xv1x',c2:'x123x',c4:'xx'}",x2.postX2(new K2a()));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// RequestBean @FormData, NameValuePairs
	//-----------------------------------------------------------------------------------------------------------------

	@Remote(path="/")
	public interface K3 {
		@RemoteOp(path="/") String postX1(@Request K3a rb);
		@RemoteOp(path="/") String postX2(@Request(serializer=FakeWriterSerializer.X.class) K3a rb);
	}

	public static class K3a {
		@FormData
		public PartList getA() {
			return parts("a1","v1","a2","123","a3",null,"a4","");
		}
		@FormData("*")
		public PartList getB() {
			return parts("b1","true","b2","123","b3","null");
		}
		@FormData("*")
		public PartList getC() {
			return parts("c1","v1","c2","123","c3",null,"c4","");
		}
		@FormData("*")
		public PartList getD() {
			return null;
		}
		@FormData
		public NameValuePair[] getE() {
			return parts("e1","v1","e2","123","e3",null,"e4","").getAll();
		}
	}

	@Test void k03_requestBean_nameValuePairs() {
		K3 x1 = remote(K.class,K3.class);
		K3 x2 = client(K.class).partSerializer(UonSerializer.class).build().getRemote(K3.class);
		assertEquals("{a1:'v1',a2:'123',a4:'',b1:'true',b2:'123',b3:'null',c1:'v1',c2:'123',c4:'',e1:'v1',e2:'123',e4:''}",x1.postX1(new K3a()));
		assertEquals("{a1:'v1',a2:'123',a4:'',b1:'true',b2:'123',b3:'null',c1:'v1',c2:'123',c4:'',e1:'v1',e2:'123',e4:''}",x2.postX1(new K3a()));
		assertEquals("{a1:'v1',a2:'123',a4:'',b1:'true',b2:'123',b3:'null',c1:'v1',c2:'123',c4:'',e1:'v1',e2:'123',e4:''}",x2.postX2(new K3a()));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// RequestBean @FormData,CharSequence
	//-----------------------------------------------------------------------------------------------------------------

	@Remote(path="/")
	public interface K4 {
		String post(@Request C04_Bean rb);
	}

	public static class C04_Bean {
		@FormData("*")
		public StringBuilder getA() {
			return new StringBuilder("foo=bar&baz=qux");
		}
	}

	@Test void k04_requestBean_charSequence() {
		K4 x = remote(K.class,K4.class);
		assertEquals("{foo:'bar',baz:'qux'}",x.post(new C04_Bean()));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// RequestBean @FormData, Reader
	//-----------------------------------------------------------------------------------------------------------------

	@Remote(path="/")
	public interface K5 {
		String post(@Request K5a rb);
	}

	public static class K5a {
		@FormData("*")
		public Reader getA() {
			return reader("foo=bar&baz=qux");
		}
	}

	@Test void k05_requestBean_reader() {
		K5 x = remote(K.class,K5.class);
		assertEquals("{foo:'bar',baz:'qux'}",x.post(new K5a()));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// RequestBean @FormData, Collections
	//-----------------------------------------------------------------------------------------------------------------

	@Remote(path="/")
	public interface K6 {
		@RemoteOp(path="/") String postX1(@Request K6a rb);
		@RemoteOp(path="/") String postX2(@Request(serializer=FakeWriterSerializer.X.class) K6a rb);
	}

	public static class K6a {
		@FormData
		public List<Object> getA() {
			return alist("foo","","true","123","null",true,123,null);
		}
		@FormData("b")
		public List<Object> getX1() {
			return alist("foo","","true","123","null",true,123,null);
		}
		@FormData(name="c",serializer=FakeWriterSerializer.X.class)
		public List<Object> getX2() {
			return alist("foo","","true","123","null",true,123,null);
		}
		@FormData("d") @Schema(aev=true)
		public List<Object> getX3() {
			return alist();
		}
		@FormData("e")
		public List<Object> getX4() {
			return null;
		}
		@FormData("f")
		public Object[] getX5() {
			return new Object[]{"foo","","true","123","null",true,123,null};
		}
		@FormData(name="g",serializer=FakeWriterSerializer.X.class)
		public Object[] getX6() {
			return new Object[]{"foo","","true","123","null",true,123,null};
		}
		@FormData("h") @Schema(aev=true)
		public Object[] getX7() {
			return new Object[]{};
		}
		@FormData("i")
		public Object[] getX8() {
			return null;
		}
	}

	@Test void k06_requestBean_collections() {
		K6 x1 = remote(K.class,K6.class);
		K6 x2 = client(K.class).partSerializer(UonSerializer.class).build().getRemote(K6.class);
		assertEquals("{a:'foo,,true,123,null,true,123,null',b:'foo,,true,123,null,true,123,null',c:'xfoo||true|123|null|true|123|nullx',d:'',f:'foo,,true,123,null,true,123,null',g:'xfoo||true|123|null|true|123|nullx',h:''}", x1.postX1(new K6a()));
		assertEquals("{a:'@(foo,\\'\\',\\'true\\',\\'123\\',\\'null\\',true,123,null)',b:'@(foo,\\'\\',\\'true\\',\\'123\\',\\'null\\',true,123,null)',c:'xfoo||true|123|null|true|123|nullx',d:'@()',f:'@(foo,\\'\\',\\'true\\',\\'123\\',\\'null\\',true,123,null)',g:'xfoo||true|123|null|true|123|nullx',h:'@()'}", x2.postX1(new K6a()));
		assertEquals("{a:'xfoo||true|123|null|true|123|nullx',b:'xfoo||true|123|null|true|123|nullx',c:'xfoo||true|123|null|true|123|nullx',d:'xx',f:'xfoo||true|123|null|true|123|nullx',g:'xfoo||true|123|null|true|123|nullx',h:'xx'}", x2.postX2(new K6a()));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Helper methods.
	//------------------------------------------------------------------------------------------------------------------

	private static NameValuePair part(String name, Object val) {
		return basicPart(name,val);
	}

	private static PartList parts(String...pairs) {
		return partList(pairs);
	}

	private static RestClient.Builder client(Class<?> c) {
		return MockRestClient.create(c);
	}

	private static <T> T remote(Class<?> rest,Class<T> t) {
		return MockRestClient.build(rest).getRemote(t);
	}
}