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
package org.apache.juneau.serializer.annotation;

import org.apache.juneau.*;
import org.apache.juneau.commons.reflect.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.svl.*;

/**
 * Utility classes and methods for the {@link SerializerConfig @SerializerConfig} annotation.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/SerializersAndParsers">Serializers and Parsers</a>
 * </ul>
 */
public class SerializerConfigAnnotation {

	/**
	 * Applies {@link SerializerConfig} annotations to a {@link org.apache.juneau.serializer.OutputStreamSerializer.Builder}.
	 */
	public static class OutputStreamSerializerApply extends AnnotationApplier<SerializerConfig,OutputStreamSerializer.Builder> {

		/**
		 * Constructor.
		 *
		 * @param vr The resolver for resolving values in annotations.
		 */
		public OutputStreamSerializerApply(VarResolverSession vr) {
			super(SerializerConfig.class, OutputStreamSerializer.Builder.class, vr);
		}

		@Override
		public void apply(AnnotationInfo<SerializerConfig> ai, OutputStreamSerializer.Builder b) {
			SerializerConfig a = ai.inner();

			string(a.binaryFormat()).map(BinaryFormat::valueOf).ifPresent(b::binaryFormat);
		}
	}

	/**
	 * Applies {@link SerializerConfig} annotations to a {@link org.apache.juneau.serializer.Serializer.Builder}.
	 */
	public static class SerializerApply extends AnnotationApplier<SerializerConfig,Serializer.Builder> {

		/**
		 * Constructor.
		 *
		 * @param vr The resolver for resolving values in annotations.
		 */
		public SerializerApply(VarResolverSession vr) {
			super(SerializerConfig.class, Serializer.Builder.class, vr);
		}

		@Override
		public void apply(AnnotationInfo<SerializerConfig> ai, Serializer.Builder b) {
			SerializerConfig a = ai.inner();

			bool(a.addBeanTypes()).ifPresent(b::addBeanTypes);
			bool(a.addRootType()).ifPresent(b::addRootType);
			bool(a.keepNullProperties()).ifPresent(b::keepNullProperties);
			type(a.listener()).ifPresent(b::listener);
			bool(a.sortCollections()).ifPresent(b::sortCollections);
			bool(a.sortMaps()).ifPresent(b::sortMaps);
			bool(a.trimEmptyCollections()).ifPresent(b::trimEmptyCollections);
			bool(a.trimEmptyMaps()).ifPresent(b::trimEmptyMaps);
			bool(a.trimStrings()).ifPresent(b::trimStrings);
			string(a.uriContext()).map(UriContext::of).ifPresent(b::uriContext);
			string(a.uriRelativity()).map(UriRelativity::valueOf).ifPresent(b::uriRelativity);
			string(a.uriResolution()).map(UriResolution::valueOf).ifPresent(b::uriResolution);
			bool(a.detectRecursions()).ifPresent(b::detectRecursions);
			bool(a.ignoreRecursions()).ifPresent(b::ignoreRecursions);
			integer(a.initialDepth(), "initialDepth").ifPresent(b::initialDepth);
			integer(a.maxDepth(), "maxDepth").ifPresent(b::maxDepth);
		}
	}

	/**
	 * Applies {@link SerializerConfig} annotations to a {@link org.apache.juneau.serializer.WriterSerializer.Builder}.
	 */
	public static class WriterSerializerApply extends AnnotationApplier<SerializerConfig,WriterSerializer.Builder> {

		/**
		 * Constructor.
		 *
		 * @param vr The resolver for resolving values in annotations.
		 */
		public WriterSerializerApply(VarResolverSession vr) {
			super(SerializerConfig.class, WriterSerializer.Builder.class, vr);
		}

		@Override
		public void apply(AnnotationInfo<SerializerConfig> ai, WriterSerializer.Builder b) {
			SerializerConfig a = ai.inner();

			charset(a.fileCharset()).ifPresent(b::fileCharset);
			integer(a.maxIndent(), "maxIndent").ifPresent(b::maxIndent);
			character(a.quoteChar(), "quoteChar").ifPresent(b::quoteChar);
			charset(a.streamCharset()).ifPresent(b::streamCharset);
			bool(a.useWhitespace()).ifPresent(b::useWhitespace);
		}
	}
}