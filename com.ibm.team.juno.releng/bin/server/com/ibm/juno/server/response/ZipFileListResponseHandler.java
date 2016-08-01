/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2015. All Rights Reserved.
 *
 * Note to U.S. Government Users Restricted Rights:  Use,
 * duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp.
 *******************************************************************************/
package com.ibm.juno.server.response;

import java.io.*;
import java.util.zip.*;

import com.ibm.juno.core.utils.*;
import com.ibm.juno.core.utils.ZipFileList.ZipFileEntry;
import com.ibm.juno.server.*;
import com.ibm.juno.server.annotation.*;

/**
 * Response handler for ZipFileList objects.
 * <p>
 * Can be associated with a REST resource using the {@link RestResource#responseHandlers} annotation.
 * <p>
 * Sets the following headers:
 * <ul>
 * 	<li><code>Content-Type</code> - <code>application/zip</code>
 * 	<li><code>Content-Disposition=attachment;filename=X</code> - Sets X to the file name passed in through
 * 		the constructor {@link ZipFileList#ZipFileList(String)}.
 * </ul>
 */
public class ZipFileListResponseHandler implements ResponseHandler {

	@Override /* ResponseHandler */
	public boolean handle(RestRequest req, RestResponse res, Object output) throws IOException, RestException {
		if (output.getClass() == ZipFileList.class) {
			ZipFileList m = (ZipFileList)output;
			res.setContentType("application/zip"); //$NON-NLS-1$
			res.setHeader("Content-Disposition", "attachment;filename=" + m.fileName); //$NON-NLS-1$ //$NON-NLS-2$
			OutputStream os = res.getOutputStream();
			try {
				ZipOutputStream zos = new ZipOutputStream(os);
				try {
					for (ZipFileEntry e : m)
						e.write(zos);
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					zos.flush();
					zos.close();
				}
			} finally {
				os.flush();
			}
			return true;
		}
		return false;
	}
}
