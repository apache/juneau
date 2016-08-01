/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 * Note to U.S. Government Users Restricted Rights:
 * Use, duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp.
 *******************************************************************************/
package com.ibm.juno.core.utils;

import java.io.*;
import java.util.*;
import java.util.zip.*;

/**
 * Utility class for representing the contents of a zip file as a list of entries
 * whose contents don't resolve until serialize time.
 * <p>
 * Generally associated with <code>RestServlets</code> using the <code>responseHandlers</code>
 * 	annotation so that REST methods can easily create ZIP file responses by simply returning instances
 * 	of this class.
 */
@SuppressWarnings("serial")
public class ZipFileList extends LinkedList<ZipFileList.ZipFileEntry> {

	/**
	 * The name of the zip file.
	 */
	public final String fileName;

	/**
	 * Constructor.
	 *
	 * @param fileName The file name of the zip file to create.
	 */
	public ZipFileList(String fileName) {
		this.fileName = fileName;
	}

	/**
	 * Add an entry to this list.
	 *
	 * @param e The zip file entry.
	 * @return This object (for method chaining).
	 */
	public ZipFileList append(ZipFileEntry e) {
		add(e);
		return this;
	}

	/**
	 * Interface for ZipFileList entries.
	 */
	public static interface ZipFileEntry {
		/**
		 * Write this entry to the specified output stream.
		 *
		 * @param zos The output stream to write to.
		 * @throws IOException
		 */
		void write(ZipOutputStream zos) throws IOException;
	}

	/**
	 * ZipFileList entry for File entry types.
	 */
	public static class FileEntry implements ZipFileEntry {

		/** The root file to base the entry paths on. */
		protected File root;

		/** The file being zipped. */
		protected File file;

		/**
		 * Constructor.
		 *
		 * @param root The root file that represents the base path.
		 * @param file The file to add to the zip file.
		 */
		public FileEntry(File root, File file) {
			this.root = root;
			this.file = file;
		}

		/**
		 * Constructor.
		 *
		 * @param file The file to add to the zip file.
		 */
		public FileEntry(File file) {
			this.file = file;
			this.root = (file.isDirectory() ? file : file.getParentFile());
		}

		@Override /* ZipFileEntry */
		public void write(ZipOutputStream zos) throws IOException {
			addFile(zos, file);
		}

		/**
		 * Subclasses can override this method to customize which files get added to a zip file.
		 *
		 * @param f The file being added to the zip file.
		 * @return Always returns <jk>true</jk>.
		 */
		public boolean doAdd(File f) {
			return true;
		}

		/**
		 * Adds the specified file to the specified output stream.
		 *
		 * @param zos The output stream.
		 * @param f The file to add.
		 * @throws IOException
		 */
		protected void addFile(ZipOutputStream zos, File f) throws IOException {
			if (doAdd(f)) {
				if (f.isDirectory()) {
					File[] fileList = f.listFiles();
					if (fileList == null)
						throw new IOException(f.toString());
					for (File fc : fileList)
						addFile(zos, fc);
				} else if (f.canRead()) {
					String path = f.getAbsolutePath().substring(root.getAbsolutePath().length() + 1).replace('\\', '/');
					ZipEntry e = new ZipEntry(path);
					e.setSize(f.length());
					zos.putNextEntry(e);
					IOPipe.create(new FileInputStream(f), zos).run();
				}
			}
		}
	}
}
