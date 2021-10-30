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
package org.apache.juneau.swap;

/**
 * Bean interceptor.
 *
 * <p>
 * Bean interceptors intercept calls to bean getters and setters to allow them to override values in transit.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode w800'>
 * 	<jc>// Interceptor that strips out sensitive information on Address beans.</jc>
 * 	<jk>public class</jk> AddressInterceptor <jk>extends</jk> BeanInterceptor&lt;Address&gt; {
 *
 * 		<ja>@Override</ja>
 * 		<jk>public</jk> Object readProperty(Address bean, String name, Object value) {
 * 			<jk>if</jk> (<js>"taxInfo"</js>.equals(name))
 * 				<jk>return</jk> <js>"redacted"</js>;
 * 			<jk>return</jk> value;
 * 		}
 *
 * 		<ja>@Override</ja>
 * 		<jk>public</jk> Object writeProperty(Address bean, String name, Object value) {
 * 			<jk>if</jk> (<js>"taxInfo"</js>.equals(name) &amp;&amp; <js>"redacted"</js>.equals(value))
 * 				<jk>return</jk> TaxInfoUtils.<jsm>lookup</jsm>(bean.getStreet(), bean.getCity(), bean.getState());
 * 			<jk>return</jk> value;
 * 		}
 * 	}
 * </p>
 *
 * <p>
 * Bean interceptors are registered in the following way:
 * <ul class='javatree'>
 * 	<li class='ja'>{@link org.apache.juneau.annotation.Bean#interceptor() @Bean(interceptor)}
 * 	<li class='jm'>{@link org.apache.juneau.BeanContext.Builder#beanInterceptor(Class,Class)}
 * </ul>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode w800'>
 * 	<jc>// Register interceptor on bean class.</jc>
 * 	<ja>@Bean</ja>(interceptor=AddressInterceptor.<jk>class</jk>)
 * 	<jk>public class</jk> Address {
 * 		<jk>public</jk> String getTaxInfo() {...}
 * 		<jk>public void</jk> setTaxInfo(String s) {...}
 * 	}
 * </p>
 * @param <T> The bean type.
 */
public class BeanInterceptor<T> {

	/** Default interceptor */
	public static class Default extends BeanInterceptor<Object> {}

	/**
	 * Default reusable property filter instance.
	 */
	public static final BeanInterceptor<Object> DEFAULT = new Default();

	/**
	 * Property read interceptor.
	 *
	 * <p>
	 * Subclasses can override this property to convert property values to some other object just before serialization.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Address filter that strips out sensitive information.</jc>
	 * 	<jk>public class</jk> AddressInterceptor <jk>extends</jk> BeanInterceptor&lt;Address&gt; {
	 *
	 * 		<jk>public</jk> Object readProperty(Address bean, String name, Object value) {
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
	public Object readProperty(T bean, String name, Object value) {
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
	 * 	<jk>public class</jk> AddressInterceptor <jk>extends</jk> BeanInterceptor&lt;Address&gt; {
	 *
	 * 		<jk>public</jk> Object writeProperty(Address bean, String name, Object value) {
	 * 			<jk>if</jk> (<js>"taxInfo"</js>.equals(name) &amp;&amp; <js>"redacted"</js>.equals(value))
	 * 				<jk>return</jk> TaxInfoUtils.<jsm>lookup</jsm>(bean.getStreet(), bean.getCity(), bean.getState());
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
	public Object writeProperty(T bean, String name, Object value) {
		return value;
	}
}
