---
title: Security Analysis
description: Security analysis and vulnerability reporting for Apache Juneau
---

# Security Analysis

Apache Juneau takes security seriously and uses automated tools to help identify potential security vulnerabilities and code quality issues.

## Current Security Status

[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=apache_juneau&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=apache_juneau)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=apache_juneau&metric=security_rating)](https://sonarcloud.io/summary/new_code?id=apache_juneau)
[![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=apache_juneau&metric=reliability_rating)](https://sonarcloud.io/summary/new_code?id=apache_juneau)

### Analysis Results
- [**GitHub CodeQL Analysis →**](https://github.com/apache/juneau/security/code-scanning) - Security vulnerability detection
- [**SonarCloud Quality Analysis →**](https://sonarcloud.io/summary/new_code?id=apache_juneau) - Code quality, security, and maintainability

For how Juneau's serializers, parsers, SVL and REST layers handle untrusted input, see the [Security chapter](/docs/topics/Security) of the documentation.

## Security Advisories

### Apache Juneau 9.2.1 (2026-11-03)

Apache Juneau 9.2.1 fixes the following vulnerabilities, which affect Apache Juneau through 9.2.0. Users are recommended to upgrade to version 9.2.1. The [advisory](https://lists.apache.org/thread/hhn0377kyqb48b1hgr6mlfb4mp8p04d7) gives the affected modules, mitigations and credits for each issue.

| CVE | Severity | Summary |
|-----|----------|---------|
| [CVE-2026-73095](https://www.cve.org/CVERecord?id=CVE-2026-73095) | Important | Denial of service via unbounded memory allocation in parsers and REST request handling |
| [CVE-2026-73099](https://www.cve.org/CVERecord?id=CVE-2026-73099) | Important | Cross-site request forgery via default-enabled content query-parameter body override |
| [CVE-2026-73100](https://www.cve.org/CVERecord?id=CVE-2026-73100) | Important | Sensitive request headers logged in cleartext by the default request logger on server errors |
| [CVE-2026-73116](https://www.cve.org/CVERecord?id=CVE-2026-73116) | Important | REST client forwards credentials to a different origin when following HTTP redirects |
| [CVE-2026-121489](https://www.cve.org/CVERecord?id=CVE-2026-121489) | Moderate | Path traversal in DirectoryResource allows reading files outside the configured root directory |
| [CVE-2026-96677](https://www.cve.org/CVERecord?id=CVE-2026-96677) | Moderate | Cross-site scripting via unescaped request variables in HTML-doc chrome |
| [CVE-2026-73101](https://www.cve.org/CVERecord?id=CVE-2026-73101) | Moderate | Internal exception class and message disclosed via error response header and body |
| [CVE-2026-73098](https://www.cve.org/CVERecord?id=CVE-2026-73098) | Moderate | Unsafe reflection allows attacker-controlled class initialization during parsing |
| [CVE-2026-73117](https://www.cve.org/CVERecord?id=CVE-2026-73117) | Low | Response header CRLF injection and open redirect when applications set headers or redirect targets from request data |

Upgrading changes some defaults: request bodies over 1 MB need a larger `@Rest(maxInput)`, error responses no longer carry internal exception detail unless `renderResponseStackTraces` is on, and the `content` query parameter is honored only on PUT and POST. See the [9.2.1 release notes](/docs/release-notes/9.2.1) for details.

## About Our Analysis Tools

### GitHub CodeQL

We use [GitHub CodeQL](https://codeql.github.com/) for automated security analysis. CodeQL is GitHub's semantic code analysis engine that helps identify:

- **Security vulnerabilities** - Potential security issues in your code
- **Bugs and errors** - Logic errors and potential runtime issues  
- **Code quality issues** - Patterns that could lead to problems

### SonarCloud

We use [SonarCloud](https://sonarcloud.io/) for comprehensive code quality analysis. SonarCloud provides:

- **Code quality metrics** - Maintainability, reliability, and security ratings
- **Technical debt analysis** - Time to fix code quality issues
- **Code coverage integration** - Test coverage analysis with JaCoCo
- **Security hotspot detection** - Potential security vulnerabilities
- **Code smell detection** - Anti-patterns and maintainability issues
- **Duplication analysis** - Code duplication detection

## Analysis Schedule

Our security and quality analysis runs automatically:

- **CodeQL**: Weekly
- **SonarCloud**: Every Monday at 2:00 AM UTC
- **On every push** to the master branch
- **On every pull request** to the master branch
- **Languages analyzed**: Java

## Security Reporting

If you discover a security vulnerability in Apache Juneau, please report it responsibly:

### For Critical Security Issues

**Do not** report critical security vulnerabilities through public GitHub issues. Instead:

1. **Email**: Send details to [security@apache.org](mailto:security@apache.org)
2. **Include**: 
   - Description of the vulnerability
   - Steps to reproduce
   - Potential impact assessment
   - Suggested fix (if available)

### For Non-Critical Issues

For non-critical security concerns or code quality issues:

1. **GitHub Issues**: Create a new issue in our [GitHub repository](https://github.com/apache/juneau/issues)
2. **Label**: Use the "security" label if available
3. **Description**: Provide clear steps to reproduce and expected behavior

## Security Best Practices

When using Apache Juneau in your applications:

- **Keep updated**: Always use the latest stable release
- **Review dependencies**: Regularly check for vulnerable dependencies
- **Input validation**: Validate all external inputs
- **Error handling**: Implement proper error handling and logging
- **Configuration**: Use secure default configurations

## Dependencies

We regularly monitor our dependencies for known vulnerabilities:

- **Maven dependencies**: Automatically checked during builds
- **Security alerts**: GitHub automatically notifies us of vulnerable dependencies
- **Regular updates**: Dependencies are updated regularly in new releases

## Contact

For security-related questions or concerns:

- **Email**: [dev@juneau.apache.org](mailto:dev@juneau.apache.org)
- **Mailing list**: [Subscribe to our dev mailing list](https://lists.apache.org/list.html?dev@juneau.apache.org)
- **GitHub**: [Create an issue](https://github.com/apache/juneau/issues)
