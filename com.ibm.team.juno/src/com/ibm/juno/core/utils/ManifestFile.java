/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2016. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.utils;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.jar.*;

import com.ibm.juno.core.*;

/**
 * Utility class for working with Jar manifest files.
 * <p>
 * Copies the contents of a {@link Manifest} into an {@link ObjectMap} so that the various
 * 	convenience methods on that class can be used to retrieve values.
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
public class ManifestFile extends ObjectMap {

	private static final long serialVersionUID = 1L;
	private transient volatile StringVar stringVar;

	/**
	 * Create an instance of this class from a manifest file on the file system.
	 *
	 * @param f The manifest file.
	 * @throws IOException If a problem occurred while trying to read the manifest file.
	 */
	public ManifestFile(File f) throws IOException {
		Manifest mf = new Manifest();
		FileInputStream fis = new FileInputStream(f);
		try {
			mf.read(fis);
			load(mf);
		} catch (IOException e) {
			throw new IOException("Problem detected in MANIFEST.MF.  Contents below:\n" + IOUtils.read(f), e);
		} finally {
			IOUtils.closeQuietly(fis);
		}
	}

	/**
	 * Create an instance of this class from a {@link Manifest} object.
	 *
	 * @param f The manifest to read from.
	 */
	public ManifestFile(Manifest f) {
		load(f);
	}

	/**
	 * Finds and loads the manifest file of the jar file that the specified class is contained within.
	 *
	 * @param c The class to get the manifest file of.
	 * @throws IOException If a problem occurred while trying to read the manifest file.
	 */
	public ManifestFile(Class<?> c) throws IOException {
		String className = c.getSimpleName() + ".class";
		String classPath = c.getResource(className).toString();
		if (! classPath.startsWith("jar")) {
			return;
		}
		String manifestPath = classPath.substring(0, classPath.lastIndexOf("!") + 1) +  "/META-INF/MANIFEST.MF";
		try {
			Manifest mf = new Manifest(new URL(manifestPath).openStream());
			load(mf);
		} catch (MalformedURLException e) {
			throw new IOException(e);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Create an instance of a {@link StringVarWithDefault} that returns values in this manifest file.
	 *
	 * @return A new {@link StringVarWithDefault}.
	 */
	public StringVar getStringVar() {
		if (stringVar == null)
			stringVar = new StringMapVar(this);
		return stringVar;
	}

	private void load(Manifest mf) {
		for (Map.Entry<Object,Object> e : mf.getMainAttributes().entrySet())
			put(e.getKey().toString(), e.getValue().toString());
	}
}
