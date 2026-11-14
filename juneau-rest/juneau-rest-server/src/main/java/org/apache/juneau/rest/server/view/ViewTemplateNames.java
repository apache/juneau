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
package org.apache.juneau.rest.server.view;

/**
 * Shared template-name checks applied by the view bridges before a name reaches a templating engine.
 *
 * <p>
 * A template name is a path relative to the bridge's configured base path.  Names that carry a URL scheme
 * (or any {@code :}) or a backslash are never valid, and raw-served names must end in one of the extensions
 * registered for the engine.
 *
 * @since 10.0.0
 */
public final class ViewTemplateNames {

	private ViewTemplateNames() {}

	/**
	 * Returns {@code true} if the name is free of URL-scheme and path-separator tricks.
	 *
	 * @param name The template name.  May be {@code null}.
	 * @return {@code false} if the name contains {@code :}, a backslash, or a control character.
	 */
	public static boolean isPlainName(String name) {
		if (name == null)
			return true;
		for (var i = 0; i < name.length(); i++) {
			var c = name.charAt(i);
			if (c == ':' || c == '\\' || c <= 0x1f || c == 0x7f)
				return false;
		}
		return true;
	}

	/**
	 * Returns {@code true} if the name ends with one of the allowed extensions.
	 *
	 * @param name The template name.  Must not be {@code null}.
	 * @param extensions The allowed extensions, including the leading dot (e.g. {@code ".ftlh"}).
	 * @return {@code true} if the name has an allowed extension.
	 */
	public static boolean hasAllowedExtension(String name, String... extensions) {
		var lower = name.toLowerCase();
		for (var e : extensions)
			if (lower.endsWith(e))
				return true;
		return false;
	}

	/**
	 * Validates a template name for the typed-view path, throwing if it is not a plain name.
	 *
	 * @param name The template name.
	 * @throws IllegalArgumentException If the name is not plain.
	 */
	public static void requirePlainName(String name) {
		if (! isPlainName(name))
			throw new IllegalArgumentException("Template name contains a scheme or illegal character: " + name);
	}
}
