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

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;
import static org.apache.juneau.internal.ArrayUtils.*;

import java.lang.annotation.*;

import org.apache.juneau.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.svl.*;
import org.apache.juneau.swap.*;

/**
 * Utility classes and methods for the {@link Bean @Bean} annotation.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 */
public class BeanAnnotation {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	/** Default value */
	public static final Bean DEFAULT = create().build();

	/**
	 * Instantiates a new builder for this class.
	 *
	 * @return A new builder object.
	 */
	public static Builder create() {
		return new Builder();
	}

	/**
	 * Instantiates a new builder for this class.
	 *
	 * @param on The targets this annotation applies to.
	 * @return A new builder object.
	 */
	public static Builder create(Class<?>...on) {
		return create().on(on);
	}

	/**
	 * Instantiates a new builder for this class.
	 *
	 * @param on The targets this annotation applies to.
	 * @return A new builder object.
	 */
	public static Builder create(String...on) {
		return create().on(on);
	}

	/**
	 * Creates a copy of the specified annotation.
	 *
	 * @param a The annotation to copy.
	 * @param r The var resolver for resolving any variables.
	 * @return A copy of the specified annotation.
	 */
	public static Bean copy(Bean a, VarResolverSession r) {
		return
			create()
			.dictionary(a.dictionary())
			.example(r.resolve(a.example()))
			.excludeProperties(r.resolve(a.excludeProperties()))
			.findFluentSetters(a.findFluentSetters())
			.implClass(a.implClass())
			.interceptor(a.interceptor())
			.interfaceClass(a.interfaceClass())
			.on(r.resolve(a.on()))
			.onClass(a.onClass())
			.p(r.resolve(a.p()))
			.properties(r.resolve(a.properties()))
			.propertyNamer(a.propertyNamer())
			.readOnlyProperties(r.resolve(a.readOnlyProperties()))
			.ro(r.resolve(a.ro()))
			.sort(a.sort())
			.stopClass(a.stopClass())
			.typeName(r.resolve(a.typeName()))
			.typePropertyName(r.resolve(a.typePropertyName()))
			.wo(r.resolve(a.wo()))
			.writeOnlyProperties(r.resolve(a.writeOnlyProperties()))
			.xp(r.resolve(a.xp()))
			.build();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Builder
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Builder class.
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.BeanContext.Builder#annotations(Annotation...)}
	 * </ul>
	 */
	public static class Builder extends TargetedAnnotationTBuilder {

		Class<?>[] dictionary = new Class[0];
		Class<?> implClass=void.class, interfaceClass=void.class, stopClass=void.class;
		Class<? extends BeanInterceptor<?>> interceptor=BeanInterceptor.Void.class;
		Class<? extends PropertyNamer> propertyNamer=BasicPropertyNamer.class;
		String example="", excludeProperties="", p="", properties="", readOnlyProperties="", ro="", typeName="", typePropertyName="", wo="", writeOnlyProperties="", xp="";
		boolean findFluentSetters, sort;

		/**
		 * Constructor.
		 */
		protected Builder() {
			super(Bean.class);
		}

		/**
		 * Instantiates a new {@link Bean @Bean} object initialized with this builder.
		 *
		 * @return A new {@link Bean @Bean} object.
		 */
		public Bean build() {
			return new Impl(this);
		}

		/**
		 * Sets the {@link Bean#dictionary()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder dictionary(Class<?>...value) {
			this.dictionary = value;
			return this;
		}

		/**
		 * Sets the {@link Bean#example()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder example(String value) {
			this.example = value;
			return this;
		}

		/**
		 * Sets the {@link Bean#excludeProperties()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder excludeProperties(String value) {
			this.excludeProperties = value;
			return this;
		}

		/**
		 * Sets the {@link Bean#findFluentSetters()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder findFluentSetters(boolean value) {
			this.findFluentSetters = value;
			return this;
		}

		/**
		 * Sets the {@link Bean#implClass()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder implClass(Class<?> value) {
			this.implClass = value;
			return this;
		}

		/**
		 * Sets the {@link Bean#interceptor()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder interceptor(Class<? extends BeanInterceptor<?>> value) {
			this.interceptor = value;
			return this;
		}

		/**
		 * Sets the {@link Bean#interfaceClass()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder interfaceClass(Class<?> value) {
			this.interfaceClass = value;
			return this;
		}

		/**
		 * Sets the {@link Bean#properties()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder properties(String value) {
			this.properties = value;
			return this;
		}

		/**
		 * Sets the {@link Bean#p()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder p(String value) {
			this.p = value;
			return this;
		}

		/**
		 * Sets the {@link Bean#propertyNamer()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder propertyNamer(Class<? extends PropertyNamer> value) {
			this.propertyNamer = value;
			return this;
		}

		/**
		 * Sets the {@link Bean#readOnlyProperties()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder readOnlyProperties(String value) {
			this.readOnlyProperties = value;
			return this;
		}

		/**
		 * Sets the {@link Bean#ro()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder ro(String value) {
			this.ro = value;
			return this;
		}

		/**
		 * Sets the {@link Bean#sort()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder sort(boolean value) {
			this.sort = value;
			return this;
		}

		/**
		 * Sets the {@link Bean#stopClass()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder stopClass(Class<?> value) {
			this.stopClass = value;
			return this;
		}

		/**
		 * Sets the {@link Bean#typeName()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder typeName(String value) {
			this.typeName = value;
			return this;
		}

		/**
		 * Sets the {@link Bean#typePropertyName()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder typePropertyName(String value) {
			this.typePropertyName = value;
			return this;
		}

		/**
		 * Sets the{@link Bean#wo()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder wo(String value) {
			this.wo = value;
			return this;
		}

		/**
		 * Sets the{@link Bean#writeOnlyProperties()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder writeOnlyProperties(String value) {
			this.writeOnlyProperties = value;
			return this;
		}

		/**
		 * Sets the {@link Bean#xp()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder xp(String value) {
			this.xp = value;
			return this;
		}

		// <FluentSetters>

		@Override /* GENERATED - TargetedAnnotationBuilder */
		public Builder on(String...values) {
			super.on(values);
			return this;
		}

		@Override /* GENERATED - TargetedAnnotationTBuilder */
		public Builder on(java.lang.Class<?>...value) {
			super.on(value);
			return this;
		}

		@Override /* GENERATED - TargetedAnnotationTBuilder */
		public Builder onClass(java.lang.Class<?>...value) {
			super.onClass(value);
			return this;
		}

		// </FluentSetters>
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Implementation
	//-----------------------------------------------------------------------------------------------------------------

	private static class Impl extends TargetedAnnotationTImpl implements Bean {

		private final boolean findFluentSetters, sort;
		private final Class<? extends BeanInterceptor<?>> interceptor;
		private final Class<? extends PropertyNamer> propertyNamer;
		private final Class<?> implClass, interfaceClass, stopClass;
		private final Class<?>[] dictionary;
		private final String example, excludeProperties, p, properties, readOnlyProperties, ro, typeName, typePropertyName, wo, writeOnlyProperties, xp;

		Impl(Builder b) {
			super(b);
			this.dictionary = copyOf(b.dictionary);
			this.example = b.example;
			this.excludeProperties = b.excludeProperties;
			this.findFluentSetters = b.findFluentSetters;
			this.implClass = b.implClass;
			this.interceptor = b.interceptor;
			this.interfaceClass = b.interfaceClass;
			this.p = b.p;
			this.properties = b.properties;
			this.propertyNamer = b.propertyNamer;
			this.readOnlyProperties = b.readOnlyProperties;
			this.ro = b.ro;
			this.sort = b.sort;
			this.stopClass = b.stopClass;
			this.typeName = b.typeName;
			this.typePropertyName = b.typePropertyName;
			this.wo = b.wo;
			this.writeOnlyProperties = b.writeOnlyProperties;
			this.xp = b.xp;
			postConstruct();
		}

		@Override /* Bean */
		public Class<?>[] dictionary() {
			return dictionary;
		}

		@Override /* Bean */
		public String example() {
			return example;
		}

		@Override /* Bean */
		public String excludeProperties() {
			return excludeProperties;
		}

		@Override /* Bean */
		public boolean findFluentSetters() {
			return findFluentSetters;
		}

		@Override /* Bean */
		public Class<?> implClass() {
			return implClass;
		}

		@Override /* Bean */
		public Class<? extends BeanInterceptor<?>> interceptor() {
			return interceptor;
		}

		@Override /* Bean */
		public Class<?> interfaceClass() {
			return interfaceClass;
		}

		@Override /* Bean */
		public String p() {
			return p;
		}

		@Override /* Bean */
		public String properties() {
			return properties;
		}

		@Override /* Bean */
		public Class<? extends PropertyNamer> propertyNamer() {
			return propertyNamer;
		}

		@Override /* Bean */
		public String readOnlyProperties() {
			return readOnlyProperties;
		}

		@Override /* Bean */
		public String ro() {
			return ro;
		}

		@Override /* Bean */
		public boolean sort() {
			return sort;
		}

		@Override /* Bean */
		public Class<?> stopClass() {
			return stopClass;
		}

		@Override /* Bean */
		public String typeName() {
			return typeName;
		}

		@Override /* Bean */
		public String typePropertyName() {
			return typePropertyName;
		}

		@Override /* Bean */
		public String writeOnlyProperties() {
			return writeOnlyProperties;
		}

		@Override /* Bean */
		public String wo() {
			return wo;
		}

		@Override /* Bean */
		public String xp() {
			return xp;
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Appliers
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Applies targeted {@link Bean} annotations to a {@link org.apache.juneau.BeanContext.Builder}.
	 */
	public static class Applier extends AnnotationApplier<Bean,BeanContext.Builder> {

		/**
		 * Constructor.
		 *
		 * @param vr The resolver for resolving values in annotations.
		 */
		public Applier(VarResolverSession vr) {
			super(Bean.class, BeanContext.Builder.class, vr);
		}

		@Override
		public void apply(AnnotationInfo<Bean> ai, BeanContext.Builder b) {
			Bean a = ai.inner();
			if (isEmptyArray(a.on(), a.onClass()))
				return;
			b.annotations(copy(a, vr()));
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * A collection of {@link Bean @Bean annotations}.
	 */
	@Documented
	@Target({METHOD,TYPE})
	@Retention(RUNTIME)
	@Inherited
	public static @interface Array {

		/**
		 * The child annotations.
		 *
		 * @return The annotation value.
		 */
		Bean[] value();
	}
}
