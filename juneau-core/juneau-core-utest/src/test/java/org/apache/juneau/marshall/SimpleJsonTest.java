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
package org.apache.juneau.marshall;

import static org.apache.juneau.assertions.Assertions.*;
import static org.junit.Assert.assertEquals;
import static org.junit.runners.MethodSorters.*;

import java.io.*;
import java.util.*;

import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class SimpleJsonTest {

	CharMarshall m = SimpleJson.DEFAULT;

	@Test
	public void write1() throws Exception {
		assertEquals("'foo'", m.write("foo"));
	}

	@Test
	public void write2() throws Exception {
		StringWriter sw = new StringWriter();
		m.write("foo", sw);
		assertEquals("'foo'", sw.toString());
	}

	@Test
	public void toString1() throws Exception {
		assertEquals("'foo'", m.toString("foo"));
	}

	@Test
	public void read1() throws Exception {
		String s = m.read("'foo'", String.class);
		assertEquals("foo", s);
	}

	@Test
	public void read2() throws Exception {
		Map<?,?> o = m.read("{foo:'bar'}", Map.class, String.class, String.class);
		assertObject(o).asJson().is("{foo:'bar'}");
	}
}
