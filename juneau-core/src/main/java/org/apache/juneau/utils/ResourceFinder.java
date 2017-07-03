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

import static org.apache.juneau.internal.FileUtils.*;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.ResourceBundle.*;
import java.util.concurrent.*;

import org.apache.juneau.internal.*;

/**
 * Utility class for finding resources for a class.
 *
 * <p>
 * Same as {@link Class#getResourceAsStream(String)} except if it doesn't find the resource on this class, searches
 * up the parent hierarchy chain.
 *
 * <p>
 * If the resource cannot be found in the classpath, then an attempt is made to look in the JVM working directory.
 * <br>Path traversals outside the working directory are not allowed for security reasons.
 *
 * <p>
 * If the <code>locale</code> is specified, then we look for resources whose name matches that locale.
 * For example, if looking for the resource <js>"MyResource.txt"</js> for the Japanese locale, we will look for
 * files in the following order:
 * <ol>
 * 	<li><js>"MyResource_ja_JP.txt"</js>
 * 	<li><js>"MyResource_ja.txt"</js>
 * 	<li><js>"MyResource.txt"</js>
 * </ol>
 *
 * <p>
 * Results are cached for fast lookup.
 */
public class ResourceFinder {

	private static final ResourceBundle.Control RB_CONTROL = ResourceBundle.Control.getControl(Control.FORMAT_DEFAULT);
	private static final List<Locale> ROOT_LOCALE = Arrays.asList(Locale.ROOT);

	// Maps resource names+locales to found resources.
	private final ConcurrentHashMap<ResourceKey,Resource> cache = new ConcurrentHashMap<ResourceKey,Resource>();

	// Maps resolved URLs to resources.
	private final ConcurrentHashMap<URL,Resource> cacheByUrl = new ConcurrentHashMap<URL,Resource>();

	private final Class<?> c;

	/**
	 * Constructor.
	 *
	 * @param forClass The class that this resource finder searches against.
	 */
	public ResourceFinder(Class<?> forClass) {
		this.c = forClass;
	}

	/**
	 * Finds the resource with the given name.
	 *
	 * @param name Name of the desired resource.
	 * @return An input stream to the object, or <jk>null</jk> if the resource could not be found.
	 * @throws IOException
	 */
	public InputStream getResourceAsStream(String name) throws IOException {
		return getResourceAsStream(name, null);
	}

	/**
	 * Finds the resource with the given name for the specified locale.
	 *
	 * @param name Name of the desired resource.
	 * @param locale The locale.  Can be <jk>null</jk>.
	 * @return An input stream to the object, or <jk>null</jk> if the resource could not be found.
	 * @throws IOException
	 */
	public InputStream getResourceAsStream(String name, Locale locale) throws IOException {
		return getResource(name, locale).asInputStream();
	}

	/**
	 * Finds the resource with the given name and converts it to a simple string.
	 *
	 * @param name Name of the desired resource.
	 * @return The resource converted to a string, or <jk>null</jk> if the resource could not be found.
	 * @throws IOException
	 */
	public String getResourceAsString(String name) throws IOException {
		return getResourceAsString(name, null);
	}

	/**
	 * Finds the resource with the given name and converts it to a simple string.
	 *
	 * @param name Name of the desired resource.
	 * @param locale The locale.  Can be <jk>null</jk>.
	 * @return The resource converted to a string, or <jk>null</jk> if the resource could not be found.
	 * @throws IOException
	 */
	public String getResourceAsString(String name, Locale locale) throws IOException {
		return getResource(name, locale).asString();
	}


	//-------------------------------------------------------------------------------------------------------------------
	// Support classes and methods.
	//-------------------------------------------------------------------------------------------------------------------

	private static class ResourceKey {
		private final String name;
		private final Locale locale;

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

	private static class Resource {
		private byte[] bytes;
		private String string;

		private Resource(byte[] bytes) {
			this.bytes = bytes;
		}

		private String asString() {
			if (bytes == null)
				return null;
			try {
				if (string == null)
					string = new String(bytes, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			return string;
		}

		private InputStream asInputStream() {
			if (bytes == null)
				return null;
			return new ByteArrayInputStream(bytes);
		}
	}

	private Resource getResource(String name, Locale locale) throws IOException {
		ResourceKey key = new ResourceKey(name, locale);
		Resource r = cache.get(key);
		if (r != null)
			return r;

		r = findResource(c, key);
		cache.putIfAbsent(key, r);
		r = cache.get(key);
		return r;
	}

	private Resource findResource(Class<?> c2, ResourceKey key) throws IOException {
		while (c2 != null) {
			if (key.locale == null) {
				URL url = c2.getResource(key.name);
				if (url != null) {
					Resource r = cacheByUrl.get(url);
					if (r == null) {
						r = new Resource(IOUtils.readBytes(c2.getResourceAsStream(key.name), 1024));
						cacheByUrl.putIfAbsent(url, r);
						r = cacheByUrl.get(url);
					}
					return r;
				}
			} else {
				for (String n : getCandidateFileNames(key.name, key.locale)) {
					URL url = c2.getResource(n);
					if (url != null) {
						Resource r = cacheByUrl.get(url);
						if (r == null) {
							r = new Resource(IOUtils.readBytes(c2.getResourceAsStream(key.name), 1024));
							cacheByUrl.putIfAbsent(url, r);
							r = cacheByUrl.get(url);
						}
						return r;
					}
				}
			}
			c2 = c2.getSuperclass();
		}

		if (key.name.indexOf("..") == -1) {
			for (String n2 : getCandidateFileNames(key.name, key.locale)) {
				File f = new File(n2);
				if (f.exists() && f.canRead() && ! f.isAbsolute()) {
					URL url = f.toURI().toURL();
					Resource r = cacheByUrl.get(url);
					if (r == null) {
						r = new Resource(IOUtils.readBytes(new FileInputStream(f), 1024));
						cacheByUrl.putIfAbsent(url, r);
						r = cacheByUrl.get(url);
					}
					return r;
				}
			}
		}

		return new Resource(null);
	}

	/**
	 * Returns the candidate file names for the specified file name in the specified locale.
	 *
	 * <p>
	 * For example, if looking for the <js>"MyResource.txt"</js> file in the Japanese locale, the iterator will return
	 * names in the following order:
	 * <ol>
	 * 	<li><js>"MyResource_ja_JP.txt"</js>
	 * 	<li><js>"MyResource_ja.txt"</js>
	 * 	<li><js>"MyResource.txt"</js>
	 * </ol>
	 *
	 * <p>
	 * If the locale is null, then it will only return <js>"MyResource.txt"</js>.
	 *
	 * @param fileName The name of the file to get candidate file names on.
	 * @param l The locale.
	 * @return An iterator of file names to look at.
	 */
	private static Iterable<String> getCandidateFileNames(final String fileName, final Locale l) {
		return new Iterable<String>() {
			@Override
			public Iterator<String> iterator() {
				return new Iterator<String>() {
					final Iterator<Locale> locales = getCandidateLocales(l).iterator();
					String baseName, ext;

					@Override
					public boolean hasNext() {
						return locales.hasNext();
					}

					@Override
					public String next() {
						Locale l2 = locales.next();
						if (l2.toString().isEmpty())
							return fileName;
						if (baseName == null)
							baseName = getBaseName(fileName);
						if (ext == null)
							ext = getExtension(fileName);
						return baseName + "_" + l2.toString() + (ext.isEmpty() ? "" : ('.' + ext));
					}
					@Override
					public void remove() {
						throw new UnsupportedOperationException();
					}
				};
			}
		};
	}

	/**
	 * Returns the candidate locales for the specified locale.
	 *
	 * <p>
	 * For example, if <code>locale</code> is <js>"ja_JP"</js>, then this method will return:
	 * <ol>
	 * 	<li><js>"ja_JP"</js>
	 * 	<li><js>"ja"</js>
	 * 	<li><js>""</js>
	 * </ol>
	 *
	 * @param locale The locale to get the list of candidate locales for.
	 * @return The list of candidate locales.
	 */
	private static List<Locale> getCandidateLocales(Locale locale) {
		if (locale == null)
			return ROOT_LOCALE;
		return RB_CONTROL.getCandidateLocales("", locale);
	}
}
