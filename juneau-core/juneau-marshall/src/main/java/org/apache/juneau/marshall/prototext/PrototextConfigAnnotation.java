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
package org.apache.juneau.marshall.prototext;

import org.apache.juneau.commons.reflect.*;
import org.apache.juneau.commons.svl.*;
import org.apache.juneau.marshall.*;

/**
 * Utility classes and methods for the {@link PrototextConfig @PrototextConfig} annotation.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/Prototext">Protobuf Text Format Basics</a>
 * </ul>
 */
public class PrototextConfigAnnotation {

	/**
	 * Prevents instantiation.
	 */
	private PrototextConfigAnnotation() {}

	/**
	 * Applies {@link PrototextConfig} annotations to a {@link org.apache.juneau.marshall.prototext.PrototextParser.Builder}.
	 */
	public static class ParserApply extends AnnotationApplier<PrototextConfig,PrototextParser.Builder> {

		/**
		 * Constructor.
		 *
		 * @param vr The resolver for resolving values in annotations.
		 */
		public ParserApply(VarResolverSession vr) {
			super(PrototextConfig.class, PrototextParser.Builder.class, vr);
		}

		@Override
		public void apply(AnnotationInfo<PrototextConfig> ai, PrototextParser.Builder b) {
			// No-op: PrototextParser has no config options from PrototextConfig
		}
	}

	/**
	 * Applies {@link PrototextConfig} annotations to a {@link org.apache.juneau.marshall.prototext.PrototextSerializer.Builder}.
	 */
	public static class SerializerApply extends AnnotationApplier<PrototextConfig,PrototextSerializer.Builder> {

		/**
		 * Constructor.
		 *
		 * @param vr The resolver for resolving values in annotations.
		 */
		public SerializerApply(VarResolverSession vr) {
			super(PrototextConfig.class, PrototextSerializer.Builder.class, vr);
		}

		@Override
		public void apply(AnnotationInfo<PrototextConfig> ai, PrototextSerializer.Builder b) {
			var a = ai.inner();

			bool(a.useListSyntaxForBeans()).ifPresent(b::useListSyntaxForBeans);
			bool(a.useColonForMessages()).ifPresent(b::useColonForMessages);
		}
	}
}
