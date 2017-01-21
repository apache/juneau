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
 */
public class BeanDictionary extends ArrayList<Class<?>> {
	private static final long serialVersionUID = 1L;

	/**
	 * Constructor.
	 *
	 * @param c The list of bean classes to add to this dictionary.
	 * 	Classes must either specify a {@link Bean#typeName()} value or be another subclass of <code>BeanDictionary</code>.
	 */
	public BeanDictionary(Class<?>...c) {
		append(c);
	}

	/**
	 * Append one or more bean classes to this bean dictionary.
	 *
	 * @param c The list of bean classes to add to this dictionary.
	 * 	Classes must either specify a {@link Bean#typeName()} value or be another subclass of <code>BeanDictionary</code>.
	 * @return This object (for method chaining).
	 */
	public BeanDictionary append(Class<?>...c) {
		for (Class<?> cc : c)
			add(cc);
		return this;
	}
}
