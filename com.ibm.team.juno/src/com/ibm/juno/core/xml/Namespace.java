/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.xml;

/**
 * Represents a simple namespace mapping between a simple name and URI.
 * <p>
 * 	In general, the simple name will be used as the XML prefix mapping unless
 * 	there are conflicts or prefix remappings in the serializer.
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
public final class Namespace implements Comparable<Namespace> {
	final String name, uri;
	private final int hashCode;

	/**
	 * Constructor.
	 * <p>
	 * 	Use this constructor when the long name and short name are the same value.
	 *
	 * @param name The long and short name of this schema.
	 * @param uri The URI of this schema.
	 */
	protected Namespace(String name, String uri) {
		this.name = name;
		this.uri = uri;
		this.hashCode = (name == null ? 0 : name.hashCode()) + uri.hashCode();
	}

	/**
	 * Returns the namespace name.
	 *
	 * @return The namespace name.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Returns the namespace URI.
	 *
	 * @return The namespace URI.
	 */
	public String getUri() {
		return uri;
	}

	@Override /* Comparable */
	public int compareTo(Namespace o) {
		int i = name.compareTo(o.name);
		if (i == 0)
			i = uri.compareTo(o.uri);
		return i;
	}

	/**
	 * For performance reasons, equality is always based on identity, since
	 * the {@link NamespaceFactory} class ensures no duplicate name+uri pairs.
	 */
	@Override /* Object */
	public boolean equals(Object o) {
		return this == o;
	}

	@Override /* Object */
	public int hashCode() {
		return hashCode;
	}

	@Override /* Object */
	public String toString() {
		return "{name='"+name+"',uri:'"+uri+"'}";
	}
}
