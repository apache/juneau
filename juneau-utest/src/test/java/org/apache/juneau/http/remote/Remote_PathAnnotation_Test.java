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

import java.math.*;
import java.util.*;
import java.util.concurrent.atomic.*;

import org.apache.http.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.http.part.*;
import org.apache.juneau.jsonschema.annotation.*;
import org.apache.juneau.rest.RestRequest;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.client.*;
import org.apache.juneau.rest.mock.*;
import org.apache.juneau.testutils.*;
import org.apache.juneau.uon.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class Remote_PathAnnotation_Test {

	public static class Bean {
		public int x;

		public static Bean create() {
			Bean b = new Bean();
			b.x = 1;
			return b;
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Basic tests
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class A {
		@RestGet(path="/a/{x}")
		public String a(@Path("x") Object x) {
			return x.toString();
		}
	}

	@Remote
	public static interface A1 {
		@RemoteOp(path="a/{x}") String getX1(@Path("x") int b);
		@RemoteOp(path="a/{x}") String getX2(@Path("x") float b);
		@RemoteOp(path="a/{x}") String getX3(@Path("x") Bean b);
		@RemoteOp(path="a/{x}") String getX4(@Path("*") Bean b);
		@RemoteOp(path="a/{x}") String getX5(@Path Bean b);
		@RemoteOp(path="a/{x}") String getX6(@Path("x") Bean[] b);
		@RemoteOp(path="a/{x}") String getX7(@Path(n="x",cf="uon") Bean[] b);
		@RemoteOp(path="a/{x}") String getX8(@Path("x") List<Bean> b);
		@RemoteOp(path="a/{x}") String getX9(@Path(n="x",cf="uon") List<Bean> b);
		@RemoteOp(path="a/{x}") String getX10(@Path("x") Map<String,Bean> b);
		@RemoteOp(path="a/{x}") String getX11(@Path("*") Map<String,Bean> b);
		@RemoteOp(path="a/{x}") String getX12(@Path Map<String,Bean> b);
		@RemoteOp(path="a/{x}") String getX13(@Path(n="x",cf="uon") Map<String,Bean> b);
		@RemoteOp(path="a/{x}") String getX14(@Path(f="uon") Map<String,Bean> b);
		@RemoteOp(path="a/{x}") String getX15(@Path("*") PartList b);
		@RemoteOp(path="a/{x}") String getX16(@Path PartList b);
		@RemoteOp(path="a/{x}") String getX17(@Path(n="x",serializer=UonSerializer.class) Map<String,Bean> b);
		@RemoteOp(path="a/{x}") String getX18(@Path(n="*") NameValuePair b);
		@RemoteOp(path="a/{x}") String getX19(@Path NameValuePair b);
		@RemoteOp(path="a/{x}") String getX20(@Path NameValuePair[] b);
		@RemoteOp(path="a/{x}") String getX21(@Path String b);
		@RemoteOp(path="a/{x}") String getX22(@Path List<NameValuePair> b);
	}

	@Test
	public void a01_path_objectTypes() throws Exception {
		A1 x = remote(A.class,A1.class);
		assertEquals("1",x.getX1(1));
		assertEquals("1.0",x.getX2(1));
		assertEquals("x=1",x.getX3(Bean.create()));
		assertEquals("1",x.getX4(Bean.create()));
		assertEquals("1",x.getX5(Bean.create()));
		assertEquals("x=1,x=1",x.getX6(new Bean[]{Bean.create(),Bean.create()}));
		assertEquals("@((x=1),(x=1))",x.getX7(new Bean[]{Bean.create(),Bean.create()}));
		assertEquals("x=1,x=1",x.getX8(AList.of(Bean.create(),Bean.create())));
		assertEquals("@((x=1),(x=1))",x.getX9(AList.of(Bean.create(),Bean.create())));
		assertEquals("x=x\\=1",x.getX10(AMap.of("x",Bean.create())));
		assertEquals("x=1",x.getX11(AMap.of("x",Bean.create())));
		assertEquals("x=1",x.getX12(AMap.of("x",Bean.create())));
		assertEquals("(x=(x=1))",x.getX13(AMap.of("x",Bean.create())));
		assertEquals("x=1",x.getX14(AMap.of("x",Bean.create())));
		assertEquals("bar",x.getX15(parts("x","bar")));
		assertEquals("bar",x.getX16(parts("x","bar")));
		assertEquals("(x=(x=1))",x.getX17(AMap.of("x",Bean.create())));
		assertEquals("bar",x.getX18(part("x","bar")));
		assertEquals("bar",x.getX19(part("x","bar")));
		assertEquals("bar",x.getX20(new NameValuePair[]{part("x","bar")}));
		assertEquals("{x}",x.getX20(null));
		assertThrown(()->x.getX21("foo")).messages().contains("Invalid value type for path arg 'null': java.lang.String");
		assertEquals("bar",x.getX22(AList.of(part("x","bar"))));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// @Query(collectionFormat)
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class B {
		@RestGet(path="/a/{x}")
		public String a(@Path("x") Object x) {
			return x.toString();
		}
	}

	@Remote
	public static interface B1 {
		@RemoteOp(path="/a/{x}") String getX1(@Path(n="x") String...b);
		@RemoteOp(path="/a/{x}") String getX2(@Path(n="x",cf="csv") String...b);
		@RemoteOp(path="/a/{x}") String getX3(@Path(n="x",cf="ssv") String...b);
		@RemoteOp(path="/a/{x}") String getX4(@Path(n="x",cf="tsv") String...b);
		@RemoteOp(path="/a/{x}") String getX5(@Path(n="x",cf="pipes") String...b);
		@RemoteOp(path="/a/{x}") String getX6(@Path(n="x",cf="multi") String...b); // Not supported,but should be treated as csv.
		@RemoteOp(path="/a/{x}") String getX7(@Path(n="x",cf="uon") String...b);
	}

	@Test
	public void b01_path_collectionFormat() throws Exception {
		B1 x = remote(B.class,B1.class);
		assertEquals("foo,bar",x.getX1("foo","bar"));
		assertEquals("foo,bar",x.getX2("foo","bar"));
		assertEquals("foo bar",x.getX3("foo","bar"));
		assertEquals("foo\tbar",x.getX4("foo","bar"));
		assertEquals("foo|bar",x.getX5("foo","bar"));
		assertEquals("foo,bar",x.getX6("foo","bar"));
		assertEquals("@(foo,bar)",x.getX7("foo","bar"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// @Path(maximum,exclusiveMaximum,minimum,exclusiveMinimum)
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class C {
		@RestOp(path="/a/{x}")
		public String get(@Path("*") OMap m) {
			m.removeAll("/*","/**");
			return m.toString();
		}
	}

	@Remote
	public static interface C1 {
		@RemoteOp(path="/a/{x}") String getX1(@Path(n="x",min="1",max="10") int b);
		@RemoteOp(path="/a/{x}") String getX2(@Path(n="x",min="1",max="10",emin=false,emax=false) int b);
		@RemoteOp(path="/a/{x}") String getX3(@Path(n="x",min="1",max="10",emin=true,emax=true) int b);
		@RemoteOp(path="/a/{x}") String getX4(@Path(n="x",min="1",max="10") short b);
		@RemoteOp(path="/a/{x}") String getX5(@Path(n="x",min="1",max="10",emin=false,emax=false) short b);
		@RemoteOp(path="/a/{x}") String getX6(@Path(n="x",min="1",max="10",emin=true,emax=true) short b);
		@RemoteOp(path="/a/{x}") String getX7(@Path(n="x",min="1",max="10") long b);
		@RemoteOp(path="/a/{x}") String getX8(@Path(n="x",min="1",max="10",emin=false,emax=false) long b);
		@RemoteOp(path="/a/{x}") String getX9(@Path(n="x",min="1",max="10",emin=true,emax=true) long b);
		@RemoteOp(path="/a/{x}") String getX10(@Path(n="x",min="1",max="10") float b);
		@RemoteOp(path="/a/{x}") String getX11(@Path(n="x",min="1",max="10",emin=false,emax=false) float b);
		@RemoteOp(path="/a/{x}") String getX12(@Path(n="x",min="1",max="10",emin=true,emax=true) float b);
		@RemoteOp(path="/a/{x}") String getX13(@Path(n="x",min="1",max="10") double b);
		@RemoteOp(path="/a/{x}") String getX14(@Path(n="x",min="1",max="10",emin=false,emax=false) double b);
		@RemoteOp(path="/a/{x}") String getX15(@Path(n="x",min="1",max="10",emin=true,emax=true) double b);
		@RemoteOp(path="/a/{x}") String getX16(@Path(n="x",min="1",max="10") byte b);
		@RemoteOp(path="/a/{x}") String getX17(@Path(n="x",min="1",max="10",emin=false,emax=false) byte b);
		@RemoteOp(path="/a/{x}") String getX18(@Path(n="x",min="1",max="10",emin=true,emax=true) byte b);
		@RemoteOp(path="/a/{x}") String getX19(@Path(n="x",min="1",max="10") AtomicInteger b);
		@RemoteOp(path="/a/{x}") String getX20(@Path(n="x",min="1",max="10",emin=false,emax=false) AtomicInteger b);
		@RemoteOp(path="/a/{x}") String getX21(@Path(n="x",min="1",max="10",emin=true,emax=true) AtomicInteger b);
		@RemoteOp(path="/a/{x}") String getX22(@Path(n="x",min="1",max="10") BigDecimal b);
		@RemoteOp(path="/a/{x}") String getX23(@Path(n="x",min="1",max="10",emin=false,emax=false) BigDecimal b);
		@RemoteOp(path="/a/{x}") String getX24(@Path(n="x",min="1",max="10",emin=true,emax=true) BigDecimal b);
		@RemoteOp(path="/a/{x}") String getX25(@Path(n="x",min="1",max="10") Integer b);
		@RemoteOp(path="/a/{x}") String getX26(@Path(n="x",min="1",max="10",emin=false,emax=false) Integer b);
		@RemoteOp(path="/a/{x}") String getX27(@Path(n="x",min="1",max="10",emin=true,emax=true) Integer b);
		@RemoteOp(path="/a/{x}") String getX28(@Path(n="x",min="1",max="10") Short b);
		@RemoteOp(path="/a/{x}") String getX29(@Path(n="x",min="1",max="10",emin=false,emax=false) Short b);
		@RemoteOp(path="/a/{x}") String getX30(@Path(n="x",min="1",max="10",emin=true,emax=true) Short b);
		@RemoteOp(path="/a/{x}") String getX31(@Path(n="x",min="1",max="10") Long b);
		@RemoteOp(path="/a/{x}") String getX32(@Path(n="x",min="1",max="10",emin=false,emax=false) Long b);
		@RemoteOp(path="/a/{x}") String getX33(@Path(n="x",min="1",max="10",emin=true,emax=true) Long b);
		@RemoteOp(path="/a/{x}") String getX34(@Path(n="x",min="1",max="10") Float b);
		@RemoteOp(path="/a/{x}") String getX35(@Path(n="x",min="1",max="10",emin=false,emax=false) Float b);
		@RemoteOp(path="/a/{x}") String getX36(@Path(n="x",min="1",max="10",emin=true,emax=true) Float b);
		@RemoteOp(path="/a/{x}") String getX37(@Path(n="x",min="1",max="10") Double b);
		@RemoteOp(path="/a/{x}") String getX38(@Path(n="x",min="1",max="10",emin=false,emax=false) Double b);
		@RemoteOp(path="/a/{x}") String getX39(@Path(n="x",min="1",max="10",emin=true,emax=true) Double b);
		@RemoteOp(path="/a/{x}") String getX40(@Path(n="x",min="1",max="10") Byte b);
		@RemoteOp(path="/a/{x}") String getX41(@Path(n="x",min="1",max="10",emin=false,emax=false) Byte b);
		@RemoteOp(path="/a/{x}") String getX42(@Path(n="x",min="1",max="10",emin=true,emax=true) Byte b);
	}

	@Test
	public void c01_path_min_max_emin_emax() throws Exception {
		C1 x = remote(C.class,C1.class);
		assertEquals("{x:'1'}",x.getX1(1));
		assertEquals("{x:'10'}",x.getX1(10));
		assertThrown(()->x.getX1(0)).messages().contains("Minimum value not met.");
		assertThrown(()->x.getX1(11)).messages().contains("Maximum value exceeded.");
		assertEquals("{x:'1'}",x.getX2(1));
		assertEquals("{x:'10'}",x.getX2(10));
		assertThrown(()->x.getX2(0)).messages().contains("Minimum value not met.");
		assertThrown(()->x.getX2(11)).messages().contains("Maximum value exceeded.");
		assertEquals("{x:'2'}",x.getX3(2));
		assertEquals("{x:'9'}",x.getX3(9));
		assertThrown(()->x.getX3(1)).messages().contains("Minimum value not met.");
		assertThrown(()->x.getX3(10)).messages().contains("Maximum value exceeded.");
		assertEquals("{x:'1'}",x.getX4((short)1));
		assertEquals("{x:'10'}",x.getX4((short)10));
		assertThrown(()->x.getX4((short)0)).messages().contains("Minimum value not met.");
		assertThrown(()->x.getX4((short)11)).messages().contains("Maximum value exceeded.");
		assertEquals("{x:'1'}",x.getX5((short)1));
		assertEquals("{x:'10'}",x.getX5((short)10));
		assertThrown(()->x.getX5((short)0)).messages().contains("Minimum value not met.");
		assertThrown(()->x.getX5((short)11)).messages().contains("Maximum value exceeded.");
		assertEquals("{x:'2'}",x.getX6((short)2));
		assertEquals("{x:'9'}",x.getX6((short)9));
		assertThrown(()->x.getX6((short)1)).messages().contains("Minimum value not met.");
		assertThrown(()->x.getX6((short)10)).messages().contains("Maximum value exceeded.");
		assertEquals("{x:'1'}",x.getX7(1l));
		assertEquals("{x:'10'}",x.getX7(10l));
		assertThrown(()->x.getX7(0l)).messages().contains("Minimum value not met.");
		assertThrown(()->x.getX7(11l)).messages().contains("Maximum value exceeded.");
		assertEquals("{x:'1'}",x.getX8(1l));
		assertEquals("{x:'10'}",x.getX8(10l));
		assertThrown(()->x.getX8(0l)).messages().contains("Minimum value not met.");
		assertThrown(()->x.getX8(11l)).messages().contains("Maximum value exceeded.");
		assertEquals("{x:'2'}",x.getX9(2l));
		assertEquals("{x:'9'}",x.getX9(9l));
		assertThrown(()->x.getX9(1l)).messages().contains("Minimum value not met.");
		assertThrown(()->x.getX9(10l)).messages().contains("Maximum value exceeded.");
		assertEquals("{x:'1.0'}",x.getX10(1f));
		assertEquals("{x:'10.0'}",x.getX10(10f));
		assertThrown(()->x.getX10(0.9f)).messages().contains("Minimum value not met.");
		assertThrown(()->x.getX10(10.1f)).messages().contains("Maximum value exceeded.");
		assertEquals("{x:'1.0'}",x.getX11(1f));
		assertEquals("{x:'10.0'}",x.getX11(10f));
		assertThrown(()->x.getX11(0.9f)).messages().contains("Minimum value not met.");
		assertThrown(()->x.getX11(10.1f)).messages().contains("Maximum value exceeded.");
		assertEquals("{x:'1.1'}",x.getX12(1.1f));
		assertEquals("{x:'9.9'}",x.getX12(9.9f));
		assertThrown(()->x.getX12(1f)).messages().contains("Minimum value not met.");
		assertThrown(()->x.getX12(10f)).messages().contains("Maximum value exceeded.");
		assertEquals("{x:'1.0'}",x.getX13(1d));
		assertEquals("{x:'10.0'}",x.getX13(10d));
		assertThrown(()->x.getX13(0.9d)).messages().contains("Minimum value not met.");
		assertThrown(()->x.getX13(10.1d)).messages().contains("Maximum value exceeded.");
		assertEquals("{x:'1.0'}",x.getX14(1d));
		assertEquals("{x:'10.0'}",x.getX14(10d));
		assertThrown(()->x.getX14(0.9d)).messages().contains("Minimum value not met.");
		assertThrown(()->x.getX14(10.1d)).messages().contains("Maximum value exceeded.");
		assertEquals("{x:'1.1'}",x.getX15(1.1d));
		assertEquals("{x:'9.9'}",x.getX15(9.9d));
		assertThrown(()->x.getX15(1d)).messages().contains("Minimum value not met.");
		assertThrown(()->x.getX15(10d)).messages().contains("Maximum value exceeded.");
		assertEquals("{x:'1'}",x.getX16((byte)1));
		assertEquals("{x:'10'}",x.getX16((byte)10));
		assertThrown(()->x.getX16((byte)0)).messages().contains("Minimum value not met.");
		assertThrown(()->x.getX16((byte)11)).messages().contains("Maximum value exceeded.");
		assertEquals("{x:'1'}",x.getX17((byte)1));
		assertEquals("{x:'10'}",x.getX17((byte)10));
		assertThrown(()->x.getX17((byte)0)).messages().contains("Minimum value not met.");
		assertThrown(()->x.getX17((byte)11)).messages().contains("Maximum value exceeded.");
		assertEquals("{x:'2'}",x.getX18((byte)2));
		assertEquals("{x:'9'}",x.getX18((byte)9));
		assertThrown(()->x.getX18((byte)1)).messages().contains("Minimum value not met.");
		assertThrown(()->x.getX18((byte)10)).messages().contains("Maximum value exceeded.");
		assertEquals("{x:'1'}",x.getX19(new AtomicInteger(1)));
		assertEquals("{x:'10'}",x.getX19(new AtomicInteger(10)));
		assertThrown(()->x.getX19(new AtomicInteger(0))).messages().contains("Minimum value not met.");
		assertThrown(()->x.getX19(new AtomicInteger(11))).messages().contains("Maximum value exceeded.");
		assertEquals("{x:'1'}",x.getX20(new AtomicInteger(1)));
		assertEquals("{x:'10'}",x.getX20(new AtomicInteger(10)));
		assertThrown(()->x.getX20(new AtomicInteger(0))).messages().contains("Minimum value not met.");
		assertThrown(()->x.getX20(new AtomicInteger(11))).messages().contains("Maximum value exceeded.");
		assertEquals("{x:'2'}",x.getX21(new AtomicInteger(2)));
		assertEquals("{x:'9'}",x.getX21(new AtomicInteger(9)));
		assertThrown(()->x.getX21(new AtomicInteger(1))).messages().contains("Minimum value not met.");
		assertThrown(()->x.getX21(new AtomicInteger(10))).messages().contains("Maximum value exceeded.");
		assertEquals("{x:'1'}",x.getX22(new BigDecimal(1)));
		assertEquals("{x:'10'}",x.getX22(new BigDecimal(10)));
		assertThrown(()->x.getX22(new BigDecimal(0))).messages().contains("Minimum value not met.");
		assertThrown(()->x.getX22(new BigDecimal(11))).messages().contains("Maximum value exceeded.");
		assertEquals("{x:'1'}",x.getX23(new BigDecimal(1)));
		assertEquals("{x:'10'}",x.getX23(new BigDecimal(10)));
		assertThrown(()->x.getX23(new BigDecimal(0))).messages().contains("Minimum value not met.");
		assertThrown(()->x.getX23(new BigDecimal(11))).messages().contains("Maximum value exceeded.");
		assertEquals("{x:'2'}",x.getX24(new BigDecimal(2)));
		assertEquals("{x:'9'}",x.getX24(new BigDecimal(9)));
		assertThrown(()->x.getX24(new BigDecimal(1))).messages().contains("Minimum value not met.");
		assertThrown(()->x.getX24(new BigDecimal(10))).messages().contains("Maximum value exceeded.");
		assertEquals("{x:'1'}",x.getX25(1));
		assertEquals("{x:'10'}",x.getX25(10));
		assertThrown(()->x.getX25(0)).messages().contains("Minimum value not met.");
		assertThrown(()->x.getX25(11)).messages().contains("Maximum value exceeded.");
		assertEquals("{x:'1'}",x.getX26(1));
		assertEquals("{x:'10'}",x.getX26(10));
		assertThrown(()->x.getX26(0)).messages().contains("Minimum value not met.");
		assertThrown(()->x.getX26(11)).messages().contains("Maximum value exceeded.");
		assertEquals("{x:'2'}",x.getX27(2));
		assertEquals("{x:'9'}",x.getX27(9));
		assertThrown(()->x.getX27(1)).messages().contains("Minimum value not met.");
		assertThrown(()->x.getX27(10)).messages().contains("Maximum value exceeded.");
		assertEquals("{x:'1'}",x.getX28((short)1));
		assertEquals("{x:'10'}",x.getX28((short)10));
		assertThrown(()->x.getX28((short)0)).messages().contains("Minimum value not met.");
		assertThrown(()->x.getX28((short)11)).messages().contains("Maximum value exceeded.");
		assertEquals("{x:'1'}",x.getX29((short)1));
		assertEquals("{x:'10'}",x.getX29((short)10));
		assertThrown(()->x.getX29((short)0)).messages().contains("Minimum value not met.");
		assertThrown(()->x.getX29((short)11)).messages().contains("Maximum value exceeded.");
		assertEquals("{x:'2'}",x.getX30((short)2));
		assertEquals("{x:'9'}",x.getX30((short)9));
		assertThrown(()->x.getX30((short)1)).messages().contains("Minimum value not met.");
		assertThrown(()->x.getX30((short)10)).messages().contains("Maximum value exceeded.");
		assertEquals("{x:'1'}",x.getX31(1l));
		assertEquals("{x:'10'}",x.getX31(10l));
		assertThrown(()->x.getX31(0l)).messages().contains("Minimum value not met.");
		assertThrown(()->x.getX31(11l)).messages().contains("Maximum value exceeded.");
		assertEquals("{x:'1'}",x.getX32(1l));
		assertEquals("{x:'10'}",x.getX32(10l));
		assertThrown(()->x.getX32(0l)).messages().contains("Minimum value not met.");
		assertThrown(()->x.getX32(11l)).messages().contains("Maximum value exceeded.");
		assertEquals("{x:'2'}",x.getX33(2l));
		assertEquals("{x:'9'}",x.getX33(9l));
		assertThrown(()->x.getX33(1l)).messages().contains("Minimum value not met.");
		assertThrown(()->x.getX33(10l)).messages().contains("Maximum value exceeded.");
		assertEquals("{x:'1.0'}",x.getX34(1f));
		assertEquals("{x:'10.0'}",x.getX34(10f));
		assertThrown(()->x.getX34(0.9f)).messages().contains("Minimum value not met.");
		assertThrown(()->x.getX34(10.1f)).messages().contains("Maximum value exceeded.");
		assertEquals("{x:'1.0'}",x.getX35(1f));
		assertEquals("{x:'10.0'}",x.getX35(10f));
		assertThrown(()->x.getX35(0.9f)).messages().contains("Minimum value not met.");
		assertThrown(()->x.getX35(10.1f)).messages().contains("Maximum value exceeded.");
		assertEquals("{x:'1.1'}",x.getX36(1.1f));
		assertEquals("{x:'9.9'}",x.getX36(9.9f));
		assertThrown(()->x.getX36(1f)).messages().contains("Minimum value not met.");
		assertThrown(()->x.getX36(10f)).messages().contains("Maximum value exceeded.");
		assertEquals("{x:'1.0'}",x.getX37(1d));
		assertEquals("{x:'10.0'}",x.getX37(10d));
		assertThrown(()->x.getX37(0.9d)).messages().contains("Minimum value not met.");
		assertThrown(()->x.getX37(10.1d)).messages().contains("Maximum value exceeded.");
		assertEquals("{x:'1.0'}",x.getX38(1d));
		assertEquals("{x:'10.0'}",x.getX38(10d));
		assertThrown(()->x.getX38(0.9d)).messages().contains("Minimum value not met.");
		assertThrown(()->x.getX38(10.1d)).messages().contains("Maximum value exceeded.");
		assertEquals("{x:'1.1'}",x.getX39(1.1d));
		assertEquals("{x:'9.9'}",x.getX39(9.9d));
		assertThrown(()->x.getX39(1d)).messages().contains("Minimum value not met.");
		assertThrown(()->x.getX39(10d)).messages().contains("Maximum value exceeded.");
		assertEquals("{x:'1'}",x.getX40((byte)1));
		assertEquals("{x:'10'}",x.getX40((byte)10));
		assertThrown(()->x.getX40((byte)0)).messages().contains("Minimum value not met.");
		assertThrown(()->x.getX40((byte)11)).messages().contains("Maximum value exceeded.");
		assertEquals("{x:'1'}",x.getX41((byte)1));
		assertEquals("{x:'10'}",x.getX41((byte)10));
		assertThrown(()->x.getX41((byte)0)).messages().contains("Minimum value not met.");
		assertThrown(()->x.getX41((byte)11)).messages().contains("Maximum value exceeded.");
		assertEquals("{x:'2'}",x.getX42((byte)2));
		assertEquals("{x:'9'}",x.getX42((byte)9));
		assertThrown(()->x.getX42((byte)1)).messages().contains("Minimum value not met.");
		assertThrown(()->x.getX42((byte)10)).messages().contains("Maximum value exceeded.");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// @Path(maxItems,minItems,uniqueItems)
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class D {
		@RestOp(path="/{x}")
		public String get(@Path("*") OMap m) {
			m.removeAll("/*","/**");
			return m.toString();
		}
	}

	@Remote
	public static interface D1 {
		@RemoteOp(path="/{x}") String getX1(@Path(n="x",cf="pipes",mini=1,maxi=2) String...b);
		@RemoteOp(path="/{x}") String getX2(@Path(n="x",items=@Items(cf="pipes",mini=1,maxi=2)) String[]...b);
		@RemoteOp(path="/{x}") String getX3(@Path(n="x",cf="pipes",ui=false) String...b);
		@RemoteOp(path="/{x}") String getX4(@Path(n="x",items=@Items(cf="pipes",ui=false)) String[]...b);
		@RemoteOp(path="/{x}") String getX5(@Path(n="x",cf="pipes",ui=true) String...b);
		@RemoteOp(path="/{x}") String getX6(@Path(n="x",items=@Items(cf="pipes",ui=true)) String[]...b);
	}

	@Test
	public void d01_path_miniMaxi() throws Exception {
		D1 x = remote(D.class,D1.class);
		assertEquals("{x:'1'}",x.getX1("1"));
		assertEquals("{x:'1|2'}",x.getX1("1","2"));
		assertThrown(()->x.getX1()).messages().contains("Minimum number of items not met.");
		assertThrown(()->x.getX1("1","2","3")).messages().contains("Maximum number of items exceeded.");
		assertEquals("{x:'null'}",x.getX1((String)null));
		assertEquals("{x:'1'}",x.getX2(new String[]{"1"}));
		assertEquals("{x:'1|2'}",x.getX2(new String[]{"1","2"}));
		assertThrown(()->x.getX2(new String[]{})).messages().contains("Minimum number of items not met.");
		assertThrown(()->x.getX2(new String[]{"1","2","3"})).messages().contains("Maximum number of items exceeded.");
		assertEquals("{x:'null'}",x.getX2(new String[]{null}));
		assertEquals("{x:'1|1'}",x.getX3("1","1"));
		assertEquals("{x:'1|1'}",x.getX4(new String[]{"1","1"}));
		assertEquals("{x:'1|2'}",x.getX5("1","2"));
		assertThrown(()->x.getX5("1","1")).messages().contains("Duplicate items not allowed.");
		assertEquals("{x:'1|2'}",x.getX6(new String[]{"1","2"}));
		assertThrown(()->x.getX6(new String[]{"1","1"})).messages().contains("Duplicate items not allowed.");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// @Path(maxLength,minLength,enum)
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class E {
		@RestOp(path="/{x}")
		public String get(@Path("*") OMap m) {
			m.removeAll("/*","/**");
			return m.toString();
		}
	}

	@Remote
	public static interface E1 {
		@RemoteOp(path="/{x}") String getX1(@Path(n="x",minl=2,maxl=3) String b);
		@RemoteOp(path="/{x}") String getX2(@Path(n="x",cf="pipes",items=@Items(minl=2,maxl=3)) String...b);
		@RemoteOp(path="/{x}") String getX3(@Path(n="x",e={"foo"}) String b);
		@RemoteOp(path="/{x}") String getX4(@Path(n="x",cf="pipes",items=@Items(e={"foo"})) String...b);
		@RemoteOp(path="/{x}") String getX5(@Path(n="x",p="foo\\d{1,3}") String b);
		@RemoteOp(path="/{x}") String getX6(@Path(n="x",cf="pipes",items=@Items(p="foo\\d{1,3}")) String...b);
	}

	@Test
	public void e01_path_minLength_maxLength() throws Exception {
		E1 x = remote(E.class,E1.class);
		assertEquals("{x:'12'}",x.getX1("12"));
		assertEquals("{x:'123'}",x.getX1("123"));
		assertThrown(()->x.getX1("1")).messages().contains("Minimum length of value not met.");
		assertThrown(()->x.getX1("1234")).messages().contains("Maximum length of value exceeded.");
		assertEquals("{x:'12|34'}",x.getX2("12","34"));
		assertEquals("{x:'123|456'}",x.getX2("123","456"));
		assertThrown(()->x.getX2("1","2")).messages().contains("Minimum length of value not met.");
		assertThrown(()->x.getX2("1234","5678")).messages().contains("Maximum length of value exceeded.");
		assertEquals("{x:'12|null'}",x.getX2("12",null));
		assertEquals("{x:'foo'}",x.getX3("foo"));
		assertThrown(()->x.getX3("bar")).messages().contains("Value does not match one of the expected values.  Must be one of the following:  foo");
		assertEquals("{x:'foo'}",x.getX4("foo"));
		assertThrown(()->x.getX4("bar")).messages().contains("Value does not match one of the expected values.  Must be one of the following:  foo");
		assertEquals("{x:'null'}",x.getX4((String)null));
		assertEquals("{x:'foo123'}",x.getX5("foo123"));
		assertThrown(()->x.getX5("bar")).messages().contains("Value does not match expected pattern.  Must match pattern: foo\\d{1,3}");
		assertEquals("{x:'foo123'}",x.getX6("foo123"));
		assertThrown(()->x.getX6("foo")).messages().contains("Value does not match expected pattern.  Must match pattern: foo\\d{1,3}");
		assertEquals("{x:'null'}",x.getX6((String)null));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// @Path(multipleOf)
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class F {
		@RestOp(path="/{x}")
		public String get(@Path("*") OMap m) {
			m.removeAll("/*","/**");
			return m.toString();
		}
	}

	@Remote
	public static interface F1 {
		@RemoteOp(path="/{x}") String getX1(@Path(n="x",mo="2") int b);
		@RemoteOp(path="/{x}") String getX2(@Path(n="x",mo="2") short b);
		@RemoteOp(path="/{x}") String getX3(@Path(n="x",mo="2") long b);
		@RemoteOp(path="/{x}") String getX4(@Path(n="x",mo="2") float b);
		@RemoteOp(path="/{x}") String getX5(@Path(n="x",mo="2") double b);
		@RemoteOp(path="/{x}") String getX6(@Path(n="x",mo="2") byte b);
		@RemoteOp(path="/{x}") String getX7(@Path(n="x",mo="2") AtomicInteger b);
		@RemoteOp(path="/{x}") String getX8(@Path(n="x",mo="2") BigDecimal b);
		@RemoteOp(path="/{x}") String getX9(@Path(n="x",mo="2") Integer b);
		@RemoteOp(path="/{x}") String getX10(@Path(n="x",mo="2") Short b);
		@RemoteOp(path="/{x}") String getX11(@Path(n="x",mo="2") Long b);
		@RemoteOp(path="/{x}") String getX12(@Path(n="x",mo="2") Float b);
		@RemoteOp(path="/{x}") String getX13(@Path(n="x",mo="2") Double b);
		@RemoteOp(path="/{x}") String getX14(@Path(n="x",mo="2") Byte b);
	}

	@Test
	public void f01_path_multipleOf() throws Exception {
		F1 x = remote(F.class,F1.class);
		assertEquals("{x:'4'}",x.getX1(4));
		assertThrown(()->x.getX1(5)).messages().contains("Multiple-of not met.");
		assertEquals("{x:'4'}",x.getX2((short)4));
		assertThrown(()->x.getX2((short)5)).messages().contains("Multiple-of not met.");
		assertEquals("{x:'4'}",x.getX3(4));
		assertThrown(()->x.getX3(5)).messages().contains("Multiple-of not met.");
		assertEquals("{x:'4.0'}",x.getX4(4));
		assertThrown(()->x.getX4(5)).messages().contains("Multiple-of not met.");
		assertEquals("{x:'4.0'}",x.getX5(4));
		assertThrown(()->x.getX5(5)).messages().contains("Multiple-of not met.");
		assertEquals("{x:'4'}",x.getX6((byte)4));
		assertThrown(()->x.getX6((byte)5)).messages().contains("Multiple-of not met.");
		assertEquals("{x:'4'}",x.getX7(new AtomicInteger(4)));
		assertThrown(()->x.getX7(new AtomicInteger(5))).messages().contains("Multiple-of not met.");
		assertEquals("{x:'4'}",x.getX8(new BigDecimal(4)));
		assertThrown(()->x.getX8(new BigDecimal(5))).messages().contains("Multiple-of not met.");
		assertEquals("{x:'4'}",x.getX9(4));
		assertThrown(()->x.getX9(5)).messages().contains("Multiple-of not met.");
		assertEquals("{x:'4'}",x.getX10((short)4));
		assertThrown(()->x.getX10((short)5)).messages().contains("Multiple-of not met.");
		assertEquals("{x:'4'}",x.getX11(4l));
		assertThrown(()->x.getX11(5l)).messages().contains("Multiple-of not met.");
		assertEquals("{x:'4.0'}",x.getX12(4f));
		assertThrown(()->x.getX12(5f)).messages().contains("Multiple-of not met.");
		assertEquals("{x:'4.0'}",x.getX13(4d));
		assertThrown(()->x.getX13(5d)).messages().contains("Multiple-of not met.");
		assertEquals("{x:'4'}",x.getX14((byte)4));
		assertThrown(()->x.getX14((byte)5)).messages().contains("Multiple-of not met.");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// @Path(required)
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class G {
	}

	@Remote
	public static interface G1 {
		@RemoteOp(path="/{x}") String getX1(@Path("x") String b);
	}

	@Test
	public void h01_path_required() throws Exception {
		G1 x = remote(G.class,G1.class);
		assertThrown(()->x.getX1(null)).messages().contains("Required value not provided.");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// @Path(serializer)
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class H {
		@RestOp(path="/{x}")
		public String get(@Path("*") OMap m) {
			m.removeAll("/*","/**");
			return m.toString();
		}
	}

	@Remote
	public static interface H1 {
		@RemoteOp(path="/{x}") String getX1(@Path(n="x",serializer=MockWriterSerializer.X.class) String b);
	}

	@Test
	public void h01_path_serializer() throws Exception {
		H1 x = remote(H.class,H1.class);
		assertEquals("{x:'xXx'}",x.getX1("X"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// RequstBean @Path
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class K  {
		@RestOp(path="/*")
		public String get(RestRequest req) throws Exception {
			return req.getPathParams().getRemainder().orElse(null);
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// RequstBean @Path, Simple values
	//-----------------------------------------------------------------------------------------------------------------

	@Remote(path="/")
	public static interface K1 {
		@RemoteOp(path="/{a}/{b}/{c}/{e}/{g}/{h}") String getX1(@Request K1a rb);
		@RemoteOp(path="/{a}/{b}/{c}/{e}/{g}/{h}") String getX2(@Request(serializer=MockWriterSerializer.X.class) K1a rb);
	}

	public static class K1a {
		@Path
		public String getA() {
			return "a1";
		}
		@Path("b")
		public String getX1() {
			return "b1";
		}
		@Path(n="c")
		public String getX2() {
			return "c1";
		}
		@Path(n="e",aev=true)
		public String getX4() {
			return "";
		}
		@Path("g")
		public String getX6() {
			return "true";
		}
		@Path("h")
		public String getX7() {
			return "123";
		}
	}

	@Test
	public void k01_requestBean_simpleVals() throws Exception {
		K1 x1 = remote(K.class,K1.class);
		K1 x2 = client(K.class).partSerializer(UonSerializer.class).build().getRemote(K1.class);
		assertEquals("a1/b1/c1//true/123",x1.getX1(new K1a()));
		assertEquals("a1/b1/c1//'true'/'123'",x2.getX1(new K1a()));
		assertEquals("xa1x/xb1x/xc1x/xx/xtruex/x123x",x2.getX2(new K1a()));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// RequstBean @Path, Maps
	//-----------------------------------------------------------------------------------------------------------------

	@Remote(path="/")
	public static interface K2 {
		@RemoteOp(path="/{a1}/{a2}/{a3}/{a4}/{b1}/{b2}/{b3}/{c1}/{c2}/{c3}/{c4}") String getX1(@Request K2a rb);
		@RemoteOp(path="/{a1}/{a2}/{a3}/{a4}/{b1}/{b2}/{b3}/{c1}/{c2}/{c3}/{c4}") String getX2(@Request(serializer=MockWriterSerializer.X.class) K2a rb);
	}

	public static class K2a {
		@Path(n="*",aev=true)
		public Map<String,Object> getA() {
			return AMap.of("a1","v1","a2",123,"a3",null,"a4","");
		}
		@Path("*")
		public Map<String,Object> getB() {
			return AMap.of("b1","true","b2","123","b3","null");
		}
		@Path(n="*",aev=true)
		public Map<String,Object> getC() {
			return AMap.of("c1","v1","c2",123,"c3",null,"c4","");
		}
		@Path("*")
		public Map<String,Object> getD() {
			return null;
		}
	}

	@Test
	public void k02_reauestBean_maps() throws Exception {
		K2 x1 = remote(K.class,K2.class);
		K2 x2 = client(K.class).partSerializer(UonSerializer.class).build().getRemote(K2.class);
		assertEquals("v1/123/null//true/123/null/v1/123/null/",x1.getX1(new K2a()));
		assertEquals("v1/123/null//'true'/'123'/'null'/v1/123/null/",x2.getX1(new K2a()));
		assertEquals("xv1x/x123x/null/xx/xtruex/x123x/xnullx/xv1x/x123x/null/xx",x2.getX2(new K2a()));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// RequstBean @Path, NameValuePairs
	//-----------------------------------------------------------------------------------------------------------------

	@Remote(path="/")
	public static interface K3 {
		@RemoteOp(path="/{a1}/{a2}/{a3}/{a4}/{b1}/{b2}/{b3}/{c1}/{c2}/{c3}/{c4}/{e1}/{e2}/{e3}/{e4}") String getX1(@Request K3a rb);
		@RemoteOp(path="/{a1}/{a2}/{a3}/{a4}/{b1}/{b2}/{b3}/{c1}/{c2}/{c3}/{c4}/{e1}/{e2}/{e3}/{e4}") String getX2(@Request(serializer=MockWriterSerializer.X.class) K3a rb);
	}

	public static class K3a {
		@Path(n="*",aev=true)
		public PartList getA() {
			return parts("a1","v1","a2",123,"a3",null,"a4","");
		}
		@Path("/*")
		public PartList getB() {
			return parts("b1","true","b2","123","b3","null");
		}
		@Path(n="*",aev=true)
		public PartList getC() {
			return parts("c1","v1","c2",123,"c3",null,"c4","");
		}
		@Path("/*")
		public PartList getD() {
			return null;
		}
		@Path(aev=true)
		public NameValuePair[] getE() {
			return parts("e1","v1","e2",123,"e3",null,"e4","").getAll();
		}
	}

	@Test
	public void k03_requestBean_nameValuePairs() throws Exception {
		K3 x1 = remote(K.class,K3.class);
		K3 x2 = client(K.class).partSerializer(UonSerializer.class).build().getRemote(K3.class);
		assertEquals("v1/123/null//true/123/null/v1/123/null//v1/123/null/",x1.getX1(new K3a()));
		assertEquals("v1/123/null//true/123/null/v1/123/null//v1/123/null/",x2.getX1(new K3a()));
		assertEquals("v1/123/null//true/123/null/v1/123/null//v1/123/null/",x2.getX2(new K3a()));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// RequstBean @Path, Collections
	//-----------------------------------------------------------------------------------------------------------------

	@Remote(path="/")
	public static interface K4 {
		@RemoteOp(path="/{a}/{b}/{c}/{d}/{f}/{g}/{h}") String getX1(@Request K4a rb);
		@RemoteOp(path="/{a}/{b}/{c}/{d}/{f}/{g}/{h}") String getX2(@Request(serializer=MockWriterSerializer.X.class) K4a rb);
	}

	public static class K4a {
		@Path
		public List<Object> getA() {
			return AList.of("foo","","true","123","null",true,123,null);
		}
		@Path("b")
		public List<Object> getX1() {
			return AList.of("foo","","true","123","null",true,123,null);
		}
		@Path(n="c",serializer=MockWriterSerializer.X.class)
		public List<Object> getX2() {
			return AList.of("foo","","true","123","null",true,123,null);
		}
		@Path(n="d",aev=true)
		public List<Object> getX3() {
			return AList.create();
		}
		@Path("f")
		public Object[] getX5() {
			return new Object[]{"foo","","true","123","null",true,123,null};
		}
		@Path(n="g",serializer=MockWriterSerializer.X.class)
		public Object[] getX6() {
			return new Object[]{"foo","","true","123","null",true,123,null};
		}
		@Path(n="h",aev=true)
		public Object[] getX7() {
			return new Object[]{};
		}
	}

	@Test
	public void k04_requestBean_collections() throws Exception {
		K4 x1 = remote(K.class,K4.class);
		K4 x2 = client(K.class).partSerializer(UonSerializer.class).build().getRemote(K4.class);
		assertString(x1.getX1(new K4a())).is("foo,,true,123,null,true,123,null/foo,,true,123,null,true,123,null/xfoo||true|123|null|true|123|nullx//foo,,true,123,null,true,123,null/xfoo||true|123|null|true|123|nullx/");
		assertString(x2.getX1(new K4a())).is("@(foo,'','true','123','null',true,123,null)/@(foo,'','true','123','null',true,123,null)/xfoo||true|123|null|true|123|nullx/@()/@(foo,'','true','123','null',true,123,null)/xfoo||true|123|null|true|123|nullx/@()");
		assertString(x2.getX2(new K4a())).is("xfoo||true|123|null|true|123|nullx/xfoo||true|123|null|true|123|nullx/xfoo||true|123|null|true|123|nullx/xx/xfoo||true|123|null|true|123|nullx/xfoo||true|123|null|true|123|nullx/xx");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Helper methods.
	//------------------------------------------------------------------------------------------------------------------

	private static PartList parts(Object...pairs) {
		return partList(pairs);
	}

	private static NameValuePair part(String key, Object val) {
		return basicPart(key, val);
	}

	private static RestClientBuilder client(Class<?> c) {
		return MockRestClient.create(c);
	}

	private static <T> T remote(Class<?> rest, Class<T> t) {
		return MockRestClient.build(rest).getRemote(t);
	}
}
