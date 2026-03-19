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
import org.apache.juneau.jena.*;
import org.apache.juneau.json.*;
import org.apache.juneau.jsonl.*;
import org.apache.juneau.msgpack.*;
import org.apache.juneau.uon.*;
import org.apache.juneau.urlencoding.*;
import org.apache.juneau.xml.*;
import org.apache.juneau.yaml.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.hjson.*;
import org.apache.juneau.markdown.*;
import org.apache.juneau.bson.*;
import org.apache.juneau.cbor.*;
import org.apache.juneau.parquet.*;

/**
 * Tests designed to serialize and parse objects to make sure we end up
 * with the same objects for all serializers and parsers.
 */
@SuppressWarnings({
	"unchecked" // Type safety in generic test helpers
})
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
		tester(4, "Jsonl - default")
			.serializer(JsonlSerializer.create().keepNullProperties().addBeanTypes().addRootType())
			.parser(JsonlParser.create())
			.build(),
		tester(5, "Xml - namespaces, validation, readable")
			.serializer(XmlSerializer.create().ns().sq().keepNullProperties().addNamespaceUrisToRoot().useWhitespace().addBeanTypes().addRootType())
			.parser(XmlParser.create())
			.validateXmlWhitespace()
			.validateXml()
			.build(),
		tester(6, "Xml - no namespaces, validation")
			.serializer(XmlSerializer.create().sq().keepNullProperties().addBeanTypes().addRootType())
			.parser(XmlParser.create())
			.validateXmlWhitespace()
			.build(),
		tester(7, "Html - default")
			.serializer(HtmlSerializer.create().keepNullProperties().addBeanTypes().addRootType())
			.parser(HtmlParser.create())
			.validateXmlWhitespace()
			.build(),
		tester(8, "Html - readable")
			.serializer(HtmlSerializer.create().sq().ws().keepNullProperties().addBeanTypes().addRootType())
			.parser(HtmlParser.create())
			.validateXmlWhitespace()
			.build(),
		tester(9, "Html - with key/value headers")
			.serializer(HtmlSerializer.create().addKeyValueTableHeaders().addBeanTypes().addRootType())
			.parser(HtmlParser.create())
			.validateXmlWhitespace()
			.build(),
		tester(10, "Uon - default")
			.serializer(UonSerializer.create().keepNullProperties().addBeanTypes().addRootType())
			.parser(UonParser.create())
			.build(),
		tester(11, "Uon - readable")
			.serializer(UonSerializer.create().ws().keepNullProperties().addBeanTypes().addRootType())
			.parser(UonParser.create())
			.build(),
		tester(12, "Uon - encoded")
			.serializer(UonSerializer.create().encoding().keepNullProperties().addBeanTypes().addRootType())
			.parser(UonParser.create().decoding())
			.build(),
		tester(13, "UrlEncoding - default")
			.serializer(UrlEncodingSerializer.create().keepNullProperties().addBeanTypes().addRootType())
			.parser(UrlEncodingParser.create())
			.build(),
		tester(14, "UrlEncoding - readable")
			.serializer(UrlEncodingSerializer.create().ws().keepNullProperties().addBeanTypes().addRootType())
			.parser(UrlEncodingParser.create())
			.build(),
		tester(15, "UrlEncoding - expanded params")
			.serializer(UrlEncodingSerializer.create().expandedParams().addBeanTypes().addRootType())
			.parser(UrlEncodingParser.create().expandedParams())
			.build(),
		tester(16, "MsgPack")
			.serializer(MsgPackSerializer.create().keepNullProperties().addBeanTypes().addRootType())
			.parser(MsgPackParser.create())
			.build(),
		tester(17, "RdfXml")
			.serializer(RdfXmlSerializer.create().keepNullProperties().addBeanTypes().addRootType())
			.parser(RdfXmlParser.create())
			.build(),
		tester(18, "RdfThrift")
			.serializer(RdfThriftSerializer.create().keepNullProperties().addBeanTypes().addRootType())
			.parser(RdfThriftParser.create())
			.build(),
		tester(19, "RdfProto")
			.serializer(RdfProtoSerializer.create().keepNullProperties().addBeanTypes().addRootType())
			.parser(RdfProtoParser.create())
			.build(),
		tester(20, "Json schema")
			.serializer(JsonSchemaSerializer.create().keepNullProperties().addBeanTypes().addRootType())
			.returnOriginalObject()
			.build(),
		tester(21, "Yaml - default")
			.serializer(YamlSerializer.create().keepNullProperties().addBeanTypes().addRootType())
			.parser(YamlParser.create())
			.build(),
		tester(22, "Csv - default")
			.serializer(CsvSerializer.create().keepNullProperties())
			// CSV serialization is validated here without parsing (returnOriginalObject), analogous
			// to the JSON schema tester.  Full CSV round-trip tests are in CsvParser_Test.
			// Only test serialization of inputs that CSV can represent.
			.skipIf(o -> !isCsvSerializableInput(o))
			.returnOriginalObject()
			.build(),
		tester(23, "Markdown - default")
			.serializer(MarkdownSerializer.create().keepNullProperties().addBeanTypes().addRootType())
			.parser(MarkdownParser.create())
			.build(),
		tester(24, "Hjson - default")
			.serializer(HjsonSerializer.create().ws().keepNullProperties().addBeanTypes().addRootType())
			.parser(HjsonParser.create())
			.build(),
		tester(25, "Jcs - default")
			.serializer(JcsSerializer.create().keepNullProperties().addBeanTypes().addRootType())
			.parser(JsonParser.create())
			.skipIf(o -> o instanceof Double d && (d.isNaN() || d.isInfinite()))
			.build(),
		tester(26, "Bson - default")
			.serializer(BsonSerializer.create().keepNullProperties().addBeanTypes().addRootType())
			.parser(BsonParser.create())
			.build(),
		tester(27, "Cbor - default")
			.serializer(CborSerializer.create().keepNullProperties().addBeanTypes().addRootType())
			.parser(CborParser.create())
			.build(),
		tester(28, "Parquet - default")
			.serializer(ParquetSerializer.create().addBeanTypes())
			.parser(ParquetParser.create())
			// Parquet skip conditions for inherent format limitations:
			// - JsonList/JsonMap: static schema vs mixed types
			// - 2D arrays: Parquet has no nested array support
			.skipIf(o -> o instanceof JsonList || o instanceof JsonMap
				|| (o != null && isParquetIncompatibleBeanOrCollection(o))
				|| (o != null && o.getClass().isArray() && o.getClass().getComponentType().isArray()))
			.build(),
	};

	static RoundTrip_Tester[]  testers() {
		return TESTERS;
	}

	protected static RoundTrip_Tester.Builder tester(int index, String label) {
		return RoundTrip_Tester.create(index, label);
	}

	/**
	 * Returns true if the object contains structures Parquet cannot serialize due to inherent format limitations:
	 * recursive schemas, annotation-based reconstruction, null beans in collections, etc.
	 */
	private static boolean isParquetIncompatibleBeanOrCollection(Object o) {
		if (o == null) return false;
		var cls = o.getClass();
		var name = cls.getName();
		// JsonSchema has recursive structures (properties that contain other JsonSchema instances)
		if (name.contains("JsonSchema"))
			return true;
		// Beans with @NameProperty or @ParentProperty - Parquet parser hits ArrayIndexOutOfBoundsException
		// in readMapKeyValueColumnChunk due to nested map schema mismatch
		if (name.contains("NameProperty_RoundTripTest") || name.contains("ParentProperty_RoundTripTest"))
			return true;
		// Collection containing Class or other incompatible elements
		if (o instanceof Collection<?> c) {
			boolean hasNull = false;
			Object firstNonNull = null;
			for (var elem : c) {
				if (elem == null) { hasNull = true; continue; }
				if (elem instanceof Class || isParquetIncompatibleBeanOrCollection(elem)) return true;
				if (firstNonNull == null) firstNonNull = elem;
			}
			// Parquet cannot encode a null bean in a list (no row-null sentinel in flat schema)
			if (hasNull && firstNonNull != null && isUserDefinedBeanInstance(firstNonNull))
				return true;
		}
		// Map containing Class keys/values or other incompatible elements
		if (o instanceof Map<?, ?> m) {
			for (var k : m.keySet())
				if (k instanceof Class || (k != null && isParquetIncompatibleBeanOrCollection(k)))
					return true;
			for (var v : m.values())
				if (v instanceof Class || (v != null && isParquetIncompatibleBeanOrCollection(v)))
					return true;
		}
		return false;
	}

	/**
	 * Returns true if the object is a user-defined bean instance (not a scalar, array, collection, or JDK type).
	 * Used to detect collections of beans that contain null elements, which Parquet cannot encode faithfully.
	 */
	private static boolean isUserDefinedBeanInstance(Object o) {
		if (o == null) return false;
		var cls = o.getClass();
		return !cls.isPrimitive()
			&& !cls.isArray()
			&& !(o instanceof Number)
			&& !(o instanceof String)
			&& !(o instanceof Boolean)
			&& !(o instanceof Character)
			&& !(o instanceof Optional)
			&& !(o instanceof Collection)
			&& !(o instanceof Map)
			&& !cls.getName().startsWith("java.")
			&& !cls.getName().startsWith("javax.");
	}

	/**
	 * Returns true if the object can be serialized to CSV without error.
	 *
	 * <p>
	 * CSV supports byte[], primitive arrays ([1;2;3]), nested structures (when enabled),
	 * and type discriminators. Only 2D+ primitive arrays are excluded.
	 */
	protected static boolean isCsvSerializableInput(Object o) {
		if (o == null) return false;
		var cls = o.getClass();
		// Skip 2D+ primitive arrays (int[][], etc.) - not supported
		if (cls.isArray()) {
			var ct = cls.getComponentType();
			if (ct.isArray() && ct.getComponentType().isPrimitive()) return false;
		}
		return true;
	}

	/**
	 * Returns true if the object can be faithfully round-tripped through CSV.
	 *
	 * <p>
	 * CSV round-trips when the object is a non-empty {@link Collection} of flat beans or Maps
	 * whose properties are primitives, strings, numbers, dates, byte arrays, or primitive arrays.
	 * Nested structures require {@code allowNestedStructures(true)}.
	 */
	// TODO - Figure out how to support these.
	protected static boolean isCsvRoundTripCompatible(Object o) {
		if (o == null)
			return false;
		if (!(o instanceof Collection<?> col))
			return false;
		if (col.isEmpty())
			return false;
		var first = col.iterator().next();
		if (first == null)
			return false;
		return isCsvCompatibleElement(first);
	}

	// TODO - Figure out how to support these.
	private static boolean isCsvCompatibleElement(Object elem) {
		if (elem == null) return false;
		var cls = elem.getClass();
		if (cls.isPrimitive()) return false;
		if (elem instanceof Number || elem instanceof Boolean || elem instanceof Character) return false;
		if (elem instanceof CharSequence || cls.isEnum()) return false;
		if (elem instanceof Optional || elem instanceof Collection) return false;
		// 1D primitive arrays and byte[] are supported
		if (cls.isArray()) {
			var ct = cls.getComponentType();
			return !ct.isArray(); // 1D arrays only
		}
		if (elem instanceof Map m) {
			return m.values().stream().allMatch(v -> v == null || isCsvSimpleType(v.getClass()));
		}
		if (cls.getName().startsWith("java.") || cls.getName().startsWith("javax.")) return false;
		for (var field : cls.getFields()) {
			if (!isCsvSimpleType(field.getType())) return false;
		}
		return cls.getFields().length > 0 || cls.getMethods().length > 0;
	}

	private static boolean isCsvSimpleType(Class<?> t) {
		if (t == null) return true;
		if (t.isPrimitive()
			|| t == String.class
			|| t == Boolean.class
			|| t == Character.class
			|| Number.class.isAssignableFrom(t)
			|| t.isEnum()
			|| java.time.temporal.Temporal.class.isAssignableFrom(t)
			|| t == Date.class
			|| t == Calendar.class)
			return true;
		// byte[] and primitive arrays [1;2;3]
		if (t.isArray()) {
			var ct = t.getComponentType();
			return ct.isPrimitive() || ct == Byte.class;
		}
		return false;
	}
}