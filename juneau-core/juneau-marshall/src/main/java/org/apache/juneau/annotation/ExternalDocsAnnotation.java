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

import static org.apache.juneau.internal.ArrayUtils.*;
import static org.apache.juneau.jsonschema.SchemaUtils.*;

import java.lang.annotation.*;
import java.util.function.*;

import org.apache.juneau.collections.*;
import org.apache.juneau.common.internal.*;
import org.apache.juneau.parser.*;

/**
 * Utility classes and methods for the {@link ExternalDocs @ExternalDocs} annotation.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 */
public class ExternalDocsAnnotation {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

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
	 * Returns <jk>true</jk> if the specified annotation contains all default values.
	 *
	 * @param a The annotation to check.
	 * @return <jk>true</jk> if the specified annotation contains all default values.
	 */
	public static boolean empty(ExternalDocs a) {
		return a == null || DEFAULT.equals(a);
	}

	/**
	 * Merges the contents of the specified annotation into the specified generic map.
	 *
	 * @param m The map to copy the contents to.
	 * @param a The annotation to apply.
	 * @return The same map with the annotation contents applied.
	 * @throws ParseException Invalid JSON found in value.
	 */
	public static JsonMap merge(JsonMap m, ExternalDocs a) throws ParseException {
		if (ExternalDocsAnnotation.empty(a))
			return m;
		Predicate<String> ne = StringUtils::isNotEmpty;
		return m
			.appendIf(ne, "description", joinnl(a.description()))
			.appendIf(ne, "url", a.url())
		;
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
	public static class Builder extends AnnotationBuilder {

		String url="";
		String[] description={};

		/**
		 * Constructor.
		 */
		protected Builder() {
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
		 * @return This object.
		 */
		public Builder description(String...value) {
			this.description = value;
			return this;
		}

		/**
		 * Sets the {@link ExternalDocs#url} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder url(String value) {
			this.url = value;
			return this;
		}

		// <FluentSetters>
		// </FluentSetters>
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Implementation
	//-----------------------------------------------------------------------------------------------------------------

	private static class Impl extends AnnotationImpl implements ExternalDocs {

		private final String url;
		private final String[] description;

		Impl(Builder b) {
			super(b);
			this.description = copyOf(b.description);
			this.url = b.url;
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
	}
}