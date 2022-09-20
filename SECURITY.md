# Security Policy

Last Updated: 2022-09-20

This policy covers ALL Jackson projects/repos: some repos may have their own copy of this policy document.

## Supported Versions

Current status of open branches, with new releases, can be found from [Jackson Releases](https://github.com/FasterXML/jackson/wiki/Jackson-Releases)
wiki page

## Reporting a Vulnerability

The recommended mechanism for reporting possible security vulnerabilities follows
so-called "Coordinated Disclosure Plan" (see [definition of DCP](https://vuls.cert.org/confluence/display/Wiki/Coordinated+Vulnerability+Disclosure+Guidance)
for general idea). The first step is to file a [Tidelift security contact](https://tidelift.com/security):
Tidelift will route all reports via their system to maintainers of relevant package(s), and start the
process that will evaluate concern and issue possible fixes, send update notices and so on.
Note that you do not need to be a Tidelift subscriber to file a security contact.

Alternatively you may also report possible vulnerabilities to `info` at fasterxml dot com
mailing address. Note that filing an issue to go with report is fine, but if you do that please
DO NOT include details of security problem in the issue but only in email contact.
This is important to give us time to provide a patch, if necessary, for the problem.

## Verifying Artifact signatures

(for more in-depth explanation, see [Apache Release Signing](https://infra.apache.org/release-signing#keys-po\
licy) document)

To verify that any given Jackson artifact has been signed with a valid key, have a look at `KEYS` file of the\
 main Jackson repo:

https://github.com/FasterXML/jackson/blob/master/KEYS

which lists all	known valid keys in use.
