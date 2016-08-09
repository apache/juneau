/***************************************************************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 ***************************************************************************************************************************/
package org.apache.juneau.server.labels;

import org.apache.juneau.annotation.*;
import org.apache.juneau.internal.*;

/**
 * A request or response variable.
 */
@Bean(properties={"category","name","description"})
public class Var implements Comparable<Var> {

	/** Variable category (e.g. <js>"header"</js>, <js>"content"</js>) */
	public String category;

	/** Variable name (e.g. <js>"Content-Type"</js>) */
	public String name;

	/** Variable description */
	public String description;

	/** Bean constructor */
	public Var() {}

	/**
	 *
	 * Constructor.
	 * @param category Variable category (e.g. "ATTR", "PARAM").
	 * @param name Variable name.
	 */
	public Var(String category, String name) {
		this.category = category;
		this.name = name;
	}

	@SuppressWarnings("hiding")
	boolean matches(String category, String name) {
		if (this.category.equals(category))
			if (name == null || name.equals(this.name))
				return true;
		return false;
	}

	/**
	 * Sets the description for this variable.
	 *
	 * @param description The new description.
	 * @return This object (for method chaining).
	 */
	public Var setDescription(String description) {
		this.description = description;
		return this;
	}

	@Override /* Comparable */
	public int compareTo(Var o) {
		int i = category.compareTo(o.category);
		if (i == 0)
			i = (name == null ? -1 : name.compareTo(o.name));
		return i;
	}

	@Override /* Object */
	public boolean equals(Object o) {
		if (o instanceof Var) {
			Var v = (Var)o;
			return (v.category.equals(category) && StringUtils.isEquals(name, v.name));
		}
		return false;
	}

	@Override /* Object */
	public int hashCode() {
		return category.hashCode() + (name == null ? 0 : name.hashCode());
	}
}