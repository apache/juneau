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
package org.apache.juneau.utils;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.internal.*;

/**
 * Class for retrieving and caching resource files from the classpath.
 */
public final class ClasspathResourceManager {

	// Maps resource names+locales to found resources.
	private final ConcurrentHashMap<ResourceKey,byte[]> byteCache;
	private final ConcurrentHashMap<ResourceKey,String> stringCache;

	private final Class<?> baseClass;
	private final ClasspathResourceFinder resourceFinder;
	private final boolean useCache;

	/**
	 * Constructor.
	 *
	 * @param baseClass The default class to use for retrieving resources from the classpath.
	 * @param resourceFinder The resource finder implementation.
	 * @param useCache If <jk>true</jk>, retrieved resources are stored in an in-memory cache for fast lookup.
	 */
	public ClasspathResourceManager(Class<?> baseClass, ClasspathResourceFinder resourceFinder, boolean useCache) {
		this.baseClass = baseClass;
		this.resourceFinder = resourceFinder;
		this.useCache = useCache;
		this.byteCache = useCache ? new ConcurrentHashMap<ResourceKey,byte[]>() : null;
		this.stringCache = useCache ? new ConcurrentHashMap<ResourceKey,String>() : null;
	}

	/**
	 * Constructor.
	 *
	 * <p>
	 * Uses default {@link ClasspathResourceFinderBasic} for finding resources.
	 *
	 * @param baseClass The default class to use for retrieving resources from the classpath.
	 */
	public ClasspathResourceManager(Class<?> baseClass) {
		this(baseClass, new ClasspathResourceFinderBasic(), false);
	}

	/**
	 * Finds the resource with the given name.
	 *
	 * @param name Name of the desired resource.
	 * @return An input stream to the object, or <jk>null</jk> if the resource could not be found.
	 * @throws IOException
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
	 * @throws IOException
	 */
	public InputStream getStream(String name, Locale locale) throws IOException {
		return getStream(baseClass, name, locale);
	}

	/**
	 * Finds the resource with the given name for the specified locale and returns it as an input stream.
	 *
	 * @param baseClass
	 * 	Overrides the default class to use for retrieving the classpath resource.
	 * 	<br>If <jk>null<jk>, uses the base class passed in through the constructor of this class.
	 * @param name Name of the desired resource.
	 * @param locale The locale.  Can be <jk>null</jk>.
	 * @return An input stream to the object, or <jk>null</jk> if the resource could not be found.
	 * @throws IOException
	 */
	public InputStream getStream(Class<?> baseClass, String name, Locale locale) throws IOException {

		if (baseClass == null)
			baseClass = this.baseClass;

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
	 * @throws IOException
	 */
	public String getString(String name) throws IOException {
		return getString(baseClass, name, null);
	}

	/**
	 * Finds the resource with the given name and converts it to a simple string.
	 *
	 * @param baseClass
	 * 	Overrides the default class to use for retrieving the classpath resource.
	 * 	<br>If <jk>null<jk>, uses the base class passed in through the constructor of this class.
	 * @param name Name of the desired resource.
	 * @return The resource converted to a string, or <jk>null</jk> if the resource could not be found.
	 * @throws IOException
	 */
	public String getString(Class<?> baseClass, String name) throws IOException {
		return getString(baseClass, name, null);
	}

	/**
	 * Finds the resource with the given name and converts it to a simple string.
	 *
	 * @param name Name of the desired resource.
	 * @param locale The locale.  Can be <jk>null</jk>.
	 * @return The resource converted to a string, or <jk>null</jk> if the resource could not be found.
	 * @throws IOException
	 */
	public String getString(String name, Locale locale) throws IOException {
		return getString(baseClass, name, locale);
	}

	/**
	 * Finds the resource with the given name and converts it to a simple string.
	 *
	 * @param baseClass
	 * 	Overrides the default class to use for retrieving the classpath resource.
	 * 	<br>If <jk>null<jk>, uses the base class passed in through the constructor of this class.
	 * @param name Name of the desired resource.
	 * @param locale The locale.  Can be <jk>null</jk>.
	 * @return The resource converted to a string, or <jk>null</jk> if the resource could not be found.
	 * @throws IOException
	 */
	public String getString(Class<?> baseClass, String name, Locale locale) throws IOException {

		if (baseClass == null)
			baseClass = this.baseClass;

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
			if (o == null)
				return false;
			ResourceKey ok = (ResourceKey)o;
			return ObjectUtils.equals(name, ok.name) && ObjectUtils.equals(locale, ok.locale);
		}
	}
}
