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
 * Builder class for {@link MarshalledFilter} objects.
 *
 * <p>
 * This class is the programmatic equivalent to the aggregation of one or more {@link Marshalled @Marshalled} annotations.
 */
public class MarshalledFilterBuilder {

	Class<?> marshalledClass;

	Class<?> implClass;
	String example;

	/**
	 * Constructor.
	 *
	 * @param marshalledClass The class that this filter applies to.
	 */
	public MarshalledFilterBuilder(Class<?> marshalledClass) {
		this.marshalledClass = marshalledClass;
	}

	/**
	 * Applies the information in the specified list of {@link Marshalled @Marshalled} annotations to this filter.
	 *
	 * @param annotations The annotations to apply.
	 * @return This object (for method chaining).
	 */
	public MarshalledFilterBuilder applyAnnotations(List<Marshalled> annotations) {

		for (Marshalled b : annotations) {

			if (b.implClass() != Null.class)
				implClass(b.implClass());

			if (! b.example().isEmpty())
				example(b.example());
		}
		return this;
	}

	/**
	 * Implementation class.
	 *
	 * @param value The new value for this setting.
	 * @return This object (for method chaining).
	 */
	public MarshalledFilterBuilder implClass(Class<?> value) {
		this.implClass = value;
		return this;
	}

	/**
	 * POJO example in Simplified JSON format.
	 *
	 * @param value The new value for this annotation.
	 * @return This object (for method chaining).
	 */
	public MarshalledFilterBuilder example(String value) {
		this.example = value;
		return this;
	}

	/**
	 * Creates a {@link MarshalledFilter} with settings in this builder class.
	 *
	 * @return A new {@link MarshalledFilter} instance.
	 */
	public MarshalledFilter build() {
		return new MarshalledFilter(this);
	}
}
