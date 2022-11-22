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

import org.apache.juneau.*;

/**
 * Represents a parser and media type that matches an HTTP <c>Content-Type</c> header value.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../index.html#jm.SerializersAndParsers">Serializers and Parsers</a>
 * </ul>
 */
public final class ParserMatch {

	private final MediaType mediaType;
	private final Parser parser;

	/**
	 * Constructor.
	 *
	 * @param mediaType The media type of the match.
	 * @param parser The parser that matched.
	 */
	public ParserMatch(MediaType mediaType, Parser parser) {
		this.mediaType = mediaType;
		this.parser = parser;
	}

	/**
	 * Returns the media type of the parser that matched the HTTP <c>Content-Type</c> header value.
	 *
	 * @return The media type of the match.
	 */
	public MediaType getMediaType() {
		return mediaType;
	}

	/**
	 * Returns the parser that matched the HTTP <c>Content-Type</c> header value.
	 *
	 * @return The parser of the match.
	 */
	public Parser getParser() {
		return parser;
	}
}
