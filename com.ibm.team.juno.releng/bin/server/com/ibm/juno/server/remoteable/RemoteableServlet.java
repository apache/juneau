/*******************************************************************************
 * Licensed Materials - Property of IBM
 * © Copyright IBM Corporation 2015. All Rights Reserved.
 *
 * Note to U.S. Government Users Restricted Rights:
 * Use, duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp.
 *******************************************************************************/
package com.ibm.juno.server.remoteable;

import static javax.servlet.http.HttpServletResponse.*;

import java.util.*;
import java.util.concurrent.*;

import com.ibm.juno.core.*;
import com.ibm.juno.core.dto.*;
import com.ibm.juno.core.parser.*;
import com.ibm.juno.server.*;
import com.ibm.juno.server.annotation.*;

/**
 * Abstract class for defining Remoteable services.
 * <p>
 * Remoteable services are POJOs whose methods can be invoked remotely through proxy interfaces.
 * <p>
 * To implement a remoteable service, developers must simply subclass from this class and implement the {@link #getServiceMap()} method that
 * 	maps java interfaces to POJO instances.
 *
 * See {@link com.ibm.juno.server.remoteable} for details.
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
@SuppressWarnings("serial")
public abstract class RemoteableServlet extends RestServletDefault {

	private Map<String,Class<?>> classNameMap = new ConcurrentHashMap<String,Class<?>>();

	//--------------------------------------------------------------------------------
	// Abstract methods
	//--------------------------------------------------------------------------------

	/**
	 * Returns the list of interfaces to their implementation objects.
	 * <p>
	 * This class is called often and not cached, so any caching should occur in the subclass if necessary.
	 *
	 * @return The service map.
	 * @throws Exception
	 */
	protected abstract Map<Class<?>,Object> getServiceMap() throws Exception;

	//--------------------------------------------------------------------------------
	// REST methods
	//--------------------------------------------------------------------------------

	/**
	 * [GET /] - Get the list of all remote interfaces.
	 *
	 * @param req The HTTP servlet request.
	 * @return The list of links to the remote interfaces.
	 * @throws Exception
	 */
	@RestMethod(name="GET", path="/")
	public List<Link> getInterfaces(RestRequest req) throws Exception {
		List<Link> l = new LinkedList<Link>();
		boolean useAll = ! useOnlyAnnotated();
		for (Class<?> c : getServiceMap().keySet()) {
			if (useAll || getBeanContext().getClassMeta(c).isRemoteable())
				l.add(new Link(c.getName(), "{0}/{1}", req.getRequestURI(), c.getName())); //$NON-NLS-1$
		}
		return l;
	}

	/**
	 * [GET /{javaInterface] - Get the list of all remoteable methods on the specified interface name.
	 *
	 * @param javaInterface The Java interface name.
	 * @return The methods defined on the interface.
	 * @throws Exception
	 */
	@RestMethod(name="GET", path="/{javaInterface}")
	public Collection<String> listMethods(@Attr String javaInterface) throws Exception {
		return getMethods(javaInterface).keySet();
	}

	/**
	 * [POST /{javaInterface}/{javaMethod}] - Invoke the specified service method.
	 *
	 * @param req The HTTP request.
	 * @param javaInterface The Java interface name.
	 * @param javaMethod The Java method name or signature.
	 * @return The results from invoking the specified Java method.
	 * @throws Exception
	 */
	@RestMethod(name="POST", path="/{javaInterface}/{javaMethod}")
	public Object invoke(RestRequest req, @Attr String javaInterface, @Attr String javaMethod) throws Exception {

		// Find the parser.
		ReaderParser p = req.getReaderParser();
		if (p == null)
			throw new RestException(SC_UNSUPPORTED_MEDIA_TYPE, "Could not find parser for media type ''{0}''", req.getMediaType()); //$NON-NLS-1$
		Class<?> c = getInterfaceClass(javaInterface);

		// Find the service.
		Object service = getServiceMap().get(c);
		if (service == null)
			throw new RestException(SC_NOT_FOUND, "Service not found"); //$NON-NLS-1$

		// Find the method.
		java.lang.reflect.Method m = getMethods(javaInterface).get(javaMethod);
		if (m == null)
			throw new RestException(SC_NOT_FOUND, "Method not found"); //$NON-NLS-1$

		// Parse the args and invoke the method.
		ClassMeta<?>[] argTypes = p.getBeanContext().getClassMetas(m.getParameterTypes());
		Object[] params = p.parseArgs(req.getReader(), -1, argTypes);
		return m.invoke(service, params);
	}

	//--------------------------------------------------------------------------------
	// Other methods
	//--------------------------------------------------------------------------------

	private boolean useOnlyAnnotated() {
		return getProperties().getBoolean(RemoteableServiceProperties.REMOTEABLE_includeOnlyRemotableMethods, false);
	}

	private Map<String,java.lang.reflect.Method> getMethods(String javaInterface) throws Exception {
		Class<?> c = getInterfaceClass(javaInterface);
		ClassMeta<?> cm = getBeanContext().getClassMeta(c);
		return (useOnlyAnnotated() ? cm.getRemoteableMethods() : cm.getPublicMethods());
	}

	/**
	 * Return the <code>Class</code> given it's name if it exists in the services map.
	 */
	private Class<?> getInterfaceClass(String javaInterface) throws Exception {
		Class<?> c = classNameMap.get(javaInterface);
		if (c == null) {
			for (Class<?> c2 : getServiceMap().keySet())
				if (c2.getName().equals(javaInterface)) {
					classNameMap.put(javaInterface, c2);
					return c2;
				}
			throw new RestException(SC_NOT_FOUND, "Interface class not found");
		}
		return c;
	}
}
