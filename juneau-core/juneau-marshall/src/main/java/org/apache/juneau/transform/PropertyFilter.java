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
package org.apache.juneau.transform;

/**
 * Bean property filter.
 *
 * @deprecated Use {@link BeanInterceptor}.
 */
@Deprecated
public class PropertyFilter {

	/**
	 * Default reusable property filter instance.
	 */
	public static final PropertyFilter DEFAULT = new PropertyFilter();

	/**
	 * Property read interceptor.
	 *
	 * <p>
	 * Subclasses can override this property to convert property values to some other object just before serialization.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Address filter that strips out sensitive information.</jc>
	 * 	<jk>public class</jk> AddressPropertyFilter <jk>extends</jk> PropertyFilter {
	 *
	 * 		<jk>public</jk> Object readProperty(Object bean, String name, Object value) {
	 * 			<jk>if</jk> (<js>"taxInfo"</js>.equals(name))
	 * 				<jk>return</jk> <js>"redacted"</js>;
	 * 			<jk>return</jk> value;
	 * 		}
	 * 	}
	 * </p>
	 *
	 * @param bean The bean from which the property was read.
	 * @param name The property name.
	 * @param value The value just extracted from calling the bean getter.
	 * @return The value to serialize.  Default is just to return the existing value.
	 */
	public Object readProperty(Object bean, String name, Object value) {
		return value;
	}

	/**
	 * Property write interceptor.
	 *
	 * <p>
	 * Subclasses can override this property to convert property values to some other object just before calling the
	 * bean setter.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Address filter that strips out sensitive information.</jc>
	 * 	<jk>public class</jk> AddressPropertyFilter <jk>extends</jk> PropertyFilter {
	 *
	 * 		<jk>public</jk> Object writeProperty(Object bean, String name, Object value) {
	 * 			AddressBook a = (Address)bean;
	 * 			<jk>if</jk> (<js>"taxInfo"</js>.equals(name) &amp;&amp; <js>"redacted"</js>.equals(value))
	 * 				<jk>return</jk> TaxInfoUtils.<jsm>lookup</jsm>(a.getStreet(), a.getCity(), a.getState());
	 * 			<jk>return</jk> value;
	 * 		}
	 * 	}
	 * </p>
	 *
	 * @param bean The bean from which the property was read.
	 * @param name The property name.
	 * @param value The value just parsed.
	 * @return The value to serialize.  Default is just to return the existing value.
	 */
	public Object writeProperty(Object bean, String name, Object value) {
		return value;
	}
}
