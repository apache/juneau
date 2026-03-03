# TODO

## Markdown Implementation (from plans/markdown_implementation.md)

- ~~TODO-1: Create MarkdownDocSerializer_Test (15 test cases b01–b15)~~ ✓
- ~~TODO-2: Create MarkdownWriter_Test (15 test cases c01–c15)~~ ✓
- ~~TODO-3: Create Markdown_Test marshaller (4 test cases d01–d04)~~ ✓
- ~~TODO-4: Create MarkdownAnnotation_Test (10 test cases e01–e10)~~ ✓
- ~~TODO-5: Create MarkdownEdgeCases_Test (13 test cases f01–f13)~~ ✓
- TODO-6: Create MarkdownMediaType_Test (3 test cases g01–g03)
- TODO-7: Create MarkdownAiUseCase_Test (4 test cases h01–h04)
- TODO-8: Add Markdown to RoundTripDateTime_Test TESTERS array
- TODO-9: Add Markdown to BasicUniversalConfig, add Markdown to RestClient.universal()
- TODO-10: Add Markdown to release notes and docs/pages/topics

---

- Document "juneau.enableVerboseExceptions" setting.
- Change field names that start with underscore so that they end with underscore (e.g. "_enum"->"enum_")
- Find places where we defined fields and methods as _foobar and convert them to foobar_
- Investigate navlinks URL generation issue: Either "request:?Accept=text/json&plainText=true" should be supported, or "request:/?Accept=text/json&plainText=true" should not append '/' to the request URL. Currently, "request:/?Accept=..." generates URLs like "http://localhost:5000/rest/db/request:?Accept=..." which is incorrect.
- Update REST server API to use new BeanStore2.
- ClassInfo should have a findGetter(String propertyName) convenience method.
- Make sure @Beanp("*") works on plain fields.



