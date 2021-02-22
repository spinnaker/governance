# Amazon ECS End to End Test Proposal

|               |                                                  |
|---------------|--------------------------------------------------|
| **Status**    | **Accepted**                                     |
| **RFC #**     | https://github.com/spinnaker/governance/pull/169 |
| **Author(s)** | Allison Stanko (@allisaurus)                     |
| **SIG / WG**  | AWS SIG, Platform SIG                            |


## Overview

This document proposes adding Amazon ECS provider tests to the Spinnaker 
nightly builds in order to ensure breaking changes to the provider are 
not merged into community release artifacts. 

Since longer-term plans to retire the Jenkins-run end-to-end (e2e) tests 
[are known](https://github.com/spinnaker/governance/pull/154/files#diff-856398ec244cbaddc659e4189e904a87R23-R26) 
but not yet scoped/scheduled, this RFC presents a design for tests 
built for the existing e2e setup, as well ideas about how these tests 
could be mostly supplanted by integration tests or future CI workflows.


### Goals and Non-Goals

Goals:

* Clarify the benefit of adding ECS end to end tests to the nightly builds
* Outline high-level test design and requirements
* Call out specific areas where more information is needed 

Non-Goals:

* Modifying the existing AWS e2e tests or porting them over to some future test process
* Exhaustively testing every possible ECS server group configuration
* Address Deck UI testing for the ECS provider (covered by [`Deck` functional tests](https://github.com/spinnaker/deck/tree/master/test/functional/cypress/integration/ecs))


### Terms as used in this doc

 - **e2e tests** : end-to-end tests; those that exercise all Spinnaker 
   services involved in the deployment of a server group or other atomic operation.
 - **integration tests** : tests that run in a single repo, against 
   a live application (such as `clouddriver`), but do not require standing 
   up a fully deployed Spinnaker installation. Ideally run as part of 
   some automated workflow (e.g., pull request submission)


## Motivation and Rationale

Currently, the Spinnaker nightly builds run end to end tests that exercise 
integrations with most cloud providers, including AWS and Google Cloud. The ECS 
provider lacks these, meaning its users must rely on manual testing to 
find, report, and address breaking changes. This has resulted in manual 
testing & contributions ramping up around scheduled releases, when 
most customers upgrade and first exercise new features. 

Examples of regressions or breaking changes caught by manual testing:

* https://github.com/spinnaker/clouddriver/pull/4340 
    (flipped unit test condition allowed previously supported behavior to break)
* https://github.com/spinnaker/deck/pull/8316 
    (while this occurred in Deck, good example of a core change which had downstream unintended consequence for ECS users)
* https://github.com/spinnaker/spinnaker/issues/5551 
    (issue caught by customer running 1.19 release candidate)


Adding nightly build e2e tests for the ECS provider would alleviate this by:
* surfacing major issues when they're introduced, vs. during the (already hectic) 
  days/weeks surrounding a release
* gating major breaking changes from being introduced into community build 
  artifacts (can be fixed or reverted as appropriate)
* Building confidence that code-base wide linting, refactoring, or other 
  updates do not impact core functionality of the ECS provider. 
* Free up resources for ECS contributors to work on new features and 
  performance enhancements.


## Timeline

The current goal is to have one ECS e2e test in place prior to the 1.25.x/February release.
Subsequent test cases would be added in the e2e test suite or as integration 
tests depending on the progress of the community integration test effort 
(more info below in [_Prior art and alternatives_](#prior-art-and-alternatives)).

More granular dates and milestones will be added as implementation proceeds. 

**Milestones**

* [x] Write [`ecs_server_group_test`](https://github.com/allisaurus/buildtool/blob/rfc-artifacts/testing/citest/tests/ecs_server_group_test.py), run successfully on local dev stack.
* [ ] Write S3 file upload agent & configuration code *(in progress)*
* [ ] Draft infrastructure-as-code (AWS CloudFormation) templates for test resource/account setup *(in progress)*
* [ ] Publish e2e test PR for community review
* [ ] Validate, finalize community resource/account configuration
* [ ] Merge PR

## Design

### What to test

* NOTE: most of this subsection is a duplicate of 
[spinnaker #5988](https://github.com/spinnaker/spinnaker/issues/5988), 
provided here for context.

#### Server Group creation

Most ECS code paths define logic for deploying server groups, so this 
is the operation we want to exercise in the nightly builds. Re: mechanics, 
customers can define the bulk of their container application within 
the server group in one of two ways: 

1. Providing **explicit inputs** for their docker image, resource limits, 
   network mode, etc. in the server group definition

2. Providing a **JSON artifact** which defines the container(s) they 
   want to run and their associated metadata (similar to a Kubernetes helm chart), 
   plus any network or server level settings in the server group definition

The first method is the longest-supported, however the second is popular 
and supports the greatest variety of configurations. It will also 
be the path customers need to take in order to leverage new/future 
task definition fields, so we expect its use to grow over time. 


#### Use of, but not creation of, load balancing & firewalls

The ECS provider differs from most other cloud providers in that it 
_lacks_ custom logic around load balancers and firewalls, since any 
of these in use by ECS can be directly managed through 
the Spinnaker AWS provider. 

However, ECS server groups do typically _utilize_ both - load balancer 
health checks being most common (vs. provider), and security groups 
being required for launching on AWS Fargate. Therefore testing ECS 
server groups will require prefiguring at least one (security group) 
or both resources outside of ECS-specific code paths. At runtime 
this could be done by invoking the AWS provider (not recommended, 
duplicates behavior under test) or directly via the `aws-cli` 
(already available in the test env).

Alternatively, these resources can be created once in the hosting account 
and reused between tests (preferred; discussed further down).


#### Test Cases

Given the above, the test case most representative of ongoing ECS provider 
use would:

1. Use an artifact to define a container application
2. Use an application load balancer + healthchecks

Subsequent major test cases to cover in an end-to-end way include using 
the older `targetGroup` fields vs. the newer `targetGroupMappings`, 
using provider health checks intead of load balancer, and with service 
discovery backed by [AWS Cloud Map](https://aws.amazon.com/cloud-map/). 
These represent known customer use cases and cover forking code 
paths in the ECS provider deployment logic.

##### Prioritized list of initial test cases:

1. **Create ECS server group with an artifact, using** 
   **load balancer health checks.**
    * First e2e test case to add; design discussed below.
2. Create ECS server group with direct inputs, using 
   load balancer health checks and "legacy" target group fields.
3. Create ECS server group with an artifact that defines container health 
   checks, using provider health checks and service discovery.


### Required changes

#### Actual ECS e2e test workflow

The first test would be modeled after the 
[google_server_group_test](https://github.com/spinnaker/buildtool/blob/master/testing/citest/tests/google_server_group_test.py) 
in structure and the [aws_smoke_test](https://github.com/spinnaker/buildtool/blob/master/testing/citest/tests/aws_smoke_test.py) 
w/r/t observers.

High-level test workflow:

0. ASSUMES: load balancer, target group, and S3 bucket available in account or otherwise bootstrapped.
1. Create a Spinnaker application with the AWS and ECS providers enabled.
2. Create a server group (step also uploads JSON file to S3).
3. Resize the server group.
4. Disable the server group.
5. Destroy the server group.
6. Delete the application.

* Locally functional e2e test code available [here](https://github.com/allisaurus/buildtool/blob/rfc-artifacts/testing/citest/tests/ecs_server_group_test.py).


#### Test environment : AWS Account changes

The ECS provider itself does not own any credentials, but points to a configured 
AWS provider account to deploy to. The ECS provider can leverage an 
already-configured Spinnaker AWS account in the test environment, so 
it would only need several additional resources to successfully 
deploy a server group (e.g., Amazon ECS service):

* An (empty) ECS cluster
  * NOTE: Provisioning EC2 instances isn't required when deploying to [AWS Fargate](https://aws.amazon.com/fargate/)
* A publicly available or ECR-hosted container image
* An [AWSServiceRoleForECS](https://docs.aws.amazon.com/AmazonECS/latest/developerguide/using-service-linked-roles.html) 
  IAM Role in the hosting account (typically created automatically on ECS cluster creation)
* An `ecsTaskExecutionRole`  IAM Role which grants ECS permission 
  to pull ECR hosted images.

The following network resources are also needed and can either be reused 
from the existing AWS provider tests, or created separately to maintain 
stronger isolation between tests (or if existing network config is not compatible).
* A VPC with subnets that can 
  [communicate with ECS](https://docs.aws.amazon.com/AmazonECS/latest/developerguide/launch_container_instance.html)
* A security group that allows for inbound communication from the load balancer 
 and outbound calls to ECS.


**Resource Creation**

Most of these resources make sense to create once in the hosting account and 
leave "alive" between test runs because they're free (ECS cluster, 
VPC + public subnets, IAM Roles, ECR repository) and can be created via 
CloudFormation or Terrafrom for easy en mass redeployment/tear down. 

The two exceptions to this are 1) **the container image**, and 2) **the load balancer**.

1. The actual container image in ECR probably still makes sense 
to configure once ahead of time, because it's very [low cost](https://aws.amazon.com/ecr/pricing/) 
to store and more difficult to configure during the execution of the test.
    * NOTE: The network configuration required to pull a public vs. ECR-hosted 
      image would be costlier, but a public image would require less in 
      the way of runtime dependencies & `hal` configuration. 

2. The load balancer + target group creation can be accomplished in a couple 
ways depending on community preference:
    * Could be created during the test (via `aws-cli`), since it incurs cost 
      and is a resource typically created/killed during other Spinnaker e2e tests
    * Could be created ahead of time in the same Cloudformation stack as 
      the ECS cluster to minimize API calls during execution. 
    * Could be created during the test via calls to the AWS provider, although 
      this would duplicate executions of this code path during testing. 


#### Test environment: Spinnaker Configuration

To support ECS, the Spinnaker deployment under test would need to be configured with:
* The ECS provider enabled
    * New `EcsConfigurator` needed to define `hal` commands
* An ECR docker registry _per region under test_ (unless cross-region pulls are OK - supported, but more $$)
    * can leverage existing [DockerConfigurator](https://github.com/allisaurus/buildtool/blob/ecs-test/dev/validate_bom__config.py#L924), 
      but would need to accomodate `--password-command` flag and regional registries.
* An S3 artifact account 
    * can leverage existing 
      [S3StorageConfiguratorHelper](https://github.com/allisaurus/buildtool/blob/ecs-test/dev/validate_bom__config.py#L142) 

Alternatives to consider:
* artifact location: S3 seems to make the most sense, but maybe another location preferred?
* container image location: pulling from a different private repo (like GCR) is theoretically possible, but would require storing registry credentials in [AWS Systems Manager](https://docs.aws.amazon.com/systems-manager/latest/userguide/systems-manager-parameter-store.html)


## Prior Art and Alternatives

In parallel to adding e2e tests, the ECS team is also working on `clouddriver` 
integration tests. Integration tests are a better venue for more exhaustive 
testing, especially for configurations that require few supporting resources. 
Re: ECS server groups, test cases that do not require an artifact 
(see **Test case #2**, above) or use a load balancer come to mind as good 
first candidates for these tests, in addition to basic read opertions 
on `/loadBalncers`, `/credentials`, etc.

These kinds of tests also align with where the community wants to shift 
the bulk of future testing, and it's more conducive to automation on PRs, 
which would help detect problems before they even make it to `master`.
So far a number of `CreateServerGroup` operations and Amazon ECS
resource controller tests have been built, with tests for other atomic operations
anticipated. 

See existing `clouddriver-ecs` integration tests [here](https://github.com/spinnaker/clouddriver/tree/master/clouddriver-ecs/src/integration).


### Why not just integration tests?

My ideal scenario would be to have one (or very few, strategically chosen) 
e2e ECS test running to ensure the contracts between services are intact prior to 
release, but with most permutations of the available atomic operations and 
controllers run as integration tests on PR submission. This way, individual service
code (e.g., `clouddriver`) is more thoroughly vetted prior to e2e test runs, but essential 
operations are still fully validated in a "prod"-like way before being published as a release.

After the initial test case is added and the integration test suite is 
more mature, we can evaluate the need for additional e2e test cases (or even porting 
of the tests to a new, Java-based e2e test setup, which is desired but not yet designed/scoped).


## Known Unknowns

Some things may impact this proposal which I'm hoping the RFC process can clear up:

### About the test design

* Whether any Jenkins/e2e test environment configuration would be impacted 
  by this change but is not immediately obvious given the [`buildtool`](https://github.com/spinnaker/buildtool) code.
* Whether testing a provider that relies on another provider (e.g, AWS) 
  might cause issues with an unknown (to me) authentication or test mechanism.
* What's the best way to test any new configuration + test code prior to merging? 
* Unknown/show-stopping restrictions on the AWS account currently used during tests.
* How many regions are running the e2e tests, and would we need a unique ECR repository for each?

### About the future of testing / Spinnaker community CI tooling

* Whether an alternative end to end testing mechanism (perhaps in the 
  monorepo?) will be available soon enough to start building on in the next ~3 months.


## Security, Privacy, and Compliance

Creating new AWS resources may risk exposing parts of the test environment 
via incorrect configuration. However there are established best practices 
we can follow to mitigate this risk:

* The ECR repository can be scoped to pull-only access by the IAM Role used for testing.
* VPC subnets can be configured with security group(s) to prohibit 
  ingress except to the specified load balancer.
* (Optional) If we want to prevent ALL ingress/egress from the internet, ECS and ECR 
  can also be accessed from within a completely private VPC (w/ additional 
  configuration and cost considerations)
* Additional ECS permissions needed on the test roles can be scoped 
  down to specific resource types, names, and/or tags. For example: the IAM Role 
  assumed by the test can be restricted to passing only roles that are assume-able by
  Amazon ECS (vs. some other entity or service)

If this proposal is accepted, more granular policies and configurations 
will be provided prior to test contribution.


## Operations

The main drawback of this proposal is the additional overhead required 
to set up, run, and debug potential test failures. However, since all 
the tooling and dependencies needed to interact with AWS already exist 
within the test stack, the additional surface area is mostly contained 
to the contents of the test itself and any ECS-specific knowledge 
needed to debug failures. 

* Additional resources must be created and maintained in hosting account
    * Mitigation: Use an infrastructure as code mechanism to modify/recreate (template can live alongside test, maybe?)
* Test flakiness and overhead for build cops & release managers
    * Mitigation: Pre-merge testing, maybe non-blocking test runs prior to triggering build fails? (needs input from community)
* Upstream/AWS-side API errors (already a risk for AWS provider). 
    * Mitigation: Hopefully rare, but can be clearly identified by API responses/error msgs
* Lack of ECS expertise on build cops team to triage
    * Mitigation: GitHub team for “ecs-triage” and/or “ecs-triage” team in slack to engage for problems. 
      Also: copious documentation contributed with test code.


## Future Possibilities

Big upheavals in how customers use the ECS provider or how the 
community builds & tests releases will impact the longevity of 
any end to end testing added today. Examples may include:

* Elimination of all end to end testing prior to Spinnaker releases 
  (assume then there would be an alternative that could be leveraged)
* Core providers whittled down to excluded ECS or all cloud providers 
  extracted to live as plugins (would then leverage new system/guidance; 
  any existing tests could potentially be extracted and exercised by customers)
* Abandonment of the ECS provider by customers makes these tests 
  unnecessary (given current use and the availability of the AWS 
  SIG as a support channel, seems unlikely near term).
