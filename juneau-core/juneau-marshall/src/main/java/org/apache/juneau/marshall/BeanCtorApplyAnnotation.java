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
package org.apache.juneau.marshall;

import static org.apache.juneau.commons.utils.CollectionUtils.*;

import java.lang.annotation.*;
import java.lang.reflect.*;

import org.apache.juneau.commons.*;
import org.apache.juneau.commons.bean.*;
import org.apache.juneau.commons.reflect.*;
import org.apache.juneau.commons.svl.*;

/**
 * Utility classes and methods for the {@link BeanCtorApply @BeanCtorApply} annotation.
 *
 */
public class BeanCtorApplyAnnotation {

	/**
	 * Prevents instantiation.
	 */
	private BeanCtorApplyAnnotation() {}

	/**
	 * Applies targeted {@link BeanCtorApply} annotations to a {@link Context.Builder}.
	 *
	 * <p>
	 * Passes the {@link BeanCtorApply @BeanCtorApply} annotation through to the builder's annotation list.
	 * The {@link org.apache.juneau.commons.reflect.AnnotationProvider.Builder#addRuntimeAnnotations(java.util.List)}
	 * method handles unwrapping the nested {@link BeanCtor @BeanCtor} from {@link BeanCtorApply#value()} and registering it
	 * under the targets specified by {@link BeanCtorApply#on()} and {@link BeanCtorApply#onClass()}.
	 */
	@SuppressWarnings({
		"rawtypes" // Raw types required for reflective annotation application.
	})
	public static class Applier extends AnnotationApplier<BeanCtorApply,Context.Builder> {

		/**
		 * Constructor.
		 *
		 * @param vr The resolver for resolving values in annotations.
		 */
		public Applier(VarResolverSession vr) {
			super(BeanCtorApply.class, Context.Builder.class, vr);
		}

		@Override
		public void apply(AnnotationInfo<BeanCtorApply> ai, Context.Builder b) {
			BeanCtorApply a = ai.inner();
			if (isEmptyArray(a.on()) && isEmptyArray(a.onClass()))
				return;
			b.annotations(a);
		}
	}

	/**
	 * Builder class.
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link MarshallingContext.Builder#annotations(Annotation...)}
	 * </ul>
	 */
	public static class Builder extends AppliedAnnotationObject.BuilderTMF {

		BeanCtor value = BeanCtorAnnotation.DEFAULT;

		/**
		 * Constructor.
		 */
		protected Builder() {
			super(BeanCtorApply.class);
		}

		/**
		 * Sets the {@link BeanCtorApply#value()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder value(BeanCtor value) {
			this.value = value;
			return this;
		}

		@Override /* Overridden from AppliedAnnotationObject.Builder */
		public Builder on(String...value) {
			super.on(value);
			return this;
		}

		@Override /* Overridden from AppliedAnnotationObject.BuilderT */
		public Builder on(Class<?>...value) {
			super.on(value);
			return this;
		}

		@Override /* Overridden from AppliedOnClassAnnotationObject.Builder */
		public Builder onClass(Class<?>...value) {
			super.onClass(value);
			return this;
		}

		@Override /* Overridden from AppliedAnnotationObject.BuilderM */
		public Builder on(Method...value) {
			super.on(value);
			return this;
		}

		@Override /* Overridden from AppliedAnnotationObject.BuilderMF */
		public Builder on(Field...value) {
			super.on(value);
			return this;
		}

		@Override /* Overridden from AppliedAnnotationObject.BuilderT */
		public Builder on(ClassInfo...value) {
			super.on(value);
			return this;
		}

		@Override /* Overridden from AppliedAnnotationObject.BuilderT */
		public Builder onClass(ClassInfo...value) {
			super.onClass(value);
			return this;
		}

		@Override /* Overridden from AppliedAnnotationObject.BuilderTMF */
		public Builder on(FieldInfo...value) {
			super.on(value);
			return this;
		}

		@Override /* Overridden from AppliedAnnotationObject.BuilderTMF */
		public Builder on(MethodInfo...value) {
			super.on(value);
			return this;
		}

		/**
		 * Instantiates a new {@link BeanCtorApply @BeanCtorApply} object initialized with this builder.
		 *
		 * @return A new {@link BeanCtorApply} object.
		 */
		public BeanCtorApply build() {
			return new Object(this);
		}
	}

	@SuppressWarnings({
		"java:S2160" // equals() inherited from AnnotationObject compares all annotation interface methods; subclass fields are accessed via those methods
	})
	private static class Object extends AppliedOnClassAnnotationObject implements BeanCtorApply {

		private final BeanCtor value;

		Object(BeanCtorApplyAnnotation.Builder b) {
			super(b);
			value = b.value;
		}

		@Override /* Overridden from BeanCtorApply */
		public BeanCtor value() {
			return value;
		}
	}

	/** Default value */
	public static final BeanCtorApply DEFAULT = create().build();

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
	 * Returns <jk>true</jk> if the specified annotation contains all default values.
	 *
	 * @param a The annotation to check.
	 * @return <jk>true</jk> if the specified annotation contains all default values.
	 */
	public static boolean empty(BeanCtorApply a) {
		return a == null || DEFAULT.equals(a);
	}
}
