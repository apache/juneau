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
package org.apache.juneau.jsonschema.annotation;

import static org.apache.juneau.internal.ArrayUtils.*;

import java.lang.annotation.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.svl.*;

/**
 * Utility classes and methods for the {@link ExternalDocs @ExternalDocs} annotation.
 */
public class ExternalDocsAnnotation {

	/** Default value */
	public static final ExternalDocs DEFAULT = create().build();

	/**
	 * Instantiates a new builder for this class.
	 *
	 * @return A new builder object.
	 */
	public static Builder create() {
		return new Builder();
	}

	/**
	 * Creates a copy of the specified annotation.
	 *
	 * @param a The annotation to copy.
	 * @param r The var resolver for resolving any variables.
	 * @return A copy of the specified annotation.
	 */
	public static ExternalDocs copy(ExternalDocs a, VarResolverSession r) {
		return
			create()
			.description(r.resolve(a.description()))
			.url(r.resolve(a.url()))
			.value(r.resolve(a.value()))
			.build();
	}

	/**
	 * Returns <jk>true</jk> if the specified annotation contains all default values.
	 *
	 * @param a The annotation to check.
	 * @return <jk>true</jk> if the specified annotation contains all default values.
	 */
	public static boolean empty(ExternalDocs a) {
		return a == null || DEFAULT.equals(a);
	}

	/**
	 * Builder class for the {@link ExternalDocs} annotation.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jm'>{@link BeanContextBuilder#annotations(Annotation...)}
	 * </ul>
	 */
	public static class Builder extends AnnotationBuilder {

		String url="";
		String[] description={}, value={};

		/**
		 * Constructor.
		 */
		public Builder() {
			super(ExternalDocs.class);
		}

		/**
		 * Instantiates a new {@link ExternalDocs @ExternalDocs} object initialized with this builder.
		 *
		 * @return A new {@link ExternalDocs @ExternalDocs} object.
		 */
		public ExternalDocs build() {
			return new Impl(this);
		}

		/**
		 * Sets the {@link ExternalDocs#description} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder description(String...value) {
			this.description = value;
			return this;
		}

		/**
		 * Sets the {@link ExternalDocs#url} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder url(String value) {
			this.url = value;
			return this;
		}

		/**
		 * Sets the {@link ExternalDocs#value} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder value(String...value) {
			this.value = value;
			return this;
		}

		// <FluentSetters>
		// </FluentSetters>
	}

	private static class Impl extends AnnotationImpl implements ExternalDocs {

		private final String url;
		private final String[] description, value;

		Impl(Builder b) {
			super(b);
			this.description = copyOf(b.description);
			this.url = b.url;
			this.value = copyOf(b.value);
			postConstruct();
		}

		@Override
		public String[] description() {
			return description;
		}

		@Override
		public String url() {
			return url;
		}

		@Override
		public String[] value() {
			return value;
		}
	}
}