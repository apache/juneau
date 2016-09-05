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

import static org.apache.juneau.TestUtils.*;
import static org.junit.Assert.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.json.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.transforms.*;
import org.junit.*;

@SuppressWarnings({"serial","rawtypes","javadoc"})
public class PojoQueryTest {

	//====================================================================================================
	// filterCollection, string search, 1 level
	//====================================================================================================
	@Test
	public void testFilterCollectionStringSearchOneLevel() throws Exception {
		ObjectMap query = null;
		List view = null;
		List sort = null;
		int pos = 0;
		int limit = 0;
		boolean ignoreCase = false;
		BeanContext bc = BeanContext.DEFAULT;
		List results;

		List<A> in = new LinkedList<A>() {{
			add(new A("foo"));
			add(new A("bar"));
			add(new A("baz"));
		}};

		PojoQuery filter = new PojoQuery(in, bc);

		query = new ObjectMap("{f:'foo'}");
		results = filter.filterCollection(query, view, sort, pos, limit, ignoreCase);
		assertObjectEquals("[{f:'foo'}]", results);

		query = new ObjectMap("{f:'fo*'}");
		results = filter.filterCollection(query, view, sort, pos, limit, ignoreCase);
		assertObjectEquals("[{f:'foo'}]", results);

		query = new ObjectMap("{f:'*ar'}");
		results = filter.filterCollection(query, view, sort, pos, limit, ignoreCase);
		assertObjectEquals("[{f:'bar'}]", results);

		query = new ObjectMap("{f:'foo bar'}");
		results = filter.filterCollection(query, view, sort, pos, limit, ignoreCase);
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
		ObjectMap query = null;
		List view = null;
		List sort = null;
		int pos = 0;
		int limit = 0;
		boolean ignoreCase = false;
		BeanContext bc = BeanContext.DEFAULT;
		WriterSerializer s = new JsonSerializer.Simple().addPojoSwaps(CalendarSwap.Simple.class);
		B[] in;
		PojoQuery filter;

		List results;

		in = new B[] {
			new B(2010, 0, 1),
			new B(2011, 0, 1),
			new B(2011, 0, 31),
			new B(2012, 0, 1)
		};
		filter = new PojoQuery(in, bc);

		query = new ObjectMap("{f:'2011'}");
		results = filter.filterCollection(query, view, sort, pos, limit, ignoreCase);
		assertEquals("[{f:'2011/01/01 00:00:00'},{f:'2011/01/31 00:00:00'}]", s.serialize(results));

		query = new ObjectMap("{f:'2011.01'}");
		results = filter.filterCollection(query, view, sort, pos, limit, ignoreCase);
		assertEquals("[{f:'2011/01/01 00:00:00'},{f:'2011/01/31 00:00:00'}]", s.serialize(results));

		query = new ObjectMap("{f:'2011.01.01'}");
		results = filter.filterCollection(query, view, sort, pos, limit, ignoreCase);
		assertEquals("[{f:'2011/01/01 00:00:00'}]", s.serialize(results));

		in = new B[] {
			new B(2011, 00, 01, 11, 59, 59),
			new B(2011, 00, 01, 12, 00, 00),
			new B(2011, 00, 01, 12, 59, 59),
			new B(2011, 00, 01, 13, 00, 00)
		};
		filter = new PojoQuery(in, bc);

		query = new ObjectMap("{f:'2011.01.01.12'}");
		results = filter.filterCollection(query, view, sort, pos, limit, ignoreCase);
		assertEquals("[{f:'2011/01/01 12:00:00'},{f:'2011/01/01 12:59:59'}]", s.serialize(results));

		in = new B[] {
			new B(2011, 00, 01, 12, 29, 59),
			new B(2011, 00, 01, 12, 30, 00),
			new B(2011, 00, 01, 12, 30, 59),
			new B(2011, 00, 01, 12, 31, 00)
		};
		filter = new PojoQuery(in, bc);
		query = new ObjectMap("{f:'2011.01.01.12.30'}");
		results = filter.filterCollection(query, view, sort, pos, limit, ignoreCase);
		assertEquals("[{f:'2011/01/01 12:30:00'},{f:'2011/01/01 12:30:59'}]", s.serialize(results));

		in = new B[] {
			new B(2011, 00, 01, 12, 30, 29),
			new B(2011, 00, 01, 12, 30, 30),
			new B(2011, 00, 01, 12, 30, 31)
		};
		filter = new PojoQuery(in, bc);
		query = new ObjectMap("{f:'2011.01.01.12.30.30'}");
		results = filter.filterCollection(query, view, sort, pos, limit, ignoreCase);
		assertEquals("[{f:'2011/01/01 12:30:30'}]", s.serialize(results));

		// Open-ended ranges

		in = new B[] {
			new B(2000, 11, 31),
			new B(2001, 00, 01)
		};
		filter = new PojoQuery(in, bc);

		query = new ObjectMap("{f:'>2000'}");
		results = filter.filterCollection(query, view, sort, pos, limit, ignoreCase);
		assertEquals("[{f:'2001/01/01 00:00:00'}]", s.serialize(results));

		query = new ObjectMap("{f:'>=2001'}");
		results = filter.filterCollection(query, view, sort, pos, limit, ignoreCase);
		assertEquals("[{f:'2001/01/01 00:00:00'}]", s.serialize(results));

		query = new ObjectMap("{f:'<2001'}");
		results = filter.filterCollection(query, view, sort, pos, limit, ignoreCase);
		assertEquals("[{f:'2000/12/31 00:00:00'}]", s.serialize(results));

		query = new ObjectMap("{f:'<=2000'}");
		results = filter.filterCollection(query, view, sort, pos, limit, ignoreCase);
		assertEquals("[{f:'2000/12/31 00:00:00'}]", s.serialize(results));

		in = new B[] {
			new B(2011, 00, 01, 12, 29, 59),
			new B(2011, 00, 01, 12, 30, 00)
		};
		filter = new PojoQuery(in, bc);

		query = new ObjectMap("{f:'>=2011.01.01.12.30'}");
		results = filter.filterCollection(query, view, sort, pos, limit, ignoreCase);
		assertEquals("[{f:'2011/01/01 12:30:00'}]", s.serialize(results));

		query = new ObjectMap("{f:'<2011.01.01.12.30'}");
		results = filter.filterCollection(query, view, sort, pos, limit, ignoreCase);
		assertEquals("[{f:'2011/01/01 12:29:59'}]", s.serialize(results));

		in = new B[] {
			new B(2011, 00, 01, 12, 30, 59),
			new B(2011, 00, 01, 12, 31, 00)
		};
		filter = new PojoQuery(in, bc);

		query = new ObjectMap("{f:'>2011.01.01.12.30'}");
		results = filter.filterCollection(query, view, sort, pos, limit, ignoreCase);
		assertEquals("[{f:'2011/01/01 12:31:00'}]", s.serialize(results));

		query = new ObjectMap("{f:'<=2011.01.01.12.30'}");
		results = filter.filterCollection(query, view, sort, pos, limit, ignoreCase);
		assertEquals("[{f:'2011/01/01 12:30:59'}]", s.serialize(results));

		// Closed range

		in = new B[] {
			new B(2000, 11, 31, 23, 59, 59),
			new B(2001, 00, 01, 00, 00, 00),
			new B(2003, 05, 30, 23, 59, 59),
			new B(2003, 06, 01, 00, 00, 00)
		};
		filter = new PojoQuery(in, bc);

		query = new ObjectMap("{f:'2001 - 2003.06.30'}");
		results = filter.filterCollection(query, view, sort, pos, limit, ignoreCase);
		assertEquals("[{f:'2001/01/01 00:00:00'},{f:'2003/06/30 23:59:59'}]", s.serialize(results));

		// ORed timestamps

		in = new B[] {
			new B(2000, 11, 31),
			new B(2001, 00, 01),
			new B(2001, 11, 31),
			new B(2002, 00, 01)
		};
		filter = new PojoQuery(in, bc);

		query = new ObjectMap("{f:'2001 2003 2005'}");
		results = filter.filterCollection(query, view, sort, pos, limit, ignoreCase);
		assertEquals("[{f:'2001/01/01 00:00:00'},{f:'2001/12/31 00:00:00'}]", s.serialize(results));

		in = new B[] {
			new B(2002, 11, 31),
			new B(2003, 00, 01),
			new B(2003, 11, 31),
			new B(2004, 00, 01)
		};
		filter = new PojoQuery(in, bc);

		query = new ObjectMap("{f:'2001 2003 2005'}");
		results = filter.filterCollection(query, view, sort, pos, limit, ignoreCase);
		assertEquals("[{f:'2003/01/01 00:00:00'},{f:'2003/12/31 00:00:00'}]", s.serialize(results));

		in = new B[] {
			new B(2004, 11, 31),
			new B(2005, 00, 01),
			new B(2005, 11, 31),
			new B(2006, 00, 01)
		};
		filter = new PojoQuery(in, bc);

		query = new ObjectMap("{f:'2001 2003 2005'}");
		results = filter.filterCollection(query, view, sort, pos, limit, ignoreCase);
		assertEquals("[{f:'2005/01/01 00:00:00'},{f:'2005/12/31 00:00:00'}]", s.serialize(results));
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
		ObjectMap query = null;
		List view = null;
		List sort = null;
		int pos = 0;
		int limit = 0;
		boolean ignoreCase = false;
		BeanContext bc = BeanContext.DEFAULT;
		List results;

		List<C> in = new LinkedList<C>() {{
			add(new C(1));
			add(new C(2));
			add(new C(3));
		}};

		PojoQuery filter = new PojoQuery(in, bc);

		query = new ObjectMap("{f:'1'}");
		results = filter.filterCollection(query, view, sort, pos, limit, ignoreCase);
		assertObjectEquals("[{f:1}]", results);

		query = new ObjectMap("{f:'>1'}");
		results = filter.filterCollection(query, view, sort, pos, limit, ignoreCase);
		assertObjectEquals("[{f:2},{f:3}]", results);

		query = new ObjectMap("{f:'>=2'}");
		results = filter.filterCollection(query, view, sort, pos, limit, ignoreCase);
		assertObjectEquals("[{f:2},{f:3}]", results);

		query = new ObjectMap("{f:'<=2'}");
		results = filter.filterCollection(query, view, sort, pos, limit, ignoreCase);
		assertObjectEquals("[{f:1},{f:2}]", results);

		query = new ObjectMap("{f:'<2'}");
		results = filter.filterCollection(query, view, sort, pos, limit, ignoreCase);
		assertObjectEquals("[{f:1}]", results);

		query = new ObjectMap("{f:'1 3'}");
		results = filter.filterCollection(query, view, sort, pos, limit, ignoreCase);
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
	// filterCollection, string search, 2 level
	//====================================================================================================
	@Test
	public void testFilterCollectionStringSearchTwoLevel() throws Exception {
		ObjectMap query = null;
		List view = null;
		List sort = null;
		int pos = 0;
		int limit = 0;
		boolean ignoreCase = false;
		BeanContext bc = BeanContext.DEFAULT;
		List results;

		List<D1> in = new LinkedList<D1>() {{
			add(new D1("foo"));
			add(new D1("bar"));
			add(new D1("baz"));
		}};

		PojoQuery filter = new PojoQuery(in, bc);

		query = new ObjectMap("{f:{f:'foo'}}");
		results = filter.filterCollection(query, view, sort, pos, limit, ignoreCase);
		assertObjectEquals("[{f:{f:'foo'}}]", results);

		query = new ObjectMap("{f:{f:'fo*'}}");
		results = filter.filterCollection(query, view, sort, pos, limit, ignoreCase);
		assertObjectEquals("[{f:{f:'foo'}}]", results);

		query = new ObjectMap("{f:{f:'*ar'}}");
		results = filter.filterCollection(query, view, sort, pos, limit, ignoreCase);
		assertObjectEquals("[{f:{f:'bar'}}]", results);

		query = new ObjectMap("{f:{f:'foo bar'}}");
		results = filter.filterCollection(query, view, sort, pos, limit, ignoreCase);
		assertObjectEquals("[{f:{f:'foo'}},{f:{f:'bar'}}]", results);
	}

	public class D1 {
		public D2 f;

		D1() {}

		D1(String f) {
			this.f = new D2(f);
		}
	}
	public class D2 {
		public String f;

		D2() {}

		D2(String f) {
			this.f = f;
		}
	}

	//====================================================================================================
	// filterCollection, view, 1 level
	//====================================================================================================
	@Test
	public void testFilterCollectionViewOneLevel() throws Exception {
		ObjectMap query = null;
		List view = null;
		List sort = null;
		int pos = 0;
		int limit = 0;
		boolean ignoreCase = false;
		BeanContext bc = BeanContext.DEFAULT;
		List results;

		List<E> in = new LinkedList<E>() {{
			add(new E("foo", 1, true));
			add(new E("bar", 2, false));
			add(new E("baz", 3, true));
		}};

		PojoQuery filter = new PojoQuery(in, bc);

		view = new ObjectList("['f1']");
		results = filter.filterCollection(query, view, sort, pos, limit, ignoreCase);
		assertObjectEquals("[{f1:'foo'},{f1:'bar'},{f1:'baz'}]", results);

		view = new ObjectList("['f2']");
		results = filter.filterCollection(query, view, sort, pos, limit, ignoreCase);
		assertObjectEquals("[{f2:1},{f2:2},{f2:3}]", results);

		view = new ObjectList("['f3']");
		results = filter.filterCollection(query, view, sort, pos, limit, ignoreCase);
		assertObjectEquals("[{f3:true},{f3:false},{f3:true}]", results);

		view = new ObjectList("['f3','f2','f1']");
		results = filter.filterCollection(query, view, sort, pos, limit, ignoreCase);
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
	// filterCollection, view, 2 level
	//====================================================================================================
	@Test
	public void testFilterCollectionViewTwoLevel() throws Exception {
		ObjectMap query = null;
		List view = null;
		List sort = null;
		int pos = 0;
		int limit = 0;
		boolean ignoreCase = false;
		BeanContext bc = BeanContext.DEFAULT;
		List results;

		List<F1> in = new LinkedList<F1>() {{
			add(new F1("foo"));
			add(new F1("bar"));
			add(new F1("baz"));
		}};

		PojoQuery filter = new PojoQuery(in, bc);

		view = new ObjectList("['f1']");
		results = filter.filterCollection(query, view, sort, pos, limit, ignoreCase);
		assertObjectEquals("[{f1:'foo'},{f1:'bar'},{f1:'baz'}]", results);

		view = new ObjectList("[{f2:['f1']}]");
		results = filter.filterCollection(query, view, sort, pos, limit, ignoreCase);
		assertObjectEquals("[{f2:{f1:'f2_foo'}},{f2:{f1:'f2_bar'}},{f2:{f1:'f2_baz'}}]", results);

		view = new ObjectList("['f1',{f3:['f1']}]");
		results = filter.filterCollection(query, view, sort, pos, limit, ignoreCase);
		assertObjectEquals("[{f1:'foo',f3:[{f1:'f31_foo'},{f1:'f32_foo'}]},{f1:'bar',f3:[{f1:'f31_bar'},{f1:'f32_bar'}]},{f1:'baz',f3:[{f1:'f31_baz'},{f1:'f32_baz'}]}]", results);
	}

	public class F1 {
		public String f1;
		public F2 f2;
		public List<F2> f3;

		F1() {}

		F1(final String f1) {
			this.f1 = f1;
			this.f2 = new F2("f2_"+f1);
			this.f3 = new LinkedList<F2>() {{
				add(new F2("f31_"+f1));
				add(new F2("f32_"+f1));
			}};
		}
	}

	public class F2 {
		public String f1;
		public String f2;

		F2() {}

		F2(String f1) {
			this.f1 = f1;
			this.f2 = f1;
		}
	}

	//====================================================================================================
	// filterMap, 1 level
	//===================================================================================================
	@Test
	public void testFilterMapOneLevel() throws Exception {
		ObjectList view = null;
		BeanContext bc = BeanContext.DEFAULT;
		Map results;

		G in = new G("foo", 1, true);
		PojoQuery filter = new PojoQuery(in, bc);

		view = new ObjectList("['f1']");
		results = filter.filterMap(view);
		assertObjectEquals("{f1:'foo'}", results);

		view = new ObjectList("['f2']");
		results = filter.filterMap(view);
		assertObjectEquals("{f2:1}", results);

		view = new ObjectList("['f3','f1']");
		results = filter.filterMap(view);
		assertObjectEquals("{f3:true,f1:'foo'}", results);
	}

	public class G {
		public String f1;
		public int f2;
		public boolean f3;

		G() {}

		G(String f1, int f2, boolean f3) {
			this.f1 = f1;
			this.f2 = f2;
			this.f3 = f3;
		}
	}

	//====================================================================================================
	// filterMap, 2 level
	//====================================================================================================
	@Test
	public void testFilterMapTwoLevel() throws Exception {
		ObjectList view = null;
		BeanContext bc = BeanContext.DEFAULT;
		Map results;

		H1 in = new H1("foo");

		PojoQuery filter = new PojoQuery(in, bc);

		view = new ObjectList("['f1']");
		results = filter.filterMap(view);
		assertObjectEquals("{f1:'foo'}", results);

		view = new ObjectList("[{f2:['f1']}]");
		results = filter.filterMap(view);
		assertObjectEquals("{f2:{f1:'f2_foo'}}", results);

		view = new ObjectList("['f1',{f3:['f1']}]");
		results = filter.filterMap(view);
		assertObjectEquals("{f1:'foo',f3:[{f1:'f31_foo'},{f1:'f32_foo'}]}", results);
	}

	public class H1 {
		public String f1;
		public H2 f2;
		public List<H2> f3;

		H1() {}

		H1(final String f1) {
			this.f1 = f1;
			this.f2 = new H2("f2_"+f1);
			this.f3 = new LinkedList<H2>() {{
				add(new H2("f31_"+f1));
				add(new H2("f32_"+f1));
			}};
		}
	}

	public class H2 {
		public String f1;
		public String f2;

		H2() {}

		H2(String f1) {
			this.f1 = f1;
			this.f2 = f1;
		}
	}

	//====================================================================================================
	// testSorting
	//====================================================================================================
	@Test
	public void testSorting() throws Exception {
		ObjectMap query = null;
		List view = null;
		List sort = null;
		int pos = 0;
		int limit = 0;
		boolean ignoreCase = false;
		BeanContext bc = BeanContext.DEFAULT;
		WriterSerializer s = new JsonSerializer.Simple().addPojoSwaps(CalendarSwap.Simple.class);
		List results;

		I[] in = new I[] {
			new I(1, "foo", true, 2010, 1, 1),
			new I(2, "bar", false, 2011, 1, 1),
			new I(3, "baz", true, 2012, 1, 1),
		};

		PojoQuery filter = new PojoQuery(in, bc);

		sort = new ObjectList("['f2']");
		view = new ObjectList("['f1','f2']");
		results = filter.filterCollection(query, view, sort, pos, limit, ignoreCase);
		assertEquals("[{f1:2,f2:'bar'},{f1:3,f2:'baz'},{f1:1,f2:'foo'}]", s.serialize(results));

		sort = new ObjectList("[{f2:'d'}]");
		view = new ObjectList("['f1','f2']");
		results = filter.filterCollection(query, view, sort, pos, limit, ignoreCase);
		assertEquals("[{f1:1,f2:'foo'},{f1:3,f2:'baz'},{f1:2,f2:'bar'}]", s.serialize(results));

		sort = new ObjectList("['f3']");
		view = new ObjectList("['f1','f3']");
		results = filter.filterCollection(query, view, sort, pos, limit, ignoreCase);
		assertEquals("[{f1:2,f3:false},{f1:1,f3:true},{f1:3,f3:true}]", s.serialize(results));

		sort = new ObjectList("['f3',{f1:'a'}]");
		view = new ObjectList("['f1','f3']");
		results = filter.filterCollection(query, view, sort, pos, limit, ignoreCase);
		assertEquals("[{f1:2,f3:false},{f1:1,f3:true},{f1:3,f3:true}]", s.serialize(results));

		sort = new ObjectList("['f3',{f1:'d'}]");
		view = new ObjectList("['f1','f3']");
		results = filter.filterCollection(query, view, sort, pos, limit, ignoreCase);
		assertEquals("[{f1:2,f3:false},{f1:3,f3:true},{f1:1,f3:true}]", s.serialize(results));

		sort = new ObjectList("['f1']");
		view = new ObjectList("['f1']");
		limit = 1;
		pos = 0;
		results = filter.filterCollection(query, view, sort, pos, limit, ignoreCase);
		assertEquals("[{f1:1}]", s.serialize(results));

		limit = 3;
		pos = 0;
		results = filter.filterCollection(query, view, sort, pos, limit, ignoreCase);
		assertEquals("[{f1:1},{f1:2},{f1:3}]", s.serialize(results));

		limit = 1;
		pos = 2;
		results = filter.filterCollection(query, view, sort, pos, limit, ignoreCase);
		assertEquals("[{f1:3}]", s.serialize(results));

		limit = 100;
		pos = 2;
		results = filter.filterCollection(query, view, sort, pos, limit, ignoreCase);
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