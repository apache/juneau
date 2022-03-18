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
package org.apache.juneau;

import static org.junit.runners.MethodSorters.*;

import java.util.*;
import java.util.Map.*;
import java.util.function.*;
import java.util.stream.*;

import org.apache.juneau.utils.*;
import org.junit.*;
import org.junit.rules.*;

import com.carrotsearch.junitbenchmarks.*;

@BenchmarkOptions(benchmarkRounds = 1000000, warmupRounds = 20)
@Ignore
@FixMethodOrder(NAME_ASCENDING)
public class BenchmarkTest {

	@Rule
	public TestRule benchmarkRun = new BenchmarkRule();

	public static final Random rand = new Random();
	public static List<Integer> LIST;
	public static Map<String,Integer> MAP;
	static {
		int cap = 10;
		LIST = new ArrayList<>(cap);
		MAP = new LinkedHashMap<>();

		for (int i = 0; i < cap; i++) {
			LIST.add(rand.nextInt(10));
			MAP.put(String.valueOf(i), rand.nextInt());
		}
		System.gc();
		System.err.println("Initialized");
	}

	public static int result;

	@BeforeClass
	public static void beforeClass() {
	}

	private static final Consumer<List<Integer>> list_iterator = x -> {for (Integer i : x) result += i;};
	private static final Consumer<List<Integer>> list_for = x -> {for (int i = 0; i < x.size(); i++) result += x.get(i);};
	private static final Consumer<List<Integer>> list_foreach = x -> x.forEach(y -> result += y);
	private static final Consumer<Map<String,Integer>> map_iterator1 = x -> {for (Integer i : x.values()) result += i;};
	private static final Consumer<Map<String,Integer>> map_iterator2 = x -> {for (Entry<String,Integer> i : x.entrySet()) result += i.getValue();};
	private static final Consumer<Map<String,Integer>> map_forEach1 = x -> x.values().forEach(y -> result += y);
	private static final Consumer<Map<String,Integer>> map_forEach2 = x -> x.forEach((k,v) -> result += v);
	private static final ThrowingConsumer<List<Integer>> slist_iterator = x -> {for (Integer i : x) result += i;};
	private static final ThrowingConsumer<List<Integer>> slist_for = x -> {for (int i = 0; i < x.size(); i++) result += x.get(i);};
	private static final ThrowingConsumer<List<Integer>> slist_foreach = x -> x.forEach(y -> result += y);
	private static final ThrowingConsumer<Map<String,Integer>> smap_iterator1 = x -> {for (Integer i : x.values()) result += i;};
	private static final ThrowingConsumer<Map<String,Integer>> smap_iterator2 = x -> {for (Entry<String,Integer> i : x.entrySet()) result += i.getValue();};
	private static final ThrowingConsumer<Map<String,Integer>> smap_forEach1 = x -> x.values().forEach(y -> result += y);
	private static final ThrowingConsumer<Map<String,Integer>> smap_forEach2 = x -> x.forEach((k,v) -> result += v);

	@Test public void a01a_list_iterator() { list_iterator.accept(LIST); }
	@Test public void a01b_list_for() { list_for.accept(LIST); }
	@Test public void a01c_list_foreach() { list_foreach.accept(LIST); }

	@Test public void a01a_map_iterator1_S() { map_iterator1.accept(MAP); }
	@Test public void a01b_map_iterator2_S() { map_iterator2.accept(MAP); }
	@Test public void a01c_map_forEach1_S() { map_forEach1.accept(MAP); }
	@Test public void a01d_map_forEach2_S() { map_forEach2.accept(MAP); }

	@Test public void b01a_list_iterator() { slist_iterator.accept(LIST); }
	@Test public void b01b_list_for() { slist_for.accept(LIST); }
	@Test public void b01c_list_foreach() { slist_foreach.accept(LIST); }

	@Test public void b01a_map_iterator1_S() { smap_iterator1.accept(MAP); }
	@Test public void b01b_map_iterator2_S() { smap_iterator2.accept(MAP); }
	@Test public void b01c_map_forEach1_S() { smap_forEach1.accept(MAP); }
	@Test public void b01d_map_forEach2_S() { smap_forEach2.accept(MAP); }

	public static void main(String[] args) {
		int cap = 100000;
		long startTime = 0;
		List<Integer> arrayList = new ArrayList<>();
		arrayList.forEach(x -> Objects.hash(x));
		IntStream.of(null).forEach(null);

		startTime = System.currentTimeMillis();
		for (int i = 0; i < cap; i++) list_iterator.accept(LIST);
		System.err.println("X1=" + (System.currentTimeMillis() - startTime));

		startTime = System.currentTimeMillis();
		for (int i = 0; i < cap; i++) list_for.accept(LIST);
		System.err.println("X2=" + (System.currentTimeMillis() - startTime));

		startTime = System.currentTimeMillis();
		for (int i = 0; i < cap; i++) list_foreach.accept(LIST);
		System.err.println("X3=" + (System.currentTimeMillis() - startTime));

//
//		startTime = System.currentTimeMillis();
//		for (int i = 0; i < cap; i++) map_forEach1.accept(MAP);
//		System.err.println("X1=" + (System.currentTimeMillis() - startTime));
//
//		startTime = System.currentTimeMillis();
//		for (int i = 0; i < cap; i++) map_forEach2.accept(MAP);
//		System.err.println("X2=" + (System.currentTimeMillis() - startTime));
//
//		startTime = System.currentTimeMillis();
//		for (int i = 0; i < cap; i++) map_iterator1.accept(MAP);
//		System.err.println("X3=" + (System.currentTimeMillis() - startTime));
//
//		startTime = System.currentTimeMillis();
//		for (int i = 0; i < cap; i++) map_iterator2.accept(MAP);
//		System.err.println("X4=" + (System.currentTimeMillis() - startTime));
//
//		startTime = System.currentTimeMillis();
//		for (int i = 0; i < cap; i++) map_forEach1.accept(MAP);
//		System.err.println("X1=" + (System.currentTimeMillis() - startTime));
//
//		startTime = System.currentTimeMillis();
//		for (int i = 0; i < cap; i++) map_forEach2.accept(MAP);
//		System.err.println("X2=" + (System.currentTimeMillis() - startTime));
//
//		startTime = System.currentTimeMillis();
//		for (int i = 0; i < cap; i++) map_iterator1.accept(MAP);
//		System.err.println("X3=" + (System.currentTimeMillis() - startTime));
//
//		startTime = System.currentTimeMillis();
//		for (int i = 0; i < cap; i++) map_iterator2.accept(MAP);
//		System.err.println("X4=" + (System.currentTimeMillis() - startTime));

		startTime = System.currentTimeMillis();

	}

}
