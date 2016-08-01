/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2011, 2016. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.csv;

import java.io.*;
import java.util.*;

import com.ibm.juno.core.*;
import com.ibm.juno.core.annotation.*;
import com.ibm.juno.core.serializer.*;

/**
 * TODO - Work in progress.  CSV serializer.
 */
@Produces("text/csv")
@SuppressWarnings({"unchecked","rawtypes"})
public final class CsvSerializer extends WriterSerializer {

	//--------------------------------------------------------------------------------
	// Overridden methods
	//--------------------------------------------------------------------------------

	@Override /* Serializer */
	protected void doSerialize(Object o, Writer out, SerializerContext ctx) throws IOException, SerializeException {
		BeanContext bc = ctx.getBeanContext();
		ClassMeta cm = bc.getClassMetaForObject(o);
		Collection l = null;
		if (cm.isArray()) {
			l = Arrays.asList((Object[])o);
		} else
			l = (Collection)o;
		if (l.size() > 0) {
			ClassMeta entryType = bc.getClassMetaForObject(l.iterator().next());
			if (entryType.isBean()) {
				BeanMeta<?> bm = entryType.getBeanMeta();
				int i = 0;
				for (BeanPropertyMeta pm : bm.getPropertyMetas()) {
					if (i++ > 0)
						out.append(',');
					append(out, pm.getName());
				}
				out.append('\n');
				for (Object o2 : l) {
					i = 0;
					BeanMap bean = bc.forBean(o2);
					for (BeanPropertyMeta pm : bm.getPropertyMetas()) {
						if (i++ > 0)
							out.append(',');
						append(out, pm.get(bean));
					}
					out.append('\n');
				}
			}

		}
	}

	private void append(Writer w, Object o) throws IOException {
		if (o == null)
			w.append("null");
		else {
			String s = o.toString();
			boolean mustQuote = false;
			for (int i = 0; i < s.length() && ! mustQuote; i++) {
				char c = s.charAt(i);
				if (Character.isWhitespace(c) || c == ',')
					mustQuote = true;
			}
			if (mustQuote)
				w.append('"').append(s).append('"');
			else
				w.append(s);
		}
	}

	@Override /* Serializer */
	public CsvSerializer clone() {
		try {
			return (CsvSerializer)super.clone();
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e); // Shouldn't happen.
		}
	}
}
