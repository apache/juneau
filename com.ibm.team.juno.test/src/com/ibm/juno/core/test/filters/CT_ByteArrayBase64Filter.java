/*******************************************************************************
 * Licensed Materials - Property of IBM
 * © Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 * Note to U.S. Government Users Restricted Rights:  Use,
 * duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp.
 *******************************************************************************/
package com.ibm.juno.core.test.filters;

import static com.ibm.juno.core.serializer.SerializerProperties.*;
import static org.junit.Assert.*;

import java.util.*;

import org.junit.*;

import com.ibm.juno.core.filters.*;
import com.ibm.juno.core.json.*;
import com.ibm.juno.core.parser.*;
import com.ibm.juno.core.serializer.*;
import com.ibm.juno.core.test.a.rttests.*;

@SuppressWarnings({"unchecked","hiding","serial"})
public class CT_ByteArrayBase64Filter extends RoundTripTest {

	public CT_ByteArrayBase64Filter(String label, WriterSerializer s, ReaderParser p, int flags) throws Exception {
		super(label, s, p, flags);
	}

	@Override /* RoundTripTest */
	public Class<?>[] getFilters() {
		return new Class<?>[] {
			ByteArrayBase64Filter.class
		};
	}

	//====================================================================================================
	// testPrimitiveArrays
	//====================================================================================================
	@Test
	public void testPrimitiveArrays() throws Exception {
		WriterSerializer s = new JsonSerializer.Simple().addFilters(ByteArrayBase64Filter.class).setProperty(SERIALIZER_trimNullProperties, false);

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