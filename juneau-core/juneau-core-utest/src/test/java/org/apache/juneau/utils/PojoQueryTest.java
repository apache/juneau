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

import static org.apache.juneau.testutils.TestUtils.*;
import static org.junit.Assert.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.json.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.transforms.*;
import org.junit.*;

@SuppressWarnings("rawtypes")
public class PojoQueryTest {

	//====================================================================================================
	// filterCollection, string search, 1 level
	//====================================================================================================
	@Test
	public void testFilterCollectionStringSearchOneLevel() throws Exception {
		SearchArgs sa;
		List results;

		List<A> in = AList.of(new A("foo"),new A("bar"),new A("baz"));

		PojoQuery q = new PojoQuery(in, BeanContext.DEFAULT.createSession());

		sa = SearchArgs.builder().search("f=foo").build();
		results = q.filter(sa);
		assertObjectEquals("[{f:'foo'}]", results);

		sa = SearchArgs.builder().search("f=fo*").build();
		results = q.filter(sa);
		assertObjectEquals("[{f:'foo'}]", results);

		sa = SearchArgs.builder().search("f=*ar").build();
		results = q.filter(sa);
		assertObjectEquals("[{f:'bar'}]", results);

		sa = SearchArgs.builder().search("f=foo bar").build();
		results = q.filter(sa);
		assertObjectEquals("[{f:'foo'},{f:'bar'}]", results);
	}

	public class A {
		public String f;

		A() {}

		A(String f) {
			this.f = f;
		}
	}

	//====================================================================================================
	// filterCollection, date search, 1 level
	//====================================================================================================
	@Test
	public void testFilterCollectionDateSearchOneLevel() throws Exception {
		BeanSession session = BeanContext.DEFAULT.createSession();
		WriterSerializer s = JsonSerializer.create().ssq().pojoSwaps(TemporalCalendarSwap.IsoLocalDateTime.class).build();
		B[] in;
		PojoQuery q;
		SearchArgs sa;
		List results;

		in = new B[] {
			new B(2010, 0, 1),
			new B(2011, 0, 1),
			new B(2011, 0, 31),
			new B(2012, 0, 1)
		};
		q = new PojoQuery(in, session);

		sa = SearchArgs.builder().search("f=2011").build();
		results = q.filter(sa);
		assertEquals("[{f:'2011-01-01T00:00:00'},{f:'2011-01-31T00:00:00'}]", s.serialize(results));

		sa = SearchArgs.builder().search("f=2011.01").build();
		results = q.filter(sa);
		assertEquals("[{f:'2011-01-01T00:00:00'},{f:'2011-01-31T00:00:00'}]", s.serialize(results));

		sa = SearchArgs.builder().search("f=2011.01.01").build();
		results = q.filter(sa);
		assertEquals("[{f:'2011-01-01T00:00:00'}]", s.serialize(results));

		in = new B[] {
			new B(2011, 00, 01, 11, 59, 59),
			new B(2011, 00, 01, 12, 00, 00),
			new B(2011, 00, 01, 12, 59, 59),
			new B(2011, 00, 01, 13, 00, 00)
		};
		q = new PojoQuery(in, session);

		sa = SearchArgs.builder().search("f=2011.01.01.12").build();
		results = q.filter(sa);
		assertEquals("[{f:'2011-01-01T12:00:00'},{f:'2011-01-01T12:59:59'}]", s.serialize(results));

		in = new B[] {
			new B(2011, 00, 01, 12, 29, 59),
			new B(2011, 00, 01, 12, 30, 00),
			new B(2011, 00, 01, 12, 30, 59),
			new B(2011, 00, 01, 12, 31, 00)
		};
		q = new PojoQuery(in, session);
		sa = SearchArgs.builder().search("f=2011.01.01.12.30").build();
		results = q.filter(sa);
		assertEquals("[{f:'2011-01-01T12:30:00'},{f:'2011-01-01T12:30:59'}]", s.serialize(results));

		in = new B[] {
			new B(2011, 00, 01, 12, 30, 29),
			new B(2011, 00, 01, 12, 30, 30),
			new B(2011, 00, 01, 12, 30, 31)
		};
		q = new PojoQuery(in, session);
		sa = SearchArgs.builder().search("f=2011.01.01.12.30.30").build();
		results = q.filter(sa);
		assertEquals("[{f:'2011-01-01T12:30:30'}]", s.serialize(results));

		// Open-ended ranges

		in = new B[] {
			new B(2000, 11, 31),
			new B(2001, 00, 01)
		};
		q = new PojoQuery(in, session);

		sa = SearchArgs.builder().search("f>2000").build();
		results = q.filter(sa);
		assertEquals("[{f:'2001-01-01T00:00:00'}]", s.serialize(results));

		sa = SearchArgs.builder().search("f>=2001").build();
		results = q.filter(sa);
		assertEquals("[{f:'2001-01-01T00:00:00'}]", s.serialize(results));

		sa = SearchArgs.builder().search("f<2001").build();
		results = q.filter(sa);
		assertEquals("[{f:'2000-12-31T00:00:00'}]", s.serialize(results));

		sa = SearchArgs.builder().search("f<=2000").build();
		results = q.filter(sa);
		assertEquals("[{f:'2000-12-31T00:00:00'}]", s.serialize(results));

		in = new B[] {
			new B(2011, 00, 01, 12, 29, 59),
			new B(2011, 00, 01, 12, 30, 00)
		};
		q = new PojoQuery(in, session);

		sa = SearchArgs.builder().search("f>=2011.01.01.12.30").build();
		results = q.filter(sa);
		assertEquals("[{f:'2011-01-01T12:30:00'}]", s.serialize(results));

		sa = SearchArgs.builder().search("f<2011.01.01.12.30").build();
		results = q.filter(sa);
		assertEquals("[{f:'2011-01-01T12:29:59'}]", s.serialize(results));

		in = new B[] {
			new B(2011, 00, 01, 12, 30, 59),
			new B(2011, 00, 01, 12, 31, 00)
		};
		q = new PojoQuery(in, session);

		sa = SearchArgs.builder().search("f>2011.01.01.12.30").build();
		results = q.filter(sa);
		assertEquals("[{f:'2011-01-01T12:31:00'}]", s.serialize(results));

		sa = SearchArgs.builder().search("f<=2011.01.01.12.30").build();
		results = q.filter(sa);
		assertEquals("[{f:'2011-01-01T12:30:59'}]", s.serialize(results));

		// Closed range

		in = new B[] {
			new B(2000, 11, 31, 23, 59, 59),
			new B(2001, 00, 01, 00, 00, 00),
			new B(2003, 05, 30, 23, 59, 59),
			new B(2003, 06, 01, 00, 00, 00)
		};
		q = new PojoQuery(in, session);

		sa = SearchArgs.builder().search("f=2001 - 2003.06.30").build();
		results = q.filter(sa);
		assertEquals("[{f:'2001-01-01T00:00:00'},{f:'2003-06-30T23:59:59'}]", s.serialize(results));

		// ORed timestamps

		in = new B[] {
			new B(2000, 11, 31),
			new B(2001, 00, 01),
			new B(2001, 11, 31),
			new B(2002, 00, 01)
		};
		q = new PojoQuery(in, session);

		sa = SearchArgs.builder().search("f=2001 2003 2005").build();
		results = q.filter(sa);
		assertEquals("[{f:'2001-01-01T00:00:00'},{f:'2001-12-31T00:00:00'}]", s.serialize(results));

		in = new B[] {
			new B(2002, 11, 31),
			new B(2003, 00, 01),
			new B(2003, 11, 31),
			new B(2004, 00, 01)
		};
		q = new PojoQuery(in, session);

		sa = SearchArgs.builder().search("f=2001 2003 2005").build();
		results = q.filter(sa);
		assertEquals("[{f:'2003-01-01T00:00:00'},{f:'2003-12-31T00:00:00'}]", s.serialize(results));

		in = new B[] {
			new B(2004, 11, 31),
			new B(2005, 00, 01),
			new B(2005, 11, 31),
			new B(2006, 00, 01)
		};
		q = new PojoQuery(in, session);

		sa = SearchArgs.builder().search("f=2001 2003 2005").build();
		results = q.filter(sa);
		assertEquals("[{f:'2005-01-01T00:00:00'},{f:'2005-12-31T00:00:00'}]", s.serialize(results));
	}

	public class B {
		public Calendar f;

		B() {}

		B(int year, int month, int day) {
			this.f = new GregorianCalendar(year, month, day);
		}

		B(int year, int month, int day, int hour, int minute, int second) {
			this.f = new GregorianCalendar(year, month, day, hour, minute, second);
		}
	}

	//====================================================================================================
	// filterCollection, int search, 1 level
	//====================================================================================================
	@Test
	public void testFilterCollectionIntSearchOneLevel() throws Exception {
		BeanSession session = BeanContext.DEFAULT.createSession();
		SearchArgs sa;
		List results;

		List<C> in = AList.of(new C(1),new C(2),new C(3));

		PojoQuery q = new PojoQuery(in, session);

		sa = SearchArgs.builder().search("f=1").build();
		results = q.filter(sa);
		assertObjectEquals("[{f:1}]", results);

		sa = SearchArgs.builder().search("f>1").build();
		results = q.filter(sa);
		assertObjectEquals("[{f:2},{f:3}]", results);

		sa = SearchArgs.builder().search("f>=2").build();
		results = q.filter(sa);
		assertObjectEquals("[{f:2},{f:3}]", results);

		sa = SearchArgs.builder().search("f<=2").build();
		results = q.filter(sa);
		assertObjectEquals("[{f:1},{f:2}]", results);

		sa = SearchArgs.builder().search("f<2").build();
		results = q.filter(sa);
		assertObjectEquals("[{f:1}]", results);

		sa = SearchArgs.builder().search("f=1 3").build();
		results = q.filter(sa);
		assertObjectEquals("[{f:1},{f:3}]", results);
	}

	public class C {
		public int f;

		C() {}

		C(int f) {
			this.f = f;
		}
	}

	//====================================================================================================
	// filterCollection, view, 1 level
	//====================================================================================================
	@Test
	public void testFilterCollectionViewOneLevel() throws Exception {
		BeanSession session = BeanContext.DEFAULT.createSession();
		SearchArgs sa;
		List results;

		List<E> in = AList.of(new E("foo", 1, true),new E("bar", 2, false),new E("baz", 3, true));

		PojoQuery q = new PojoQuery(in, session);

		sa = SearchArgs.builder().view("f1").build();
		results = q.filter(sa);
		assertObjectEquals("[{f1:'foo'},{f1:'bar'},{f1:'baz'}]", results);

		sa = SearchArgs.builder().view("f2").build();
		results = q.filter(sa);
		assertObjectEquals("[{f2:1},{f2:2},{f2:3}]", results);

		sa = SearchArgs.builder().view("f3").build();
		results = q.filter(sa);
		assertObjectEquals("[{f3:true},{f3:false},{f3:true}]", results);

		sa = SearchArgs.builder().view("f3,f2,f1").build();
		results = q.filter(sa);
		assertObjectEquals("[{f3:true,f2:1,f1:'foo'},{f3:false,f2:2,f1:'bar'},{f3:true,f2:3,f1:'baz'}]", results);
	}

	public class E {
		public String f1;
		public int f2;
		public boolean f3;

		E() {}

		E(String f1, int f2, boolean f3) {
			this.f1 = f1;
			this.f2 = f2;
			this.f3 = f3;
		}
	}


	//====================================================================================================
	// testSorting
	//====================================================================================================
	@Test
	public void testSorting() throws Exception {
		BeanSession session = BeanContext.DEFAULT.createSession();
		WriterSerializer s = JsonSerializer.create().ssq().pojoSwaps(TemporalCalendarSwap.IsoLocalDateTime.class).build();
		SearchArgs sa;
		List results;

		I[] in = new I[] {
			new I(1, "foo", true, 2010, 1, 1),
			new I(2, "bar", false, 2011, 1, 1),
			new I(3, "baz", true, 2012, 1, 1),
		};

		PojoQuery q = new PojoQuery(in, session);

		sa = SearchArgs.builder().sort("f2").view("f1, f2").build();
		results = q.filter(sa);
		assertEquals("[{f1:2,f2:'bar'},{f1:3,f2:'baz'},{f1:1,f2:'foo'}]", s.serialize(results));

		sa = SearchArgs.builder().sort("f2-").view("f1,f2").build();
		results = q.filter(sa);
		assertEquals("[{f1:1,f2:'foo'},{f1:3,f2:'baz'},{f1:2,f2:'bar'}]", s.serialize(results));

		sa = SearchArgs.builder().sort("f3").view("f1,f3").build();
		results = q.filter(sa);
		assertEquals("[{f1:2,f3:false},{f1:1,f3:true},{f1:3,f3:true}]", s.serialize(results));

		sa = SearchArgs.builder().sort("f3,f1+").view("f1,f3").build();
		results = q.filter(sa);
		assertEquals("[{f1:2,f3:false},{f1:1,f3:true},{f1:3,f3:true}]", s.serialize(results));

		sa = SearchArgs.builder().sort("f3,f1-").view("f1,f3").build();
		results = q.filter(sa);
		assertEquals("[{f1:2,f3:false},{f1:3,f3:true},{f1:1,f3:true}]", s.serialize(results));

		sa = SearchArgs.builder().sort("f1").view("f1").limit(1).position(0).build();
		results = q.filter(sa);
		assertEquals("[{f1:1}]", s.serialize(results));

		sa = SearchArgs.builder().sort("f1").view("f1").limit(3).position(0).build();
		results = q.filter(sa);
		assertEquals("[{f1:1},{f1:2},{f1:3}]", s.serialize(results));

		sa = SearchArgs.builder().sort("f1").view("f1").limit(1).position(2).build();
		results = q.filter(sa);
		assertEquals("[{f1:3}]", s.serialize(results));

		sa = SearchArgs.builder().sort("f1").view("f1").limit(100).position(2).build();
		results = q.filter(sa);
		assertEquals("[{f1:3}]", s.serialize(results));
	}

	public class I {
		public int f1;
		public String f2;
		public boolean f3;
		public Calendar f4;

		I() {}

		I(int f1, String f2, boolean f3, int year, int month, int day) {
			this.f1 = f1;
			this.f2 = f2;
			this.f3 = f3;
			this.f4 = new GregorianCalendar(year, month, day);
		}
	}
}