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

import org.apache.juneau.internal.*;

/**
 * Used to convert property values to standardized Boolean/Integer/Class/Object values in property store builders.
 *
 * @param <T> The normalized form.
 */
public interface PropertyConverter<T> {

	/**
	 * Convert the value to normalized form.
	 *
	 * @param o The raw value.
	 * @return The converted value.
	 */
	T convert(Object o);

	/**
	 * Converts objects to strings.
	 */
	static final PropertyConverter<String> STRING_CONVERTER = new PropertyConverter<String>() {
		@Override
		public String convert(Object o) {
			return ClassUtils.toString(o);
		}
	};

	/**
	 * Converts objects to integers.
	 */
	static final PropertyConverter<Integer> INTEGER_CONVERTER = new PropertyConverter<Integer>() {
		@Override
		public Integer convert(Object o) {
			try {
				if (o instanceof Integer)
					return (Integer)o;
				return Integer.valueOf(o.toString());
			} catch (Exception e) {
				throw new ConfigException("Value ''{0}'' ({1}) cannot be converted to an Integer.", o, o.getClass().getSimpleName());
			}
		}
	};

	/**
	 * Converts objects to booleans.
	 */
	static final PropertyConverter<Boolean> BOOLEAN_CONVERTER = new PropertyConverter<Boolean>() {
		@Override
		public Boolean convert(Object o) {
			if (o instanceof Boolean)
				return (Boolean)o;
			return Boolean.parseBoolean(o.toString());
		}
	};

	/**
	 * Converts objects to classes.
	 */
	static final PropertyConverter<Class<?>> CLASS_CONVERTER = new PropertyConverter<Class<?>>() {
		@Override
		public Class<?> convert(Object o) {
			try {
				if (o instanceof Class)
					return (Class<?>)o;
				throw new ConfigException("Value ''{0}'' ({1}) cannot be converted to a Class.", o, o.getClass().getSimpleName());
			} catch (Exception e) {
				throw new ConfigException("Value ''{0}'' ({1}) cannot be converted to a Class.", o, o.getClass().getSimpleName());
			}
		}
	};

	/**
	 * Converts objects to objects.
	 */
	static final PropertyConverter<Object> OBJECT_CONVERTER = new PropertyConverter<Object>() {
		@Override
		public Object convert(Object o) {
			return o;
		}
	};
}
