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
package org.apache.juneau.ini;

import org.apache.juneau.*;
import org.apache.juneau.commons.reflect.*;
import org.apache.juneau.ini.annotation.*;

/**
 * Metadata on bean properties specific to INI serializers and parsers, from {@link Ini @Ini}.
 */
public class IniBeanPropertyMeta extends ExtendedBeanPropertyMeta {

	/** Default instance. */
	public static final IniBeanPropertyMeta DEFAULT = new IniBeanPropertyMeta();

	private final String section;
	private final String comment;
	private final boolean json5Encoding;

	/**
	 * Constructor.
	 *
	 * @param bpm The bean property metadata.
	 * @param mp INI metadata provider.
	 */
	public IniBeanPropertyMeta(BeanPropertyMeta bpm, IniMetaProvider mp) {
		super(bpm);
		var a = bpm.getAnnotations(Ini.class).map(AnnotationInfo::inner).reduce((first, second) -> second).orElse(null);
		section = a != null && !a.section().isEmpty() ? a.section() : "";
		comment = a != null && !a.comment().isEmpty() ? a.comment() : "";
		json5Encoding = a != null && a.json5Encoding();
	}

	private IniBeanPropertyMeta() {
		super(null);
		section = "";
		comment = "";
		json5Encoding = false;
	}

	/**
	 * Returns the custom section name for this property.
	 *
	 * @return The section name, or empty string if not specified.
	 */
	public String getSection() {
		return section;
	}

	/**
	 * Returns the comment text to emit before this property.
	 *
	 * @return The comment, or empty string if not specified.
	 */
	public String getComment() {
		return comment;
	}

	/**
	 * Returns whether this property should be JSON5-encoded even when normally simple.
	 *
	 * @return Whether JSON5 encoding is forced.
	 */
	public boolean isJson5Encoding() {
		return json5Encoding;
	}
}
