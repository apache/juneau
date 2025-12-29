/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.rest.springboot;

import static org.apache.juneau.commons.utils.Utils.*;

import java.util.*;
import java.util.stream.*;

import org.apache.juneau.cp.*;
import org.springframework.context.*;

/**
 * A bean store that uses Spring bean resolution to find beans if they're not already in this store.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauRestServerSpringbootBasics">juneau-rest-server-springboot Basics</a>
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

	@Override /* Overridden from BeanStore */
	public SpringBeanStore clear() {
		super.clear();
		return this;
	}

	@Override
	public <T> Optional<T> getBean(Class<T> c) {
		try {
			var o = super.getBean(c);
			if (o.isPresent())
				return o;
			if (appContext.isPresent()) {
				return opt(appContext.get().getBeanProvider(c).getIfAvailable());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return opte();
	}

	@Override
	public <T> Optional<T> getBean(Class<T> c, String name) {
		try {
			var o = super.getBean(c, name);
			if (o.isPresent())
				return o;
			if (appContext.isPresent()) {
				var ctx = appContext.get();
				if (nn(name))
					return opt(ctx.containsBean(name) ? appContext.get().getBean(name, c) : null);
				return opt(appContext.get().getBean(c));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return opte();
	}

	@Override /* Overridden from BeanStore */
	public SpringBeanStore removeBean(Class<?> beanType) {
		super.removeBean(beanType);
		return this;
	}

	@Override /* Overridden from BeanStore */
	public SpringBeanStore removeBean(Class<?> beanType, String name) {
		super.removeBean(beanType, name);
		return this;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> Stream<BeanStoreEntry<T>> stream(Class<T> c) {
		try {
			var o = super.stream(c);
			if (appContext.isPresent())
				o = Stream.concat(o, appContext.get().getBeansOfType(c).entrySet().stream().map(x -> BeanStoreEntry.create(c, () -> x.getValue(), x.getKey())));
			return o;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return Collections.emptyList().stream().map(x -> (BeanStoreEntry<T>)x);
	}
}