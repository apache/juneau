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
package org.apache.juneau.annotation;

import static org.apache.juneau.BeanContext.*;

import org.apache.juneau.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.svl.*;

/**
 * Applies targeted {@link Bean} annotations to a {@link PropertyStoreBuilder}.
 */
public class BeanApply extends ConfigApply<Bean> {

	/**
	 * Constructor.
	 *
	 * @param c The annotation class.
	 * @param r The resolver for resolving values in annotations.
	 */
	public BeanApply(Class<Bean> c, VarResolverSession r) {
		super(c, r);
	}

	@Override
	public void apply(AnnotationInfo<Bean> ai, PropertyStoreBuilder psb) {
		Bean a = ai.getAnnotation();

		if (a.on().length == 0 && a.onClass().length == 0)
			return;

		Bean copy = BeanBuilder
			.create()
			.dictionary(a.dictionary())
			.example(string(a.example()))
			.excludeProperties(string(a.excludeProperties()))
			.fluentSetters(a.fluentSetters())
			.implClass(a.implClass())
			.interceptor(a.interceptor())
			.interfaceClass(a.interfaceClass())
			.on(strings(a.on()))
			.onClass(a.onClass())
			.p(string(a.p()))
			.properties(string(a.properties()))
			.propertyNamer(a.propertyNamer())
			.readOnlyProperties(string(a.readOnlyProperties()))
			.ro(string(a.ro()))
			.sort(a.sort())
			.stopClass(a.stopClass())
			.typeName(string(a.typeName()))
			.typePropertyName(string(a.typePropertyName()))
			.wo(string(a.wo()))
			.writeOnlyProperties(string(a.writeOnlyProperties()))
			.xp(string(a.xp()))
			.build();

		psb.prependTo(BEAN_annotations, copy);
	}
}
