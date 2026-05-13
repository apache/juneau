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
package org.apache.juneau.commons.bean;

import org.apache.juneau.commons.reflect.AnnotationInfo;
import org.apache.juneau.commons.reflect.AnnotationProvider;
import org.apache.juneau.commons.reflect.ClassInfo;
import org.apache.juneau.commons.reflect.TypeVariables;

/**
 * Bean-modeling SPI seam that exposes the type-resolution operations the bean-property
 * validation pass needs from the marshalling layer.
 *
 * <p>
 * Implemented by the marshalling-side {@code MarshallingContext} so that
 * {@code BeanPropertyMeta.Builder#validate} can stay free of direct
 * {@code MarshallingContext} references and live in {@code org.apache.juneau.commons.bean}.
 *
 * <p>
 * The {@code Object}-typed counterpart on the {@code Builder} is what gets stored long-term;
 * {@code validate(...)} narrows it to {@link BeanTypeResolver} just for the duration of the
 * resolve+validate pass.
 *
 * @see BeanInfo
 */
public interface BeanTypeResolver {

	/**
	 * Resolves the bean-property type-info for a property given its (optional)
	 * {@link BeanProp @BeanProp} annotation and Java type.
	 *
	 * @param lastBeanProp The last {@link BeanProp @BeanProp} annotation found on the property's
	 *	field/getter/setter, or {@code null} if none.
	 * @param type The Java type of the property as derived from the field/getter/setter signature.
	 * @param typeVarImpls Resolved type-variable substitutions for the enclosing class.
	 * @return The resolved type-info, or {@code null} if no resolution was possible.
	 */
	BeanInfo<?> resolveType(AnnotationInfo<BeanProp> lastBeanProp, ClassInfo type, TypeVariables typeVarImpls);

	/**
	 * Returns the type-info for {@code Object.class} (used as the fallback element/value type for
	 * raw/unparameterized collections and maps).
	 *
	 * @return Non-{@code null} type-info for {@code Object.class}.
	 */
	BeanInfo<?> objectType();

	/**
	 * Returns the annotation provider used to read annotations during property validation.
	 *
	 * @return Non-{@code null} annotation provider.
	 */
	AnnotationProvider getAnnotationProvider();
}
