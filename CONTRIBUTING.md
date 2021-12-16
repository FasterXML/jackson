## Contributing to Jackson

First of all: we would love to get your contributions, whether they are in form of bug reports,
Requests for Enhancement (RFE), documentation, or code patches.
This page lists things that are important to know about contributing to Jackson project repositories.

### Community, Communication

The easiest ways to participate beyond using Jackson is to join one of Jackson mailing lists
(Jackson google groups):

* [Jackson Announce](https://groups.google.com/forum/#!forum/jackson-announce): Announcement-only list for new Jackson releases, meetups and other events related to Jackson
* [Jackson User](https://groups.google.com/forum/#!forum/jackson-user): List dedicated for discussion on Jackson usage
* [Jackson Dev](https://groups.google.com/forum/#!forum/jackson-dev): List for developers of Jackson core components and modules, discussing implementation details, API changes.

or to join chat on

* [Jackson-databind gitter](https://gitter.im/FasterXML/jackson-databind) forum

There are other related lists and forums as well:

* [Smile Format Discussion](https://groups.google.com/forum/#!forum/smile-format-discussion): List for discussing details of the binary JSON format called [Smile](https://en.wikipedia.org/wiki/Smile_%28data_interchange_format%29) (see [Smile Specification](https://github.com/FasterXML/smile-format-specification))

Note that individual Jackson projects have different maintainers; see the individual project
README for the list of maintainers of that module.

<br>

### Issue Tracking

All bug reports, improvements ideas and RFEs are handled as Github Issues using
per-repository Issue Tracker. For example, issues related to Jackson databinding
(main json-to/from-Java objects handling) component would go
to [jackson-databind Issue Tracker](https://github.com/FasterXML/jackson-databind/issues).

#### "New Contributor Friendly" issues

One effort to help new contributors is to try to collect issues that might be particularly good for
new contributors:

[Issues for New Contributors](https://github.com/FasterXML/jackson/wiki/Issues-For-New-Contributors) (Added for Hacktoberfest 2019)

### Code contributions, related

#### Pull Requests

All code contributions are made using Github Pull Requests: you typically fork the component
to modify, and eventually create a Pull Request. It is good to have a Github Issue created
for change you want to submit, explaining what is needed (bug fix, change to behavior,
new feature) although this is not absolutely required.

#### Branches

When creating code (or documentation, test) change for eventual Pull Request, it is important to
understand which Git Branch to use as the base.

Jackson projects maintain a few branches:

* `master` for developing the still-far-off 3.0.0 release -- but is also used for `README.md`s
* `2.14` the next minor version in development
* `2.13` the current stable release
* `2.12` the previous stable branch, for which patch releases are still made
* `2.11` inactive branch that may receive micro-patches for urgent security issues (usually only [`jackson-databind`](https://github.com/FasterXML/jackson-databind))

Most bug-fix Pull Requests should be made against the current stable branch, `2.13`.
Pull requests for major new functionality or that significantly alter internals,
but are backwards-compatible with existing behavior should be made against the next minor version
branch (`2.14`).
If Jackson's functionality or default behavior is to be altered, `master` is the correct branch, but
discussion is probably in order.

If you have any concerns or doubts about branch to use, feel free to reach out on user mailing
list or chat; or even on issue tracker of relevant repository.

#### Backwards Compatibility

When submitting a pull request, your choice of a base branch should take into account backwards
compatibility.

The Jackson project follows [Apache versioning](https://apr.apache.org/versioning.html).  Patch
versions maintain source and binary compatibility; functionality may be added, but existing code
that depends upon Jackson must continue to function properly without alteration.  Minor versions
add functionality, may deprecate existing functionality, and may remove functionality that has
been deprecated for at least two minor versions.  Any changes that require breaking existing
functionality must be part of a major version release.

See [Jackson Releases on the wiki](https://github.com/FasterXML/jackson/wiki/Jackson-Releases)
for more information.

#### Testing

Jackson's functionality is vast and is used widely, so automated testing for any changes is
important for preventing accidental breakage in the future.  Tests also document and demonstrate
the bounds of functionality, showing the author's intent to others working on the code in the
future.

#### Paperwork

There is not a lot of paperwork related to code changes: Pull Requests are almost all it takes.

But there is one thing that is needed before development team can accept a code change (exception:
test code changes do not require one):
[Contributor License Agreement (CLA)](https://en.wikipedia.org/wiki/Contributor_License_Agreement).
This is needed before your very first code contribution, but covers all Jackson projects and
will NOT be needed for other future contributions.

All you need to do is to download the CLA document, print it, fill and sign, scan (or take
photo on your phone) and email that copy to `info` at `fasterxml` dot `com`.

As to document to download, you have 2 choices:

* Standard Jackson [Contributor License Agreement](../../blob/master/contributor-agreement.pdf) (CLA) is a one-page document we need from every contributor of code (we will request it for pull requests), used mostly by individual contributors
* [Corporate CLA](../../blob/master/contributor-agreement-corporate.txt) is used by Corporations to avoid individual employees from having to send separate CLAs; it is also favored by corporate IP lawyers.

of which the first option is more commonly used (by over 90% of contributors).

Note that the first option is available for corporations as well, but most companies have opted to use the second option instead. Core team has no preference over which one gets used: both work; we care more about actual contributions.
