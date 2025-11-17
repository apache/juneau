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
package org.apache.juneau.http.annotation;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;
import static org.apache.juneau.Constants.*;
import static org.apache.juneau.common.utils.CollectionUtils.*;
import static org.apache.juneau.common.utils.Utils.*;

import java.lang.annotation.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.common.collections.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.common.reflect.*;
import org.apache.juneau.svl.*;

/**
 * Utility classes and methods for the {@link Path @Path} annotation.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 */
public class PathAnnotation {
	/**
	 * Applies targeted {@link Path} annotations to a {@link org.apache.juneau.BeanContext.Builder}.
	 */
	public static class Applier extends AnnotationApplier<Path,BeanContext.Builder> {

		/**
		 * Constructor.
		 *
		 * @param vr The resolver for resolving values in annotations.
		 */
		public Applier(VarResolverSession vr) {
			super(Path.class, BeanContext.Builder.class, vr);
		}

		@Override
		public void apply(AnnotationInfo<Path> ai, BeanContext.Builder b) {
			Path a = ai.inner();
			if (isEmptyArray(a.on()) && isEmptyArray(a.onClass()))
				return;
			b.annotations(a);
		}
	}

	/**
	 * A collection of {@link Path @Path annotations}.
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
		Path[] value();
	}

	/**
	 * Builder class.
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.BeanContext.Builder#annotations(Annotation...)}
	 * </ul>
	 */
	public static class Builder extends TargetedAnnotationTMFBuilder<Builder> {

		Class<? extends HttpPartParser> parser = HttpPartParser.Void.class;
		Class<? extends HttpPartSerializer> serializer = HttpPartSerializer.Void.class;
		Schema schema = SchemaAnnotation.DEFAULT;
		String name = "", value = "", def = NONE;

		/**
		 * Constructor.
		 */
		protected Builder() {
			super(Path.class);
		}

		/**
		 * Instantiates a new {@link Path @Path} object initialized with this builder.
		 *
		 * @return A new {@link Path @Path} object.
		 */
		public Path build() {
			return new Impl(this);
		}

		/**
		 * Sets the {@link Path#name} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder def(String value) {
			this.def = value;
			return this;
		}

		/**
		 * Sets the {@link Path#name} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder name(String value) {
			this.name = value;
			return this;
		}

		/**
		 * Sets the {@link Path#parser} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder parser(Class<? extends HttpPartParser> value) {
			this.parser = value;
			return this;
		}

		/**
		 * Sets the {@link Path#schema} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder schema(Schema value) {
			this.schema = value;
			return this;
		}

		/**
		 * Sets the {@link Path#serializer} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder serializer(Class<? extends HttpPartSerializer> value) {
			this.serializer = value;
			return this;
		}

		/**
		 * Sets the {@link Path#value} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder value(String value) {
			this.value = value;
			return this;
		}

	}

	private static class Impl extends TargetedAnnotationTImpl implements Path {

		private final Class<? extends HttpPartParser> parser;
		private final Class<? extends HttpPartSerializer> serializer;
		private final String name, value, def;
		private final Schema schema;

		Impl(Builder b) {
			super(b);
			this.def = b.def;
			this.name = b.name;
			this.parser = b.parser;
			this.schema = b.schema;
			this.serializer = b.serializer;
			this.value = b.value;
			postConstruct();
		}

		@Override /* Overridden from Path */
		public String def() {
			return def;
		}

		@Override /* Overridden from Path */
		public String name() {
			return name;
		}

		@Override /* Overridden from Path */
		public Class<? extends HttpPartParser> parser() {
			return parser;
		}

		@Override /* Overridden from Path */
		public Schema schema() {
			return schema;
		}

		@Override /* Overridden from Path */
		public Class<? extends HttpPartSerializer> serializer() {
			return serializer;
		}

		@Override /* Overridden from Path */
		public String value() {
			return value;
		}
	}

	/** Default value */
	public static final Path DEFAULT = create().build();

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
	public static boolean empty(Path a) {
		return a == null || DEFAULT.equals(a);
	}

	/**
	 * Finds the default value from the specified list of annotations.
	 *
	 * @param pi The parameter.
	 * @return The last matching default value, or {@link Value#empty()} if not found.
	 */
	public static Value<String> findDef(ParameterInfo pi) {
		Value<String> n = Value.empty();
		rstream(pi.getAllAnnotations(Path.class)).map(AnnotationInfo::inner).filter(x -> isNotEmpty(x.def()) && ne(NONE, x.def())).forEach(x -> n.set(x.def()));
		return n;
	}

	/**
	 * Finds the name from the specified lists of annotations.
	 *
	 * <p>
	 * The last matching name found is returned.
	 *
	 * @param pi The parameter.
	 * @return The last matching name, or {@link Value#empty()} if not found.
	 */
	public static Value<String> findName(ParameterInfo pi) {
		Value<String> n = Value.empty();
		rstream(pi.getAllAnnotations(Path.class)).map(AnnotationInfo::inner).filter(x -> isNotEmpty(x.value())).forEach(x -> n.set(x.value()));
		rstream(pi.getAllAnnotations(Path.class)).map(AnnotationInfo::inner).filter(x -> isNotEmpty(x.name())).forEach(x -> n.set(x.name()));
		return n;
	}
}