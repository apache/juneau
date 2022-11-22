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
package org.apache.juneau.xml.annotation;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;
import static org.apache.juneau.internal.ArrayUtils.*;

import java.lang.annotation.*;
import java.lang.reflect.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.svl.*;

/**
 * Utility classes and methods for the {@link Xml @Xml} annotation.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jm.XmlDetails">XML Details</a>
 * </ul>
 */
public class XmlAnnotation {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	/** Default value */
	public static final Xml DEFAULT = create().build();

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
	 * @param a The annotation to copy.s
	 * @param r The var resolver for resolving any variables.
	 * @return A copy of the specified annotation.
	 */
	public static Xml copy(Xml a, VarResolverSession r) {
		return
			create()
			.childName(r.resolve(a.childName()))
			.format(a.format())
			.namespace(r.resolve(a.namespace()))
			.on(r.resolve(a.on()))
			.onClass(a.onClass())
			.prefix(r.resolve(a.prefix()))
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
	public static class Builder extends TargetedAnnotationTMFBuilder {

		String childName="", namespace="", prefix="";
		XmlFormat format=XmlFormat.DEFAULT;

		/**
		 * Constructor.
		 */
		protected Builder() {
			super(Xml.class);
		}

		/**
		 * Instantiates a new {@link Xml @Xml} object initialized with this builder.
		 *
		 * @return A new {@link Xml @Xml} object.
		 */
		public Xml build() {
			return new Impl(this);
		}

		/**
		 * Sets the {@link Xml#childName} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder childName(String value) {
			this.childName = value;
			return this;
		}

		/**
		 * Sets the {@link Xml#format} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder format(XmlFormat value) {
			this.format = value;
			return this;
		}

		/**
		 * Sets the {@link Xml#namespace} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder namespace(String value) {
			this.namespace = value;
			return this;
		}

		/**
		 * Sets the {@link Xml#prefix} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder prefix(String value) {
			this.prefix = value;
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

		// </FluentSetters>
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Implementation
	//-----------------------------------------------------------------------------------------------------------------

	private static class Impl extends TargetedAnnotationTImpl implements Xml {

		private final String childName, namespace, prefix;
		private final XmlFormat format;

		Impl(Builder b) {
			super(b);
			this.childName = b.childName;
			this.format = b.format;
			this.namespace = b.namespace;
			this.prefix = b.prefix;
			postConstruct();
		}

		@Override /* Xml */
		public String childName() {
			return childName;
		}

		@Override /* Xml */
		public XmlFormat format() {
			return format;
		}

		@Override /* Xml */
		public String namespace() {
			return namespace;
		}

		@Override /* Xml */
		public String prefix() {
			return prefix;
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Appliers
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Applies targeted {@link Xml} annotations to a {@link org.apache.juneau.Context.Builder}.
	 */
	public static class Apply extends AnnotationApplier<Xml,Context.Builder> {

		/**
		 * Constructor.
		 *
		 * @param vr The resolver for resolving values in annotations.
		 */
		public Apply(VarResolverSession vr) {
			super(Xml.class, Context.Builder.class, vr);
		}

		@Override
		public void apply(AnnotationInfo<Xml> ai, Context.Builder b) {
			Xml a = ai.inner();
			if (isEmptyArray(a.on(), a.onClass()))
				return;
			b.annotations(copy(a, vr()));
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * A collection of {@link Xml @Xml annotations}.
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
		Xml[] value();
	}
}