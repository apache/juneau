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
package org.apache.juneau.commons.bean;

/**
 * Bean-modeling SPI seam that exposes the dictionary-lookup surface the bean-runtime types need without
 * coupling the bean-modeling layer to the marshalling-side {@code BeanRegistry}.
 *
 * <p>
 * Marshalling-side {@code BeanRegistry} implements this interface.  The bean-modeling-side
 * only sees {@link BeanRegistryLookup} when it needs to translate between a raw {@link Class}
 * and a polymorphic type name.  All operations involving {@code ClassMeta} (e.g.
 * {@code getClassMeta(String)}) remain on the marshalling-side {@code BeanRegistry} itself
 * and are reached only via narrowing casts from marshalling-side call sites.
 *
 * <p>
 * The bean-modeling layer never instantiates a registry directly — instances are always supplied
 * by the marshalling layer via marshalling-side hooks.
 *
 * <h5 class='topic'>Thread safety</h5>
 * Thread safety depends on implementation.
 */
public interface BeanRegistryLookup {

	/**
	 * Given the specified raw class, return the dictionary name for it, or <jk>null</jk> if not found.
	 *
	 * @param c The class to lookup in this registry.
	 * @return The dictionary name for the specified class, or <jk>null</jk> if not found.
	 */
	String getTypeName(Class<?> c);

	/**
	 * Returns <jk>true</jk> if this dictionary has an entry for the specified type name.
	 *
	 * @param typeName The bean type name.
	 * @return <jk>true</jk> if this dictionary has an entry for the specified type name.
	 */
	boolean hasName(String typeName);
}
