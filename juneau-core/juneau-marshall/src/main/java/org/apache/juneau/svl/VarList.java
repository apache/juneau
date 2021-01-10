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
package org.apache.juneau.svl;

import java.util.*;

/**
 * Simple list of variables that can consist of either variable classes or instances.
 */
public class VarList extends ArrayList<Object> {

	private static final long serialVersionUID = 1L;

	/**
	 * Creates a new list of variables.
	 *
	 * @param vars The variables to create.
	 * @return A new list of variables.
	 */
	public static VarList of(Var...vars) {
		return new VarList().append(vars);
	}

	/**
	 * Creates a new list of variables.
	 *
	 * @param vars The variables to create.
	 * @return A new list of variables.
	 */
	@SuppressWarnings("unchecked")
	public static VarList of(Class<? extends Var>...vars) {
		return new VarList().append(vars);
	}

	/**
	 * Appends a list of variables to this list.
	 *
	 * @param vars The variables to append to this list.
	 * @return This object (for method chaining).
	 */
	private VarList append(Var...vars) {
		addAll(Arrays.asList(vars));
		return this;
	}

	/**
	 * Appends a list of variables to this list.
	 *
	 * @param vars The variables to append to this list.
	 * @return This object (for method chaining).
	 */
	@SuppressWarnings("unchecked")
	private VarList append(Class<? extends Var>...vars) {
		addAll(Arrays.asList(vars));
		return this;
	}
}
