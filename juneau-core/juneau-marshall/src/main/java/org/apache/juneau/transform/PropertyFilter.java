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
 * <p>
 * Registers a bean property filter with a bean class.
 *
 * <p>
 * Property filters can be used to intercept calls to getters and setters and alter their values in transit.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode w800'>
 * 	<jc>// Property filter that strips out sensitive information on Address beans.</jc>
 * 	<jk>public class</jk> AddressPropertyFilter <jk>extends</jk> PropertyFilter {
 *
 * 		<ja>@Override</ja>
 * 		<jk>public</jk> Object readProperty(Object bean, String name, Object value) {
 * 			<jk>if</jk> (<js>"taxInfo"</js>.equals(name))
 * 				<jk>return</jk> <js>"redacted"</js>;
 * 			<jk>return</jk> value;
 * 		}
 *
 * 		<ja>@Override</ja>
 * 		<jk>public</jk> Object writeProperty(Object bean, String name, Object value) {
 * 			AddressBook a = (Address)bean;
 * 			<jk>if</jk> (<js>"taxInfo"</js>.equals(name) &amp;&amp; <js>"redacted"</js>.equals(value))
 * 				<jk>return</jk> TaxInfoUtils.<jsm>lookup</jsm>(a.getStreet(), a.getCity(), a.getState());
 * 			<jk>return</jk> value;
 * 		}
 * 	}
 * </p>
 *
 * <p>
 * Property filters are registered in the following ways:
 * <ul>
 * 	<li class='ja'>{@link org.apache.juneau.annotation.Bean#propertyFilter() @Bean.propertyFilter()}
 * 	<li class='jm'>{@link org.apache.juneau.transform.BeanFilterBuilder#propertyFilter(Class)}
 * </ul>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode w800'>
 * 	<jc>// Register filter on bean class.</jc>
 * 	<ja>@Bean</ja>(propertyFilter=AddressPropertyFilter.<jk>class</jk>)
 * 	<jk>public class</jk> Address {
 * 		<jk>public</jk> String getTaxInfo() {...}
 * 		<jk>public void</jk> setTaxInfo(String s) {...}
 * 	}
 *
 * 	<jc>// Or define a bean filter.</jc>
 * 	<jk>public class</jk> MyFilter <jk>extends</jk> BeanFilterBuilder&lt;Address&gt; {
 * 		<jk>public</jk> MyFilter() {
 * 			<jc>// Our bean contains generic collections of Foo and Bar objects.</jc>
 * 			propertyFilter(AddressPropertyFilter.<jk>class</jk>);
 * 		}
 * 	}
 *
 * 	<jc>// Register filter on serializer or parser.</jc>
 * 	WriterSerializer s = JsonSerializer
 * 		.<jsm>create</jsm>()
 * 		.beanFilters(MyFilter.<jk>class</jk>)
 * 		.build();
 * </p>
 */
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
