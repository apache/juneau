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
import java.util.zip.*;

import org.apache.juneau.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.annotation.*;

/**
 * Utility class for representing the contents of a zip file as a list of entries whose contents don't resolve until
 * serialization time.
 *
 * <p>
 * Generally associated with <c>RestServlets</c> using the <c>responseHandlers</c> annotation so that
 * REST methods can easily create ZIP file responses by simply returning instances of this class.
 */
@Response
public class ZipFileList extends LinkedList<ZipFileList.ZipFileEntry> implements Streamable {

	private static final long serialVersionUID = 1L;

	/**
	 * The name of the zip file.
	 */
	public final String fileName;

	@Header("Content-Type")
	@Override /* Streamable */
	public MediaType getMediaType() {
		return MediaType.forString("application/zip");
	}

	/**
	 * Returns the value for the <c>Content-Disposition</c> header.
	 *
	 * @return The value for the <c>Content-Disposition</c> header.
	 */
	@Header("Content-Disposition")
	public String getContentDisposition() {
		return "attachment;filename=" + fileName;
	}

	@ResponseBody
	@Override /* Streamable */
	public void streamTo(OutputStream os) throws IOException {
		try (ZipOutputStream zos = new ZipOutputStream(os)) {
			for (ZipFileEntry e : this)
				e.write(zos);
		}
		os.flush();
	}

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
		 * @throws IOException Thrown by underlying stream.
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
		 * @throws IOException Thrown by underlying stream.
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
					try (FileInputStream fis = new FileInputStream(f)) {
						IOPipe.create(fis, zos).run();
					}
				}
			}
		}
	}
}
