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
package org.apache.juneau.marshall.jena;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.bean.*;
import org.apache.juneau.marshall.parser.*;
import org.junit.jupiter.api.*;

/**
 * Tests {@code @BeanProp(required)} enforcement in the RDF parsers.
 */
@SuppressWarnings("unused") // Fixture fields are read reflectively.
class RdfRequired_Test extends TestBase {

	public static class A {
		@BeanProp(required=true) public String name;
		public int age;
	}

	public static class ALoose { public int age = 7; }

	public static class AFull { public String name = "n"; public int age = 7; }

	@Test void a01_rdfXml_missingRequired_fails() throws Exception {
		var in = RdfXmlSerializer.DEFAULT.write(new ALoose());
		var e = assertThrows(MissingRequiredPropertyException.class, () -> RdfXmlParser.DEFAULT.read(in, A.class));
		assertEquals(List.of("name"), e.getPropertyNames());
	}

	@Test void a02_rdfXml_positiveControl() throws Exception {
		var a = RdfXmlParser.DEFAULT.read(RdfXmlSerializer.DEFAULT.write(new AFull()), A.class);
		assertEquals("n", a.name);
	}

	@Test void b01_rdfStream_missingRequired_fails() throws Exception {
		var s = RdfStreamSerializer.create().language(Constants.LANG_RDFTHRIFT).build();
		var p = RdfStreamParser.create().language(Constants.LANG_RDFTHRIFT).build();
		var in = s.write(new ALoose());
		var e = assertThrows(MissingRequiredPropertyException.class, () -> p.read(in, A.class));
		assertEquals(List.of("name"), e.getPropertyNames());
	}

	@Test void b02_rdfStream_positiveControl() throws Exception {
		var s = RdfStreamSerializer.create().language(Constants.LANG_RDFTHRIFT).build();
		var p = RdfStreamParser.create().language(Constants.LANG_RDFTHRIFT).build();
		assertEquals("n", p.read(s.write(new AFull()), A.class).name);
	}
}
