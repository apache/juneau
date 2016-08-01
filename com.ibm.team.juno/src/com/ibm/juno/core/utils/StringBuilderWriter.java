/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.utils;

import java.io.*;

/**
 * Similar to {@link StringWriter}, but uses a {@link StringBuilder} instead to avoid synchronization overhead.
 * <p>
 * Note that this class is NOT thread safe.
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
public final class StringBuilderWriter extends Writer {

	private StringBuilder sb;

	/**
	 * Create a new string writer using the default initial string-builder size.
	 */
	public StringBuilderWriter() {
		sb = new StringBuilder();
		lock = null;
	}

	/**
	 * Create a new string writer using the specified initial string-builder size.
	 *
	 * @param initialSize The number of <tt>char</tt> values that will fit into this buffer before it is automatically expanded
	 * @throws IllegalArgumentException If <tt>initialSize</tt> is negative
	 */
	public StringBuilderWriter(int initialSize) {
		sb = new StringBuilder(initialSize);
		lock = null;
	}

	@Override /* Writer */
	public void write(int c) {
		sb.append((char) c);
	}

	@Override /* Writer */
	public void write(char cbuf[], int start, int length) {
		sb.append(cbuf, start, length);
	}

	@Override /* Writer */
	public void write(String str) {
		sb.append(str);
	}

	@Override /* Writer */
	public void write(String str, int off, int len) {
		sb.append(str.substring(off, off + len));
	}

	@Override /* Writer */
	public StringBuilderWriter append(CharSequence csq) {
		if (csq == null)
			write("null");
		else
			write(csq.toString());
		return this;
	}

	@Override /* Writer */
	public StringBuilderWriter append(CharSequence csq, int start, int end) {
		CharSequence cs = (csq == null ? "null" : csq);
		write(cs.subSequence(start, end).toString());
		return this;
	}

	@Override /* Writer */
	public StringBuilderWriter append(char c) {
		write(c);
		return this;
	}

	@Override /* Object */
	public String toString() {
		return sb.toString();
	}

	@Override /* Writer */
	public void flush() {}

	@Override /* Writer */
	public void close() throws IOException {}
}
