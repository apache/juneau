# Apache Juneau Documentation Project Context

## Project Overview

This is the Docusaurus-based documentation site for Apache Juneau, a Java ecosystem for marshalling POJOs to various content types and creating annotation-based REST APIs. The documentation is hosted at https://juneau.staged.apache.org/ and is built from this repository.

## Current Project State

### Documentation Structure
- **Main Documentation**: Located in `/docs/topics/` with numbered files (e.g., `01.01.00.JuneauEcosystemOverview.md`)
- **Release Notes**: Located in `/docs/release-notes/` 
- **Sidebar Configuration**: Defined in `sidebars.ts` with hierarchical structure
- **Static Assets**: Maven site files copied to `/static/site/` including Javadocs

### Recent Major Changes Completed

#### 1. Documentation Reorganization (December 2024)
- **Created new "17. juneau-examples" section** to group all example projects
- **Moved examples from juneau-petstore section** to dedicated examples section
- **Renumbered existing sections**: Security (17→18), V9.0 Migration Guide (18→19)
- **Split "Microservice Examples"** into two specific pages:
  - `17.03.00.JuneauExamplesRestJetty.md` - Jetty-based microservices
  - `17.04.00.JuneauExamplesRestSpringboot.md` - Spring Boot microservices

#### 2. Example Documentation Created
- **`17.01.00.JuneauExamplesCore.md`** - Core serialization examples (JSON, XML, RDF)
- **`17.02.00.JuneauExamplesRest.md`** - REST API examples and patterns
- **`17.03.00.JuneauExamplesRestJetty.md`** - Standalone Jetty microservice examples
- **`17.04.00.JuneauExamplesRestSpringboot.md`** - Spring Boot integrated examples

#### 3. Link Fixes and Improvements
- **Fixed all broken Javadoc links** - Comprehensive link validation and repair
- **Converted API_DOCS links** to HTML hyperlinks with `target="_blank"`
- **Fixed package links** to point to correct Javadoc locations
- **Updated external links** to point to correct Oracle/Jakarta documentation
- **Fixed Builder class links** to use dot notation (e.g., `Class.Builder.html`)
- **Inferred parameter types** for method signatures in Javadoc links

#### 4. README and Navigation Updates
- **Updated main README** to point to staged Apache site (https://juneau.staged.apache.org/)
- **Added SonarCloud badges** for code quality metrics
- **Enhanced Examples section** with child bullets linking to specific example docs
- **Fixed dependencies link** to point to correct Maven site file (`dependency-info.html`)

#### 5. SonarCloud Integration
- **Added SonarCloud workflow** (`.github/workflows/sonarcloud.yml`)
- **Updated POM files** with SonarCloud Maven plugin
- **Created SonarCloud configuration** (`sonar-project.properties`)
- **Enhanced security page** with SonarCloud badges and analysis links
- **Created setup guide** (`SONARCLOUD_SETUP.md`)

## Technical Configuration

### Docusaurus Setup
- **Version**: Latest Docusaurus 3.x
- **Configuration**: `docusaurus.config.ts` with custom plugins
- **Build Process**: Maven site generation + Docusaurus build
- **Static Files**: Maven site copied to `/static/site/` for Javadocs and reports

### Maven Integration
- **Site Generation**: `create-mvn-site.sh` script generates Maven site
- **Javadoc Integration**: Javadocs served from `/site/apidocs/`
- **Test Reports**: Surefire reports at `/site/surefire.html`
- **Dependencies**: Dependency info at `/site/dependency-info.html`

### Link Management
- **Javadoc Links**: All point to `/site/apidocs/` with full method signatures
- **Javadoc Link Format**: Use HTML anchor tags (`<a href="/site/apidocs/..." target="_blank">`) instead of markdown links for Javadoc URLs, as Docusaurus doesn't handle markdown links to static files correctly
- **External Links**: Java standard library → Oracle docs, Jakarta → Jakarta docs
- **Internal Documentation Links**: When creating links to other docs, use the slug names and their final location (e.g., `/docs/topics/JuneauRestServerSpringbootBasics`) instead of relative file paths
- **GitHub Links**: PetStore project links use `master` branch
- **Hyperlink Formatting**: When creating hyperlinks, remove inline code blocks (backticks) from the link text

### Code Formatting
- **Indentation**: All code blocks in documentation use 4-space indentation (not tabs)

### Java Tree Rendering
- **Java Trees**: Special tree-like structures for rendering Java elements (packages, classes, methods, etc.) in documentation
- **Structure**: Uses `<tree>` container with `<node-X>` elements for hierarchy levels
- **Element Types**:
  - **`java-X` tags**: Normal nodes in a tree (e.g., `<java-abstract-class>`, `<java-interface>`)
  - **`javac-X` tags**: Condensed into a single line of a tree (e.g., `<javac-class>`, `<javac-method>`)
  - **`java-project` tags**: Project/module references with folder icon (📂)
  - **`java-doc` tags**: Documentation links with book icon (📖)
- **Styling**: Special element names are defined in `custom.css`
- **Example**: See lines 35-46 of `08.03.02.PredefinedClasses.md` for inheritance tree structure
- **Usage**: When user says "create a java tree", they're referring to this specific rendering system

## File Naming Conventions

### Documentation Files
- **Format**: `XX.YY.00.TopicName.md` where XX.YY is section.subsection
- **Slugs**: Use `TopicName` format (e.g., `JuneauEcosystemOverview`)
- **Front Matter**: Include `title` and `slug` fields

### Current Section Structure
```
1. Juneau Ecosystem (01.XX.00.*)
2. juneau-marshall (02.XX.00.*)
3. juneau-marshall-rdf (03.XX.00.*)
4. juneau-dto (04.XX.00.*)
5. juneau-config (05.XX.00.*)
6. juneau-assertions (06.XX.00.*)
7. juneau-rest-common (07.XX.00.*)
8. juneau-rest-server (08.XX.00.*)
9. juneau-rest-server-springboot (09.XX.00.*)
10. juneau-rest-client (10.XX.00.*)
11. juneau-rest-mock (11.XX.00.*)
12. juneau-microservice-core (12.XX.00.*)
13. juneau-microservice-jetty (13.XX.00.*)
14. My Jetty Microservice (14.XX.00.*)
15. My SpringBoot Microservice (15.XX.00.*)
16. juneau-petstore (16.XX.00.*)
17. juneau-examples (17.XX.00.*) ← NEW
18. Security (18.XX.00.*) ← RENUMBERED
19. V9.0 Migration Guide (19.XX.00.*) ← RENUMBERED
```

## Key URLs and Links

### Production Sites
- **Documentation**: https://juneau.staged.apache.org/
- **Javadocs**: https://juneau.staged.apache.org/site/apidocs/
- **PetStore Project**: https://github.com/apache/juneau-petstore (master branch)

### External Documentation
- **Oracle Java 17**: https://docs.oracle.com/en/java/javase/17/docs/api/
- **Jakarta Servlet 6.0**: https://jakarta.ee/specifications/servlet/6.0/apidocs/
- **Apache HttpClient**: https://hc.apache.org/httpcomponents-client-4.5.x/
- **SonarCloud**: https://sonarcloud.io/project/overview?id=apache_juneau

## Development Workflow

### Local Development
1. **Start Server**: `./start-server.sh` (runs Docusaurus dev server)
2. **Generate Maven Site**: `./create-mvn-site.sh` (updates Javadocs and reports)
3. **Build**: `npm run build` (creates production build)

### Link Validation
- **Static Check**: Use built HTML files for comprehensive link validation
- **Dev Check**: Use running server for dynamic content validation
- **Javadoc Validation**: Check against generated Javadoc files in `/static/site/apidocs/`

## Common Tasks and Patterns

### Adding New Documentation
1. Create numbered file in `/docs/topics/`
2. Add entry to `sidebars.ts` with proper numbering
3. Update any cross-references
4. Test links and navigation

### Fixing Broken Links
1. Identify link type (Javadoc, external, internal)
2. Check against valid URL lists (e.g., `focused-apidocs-urls.txt`)
3. Apply appropriate fix (parameter inference, external link conversion, etc.)
4. Validate fix works in browser

### Updating Examples
1. Update source code in respective modules
2. Update corresponding documentation in section 17
3. Update README links if needed
4. Test example functionality

## Known Issues and Limitations

### Link Validation
- Static link checkers may report false positives for dynamic content
- Some Javadoc links require parameter type inference
- External links may change over time

### Build Process
- Maven site generation required before Javadoc links work
- Docusaurus dev server caches static files (restart needed after Maven site updates)
- Some Maven site reports may show zero values (known configuration issues)

### Documentation Gaps
- Some advanced topics may need additional examples
- Migration guides may need updates for newer versions
- Some API documentation may be incomplete

## Maintenance Notes

### Regular Tasks
- **Link Validation**: Check for broken links after major updates
- **External Link Updates**: Verify Oracle/Jakarta links still work
- **Example Testing**: Ensure examples still work with current Juneau version
- **SonarCloud Monitoring**: Check quality gate status regularly

## Chat Session Rules

### Context Persistence
- **"Persist this rule to the context"**: When the user says this phrase, immediately add the rule they just stated to this PROJECT_CONTEXT.md file under an appropriate section
- **Rule Updates**: Always update this file when new rules, patterns, or procedures are established
- **Context Maintenance**: Keep this file current with project changes and established workflows
- **Cross-Session Consistency**: This helps maintain consistency across chat sessions and ensures that important rules and procedures are properly documented and preserved

### Version Updates
- Update version numbers in documentation
- Update external link versions (Java, Jakarta, etc.)
- Test all examples with new version
- Update migration guides if needed

## Contact and Resources

### Project Information
- **Apache Juneau**: https://juneau.apache.org/
- **GitHub Repository**: https://github.com/apache/juneau
- **Mailing List**: dev@juneau.apache.org
- **Wiki**: https://github.com/apache/juneau/wiki

### Documentation Team
- **Primary Maintainer**: James Bognar
- **Documentation Site**: https://juneau.staged.apache.org/
- **Issue Tracking**: GitHub Issues in main repository

---

*This file should be updated whenever significant changes are made to the documentation structure, build process, or project configuration.*
