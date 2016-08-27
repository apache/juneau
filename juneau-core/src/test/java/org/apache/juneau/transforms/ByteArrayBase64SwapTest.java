/***************************************************************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 ***************************************************************************************************************************/
package org.apache.juneau.transforms;

import static org.apache.juneau.serializer.SerializerContext.*;
import static org.junit.Assert.*;

import java.util.*;

import org.apache.juneau.a.rttests.*;
import org.apache.juneau.json.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.serializer.*;
import org.junit.*;

@SuppressWarnings({"unchecked","hiding","serial","javadoc"})
public class ByteArrayBase64SwapTest extends RoundTripTest {

	public ByteArrayBase64SwapTest(String label, Serializer s, Parser p, int flags) throws Exception {
		super(label, s, p, flags);
	}

	@Override /* RoundTripTest */
	public Class<?>[] getTransforms() {
		return new Class<?>[] {
			ByteArrayBase64Swap.class
		};
	}

	//====================================================================================================
	// testPrimitiveArrays
	//====================================================================================================
	@Test
	public void testPrimitiveArrays() throws Exception {
		WriterSerializer s = new JsonSerializer.Simple().addTransforms(ByteArrayBase64Swap.class).setProperty(SERIALIZER_trimNullProperties, false);

		byte[] a1 = {1,2,3};
		assertEquals("'AQID'", s.serialize(a1));
		a1 = roundTrip(a1, byte[].class);
		assertEquals(1, a1[0]);

		byte[][] a2 = {{1,2,3},{4,5,6},null};
		assertEquals("['AQID','BAUG',null]", s.serialize(a2));
		a2 = roundTrip(a2, byte[][].class);
		assertEquals(1, a2[0][0]);
		assertNull(a2[2]);

		byte[] a3 = null;
		assertEquals("null", s.serialize(a3));
		a3 = roundTrip(a3, byte[].class);
		assertNull(a3);

		if (p == null)
			return;

		List<byte[]> fl = new ArrayList<byte[]>() {{
			add(new byte[]{1,2,3});
			add(new byte[]{4,5,6});
			add(null);
		}};
		assertEquals("['AQID','BAUG',null]", s.serialize(fl));
		fl = roundTrip(fl, p.getBeanContext().getCollectionClassMeta(List.class, byte[].class));
		assertEquals(1, fl.get(0)[0]);
		assertEquals(5, fl.get(1)[1]);
		assertNull(fl.get(2));

		Map<String,byte[]> fm = new LinkedHashMap<String,byte[]>() {{
			put("foo", new byte[]{1,2,3});
			put("bar", null);
			put(null, new byte[]{4,5,6});
			put("null", new byte[]{7,8,9});
		}};
		fm = roundTrip(fm, p.getBeanContext().getMapClassMeta(Map.class, String.class, byte[].class));
		assertEquals(1, fm.get("foo")[0]);
		assertNull(fm.get(1));
		assertEquals(5, fm.get(null)[1]);
		assertEquals(8, fm.get("null")[1]);
	}

	//====================================================================================================
	// testBean
	//====================================================================================================
	@Test
	public void testBean() throws Exception {
		A t = new A().init();
		t = roundTrip(t, A.class);
		assertEquals(1, t.f1[0]);
		assertEquals(4, t.f2[1][0]);
		assertNull(t.f2[2]);
		assertNull(t.f3);
		assertEquals(1, t.fl.get(0)[0]);
		assertNull(t.fl.get(2));
		assertEquals(1, t.fm.get("foo")[0]);
		assertNull(t.fm.get("bar"));
		assertEquals(4, t.fm.get(null)[0]);
		assertEquals(1, t.flb.get(0).fl.get(0)[0]);
		assertNull(t.flb.get(1));
		assertEquals(1, t.fmb.get("foo").fl.get(0)[0]);
		assertNull(t.fmb.get("bar"));
	}

	public static class A {
		public byte[] f1;
		public byte[][] f2;
		public byte[] f3;
		public List<byte[]> fl;
		public Map<String,byte[]> fm;
		public List<B> flb;
		public Map<String,B> fmb;

		public A init() {
			f1 = new byte[]{1,2,3};
			f2 = new byte[][]{{1,2,3},{4,5,6},null};
			f3 = null;
			fl = new ArrayList<byte[]>() {{
				add(new byte[]{1,2,3});
				add(new byte[]{4,5,6});
				add(null);
			}};
			fm = new LinkedHashMap<String,byte[]>() {{
				put("foo", new byte[]{1,2,3});
				put("bar", null);
				put(null, new byte[]{4,5,6});
			}};
			flb = new ArrayList<B>() {{
				add(new B().init());
				add(null);
			}};
			fmb = new LinkedHashMap<String,B>() {{
				put("foo", new B().init());
				put("bar", null);
				put(null, new B().init());
			}};
			return this;
		}
	}

	public static class B {
		public byte[] f1;
		public byte[][] f2;
		public byte[] f3;
		public List<byte[]> fl;
		public Map<String,byte[]> fm;

		public B init() {
			f1 = new byte[]{1,2,3};
			f2 = new byte[][]{{1,2,3},{4,5,6},null};
			f3 = null;
			fl = new ArrayList<byte[]>() {{
				add(new byte[]{1,2,3});
				add(new byte[]{4,5,6});
				add(null);
			}};
			fm = new LinkedHashMap<String,byte[]>() {{
				put("foo", new byte[]{1,2,3});
				put("bar", null);
				put(null, new byte[]{4,5,6});
			}};
			return this;
		}
	}
}