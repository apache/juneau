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
package org.apache.juneau.marshall.soap;

import static org.apache.juneau.commons.utils.CollectionUtils.*;

import java.lang.reflect.*;

import org.apache.juneau.commons.*;
import org.apache.juneau.commons.reflect.*;
import org.apache.juneau.commons.svl.*;
import org.apache.juneau.marshall.*;

/**
 * Utility classes and methods for the {@link SoapXmlApply @SoapXmlApply} annotation.
 *
 */
public class SoapXmlApplyAnnotation {

	/**
	 * Prevents instantiation.
	 */
	private SoapXmlApplyAnnotation() {}

	/**
	 * Applies targeted {@link SoapXmlApply} annotations to a {@link org.apache.juneau.marshall.Context.Builder}.
	 */
	@SuppressWarnings({
		"rawtypes" // Raw types required for reflective annotation application.
	})
	public static class Applier extends AnnotationApplier<SoapXmlApply,Context.Builder> {

		/**
		 * Constructor.
		 *
		 * @param vr The resolver for resolving values in annotations.
		 * 	<br>Must not be <jk>null</jk>.
		 */
		public Applier(VarResolverSession vr) {
			super(SoapXmlApply.class, Context.Builder.class, vr);
		}

		@Override
		public void apply(AnnotationInfo<SoapXmlApply> ai, Context.Builder b) {
			SoapXmlApply a = ai.inner();
			if (isEmptyArray(a.on()) && isEmptyArray(a.onClass()))
				return;
			b.annotations(a);
		}
	}

	/**
	 * Builder class.
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.marshall.MarshallingContext.Builder#annotations(java.lang.annotation.Annotation...)}
	 * </ul>
	 */
	public static class Builder extends AppliedAnnotationObject.BuilderTMF {

		SoapXml value = SoapXmlAnnotation.DEFAULT;

		/**
		 * Constructor.
		 */
		protected Builder() {
			super(SoapXmlApply.class);
		}

		/**
		 * Sets the {@link SoapXmlApply#value()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder value(SoapXml value) {
			this.value = value;
			return this;
		}

		@Override public Builder on(String...value) { super.on(value); return this; }
		@Override public Builder on(Class<?>...value) { super.on(value); return this; }
		@Override public Builder onClass(Class<?>...value) { super.onClass(value); return this; }
		@Override public Builder on(Method...value) { super.on(value); return this; }
		@Override public Builder on(Field...value) { super.on(value); return this; }
		@Override public Builder on(ClassInfo...value) { super.on(value); return this; }
		@Override public Builder onClass(ClassInfo...value) { super.onClass(value); return this; }
		@Override public Builder on(FieldInfo...value) { super.on(value); return this; }
		@Override public Builder on(MethodInfo...value) { super.on(value); return this; }

		/**
		 * Instantiates a new {@link SoapXmlApply @SoapXmlApply} object initialized with this builder.
		 *
		 * @return A new {@link SoapXmlApply} object.
		 */
		public SoapXmlApply build() {
			return new Object(this);
		}
	}

	@SuppressWarnings({
		"java:S2160" // equals() inherited from AnnotationObject compares all annotation interface methods; subclass fields are accessed via those methods
	})
	private static class Object extends AppliedOnClassAnnotationObject implements SoapXmlApply {

		private final SoapXml value;

		Object(SoapXmlApplyAnnotation.Builder b) {
			super(b);
			value = b.value;
		}

		@Override public SoapXml value() { return value; }
	}

	/** Default value */
	public static final SoapXmlApply DEFAULT = create().build();

	/**
	 * Instantiates a new builder for this class.
	 *
	 * @return A new builder object.
	 */
	public static Builder create() { return new Builder(); }

	/**
	 * Instantiates a new builder for this class.
	 *
	 * @param on The targets this annotation applies to.
	 * @return A new builder object.
	 */
	public static Builder create(Class<?>...on) { return create().on(on); }

	/**
	 * Instantiates a new builder for this class.
	 *
	 * @param on The targets this annotation applies to.
	 * @return A new builder object.
	 */
	public static Builder create(String...on) { return create().on(on); }

	/**
	 * Returns <jk>true</jk> if the specified annotation contains all default values.
	 *
	 * @param a The annotation to check.
	 * 	<br>Can be <jk>null</jk> (returns <jk>true</jk>).
	 * @return <jk>true</jk> if the specified annotation contains all default values.
	 */
	public static boolean empty(SoapXmlApply a) {
		return a == null || DEFAULT.equals(a);
	}
}
