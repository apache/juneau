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
import static org.apache.juneau.assertions.Assertions.*;
import java.util.*;

import org.junit.*;

@SuppressWarnings("serial")
@FixMethodOrder(NAME_ASCENDING)
public class StackTraceDatabaseTest {

	@Test
	public void testBasic() {
		Throwable t1 = new Throwable();
		t1.fillInStackTrace();
		Throwable t2 = new Throwable();
		t2.fillInStackTrace();

		StackTraceDatabase db = new StackTraceDatabase();
		db.add(t1);
		StackTraceInfo t1a = db.getStackTraceInfo(t1);
		db.add(t1);
		StackTraceInfo t1b = db.getStackTraceInfo(t1);
		db.add(t2);
		StackTraceInfo t2a = db.getStackTraceInfo(t2);

		assertEquals(t1a.getHash(), t1b.getHash());
		assertNotEquals(t1a.getHash(), t2a.getHash());

		assertEquals(1, t1a.getCount());
		assertEquals(2, t1b.getCount());
		assertEquals(1, t2a.getCount());
	}

	@Test
	public void testGetClonedStackTraceInfos() {
		Throwable t1 = new Throwable();
		t1.fillInStackTrace();
		Throwable t2 = new Throwable();
		t2.fillInStackTrace();

		StackTraceDatabase db = new StackTraceDatabase();
		db.add(t1);
		db.add(t1);
		db.add(t2);

		List<StackTraceInfo> l = db.getClonedStackTraceInfos();
		db.add(t1);

		assertObject(l).json().matchesSimple("[{exception:'Throwable',hash:'*',count:2},{exception:'Throwable',hash:'*',count:1}]");
	}

	@Test
	public void testTimeout() {
		Throwable t1 = new Throwable();
		t1.fillInStackTrace();
		Throwable t2 = new Throwable();
		t2.fillInStackTrace();

		StackTraceDatabase db = new StackTraceDatabase(-2, null);
		db.add(t1);
		StackTraceInfo t1a = db.getStackTraceInfo(t1);
		db.add(t1);
		StackTraceInfo t1b = db.getStackTraceInfo(t1);
		db.add(t2);
		StackTraceInfo t2a = db.getStackTraceInfo(t2);

		assertEquals(t1a.getHash(), t1b.getHash());
		assertNotEquals(t1a.getHash(), t2a.getHash());

		assertEquals(0, t1a.getCount());
		assertEquals(0, t1b.getCount());
		assertEquals(0, t2a.getCount());
	}

	@Test
	public void testReset() {
		Throwable t1 = new Throwable();
		t1.fillInStackTrace();

		StackTraceDatabase db = new StackTraceDatabase();
		db.add(t1);
		StackTraceInfo t1a = db.getStackTraceInfo(t1);
		assertEquals(1, t1a.getCount());

		db.reset();
		t1a = db.getStackTraceInfo(t1);
		assertEquals(0, t1a.getCount());
	}

	@Test
	public void testNullException() {
		StackTraceDatabase db = new StackTraceDatabase();
		db.add(null).add(null);
		StackTraceInfo t1a = db.getStackTraceInfo(null);
		assertEquals(2, t1a.getCount());
	}

	@Test
	public void testSameStackTraces() {
		StackTraceDatabase db = new StackTraceDatabase();

		Throwable t1 = new Throwable() {
			@Override
			public StackTraceElement[] getStackTrace() {
				return new StackTraceElement[] {
					new StackTraceElement("Foo", "bar", "Foo.class", 1),
					new StackTraceElement("Foo", "baz", "Foo.class", 2),
					new StackTraceElement("Stop", "baz", "Stop.class", 3),
					new StackTraceElement("Object", "baz", "Object.class", 6)
				};
			}
		};
		Throwable t2 = new Throwable() {
			@Override
			public StackTraceElement[] getStackTrace() {
				return new StackTraceElement[] {
					new StackTraceElement("Foo", "bar", "Foo.class", 1),
					new StackTraceElement("Foo", "baz", "Foo.class", 2),
					new StackTraceElement("Stop", "baz", "Stop.class", 3),
					new StackTraceElement("Object", "baz", "Object.class", 6)
				};
			}
		};
		StackTraceInfo sti1 = db.getStackTraceInfo(t1);
		StackTraceInfo sti2 = db.getStackTraceInfo(t2);
		assertEquals(sti1.getHash(), sti2.getHash());
	}

	@Test
	public void testSlightlyDifferentStackTraces() {
		StackTraceDatabase db = new StackTraceDatabase();

		Throwable t1 = new Throwable() {
			@Override
			public StackTraceElement[] getStackTrace() {
				return new StackTraceElement[] {
					new StackTraceElement("Foo", "bar", "Foo.class", 1),
					new StackTraceElement("Foo", "baz", "Foo.class", 2),
					new StackTraceElement("Stop", "baz", "Stop.class", 3),
					new StackTraceElement("Object", "baz", "Object.class", 6)
				};
			}
		};
		Throwable t2 = new Throwable() {
			@Override
			public StackTraceElement[] getStackTrace() {
				return new StackTraceElement[] {
					new StackTraceElement("Foo", "bar", "Foo.class", 1),
					new StackTraceElement("Foo", "baz", "Foo.class", 2),
					new StackTraceElement("Stop", "baz", "Stop.class", 3),
					new StackTraceElement("Object", "baz", "Object.class", 7)
				};
			}
		};
		StackTraceInfo sti1 = db.getStackTraceInfo(t1);
		StackTraceInfo sti2 = db.getStackTraceInfo(t2);
		assertNotEquals(sti1.getHash(), sti2.getHash());
	}

	@Test
	public void testStopClass() {
		StackTraceDatabase db = new StackTraceDatabase(-1, StopClass.class);

		Throwable t1 = new Throwable() {
			@Override
			public StackTraceElement[] getStackTrace() {
				return new StackTraceElement[] {
					new StackTraceElement("Foo", "bar", "Foo.class", 1),
					new StackTraceElement("Foo", "baz", "Foo.class", 2),
					new StackTraceElement(StopClass.class.getName(), "baz", "Stop.class", 3),
					new StackTraceElement("Object", "baz", "Object.class", 6)
				};
			}
		};
		Throwable t2 = new Throwable() {
			@Override
			public StackTraceElement[] getStackTrace() {
				return new StackTraceElement[] {
					new StackTraceElement("Foo", "bar", "Foo.class", 1),
					new StackTraceElement("Foo", "baz", "Foo.class", 2),
					new StackTraceElement(StopClass.class.getName(), "baz", "Stop.class", 4),
					new StackTraceElement("Object", "baz", "Object.class", 7)
				};
			}
		};
		StackTraceInfo sti1 = db.getStackTraceInfo(t1);
		StackTraceInfo sti2 = db.getStackTraceInfo(t2);
		assertEquals(sti1.getHash(), sti2.getHash());
	}

	private static final class StopClass {}

	@Test
	public void testProxyElements() {
		StackTraceDatabase db = new StackTraceDatabase();

		Throwable t1 = new Throwable() {
			@Override
			public StackTraceElement[] getStackTrace() {
				return new StackTraceElement[] {
					new StackTraceElement("Foo", "bar", "Foo.class", 1),
					new StackTraceElement("Foo", "baz", "Foo.class", 2),
					new StackTraceElement("Stop$1", "baz", "Stop.class", 5),
					new StackTraceElement("Object", "baz", "Object.class", 6)
				};
			}
		};
		Throwable t2 = new Throwable() {
			@Override
			public StackTraceElement[] getStackTrace() {
				return new StackTraceElement[] {
					new StackTraceElement("Foo", "bar", "Foo.class", 1),
					new StackTraceElement("Foo", "baz", "Foo.class", 2),
					new StackTraceElement("Stop$2", "baz", "Stop.class", 6),
					new StackTraceElement("Object", "baz", "Object.class", 6)
				};
			}
		};
		StackTraceInfo sti1 = db.getStackTraceInfo(t1);
		StackTraceInfo sti2 = db.getStackTraceInfo(t2);
		assertEquals(sti1.getHash(), sti2.getHash());
	}
}
