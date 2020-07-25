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
package org.apache.juneau.cp;

import static org.apache.juneau.internal.IOUtils.*;
import static org.apache.juneau.internal.ObjectUtils.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.internal.*;
import org.apache.juneau.parser.*;

/**
 * Class for retrieving and caching resource files from the classpath.
 */
public final class ResourceManager {

	// Maps resource names+locales to found resources.
	private final ConcurrentHashMap<ResourceKey,byte[]> byteCache;
	private final ConcurrentHashMap<ResourceKey,String> stringCache;

	private final Class<?> baseClass;
	private final ResourceFinder resourceFinder;
	private final boolean useCache;

	/**
	 * Constructor.
	 *
	 * @param baseClass The default class to use for retrieving resources from the classpath.
	 * @param resourceFinder The resource finder implementation.
	 * @param useCache If <jk>true</jk>, retrieved resources are stored in an in-memory cache for fast lookup.
	 */
	public ResourceManager(Class<?> baseClass, ResourceFinder resourceFinder, boolean useCache) {
		this.baseClass = baseClass;
		this.resourceFinder = resourceFinder;
		this.useCache = useCache;
		if (useCache) {
			this.byteCache = new ConcurrentHashMap<>();
			this.stringCache = new ConcurrentHashMap<>();
		} else {
			this.byteCache = null;
			this.stringCache = null;
		}
	}

	/**
	 * Constructor.
	 *
	 * <p>
	 * Uses default {@link BasicResourceFinder} for finding resources.
	 *
	 * @param baseClass The default class to use for retrieving resources from the classpath.
	 */
	public ResourceManager(Class<?> baseClass) {
		this(baseClass, BasicResourceFinder.INSTANCE, false);
	}

	/**
	 * Finds the resource with the given name.
	 *
	 * @param name Name of the desired resource.
	 * @return An input stream to the object, or <jk>null</jk> if the resource could not be found.
	 * @throws IOException Thrown by underlying stream.
	 */
	public InputStream getStream(String name) throws IOException {
		return getStream(name, null);
	}

	/**
	 * Finds the resource with the given name for the specified locale and returns it as an input stream.
	 *
	 * @param name Name of the desired resource.
	 * @param locale The locale.  Can be <jk>null</jk>.
	 * @return An input stream to the object, or <jk>null</jk> if the resource could not be found.
	 * @throws IOException Thrown by underlying stream.
	 */
	public InputStream getStream(String name, Locale locale) throws IOException {

		if (isEmpty(name))
			return null;

		if (! useCache)
			return resourceFinder.findResource(baseClass, name, locale);

		ResourceKey key = new ResourceKey(name, locale);

		byte[] r = byteCache.get(key);
		if (r == null) {
			try (InputStream is = resourceFinder.findResource(baseClass, name, locale)) {
				if (is != null)
					byteCache.putIfAbsent(key, IOUtils.readBytes(is, 1024));
			}
		}

		r = byteCache.get(key);
		return r == null ? null : new ByteArrayInputStream(r);
	}

	/**
	 * Finds the resource with the given name and converts it to a simple string.
	 *
	 * @param name Name of the desired resource.
	 * @return The resource converted to a string, or <jk>null</jk> if the resource could not be found.
	 * @throws IOException Thrown by underlying stream.
	 */
	public String getString(String name) throws IOException {
		return getString(name, null);
	}

	/**
	 * Finds the resource with the given name and converts it to a simple string.
	 *
	 * @param name Name of the desired resource.
	 * @param locale The locale.  Can be <jk>null</jk>.
	 * @return The resource converted to a string, or <jk>null</jk> if the resource could not be found.
	 * @throws IOException Thrown by underlying stream.
	 */
	public String getString(String name, Locale locale) throws IOException {

		if (isEmpty(name))
			return null;

		if (! useCache) {
			try (InputStream is = resourceFinder.findResource(baseClass, name, locale)) {
				return IOUtils.read(is, IOUtils.UTF8);
			}
		}

		ResourceKey key = new ResourceKey(name, locale);

		String r = stringCache.get(key);
		if (r == null) {
			try (InputStream is = resourceFinder.findResource(baseClass, name, locale)) {
				if (is != null)
					stringCache.putIfAbsent(key, IOUtils.read(is, IOUtils.UTF8));
			}
		}

		return stringCache.get(key);
	}

	/**
	 * Reads the input stream and parses it into a POJO using the specified parser.
	 *
	 * @param c The class type of the POJO to create.
	 * @param parser The parser to use to parse the stream.
	 * @param name The resource name (e.g. "htdocs/styles.css").
	 * @return The parsed resource, or <jk>null</jk> if the resource could not be found.
	 * @throws IOException Thrown by underlying stream.
	 * @throws ParseException If stream could not be parsed using the specified parser.
	 */
	public <T> T getResource(Class<T> c, Parser parser, String name) throws IOException, ParseException {
		return getResource(c, parser, name, null);
	}

	/**
	 * Reads the input stream and parses it into a POJO using the specified parser.
	 *
	 * @param c The class type of the POJO to create.
	 * @param parser The parser to use to parse the stream.
	 * @param name The resource name (e.g. "htdocs/styles.css").
	 * @param locale
	 * 	Optional locale.
	 * 	<br>If <jk>null</jk>, won't look for localized file names.
	 * @return The parsed resource, or <jk>null</jk> if the resource could not be found.
	 * @throws IOException Thrown by underlying stream.
	 * @throws ParseException If stream could not be parsed using the specified parser.
	 */
	public <T> T getResource(Class<T> c, Parser parser, String name, Locale locale) throws IOException, ParseException {

		InputStream is = getStream(name, locale);
		if (is == null)
			return null;
		try (Closeable in = parser.isReaderParser() ? new InputStreamReader(is, UTF8) : is) {
			return parser.parse(in, c);
		}
	}

	private class ResourceKey {
		final String name;
		final Locale locale;

		ResourceKey(String name, Locale locale) {
			this.name = name;
			this.locale = locale;
		}

		@Override
		public int hashCode() {
			return name.hashCode() + (locale == null ? 0 : locale.hashCode());
		}

		@Override
		public boolean equals(Object o) {
			return eq(this, (ResourceKey)o, (x,y)->eq(x.name, y.name) && eq(x.locale, y.locale));
		}
	}
}
