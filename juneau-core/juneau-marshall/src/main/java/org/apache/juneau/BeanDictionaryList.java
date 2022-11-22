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
package org.apache.juneau;

import java.util.*;

import org.apache.juneau.annotation.*;

/**
 * Represents a collection of bean classes that make up a bean dictionary.
 *
 * <p>
 * The classes in the list must be one of the following:
 * <ul>
 * 	<li>Beans that provide a dictionary name using the {@link Bean#typeName() @Bean(typeName)} annotation.
 * 	<li>Other subclasses of {@link BeanDictionaryList}.
 * 	<li>Other subclasses of {@link BeanDictionaryMap}.
 * </ul>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// A bean dictionary list consisting of classes with @Bean(typeName) annotations.</jc>
 * 	<jk>public class</jk> MyBeanDictionaryList <jk>extends</jk> BeanDictionaryList {
 *
 * 		<jc>// Must provide a no-arg constructor!</jc>
 * 		<jk>public</jk> MyBeanDictionaryList() {
 * 			<jk>super</jk>(ABean.<jk>class</jk>, BBean.<jk>class</jk>, CBean.<jk>class</jk>);
 * 		}
 * 	}
 *
 * 	<jc>// Use it in a parser.</jc>
 * 	ReaderParser <jv>parser</jv> = JsonParser
 * 		.<jsm>create</jsm>()
 * 		.dictionary(MyBeanDictionaryList.<jk>class</jk>)
 * 		.build();
 * </p>
 *
 * <p>
 * Subclasses must implement a public no-arg constructor so that it can be instantiated by the bean context code.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 *
 * @serial exclude
 */
public class BeanDictionaryList extends ArrayList<Class<?>> {
	private static final long serialVersionUID = 1L;

	/**
	 * Constructor.
	 *
	 * @param c
	 * 	The list of bean classes to add to this dictionary.
	 * 	Classes must either specify a {@link Bean#typeName() @Bean(typeName)} value or be another subclass of
	 * 	<c>BeanDictionaryList</c>.
	 */
	protected BeanDictionaryList(Class<?>...c) {
		append(c);
	}

	/**
	 * Append one or more bean classes to this bean dictionary.
	 *
	 * @param c
	 * 	The list of bean classes to add to this dictionary.
	 * 	Classes must either specify a {@link Bean#typeName() @Bean(typeName)} value or be another subclass of
	 * 	<c>BeanDictionaryList</c>.
	 * @return This object.
	 */
	protected BeanDictionaryList append(Class<?>...c) {
		for (Class<?> cc : c)
			add(cc);
		return this;
	}
}
