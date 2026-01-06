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
package org.apache.juneau.commons.utils;

import static java.util.stream.Collectors.*;
import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;

import java.util.*;

import org.apache.juneau.commons.inject.*;
import org.apache.juneau.commons.reflect.*;

public class BeanUtils {

	/**
	 * Given an executable, returns a list of types that are missing from this factory.
	 *
	 * @param executable The constructor or method to get the params for.
	 * @param outer The outer object to use when instantiating inner classes.  Can be <jk>null</jk>.
	 * @return A comma-delimited list of types that are missing from this factory, or <jk>null</jk> if none are missing.
	 */
	public String getMissingParams(ExecutableInfo executable, BeanStore beanStore, Object outer) {
		var params = executable.getParameters();
		List<String> l = list();
		loop: for (int i = 0; i < params.size(); i++) {
			var pi = params.get(i);
			var pt = pi.getParameterType();
			if (i == 0 && nn(outer) && pt.isInstance(outer))
				continue loop;
			if (pt.is(Optional.class))
				continue loop;
			var beanName = pi.getResolvedQualifier();  // Use @Named for bean injection
			var ptc = pt.inner();
			if (beanName == null && ! beanStore.hasBean(ptc))
				l.add(pt.getNameSimple());
			if (nn(beanName) && ! beanStore.hasBean(ptc, beanName))
				l.add(pt.getNameSimple() + '@' + beanName);
		}
		return l.isEmpty() ? null : l.stream().sorted().collect(joining(","));
	}

	/**
	 * Returns the corresponding beans in this factory for the specified param types.
	 *
	 * @param executable The constructor or method to get the params for.
	 * @param outer The outer object to use when instantiating inner classes.  Can be <jk>null</jk>.
	 * @return The corresponding beans in this factory for the specified param types.
	 */
	public Object[] getParams(ExecutableInfo executable, BeanStore beanStore, Object outer) {
		var o = new Object[executable.getParameterCount()];
		for (var i = 0; i < executable.getParameterCount(); i++) {
			var pi = executable.getParameter(i);
			var pt = pi.getParameterType();
			if (i == 0 && nn(outer) && pt.isInstance(outer)) {
				o[i] = outer;
			} else {
				var beanQualifier = pi.getResolvedQualifier();
				var ptc = pt.unwrap(Optional.class).inner();
				var o2 = beanQualifier == null ? beanStore.getBean(ptc) : beanStore.getBean(ptc, beanQualifier);
				o[i] = pt.is(Optional.class) ? o2 : o2.orElse(null);
			}
		}
		return o;
	}

	public <T> T invoke(ExecutableInfo executable, BeanStore beanStore, Object bean) {
		var params = getParams(executable, beanStore, bean);
		if (executable instanceof ConstructorInfo ci)
			return ci.newInstance(params);
		return MethodInfo.class.cast(executable).invoke(bean, params);
	}

	/**
	 * Given the list of param types, returns <jk>true</jk> if this factory has all the parameters for the specified executable.
	 *
	 * @param executable The constructor or method to get the params for.
	 * @param outer The outer object to use when instantiating inner classes.  Can be <jk>null</jk>.
	 * @return A comma-delimited list of types that are missing from this factory.
	 */
	public boolean hasAllParams(ExecutableInfo executable, BeanStore beanStore, Object outer) {
		loop: for (int i = 0; i < executable.getParameterCount(); i++) {
			var pi = executable.getParameter(i);
			var pt = pi.getParameterType();
			if (i == 0 && nn(outer) && pt.isInstance(outer))
				continue loop;
			if (pt.is(Optional.class))
				continue loop;
			var beanQualifier = pi.getResolvedQualifier();
			var ptc = pt.inner();
			if ((beanQualifier == null && ! beanStore.hasBean(ptc)) || (nn(beanQualifier) && ! beanStore.hasBean(ptc, beanQualifier)))
				return false;
		}
		return true;
	}
}
