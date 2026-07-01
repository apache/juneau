/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.marshall.protobuf;

import static org.apache.juneau.test.bct.BctAssertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Lossless round-trip tests for the protobuf binary codec.
 */
class ProtobufRoundTrip_Test extends TestBase {

	private static <T> T roundTrip(T o, Class<T> c) throws Exception {
		return ProtobufParser.DEFAULT.parse(ProtobufSerializer.DEFAULT.serialize(o), c);
	}

	public static class AllScalars {
		public boolean bool;
		public byte by;
		public char ch;
		public double d;
		public float f;
		public int i;
		public long l;
		public short sh;
		public String s;
		public java.math.BigInteger bi;
		public java.math.BigDecimal bd;
		public AllScalars() { /* Public no-arg constructor required for Juneau bean recognition. */ }
	}

	@Test
	void a01_allScalars() throws Exception {
		var a = new AllScalars();
		a.bool = true; a.by = 7; a.ch = 'q'; a.d = 3.5; a.f = 2.5f; a.i = 1234;
		a.l = 9_000_000_000L; a.sh = 99; a.s = "hello";
		a.bi = new java.math.BigInteger("123456789012345678901234567890");
		a.bd = new java.math.BigDecimal("3.14159265358979");
		var b = roundTrip(a, AllScalars.class);
		assertBean(b, "bool,by,ch,d,f,i,l,sh,s,bi,bd",
			"true,7,q,3.5,2.5,1234,9000000000,99,hello,123456789012345678901234567890,3.14159265358979");
	}

	public static class Recursive {
		public String name;
		public Recursive child;
		public Recursive() {}
		public Recursive(String name) { this.name = name; }
	}

	@Test
	void a02_nestedGraph() throws Exception {
		var a = new Recursive("a");
		a.child = new Recursive("b");
		a.child.child = new Recursive("c");
		var b = roundTrip(a, Recursive.class);
		assertBean(b, "name,child{name,child{name}}", "a,{b,{c}}");
	}

	@Test
	void a03_emptyBean() throws Exception {
		var b = roundTrip(new AllScalars(), AllScalars.class);
		assertBean(b, "i,s", "0,<null>");
	}
}
