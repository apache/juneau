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
import org.apache.juneau.http.UnmodifiableBean;
import org.apache.juneau.http.classic.entity.*;
import org.apache.juneau.http.classic.header.*;

/**
 * A streamed, non-repeatable resource that obtains its content from an {@link Reader}.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauRestCommon">juneau-rest-common Basics</a>
 * </ul>
 */
public class ReaderResource extends BasicResource<ReaderResource> {

	/**
	 * Constructor.
	 */
	public ReaderResource() {
		super(new ReaderEntity());
	}

	/**
	 * Constructor.
	 *
	 * @param contentType The entity content type.  Can be <jk>null</jk> to omit an explicit <c>Content-Type</c> header.
	 * @param contents The entity contents.  Can be <jk>null</jk>, in which case a {@link NullPointerException} is thrown when the resource's content is read.
	 */
	public ReaderResource(ContentType contentType, Reader contents) {
		super(new ReaderEntity(contentType, contents));
	}

	/**
	 * Copy constructor.
	 *
	 * @param copyFrom The bean being copied.  Must not be <jk>null</jk>.
	 */
	protected ReaderResource(ReaderResource copyFrom) {
		super(copyFrom);
	}

	@Override /* Overridden from BasicResource */
	public ReaderResource copy() {
		return new ReaderResource(this);
	}

	@Override /* Overridden from BasicResource */
	public ReaderResource unmodifiable() {
		return this instanceof UnmodifiableBean ? this : new Unmodifiable(this);
	}

	/**
	 * An unmodifiable snapshot of a {@link ReaderResource}.
	 */
	public static class Unmodifiable extends ReaderResource implements UnmodifiableBean {

		/**
		 * Constructor.
		 *
		 * @param copyFrom The bean to snapshot-copy.  Must not be <jk>null</jk>.
		 */
		@SuppressWarnings({
			"java:S1699" // Paradigm intentionally calls the overridable freeze() from the ctor to deep-freeze sub-beans.
		})
		protected Unmodifiable(ReaderResource copyFrom) {
			super(copyFrom);
			freeze();
		}

		@Override /* Overridden from BasicResource */
		protected ReaderResource modify(Runnable mutation) {
			throw uoex("Bean is unmodifiable.");
		}
	}
}