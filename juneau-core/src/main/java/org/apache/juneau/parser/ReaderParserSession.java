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

/**
 * Subclass of parser session objects for character-based parsers.
 *
 * <p>
 * This class is NOT thread safe.  It is typically discarded after one-time use.
 */
public abstract class ReaderParserSession extends ParserSession {

	/**
	 * Create a new session using properties specified in the context.
	 *
	 * @param ctx
	 * 	The context creating this session object.
	 * 	The context contains all the configuration settings for this object.
	 * @param args
	 * 	Runtime session arguments.
	 */
	protected ReaderParserSession(ParserContext ctx, ParserSessionArgs args) {
		super(ctx, args);
	}

	/**
	 * Constructor for sessions that don't require context.
	 *
	 * @param args
	 * 	Runtime session arguments.
	 */
	protected ReaderParserSession(ParserSessionArgs args) {
		this(null, args);
	}


	@Override /* ParserSession */
	public final boolean isReaderParser() {
		return true;
	}
}
