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
package org.apache.juneau.marshall.protobuf;

import org.apache.juneau.commons.reflect.*;
import org.apache.juneau.commons.svl.*;
import org.apache.juneau.marshall.*;

/**
 * Utility classes and methods for the {@link ProtobufConfig @ProtobufConfig} annotation.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/Protobuf">Protobuf Binary Format Basics</a>
 * </ul>
 */
public class ProtobufConfigAnnotation {

	/**
	 * Prevents instantiation.
	 */
	private ProtobufConfigAnnotation() {}

	/**
	 * Applies {@link ProtobufConfig} annotations to a {@link org.apache.juneau.marshall.protobuf.ProtobufParser.Builder}.
	 */
	public static class ParserApply extends AnnotationApplier<ProtobufConfig,ProtobufParser.Builder> {

		/**
		 * Constructor.
		 *
		 * @param vr The resolver for resolving values in annotations.
		 */
		public ParserApply(VarResolverSession vr) {
			super(ProtobufConfig.class, ProtobufParser.Builder.class, vr);
		}

		@Override
		public void apply(AnnotationInfo<ProtobufConfig> ai, ProtobufParser.Builder b) {
			ProtobufConfig a = ai.inner();

			bool(a.nativeTypes()).ifPresent(b::nativeTypes);
		}
	}

	/**
	 * Applies {@link ProtobufConfig} annotations to a {@link org.apache.juneau.marshall.protobuf.ProtobufSerializer.Builder}.
	 */
	public static class SerializerApply extends AnnotationApplier<ProtobufConfig,ProtobufSerializer.Builder> {

		/**
		 * Constructor.
		 *
		 * @param vr The resolver for resolving values in annotations.
		 */
		public SerializerApply(VarResolverSession vr) {
			super(ProtobufConfig.class, ProtobufSerializer.Builder.class, vr);
		}

		@Override
		public void apply(AnnotationInfo<ProtobufConfig> ai, ProtobufSerializer.Builder b) {
			ProtobufConfig a = ai.inner();

			bool(a.addBeanTypes()).ifPresent(b::addBeanTypesProtobuf);
			bool(a.nativeTypes()).ifPresent(b::nativeTypes);
		}
	}
}
