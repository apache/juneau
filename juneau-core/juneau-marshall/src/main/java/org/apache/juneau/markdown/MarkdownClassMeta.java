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
package org.apache.juneau.markdown;

import java.util.concurrent.atomic.*;

import org.apache.juneau.*;
import org.apache.juneau.markdown.annotation.*;

/**
 * Metadata on classes specific to the Markdown serializers and parsers pulled from the {@link Markdown @Markdown}
 * annotation on the class.
 *
 */
public class MarkdownClassMeta extends ExtendedClassMeta {

	private final MarkdownFormat format;
	private final boolean noTables;
	private final boolean noHeaders;

	/**
	 * Constructor.
	 *
	 * @param cm The class that this annotation is defined on.
	 * @param mp Markdown metadata provider (for finding information about other artifacts).
	 */
	public MarkdownClassMeta(ClassMeta<?> cm, MarkdownMetaProvider mp) {
		super(cm);
		var ref = new AtomicReference<Markdown>();
		cm.forEachAnnotation(Markdown.class, null, ref::set);
		var a = ref.get();
		format = a != null ? a.format() : MarkdownFormat.DEFAULT;
		noTables = a != null && a.noTables();
		noHeaders = a != null && a.noHeaders();
	}

	/**
	 * Returns the rendering format for this class.
	 *
	 * @return The format.
	 */
	public MarkdownFormat getFormat() {
		return format;
	}

	/**
	 * Returns whether tables should be suppressed for collections of this type.
	 *
	 * @return Whether no-tables is set.
	 */
	public boolean isNoTables() {
		return noTables;
	}

	/**
	 * Returns whether table header rows should be suppressed.
	 *
	 * @return Whether no-headers is set.
	 */
	public boolean isNoHeaders() {
		return noHeaders;
	}
}
