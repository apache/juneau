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

import org.apache.juneau.cp.BeanFactory;
import org.springframework.context.*;

/**
 * A bean factory that uses Spring bean resolution to find beans if they're not already in this factory.
 */
public class SpringBeanFactory extends BeanFactory {

	private final ApplicationContext appContext;

	/**
	 * Constructor.
	 *
	 * @param appContext The Spring application context used to resolve beans.
	 * @param parent The parent REST object bean factory.  Can be <jk>null</jk>.
	 * @param resource The REST object.  Can be <jk>null</jk>.
	 */
	public SpringBeanFactory(ApplicationContext appContext, BeanFactory parent, Object resource) {
		super(parent, resource);
		this.appContext = appContext;
	}

	@Override
	public <T> Optional<T> getBean(Class<T> c) {
		Optional<T> o = super.getBean(c);
		if (o.isPresent())
			return o;
		try {
			T t = appContext.getBean(c);
			return Optional.of(t);
		} catch (Exception e) {
			return Optional.empty();
		}
	}
}
