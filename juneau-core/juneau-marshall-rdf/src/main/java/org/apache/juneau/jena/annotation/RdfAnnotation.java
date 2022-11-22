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
package org.apache.juneau.jena.annotation;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;
import static org.apache.juneau.internal.ArrayUtils.*;

import java.lang.annotation.*;
import java.lang.reflect.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.jena.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.svl.*;

/**
 * Utility classes and methods for the {@link Rdf @Rdf} annotation.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'>{doc jmr.RdfDetails}
 * </ul>
 */
public class RdfAnnotation {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	/** Default value */
	public static final Rdf DEFAULT = create().build();

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
	public static Rdf copy(Rdf a, VarResolverSession r) {
		return
			create()
			.beanUri(r.resolve(a.beanUri()))
			.collectionFormat(a.collectionFormat())
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

		String namespace="", prefix="";
		boolean	beanUri;
		RdfCollectionFormat collectionFormat=RdfCollectionFormat.DEFAULT;

		/**
		 * Constructor.
		 */
		protected Builder() {
			super(Rdf.class);
		}

		/**
		 * Instantiates a new {@link Rdf @Rdf} object initialized with this builder.
		 *
		 * @return A new {@link Rdf @Rdf} object.
		 */
		public Rdf build() {
			return new Impl(this);
		}

		/**
		 * Sets the {@link Rdf#beanUri} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder beanUri(boolean value) {
			this.beanUri = value;
			return this;
		}

		/**
		 * Sets the {@link Rdf#collectionFormat} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder collectionFormat(RdfCollectionFormat value) {
			this.collectionFormat = value;
			return this;
		}

		/**
		 * Sets the {@link Rdf#namespace} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder namespace(String value) {
			this.namespace = value;
			return this;
		}

		/**
		 * Sets the {@link Rdf#prefix} property on this annotation.
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

	private static class Impl extends TargetedAnnotationTImpl implements Rdf {

		private final boolean beanUri;
		private final RdfCollectionFormat collectionFormat;
		private final String namespace, prefix;

		Impl(Builder b) {
			super(b);
			this.beanUri = b.beanUri;
			this.collectionFormat = b.collectionFormat;
			this.namespace = b.namespace;
			this.prefix = b.prefix;
			postConstruct();
		}

		@Override /* Rdf */
		public boolean beanUri() {
			return beanUri;
		}

		@Override /* Rdf */
		public RdfCollectionFormat collectionFormat() {
			return collectionFormat;
		}

		@Override /* Rdf */
		public String namespace() {
			return namespace;
		}

		@Override /* Rdf */
		public String prefix() {
			return prefix;
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Appliers
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Applies targeted {@link Rdf} annotations to a {@link org.apache.juneau.jena.RdfSerializer.Builder}.
	 */
	public static class SerializerApplier extends AnnotationApplier<Rdf,RdfSerializer.Builder> {

		/**
		 * Constructor.
		 *
		 * @param vr The resolver for resolving values in annotations.
		 */
		public SerializerApplier(VarResolverSession vr) {
			super(Rdf.class, RdfSerializer.Builder.class, vr);
		}

		@Override
		public void apply(AnnotationInfo<Rdf> ai, RdfSerializer.Builder b) {
			Rdf a = ai.inner();
			if (isEmptyArray(a.on(), a.onClass()))
				return;
			b.annotations(copy(a, vr()));
		}
	}

	/**
	 * Applies targeted {@link Rdf} annotations to a {@link org.apache.juneau.jena.RdfParser.Builder}.
	 */
	public static class ParserApplier extends AnnotationApplier<Rdf,RdfParser.Builder> {

		/**
		 * Constructor.
		 *
		 * @param vr The resolver for resolving values in annotations.
		 */
		public ParserApplier(VarResolverSession vr) {
			super(Rdf.class, RdfParser.Builder.class, vr);
		}

		@Override
		public void apply(AnnotationInfo<Rdf> ai, RdfParser.Builder b) {
			Rdf a = ai.inner();
			if (isEmptyArray(a.on(), a.onClass()))
				return;
			b.annotations(copy(a, vr()));
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * A collection of {@link Rdf @Rdf annotations}.
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
		Rdf[] value();
	}
}