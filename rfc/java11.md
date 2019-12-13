# Java 11 Migration

| | |
|-|-|
| **Status**     | _**Proposed**, Accepted, Implemented, Obsolete_ |
| **RFC #**      | [62](https://github.com/spinnaker/community/pull/62) |
| **Author(s)**  | Michael Plump ([`@plumpy`](https://github.com/plumpy)) |
| **SIG / WG**   | Platform SIG |

## Overview

To keep current with the evolution of Java, Spinnaker needs to move from Java 8
to Java 11. This RFC documents a process and timeline for doing so.

### Goals and Non-Goals

Goals:

*   Provide a process for moving official Spinnaker builds to the Java 11 JRE
*   Discuss ways we will encourage people using non-offical builds to also move
    to the Java 11 JRE
*   Provide a timeline for the final switch to a Java 11 JDK along with criteria
    to decide if the timeline should be delayed

Non-Goals:

*   Providing a rationale for upgrading Java; all active Java projects need to
    update their JDK version eventually as libraries we depend upon will also
    depend on new versions of the JDK
*   Debating which Java version we should upgrade to; we take it as a given that
    only LTS releases are acceptable for most organizations, especially for
    running critical infrastructure like Spinnaker
*   Documenting and solving technical issues that present themselves in the
    migration; those will be handled separately, but this document concerns
    itself primarily with the larger processes and timeline
*   Detailing the required release system changes; since the releases are still
    entirely maintained by Google, these changes aren't particularly relevant to
    the wider community

### JRE vs. JDK

There are two components of Java of interest to this RFC. The Java Runtime
Environment (JRE) is the `java` binary and associated libraries used to _run_
Spinnaker services. The Java Development Kit (JDK) is the `javac` binary and
associated libraries used to _compile_ Spinnaker.

Once we upgrade to the Java 11 JDK, Spinnaker will no longer be able to run in a
Java 8 JRE. So the first (more complicated) step is to get everyone migrated to
a Java 11 JRE. Only after that is done can we switch to the Java 11 JDK.

The Java 11 JRE has some fairly large changes, most notably the deprecation of
the long-standing CMS garbage collector. Large organizations with highly-tuned
JVMs have shown hesitancy to upgrade until the new runtime has undergone
extensive testing. (Both Google and Netflix are in this category.)

For official Spinnaker containers, we control the JRE used to run Spinnaker. For
those releases, this proposal takes a relatively slow ramp to the Java 11 JRE
and provides an escape hatch back to Java 8 for users who experience problems.

There are also some unknown number of customers who aren't using the Spinnaker-
provided containers. We will need to do some outreach to make sure these users
convert to a Java 11 JRE too.

## Timeline

### 1.18 (early January)

For our container builds, we'll first move just two (of approximately ten)
services to the new JRE. The two services will be `igor` and `front50`. By
default, anyone who uses Halyard to deploy these containers will get the Java 11
JRE for these services. Users who experience problems or aren't ready to use
Java 11 can use the `java8` variant by specifying `imageVariant: slim-java8` (or
`ubuntu-java8`) in their Halyard config file.

The release notes will prominently mention this fact. It will also be mentioned
in-channel in the release announcement to `#spinnaker-releases` and in the
release announcement email. We will strongly encourage people who opt for the
`java8` variants to contact `sig-platform@spinnaker.io` or `#sig-platform` to
discuss their concerns, which will allow us to adjust the timeline as needed.

Separately, postings to `#dev` will encourage developers to start running their
development environments under Java 11. Explanations of how to do so will be
posted to the channel. The [Spinnaker Developer
Guide](https://www.spinnaker.io/guides/developer/getting-set-up/) will also be
updated to strongly recommend using Java 11.

If issues are found in Spinnaker that are caused by running under a Java 11
runtime, we will switch the containers back to Java 8 and shift the timeline
back one release. Since we're providing the `java8` variants, we will not adjust
the timeline for any other reason.

### 1.19 (early March)

_All_ containers built by the build process will now use the Java 11 JRE. As
before, the `java8` variants will offer a way to opt out of this change.

Other than that, everything from the previous section applies to this release as
well.

### 1.20 (early May)

For this release, we will do two things:

1.  Remove the `java8` container variants.
2.  Compile with JDK 11 using the `-source 8 -target 8` flags. This produces
    builds that are still compatible with Java 8, and means the build will still
    fail if anyone tries to use Java 11 APIs or language features.

These changes are separable and don't _need_ to be done together. Either can be
delayed for a release if there are issues. The change in the next section
(tentatively scheduled for release 1.21) will not happen until both these
changes have shipped for an entire release cycle, however.

Since removing the `java8` containers removes the possibility of running with
the old JRE, the old statement of "we will not adjust the timeline for any other
reason" no longer applies. If we know for a fact (from previous discussions)
that removing the `java8` variants will cause problems for some customers, or if
a customer complains after the release, we can put back the `java8` containers
and delay this part (but keep the compiler change) until the next release.

Similarly, if the compiler change causes issues, we can revert that change and
push that out to the next release.

### 1.21 (early July)

The compiler flags will change from `-source 8 -target 8` to `-source 11 -target
11`. At this point, developers are able to commit Java 11 code to the
repositories. Undoing this change is possible, but will require some work. As
such, this should only be done after

1.  the `java8` containers have been removed for a while without complaint
2.  we don't know of any other non-container customers still running on Java 8

## Prior Art and Alternatives

The general approach was modelled after how Google migrates its monorepo to new
Java versions:

1.  switch the JRE to the new version
2.  use the new JDK's `javac` with `-target 8 -source 8`
3.  remove those flags

Between each step is a period of time to evaluate breakages and potentially roll
back.

## Known Unknowns

On the JRE side, there are probably changes to performance, memory usage, and
anything else that could be affected by a major runtime change like this.

On the JDK side, it's not clear what amount of code changes will be required for
Java 11 compatibility. It's likely that certain libraries won't work perfectly
with Java 11, for example, or might require a major version upgrade to do so.
(My very brief initial testing quickly found some issues with
[Spock](http://spockframework.org/), for example.)

My hope is that the very slow rollout will help shake out these issues, and
we'll relax the timeline as needed.

## Operations

Individual operations teams who aren't using the containers will need to upgrade
their runtimes to Java 11 between now and (tentatively) Spinnaker 1.21. My
assumption is that companies that aren't using the container builds also have
the in-house Spinnaker and Java expertise to handle this runtime change without
too many problems.

## Future Possibilities

By the time this RFC is finished, Java 14 will have been released. This is the
next LTS release of Java. This RFC should be updated with any changes to the
plan so that it can be used as a template for the Java 14 upgrade, whenever we
decide that should happen.
