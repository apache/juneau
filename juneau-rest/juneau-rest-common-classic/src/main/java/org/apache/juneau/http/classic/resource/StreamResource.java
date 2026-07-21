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
package org.apache.juneau.http.classic.resource;

import static org.apache.juneau.commons.utils.Shorts.*;

import java.io.*;
import org.apache.http.*;
import org.apache.juneau.http.UnmodifiableBean;
import org.apache.juneau.http.classic.entity.*;
import org.apache.juneau.http.classic.header.*;

/**
 * A streamed, non-repeatable resource that obtains its content from an {@link InputStream}.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauRestCommon">juneau-rest-common Basics</a>
 * </ul>
 */
public class StreamResource extends BasicResource<StreamResource> {

	/**
	 * Constructor.
	 */
	public StreamResource() {
		super(new StreamEntity());
	}

	/**
	 * Constructor.
	 *
	 * @param contentType The entity content type.  Can be <jk>null</jk>.
	 * @param contents The entity contents.  Can be <jk>null</jk>.
	 */
	public StreamResource(ContentType contentType, InputStream contents) {
		super(new StreamEntity(contentType, contents));
	}

	/**
	 * Copy constructor.
	 *
	 * @param copyFrom The bean being copied.  Must not be <jk>null</jk>.
	 */
	protected StreamResource(StreamResource copyFrom) {
		super(copyFrom);
	}

	/**
	 * Constructor.
	 *
	 * <p>
	 * This is the constructor used when converting an HTTP response into a resource.
	 *
	 * @param response The HTTP response to copy from.  Must not be <jk>null</jk>.
	 * @throws IOException Rethrown from {@link HttpEntity#getContent()}.
	 */
	public StreamResource(HttpResponse response) throws IOException {
		super(response);
	}

	@Override /* Overridden from BasicResource */
	public StreamResource copy() {
		return new StreamResource(this);
	}

	@Override /* Overridden from BasicResource */
	public StreamResource unmodifiable() {
		return this instanceof UnmodifiableBean ? this : new Unmodifiable(this);
	}

	/**
	 * An unmodifiable snapshot of a {@link StreamResource}.
	 */
	public static class Unmodifiable extends StreamResource implements UnmodifiableBean {

		/**
		 * Constructor.
		 *
		 * @param copyFrom The bean to snapshot-copy.  Must not be <jk>null</jk>.
		 */
		@SuppressWarnings({
			"java:S1699" // Paradigm intentionally calls the overridable freeze() from the ctor to deep-freeze sub-beans.
		})
		protected Unmodifiable(StreamResource copyFrom) {
			super(copyFrom);
			freeze();
		}

		@Override /* Overridden from BasicResource */
		protected StreamResource modify(Runnable mutation) {
			throw uoex("Bean is unmodifiable.");
		}
	}
}