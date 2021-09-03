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

import static org.apache.juneau.parser.InputStreamParser.*;
import static org.apache.juneau.parser.ReaderParser.*;

import org.apache.juneau.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.svl.*;

/**
 * Utility classes and methods for the {@link ParserConfig @ParserConfig} annotation.
 */
public class ParserConfigAnnotation {

	/**
	 * Applies {@link ParserConfig} annotations to a {@link ContextPropertiesBuilder}.
	 */
	public static class Apply extends AnnotationApplier<ParserConfig,ContextPropertiesBuilder> {

		/**
		 * Constructor.
		 *
		 * @param vr The resolver for resolving values in annotations.
		 */
		public Apply(VarResolverSession vr) {
			super(ParserConfig.class, ContextPropertiesBuilder.class, vr);
		}

		@Override
		public void apply(AnnotationInfo<ParserConfig> ai, ContextPropertiesBuilder b) {
			ParserConfig a = ai.getAnnotation();

			bool(a.autoCloseStreams()).ifPresent(x -> b.set(PARSER_autoCloseStreams, x));
			integer(a.debugOutputLines(), "debugOutputLines").ifPresent(x -> b.set(PARSER_debugOutputLines, x));
			type(a.listener()).ifPresent(x -> b.set(PARSER_listener, x));
			bool(a.strict()).ifPresent(x -> b.set(PARSER_strict, x));
			bool(a.trimStrings()).ifPresent(x -> b.set(PARSER_trimStrings, x));
			bool(a.unbuffered()).ifPresent(x -> b.set(PARSER_unbuffered, x));
			string(a.binaryFormat()).ifPresent(x -> b.set(ISPARSER_binaryFormat, x));
			charset(a.fileCharset()).ifPresent(x -> b.set(RPARSER_fileCharset, x));
			charset(a.streamCharset()).ifPresent(x -> b.set(RPARSER_streamCharset, x));
		}
	}
}