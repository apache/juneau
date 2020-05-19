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

import static org.apache.juneau.internal.StringUtils.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.reflect.*;

/**
 * Simple bean filter that simply identifies a class to be used as an interface class for all child classes.
 *
 * <p>
 * These objects are created when you pass in non-<c>BeanFilterBuilder</c> classes to
 * {@link BeanContextBuilder#beanFilters(Object...)}, and are equivalent to adding a
 * <code><ja>@Bean</ja>(interfaceClass=Foo.<jk>class</jk>)</code> annotation on the <c>Foo</c> class.
 *
 * @param <T> The interface class.
 */
public class InterfaceBeanFilterBuilder<T> extends BeanFilterBuilder<T> {

	/**
	 * Constructor.
	 *
	 * <p>
	 * Interface class is determined through reflection.
	 */
	protected InterfaceBeanFilterBuilder() {
		init(beanClass, BeanContext.DEFAULT);
	}

	/**
	 * Constructor.
	 *
	 * @param interfaceClass The class to use as an interface on all child classes.
	 * @param bc The bean context to use for looking up annotations.
	 */
	public InterfaceBeanFilterBuilder(Class<T> interfaceClass, BeanContext bc) {
		super(interfaceClass);
		init(interfaceClass, bc);
	}

	@SuppressWarnings("deprecation")
	private void init(Class<?> interfaceClass, BeanContext bc) {
		interfaceClass(interfaceClass);
		List<Bean> annotations = ClassInfo.of(interfaceClass).getAnnotations(Bean.class, bc);

		for (Bean b : annotations) {

			if (! b.properties().isEmpty())
				bpi(split(b.properties()));

			if (! b.excludeProperties().isEmpty())
				bpx(split(b.excludeProperties()));

			if (! b.bpi().isEmpty())
				bpi(split(b.bpi()));

			if (! b.bpx().isEmpty())
				bpx(split(b.bpx()));

			if (! b.bpro().isEmpty())
				bpro(split(b.bpro()));

			if (! b.bpwo().isEmpty())
				bpwo(split(b.bpwo()));

			if (! b.typeName().isEmpty())
				typeName(b.typeName());

			if (b.sort())
				sortProperties(true);

			if (b.fluentSetters())
				fluentSetters(true);

			try {
				if (b.propertyNamer() != PropertyNamerDefault.class)
					propertyNamer(b.propertyNamer());
			} catch (Exception e) {
				throw new RuntimeException(e);
			}

			if (b.interfaceClass() != Object.class)
				interfaceClass(b.interfaceClass());

			if (b.stopClass() != Object.class)
				stopClass(b.stopClass());

			if (b.beanDictionary().length > 0)
				dictionary(b.beanDictionary());

			if (b.dictionary().length > 0)
				dictionary(b.dictionary());
		}
	}
}
