/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.plaintext;

import java.io.*;

import com.ibm.juno.core.*;
import com.ibm.juno.core.annotation.*;
import com.ibm.juno.core.filter.*;
import com.ibm.juno.core.parser.*;
import com.ibm.juno.core.utils.*;

/**
 * Parsers HTTP plain text request bodies into <a href='../package-summary.html#PojoCategories'>Group 5</a> POJOs.
 *
 *
 * <h6 class='topic'>Media types</h6>
 * <p>
 * 	Handles <code>Accept</code> types: <code>text/plain</code>
 * <p>
 * 	Produces <code>Content-Type</code> types: <code>text/plain</code>
 *
 *
 * <h6 class='topic'>Description</h6>
 * <p>
 * 	Essentially just converts plain text to POJOs via static <code>fromString()</code> or <code>valueOf()</code>, or
 * 	through constructors that take a single string argument.
 * <p>
 * 	Also parses objects using a filter if the object class has an {@link PojoFilter PojoFilter&lt;?,String&gt;} filter defined on it.
 *
 *
 * <h6 class='topic'>Configurable properties</h6>
 * <p>
 * 	This class has the following properties associated with it:
 * <ul>
 * 	<li>{@link BeanContextProperties}
 * </ul>
 *
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
@Consumes("text/plain")
public final class PlainTextParser extends ReaderParser {

	//--------------------------------------------------------------------------------
	// Overridden methods
	//--------------------------------------------------------------------------------

	@Override /* Parser */
	protected <T> T doParse(Reader in, int estimatedSize, ClassMeta<T> type, ParserContext ctx) throws IOException, ParseException {
		return ctx.getBeanContext().convertToType(IOUtils.read(in), type);
	}

	@Override /* Lockable */
	public PlainTextParser clone() {
		try {
			return (PlainTextParser)super.clone();
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e); // Shouldn't happen.
		}
	}
}
