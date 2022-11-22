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
package org.apache.juneau.examples.core.pojo;

import org.apache.juneau.annotation.*;

/**
 * Sample pojo class.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 */
public class Pojo {
	private final String id;
	private final String name;

	/**
	 * @param id The <bc>id</bc> property value.
	 * @param name The <bc>name</bc> property value.
	 */
	@Beanc
	public Pojo(@Name("id") String id, @Name("name") String name) {
		this.id = id;
		this.name = name;
	}

	/**
	 * @return The <bc>id</bc> property value.
	 */
	public String getId() {
		return id;
	}

	/**
	 * @return The <bc>name</bc> property value.
	 */
	public String getName() {
		return name;
	}
}
