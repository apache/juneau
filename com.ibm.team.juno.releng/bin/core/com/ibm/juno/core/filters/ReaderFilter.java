/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2011, 2014. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.filters;

import java.io.*;

import com.ibm.juno.core.filter.*;
import com.ibm.juno.core.html.*;
import com.ibm.juno.core.json.*;
import com.ibm.juno.core.parser.*;
import com.ibm.juno.core.serializer.*;
import com.ibm.juno.core.utils.*;
import com.ibm.juno.core.xml.*;

/**
 * Transforms the contents of a {@link Reader} into an {@code Object}.
 *
 *
 * <h6 class='topic'>Description</h6>
 * <p>
 * 	The {@code Reader} must contain JSON, Juno-generated XML (output from {@link XmlSerializer}),
 * 		or Juno-generated HTML (output from {@link JsonSerializer}) in order to be parsed correctly.
 * <p>
 * 	Useful for serializing models that contain {@code Readers} created by {@code RestCall} instances.
 * <p>
 * 	This is a one-way filter, since {@code Readers} cannot be reconstituted.
 *
 *
 * <h6 class='topic'>Behavior-specific subclasses</h6>
 * <p>
 * 	The following direct subclasses are provided for convenience:
 * <ul>
 * 	<li>{@link Json} - Parses JSON text.
 * 	<li>{@link Xml} - Parses XML text.
 * 	<li>{@link Html} - Parses HTML text.
 * 	<li>{@link PlainText} - Parses plain text.
 * </ul>
 *
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
public class ReaderFilter extends PojoFilter<Reader,Object> {

	/** Reader filter for reading JSON text. */
	public static class Json extends ReaderFilter {
		/** Constructor */
		public Json() {
			super(JsonParser.DEFAULT);
		}
	}

	/** Reader filter for reading XML text. */
	public static class Xml extends ReaderFilter {
		/** Constructor */
		public Xml() {
			super(XmlParser.DEFAULT);
		}
	}

	/** Reader filter for reading HTML text. */
	public static class Html extends ReaderFilter {
		/** Constructor */
		public Html() {
			super(HtmlParser.DEFAULT);
		}
	}

	/** Reader filter for reading plain text. */
	public static class PlainText extends ReaderFilter {
		/** Constructor */
		public PlainText() {
			super(null);
		}
	}

	/** The parser to use to parse the contents of the Reader. */
	private ReaderParser parser;

	/**
	 * @param parser The parser to use to convert the contents of the reader to Java objects.
	 */
	public ReaderFilter(ReaderParser parser) {
		this.parser = parser;
	}

	/**
	 * Converts the specified {@link Reader} to an {@link Object} whose type is determined
	 * by the contents of the reader.
	 */
	@Override /* PojoFilter */
	public Object filter(Reader o) throws SerializeException {
		try {
			if (parser == null)
				return IOUtils.read(o);
			return parser.parse(o, -1, beanContext.object());
		} catch (IOException e) {
			return e.getLocalizedMessage();
		} catch (Exception e) {
			throw new SerializeException("ReaderFilter could not filter object of type ''{0}''", o == null ? null : o.getClass().getName()).initCause(e);
		}
	}
}
