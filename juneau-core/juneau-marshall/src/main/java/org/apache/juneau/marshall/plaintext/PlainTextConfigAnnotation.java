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
package org.apache.juneau.marshall.plaintext;

import org.apache.juneau.commons.reflect.*;
import org.apache.juneau.commons.svl.*;
import org.apache.juneau.marshall.*;

/**
 * Utility classes and methods for the {@link PlainTextConfig @PlainTextConfig} annotation.
 *
 */
public class PlainTextConfigAnnotation {

	/**
	 * Prevents instantiation.
	 */
	private PlainTextConfigAnnotation() {}

	/**
	 * Applies {@link PlainTextConfig} annotations to a {@link PlainTextParser.Builder}.
	 */
	public static class ParserApply extends AnnotationApplier<PlainTextConfig,PlainTextParser.Builder> {

		/**
		 * Constructor.
		 *
		 * @param vr The resolver for resolving values in annotations.
		 */
		public ParserApply(VarResolverSession vr) {
			super(PlainTextConfig.class, PlainTextParser.Builder.class, vr);
		}

		@Override
		public void apply(AnnotationInfo<PlainTextConfig> ai, PlainTextParser.Builder b) {
			// No-op: Annotation applier with no work to do
		}
	}

	/**
	 * Applies {@link PlainTextConfig} annotations to a {@link PlainTextSerializer.Builder}.
	 */
	public static class SerializerApply extends AnnotationApplier<PlainTextConfig,PlainTextSerializer.Builder> {

		/**
		 * Constructor.
		 *
		 * @param vr The resolver for resolving values in annotations.
		 */
		public SerializerApply(VarResolverSession vr) {
			super(PlainTextConfig.class, PlainTextSerializer.Builder.class, vr);
		}

		@Override
		public void apply(AnnotationInfo<PlainTextConfig> ai, PlainTextSerializer.Builder b) {
			// No-op: Annotation applier with no work to do
		}
	}
}