# Security Policy

Last Updated: 2022-11-02

This policy covers ALL Jackson projects/repos: some repos may have their own copy of this policy document.

## Supported Versions

Current status of open branches, with new releases, can be found from [Jackson Releases](https://github.com/FasterXML/jackson/wiki/Jackson-Releases)
wiki page

## Reporting a Vulnerability

### Primary: filing GHSA

The recommended mechanism for reporting vulnerabilities is explained on `Security and quality` tab of the repository that contains code that has the vulnerability.
For example, for [jackson-databind](../../../jackson-databind), this would be [jackson-databind/security](https://github.com/FasterXML/jackson-databind/security).
Most commonly you should see "Report Vulnerability" button, using which you can file a ["GitHub Security Advisory (GHSA)"](https://radicalnotion.ai/learn/what-is-ghsa) contribution.

Links to most commonly necessary Jackson repos are:

* [jackson-core/security](https://github.com/FasterXML/jackson-core/security)
* [jackson-databind/security](https://github.com/FasterXML/jackson-databind/security)
* [jackson-dataformats-binary/security](https://github.com/FasterXML/jackson-dataformats-binary/security)
* [jackson-dataformats-text/security](https://github.com/FasterXML/jackson-dataformats-text/security)
* [jackson-dataformat-xml/security](https://github.com/FasterXML/jackson-dataformat-xml/security)
* [jackson-datatypes-collections/security](https://github.com/FasterXML/jackson-datatypes-collections/security)

### Secondary: contact via email

Alternatively you may also report possible vulnerabilities to `info` at fasterxml dot com mailing address.
You can also ask help for filing a GHSA; for example if you are not sure which Jackson repo is the right one to use
Note that filing a Githin issue to go with report is fine, but if you do that please DO NOT include details of security problem in the issue but only in email contact. This is important to give us time to provide a patch, if necessary, for the problem.

## Verifying Artifact signatures

(for more in-depth explanation, see [Apache Release Signing](https://infra.apache.org/release-signing#keys-policy) document)

To verify that any given Jackson artifact has been signed with a valid key, have a look at `KEYS` file of the main Jackson repo:

https://github.com/FasterXML/jackson/blob/master/KEYS

which lists all	known valid keys in use.

## Related documents

There is a published security audit about 2 main Jackson components (`jackson-core`, `jackson-databind`):

* [AdaLogics Jackson Security Audit (2022)](../../blob/master/docs/AdaLogics-Security-Audit-Jackson-2022.pdf)

which documents a thorough investigation into Jackson design, implementation, as well as some security design suggestions for developers using Jackson libraries.
