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
import static org.apache.juneau.BeanContext.*;

import java.lang.annotation.*;
import java.lang.reflect.*;

import org.apache.juneau.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.svl.*;

/**
 * Utility classes and methods for the {@link BeanIgnore @BeanIgnore} annotation.
 */
public class BeanIgnoreAnnotation {

	/** Default value */
	public static final BeanIgnore DEFAULT = create().build();

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
	public static BeanIgnore copy(BeanIgnore a, VarResolverSession r) {
		return
			create()
			.on(r.resolve(a.on()))
			.onClass(a.onClass())
			.build();
	}

	/**
	 * Builder class for the {@link BeanIgnore} annotation.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jm'>{@link BeanContextBuilder#annotations(Annotation...)}
	 * </ul>
	 */
	public static class Builder extends TargetedAnnotationTMFCBuilder {

		/**
		 * Constructor.
		 */
		public Builder() {
			super(BeanIgnore.class);
		}

		/**
		 * Instantiates a new {@link BeanIgnore @BeanIgnore} object initialized with this builder.
		 *
		 * @return A new {@link BeanIgnore @BeanIgnore} object.
		 */
		public BeanIgnore build() {
			return new Impl(this);
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

		@Override /* GENERATED - TargetedAnnotationTMFBuilder */
		public Builder on(Field...value) {
			super.on(value);
			return this;
		}

		@Override /* GENERATED - TargetedAnnotationTMFBuilder */
		public Builder on(Method...value) {
			super.on(value);
			return this;
		}

		@Override /* GENERATED - TargetedAnnotationTMFCBuilder */
		public Builder on(java.lang.reflect.Constructor<?>...value) {
			super.on(value);
			return this;
		}

		// </FluentSetters>
	}

	private static class Impl extends TargetedAnnotationTImpl implements BeanIgnore {

		Impl(Builder b) {
			super(b);
			postConstruct();
		}
	}

	/**
	 * Applies targeted {@link BeanIgnore} annotations to a {@link ContextPropertiesBuilder}.
	 */
	public static class Apply extends ConfigApply<BeanIgnore> {

		/**
		 * Constructor.
		 *
		 * @param c The annotation class.
		 * @param vr The resolver for resolving values in annotations.
		 */
		public Apply(Class<BeanIgnore> c, VarResolverSession vr) {
			super(c, vr);
		}

		@Override
		public void apply(AnnotationInfo<BeanIgnore> ai, ContextPropertiesBuilder cpb, VarResolverSession vr) {
			BeanIgnore a = ai.getAnnotation();

			if (isEmpty(a.on()) && isEmpty(a.onClass()))
				return;

			cpb.prependTo(BEAN_annotations, copy(a, vr));
		}
	}

	/**
	 * A collection of {@link BeanIgnore @BeanIgnore annotations}.
	 */
	@Documented
	@Target({METHOD,TYPE})
	@Retention(RUNTIME)
	@Inherited
	public static @interface Array {

		/**
		 * The child annotations.
		 */
		BeanIgnore[] value();
	}
}
