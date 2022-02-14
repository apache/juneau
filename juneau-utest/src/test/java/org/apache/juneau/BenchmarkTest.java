package org.apache.juneau;

import static org.apache.juneau.internal.CollectionUtils.*;

import java.util.*;

import org.junit.*;
import org.junit.rules.*;

import com.carrotsearch.junitbenchmarks.*;

//***************************************************************************************************************************
//* Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
//* distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
//* to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
//* with the License.  You may obtain a copy of the License at                                                              *
//*                                                                                                                         *
//*  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
//*                                                                                                                         *
//* Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
//* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
//* specific language governing permissions and limitations under the License.                                              *
//***************************************************************************************************************************
@BenchmarkOptions(benchmarkRounds = 50, warmupRounds = 20)
@Ignore
public class BenchmarkTest {
	@Rule
	public TestRule benchmarkRun = new BenchmarkRule();

	public static List<String> LIST = list();
	public static String[] ARRAY;

	public int x;

	/** Prepare random numbers for tests. */
	@BeforeClass
	public static void beforeClass() {
		for (int i = 0; i < 100000; i++)
			LIST.add(String.valueOf(i));
		ARRAY = LIST.toArray(new String[LIST.size()]);
	}

	@Test
	public void test0() throws Exception {
		for (int a = 0; a < 1000; a++) {
			for (Iterator<String> i = LIST.iterator(); i.hasNext();) {
				String s = i.next();
				x += s.length();
			}
		}
	}

	@Test
	public void testIterator() throws Exception {
		for (int a = 0; a < 1000; a++) {
			for (Iterator<String> i = LIST.iterator(); i.hasNext();) {
				String s = i.next();
				x += s.length();
			}
		}
	}

	@Test
	public void testLoop1() throws Exception {
		for (int a = 0; a < 1000; a++) {
			for (int i = 0; i < LIST.size(); i++) {
				String s = LIST.get(i);
				x += s.length();
			}
		}
	}

	@Test
	public void testLoop2() throws Exception {
		for (int a = 0; a < 1000; a++) {
			for (int i = 0, j = LIST.size(); i < j; i++) {
				String s = LIST.get(i);
				x += s.length();
			}
		}
	}

	@Test
	public void testForEach() throws Exception {
		for (int a = 0; a < 1000; a++) {
			for (String s : LIST) {
				x += s.length();
			}
		}
	}

	@Test
	public void testArray() throws Exception {
		for (int a = 0; a < 1000; a++) {
			for (String s : ARRAY) {
				x += s.length();
			}
		}
	}
}
