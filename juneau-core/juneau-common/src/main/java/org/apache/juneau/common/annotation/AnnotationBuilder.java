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
package org.apache.juneau.common.annotation;

import java.lang.annotation.*;

/**
 * Builder for {@link AnnotationImpl} objects.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 *
 * @param <B> The actual builder class.
 */
public class AnnotationBuilder<B extends AnnotationBuilder<B>> {

	Class<? extends Annotation> annotationType;
	String[] description = {};

	/**
	 * Constructor.
	 *
	 * @param annotationType The annotation type of the annotation implementation class.
	 */
	public AnnotationBuilder(Class<? extends Annotation> annotationType) {
		this.annotationType = annotationType;
	}

	/**
	 * Sets the {@link AnnotationImpl#description()} property on the target annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 * @since 9.2.0
	 */
	public B description(final String...value) {
		this.description = value;
		return asThis();
	}

	/**
	 * Returns this instance typed as {@code B}.
	 *
	 * @return this instance typed as {@code B}.
	 * @since 9.2.0
	 */
	@SuppressWarnings("unchecked")
	protected B asThis() {
		return (B)this;
	}

	/**
	 * Returns the annotation type being built.
	 *
	 * @return The annotation type being built.
	 */
	public Class<? extends Annotation> getAnnotationType() { return annotationType; }

	/**
	 * Returns the description of this annotation builder.
	 *
	 * @return The description array, or <jk>null</jk> if not set.
	 */
	public String[] getDescription() { return description; }
}