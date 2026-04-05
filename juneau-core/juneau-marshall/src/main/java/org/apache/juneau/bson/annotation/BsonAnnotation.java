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
package org.apache.juneau.bson.annotation;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;
import static org.apache.juneau.commons.utils.CollectionUtils.*;

import java.lang.annotation.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.annotation.*;
import org.apache.juneau.commons.reflect.*;
import org.apache.juneau.svl.*;

/**
 * Utility classes and methods for the {@link Bson @Bson} annotation.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/BsonBasics">BSON Basics</a>
 * </ul>
 */
public class BsonAnnotation {

	/**
	 * Prevents instantiation.
	 */
	private BsonAnnotation() {}

	/**
	 * Applies targeted {@link Bson} annotations to a {@link Context.Builder}.
	 */
	public static class Apply extends AnnotationApplier<Bson,Context.Builder> {

		/**
		 * Constructor.
		 *
		 * @param vr The resolver for resolving values in annotations.
		 */
		public Apply(VarResolverSession vr) {
			super(Bson.class, Context.Builder.class, vr);
		}

		@Override
		public void apply(AnnotationInfo<Bson> ai, Context.Builder b) {
			Bson a = ai.inner();
			if (isEmptyArray(a.on()) && isEmptyArray(a.onClass()))
				return;
			b.annotations(copy(a, vr()));
		}
	}

	/**
	 * A collection of {@link Bson @Bson} annotations.
	 */
	@Documented
	@Target({ METHOD, TYPE })
	@Retention(RUNTIME)
	@Inherited
	public static @interface Array {

		/**
		 * The child annotations.
		 *
		 * @return The annotation value.
		 */
		Bson[] value();
	}

	/**
	 * Creates a copy of the specified annotation.
	 *
	 * @param a The annotation to copy.
	 * @param r The var resolver for resolving any variables.
	 * @return A copy of the specified annotation.
	 */
	public static Bson copy(Bson a, VarResolverSession r) {
		var b = BsonAnnotation.create().on(r.resolve(a.on()));
		if (a.onClass().length > 0)
			b = b.onClass(a.onClass());
		if (a.description().length > 0)
			b = b.description(a.description());
		return b.build();
	}

	/**
	 * Instantiates a new builder for this class.
	 *
	 * @return A new builder object.
	 */
	public static BsonAnnotation.Builder create() {
		return new BsonAnnotation.Builder();
	}

	/**
	 * Builder class.
	 */
	public static class Builder extends AppliedAnnotationObject.BuilderTMF {

		private String[] description = {};

		/**
		 * Constructor.
		 */
		protected Builder() {
			super(Bson.class);
		}

		/**
		 * Instantiates a new {@link Bson @Bson} object initialized with this builder.
		 *
		 * @return A new {@link Bson @Bson} object.
		 */
		public Bson build() {
			return new BsonAnnotation.Object(this);
		}

		/**
		 * Sets the description property.
		 *
		 * @param value The new value.
		 * @return This object.
		 */
		public Builder description(String...value) {
			description = value;
			return this;
		}

		@Override /* AppliedAnnotationObject.Builder */
		public Builder on(String...value) {
			super.on(value);
			return this;
		}

		@Override /* AppliedAnnotationObject.BuilderT */
		public Builder onClass(Class<?>...value) {
			super.onClass(value);
			return this;
		}
	}

	@SuppressWarnings({
		"java:S2160" // equals() inherited from AnnotationObject compares all annotation interface methods; subclass fields are accessed via those methods
	})
	private static class Object extends AppliedOnClassAnnotationObject implements Bson {

		private final String[] description;

		Object(BsonAnnotation.Builder b) {
			super(b);
			description = copyOf(b.description);
		}

		@Override /* Bson */
		public String[] description() {
			return description;
		}
	}
}
