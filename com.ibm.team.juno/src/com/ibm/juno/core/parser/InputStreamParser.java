/*******************************************************************************
 * Licensed Materials - Property of IBM
 * © Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.parser;

import java.io.*;

import com.ibm.juno.core.*;
import com.ibm.juno.core.annotation.*;

/**
 * Subclass of {@link Parser} for byte-based parsers.
 *
 *
 * <h6 class='topic'>Description</h6>
 * <p>
 * 	This class is typically the parent class of all byte-based parsers.
 * 	It has 1 abstract method to implement...
 * <ul>
 * 	<li><code>parse(InputStream, ClassMeta, ParserContext)</code>
 * </ul>
 *
 *
 * <h6 class='topic'>@Consumes annotation</h6>
 * <p>
 * 	The media types that this parser can handle is specified through the {@link Consumes @Consumes} annotation.
 * <p>
 * 	However, the media types can also be specified programmatically by overriding the {@link #getMediaTypes()} method.
 *
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
public abstract class InputStreamParser extends Parser<InputStream> {

	@Override /* Parser */
	public boolean isReaderParser() {
		return false;
	}

	//--------------------------------------------------------------------------------
	// Abstract methods
	//--------------------------------------------------------------------------------

	@Override /* Parser */
	protected abstract <T> T doParse(InputStream in, int estimatedSize, ClassMeta<T> type, ParserContext ctx) throws ParseException, IOException;

}
