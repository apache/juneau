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
import static org.apache.juneau.serializer.OutputStreamSerializer.*;
import static org.apache.juneau.serializer.WriterSerializer.*;

import org.apache.juneau.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.svl.*;

/**
 * Utility classes and methods for the {@link SerializerConfig @SerializerConfig} annotation.
 */
public class SerializerConfigAnnotation {

	/**
	 * Applies {@link SerializerConfig} annotations to a {@link ContextPropertiesBuilder}.
	 */
	public static class Apply extends AnnotationApplier<SerializerConfig,ContextPropertiesBuilder> {

		/**
		 * Constructor.
		 *
		 * @param vr The resolver for resolving values in annotations.
		 */
		public Apply(VarResolverSession vr) {
			super(SerializerConfig.class, ContextPropertiesBuilder.class, vr);
		}

		@Override
		public void apply(AnnotationInfo<SerializerConfig> ai, ContextPropertiesBuilder b) {
			SerializerConfig a = ai.getAnnotation();

			bool(a.addBeanTypes()).ifPresent(x -> b.set(SERIALIZER_addBeanTypes, x));
			bool(a.addRootType()).ifPresent(x -> b.set(SERIALIZER_addRootType, x));
			bool(a.keepNullProperties()).ifPresent(x -> b.set(SERIALIZER_keepNullProperties, x));
			type(a.listener()).ifPresent(x -> b.set(SERIALIZER_listener, x));
			bool(a.sortCollections()).ifPresent(x -> b.set(SERIALIZER_sortCollections, x));
			bool(a.sortMaps()).ifPresent(x -> b.set(SERIALIZER_sortMaps, x));
			bool(a.trimEmptyCollections()).ifPresent(x -> b.set(SERIALIZER_trimEmptyCollections, x));
			bool(a.trimEmptyMaps()).ifPresent(x -> b.set(SERIALIZER_trimEmptyMaps, x));
			bool(a.trimStrings()).ifPresent(x -> b.set(SERIALIZER_trimStrings, x));
			string(a.uriContext()).ifPresent(x -> b.set(SERIALIZER_uriContext, x));
			string(a.uriRelativity()).ifPresent(x -> b.set(SERIALIZER_uriRelativity, x));
			string(a.uriResolution()).ifPresent(x -> b.set(SERIALIZER_uriResolution, x));
			string(a.binaryFormat()).ifPresent(x -> b.set(OSSERIALIZER_binaryFormat, x));
			charset(a.fileCharset()).ifPresent(x -> b.set(WSERIALIZER_fileCharset, x));
			integer(a.maxIndent(), "maxIndent").ifPresent(x -> b.set(WSERIALIZER_maxIndent, x));
			character(a.quoteChar(), "quoteChar").ifPresent(x -> b.set(WSERIALIZER_quoteChar, x));
			charset(a.streamCharset()).ifPresent(x -> b.set(WSERIALIZER_streamCharset, x));
			bool(a.useWhitespace()).ifPresent(x -> b.set(WSERIALIZER_useWhitespace, x));
			bool(a.detectRecursions()).ifPresent(x -> b.set(BEANTRAVERSE_detectRecursions, x));
			bool(a.ignoreRecursions()).ifPresent(x -> b.set(BEANTRAVERSE_ignoreRecursions, x));
			integer(a.initialDepth(), "initialDepth").ifPresent(x -> b.set(BEANTRAVERSE_initialDepth, x));
			integer(a.maxDepth(), "maxDepth").ifPresent(x -> b.set(BEANTRAVERSE_maxDepth, x));
		}
	}
}