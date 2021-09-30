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

import java.nio.charset.*;

import org.apache.juneau.collections.*;

/**
 * Subclass of {@link Parser} for characters-based parsers.
 * {@review}
 *
 * <h5 class='topic'>Description</h5>
 *
 * This class is typically the parent class of all character-based parsers.
 * It has 1 abstract method to implement...
 * <ul>
 * 	<li><c>parse(ParserSession, ClassMeta)</c>
 * </ul>
 */
public abstract class ReaderParser extends Parser {

	//-------------------------------------------------------------------------------------------------------------------
	// Instance
	//-------------------------------------------------------------------------------------------------------------------

	final Charset streamCharset, fileCharset;

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	protected ReaderParser(ReaderParserBuilder builder) {
		super(builder);
		streamCharset = builder.streamCharset;
		fileCharset = builder.fileCharset;
	}

	@Override /* Parser */
	public final boolean isReaderParser() {
		return true;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * File charset.
	 *
	 * @see ReaderParserBuilder#fileCharset(Charset)
	 * @return
	 * 	The character set to use for reading <c>Files</c> from the file system.
	 */
	protected final Charset getFileCharset() {
		return fileCharset;
	}

	/**
	 * Input stream charset.
	 *
	 * @see ReaderParserBuilder#streamCharset(Charset)
	 * @return
	 * 	The character set to use for converting <c>InputStreams</c> and byte arrays to readers.
	 */
	protected final Charset getStreamCharset() {
		return streamCharset;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other methods
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* Context */
	public OMap toMap() {
		return super.toMap()
			.a(
				"ReaderParser",
				OMap
					.create()
					.filtered()
					.a("fileCharset", fileCharset)
					.a("streamCharset", streamCharset)
			);
	}
}
