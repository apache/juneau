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


import java.io.*;

import org.apache.juneau.*;

/**
 * Subclass of parser session objects for character-based parsers.
 *
 * <p>
 * This class is NOT thread safe.  It is typically discarded after one-time use.
 */
public abstract class ReaderParserSession extends ParserSession {

	private final ReaderParser ctx;

	/**
	 * Create a new session using properties specified in the context.
	 *
	 * @param ctx
	 * 	The parser creating this session object.
	 * 	The parser contains all the configuration settings for this object.
	 * @param args
	 * 	Runtime session arguments.
	 */
	protected ReaderParserSession(ReaderParser ctx, ParserSessionArgs args) {
		super(ctx, args);
		this.ctx = ctx;
	}

	/**
	 * Constructor for sessions that don't require context.
	 *
	 * @param args
	 * 	Runtime session arguments.
	 */
	protected ReaderParserSession(ParserSessionArgs args) {
		this(ReaderParser.DEFAULT, args);
	}


	@Override /* ParserSession */
	public final boolean isReaderParser() {
		return true;
	}

	/**
	 * Wraps the specified input object into a {@link ParserPipe} object so that it can be easily converted into
	 * a stream or reader.
	 *
	 * @param input
	 * 	The input.
	 * 	<br>This can be any of the following types:
	 * 	<ul>
	 * 		<li><jk>null</jk>
	 * 		<li>{@link Reader}
	 * 		<li>{@link CharSequence}
	 * 		<li>{@link InputStream} containing UTF-8 encoded text (or whatever the encoding specified by
	 * 			{@link ReaderParser#RPARSER_inputStreamCharset}).
	 * 		<li><code><jk>byte</jk>[]</code> containing UTF-8 encoded text (or whatever the encoding specified by
	 * 			{@link ReaderParser#RPARSER_inputStreamCharset}).
	 * 		<li>{@link File} containing system encoded text (or whatever the encoding specified by
	 * 			{@link ReaderParser#RPARSER_fileCharset}).
	 * 	</ul>
	 * @return
	 * 	A new {@link ParserPipe} wrapper around the specified input object.
	 */
	@SuppressWarnings("resource")
	@Override /* ParserSesson */
	public final ParserPipe createPipe(Object input) {
		return setPipe(new ParserPipe(input, isDebug(), ctx.isStrict(), ctx.isAutoCloseStreams(), ctx.isUnbuffered(), getFileCharset(), getInputStreamCharset()));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Configuration property:  Input stream charset.
	 *
	 * @see ReaderParser#RPARSER_inputStreamCharset
	 * @return
	 * 	The character set to use for converting <code>InputStreams</code> and byte arrays to readers.
	 */
	protected final String getInputStreamCharset() {
		return ctx.getInputStreamCharset();
	}

	/**
	 * Configuration property:  File charset.
	 *
	 * @see ReaderParser#RPARSER_fileCharset
	 * @return
	 * 	The character set to use for reading <code>Files</code> from the file system.
	 */
	protected final String getFileCharset() {
		return ctx.getFileCharset();
	}

	@Override /* Session */
	public ObjectMap asMap() {
		return super.asMap()
			.append("ReaderParserSession", new ObjectMap()
			);
	}
}
