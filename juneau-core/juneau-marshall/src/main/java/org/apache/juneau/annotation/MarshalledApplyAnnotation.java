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
package org.apache.juneau.annotation;

import static org.apache.juneau.commons.utils.CollectionUtils.*;

import java.lang.annotation.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.annotation.*;
import org.apache.juneau.commons.reflect.*;
import org.apache.juneau.svl.*;

/**
 * Utility classes and methods for the {@link MarshalledApply @MarshalledApply} annotation.
 *
 */
public class MarshalledApplyAnnotation {

	/**
	 * Prevents instantiation.
	 */
	private MarshalledApplyAnnotation() {}

	/**
	 * Applies targeted {@link MarshalledApply} annotations to a {@link org.apache.juneau.Context.Builder}.
	 *
	 * <p>
	 * Passes the {@link MarshalledApply @MarshalledApply} annotation through to the builder's annotation list.
	 * The {@link org.apache.juneau.commons.reflect.AnnotationProvider.Builder#addRuntimeAnnotations(java.util.List)}
	 * method handles unwrapping the nested {@link Marshalled @Marshalled} from {@link MarshalledApply#value()} and registering it
	 * under the targets specified by {@link MarshalledApply#on()} and {@link MarshalledApply#onClass()}.
	 */
	public static class Applier extends AnnotationApplier<MarshalledApply,Context.Builder> {

		/**
		 * Constructor.
		 *
		 * @param vr The resolver for resolving values in annotations.
		 */
		public Applier(VarResolverSession vr) {
			super(MarshalledApply.class, Context.Builder.class, vr);
		}

		@Override
		public void apply(AnnotationInfo<MarshalledApply> ai, Context.Builder b) {
			MarshalledApply a = ai.inner();
			if (isEmptyArray(a.on()) && isEmptyArray(a.onClass()))
				return;
			b.annotations(a);
		}
	}

	/**
	 * Builder class.
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.BeanContext.Builder#annotations(Annotation...)}
	 * </ul>
	 */
	public static class Builder extends AppliedAnnotationObject.BuilderT {

		Marshalled value = MarshalledAnnotation.DEFAULT;

		/**
		 * Constructor.
		 */
		protected Builder() {
			super(MarshalledApply.class);
		}

		/**
		 * Sets the {@link MarshalledApply#value()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder value(Marshalled value) {
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

		/**
		 * Instantiates a new {@link MarshalledApply @MarshalledApply} object initialized with this builder.
		 *
		 * @return A new {@link MarshalledApply} object.
		 */
		public MarshalledApply build() {
			return new Object(this);
		}
	}

	@SuppressWarnings({
		"java:S2160" // equals() inherited from AnnotationObject compares all annotation interface methods; subclass fields are accessed via those methods
	})
	private static class Object extends AppliedOnClassAnnotationObject implements MarshalledApply {

		private final Marshalled value;

		Object(MarshalledApplyAnnotation.Builder b) {
			super(b);
			value = b.value;
		}

		@Override /* Overridden from MarshalledApply */
		public Marshalled value() {
			return value;
		}

		@Override /* Overridden from MarshalledApply */
		public String[] on() {
			return super.on();
		}

		@Override /* Overridden from MarshalledApply */
		public Class<?>[] onClass() {
			return super.onClass();
		}
	}

	/** Default value */
	public static final MarshalledApply DEFAULT = create().build();

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
	public static boolean empty(MarshalledApply a) {
		return a == null || DEFAULT.equals(a);
	}
}
