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
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jm.SerializersAndParsers">Serializers and Parsers</a>
 * </ul>
 */
public class ParserConfigAnnotation {

	/**
	 * Applies {@link ParserConfig} annotations to a {@link org.apache.juneau.parser.Parser.Builder}.
	 */
	public static class ParserApply extends AnnotationApplier<ParserConfig,Parser.Builder> {

		/**
		 * Constructor.
		 *
		 * @param vr The resolver for resolving values in annotations.
		 */
		public ParserApply(VarResolverSession vr) {
			super(ParserConfig.class, Parser.Builder.class, vr);
		}

		@Override
		public void apply(AnnotationInfo<ParserConfig> ai, Parser.Builder b) {
			ParserConfig a = ai.inner();

			bool(a.autoCloseStreams()).ifPresent(x -> b.autoCloseStreams(x));
			integer(a.debugOutputLines(), "debugOutputLines").ifPresent(x -> b.debugOutputLines(x));
			type(a.listener()).ifPresent(x -> b.listener(x));
			bool(a.strict()).ifPresent(x -> b.strict(x));
			bool(a.trimStrings()).ifPresent(x -> b.trimStrings(x));
			bool(a.unbuffered()).ifPresent(x -> b.unbuffered(x));
		}
	}

	/**
	 * Applies {@link ParserConfig} annotations to a {@link org.apache.juneau.parser.InputStreamParser.Builder}.
	 */
	public static class InputStreamParserApply extends AnnotationApplier<ParserConfig,InputStreamParser.Builder> {

		/**
		 * Constructor.
		 *
		 * @param vr The resolver for resolving values in annotations.
		 */
		public InputStreamParserApply(VarResolverSession vr) {
			super(ParserConfig.class, InputStreamParser.Builder.class, vr);
		}

		@Override
		public void apply(AnnotationInfo<ParserConfig> ai, InputStreamParser.Builder b) {
			ParserConfig a = ai.inner();

			string(a.binaryFormat()).map(BinaryFormat::valueOf).ifPresent(x -> b.binaryFormat(x));
		}
	}

	/**
	 * Applies {@link ParserConfig} annotations to a {@link org.apache.juneau.parser.ReaderParser.Builder}.
	 */
	public static class ReaderParserApply extends AnnotationApplier<ParserConfig,ReaderParser.Builder> {

		/**
		 * Constructor.
		 *
		 * @param vr The resolver for resolving values in annotations.
		 */
		public ReaderParserApply(VarResolverSession vr) {
			super(ParserConfig.class, ReaderParser.Builder.class, vr);
		}

		@Override
		public void apply(AnnotationInfo<ParserConfig> ai, ReaderParser.Builder b) {
			ParserConfig a = ai.inner();

			charset(a.fileCharset()).ifPresent(x -> b.fileCharset(x));
			charset(a.streamCharset()).ifPresent(x -> b.streamCharset(x));
		}
	}
}