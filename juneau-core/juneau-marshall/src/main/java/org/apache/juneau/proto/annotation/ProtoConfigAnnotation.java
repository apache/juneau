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
package org.apache.juneau.proto.annotation;

import org.apache.juneau.*;
import org.apache.juneau.commons.reflect.*;
import org.apache.juneau.proto.*;
import org.apache.juneau.svl.*;

/**
 * Utility classes and methods for the {@link ProtoConfig @ProtoConfig} annotation.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/ProtobufBasics">Protobuf Text Format Basics</a>
 * </ul>
 */
public class ProtoConfigAnnotation {

	/**
	 * Prevents instantiation.
	 */
	private ProtoConfigAnnotation() {}

	/**
	 * Applies {@link ProtoConfig} annotations to a {@link org.apache.juneau.proto.ProtoParser.Builder}.
	 */
	public static class ParserApply extends AnnotationApplier<ProtoConfig,ProtoParser.Builder> {

		/**
		 * Constructor.
		 *
		 * @param vr The resolver for resolving values in annotations.
		 */
		public ParserApply(VarResolverSession vr) {
			super(ProtoConfig.class, ProtoParser.Builder.class, vr);
		}

		@Override
		public void apply(AnnotationInfo<ProtoConfig> ai, ProtoParser.Builder b) {
			// No-op: ProtoParser has no config options from ProtoConfig
		}
	}

	/**
	 * Applies {@link ProtoConfig} annotations to a {@link org.apache.juneau.proto.ProtoSerializer.Builder}.
	 */
	public static class SerializerApply extends AnnotationApplier<ProtoConfig,ProtoSerializer.Builder> {

		/**
		 * Constructor.
		 *
		 * @param vr The resolver for resolving values in annotations.
		 */
		public SerializerApply(VarResolverSession vr) {
			super(ProtoConfig.class, ProtoSerializer.Builder.class, vr);
		}

		@Override
		public void apply(AnnotationInfo<ProtoConfig> ai, ProtoSerializer.Builder b) {
			var a = ai.inner();

			bool(a.useListSyntaxForBeans()).ifPresent(b::useListSyntaxForBeans);
			bool(a.useColonForMessages()).ifPresent(b::useColonForMessages);
		}
	}
}
