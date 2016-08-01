/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.utils;

import static com.ibm.juno.core.utils.ThrowableUtils.*;

import java.io.*;

/**
 * File utilities.
 */
public class FileUtils {

	/**
	 * Same as {@link File#mkdirs()} except throws a RuntimeExeption if directory could not be created.
	 *
	 * @param f The directory to create.  Must not be <jk>null</jk>.
	 * @param clean If <jk>true</jk>, deletes the contents of the directory if it already exists.
	 * @return The same file.
	 * @throws RuntimeException if directory could not be created.
	 */
	public static File mkdirs(File f, boolean clean) {
		assertFieldNotNull(f, "f");
		if (f.exists()) {
			if (clean) {
				if (! delete(f))
					throw new RuntimeException("Could not clean directory '"+f.getAbsolutePath()+"'");
			} else {
				return f;
			}
		}
		if (! f.mkdirs())
			throw new RuntimeException("Could not create directory '" + f.getAbsolutePath() + "'");
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
		assertFieldNotNull(path, "path");
		return mkdirs(new File(path), clean);
	}

	/**
	 * Recursively deletes a file or directory.
	 *
	 * @param f The file or directory to delete.
	 * @return <jk>true</jk> if file or directory was successfully deleted.
	 */
	public static boolean delete(File f) {
		if (f == null)
			return true;
		if (f.isDirectory()) {
			File[] cf = f.listFiles();
			if (cf != null)
				for (File c : cf)
					delete(c);
		}
		return f.delete();
	}

	/**
	 * Creates a file if it doesn't already exist using {@link File#createNewFile()}.
	 * Throws a {@link RuntimeException} if the file could not be created.
	 *
	 * @param f The file to create.
	 */
	public static void create(File f) {
		if (f.exists())
			return;
		try {
			if (! f.createNewFile())
				throw new RuntimeException("Could not create file '"+f.getAbsolutePath()+"'");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Updates the modified timestamp on the specified file.
	 * Method ensures that the timestamp changes even if it's been modified within the past millisecond.
	 *
	 * @param f The file to modify the modified timestamp on.
	 */
	public static void modifyTimestamp(File f) {
		long lm = f.lastModified();
		long l = System.currentTimeMillis();
		if (lm == l)
			l++;
		if (! f.setLastModified(l))
			throw new RuntimeException("Could not modify timestamp on file '"+f.getAbsolutePath()+"'");

		// Linux only gives 1s precision, so set the date 1s into the future.
		if (lm == f.lastModified()) {
			l += 1000;
			if (! f.setLastModified(l))
				throw new RuntimeException("Could not modify timestamp on file '"+f.getAbsolutePath()+"'");
		}
	}

	/**
	 * Create a temporary file with the specified name.
	 * <p>
	 * The name is broken into file name and suffix, and the parts
	 * are passed to {@link File#createTempFile(String, String)}.
	 * <p>
	 * {@link File#deleteOnExit()} is called on the resulting file before being returned by this method.
	 *
	 * @param name The file name
	 * @return A newly-created temporary file.
	 * @throws IOException
	 */
	public static File createTempFile(String name) throws IOException {
		String[] parts = name.split("\\.");
		File f = File.createTempFile(parts[0], "." + parts[1]);
		f.deleteOnExit();
		return f;
	}
}
