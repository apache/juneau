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
package org.apache.juneau.a.rttests;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.csv.*;
import org.apache.juneau.html.*;
import org.apache.juneau.json.*;
import org.apache.juneau.msgpack.*;
import org.apache.juneau.uon.*;
import org.apache.juneau.urlencoding.*;
import org.apache.juneau.xml.*;
import org.apache.juneau.yaml.*;

/**
 * Tests designed to serialize and parse objects to make sure we end up
 * with the same objects for all serializers and parsers.
 */
public abstract class RoundTripTest_Base extends TestBase {

	private static final RoundTrip_Tester[] TESTERS = {
		tester(1, "Json - default")
			.serializer(JsonSerializer.create().keepNullProperties().addBeanTypes().addRootType())
			.parser(JsonParser.create())
			.build(),
		tester(2, "Json - lax")
			.serializer(JsonSerializer.create().json5().keepNullProperties().addBeanTypes().addRootType())
			.parser(JsonParser.create())
			.build(),
		tester(3, "Json - lax, readable")
			.serializer(JsonSerializer.create().json5().ws().keepNullProperties().addBeanTypes().addRootType())
			.parser(JsonParser.create())
			.build(),
		tester(4, "Xml - namespaces, validation, readable")
			.serializer(XmlSerializer.create().ns().sq().keepNullProperties().addNamespaceUrisToRoot().useWhitespace().addBeanTypes().addRootType())
			.parser(XmlParser.create())
			.validateXmlWhitespace()
			.validateXml()
			.build(),
		tester(5, "Xml - no namespaces, validation")
			.serializer(XmlSerializer.create().sq().keepNullProperties().addBeanTypes().addRootType())
			.parser(XmlParser.create())
			.validateXmlWhitespace()
			.build(),
		tester(6, "Html - default")
			.serializer(HtmlSerializer.create().keepNullProperties().addBeanTypes().addRootType())
			.parser(HtmlParser.create())
			.validateXmlWhitespace()
			.build(),
		tester(7, "Html - readable")
			.serializer(HtmlSerializer.create().sq().ws().keepNullProperties().addBeanTypes().addRootType())
			.parser(HtmlParser.create())
			.validateXmlWhitespace()
			.build(),
		tester(8, "Html - with key/value headers")
			.serializer(HtmlSerializer.create().addKeyValueTableHeaders().addBeanTypes().addRootType())
			.parser(HtmlParser.create())
			.validateXmlWhitespace()
			.build(),
		tester(9, "Uon - default")
			.serializer(UonSerializer.create().keepNullProperties().addBeanTypes().addRootType())
			.parser(UonParser.create())
			.build(),
		tester(10, "Uon - readable")
			.serializer(UonSerializer.create().ws().keepNullProperties().addBeanTypes().addRootType())
			.parser(UonParser.create())
			.build(),
		tester(11, "Uon - encoded")
			.serializer(UonSerializer.create().encoding().keepNullProperties().addBeanTypes().addRootType())
			.parser(UonParser.create().decoding())
			.build(),
		tester(12, "UrlEncoding - default")
			.serializer(UrlEncodingSerializer.create().keepNullProperties().addBeanTypes().addRootType())
			.parser(UrlEncodingParser.create())
			.build(),
		tester(13, "UrlEncoding - readable")
			.serializer(UrlEncodingSerializer.create().ws().keepNullProperties().addBeanTypes().addRootType())
			.parser(UrlEncodingParser.create())
			.build(),
		tester(14, "UrlEncoding - expanded params")
			.serializer(UrlEncodingSerializer.create().expandedParams().addBeanTypes().addRootType())
			.parser(UrlEncodingParser.create().expandedParams())
			.build(),
		tester(15, "MsgPack")
			.serializer(MsgPackSerializer.create().keepNullProperties().addBeanTypes().addRootType())
			.parser(MsgPackParser.create())
			.build(),
		tester(16, "Json schema")
			.serializer(JsonSchemaSerializer.create().keepNullProperties().addBeanTypes().addRootType())
			.returnOriginalObject()
			.build(),
		tester(17, "Yaml - default")
			.serializer(YamlSerializer.create().keepNullProperties().addBeanTypes().addRootType())
			.parser(YamlParser.create())
			.build(),
		tester(18, "Csv - default")
			.serializer(CsvSerializer.create().keepNullProperties())
			// CSV serialization is validated here without parsing (returnOriginalObject), analogous
			// to the JSON schema tester.  Full CSV round-trip tests are in CsvParser_Test.
			// Only test serialization of inputs that CSV can represent.
			.skipIf(o -> !isCsvSerializableInput(o))
			.returnOriginalObject()
			.build(),
	};

	static RoundTrip_Tester[]  testers() {
		return TESTERS;
	}

	protected static RoundTrip_Tester.Builder tester(int index, String label) {
		return RoundTrip_Tester.create(index, label);
	}

	/**
	 * Returns true if the object can be serialized to CSV without error.
	 *
	 * <p>
	 * CSV can serialize any non-null input, but restricts to non-null, non-array inputs
	 * to avoid serialization errors on raw byte arrays and similar types.
	 */
	protected static boolean isCsvSerializableInput(Object o) {
		if (o == null) return false;
		var cls = o.getClass();
		// Skip raw primitive arrays (byte[], char[], int[][], etc.) - they serialize as toString()
		if (cls.isArray() && cls.getComponentType().isPrimitive()) return false;
		return true;
	}

	/**
	 * Returns true if the object can be faithfully round-tripped through CSV.
	 *
	 * <p>
	 * CSV is tabular and only round-trips cleanly when:
	 * <ul>
	 *   <li>The object is a non-empty {@link Collection} of flat beans or Maps.
	 *   <li>Primitive arrays, 2D arrays, scalar lists, and enum arrays are excluded
	 *       because CSV cannot unambiguously represent them during parsing.
	 * </ul>
	 */
	protected static boolean isCsvRoundTripCompatible(Object o) {
		if (o == null)
			return false;
		// Only Collections are supported; reject raw arrays of any kind
		if (!(o instanceof Collection<?> col))
			return false;
		if (col.isEmpty())
			return false;
		var first = col.iterator().next();
		if (first == null)
			return false;
		return isCsvCompatibleElement(first);
	}

	private static boolean isCsvCompatibleElement(Object elem) {
		if (elem == null) return false;
		var cls = elem.getClass();
		// Reject scalars, arrays, enums, Optional, and any type that isn't a bean or Map
		if (cls.isPrimitive() || cls.isArray()) return false;
		if (elem instanceof Number || elem instanceof Boolean || elem instanceof Character) return false;
		if (elem instanceof CharSequence || cls.isEnum()) return false;
		if (elem instanceof java.util.Optional || elem instanceof Collection) return false;
		// Accept Maps only if all values are also simple types
		if (elem instanceof Map m) {
			return m.values().stream().allMatch(v -> v == null || isCsvSimpleType(v.getClass()));
		}
		// Reject JDK types that are not beans
		if (cls.getName().startsWith("java.") || cls.getName().startsWith("javax.")) return false;
		// For POJO beans: only accept if all public fields have simple (flat) types
		for (var field : cls.getFields()) {
			if (!isCsvSimpleType(field.getType())) return false;
		}
		return cls.getFields().length > 0 || cls.getMethods().length > 0;
	}

	private static boolean isCsvSimpleType(Class<?> t) {
		if (t == null) return true;
		return t.isPrimitive()
			|| t == String.class
			|| t == Boolean.class
			|| t == Character.class
			|| Number.class.isAssignableFrom(t)
			|| t.isEnum()
			|| java.time.temporal.Temporal.class.isAssignableFrom(t)
			|| t == java.util.Date.class
			|| t == java.util.Calendar.class;
	}
}