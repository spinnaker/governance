# Allow Feature Backports Using Flags

| | |
|-|-|
| **Status**     | _**Proposed**, Accepted, Implemented, Obsolete_ |
| **RFC #**      | [#303](https://github.com/spinnaker/governance/pull/303) |
| **Author(s)**  | @dogonthehorizon (Fernando Freire) |
| **SIG / WG**   | Platform |

## Overview

Amend the existing [backport policy][policy] to allow for new features or
backwards incompatible changes in behavior provided they are introduced with
opt-in feature flags.

[policy]: https://spinnaker.io/docs/community/contributing/code/releasing/#release-branch-patch-criteria

### Goals and Non-Goals

#### Goals

- Gather feedback about new features or modified behavior faster than a major
  release
- Increase the likelihood of catching breaking UX changes before the broader
  community

#### Non-Goals

- We do not describe a method for feature flagging in plugins

## Motivation and Rationale

Contributors are currently faced with a few options when adding or modifying
behavior in Spinnaker:

- Wait for a new major release on an uncertain timeline to add new
  functionality
- Create an extension project, modify behavior, and add to their distribution
  of Spinnaker
- Introduce a plugin, if possible, to add or modify behavior

All three options compromise on time, effort, or both in order to introduce
changes that move the platform forward.

Enabling contributors to add new and modified features behind flags in existing
releases means that we can test new changes on a much faster cadence with our
users. This, in turn, increases the likelihood that quality improves as users
give feedback on changes to the project.

Armory would like to be an early user of this change in order to run product
experiments with our existing customers on a faster cadence than major
releases.

## Timeline

We propose that this change take effect immediately in the [backport
policy][policy], and relevant contributor documentation be updated to describe
the facilities for feature flagging in the project.

## Design

### Feature flags

Feature flags are already supported within the project.

To review, JVM services accomplish this through a combination of
`ConditionalOnProperty` annotations on relevant classes and configuration of
the service itself.

As an example, introducing a new endpoint to Gate would involve the following:

```
package com.netflix.spinnaker.gate.controllers;

import io.swagger.annotations.ApiOperation;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/my/proposed/endpoint")
@ConditionalOnProperty("services.gate.featureFlag.proposedEndpoint")
public class ProposedEndpointController {

  public ProposedEndpointController() { }

  @ApiOperation(value = "Get a greeting for the given name", response = String.class)
  @RequestMapping(value = "/{name}", method = RequestMethod.GET)
  String getGreeting(@PathVariable("name") String name) {
    return "Hello " + name;
  }
}
```

Then anytime a user configured the `services.gate.featureFlag.proposedEndpoint`
field they would enable this new endpoint in their project.

Feature flags in Deck are [defined as part of the `feature` config
block](https://github.com/spinnaker/deck/blob/966862b2caf82b5c9879cf418a7c79ec51152dfd/packages/app/src/settings.js#L105-L130)
and set either through runtime environment variables or via `settings.js`.
Behavior is moderated within Deck by accessing the [`SETTINGS` object provided
by `@spinnaker/core` to verify a flag is
set](https://github.com/spinnaker/deck/blob/7202efd54ad0b048d5c1f45c24162619b25be844/packages/kubernetes/src/kubernetes.module.ts#L95-L97).

### Modifying existing behavior

In cases where backwards incompatible changes are made to the project we should
make sure to feature flag the particular behavior so that it doesn't catch
users by surprise. If modifying existing behavior leaks changes beyond the
scope of a feature flag then we should not consider this behavior for a
backport. This constraint is left as a judgement call for Approvers in the
project, and is subject to change if we find project stability negatively
impacted by this change.

### Dependencies

No new libraries, dependencies, or external systems are needed for this RFC.

## Drawbacks

This places additional burden on reviewers and approvers to ensure that PRs
marked as backport candidates adhere to this change in policy.

## Prior Art and Alternatives

The Spinnaker project previously made heavy use of feature flags for new
features, however they were not typically included in existing releases. As the
community makeup has changed, we've seen less emphasis on feature flags as a
means of iterating on the project. We'd like to recommit to this policy through
this RFC.

One could also consider plugins to be feature flags of a sort. Relative to
feature flags within the projects themselves, plugins introduce more
operational and maintenance complexity than the existing projects themselves.
Feature flags are a mid-term compromise to enable product innovation that does
not significantly impede our movement towards a "lean core" project model.

## Known Unknowns

Out of scope for this RFC is a general emphasis on feature flags for any new
behavior in the platform. We intend to document further RFCs for this purpose.

## Security, Privacy, and Compliance

It is possible that contributors will submit code that exposes a security,
privacy, or compliance issue within the project. The reintroduction of feature
flags provides additional opportunities to catch such behavior before it
becomes the default in the project.

## Operations

This RFC introduces additional checks for Reviewers and Approvers to ensure
that new or modified functionality is behind a feature flag in existing release
branches. No additional telemetry or tooling needs to be introduced.

## Risks

If a dependency within the project is updated (e.g. a new version of spring is
required for a feature flag) there is a risk that transitive dependency will
have unintended consequences in other parts of the codebase. In such cases we
should prioritize the stability of the project over enabling risky behavior.
These cases may be decided at the discretion of the Reviewer and/or Approver on
the PR in question.

Extensions or plugins that depend on code that is not part of the JVM or the
Deck extension system will face difficulty in introducing feature flags. Such
projects are outside the scope of this RFC, since the core Spinnaker projects
all exist within the JVM or the Deck project.

## Future Possibilities

### More general feature flagging

Out of scope for this RFC is a general emphasis on feature flags for any new
behavior in the platform. We intend to document further RFCs for this purpose.

We would eventually like to see a pattern of feature flagging that looks
something like this:

- New major feature or change is introduced via RFC
- Implementation is added as a feature flag to release 1.X, marked as alpha in
  documentation
- Implementation is modified based on feedback in release 1.X+1, marked as beta
  in documentation
- Implementation is promoted in release 1.X+2, marked as generally available in
  documentation

### Tooling for feature flag rollouts

It may be worth exploring the ability for annotations in JVM projects to be
introduced that enable feature flagging and assist with the rollout strategy
proposed above.

### Feature flags for installation/configuration tools

It would be useful to iterate on Spinnaker's existing configuration format in
ways that do not unduly force users to migrate their configurations. Before we
could accomplish this, however, we'd need to sort out the relationship between
the Halyard, Operator, and Kleat projects.
