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
package org.apache.juneau.ng;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.jar.*;

/**
 * Test-only helper for enumerating concrete Java classes in a package on the classpath.
 *
 * <p>
 * Used by the parametric {@code Ng*_Test} classes to walk every public class in
 * {@code org.apache.juneau.http.response} / {@code .header} without having to hard-code
 * the (large and frequently-changing) list.
 */
public final class NgPackageScanner {

	private NgPackageScanner() {}

	/**
	 * Returns every concrete class in {@code packageName}, located via {@code sentinel}'s defining JAR/dir.
	 *
	 * <p>
	 * Scans the JAR or directory that contains {@code sentinel}, listing {@code .class} entries under the
	 * package directory and loading each via the system classloader. Excludes interfaces, abstract classes,
	 * inner classes, and anonymous classes.
	 *
	 * @param packageName Dotted package name to scan (no trailing slash). Must not be {@code null}.
	 * @param sentinel    A class that lives in the JAR/dir to scan (any class from the target module works).
	 *                    Must not be {@code null}.
	 * @return Sorted-by-name list of concrete classes found in the package.
	 * @throws Exception if the JAR/dir cannot be opened.
	 */
	public static List<Class<?>> enumerateConcreteClasses(String packageName, Class<?> sentinel) throws Exception {
		var location = sentinel.getProtectionDomain().getCodeSource().getLocation();
		var dirPath = packageName.replace('.', '/');
		var out = new ArrayList<Class<?>>();
		if (location.toString().endsWith(".jar")) {
			try (var jar = new JarFile(new File(location.toURI()))) {
				for (var e : (Iterable<JarEntry>) () -> jar.entries().asIterator()) {
					var name = e.getName();
					if (name.startsWith(dirPath + "/") && name.endsWith(".class") && ! name.contains("$")) {
						var rel = name.substring(dirPath.length() + 1, name.length() - ".class".length());
						if (rel.contains("/"))
							continue; // skip sub-packages
						maybeAdd(out, packageName + "." + rel);
					}
				}
			}
		} else {
			var dir = Path.of(URI.create(location.toString() + dirPath));
			if (! Files.isDirectory(dir))
				return out;
			try (var stream = Files.list(dir)) {
				stream.forEach(p -> {
					var f = p.getFileName().toString();
					if (f.endsWith(".class") && ! f.contains("$"))
						maybeAdd(out, packageName + "." + f.substring(0, f.length() - ".class".length()));
				});
			}
		}
		out.sort(Comparator.comparing(Class::getSimpleName));
		return out;
	}

	private static void maybeAdd(List<Class<?>> out, String fqn) {
		try {
			var c = Class.forName(fqn, false, NgPackageScanner.class.getClassLoader());
			if (c.isInterface() || java.lang.reflect.Modifier.isAbstract(c.getModifiers()))
				return;
			out.add(c);
		} catch (ClassNotFoundException ignored) {
			// Skip — not loadable from the test classpath.
		}
	}
}
