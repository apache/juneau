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

import java.util.*;

import org.apache.juneau.cp.BeanStore;
import org.springframework.context.*;

/**
 * A bean store that uses Spring bean resolution to find beans if they're not already in this store.
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc juneau-rest-server-springboot}
 * 	<li class='extlink'>{@source}
 * </ul>
 */
public class SpringBeanStore extends BeanStore {

	private final Optional<ApplicationContext> appContext;

	/**
	 * Constructor.
	 *
	 * @param appContext The Spring application context used to resolve beans.
	 * @param parent The parent REST object bean store.  Can be <jk>null</jk>.
	 * @param resource The REST object.  Can be <jk>null</jk>.
	 */
	public SpringBeanStore(Optional<ApplicationContext> appContext, Optional<BeanStore> parent, Object resource) {
		super(create().parent(parent.orElse(null)).outer(resource));
		this.appContext = appContext;
	}

	@Override
	public <T> Optional<T> getBean(String name, Class<T> c) {
		try {
			Optional<T> o = super.getBean(name, c);
			if (o.isPresent())
				return o;
			if (appContext.isPresent()) {
				if (name != null)
					return Optional.ofNullable(appContext.get().getBean(name, c));
				return Optional.ofNullable(appContext.get().getBean(c));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return Optional.empty();
	}
}
