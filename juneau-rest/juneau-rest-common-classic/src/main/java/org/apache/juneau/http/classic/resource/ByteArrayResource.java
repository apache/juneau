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

import org.apache.juneau.http.UnmodifiableBean;
import org.apache.juneau.http.classic.entity.*;
import org.apache.juneau.http.classic.header.*;

/**
 * A repeatable resource that obtains its content from a byte array.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauRestCommon">juneau-rest-common Basics</a>
 * </ul>
 */
public class ByteArrayResource extends BasicResource<ByteArrayResource> {

	/**
	 * Constructor.
	 */
	public ByteArrayResource() {
		super(new ByteArrayEntity());
	}

	/**
	 * Constructor.
	 *
	 * @param contentType The entity content type.  Can be <jk>null</jk> to omit an explicit <c>Content-Type</c> header.
	 * @param contents The entity contents.  Can be <jk>null</jk> (treated as an empty byte array).
	 */
	public ByteArrayResource(ContentType contentType, byte[] contents) {
		super(new ByteArrayEntity(contentType, contents));
	}

	/**
	 * Copy constructor.
	 *
	 * @param copyFrom The bean being copied.  Must not be <jk>null</jk>.
	 */
	protected ByteArrayResource(ByteArrayResource copyFrom) {
		super(copyFrom);
	}

	@Override /* Overridden from BasicResource */
	public ByteArrayResource copy() {
		return new ByteArrayResource(this);
	}

	@Override /* Overridden from BasicResource */
	public ByteArrayResource unmodifiable() {
		return this instanceof UnmodifiableBean ? this : new Unmodifiable(this);
	}

	/**
	 * An unmodifiable snapshot of a {@link ByteArrayResource}.
	 */
	public static class Unmodifiable extends ByteArrayResource implements UnmodifiableBean {

		/**
		 * Constructor.
		 *
		 * @param copyFrom The bean to snapshot-copy.  Must not be <jk>null</jk>.
		 */
		@SuppressWarnings({
			"java:S1699" // Paradigm intentionally calls the overridable freeze() from the ctor to deep-freeze sub-beans.
		})
		protected Unmodifiable(ByteArrayResource copyFrom) {
			super(copyFrom);
			freeze();
		}

		@Override /* Overridden from BasicResource */
		protected ByteArrayResource modify(Runnable mutation) {
			throw uoex("Bean is unmodifiable.");
		}
	}
}