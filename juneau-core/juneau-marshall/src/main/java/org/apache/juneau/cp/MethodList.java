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
package org.apache.juneau.cp;

import java.lang.reflect.*;
import java.util.*;

/**
 * A simple list of {@link Method} objects.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 *
 * @serial exclude
 */
public class MethodList extends ArrayList<Method> {
	private static final long serialVersionUID = 1L;

	/**
	 * Creator.
	 *
	 * @param methods The methods to add to this list.
	 * @return A new list of methods.
	 */
	public static MethodList of(Collection<Method> methods) {
		return new MethodList(methods);
	}

	/**
	 * Constructor.
	 */
	public MethodList() {}

	/**
	 * Constructor.
	 *
	 * @param methods The methods to add to this list.
	 */
	public MethodList(Collection<Method> methods) {
		super(methods);
	}

}
