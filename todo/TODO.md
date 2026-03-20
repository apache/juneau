# TODO


- Update REST server API to use new BeanStore2.
- A comprehensive plan for handling large data sets using Suppliers and Consumers?

- CSV format supports property names in headers (make sure it can work with Suppliers and Consumers above)
  - prop1,prop2,prop3
- Need a better way to define serializer config values (e.g. useWhitespace) arguments through REST (Additional headers?  Content-Type modifications?)

- On RestClient when logging with FULL, calling RestREsponse.getContent().asString() causes a stream closed exception.
- Possibility of adding convenience classes for okhttp3.mockwebserver.Dispatcher?

- Duration.ofDays(7) serialized in hours?