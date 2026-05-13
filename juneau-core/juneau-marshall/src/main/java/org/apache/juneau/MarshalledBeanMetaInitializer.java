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
package org.apache.juneau;

import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.commons.utils.StringUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;

import java.util.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.commons.bean.*;
import org.apache.juneau.commons.reflect.*;
import org.apache.juneau.commons.utils.*;

/**
 * Marshalling-side bridge for {@link BeanMeta} construction.
 *
 * <p>
 * Encapsulates marshalling-aware operations that were lifted out of {@link BeanMeta} as part of TODO-5
 * Step 8b-ii Phase C Task 3, so that {@link BeanMeta} can physically move into the
 * {@code org.apache.juneau.commons.bean} package without directly referencing
 * {@link BeanRegistry}, {@link MarshallingContext}, or marshalling-side annotations like
 * {@link Marshalled @Marshalled} from inside its constructor body.
 *
 * <p>
 * Each helper takes the marshalling-side {@link MarshallingContext} as an {@link Object} so the
 * call sites inside {@link BeanMeta} do not need to import {@link MarshallingContext}.  The helper
 * casts back internally.
 */
final class MarshalledBeanMetaInitializer {

	private MarshalledBeanMetaInitializer() {}

	/**
	 * Builds the bean-level {@link BeanRegistry} for a given bean class.
	 *
	 * <p>
	 * Replaces the body of {@link BeanMeta}'s former private {@code findBeanRegistry()} method.  Returns
	 * <jk>null</jk> when {@code marshallingContext} is <jk>null</jk> (commons-side construction path).
	 *
	 * @param marshallingContext The marshalling context.  May be <jk>null</jk>.
	 * @param beanFilter The bean filter applied to this bean meta.  May be <jk>null</jk>.
	 * @param classInfo The class info of the bean.  Must not be <jk>null</jk>.
	 * @param config The bean-modeling configuration.  Must not be <jk>null</jk>.
	 * @return The bean-level {@link BeanRegistryLookup}, or <jk>null</jk> on the commons-side path.
	 */
	static BeanRegistryLookup buildBeanRegistry(Object marshallingContext, BeanFilter beanFilter, ClassInfo classInfo, BeanConfigContext config) {
		if (marshallingContext == null)
			return null;
		MarshallingContext mc = (MarshallingContext) marshallingContext;

		// Bean dictionary on bean filter.
		var beanDictionaryClasses = opt(beanFilter).map(x -> new ArrayList<>(x.getBeanDictionary())).orElse(new ArrayList<>());

		// Bean dictionary from @Marshalled(typeName) annotation.
		var ba = config.getAnnotationProvider().find(Marshalled.class, classInfo);
		ba.stream().map(x -> x.inner().typeName()).filter(Utils::ne).findFirst().ifPresent(x -> beanDictionaryClasses.add(classInfo));

		return new BeanRegistry(mc, null, beanDictionaryClasses);
	}

	/**
	 * Constructs a per-property {@link BeanRegistry} chained on top of the bean-level registry.
	 *
	 * <p>
	 * Replaces the inline {@code new BeanRegistry(marshallingContext, beanRegistry, dictionaryClasses)} call that
	 * lived inside {@link BeanMeta}'s property-iteration loop.
	 *
	 * @param marshallingContext The marshalling context.  Must not be <jk>null</jk>.
	 * @param parent The bean-level registry (chained behind the per-property one).  May be <jk>null</jk>.
	 * @param dictionaryClasses The per-property {@link MarshalledProp#dictionary() @MarshalledProp(dictionary)} classes.  Must not be <jk>null</jk>.
	 * @return The per-property {@link BeanRegistryLookup}.
	 */
	static BeanRegistryLookup buildPropertyBeanRegistry(Object marshallingContext, BeanRegistryLookup parent, List<ClassInfo> dictionaryClasses) {
		return new BeanRegistry((MarshallingContext) marshallingContext, (BeanRegistry) parent, dictionaryClasses);
	}

	/**
	 * Walks the parent classes/interfaces of {@code classInfo} and returns the first
	 * {@link Marshalled#typeName() @Marshalled(typeName)} mapping for {@code rawClass} found in any parent's
	 * {@link BeanRegistry}.
	 *
	 * <p>
	 * Replaces the parent-walk stream that previously used {@code marshallingContext::getClassMeta} directly
	 * inside {@link BeanMeta#findDictionaryName()}.
	 *
	 * @param marshallingContext The marshalling context.  May be <jk>null</jk>.
	 * @param classInfo The bean's class info (used to derive the parents/interfaces walk).
	 * @param rawClass The raw class to look up a type name for.
	 * @return The dictionary name found in a parent's registry, or <jk>null</jk>.
	 */
	static String findTypeNameInParents(Object marshallingContext, ClassInfo classInfo, Class<?> rawClass) {
		if (marshallingContext == null)
			return null;
		MarshallingContext mc = (MarshallingContext) marshallingContext;
		return classInfo
			.getParentsAndInterfaces()
			.stream()
			.skip(1)
			.map(mc::getClassMeta)
			.map(ClassMeta::getBeanRegistry)
			.filter(Objects::nonNull)
			.map(x -> x.getTypeName(rawClass))
			.filter(Objects::nonNull)
			.findFirst()
			.orElse(null);
	}

	/**
	 * Extracts the {@link BeanConfigContext} embedded in a marshalling-side {@link BeanTypeInfo} (which is always
	 * a {@link ClassMeta}).
	 *
	 * <p>
	 * Used by the protected {@code BeanMeta(BeanTypeInfo, BeanFilter, String[], ClassInfo)} bridging constructor —
	 * lifts the {@code ((ClassMeta<T>) cm).getMarshallingContext().getBeanConfigContext()} cast chain out of the
	 * commons-side {@link BeanMeta} body.
	 *
	 * @param cm The bean type info.  Must be a marshalling-side {@link ClassMeta} instance.
	 * @return The bean-modeling configuration of the marshalling context.
	 */
	static BeanConfigContext configOf(BeanTypeInfo<?> cm) {
		return ((ClassMeta<?>) cm).getMarshallingContext().getBeanConfigContext();
	}

	/**
	 * Extracts the {@link MarshallingContext} embedded in a marshalling-side {@link BeanTypeInfo} (which is always
	 * a {@link ClassMeta}) and returns it as an {@link Object} so the caller does not need to import
	 * {@link MarshallingContext}.
	 *
	 * @param cm The bean type info.  Must be a marshalling-side {@link ClassMeta} instance.
	 * @return The marshalling context, as an {@link Object}.
	 */
	static Object contextOf(BeanTypeInfo<?> cm) {
		return ((ClassMeta<?>) cm).getMarshallingContext();
	}

	/**
	 * Narrows a marshalling-side {@link BeanTypeInfo} (which is always a {@link ClassMeta}) to {@link ClassInfo}.
	 *
	 * <p>
	 * Since {@link ClassMeta} extends {@link ClassInfo}, the cast is direct.  Provided as a helper so the commons-side
	 * caller does not have to import {@link ClassMeta}.
	 *
	 * @param cm The bean type info.  Must be a marshalling-side {@link ClassMeta} instance.
	 * @return The class info for the underlying bean class.
	 */
	static ClassInfo classInfoOf(BeanTypeInfo<?> cm) {
		return (ClassMeta<?>) cm;
	}

	/**
	 * Resolves the {@link Marshalled#typePropertyName() @Marshalled(typePropertyName)} value for a bean class, falling
	 * back to {@link BeanConfigContext#getBeanTypePropertyName()} when no annotation supplies one.
	 *
	 * <p>
	 * Encapsulates the {@link Marshalled @Marshalled} annotation read previously performed inline by
	 * {@link BeanMeta}'s constructor, so {@link BeanMeta} itself no longer references {@link Marshalled}.
	 *
	 * @param config The bean-modeling configuration (used to access the annotation provider and the fallback default).
	 * @param classInfo The bean's class info.
	 * @return The resolved type property name.  Never <jk>null</jk>.
	 */
	static String resolveTypePropertyName(BeanConfigContext config, ClassInfo classInfo) {
		return config.getAnnotationProvider().find(Marshalled.class, classInfo).stream()
			.map(x -> x.inner().typePropertyName())
			.filter(Utils::ne)
			.findFirst()
			.orElseGet(config::getBeanTypePropertyName);
	}

	/**
	 * Looks up the {@link Marshalled#typeName() @Marshalled(typeName)} value for a bean class.
	 *
	 * <p>
	 * Used by {@link BeanMeta#findDictionaryName()} as the last fallback when no other dictionary name is found.
	 *
	 * @param config The bean-modeling configuration (used to access the annotation provider).
	 * @param classInfo The bean's class info.
	 * @return The configured type name, or <jk>null</jk> if not set.
	 */
	static String findMarshalledTypeName(BeanConfigContext config, ClassInfo classInfo) {
		return config.getAnnotationProvider().find(Marshalled.class, classInfo)
			.stream()
			.map(AnnotationInfo::inner)
			.filter(x -> ! x.typeName().isEmpty())
			.map(Marshalled::typeName)
			.findFirst()
			.orElse(null);
	}

	/**
	 * Determines whether the class has any "registered as bean" annotation
	 * ({@link Marshalled @Marshalled} or {@link BeanType @BeanType}).
	 *
	 * <p>
	 * Used by {@link BeanMeta#findBeanConstructor()} to decide whether private constructors are permissible.
	 *
	 * @param config The bean-modeling configuration (used to access the annotation provider).
	 * @param classInfo The bean's class info.
	 * @return <jk>true</jk> if the class has either annotation.
	 */
	static boolean hasBeanRegistrationAnnotation(BeanConfigContext config, ClassInfo classInfo) {
		var ap = config.getAnnotationProvider();
		return ! ap.find(Marshalled.class, classInfo).isEmpty()
			|| ! ap.find(BeanType.class, classInfo).isEmpty();
	}

	/**
	 * Resolves the bean filter for a marshalling-side {@link BeanMeta}.
	 *
	 * <p>
	 * Lifted out of {@link BeanMeta}'s former private {@code findMarshalledFilter(ClassMeta)} static helper.  Returns
	 * a {@link MarshalledFilter} (the only concrete {@link BeanFilter} implementation in-tree) built from the
	 * {@link Marshalled @Marshalled} and {@link BeanType @BeanType} annotations on the class.
	 *
	 * @param cm The class meta for the bean class.
	 * @return The bean filter, or <jk>null</jk> if no relevant annotations are present.
	 */
	static <T> MarshalledFilter findMarshalledFilter(ClassMeta<T> cm) {
		var ap = cm.getMarshallingContext().getAnnotationProvider();
		var l = ap.find(Marshalled.class, cm);
		var bt = ap.find(BeanType.class, cm);
		if (l.isEmpty() && bt.isEmpty())
			return null;
		return MarshalledFilter.create(cm)
			.applyAnnotations(reverse(l.stream().map(AnnotationInfo::inner).toList()))
			.applyBeanTypeAnnotations(reverse(bt.stream().map(AnnotationInfo::inner).toList()))
			.build();
	}
}
