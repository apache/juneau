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
package org.apache.juneau.serializer;

import org.apache.juneau.*;

/**
 * Represents a serializer and media type that matches an HTTP <c>Accept</c> header value.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../index.html#jm.SerializersAndParsers">Serializers and Parsers</a>
 * </ul>
 */
public final class SerializerMatch {

	private final MediaType mediaType;
	private final Serializer serializer;

	/**
	 * Constructor.
	 *
	 * @param mediaType The media type matched.
	 * @param serializer The serializer matched.
	 */
	public SerializerMatch(MediaType mediaType, Serializer serializer) {
		this.mediaType = mediaType;
		this.serializer = serializer;
	}

	/**
	 * Returns the media type of the serializers that matched the HTTP <c>Accept</c> header value.
	 *
	 * @return The media type of the match.
	 */
	public MediaType getMediaType() {
		return mediaType;
	}

	/**
	 * Returns the serializer that matched the HTTP <c>Accept</c> header value.
	 *
	 * @return The serializer of the match.
	 */
	public Serializer getSerializer() {
		return serializer;
	}
}
