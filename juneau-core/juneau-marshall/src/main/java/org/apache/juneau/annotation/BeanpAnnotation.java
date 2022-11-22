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
import java.lang.reflect.*;

import org.apache.juneau.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.svl.*;

/**
 * Utility classes and methods for the {@link Beanp @Beanp} annotation.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 */
public class BeanpAnnotation {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	/** Default value */
	public static final Beanp DEFAULT = create().build();

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
	public static Builder create(String...on) {
		return create().on(on);
	}

	/**
	 * Creates a copy of the specified annotation.
	 *
	 * @param a The annotation to copy.s
	 * @param r The var resolver for resolving any variables.
	 * @return A copy of the specified annotation.
	 */
	public static Beanp copy(Beanp a, VarResolverSession r) {
		return
			create()
			.dictionary(a.dictionary())
			.format(r.resolve(a.format()))
			.name(r.resolve(a.name()))
			.on(r.resolve(a.on()))
			.params(a.params())
			.properties(r.resolve(a.properties()))
			.ro(r.resolve(a.ro()))
			.type(a.type())
			.value(r.resolve(a.value()))
			.wo(r.resolve(a.wo()))
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
	public static class Builder extends TargetedAnnotationMFBuilder {

		Class<?> type=void.class;
		Class<?>[] dictionary=new Class[0], params=new Class[0];
		String format="", name="", properties="", ro="", value="", wo="";

		/**
		 * Constructor.
		 */
		protected Builder() {
			super(Beanp.class);
		}

		/**
		 * Instantiates a new {@link Beanp @Beanp} object initialized with this builder.
		 *
		 * @return A new {@link Beanp @Beanp} object.
		 */
		public Beanp build() {
			return new Impl(this);
		}

		/**
		 * Sets the {@link Beanp#dictionary()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder dictionary(Class<?>...value) {
			this.dictionary = value;
			return this;
		}

		/**
		 * Sets the {@link Beanp#format()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder format(String value) {
			this.format = value;
			return this;
		}

		/**
		 * Sets the {@link Beanp#name()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder name(String value) {
			this.name = value;
			return this;
		}

		/**
		 * Sets the {@link Beanp#params()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder params(Class<?>...value) {
			this.params = value;
			return this;
		}

		/**
		 * Sets the {@link Beanp#properties()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder properties(String value) {
			this.properties = value;
			return this;
		}

		/**
		 * Sets the {@link Beanp#ro()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder ro(String value) {
			this.ro = value;
			return this;
		}

		/**
		 * Sets the {@link Beanp#type()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder type(Class<?> value) {
			this.type = value;
			return this;
		}

		/**
		 * Sets the {@link Beanp#value()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder value(String value) {
			this.value = value;
			return this;
		}

		/**
		 * Sets the {@link Beanp#wo()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder wo(String value) {
			this.wo = value;
			return this;
		}

		// <FluentSetters>

		@Override /* GENERATED - TargetedAnnotationBuilder */
		public Builder on(String...values) {
			super.on(values);
			return this;
		}

		@Override /* GENERATED - TargetedAnnotationMFBuilder */
		public Builder on(Field...value) {
			super.on(value);
			return this;
		}

		@Override /* GENERATED - TargetedAnnotationMFBuilder */
		public Builder on(Method...value) {
			super.on(value);
			return this;
		}

		// </FluentSetters>
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Implementation
	//-----------------------------------------------------------------------------------------------------------------

	private static class Impl extends TargetedAnnotationImpl implements Beanp {

		private final Class<?> type;
		private final Class<?>[] params, dictionary;
		private final String name, value, properties, format, ro, wo;

		Impl(Builder b) {
			super(b);
			this.dictionary = copyOf(b.dictionary);
			this.format = b.format;
			this.name = b.name;
			this.params = copyOf(b.params);
			this.properties = b.properties;
			this.ro = b.ro;
			this.type = b.type;
			this.value = b.value;
			this.wo = b.wo;
			postConstruct();
		}

		@Override /* Beanp */
		public Class<?>[] dictionary() {
			return dictionary;
		}

		@Override /* Beanp */
		public String format() {
			return format;
		}

		@Override /* Beanp */
		public String name() {
			return name;
		}

		@Override /* Beanp */
		public Class<?>[] params() {
			return params;
		}

		@Override /* Beanp */
		public String properties() {
			return properties;
		}

		@Override /* Beanp */
		public String ro() {
			return ro;
		}

		@Override /* Beanp */
		public Class<?> type() {
			return type;
		}

		@Override /* Beanp */
		public String value() {
			return value;
		}

		@Override /* Beanp */
		public String wo() {
			return wo;
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Appliers
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Applies targeted {@link Beanp} annotations to a {@link org.apache.juneau.BeanContext.Builder}.
	 */
	public static class Applier extends AnnotationApplier<Beanp,BeanContext.Builder> {

		/**
		 * Constructor.
		 *
		 * @param vr The resolver for resolving values in annotations.
		 */
		public Applier(VarResolverSession vr) {
			super(Beanp.class, BeanContext.Builder.class, vr);
		}

		@Override
		public void apply(AnnotationInfo<Beanp> ai, BeanContext.Builder b) {
			Beanp a = ai.inner();
			if (isEmptyArray(a.on()))
				return;
			b.annotations(copy(a, vr()));
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * A collection of {@link Beanp @Beanp annotations}.
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
		Beanp[] value();
	}
}
