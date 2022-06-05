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

import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class RdfXmlAbbrevTest {

	CharMarshall m = RdfXmlAbbrev.DEFAULT;

	String r = ""
		+ "<rdf:RDF"
		+ "    xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\""
		+ "    xmlns:j=\"http://www.apache.org/juneau/\""
		+ "    xmlns:jp=\"http://www.apache.org/juneaubp/\">"
		+ "<rdf:Description>"
		+ "<j:value>foo</j:value>"
		+ "</rdf:Description>"
		+ "</rdf:RDF>";

	@Test
	public void write1() throws Exception {
		assertString(m.write("foo")).isContains("<j:value>foo</j:value>");
	}

	@Test
	public void write2() throws Exception {
		StringWriter sw = new StringWriter();
		m.write("foo", sw);
		assertString(sw.toString()).isContains("<j:value>foo</j:value>");
	}

	@Test
	public void toString1() throws Exception {
		assertString(m.toString("foo")).isContains("<j:value>foo</j:value>");
	}

	@Test
	public void read1() throws Exception {
		String s = m.read(r, String.class);
		assertEquals("foo", s);
	}
}
