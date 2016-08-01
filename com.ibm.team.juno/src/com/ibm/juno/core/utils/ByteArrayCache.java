/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.utils;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * A utility class for caching byte arrays in memory so that duplicate arrays can be reused.
 * <p>
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
public class ByteArrayCache {

	/**
	 * Default global byte array cache.
	 * Note that this can't ever get garbage collected so don't add really large arrays!
	 */
	public static final ByteArrayCache DEFAULT = new ByteArrayCache();

	private final ConcurrentHashMap<ByteArray,byte[]> cache = new ConcurrentHashMap<ByteArray,byte[]>();

	/**
	 * Add the specified byte array to this cache.
	 *
	 * @param contents The byte array to add to this cache.
	 * @return Either the same byte array or a previously cached byte array depending on whether the byte array
	 * 	already exists in the cache.
	 */
	public byte[] cache(byte[] contents) {
		if (contents == null)
			return null;
		ByteArray ba = new ByteArray(contents);
		cache.putIfAbsent(ba, ba.contents);
		return cache.get(ba);
	}

	/**
	 * Add the specified input stream to this cache.
	 *
	 * @param contents The input stream whose contents are to be added to this cache.
	 * @return Either the same byte array or a previously cached byte array depending on whether the byte array
	 * 	already exists in the cache.
	 * @throws IOException
	 */
	public byte[] cache(InputStream contents) throws IOException {
		if (contents == null)
			return null;
		ByteArray ba = new ByteArray(IOUtils.readBytes(contents, 1024));
		cache.putIfAbsent(ba, ba.contents);
		return cache.get(ba);
	}

	/**
	 * Returns the number of byte arrays in this cache.
	 *
	 * @return The number of byte arrays in this cache.
	 */
	public int size() {
		return cache.size();
	}

	private static class ByteArray {
		private int hashCode;
		private byte[] contents;

		private ByteArray(byte[] contents) {
			this.contents = contents;
			int multiplier = 1;
			for (int i = 0; i < contents.length; i++) {
				hashCode += contents[i] * multiplier;
				int shifted = multiplier << 5;
				multiplier = shifted - multiplier;
			}
		}

		@Override /* Object */
		public int hashCode() {
			if (hashCode == 0) {
			}
			return hashCode;
		}

		@Override /* Object */
		public boolean equals(Object o) {
			if (o instanceof ByteArray) {
				ByteArray ba = (ByteArray)o;
				if (ba.hashCode == hashCode)
					return Arrays.equals(ba.contents, contents);
			}
			return false;
		}
	}
}
