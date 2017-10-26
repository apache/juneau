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
package org.apache.juneau.parser;

import static org.apache.juneau.parser.Parser.*;

import org.apache.juneau.*;

/**
 * Contains a snapshot-in-time read-only copy of the settings on the {@link Parser} class.
 */
public class ParserContext extends BeanContext {

	/**
	 * Default context with all default values.
	 */
	static final ParserContext DEFAULT = new ParserContext(PropertyStore.create());


	final boolean trimStrings, strict;
	final String inputStreamCharset, fileCharset;
	final Class<? extends ParserListener> listener;

	/**
	 * Constructor.
	 *
	 * @param ps The property store that created this context.
	 */
	public ParserContext(PropertyStore ps) {
		super(ps);
		this.trimStrings = ps.getProperty(PARSER_trimStrings, boolean.class, false);
		this.strict = ps.getProperty(PARSER_strict, boolean.class, false);
		this.inputStreamCharset = ps.getProperty(PARSER_inputStreamCharset, String.class, "UTF-8");
		this.fileCharset = ps.getProperty(PARSER_fileCharset, String.class, "default");
		this.listener = ps.getProperty(PARSER_listener, Class.class, null);
	}

	@Override /* Context */
	public ObjectMap asMap() {
		return super.asMap()
			.append("ParserContext", new ObjectMap()
				.append("trimStrings", trimStrings)
				.append("strict", strict)
				.append("inputStreamCharset", inputStreamCharset)
				.append("fileCharset", fileCharset)
				.append("listener", listener)
			);
	}
}
