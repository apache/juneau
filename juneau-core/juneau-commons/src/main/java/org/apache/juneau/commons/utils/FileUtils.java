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
package org.apache.juneau.commons.utils;

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.Exceptions.*;
import static org.apache.juneau.commons.utils.IoUtils.*;
import static org.apache.juneau.commons.utils.ObjectUtils.*;
import static org.apache.juneau.commons.utils.StringUtils.*;
import static org.apache.juneau.commons.utils.StringUtils.isEmpty;
import static org.apache.juneau.commons.utils.ThrowableUtils.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * File utilities.
 *
 */
@SuppressWarnings({
	"java:S115" // Constants use UPPER_snakeCase convention
})
public class FileUtils {

	/**
	 * Prevents instantiation.
	 */
	private FileUtils() {}

	// Argument name constants for assertArgNotNull
	private static final String ARG_basePath = "basePath";
	private static final String ARG_f = "f";
	private static final String ARG_path = "path";
	private static final String ARG_rootDir = "rootDir";

	// Shared message text — kept identical across the two boundary-check sites so attackers
	// cannot distinguish "rejected by pre-existence check" from "rejected by symlink check".
	private static final String MSG_pathEscape = "Path escapes configured root directory.";

	/**
	 * Creates a file if it doesn't already exist using {@link File#createNewFile()}.
	 *
	 * <p>
	 * Throws a {@link RuntimeException} if the file could not be created.
	 *
	 * @param f The file to create.
	 */
	public static void create(File f) {
		if (f.exists())
			return;
		safe(() -> optional(f.createNewFile()).filter(x -> x).orElseThrow(() -> rex("Could not create file ''{0}''", f.getAbsolutePath())));
	}

	/**
	 * Create a temporary file with the specified name.
	 *
	 * <p>
	 * The name is broken into file name and suffix, and the parts are passed to
	 * {@link File#createTempFile(String, String)}.
	 *
	 * <p>
	 * {@link File#deleteOnExit()} is called on the resulting file before being returned by this method.
	 *
	 * @param name The file name
	 * @return A newly-created temporary file.
	 * @throws IOException Thrown by underlying stream.
	 */
	@SuppressWarnings({
		"java:S5443" // Controlled internal helper: uses JDK default temp dir, deleteOnExit; explicit POSIX perms aren't portable to Windows.
	})
	public static File createTempFile(String name) throws IOException {
		var parts = name.split("\\.");
		var f = File.createTempFile(parts[0], "." + parts[1]);
		f.deleteOnExit();
		return f;
	}

	/**
	 * Create a temporary file with the specified name and specified contents.
	 *
	 * <p>
	 * The name is broken into file name and suffix, and the parts are passed to
	 * {@link File#createTempFile(String, String)}.
	 *
	 * <p>
	 * {@link File#deleteOnExit()} is called on the resulting file before being returned by this method.
	 *
	 * @param name The file name
	 * @param contents The file contents.
	 * @return A newly-created temporary file.
	 * @throws IOException Thrown by underlying stream.
	 */
	public static File createTempFile(String name, String contents) throws IOException {
		var f = createTempFile(name);
		if (contents != null) {
			try (var r = new StringReader(contents); Writer w = new FileWriter(f)) {
				pipe(r, w);
				w.flush();
			}
		}
		// If contents is null, create an empty file
		return f;
	}

	/**
	 * Recursively deletes a file or directory.
	 *
	 * @param f The file or directory to delete.
	 * @return <jk>true</jk> if file or directory was successfully deleted.
	 */
	public static boolean deleteFile(File f) {
		if (f == null)
			return true;
		if (f.isDirectory()) {
			var cf = f.listFiles();
			if (isNotNull(cf))
				for (var c : cf)
					deleteFile(c);
		}
		try {
			Files.delete(f.toPath());
			return true;
		} catch (@SuppressWarnings("unused") IOException e) {
			return false;
		}
	}

	/**
	 * Returns <jk>true</jk> if the specified file exists in the specified directory.
	 *
	 * @param dir The directory.
	 * @param fileName The file name.
	 * @return <jk>true</jk> if the specified file exists in the specified directory.
	 */
	public static boolean fileExists(File dir, String fileName) {
		if (dir == null || fileName == null)
			return false;
		return Files.exists(dir.toPath().resolve(fileName));
	}

	/**
	 * Strips the extension from a file name.
	 *
	 * @param name The file name.
	 * @return The file name without the extension, or <jk>null</jk> if name was <jk>null</jk>.
	 */
	public static String getBaseName(String name) {
		if (name == null)
			return null;
		var i = name.lastIndexOf('.');
		if (i == -1)
			return name;
		return name.substring(0, i);
	}

	/**
	 * Returns the extension from a file name.
	 *
	 * @param name The file name.
	 * @return The the extension, or <jk>null</jk> if name was <jk>null</jk>.
	 */
	public static String getFileExtension(String name) {
		if (name == null)
			return null;
		var i = name.lastIndexOf('.');
		if (i == -1)
			return "";
		return name.substring(i + 1);
	}

	/**
	 * Given an arbitrary path, returns the file name portion of that path.
	 *
	 * @param path The path to check.
	 * @return The file name.
	 */
	public static String getFileName(String path) {
		if (isEmpty(path))
			return null;
		path = trimTrailingSlashes(path);
		if (isEmpty(path))
			return null;  // Path contained only slashes
		// Handle both forward slashes (Unix) and backslashes (Windows)
		var i = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
		return i == -1 ? path : path.substring(i + 1);
	}

	/**
	 * Returns <jk>true</jk> if the specified file name contains the specified extension.
	 *
	 * @param name The file name.
	 * @param ext The extension.
	 * @return <jk>true</jk> if the specified file name contains the specified extension.
	 */
	public static boolean hasExtension(String name, String ext) {
		if (name == null || ext == null)
			return false;
		return ext.equals(getFileExtension(name));
	}

	/**
	 * Same as {@link File#mkdirs()} except throws a RuntimeExeption if directory could not be created.
	 *
	 * @param f The directory to create.  Must not be <jk>null</jk>.
	 * @param clean If <jk>true</jk>, deletes the contents of the directory if it already exists.
	 * @return The same file.
	 * @throws RuntimeException if directory could not be created.
	 */
	public static File mkdirs(File f, boolean clean) {
		assertArgNotNull(ARG_f, f);
		if (f.exists()) {
			if (clean) {
				optional(deleteFile(f)).filter(x -> x).orElseThrow(() -> rex("Could not clean directory ''{0}''", f.getAbsolutePath()));
			} else {
				return f;
			}
		}
		optional(f.mkdirs()).filter(x -> x).orElseThrow(() -> rex("Could not create directory ''{0}''", f.getAbsolutePath()));
		return f;
	}

	/**
	 * Same as {@link #mkdirs(String, boolean)} but uses String path.
	 *
	 * @param path The path of the directory to create.  Must not be <jk>null</jk>
	 * @param clean If <jk>true</jk>, deletes the contents of the directory if it already exists.
	 * @return The directory.
	 */
	public static File mkdirs(String path, boolean clean) {
		assertArgNotNull(ARG_path, path);
		return mkdirs(new File(path), clean);
	}

	/**
	 * Resolves a user-supplied path string against a root directory with a strict
	 * filesystem-boundary check &mdash; rejects any path that escapes the root (via
	 * {@code ../}, absolute-path injection, symlinks pointing outside, etc.).
	 *
	 * <p>
	 * Resolves both {@code rootDir} and the resolved target via
	 * {@link Path#toRealPath Path.toRealPath()} + {@link Path#normalize Path.normalize()}
	 * to handle symlinks deterministically, then asserts {@code target.startsWith(root)}
	 * before returning the resolved {@link File}.
	 *
	 * <p>
	 * Symlinks <i>inside</i> {@code rootDir} that resolve <i>inside</i> {@code rootDir} are
	 * followed silently. Symlinks that resolve <i>outside</i> {@code rootDir} are rejected with
	 * {@link IllegalArgumentException} (callers should map to a {@code 403 Forbidden}).
	 *
	 * <p>
	 * Pre-existence semantics: when the resolved target does not exist on the filesystem, the
	 * helper returns {@link Optional#empty()} so the caller can map to a {@code 404 Not Found}.
	 * Boundary violation always wins over pre-existence &mdash; even a non-existent path that
	 * escapes {@code rootDir} (after {@code ../} normalization) is rejected with
	 * {@link IllegalArgumentException} rather than {@code Optional.empty()}.
	 *
	 * <p>
	 * Used by {@code DirectoryResource}, {@code LogsResource}, and any other resource that
	 * resolves an HTTP-supplied path under a configured root directory.
	 *
	 * @param rootDir The configured root directory. Must not be <jk>null</jk>.
	 * @param userPath The user-supplied path, relative to {@code rootDir}. May be
	 * 	<jk>null</jk> or empty &mdash; in which case the helper returns the resolved root
	 * 	itself.
	 * @return An {@link Optional} containing the resolved {@link File} if it exists inside
	 * 	{@code rootDir}, or {@link Optional#empty()} if the target does not exist.
	 * @throws IllegalArgumentException If the resolved target escapes {@code rootDir}, or if
	 * 	the supplied {@code userPath} is not a valid path string. Callers should map to
	 * 	{@code 403 Forbidden}.
	 */
	public static Optional<File> resolveSafely(File rootDir, String userPath) {
		assertArgNotNull(ARG_rootDir, rootDir);
		var root = canonicalizeRoot(rootDir.toPath());
		if (userPath == null || userPath.isEmpty())
			return optional(root.toFile());
		Path target;
		try {
			target = root.resolve(userPath).normalize();
		} catch (@SuppressWarnings("unused") InvalidPathException e) {
			throw iaex(MSG_pathEscape);
		}
		if (! target.startsWith(root))
			throw iaex(MSG_pathEscape);
		var f = target.toFile();
		if (! f.exists())
			return emptyOptional();
		try {
			if (! target.toRealPath().startsWith(root))
				throw iaex(MSG_pathEscape);
		} catch (@SuppressWarnings("unused") NoSuchFileException e) {
			return emptyOptional();
		} catch (IOException e) {
			throw rex(e, "Could not canonicalize ''{0}''", target);
		}
		return optional(f);
	}

	/**
	 * Virtual-path variant of {@link #resolveSafely(File, String)} for use with servlet-context
	 * paths, classpath resource lookups, or other virtual-path resolvers where no filesystem
	 * operation is desired.
	 *
	 * <p>
	 * Operates purely on the virtual path string &mdash; no {@code toRealPath()} call, no
	 * filesystem touch, no symlink resolution. Safe to use against base paths whose resources
	 * do not exist on the local filesystem at all (e.g. {@code /WEB-INF/views/} inside a
	 * Spring Boot fat jar, or a classpath URL).
	 *
	 * <p>
	 * Normalizes the combined path by walking segments and applying {@code ./} (drop) and
	 * {@code ../} (pop) semantics, then asserts the result stays under the normalized
	 * {@code basePath}. {@code ../} segments that would pop above {@code basePath} are
	 * rejected.
	 *
	 * <p>
	 * URL-encoding is the caller's responsibility &mdash; this helper treats the literal
	 * {@code %2e%2e} as a normal path segment (not as {@code ..}). Callers that receive
	 * user input from URLs should URL-decode before calling.
	 *
	 * @param basePath The configured base path (virtual; e.g. {@code "/WEB-INF/views/"}).
	 * 	Must not be <jk>null</jk> or blank.
	 * @param userPath The user-supplied path, relative to {@code basePath}. May be
	 * 	<jk>null</jk> or empty &mdash; in which case the helper returns the normalized
	 * 	{@code basePath}.
	 * @return The resolved virtual path string (always starting with {@code basePath}).
	 * @throws IllegalArgumentException If the resolved target escapes {@code basePath}, or if
	 * 	{@code basePath} is <jk>null</jk> or blank. Callers should map to
	 * 	{@code 403 Forbidden}.
	 */
	public static String resolveVirtualPathSafely(String basePath, String userPath) {
		assertArgNotNull(ARG_basePath, basePath);
		if (basePath.isBlank())
			throw iaex("basePath must not be blank.");
		var bp = normalizeVirtualPath(basePath);
		if (bp == null)
			throw iaex("basePath escapes its own root.");
		if (! bp.endsWith("/"))
			bp = bp + "/";
		if (userPath == null || userPath.isEmpty())
			return bp;
		var up = userPath.startsWith("/") ? userPath.substring(1) : userPath;
		var combined = normalizeVirtualPath(bp + up);
		if (combined == null || ! combined.startsWith(bp))
			throw iaex(MSG_pathEscape);
		return combined;
	}

	/**
	 * Resolves {@code rootDir} via {@link Path#toRealPath()} so symlinks at the root itself
	 * are normalized once per call. Falls back to {@link Path#toAbsolutePath()} +
	 * {@link Path#normalize()} when the directory does not exist at the moment of the call
	 * (rare; legacy contract tolerated this).
	 */
	private static Path canonicalizeRoot(Path rootDir) {
		try {
			return rootDir.toRealPath();
		} catch (@SuppressWarnings("unused") IOException e) {
			return rootDir.toAbsolutePath().normalize();
		}
	}

	/**
	 * String-level path normalization for forward-slash separated virtual paths.
	 * Returns {@code null} if a {@code ..} segment would pop above the root.
	 */
	@SuppressWarnings({
		"java:S3776", // Cognitive complexity: small loop with three early-exit cases; splitting hurts readability.
		"java:S135" // Multiple continue statements are intrinsic to this path-segment scan/normalization loop.
	})
	private static String normalizeVirtualPath(String path) {
		if (path == null)
			return null;
		var startsWithSlash = path.startsWith("/");
		var endsWithSlash = path.length() > 1 && path.endsWith("/");
		var segments = new ArrayList<String>();
		for (var s : path.split("/")) {
			if (s.isEmpty() || ".".equals(s))
				continue;
			if ("..".equals(s)) {
				if (segments.isEmpty())
					return null;
				segments.remove(segments.size() - 1);
				continue;
			}
			segments.add(s);
		}
		var sb = new StringBuilder();
		if (startsWithSlash)
			sb.append('/');
		sb.append(String.join("/", segments));
		if (endsWithSlash && (sb.isEmpty() || sb.charAt(sb.length() - 1) != '/'))
			sb.append('/');
		return sb.toString();
	}

	/**
	 * Updates the modified timestamp on the specified file.
	 *
	 * <p>
	 * Method ensures that the timestamp changes even if it's been modified within the past millisecond.
	 *
	 * @param f The file to modify the modified timestamp on.
	 */
	public static void modifyTimestamp(File f) {
		var lm = f.lastModified();
		var l = System.currentTimeMillis();
		if (lm == l)
			l++;
		optional(f.setLastModified(l)).filter(x -> x).orElseThrow(() -> rex("Could not modify timestamp on file ''{0}''", f.getAbsolutePath()));

		// Linux only gives 1s precision, so set the date 1s into the future.
		if (lm == f.lastModified())
			optional(f.setLastModified(l + 1000)).filter(x -> x).orElseThrow(() -> rex("Could not modify timestamp on file ''{0}''", f.getAbsolutePath()));
	}
}