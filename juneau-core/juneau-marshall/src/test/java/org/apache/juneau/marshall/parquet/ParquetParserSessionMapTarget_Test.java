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
package org.apache.juneau.marshall.parquet;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Additional coverage for {@link ParquetParserSession#doRead}'s {@code type.isMap()} branch family: the
 * single-row {@code {value: Map}} unwrap distinct from the {@code {root: Map}} case, the non-string-key
 * key/value-pair-format detection when the actual data is NOT in that shape, and the {@code valueType==null}
 * fallback to {@code Object} when reading a non-string-keyed map without an explicit value type.
 */
@SuppressWarnings("unchecked")
class ParquetParserSessionMapTarget_Test extends TestBase {

	@Test void a01_singleKeyLiterallyValue_unwrapsInnerMap() throws Exception {
		// A root Map whose only entry's key is literally "value" (not "root") pointing to another Map.
		// Exercises the m.containsKey("value") && m.size()==1 branch (distinct from the {root:{...}} case).
		var inner = new LinkedHashMap<String,Object>();
		inner.put("a", 1);
		var in = new LinkedHashMap<String,Object>();
		in.put("value", inner);
		var bytes = ParquetSerializer.DEFAULT.write(in);
		var out = (Map<?,?>) ParquetParser.DEFAULT.read(bytes, Map.class);
		assertNotNull(out);
	}

	@Test void a02_singleKeyValue_pointingToNonMap_fallsThroughToNormalPath() throws Exception {
		// Row has containsKey("value") but the map has more than one key, so the single-key-unwrap
		// guard (m.size()==1) is false; falls through to the generic prepareMapForBean(m, type) path.
		var in = new LinkedHashMap<String,Object>();
		in.put("value", "x");
		in.put("other", "y");
		var bytes = ParquetSerializer.DEFAULT.write(in);
		var out = (Map<String,Object>) ParquetParser.DEFAULT.read(bytes, Map.class, String.class, Object.class);
		assertEquals("x", out.get("value"));
		assertEquals("y", out.get("other"));
	}

	@Test void b01_nonStringKeyMap_withoutExplicitValueType_defaultsToObject() throws Exception {
		// Map<Integer,?> read with only the key type specified (no value type argument): exercises the
		// valueType==null -> ctx.getMarshallingContext().getClassMeta(Object.class) fallback.
		var in = new LinkedHashMap<Integer,String>();
		in.put(1, "one");
		in.put(2, "two");
		var bytes = ParquetSerializer.DEFAULT.write(in);
		var out = (Map<Integer,Object>) ParquetParser.DEFAULT.read(bytes, Map.class, Integer.class);
		assertEquals("one", out.get(1));
		assertEquals("two", out.get(2));
	}

	@Test void c01_nonStringKeyType_dataNotInKeyValuePairFormat_skipsPairBranch() throws Exception {
		// The actual wire data is a plain String-keyed map (flat columns "1"/"2", numeric so the eventual
		// key conversion to Integer succeeds), but we deliberately request a non-String key type.
		// isKeyValuePairFormat(rows) inspects the *actual* row shape (first row lacks "key"/"value" keys
		// entirely) and returns false, so the isMap() block falls through to the plain single-row-map
		// unwrap instead of the key/value-pair reconstruction.
		var in = new LinkedHashMap<String,Object>();
		in.put("1", "one");
		in.put("2", "two");
		var bytes = ParquetSerializer.DEFAULT.write(in);
		var out = (Map<Integer,String>) ParquetParser.DEFAULT.read(bytes, Map.class, Integer.class, String.class);
		assertNotNull(out);
		assertEquals("one", out.get(1));
	}

	@Test void d01_intKeyMap_multipleRows_keyValuePairFormat_valueTypeExplicit() throws Exception {
		// Sanity check that the key/value-pair branch (valueType != null) still round-trips correctly
		// alongside the valueType==null variant above.
		var in = new LinkedHashMap<Integer,String>();
		in.put(5, "five");
		in.put(6, "six");
		in.put(7, "seven");
		var bytes = ParquetSerializer.DEFAULT.write(in);
		var out = (Map<Integer,String>) ParquetParser.DEFAULT.read(bytes, Map.class, Integer.class, String.class);
		assertEquals(3, out.size());
		assertEquals("seven", out.get(7));
	}
}
