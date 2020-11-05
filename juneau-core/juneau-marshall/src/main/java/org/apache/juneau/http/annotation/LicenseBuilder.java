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
package org.apache.juneau.http.annotation;

import static org.apache.juneau.internal.ArrayUtils.*;

import java.lang.annotation.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;

/**
 * Builder class for the {@link License} annotation.
 *
 * <ul class='seealso'>
 * 	<li class='jm'>{@link BeanContextBuilder#annotations(Annotation...)}
 * </ul>
 */
public class LicenseBuilder extends AnnotationBuilder {

	/** Default value */
	public static final License DEFAULT = create().build();

	/**
	 * Instantiates a new builder for this class.
	 *
	 * @return A new builder object.
	 */
	public static LicenseBuilder create() {
		return new LicenseBuilder();
	}

	private static class Impl extends AnnotationImpl implements License {

		private final String name, url;
		private final String[] value;

		Impl(LicenseBuilder b) {
			super(b);
			this.name = b.name;
			this.url = b.url;
			this.value = copyOf(b.value);
			postConstruct();
		}

		@Override /* License */
		public String name() {
			return name;
		}

		@Override /* License */
		public String url() {
			return url;
		}

		@Override /* License */
		public String[] value() {
			return value;
		}
	}


	String name="", url="";
	String[] value={};

	/**
	 * Constructor.
	 */
	public LicenseBuilder() {
		super(License.class);
	}

	/**
	 * Instantiates a new {@link License @License} object initialized with this builder.
	 *
	 * @return A new {@link License @License} object.
	 */
	public License build() {
		return new Impl(this);
	}


	/**
	 * Sets the {@link License#name} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public LicenseBuilder name(String value) {
		this.name = value;
		return this;
	}

	/**
	 * Sets the {@link License#url} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public LicenseBuilder url(String value) {
		this.url = value;
		return this;
	}

	/**
	 * Sets the {@link License#value} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public LicenseBuilder value(String...value) {
		this.value = value;
		return this;
	}

	// <FluentSetters>

	// </FluentSetters>
}
