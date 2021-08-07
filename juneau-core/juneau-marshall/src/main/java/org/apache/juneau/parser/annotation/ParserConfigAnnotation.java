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

import java.nio.charset.*;

import org.apache.juneau.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.svl.*;

/**
 * Utility classes and methods for the {@link ParserConfig @ParserConfig} annotation.
 */
public class ParserConfigAnnotation {

	/**
	 * Applies {@link ParserConfig} annotations to a {@link ContextPropertiesBuilder}.
	 */
	public static class Apply extends ConfigApply<ParserConfig> {

		/**
		 * Constructor.
		 *
		 * @param vr The resolver for resolving values in annotations.
		 */
		public Apply(VarResolverSession vr) {
			super(ParserConfig.class, vr);
		}

		@Override
		public void apply(AnnotationInfo<ParserConfig> ai, ContextPropertiesBuilder b) {
			ParserConfig a = ai.getAnnotation();

			b.setIfNotEmpty(PARSER_autoCloseStreams, bool(a.autoCloseStreams()));
			b.setIfNotEmpty(PARSER_debugOutputLines, integer(a.debugOutputLines(), "debugOutputLines"));
			b.setIf(a.listener() != ParserListener.Null.class, PARSER_listener, a.listener());
			b.setIfNotEmpty(PARSER_strict, bool(a.strict()));
			b.setIfNotEmpty(PARSER_trimStrings, bool(a.trimStrings()));
			b.setIfNotEmpty(PARSER_unbuffered, bool(a.unbuffered()));
			b.setIfNotEmpty(ISPARSER_binaryFormat, string(a.binaryFormat()));
			b.setIfNotEmpty(RPARSER_fileCharset, charset(a.fileCharset()));
			b.setIfNotEmpty(RPARSER_streamCharset, charset(a.streamCharset()));
		}

		private Object charset(String in) {
			String s = string(in);
			if ("default".equalsIgnoreCase(s))
				return Charset.defaultCharset();
			return s;
		}
	}
}