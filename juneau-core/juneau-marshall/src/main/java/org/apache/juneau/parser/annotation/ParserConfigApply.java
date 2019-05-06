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

import static org.apache.juneau.parser.Parser.*;
import static org.apache.juneau.parser.ReaderParser.*;
import static org.apache.juneau.parser.InputStreamParser.*;

import org.apache.juneau.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.utils.*;

/**
 * Applies {@link ParserConfig} annotations to a {@link PropertyStoreBuilder}.
 */
public class ParserConfigApply extends ConfigApply<ParserConfig> {

	/**
	 * Constructor.
	 *
	 * @param c The annotation class.
	 * @param r The resolver for resolving values in annotations.
	 */
	public ParserConfigApply(Class<ParserConfig> c, StringResolver r) {
		super(c, r);
	}

	@Override
	public void apply(ParserConfig a, PropertyStoreBuilder psb) {
		if (! a.autoCloseStreams().isEmpty())
			psb.set(PARSER_autoCloseStreams, bool(a.autoCloseStreams()));
		if (! a.debugOutputLines().isEmpty())
			psb.set(PARSER_debugOutputLines, integer(a.debugOutputLines(), "debugOutputLines"));
		if (a.listener() != ParserListener.Null.class)
			psb.set(PARSER_listener, a.listener());
		if (! a.strict().isEmpty())
			psb.set(PARSER_strict, bool(a.strict()));
		if (! a.trimStrings().isEmpty())
			psb.set(PARSER_trimStrings, bool(a.trimStrings()));
		if (! a.unbuffered().isEmpty())
			psb.set(PARSER_unbuffered, bool(a.unbuffered()));

		if (! a.binaryFormat().isEmpty())
			psb.set(ISPARSER_binaryFormat, string(a.binaryFormat()));

		if (! a.fileCharset().isEmpty())
			psb.set(RPARSER_fileCharset, string(a.fileCharset()));
		if (! a.inputStreamCharset().isEmpty())
			psb.set(RPARSER_inputStreamCharset, string(a.inputStreamCharset()));
	}
}
