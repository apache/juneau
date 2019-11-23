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
package org.apache.juneau.rest.springboot;

import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.springframework.context.ApplicationContext;

/**
 * Implementation of a {@link RestResourceResolver} for resolving resource classes using Spring.
 *
 * <p>
 * Used for resolving resource classes defined via {@link Rest#children()}.
 *
 * <p>
 * A typical usage pattern for registering a Juneau REST resource class is shown below:
 *
 * <p class='bpcode w800'>
 * 	<ja>@Configuration</ja>
 * 	<jk>public class</jk> MySpringConfiguration {
 *
 * 		<ja>@AutoWired</ja>
 * 		<jk>private static volatile</jk> ApplicationContext <jsf>appContext</jsf>;
 *
 * 		<ja>@Bean</ja>
 * 		<jk>public</jk> RestResourceResolver restResourceResolver(ApplicationContext appContext) {
 * 			<jk>return new</jk> SpringRestResourceResolver(appContext);
 * 		}
 *
 * 		<ja>@Bean</ja>
 * 		<jk>public</jk> RootRest root(RestResourceResolver resolver) {
 * 			<jk>return new</jk> RootRest().setRestResourceResolver(resolver);
 * 		}
 *
 * 		<ja>@Bean</ja>
 * 		<jk>public</jk> ServletRegistrationBean rootRegistration(RootRest root) {
 * 			<jk>return new</jk> ServletRegistrationBean(root, <jsf>CONTEXT_ROOT</jsf>, <jsf>CONTEXT_ROOT</jsf>+<js>"/"</js>, <jsf>CONTEXT_ROOT</jsf>+<js>"/*"</js>);
 * 		}
 * </p>
 */
public class SpringRestResourceResolver extends BasicRestResourceResolver {

	private ApplicationContext ctx;

	/**
	 * Constructor.
	 *
	 * @param ctx The spring application context object.
	 */
	public SpringRestResourceResolver(ApplicationContext ctx) {
		this.ctx = ctx;
	}

	@Override /* RestResourceResolver */
	public <T> T resolve(Object parent, Class<T> c, RestContextBuilder builder, Object...args) {
		T resource = null;
		try {
			resource = ctx.getBean(c);
		} catch (Exception e) { /* Ignore */ }
		if (resource == null)
			resource = super.resolve(parent, c, builder);
		return resource;
	}
}
