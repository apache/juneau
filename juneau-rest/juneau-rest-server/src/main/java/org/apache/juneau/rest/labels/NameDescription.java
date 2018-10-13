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
package org.apache.juneau.rest.labels;

import org.apache.juneau.annotation.*;

/**
 * @deprecated No replacement.
 */
@Deprecated
@Bean(properties="name,description")
public class NameDescription {

	private Object name;
	private Object description;

	/** No-arg constructor.  Used for JUnit testing of OPTIONS pages. */
	public NameDescription() {}

	/**
	 * Constructor.
	 *
	 * @param name A name.
	 * @param description A description.
	 */
	public NameDescription(Object name, Object description) {
		this.name = name;
		this.description = description;
	}

	/**
	 * Returns the name field on this label.
	 *
	 * @return The name.
	 */
	public Object getName() {
		return name;
	}

	/**
	 * Sets the name field on this label to a new value.
	 *
	 * @param name The new name.
	 * @return This object (for method chaining).
	 */
	@BeanProperty
	public NameDescription name(Object name) {
		this.name = name;
		return this;
	}

	/**
	 * Returns the description field on this label.
	 *
	 * @return The description.
	 */
	public Object getDescription() {
		return description;
	}

	/**
	 * Sets the description field on this label to a new value.
	 *
	 * @param description The new description.
	 * @return This object (for method chaining).
	 */
	@BeanProperty
	public NameDescription description(Object description) {
		this.description = description;
		return this;
	}
}
