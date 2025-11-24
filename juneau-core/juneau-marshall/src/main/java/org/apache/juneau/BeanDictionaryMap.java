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
package org.apache.juneau;

import static org.apache.juneau.common.utils.CollectionUtils.*;
import static org.apache.juneau.common.utils.ThrowableUtils.*;
import static org.apache.juneau.common.utils.Utils.*;

import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.annotation.*;

/**
 * Represents a map of dictionary type names to bean classes that make up a bean dictionary.
 *
 * <p>
 * In general, this approach for defining dictionary names for classes is used when it's not possible to use the
 * {@link Bean#typeName() @Bean(typeName)} annotation.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// A bean dictionary map consisting of classes without @Bean(typeName) annotations</jc>
 * 	<jc>// that require type names to be explicitly specified.</jc>
 * 	<jk>public class</jk> MyBeanDictionaryMap <jk>extends</jk> BeanDictionaryMap {
 *
 * 		<jc>// Must provide a no-arg constructor!</jc>
 * 		<jk>public</jk> MyBeanDictionaryMap() {
 * 			append(<js>"MyBean"</js>, MyBean.<jk>class</jk>);
 * 			append(<js>"MyBeanArray"</js>, MyBean[].<jk>class</jk>);
 * 			append(<js>"StringArray"</js>, String[].<jk>class</jk>);
 * 			append(<js>"String2dArray"</js>, String[][].<jk>class</jk>);
 * 			append(<js>"IntArray"</js>, <jk>int</jk>[].<jk>class</jk>);
 * 			append(<js>"Int2dArray"</js>, <jk>int</jk>[][].<jk>class</jk>);
 * 			append(<js>"LinkedList"</js>, LinkedList.<jk>class</jk>);
 * 			append(<js>"TreeMap"</js>, TreeMap.<jk>class</jk>);
 * 			append(<js>"LinkedListOfInts"</js>, LinkedList.<jk>class</jk>, Integer.<jk>class</jk>);
 * 			append(<js>"LinkedListOfR1"</js>, LinkedList.<jk>class</jk>, R1.<jk>class</jk>);
 * 			append(<js>"LinkedListOfCalendar"</js>, LinkedList.<jk>class</jk>, Calendar.<jk>class</jk>);
 * 		}
 * 	}
 *
 * 	<jc>// Use it in a parser.</jc>
 * 	ReaderParser <jv>parser</jv> = JsonParser
 * 		.<jsm>create</jsm>()
 * 		.dictionary(MyBeanDictionaryMap.<jk>class</jk>)
 * 		.build();
 * </p>
 *
 * <p>
 * Subclasses must implement a public no-arg constructor so that it can be instantiated by the bean context code.
 *
 *
 * @serial exclude
 */
@SuppressWarnings("rawtypes")
public class BeanDictionaryMap extends LinkedHashMap<String,Object> {
	private static final long serialVersionUID = 1L;

	/**
	 * Constructor.
	 */
	protected BeanDictionaryMap() {}

	private void assertValidParameter(Object o) {
		if (nn(o)) {
			if (o instanceof Class)
				return;
			if (isArray(o)) {
				for (var i = 0; i < Array.getLength(o); i++)
					assertValidParameter(Array.get(o, i));
				return;
			}
		}
		throw bex("Invalid object type passed to BeanDictionaryMap: ''{0}''.  Only objects of type Class or Object[] containing Class or Object[] objects can be used.", cn(o));
	}

	/**
	 * Add a dictionary name mapping for the specified class.
	 *
	 * @param typeName The dictionary name of the class.
	 * @param c The class represented by the dictionary name.
	 * @return This object.
	 */
	protected BeanDictionaryMap append(String typeName, Class<?> c) {
		put(typeName, c);
		return this;
	}

	/**
	 * Add a dictionary name mapping for the specified collection class with the specified entry class.
	 *
	 * @param typeName The dictionary name of the class.
	 * @param collectionClass The collection implementation class.
	 * @param entryClass The entry class.
	 * @return This object.
	 */
	protected BeanDictionaryMap append(String typeName, Class<? extends Collection> collectionClass, Object entryClass) {
		assertValidParameter(entryClass);
		put(typeName, a(collectionClass, entryClass));
		return this;
	}

	/**
	 * Add a dictionary name mapping for the specified map class with the specified key and value classes.
	 *
	 * @param typeName The dictionary name of the class.
	 * @param mapClass The map implementation class.
	 * @param keyClass The key class.
	 * @param valueClass The value class.
	 * @return This object.
	 */
	protected BeanDictionaryMap append(String typeName, Class<? extends Map> mapClass, Object keyClass, Object valueClass) {
		assertValidParameter(keyClass);
		assertValidParameter(valueClass);
		put(typeName, a(mapClass, keyClass, valueClass));
		return this;
	}
}