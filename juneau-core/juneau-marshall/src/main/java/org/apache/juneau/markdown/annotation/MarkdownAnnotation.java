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
package org.apache.juneau.markdown.annotation;

import java.lang.annotation.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.reflect.*;
import org.apache.juneau.svl.*;

/**
 * Utility classes and methods for the {@link Markdown @Markdown} annotation.
 *
 */
public class MarkdownAnnotation {

	/**
	 * Applies {@link Markdown} annotations to a {@link org.apache.juneau.Context.Builder}.
	 */
	public static class Apply extends AnnotationApplier<Markdown,Context.Builder> {

		/**
		 * Constructor.
		 *
		 * @param vr The resolver for resolving values in annotations.
		 */
		public Apply(VarResolverSession vr) {
			super(Markdown.class, Context.Builder.class, vr);
		}

		@Override /* AnnotationApplier */
		public void apply(AnnotationInfo<Markdown> ai, Context.Builder b) {
			// No-op: @Markdown settings are read at serialization/parse time via MarkdownMetaProvider.
		}
	}

	/**
	 * A collection of {@link Markdown} annotations.
	 */
	@Documented
	@Target({ java.lang.annotation.ElementType.TYPE, java.lang.annotation.ElementType.FIELD, java.lang.annotation.ElementType.METHOD })
	@Retention(RetentionPolicy.RUNTIME)
	@Inherited
	public @interface Array {

		/**
		 * The child annotations.
		 *
		 * @return The annotation value.
		 */
		Markdown[] value();
	}
}
