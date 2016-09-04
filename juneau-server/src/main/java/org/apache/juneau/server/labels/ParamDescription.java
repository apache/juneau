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
package org.apache.juneau.server.labels;

import org.apache.juneau.annotation.*;

/**
 * Simple bean for describing GET parameters.
 *
 * @author James Bognar (james.bognar@salesforce.com)
 */
@Bean(properties={"name","dataType","description"})
public final class ParamDescription {
	private String name;
	private String dataType;
	private String description;

	/** No-arg constructor.  Used for JUnit testing of OPTIONS pages. */
	public ParamDescription() {}

	/**
	 * Constructor.
	 *
	 * @param name A name.
	 * @param dataType Typically a fully-qualified class name.
	 * @param description A description.
	 */
	public ParamDescription(String name, String dataType, String description) {
		this.name = name;
		this.dataType = dataType;
		this.description = description;
	}

	/**
	 * Returns the name field on this label.
	 *
	 * @return The name.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Sets the name field on this label to a new value.
	 *
	 * @param name The new name.
	 * @return This object (for method chaining).
	 */
	public ParamDescription setName(String name) {
		this.name = name;
		return this;
	}

	/**
	 * Returns the dataType field on this label.
	 *
	 * @return The dataType.
	 */
	public String getDataType() {
		return dataType;
	}

	/**
	 * Sets the dataType field on this label to a new value.
	 *
	 * @param dataType The new data type.
	 * @return This object (for method chaining).
	 */
	public ParamDescription setDataType(String dataType) {
		this.dataType = dataType;
		return this;
	}

	/**
	 * Returns the description field on this label.
	 *
	 * @return The description.
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Sets the description field on this label to a new value.
	 *
	 * @param description The new description.
	 * @return This object (for method chaining).
	 */
	public ParamDescription setDescription(String description) {
		this.description = description;
		return this;
	}
}
