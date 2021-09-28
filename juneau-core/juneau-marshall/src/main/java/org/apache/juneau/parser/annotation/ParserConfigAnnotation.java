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
package org.apache.juneau.parser.annotation;

import org.apache.juneau.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.svl.*;

/**
 * Utility classes and methods for the {@link ParserConfig @ParserConfig} annotation.
 */
public class ParserConfigAnnotation {

	/**
	 * Applies {@link ParserConfig} annotations to a {@link ParserBuilder}.
	 */
	public static class ParserApply extends AnnotationApplier<ParserConfig,ParserBuilder> {

		/**
		 * Constructor.
		 *
		 * @param vr The resolver for resolving values in annotations.
		 */
		public ParserApply(VarResolverSession vr) {
			super(ParserConfig.class, ParserBuilder.class, vr);
		}

		@Override
		public void apply(AnnotationInfo<ParserConfig> ai, ParserBuilder b) {
			ParserConfig a = ai.getAnnotation();

			bool(a.autoCloseStreams()).ifPresent(x -> b.autoCloseStreams(x));
			integer(a.debugOutputLines(), "debugOutputLines").ifPresent(x -> b.debugOutputLines(x));
			type(a.listener()).ifPresent(x -> b.listener(x));
			bool(a.strict()).ifPresent(x -> b.strict(x));
			bool(a.trimStrings()).ifPresent(x -> b.trimStrings(x));
			bool(a.unbuffered()).ifPresent(x -> b.unbuffered(x));
		}
	}

	/**
	 * Applies {@link ParserConfig} annotations to a {@link InputStreamParserBuilder}.
	 */
	public static class InputStreamParserApply extends AnnotationApplier<ParserConfig,InputStreamParserBuilder> {

		/**
		 * Constructor.
		 *
		 * @param vr The resolver for resolving values in annotations.
		 */
		public InputStreamParserApply(VarResolverSession vr) {
			super(ParserConfig.class, InputStreamParserBuilder.class, vr);
		}

		@Override
		public void apply(AnnotationInfo<ParserConfig> ai, InputStreamParserBuilder b) {
			ParserConfig a = ai.getAnnotation();

			string(a.binaryFormat()).map(BinaryFormat::valueOf).ifPresent(x -> b.binaryFormat(x));
		}
	}

	/**
	 * Applies {@link ParserConfig} annotations to a {@link ReaderParserBuilder}.
	 */
	public static class ReaderParserApply extends AnnotationApplier<ParserConfig,ReaderParserBuilder> {

		/**
		 * Constructor.
		 *
		 * @param vr The resolver for resolving values in annotations.
		 */
		public ReaderParserApply(VarResolverSession vr) {
			super(ParserConfig.class, ReaderParserBuilder.class, vr);
		}

		@Override
		public void apply(AnnotationInfo<ParserConfig> ai, ReaderParserBuilder b) {
			ParserConfig a = ai.getAnnotation();

			charset(a.fileCharset()).ifPresent(x -> b.fileCharset(x));
			charset(a.streamCharset()).ifPresent(x -> b.streamCharset(x));
		}
	}
}