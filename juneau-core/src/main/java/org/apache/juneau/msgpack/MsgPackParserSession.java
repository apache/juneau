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
package org.apache.juneau.msgpack;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.parser.*;

/**
 * Session object that lives for the duration of a single use of {@link MsgPackParser}.
 * <p>
 * This class is NOT thread safe.  It is meant to be discarded after one-time use.
 *
 * @author James Bognar (james.bognar@salesforce.com)
 */
public final class MsgPackParserSession extends ParserSession {

	private MsgPackInputStream inputStream;

	/**
	 * Create a new session using properties specified in the context.
	 *
	 * @param ctx The context creating this session object.
	 * 	The context contains all the configuration settings for this object.
	 * @param beanContext The bean context being used.
	 * @param input The input.  Can be any of the following types:
	 * 	<ul>
	 * 		<li><jk>null</jk>
	 * 		<li>{@link Reader}
	 * 		<li>{@link CharSequence}
	 * 		<li>{@link InputStream} containing UTF-8 encoded text.
	 * 		<li>{@link File} containing system encoded text.
	 * 	</ul>
	 * @param op The override properties.
	 * 	These override any context properties defined in the context.
	 * @param javaMethod The java method that called this parser, usually the method in a REST servlet.
	 * @param outer The outer object for instantiating top-level non-static inner classes.
	 */
	public MsgPackParserSession(MsgPackParserContext ctx, BeanContext beanContext, Object input, ObjectMap op, Method javaMethod, Object outer) {
		super(ctx, beanContext, input, op, javaMethod, outer);
	}

	@Override /* ParserSession */
	public MsgPackInputStream getInputStream() throws ParseException {
		if (inputStream == null)
			inputStream = new MsgPackInputStream(super.getInputStream());
		return inputStream;
	}

	@Override /* ParserSession */
	public Map<String,Object> getLastLocation() {
		Map<String,Object> m = super.getLastLocation();
		if (inputStream != null)
			m.put("position", inputStream.getPosition());
		return m;
	}
}
