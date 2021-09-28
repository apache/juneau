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
package org.apache.juneau.serializer.annotation;

import static org.apache.juneau.BeanTraverseContext.*;

import org.apache.juneau.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.svl.*;

/**
 * Utility classes and methods for the {@link SerializerConfig @SerializerConfig} annotation.
 */
public class SerializerConfigAnnotation {

	/**
	 * Applies {@link SerializerConfig} annotations to a {@link SerializerBuilder}.
	 */
	public static class SerializerApply extends AnnotationApplier<SerializerConfig,SerializerBuilder> {

		/**
		 * Constructor.
		 *
		 * @param vr The resolver for resolving values in annotations.
		 */
		public SerializerApply(VarResolverSession vr) {
			super(SerializerConfig.class, SerializerBuilder.class, vr);
		}

		@Override
		public void apply(AnnotationInfo<SerializerConfig> ai, SerializerBuilder b) {
			SerializerConfig a = ai.getAnnotation();

			bool(a.addBeanTypes()).ifPresent(x -> b.addBeanTypes(x));
			bool(a.addRootType()).ifPresent(x -> b.addRootType(x));
			bool(a.keepNullProperties()).ifPresent(x -> b.keepNullProperties(x));
			type(a.listener()).ifPresent(x -> b.listener(x));
			bool(a.sortCollections()).ifPresent(x -> b.sortCollections(x));
			bool(a.sortMaps()).ifPresent(x -> b.sortMaps(x));
			bool(a.trimEmptyCollections()).ifPresent(x -> b.trimEmptyCollections(x));
			bool(a.trimEmptyMaps()).ifPresent(x -> b.trimEmptyMaps(x));
			bool(a.trimStrings()).ifPresent(x -> b.trimStrings(x));
			string(a.uriContext()).map(UriContext::of).ifPresent(x -> b.uriContext(x));
			string(a.uriRelativity()).map(UriRelativity::valueOf).ifPresent(x -> b.uriRelativity(x));
			string(a.uriResolution()).map(UriResolution::valueOf).ifPresent(x -> b.uriResolution(x));
			bool(a.detectRecursions()).ifPresent(x -> b.set(BEANTRAVERSE_detectRecursions, x));
			bool(a.ignoreRecursions()).ifPresent(x -> b.set(BEANTRAVERSE_ignoreRecursions, x));
			integer(a.initialDepth(), "initialDepth").ifPresent(x -> b.set(BEANTRAVERSE_initialDepth, x));
			integer(a.maxDepth(), "maxDepth").ifPresent(x -> b.set(BEANTRAVERSE_maxDepth, x));
		}
	}

	/**
	 * Applies {@link SerializerConfig} annotations to a {@link OutputStreamSerializerBuilder}.
	 */
	public static class OutputStreamSerializerApply extends AnnotationApplier<SerializerConfig,OutputStreamSerializerBuilder> {

		/**
		 * Constructor.
		 *
		 * @param vr The resolver for resolving values in annotations.
		 */
		public OutputStreamSerializerApply(VarResolverSession vr) {
			super(SerializerConfig.class, OutputStreamSerializerBuilder.class, vr);
		}

		@Override
		public void apply(AnnotationInfo<SerializerConfig> ai, OutputStreamSerializerBuilder b) {
			SerializerConfig a = ai.getAnnotation();

			string(a.binaryFormat()).map(BinaryFormat::valueOf).ifPresent(x -> b.binaryFormat(x));
		}
	}

	/**
	 * Applies {@link SerializerConfig} annotations to a {@link WriterSerializerBuilder}.
	 */
	public static class WriterSerializerApply extends AnnotationApplier<SerializerConfig,WriterSerializerBuilder> {

		/**
		 * Constructor.
		 *
		 * @param vr The resolver for resolving values in annotations.
		 */
		public WriterSerializerApply(VarResolverSession vr) {
			super(SerializerConfig.class, WriterSerializerBuilder.class, vr);
		}

		@Override
		public void apply(AnnotationInfo<SerializerConfig> ai, WriterSerializerBuilder b) {
			SerializerConfig a = ai.getAnnotation();

			charset(a.fileCharset()).ifPresent(x -> b.fileCharset(x));
			integer(a.maxIndent(), "maxIndent").ifPresent(x -> b.maxIndent(x));
			character(a.quoteChar(), "quoteChar").ifPresent(x -> b.quoteChar(x));
			charset(a.streamCharset()).ifPresent(x -> b.streamCharset(x));
			bool(a.useWhitespace()).ifPresent(x -> b.useWhitespace(x));
		}
	}
}