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

/**
 * Builder class for the {@link ExternalDocs} annotation.
 *
 * <ul class='seealso'>
 * 	<li class='jm'>{@link BeanContextBuilder#annotations(Annotation...)}
 * </ul>
 */
public class ExternalDocsBuilder extends AnnotationBuilder {

	/** Default value */
	public static final ExternalDocs DEFAULT = create().build();

	/**
	 * Instantiates a new builder for this class.
	 *
	 * @return A new builder object.
	 */
	public static ExternalDocsBuilder create() {
		return new ExternalDocsBuilder();
	}

	private static class Impl extends AnnotationImpl implements ExternalDocs {

		private final String url;
		private final String[] description, value;

		Impl(ExternalDocsBuilder b) {
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


	String url="";
	String[] description={}, value={};

	/**
	 * Constructor.
	 */
	public ExternalDocsBuilder() {
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
	public ExternalDocsBuilder description(String...value) {
		this.description = value;
		return this;
	}

	/**
	 * Sets the {@link ExternalDocs#url} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ExternalDocsBuilder url(String value) {
		this.url = value;
		return this;
	}

	/**
	 * Sets the {@link ExternalDocs#value} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ExternalDocsBuilder value(String...value) {
		this.value = value;
		return this;
	}

	// <FluentSetters>
	// </FluentSetters>
}
