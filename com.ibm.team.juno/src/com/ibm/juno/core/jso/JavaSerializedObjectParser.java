/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.jso;

import java.io.*;

import com.ibm.juno.core.*;
import com.ibm.juno.core.annotation.*;
import com.ibm.juno.core.parser.*;

/**
 * Parses POJOs from HTTP responses as Java {@link ObjectInputStream ObjectInputStreams}.
 *
 *
 * <h6 class='topic'>Media types</h6>
 * <p>
 * 	Consumes <code>Content-Type</code> types: <code>application/x-java-serialized-object</code>
 *
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
@Consumes("application/x-java-serialized-object")
public final class JavaSerializedObjectParser extends InputStreamParser {

	//--------------------------------------------------------------------------------
	// Overridden methods
	//--------------------------------------------------------------------------------

	@SuppressWarnings("unchecked")
	@Override /* InputStreamParser */
	protected <T> T doParse(InputStream in, int estimatedSize, ClassMeta<T> type, ParserContext ctx) throws ParseException, IOException {
		try {
			ObjectInputStream ois = new ObjectInputStream(in);
			return (T)ois.readObject();
		} catch (ClassNotFoundException e) {
			throw new ParseException(e);
		}
	}


	@Override /* Lockable */
	public JavaSerializedObjectParser clone() {
		try {
			return (JavaSerializedObjectParser)super.clone();
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e); // Shouldn't happen
		}
	}
}
