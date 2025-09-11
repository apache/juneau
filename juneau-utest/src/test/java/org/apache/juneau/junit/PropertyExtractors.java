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
package org.apache.juneau.junit;

import static java.lang.Integer.*;
import static org.apache.juneau.junit.Utils.*;

import java.lang.reflect.*;
import java.util.*;

/**
 * Collection of standard property extractor implementations for the Bean-Centric Testing framework.
 *
 * <p>This class provides the built-in property extraction strategies that handle the most common
 * object types and property access patterns. These extractors are automatically registered when
 * using {@link BasicBeanConverter.Builder#defaultSettings()}.</p>
 *
 * <h5 class='section'>Extractor Hierarchy:</h5>
 * <p>The extractors form an inheritance hierarchy for code reuse:</p>
 * <ul>
 * 	<li><b>{@link ObjectPropertyExtractor}</b> - Base class with JavaBean property access</li>
 * 	<li><b>{@link ListPropertyExtractor}</b> - Extends ObjectPropertyExtractor with array/collection support</li>
 * 	<li><b>{@link MapPropertyExtractor}</b> - Extends ObjectPropertyExtractor with Map key access</li>
 * </ul>
 *
 * <h5 class='section'>Execution Order:</h5>
 * <p>In {@link BasicBeanConverter}, the extractors are tried in this order:</p>
 * <ol>
 * 	<li><b>Custom extractors</b> - User-registered extractors via {@link BasicBeanConverter.Builder#addPropertyExtractor(PropertyExtractor)}</li>
 * 	<li><b>{@link ObjectPropertyExtractor}</b> - JavaBean properties, fields, and methods</li>
 * 	<li><b>{@link ListPropertyExtractor}</b> - Array/collection indices and size properties</li>
 * 	<li><b>{@link MapPropertyExtractor}</b> - Map key access and size property</li>
 * </ol>
 *
 * <h5 class='section'>Property Access Strategy:</h5>
 * <p>Each extractor implements a comprehensive fallback strategy for maximum compatibility:</p>
 *
 * @see PropertyExtractor
 * @see BasicBeanConverter.Builder#defaultSettings()
 * @see BasicBeanConverter.Builder#addPropertyExtractor(PropertyExtractor)
 */
public class PropertyExtractors {

	/**
	 * Standard JavaBean property extractor using reflection.
	 *
	 * <p>This extractor serves as the universal fallback for property access, implementing
	 * comprehensive JavaBean property access patterns. It tries multiple approaches to
	 * access object properties, providing maximum compatibility with different coding styles.</p>
	 *
	 * <h5 class='section'>Property Access Order:</h5>
	 * <ol>
	 * 	<li><b>{@code is{Property}()}</b> - Boolean property getters (e.g., {@code isActive()})</li>
	 * 	<li><b>{@code get{Property}()}</b> - Standard getter methods (e.g., {@code getName()})</li>
	 * 	<li><b>{@code get(String)}</b> - Map-style property access with property name as parameter</li>
	 * 	<li><b>Fields</b> - Public fields with matching names (searches inheritance hierarchy)</li>
	 * 	<li><b>{@code {property}()}</b> - No-argument methods with exact property name</li>
	 * </ol>
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Property "name" can be accessed via:</jc>
	 * 	<jv>obj</jv>.getName()        <jc>// Standard getter</jc>
	 * 	<jv>obj</jv>.name             <jc>// Public field</jc>
	 * 	<jv>obj</jv>.name()           <jc>// Method with property name</jc>
	 * 	<jv>obj</jv>.get(<js>"name"</js>)       <jc>// Map-style getter</jc>
	 *
	 * 	<jc>// Property "active" (boolean) can be accessed via:</jc>
	 * 	<jv>obj</jv>.isActive()       <jc>// Boolean getter</jc>
	 * 	<jv>obj</jv>.getActive()      <jc>// Standard getter alternative</jc>
	 * 	<jv>obj</jv>.active           <jc>// Public field</jc>
	 * </p>
	 *
	 * <p><b>Compatibility:</b> This extractor can handle any object type, making it the
	 * universal fallback. It always returns {@code true} from {@link #canExtract(BeanConverter, Object, String)}.</p>
	 */
	public static class ObjectPropertyExtractor implements PropertyExtractor {

		@Override
		public boolean canExtract(BeanConverter converter, Object o, String name) {
			return true;
		}

		@Override
		public Object extract(BeanConverter converter, Object o, String name) {
			return
				safe(() -> {
					if (o == null)
						return null;
					var f = (Field)null;
					var c = o.getClass();
					var n = Character.toUpperCase(name.charAt(0)) + name.substring(1);
					var m = Arrays.stream(c.getMethods()).filter(x -> x.getName().equals("is"+n) && x.getParameterCount() == 0).findFirst().orElse(null);
					if (m != null) {
						m.setAccessible(true);
						return m.invoke(o);
					}
					m = Arrays.stream(c.getMethods()).filter(x -> x.getName().equals("get"+n) && x.getParameterCount() == 0).findFirst().orElse(null);
					if (m != null) {
						m.setAccessible(true);
						return m.invoke(o);
					}
					m = Arrays.stream(c.getMethods()).filter(x -> x.getName().equals("get") && x.getParameterCount() == 1 && x.getParameterTypes()[0] == String.class).findFirst().orElse(null);
					if (m != null) {
						m.setAccessible(true);
						return m.invoke(o, name);
					}
					var c2 = c;
					while (f == null && c2 != null) {
						f = Arrays.stream(c2.getDeclaredFields()).filter(x -> x.getName().equals(name)).findFirst().orElse(null);
						c2 = c2.getSuperclass();
					}
					if (f != null) {
						f.setAccessible(true);
						return f.get(o);
					}
					m = Arrays.stream(c.getMethods()).filter(x -> x.getName().equals(name) && x.getParameterCount() == 0).findFirst().orElse(null);
					if (m != null) {
						m.setAccessible(true);
						return m.invoke(o);
					}
					throw new RuntimeException(f("Property {0} not found on object of type {1}", name, o.getClass().getSimpleName()));
				});
		}
	}

	/**
	 * Property extractor for array and collection objects with numeric indexing and size access.
	 *
	 * <p>This extractor extends {@link ObjectPropertyExtractor} to add special handling for
	 * collection-like objects. It provides array-style access using numeric indices and
	 * universal size/length properties for any listifiable object.</p>
	 *
	 * <h5 class='section'>Additional Properties:</h5>
	 * <ul>
	 * 	<li><b>Numeric indices:</b> {@code "0"}, {@code "1"}, {@code "2"}, etc. for element access</li>
	 * 	<li><b>Negative indices:</b> {@code "-1"}, {@code "-2"} for reverse indexing (from end)</li>
	 * 	<li><b>Size properties:</b> {@code "length"} and {@code "size"} return collection size</li>
	 * </ul>
	 *
	 * <h5 class='section'>Supported Types:</h5>
	 * <p>Works with any object that can be listified by the converter:</p>
	 * <ul>
	 * 	<li><b>Arrays:</b> All array types (primitive and object)</li>
	 * 	<li><b>Collections:</b> List, Set, Queue, and all Collection subtypes</li>
	 * 	<li><b>Iterables:</b> Any object implementing Iterable</li>
	 * 	<li><b>Streams:</b> Stream objects and other lazy sequences</li>
	 * 	<li><b>Maps:</b> Converted to list of entries for iteration</li>
	 * </ul>
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Array/List access</jc>
	 * 	<jv>list</jv>.get(0)          <jc>// "0" property</jc>
	 * 	<jv>array</jv>[2]             <jc>// "2" property</jc>
	 * 	<jv>list</jv>.get(-1)         <jc>// "-1" property (last element)</jc>
	 *
	 * 	<jc>// Size access</jc>
	 * 	<jv>array</jv>.length         <jc>// "length" property</jc>
	 * 	<jv>collection</jv>.size()    <jc>// "size" property</jc>
	 * 	<jv>stream</jv>.count()       <jc>// "length" or "size" property</jc>
	 * </p>
	 *
	 * <p><b>Fallback:</b> If the property is not a numeric index or size property,
	 * delegates to {@link ObjectPropertyExtractor} for standard property access.</p>
	 */
	public static class ListPropertyExtractor extends ObjectPropertyExtractor {

		@Override
		public boolean canExtract(BeanConverter converter, Object o, String name) {
			return converter.canListify(o);
		}

		@Override
		public Object extract(BeanConverter converter, Object o, String name) {
			var l = converter.listify(o);
			if (name.matches("-?\\d+"))
				return l.get(parseInt(name));
			if ("length".equals(name)) return l.size();
			if ("size".equals(name)) return l.size();
			return super.extract(converter, o, name);
		}
	}

	/**
	 * Property extractor for Map objects with direct key access and size property.
	 *
	 * <p>This extractor extends {@link ObjectPropertyExtractor} to add special handling for
	 * Map objects. It provides direct key-based property access and a universal size
	 * property for Map objects.</p>
	 *
	 * <h5 class='section'>Map-Specific Properties:</h5>
	 * <ul>
	 * 	<li><b>Direct key access:</b> Any property name that exists as a Map key</li>
	 * 	<li><b>Size property:</b> {@code "size"} returns {@code map.size()}</li>
	 * </ul>
	 *
	 * <h5 class='section'>Supported Types:</h5>
	 * <p>Works with any object implementing the {@code Map} interface:</p>
	 * <ul>
	 * 	<li><b>HashMap, LinkedHashMap:</b> Standard Map implementations</li>
	 * 	<li><b>TreeMap, ConcurrentHashMap:</b> Specialized Map implementations</li>
	 * 	<li><b>Properties:</b> Java Properties objects</li>
	 * 	<li><b>Custom Maps:</b> Any Map implementation</li>
	 * </ul>
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Direct key access</jc>
	 * 	<jv>map</jv>.get(<js>"name"</js>)       <jc>// "name" property</jc>
	 * 	<jv>map</jv>.get(<js>"timeout"</js>)    <jc>// "timeout" property</jc>
	 * 	<jv>props</jv>.getProperty(<js>"key"</js>) <jc>// "key" property</jc>
	 *
	 * 	<jc>// Size access</jc>
	 * 	<jv>map</jv>.size()           <jc>// "size" property</jc>
	 * </p>
	 *
	 * <h5 class='section'>Key Priority:</h5>
	 * <p>Map key access takes priority over JavaBean properties. If a Map contains
	 * a key with the same name as a property/method, the Map value is returned first.</p>
	 *
	 * <p><b>Fallback:</b> If the property is not found as a Map key and is not "size",
	 * delegates to {@link ObjectPropertyExtractor} for standard property access.</p>
	 */
	public static class MapPropertyExtractor extends ObjectPropertyExtractor {

		@Override
		public boolean canExtract(BeanConverter converter, Object o, String name) {
			return o instanceof Map;
		}

		@Override
		public Object extract(BeanConverter converter, Object o, String name) {
			var m = (Map<?,?>)o;
			if (m.containsKey(name)) return m.get(name);
			if ("size".equals(name)) return m.size();
			return super.extract(converter, o, name);
		}
	}
}