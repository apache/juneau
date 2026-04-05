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
 * WITHOUT WARRANTIES OR CONDITIONS FOR A PARTICULAR PURPOSE.  See the
 * License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.ini;

import static org.apache.juneau.junit.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Tests for {@link IniParser}.
 */
@SuppressWarnings("unchecked")
class IniParser_Test extends TestBase {

	//====================================================================================================
	// a - Simple flat parsing
	//====================================================================================================

	@Test
	void a01_simpleBean() throws Exception {
		var ini = "host = localhost\nport = 8080\ndebug = true";
		var m = (Map<String, Object>) IniParser.DEFAULT.parse(ini, Map.class, String.class, Object.class);
		assertBean(m, "host,port,debug", "localhost,8080,true");
	}

	@Test
	void a02_nestedBean() throws Exception {
		var ini = "name = myapp\n\n[database]\nhost = localhost\nport = 5432";
		var m = (Map<String, Object>) IniParser.DEFAULT.parse(ini, Map.class, String.class, Object.class);
		assertBean(m, "name,database{host,port}", "myapp,{localhost,5432}");
	}

	@Test
	void a03_nullValues() throws Exception {
		var ini = "name = Alice\nmiddle = null";
		var m = (Map<String, Object>) IniParser.DEFAULT.parse(ini, Map.class, String.class, Object.class);
		assertBean(m, "name,middle", "Alice,<null>");
	}

	@Test
	void a04_quotedStrings() throws Exception {
		var ini = "a = 'hello'\nb = 'world'";
		var m = (Map<String, Object>) IniParser.DEFAULT.parse(ini, Map.class, String.class, Object.class);
		assertBean(m, "a,b", "hello,world");
	}

	@Test
	void a05_numbersAndBooleans() throws Exception {
		var ini = "count = 42\nratio = 3.14\nflag = true";
		var m = (Map<String, Object>) IniParser.DEFAULT.parse(ini, Map.class, String.class, Object.class);
		assertBean(m, "count,ratio,flag", "42,3.14,true");
	}

	@Test
	void a06_listOfStrings() throws Exception {
		var ini = "tags = ['a','b','c']";
		var m = (Map<String, Object>) IniParser.DEFAULT.parse(ini, Map.class, String.class, Object.class);
		assertBean(m, "tags", "[a,b,c]");
	}

	@Test
	void a07_commentsIgnored() throws Exception {
		var ini = "# comment\nname = Alice\n# another";
		var m = (Map<String, Object>) IniParser.DEFAULT.parse(ini, Map.class, String.class, Object.class);
		assertBean(m, "name", "Alice");
	}

	@Test
	void a08_blankLinesIgnored() throws Exception {
		var ini = "a = 1\n\n\nb = 2";
		var m = (Map<String, Object>) IniParser.DEFAULT.parse(ini, Map.class, String.class, Object.class);
		assertBean(m, "a,b", "1,2");
	}

	@Test
	void a09_deeplyNestedBean() throws Exception {
		var ini = "name = John\n\n[employment]\ntitle = Engineer\n\n[employment/company]\nname = Acme\nticker = ACME";
		var m = (Map<String, Object>) IniParser.DEFAULT.parse(ini, Map.class, String.class, Object.class);
		assertBean(m, "name,employment{title,company{name,ticker}}", "John,{Engineer,{Acme,ACME}}");
	}

	@Test
	void a10_emptyInput() throws Exception {
		var m = (Map<String, Object>) IniParser.DEFAULT.parse("", Map.class, String.class, Object.class);
		assertNotNull(m);
		assertTrue(m.isEmpty());
	}

	@Test
	void a11_colonSeparator() throws Exception {
		var ini = "key: value";
		var m = (Map<String, Object>) IniParser.DEFAULT.parse(ini, Map.class, String.class, Object.class);
		assertBean(m, "key", "value");
	}

	@Test
	void a12_noSpaceSeparator() throws Exception {
		var ini = "key=value";
		var m = (Map<String, Object>) IniParser.DEFAULT.parse(ini, Map.class, String.class, Object.class);
		assertBean(m, "key", "value");
	}

	@Test
	void a13_topLevelCollectionThrows() {
		var ex = assertThrows(Exception.class, () ->
			IniParser.DEFAULT.parse("[]", List.class, String.class));
		assertTrue(ex.getMessage().contains("not supported") || ex.getMessage().contains("bean")
			|| ex.getClass().getSimpleName().contains("Parse"));
	}

}
