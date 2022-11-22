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

import static org.apache.juneau.internal.CollectionUtils.*;

import java.util.*;
import java.util.stream.*;

import org.apache.juneau.cp.*;
import org.springframework.context.*;

/**
 * A bean store that uses Spring bean resolution to find beans if they're not already in this store.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#juneau-rest-server-springboot">juneau-rest-server-springboot</a>
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
	public <T> Optional<T> getBean(Class<T> c) {
		try {
			Optional<T> o = super.getBean(c);
			if (o.isPresent())
				return o;
			if (appContext.isPresent()) {
				return optional(appContext.get().getBeanProvider(c).getIfAvailable());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return empty();
	}

	@Override
	public <T> Optional<T> getBean(Class<T> c, String name) {
		try {
			Optional<T> o = super.getBean(c, name);
			if (o.isPresent())
				return o;
			if (appContext.isPresent()) {
				ApplicationContext ctx = appContext.get();
				if (name != null)
					return optional(ctx.containsBean(name) ? appContext.get().getBean(name, c) : null);
				return optional(appContext.get().getBean(c));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return empty();
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> Stream<BeanStoreEntry<T>> stream(Class<T> c)  {
		try {
			Stream<BeanStoreEntry<T>> o = super.stream(c);
			if (appContext.isPresent())
				o = Stream.concat(o, appContext.get().getBeansOfType(c).entrySet().stream().map(x -> BeanStoreEntry.create(c, ()->x.getValue(), x.getKey())));
			return o;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return Collections.emptyList().stream().map(x -> (BeanStoreEntry<T>)x);
	}
}
