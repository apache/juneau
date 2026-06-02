/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.html;

import org.apache.juneau.commons.http.MediaType;
import static org.apache.juneau.commons.utils.Utils.*;

import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.commons.svl.*;

/**
 * Session object that lives for the duration of a single use of {@link HtmlStrippedDocSerializer}.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='warn'>This class is not thread safe and is typically discarded after one use.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/HtmlBasics">HTML Basics</a>
 * </ul>
 */
@SuppressWarnings({
	"java:S3740" // Raw ObjectMap/ClassMeta types propagated from parent serializer session hierarchy where type parameters are erased
})
public class HtmlStrippedDocSerializerSession extends HtmlSerializerSession {

	/**
	 * Builder class.
	 */
	public abstract static class Builder<SELF extends Builder<SELF>> extends HtmlSerializerSession.Builder<SELF> {

		/**
		 * Constructor
		 *
		 * @param ctx The context creating this session.
		 */
		protected Builder(HtmlStrippedDocSerializer ctx) {
			super(ctx);
		}

		@Override
		public HtmlStrippedDocSerializerSession build() {
			return new HtmlStrippedDocSerializerSession(this);
		}

	}

	/**
	 * Concrete default builder leaf for the non-subclassed {@code create()} path (CRTP terminal).
	 */
	public static final class DefaultBuilder extends Builder<DefaultBuilder> {

		DefaultBuilder(HtmlStrippedDocSerializer ctx) {
			super(ctx);
		}
	}

	/**
	 * Creates a new builder for this object.
	 *
	 * @param ctx The context creating this session.
	 * @return A new builder.
	 */
	public static Builder<?> create(HtmlStrippedDocSerializer ctx) {
		return new DefaultBuilder(ctx);
	}

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	protected HtmlStrippedDocSerializerSession(Builder<?> builder) {
		super(builder);
	}

	@SuppressWarnings({
		"resource" // w is closed by try-with-resources; analyzer false positive on super.doSerialize path
	})
	@Override /* Overridden from SerializerSession */
	protected void doSerialize(SerializerPipe out, Object o) throws IOException, SerializeException {
		try (var w = getHtmlWriter(out)) {
			if (o == null || (o instanceof Collection o2 && o2.isEmpty()) || (isArray(o) && Array.getLength(o) == 0))
				w.sTag(1, "p").append("No Results").eTag("p").nl(1);
			else
				super.doSerialize(out, o);
		}
	}
}