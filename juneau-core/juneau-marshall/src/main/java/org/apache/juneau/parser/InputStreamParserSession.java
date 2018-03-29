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

import static org.apache.juneau.parser.InputStreamParser.*;

import java.io.*;

import org.apache.juneau.*;

/**
 * Subclass of parser session objects for byte-based parsers.
 * 
 * <p>
 * This class is NOT thread safe.  It is typically discarded after one-time use.
 */
public abstract class InputStreamParserSession extends ParserSession {

	private final BinaryFormat binaryFormat;
	
	/**
	 * Create a new session using properties specified in the context.
	 * 
	 * @param ctx
	 * 	The context creating this session object.
	 * 	The context contains all the configuration settings for this object.
	 * @param args
	 * 	Runtime session arguments.
	 */
	protected InputStreamParserSession(InputStreamParser ctx, ParserSessionArgs args) {
		super(ctx, args);
		
		binaryFormat = getProperty(ISPARSER_binaryFormat, BinaryFormat.class, BinaryFormat.HEX);
	}

	/**
	 * Constructor for sessions that don't require context.
	 * 
	 * @param args
	 * 	Runtime session arguments.
	 */
	protected InputStreamParserSession(ParserSessionArgs args) {
		this(InputStreamParser.DEFAULT, args);
	}

	@Override /* ParserSession */
	public final boolean isReaderParser() {
		return false;
	}
	
	/**
	 * Wraps the specified input object into a {@link ParserPipe} object so that it can be easily converted into
	 * a stream or reader.
	 * 
	 * @param input
	 * 	The input.
	 * 	</ul>
	 * 	<br>This can be any of the following types:
	 * 	<ul>
	 * 		<li><jk>null</jk>
	 * 		<li>{@link InputStream}
	 * 		<li><code><jk>byte</jk>[]</code>
	 * 		<li>{@link File}
	 * 		<li>{@link CharSequence} containing encoded bytes according to the {@link InputStreamParser#ISPARSER_binaryFormat} setting.
	 * 	</ul>
	 * @return
	 * 	A new {@link ParserPipe} wrapper around the specified input object.
	 */
	@SuppressWarnings("resource")
	@Override /* ParserSession */
	public final ParserPipe createPipe(Object input) {
		return setPipe(new ParserPipe(input, isDebug(), autoCloseStreams, unbuffered, binaryFormat));
	}

	@Override /* Session */
	public ObjectMap asMap() {
		return super.asMap()
			.append("InputStreamParserSession", new ObjectMap()
				.append("binaryFormat", binaryFormat)
			);
	}
}
