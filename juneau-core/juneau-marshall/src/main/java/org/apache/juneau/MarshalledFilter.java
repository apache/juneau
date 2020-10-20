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

import org.apache.juneau.annotation.*;

/**
 * Parent class for all non-bean filters.
 *
 * <p>
 * Marshall filters are used to control aspects of how POJOs are handled during serialization and parsing.
 *
 * <p>
 * Marshall filters are created by {@link MarshalledFilterBuilder} which is the programmatic equivalent to the {@link Marshalled @Marshalled}
 * annotation.
 */
public final class MarshalledFilter {

	private final Class<?> marshalledClass;

	private final Class<?> implClass;
	private final String example;

	/**
	 * Constructor.
	 */
	MarshalledFilter(MarshalledFilterBuilder builder) {
		this.marshalledClass = builder.marshalledClass;
		this.implClass = builder.implClass;
		this.example = builder.example;
	}

	/**
	 * Create a new instance of this POJO filter.
	 *
	 * @param <T> The POJO class being filtered.
	 * @param marshalledClass The POJO class being filtered.
	 * @return A new {@link MarshalledFilterBuilder} object.
	 */
	public static <T> MarshalledFilterBuilder create(Class<T> marshalledClass) {
		return new MarshalledFilterBuilder(marshalledClass);
	}

	/**
	 * Returns the class that this filter applies to.
	 *
	 * @return The class that this filter applies to.
	 */
	public Class<?> getMarshalledClass() {
		return marshalledClass;
	}

	/**
	 * Returns the implementation class associated with this class.
	 *
	 * @return The implementation class associated with this class, or <jk>null</jk> if no implementation class is associated.
	 */
	public Class<?> getImplClass() {
		return implClass;
	}

	/**
	 * Returns the example string with this class.
	 *
	 * @return The example string associated with this class, or <jk>null</jk> if no example string is associated.
	 */
	public String getExample() {
		return example;
	}
}
