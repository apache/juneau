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

import static org.apache.juneau.common.internal.StringUtils.*;
import static org.apache.juneau.internal.ClassUtils.*;

import java.util.*;

import org.apache.juneau.annotation.*;

/**
 * Parent class for all non-bean filters.
 *
 * <p>
 * Marshalled filters are used to control aspects of how POJOs are handled during serialization and parsing.
 *
 * <p>
 * Marshalled filters are created by {@link Builder} which is the programmatic equivalent to the {@link Marshalled @Marshalled}
 * annotation.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 */
public final class MarshalledFilter {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Create a new instance of this POJO filter.
	 *
	 * @param <T> The POJO class being filtered.
	 * @param marshalledClass The POJO class being filtered.
	 * @return A new {@link Builder} object.
	 */
	public static <T> Builder create(Class<T> marshalledClass) {
		return new Builder(marshalledClass);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Builder
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Builder class.
	 */
	public static class Builder {

		Class<?> marshalledClass;

		Class<?> implClass;
		String example;

		/**
		 * Constructor.
		 *
		 * @param marshalledClass The class that this filter applies to.
		 */
		protected Builder(Class<?> marshalledClass) {
			this.marshalledClass = marshalledClass;
		}

		/**
		 * Applies the information in the specified list of {@link Marshalled @Marshalled} annotations to this filter.
		 *
		 * @param annotations The annotations to apply.
		 * @return This object.
		 */
		public Builder applyAnnotations(List<Marshalled> annotations) {

			annotations.forEach(x -> {
				if (isNotVoid(x.implClass()))
					implClass(x.implClass());
				if (isNotEmpty(x.example()))
					example(x.example());
			});
			return this;
		}

		/**
		 * Implementation class.
		 *
		 * @param value The new value for this setting.
		 * @return This object.
		 */
		public Builder implClass(Class<?> value) {
			this.implClass = value;
			return this;
		}

		/**
		 * POJO example in Simplified JSON format.
		 *
		 * @param value The new value for this annotation.
		 * @return This object.
		 */
		public Builder example(String value) {
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

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	private final Class<?> marshalledClass;
	private final Class<?> implClass;
	private final String example;

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	protected MarshalledFilter(Builder builder) {
		this.marshalledClass = builder.marshalledClass;
		this.implClass = builder.implClass;
		this.example = builder.example;
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
