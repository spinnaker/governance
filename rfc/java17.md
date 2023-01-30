# Java 17 Migration

| | |
|-|-|
| **Status**     | Proposed |
| **RFC #**      | [62](https://github.com/spinnaker/governance/pull/325) |
| **Author(s)**  | Matt Gogerly ([`@mattgogerly`](https://github.com/mattgogerly)) |
| **SIG / WG**   | Platform SIG |

## Overview

To keep current with the evolution of Java, Spinnaker needs to move from Java 11
to Java 17. This RFC documents a process and timeline for doing so.

### Goals and Non-Goals

Goals:

*   Provide a process for moving official Spinnaker builds to the Java 17 JRE
*   Discuss ways we will encourage people using non-offical builds to also move
    to the Java 17 JRE
*   Provide a timeline for the final switch to a Java 17 JDK along with criteria
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

Once we upgrade to the Java 17 JDK, Spinnaker will no longer be able to run in a
Java 11 JRE. So the first (more complicated) step is to get everyone migrated to
a Java 17 JRE. Only after that is done can we switch to the Java 17 JDK.

For official Spinnaker containers, we control the JRE used to run Spinnaker. For
those releases, this proposal takes a relatively slow ramp to the Java 17 JRE
and provides an escape hatch back to Java 8 for users who experience problems.

There are also some unknown number of customers who aren't using the Spinnaker-
provided containers. We will need to do some outreach to make sure these users
convert to a Java 17 JRE too.

## Timeline

The timeline in which this was implemented is detailed below.

### 1.31 (early March)

Before we can upgrade to Java 17 we must first upgrade from Gradle 6 to Gradle 7.
The [compatibility matrix](https://docs.gradle.org/current/userguide/compatibility.html)
shows that Java 17 is supported by Gradle 7.3, however in the interests of not
adding more technical debt this RFC proposes we upgrade to the latest Gradle, 7.6.

Early testing suggests this shouldn't be too much of a hassle - I was able to compile
Orca and Clouddriver using Gradle 7 without _any_ changes.

### 1.32 (early April)

For our container builds, we'll first move just two (of approximately ten)
services to the new JRE. The two services will be `igor` and `front50`. By
default, anyone who uses Halyard to deploy these containers will get the Java 17
JRE for these services. Users who experience problems or aren't ready to use
Java 17 can use the `java11` variant by specifying `imageVariant: slim-java11` (or
`ubuntu-java11`) in their Halyard config file.

The release notes will prominently mention this fact. It will also be mentioned
in-channel in the release announcement to `#spinnaker-releases` and in the
release announcement email. We will strongly encourage people who opt for the
`java11` variants to contact `sig-platform@spinnaker.io` or `#sig-platform` to
discuss their concerns, which will allow us to adjust the timeline as needed.

Separately, postings to `#dev` will encourage developers to start running their
development environments under Java 17. Explanations of how to do so will be
posted to the channel. The [Spinnaker Developer
Guide](https://www.spinnaker.io/guides/developer/getting-set-up/) will also be
updated to strongly recommend using Java 17.

If issues are found in Spinnaker that are caused by running under a Java 17
runtime, we will switch the containers back to Java 11 and shift the timeline
back one release. Since we're providing the `java11` variants, we will not adjust
the timeline for any other reason.

### 1.33 (mid May)

_All_ containers built by the build process will now use the Java 17 JRE. As
before, the `java11` variants will offer a way to opt out of this change.

Other than that, everything from the previous section applies to this release as
well.

### 1.34 (mid June)

For this release, we will do two things:

1.  Remove the `java11` container variants.
2.  Compile with JDK 17 using the `-source 11 -target 11` flags. This produces
    bytecode that is compatible with Java 11. We will also add the
    `-PenableCrossCompilerPlugin=true` flag to CI builds, which compiles against
    a Java 11 JDK, preventing developers from using Java 11 APIs.

These changes are separable and don't _need_ to be done together. Either can be
delayed for a release if there are issues. The change in the next section
(tentatively scheduled for release 1.34) will not happen until both these
changes have shipped for an entire release cycle, however.

Since removing the `java11` containers removes the possibility of running with
the old JRE, the old statement of "we will not adjust the timeline for any other
reason" no longer applies. If we know for a fact (from previous discussions)
that removing the `java11` variants will cause problems for some customers, or if
a customer complains after the release, we can put back the `java11` containers
and delay this part (but keep the compiler change) until the next release.

Similarly, if the compiler change causes issues, we can revert that change and
push that out to the next release.

### 1.34 (early August)

The compiler flags will change from `-source 11 -target 11` to `-source 17 -target
17`. At this point, developers are able to commit Java 17 code to the
repositories. Undoing this change is possible, but will require some work. As
such, this should only be done after

1.  the `java11` containers have been removed for a while without complaint
2.  we don't know of any other non-container customers still running on Java 11

## Prior Art and Alternatives

This RFC is based mostly on the [Java 8 to Java 11 RFC](https://github.com/spinnaker/governance/blob/ff09e65ac34f537299b7e2e4386315ec126622b4/rfc/java11.md)

The general approach was modelled after how Google migrates its monorepo to new
Java versions:

1.  switch the JRE to the new version
2.  use the new JDK's `javac` with `-target 11 -source 11`
3.  remove those flags

Between each step is a period of time to evaluate breakages and potentially roll
back.

In addition this RFC includes a Gradle upgrade, which is long overdue.

## Known Unknowns

On the JRE side, there are probably changes to performance, memory usage, and
anything else that could be affected by a major runtime change like this.

On the JDK side, it's not clear what amount of code changes will be required for
Java 17 compatibility. It's likely that certain libraries won't work perfectly
with Java 17, for example, or might require a major version upgrade to do so.

My hope is that the very slow rollout will help shake out these issues, and
we'll relax the timeline as needed.

## Operations

Individual operations teams who aren't using the containers will need to upgrade
their runtimes to Java 17 between now and (tentatively) Spinnaker 1.33. My
assumption is that companies that aren't using the container builds also have
the in-house Spinnaker and Java expertise to handle this runtime change without
too many problems.

## Future Possibilities

In September 2023, immediately as this RFC is scheduled to complete, Java 21 is scheduled
to be released. This is the next LTS release of Java. This RFC should be updated
with any changes to the plan so that it can be used as a template for the Java
21 upgrade, whenever that takes place.
