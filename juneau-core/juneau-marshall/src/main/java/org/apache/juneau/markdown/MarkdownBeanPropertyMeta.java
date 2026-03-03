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

import org.apache.juneau.*;
import org.apache.juneau.commons.reflect.*;
import org.apache.juneau.markdown.annotation.*;

/**
 * Metadata on bean properties specific to the Markdown serializers and parsers pulled from the
 * {@link Markdown @Markdown} annotation on the bean property.
 *
 */
public class MarkdownBeanPropertyMeta extends ExtendedBeanPropertyMeta {

	/**
	 * Default instance.
	 */
	public static final MarkdownBeanPropertyMeta DEFAULT = new MarkdownBeanPropertyMeta();

	private final MarkdownFormat format;
	private final String heading;
	private final boolean noTables;
	private final boolean noHeaders;
	private final boolean code;
	private final String link;

	/**
	 * Constructor.
	 *
	 * @param bpm The metadata of the bean property of this additional metadata.
	 * @param mp Markdown metadata provider (for finding information about other artifacts).
	 */
	public MarkdownBeanPropertyMeta(BeanPropertyMeta bpm, MarkdownMetaProvider mp) {
		super(bpm);
		var a = bpm.getAnnotations(Markdown.class).map(AnnotationInfo::inner).reduce((first, second) -> second).orElse(null);
		format = a != null ? a.format() : MarkdownFormat.DEFAULT;
		heading = a != null ? a.heading() : "";
		noTables = a != null && a.noTables();
		noHeaders = a != null && a.noHeaders();
		code = a != null && a.code();
		link = a != null ? a.link() : "";
	}

	private MarkdownBeanPropertyMeta() {
		super(null);
		format = MarkdownFormat.DEFAULT;
		heading = "";
		noTables = false;
		noHeaders = false;
		code = false;
		link = "";
	}

	/**
	 * Returns the rendering format for this property.
	 *
	 * @return The format.
	 */
	public MarkdownFormat getFormat() {
		return format;
	}

	/**
	 * Returns the custom heading text for document-mode rendering.
	 *
	 * @return The heading, or empty string if not specified.
	 */
	public String getHeading() {
		return heading;
	}

	/**
	 * Returns whether tables should be suppressed for this property's collection value.
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

	/**
	 * Returns whether the value should be rendered as inline code.
	 *
	 * @return Whether code rendering is enabled.
	 */
	public boolean isCode() {
		return code;
	}

	/**
	 * Returns the link template for rendering this property as a Markdown link.
	 *
	 * @return The link template, or empty string if not specified.
	 */
	public String getLink() {
		return link;
	}
}
