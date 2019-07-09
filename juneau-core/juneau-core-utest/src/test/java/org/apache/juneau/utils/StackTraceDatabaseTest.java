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

import org.junit.*;

public class StackTraceDatabaseTest {

	@Test
	public void testBasic() {
		Throwable t1 = new Throwable();
		t1.fillInStackTrace();
		Throwable t2 = new Throwable();
		t2.fillInStackTrace();

		StackTraceDatabase db = new StackTraceDatabase();
		StackTraceInfo t1a = db.getStackTraceInfo(t1, Integer.MAX_VALUE);
		StackTraceInfo t1b = db.getStackTraceInfo(t1, Integer.MAX_VALUE);
		StackTraceInfo t2a = db.getStackTraceInfo(t2, Integer.MAX_VALUE);
		assertEquals(t1a.getHash(), t1b.getHash());
		assertNotEquals(t1a.getHash(), t2a.getHash());
		assertEquals(1, t1a.getCount());
		assertEquals(2, t1b.getCount());
		assertEquals(1, t2a.getCount());
	}

	@Test
	public void testTimeout() {
		Throwable t1 = new Throwable();
		t1.fillInStackTrace();
		Throwable t2 = new Throwable();
		t2.fillInStackTrace();

		StackTraceDatabase db = new StackTraceDatabase();
		StackTraceInfo t1a = db.getStackTraceInfo(t1, -1);
		StackTraceInfo t1b = db.getStackTraceInfo(t1, -1);
		StackTraceInfo t2a = db.getStackTraceInfo(t2, -1);
		assertEquals(t1a.getHash(), t1b.getHash());
		assertNotEquals(t1a.getHash(), t2a.getHash());
		assertEquals(1, t1a.getCount());
		assertEquals(1, t1b.getCount());
		assertEquals(1, t2a.getCount());
	}
}
