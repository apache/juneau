# TODO


- Investigate navlinks URL generation issue: Either "request:?Accept=text/json&plainText=true" should be supported, or "request:/?Accept=text/json&plainText=true" should not append '/' to the request URL. Currently, "request:/?Accept=..." generates URLs like "http://localhost:5000/rest/db/request:?Accept=..." which is incorrect.
- Update REST server API to use new BeanStore2.
- Make sure @Beanp("*") works on plain fields.
- Need an easier way to specify this header:
    Content-Disposition: attachment; filename="example.pdf"

- A comprehensive plan for handling large data sets using Suppliers and Consumers?

- CSV format supports property names in headers (make sure it can work with Suppliers and Consumers above)
  - prop1,prop2,prop3
- Need a better way to define serializer config values (e.g. useWhitespace) arguments through REST (Additional headers?  Content-Type modifications?)

- RestResponse needs a setSerializer() command.

- On RestClient when logging with FULL, calling RestREsponse.getContent().asString() causes a stream closed exception.
- Possibility of adding convenience classes for okhttp3.mockwebserver.Dispatcher?

- Duration.ofDays(7) serialized in hours?