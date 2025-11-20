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
package org.apache.juneau.junit.bct;

/**
 * Functional interface for computing the size of objects in test assertions.
 *
 * <p>
 * Sizers are used by {@link BeanConverter#size(Object)} to determine the size of collection-like
 * objects when validating test assertions. This provides a flexible way to compute sizes for custom
 * types without needing to convert them to lists first.
 *
 * <h5 class='section'>Usage Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Register a sizer for a custom collection type</jc>
 * 	<jk>var</jk> <jv>converter</jv> = BasicBeanConverter.<jsm>builder</jsm>()
 * 		.defaultSettings()
 * 		.addSizer(MyCustomCollection.<jk>class</jk>, (<jp>coll</jp>, <jp>conv</jp>) -> <jp>coll</jp>.count())
 * 		.build();
 *
 * 	<jc>// Use in assertions</jc>
 * 	<jk>int</jk> <jv>size</jv> = <jv>converter</jv>.size(<jv>myCustomCollection</jv>);
 * </p>
 *
 * <h5 class='section'>See Also:</h5>
 * <ul class='seealso'>
 * 	<li class='jic'>{@link BeanConverter}
 * </ul>
 *
 * @param <T> The type of object this sizer handles.
 */
@FunctionalInterface
public interface Sizer<T> {

	/**
	 * Computes the size of the given object.
	 *
	 * @param o The object to compute the size of. Will not be <jk>null</jk>.
	 * @param bc The bean converter for accessing additional conversion utilities if needed.
	 * @return The size of the object.
	 */
	int size(T o, BeanConverter bc);
}

