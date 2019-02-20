package org.apache.juneau.server.config.rest;

import static org.apache.juneau.http.HttpMethodName.GET;

import org.apache.juneau.rest.RestServlet;
import org.apache.juneau.rest.annotation.RestMethod;
import org.apache.juneau.rest.annotation.RestResource;

@RestResource(path = "/helloWorld")
public class LoadConfigResource extends RestServlet {

	@RestMethod(name = GET, path = "/*", consumes = "application/json", produces = "application/json")
	public String sayHello() {
		return "{'msg':'OK'}";
	}

}
