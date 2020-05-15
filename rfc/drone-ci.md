# Drone CI Spinnaker Stage Type
| | |
|-|-|
| **Status**     | **Proposed**, Accepted, Implemented, Obsolete |
| **RFC #**      | ***TBD*** |
| **Author(s)**  | Victor Odusanya (`@Odusanya18`), Cameron Motevasselani (`@link108`) |
| **SIG / WG**   | CI SIG |

## Overview

This proposal seeks to enable Drone CI Support for Spinnaker. This proposal takes advantage of plugability of CI APIs in `igor` and 	`orca` and implementation consists additions to `deck`, 	`orca` and `igor`. This is to allow for seamless Drone CI integration with existing Spinnaker setups.

### Goals and Non-Goals


* Goals
	* Support Drone CI Pipeline Stage Type.
	* Support Drone CI Type  `hal` configurability.
	* Support Drone CI Stage Configuration.
	* Support passing pipeline context to Drone CI Build as environment.
	* Support for Drone CI Triggers and Hooks.
	* Support Autodiscovering Drone CI Users.
	* Support retrieving Drone Projects for autodiscovered users.
	* Support Drone CI Stage Start, Stop, Pending, etc lifecycles.
	* Support retrieving Drone execution metadata in Spinnaker execution permalinks.
	* Support caching Drone Projects retrieval in Spin- redis
	* Drone CI Build Approvals/Decline/Promotion (scope may have to be evaluated)
* Non-Goals
	* Drone CI Cron Support.
	* Drone CI Management Lifecyle outside Actions directly related to CI.
	 

_What problems are you trying to solve?_
Ease of use triggering Drone CI Support.

_What problems are not not trying to solve?_
Non-Build related  activity on a Drone CI server.

## Motivation and Rationale

_What is the current state of the world?_

Currently Spinnaker Supports five CI systems; -  
-  [AWS CodeBuild](https://www.spinnaker.io/setup/ci/codebuild/)
-   [Google Cloud Build](https://www.spinnaker.io/setup/ci/gcb/)
-   [Jenkins](https://www.spinnaker.io/setup/ci/jenkins/)
-   [Travis CI](https://www.spinnaker.io/setup/ci/travis/)
-   [Wercker](https://www.spinnaker.io/setup/ci/wercker/)
- _Concourse**_
 
But Current Drone CI users may have issues integrating with Spinnaker and may have to come up with workarounds albeit with not as rich support as the currently supported CI systems meaning a choice may have to be made between loose/no integration with Spinnaker and choosing to use Spinnaker. 

The [REST interface Drone CI](https://docs.drone.io/api/overview/) provides is far too simple and consistent to have to make such choice. 

_Why is this change being proposed?_
Drone CI users like in [#1476]([https://github.com/spinnaker/spinnaker/issues/1476](https://github.com/spinnaker/spinnaker/issues/1476)) do not have any support for Drone by Spinnaker.


_What are the benefits for users (end-users, engineers, operators, etc.)?_
* End Users
	* Click and run Drone CI Builds
* Engineers
	* Accessibilty to Drone CI's modern CI platform from Spinnaker
	* Ease of use and access to Spinnaker's exellent stage support and metadata
	* Ability to treat Drone stages with support on par with Jenkins stages.
* Operators
	*  Standard actionable alerts as for Spinnaker stages.

_Who have you identified as early customers of this change?_
- `@Odusanya18`  

## Timeline

***TBD***

## Design

_What exactly are you doing?_

#### Drone Orca Build Lifecycle
A Drone CI Build stage will need to support common CI stage task lifecycles including;

 - Start Build
 - Monitor Build
 - Stop Build
 
We propose adding a `DroneStage` component that implements `StageDefinitionBuilder` to `orca-igor` with a `taskGraph` built from the task lifecycles above:
```java
...
public class DroneStage implements StageDefinitionBuilder {

  public void taskGraph(...) {  
		builder.withTask("startDroneTask", StartDroneTask.class)  
        .withTask("monitorDroneTask", MonitorDroneTask.class)  
        .withTask("stopDroneTask", StopDroneTask.class);
  }
  
}
```
The default `CIStageDefinition` implementation should be good enough for this RFC.

#### Igor Drone Client
Drone-Igor will need an API client to communicate with a Drone CI Server installation;
`com.netflix.spinnaker.igor.drone.client` and we propose generating an API client and corresponding `models` from the [official Drone CI API reference](https://docs.drone.io/api/) for the following actions:
  * Queries 
	  * [# User List](https://docs.drone.io/api/users/users_list/) 
	  * [# Repo List](https://docs.drone.io/api/repos/repo_list/)
	  * [# Build Info](https://docs.drone.io/api/builds/build_info/)
  * Commands
	  * [# Build Approve](https://docs.drone.io/api/builds/build_approve/) 
	  * [# Build Decline](https://docs.drone.io/api/builds/build_decline/)
	  * [# Build Restart](https://docs.drone.io/api/builds/build_start/) 
	  * [# Build Stop](https://docs.drone.io/api/builds/build_stop/)
	  * [# Build Create](https://docs.drone.io/api/builds/build_create/)

#### Igor Drone Build Monitor
Drone CI Builds can be monitored from Igor with a pollable `DroneBuildMonitor` that implements `PollAccess` and `PollingMonitor` and extend `CommonPollingMonitor` behaviour.
```java 
public interface DroneBuildMonitorInterface extends PollAccess,PollingMonitor { 
 
 void poll(...);  
 
 void pollSingle(....);  
 
 void onApplicationEvent(...);  
 
 String getName();  
 
 boolean isInService();
 
 Long getLastPoll();
 
 int getPollInterval();
 
 boolean isPollingEnabled(); 
  
}
```

This means optionally caching static Drone CI metadata, like `UserList`, `RepoList` and so forth, using the exiting redis cache.
```java
public class DroneCache {}
```
#### Igor Drone REST APIs
A `DroneController` exposes consumable REST Drone CI APIs on igor for orca. Raising a need to organize Drone operations on igor into a service component easily consumable in the `DroneController`, the service implements `BuildProperties` and `BuildOperations`.
```java
public interface DroneServiceInterface extends BuildProperties,BuildOperations {

	Map<String, ?> getBuildProperties(String job, GenericBuild build, @Nullable String fileName);
	
	List<GenericGitRevision> getGenericGitRevisions(String job, GenericBuild build);
	
	GenericBuild getGenericBuild(String job, int buildNumber);  
	
	int triggerBuildWithParameters(String job, Map<String, String> queryParameters);
	
	List<?> getBuilds(String job);  
	
	JobConfiguration getJobConfig(String jobName); 
}
```

_What is this change being proposed?_
- Drone Additions to igor service.
- Drone Additions to orca-igor.

### Dependencies

_What existing internal and external systems does this proposal depend on?_

- Drone Server (optional on enable)
- Igor
- Orca-Igor
- Gate
- Deck

## Drawbacks

_Why should we **not** do this?_
***TBD***

## Prior Art and Alternatives

_What other approaches did you consider?_

 1. [#1476](https://github.com/spinnaker/spinnaker/issues/1476)
 2. Spinnaker Scripts
 
_What existing solutions are close but not quite right?_
 1. [#1476](https://github.com/spinnaker/spinnaker/issues/1476)
 2. Spinnaker Scripts

_How will this project replace or integrate with the alternatives?_
Spinnaker-Native Low effort Drone CI integration

## Known Unknowns

_What parts of the design do you expect to resolve through the RFC process?_
_What parts of the design do you expect to resolve through implementation before stabilization?_
_What related issues do you consider out of scope for this RFC that could be addressed in the future?_

## Security, Privacy, and Compliance

_What security/privacy/compliance aspects should be considered?_

_If you are not certain, never assume there aren't any._
_Always talk to the Security SIG or Technical Oversight Committee._

## Operations

_Are you adding any new, regular human processes, or extra work for any users?_
_What telemetry is needed to verify correct behavior?_
_What tooling needs to be introduced for on-going support?_

## Risks

_What known risks exist?_
- Lack of Expertise
- Drone Immaturity

_Include: security, complexity, compatibility, latency, service immaturity, lack of team expertise, etc._

## Future Possibilities

_What are the natural extensions and evolution of this RFC that would affect the project as a whole?_

_Feel free to dump ideas here, they're meant to get wheels turning, and won't affect the outcome of the approval process._
