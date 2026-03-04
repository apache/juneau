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

import org.apache.juneau.*;
import org.apache.juneau.commons.reflect.*;
import org.apache.juneau.markdown.*;
import org.apache.juneau.svl.*;

/**
 * Utility classes and methods for the {@link MarkdownConfig @MarkdownConfig} annotation.
 */
public class MarkdownConfigAnnotation {

	private MarkdownConfigAnnotation() {}

	/**
	 * Applies {@link MarkdownConfig} annotations to a {@link org.apache.juneau.markdown.MarkdownParser.Builder}.
	 */
	public static class ParserApply extends AnnotationApplier<MarkdownConfig,MarkdownParser.Builder> {

		/**
		 * Constructor.
		 *
		 * @param vr The resolver for resolving values in annotations.
		 */
		public ParserApply(VarResolverSession vr) {
			super(MarkdownConfig.class, MarkdownParser.Builder.class, vr);
		}

		@Override
		public void apply(AnnotationInfo<MarkdownConfig> ai, MarkdownParser.Builder b) {
			// No-op: Annotation applier with no work to do
		}
	}

	/**
	 * Applies {@link MarkdownConfig} annotations to a {@link org.apache.juneau.markdown.MarkdownSerializer.Builder}.
	 */
	public static class SerializerApply extends AnnotationApplier<MarkdownConfig,MarkdownSerializer.Builder> {

		/**
		 * Constructor.
		 *
		 * @param vr The resolver for resolving values in annotations.
		 */
		public SerializerApply(VarResolverSession vr) {
			super(MarkdownConfig.class, MarkdownSerializer.Builder.class, vr);
		}

		@Override
		public void apply(AnnotationInfo<MarkdownConfig> ai, MarkdownSerializer.Builder b) {
			// No-op: Annotation applier with no work to do
		}
	}
}
