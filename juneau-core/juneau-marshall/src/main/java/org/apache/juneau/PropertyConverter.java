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

import org.apache.juneau.PropertyStoreBuilder.*;
import org.apache.juneau.internal.*;

/**
 * Used to convert property values to standardized Boolean/Integer/Class/Object values in property store builders.
 */
interface PropertyConverter<T> {
	T convert(Object o, MutableProperty p);

	static final PropertyConverter<String> STRING_CONVERTER = new PropertyConverter<String>() {
		@Override
		public String convert(Object o, MutableProperty p) {
			return ClassUtils.toString(o);
		}
	};
	
	static final PropertyConverter<Integer> INTEGER_CONVERTER = new PropertyConverter<Integer>() {
		@Override
		public Integer convert(Object o, MutableProperty p) {
			try {
				if (o instanceof Integer)
					return (Integer)o;
				return Integer.valueOf(o.toString());
			} catch (Exception e) {
				throw new ConfigException("Value ''{0}'' ({1}) cannot be converted to an Integer on property ''{2}'' ({3}).", o, o.getClass().getSimpleName(), p.name, p.type);
			}
		}
	};
		
	static final PropertyConverter<Boolean> BOOLEAN_CONVERTER = new PropertyConverter<Boolean>() {
		@Override
		public Boolean convert(Object o, MutableProperty p) {
			if (o instanceof Boolean)
				return (Boolean)o;
			return Boolean.parseBoolean(o.toString());
		}
	};
		
	static final PropertyConverter<Class<?>> CLASS_CONVERTER = new PropertyConverter<Class<?>>() {
		@Override
		public Class<?> convert(Object o, MutableProperty p) {
			try {
				if (o instanceof Class)
					return (Class<?>)o;
				throw new ConfigException("Value ''{0}'' ({1}) cannot be converted to a Class on property ''{2}'' ({3}).", o, o.getClass().getSimpleName(), p.name, p.type);
			} catch (Exception e) {
				throw new ConfigException("Value ''{0}'' ({1}) cannot be converted to a Class on property ''{2}'' ({3}).", o, o.getClass().getSimpleName(), p.name, p.type);
			}
		}
	};
	
	static final PropertyConverter<Object> OBJECT_CONVERTER = new PropertyConverter<Object>() {
		@Override
		public Object convert(Object o, MutableProperty p) {
			try {
				if (o instanceof Class)
					return ((Class<?>)o).newInstance();
				return o;
			} catch (Exception e) {
				throw new ConfigException("Value ''{0}'' ({1}) cannot be converted to an Object on property ''{2}'' ({3}).", o, o.getClass().getSimpleName(), p.name, p.type);
			}
		}
	};
}
