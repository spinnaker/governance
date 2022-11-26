# _Re-ordering waiting executions of a Pipeline_

_**IMPORTANT**: Fill in and remove all italicized text before submission.
If a section is not applicable, remove it._

| | |
|-|-|
| **Status**     | _**Proposed**, Accepted, Implemented, Obsolete_ |
| **RFC #**      | _Update after PR has been made_ |
| **Author(s)**  | _Jalander Ramagiri (`@rjalander`)_
| **SIG / WG**   | _Applicable SIG(s) or Working Group(s)_ |


## Overview

_Curreently the waiting execuitions of a pipelines runs in FIFO order. But due to certain project urgencies and release purpose, we quite often need to change the executions order of a pipeline._

_Implementing the Re-order functionality helps to run the waiting executions in the order the user wants_

### Goals and Non-Goals

Goals:

_Introduce an option through Spin CLI and Spinnaker dashboard(GUI) to move the specific waiting execution of a Pipeline towards UP/DOWN or Top/Bottom from the current position, which is set in FIFO order by default_

_What problems are not not trying to solve?_

## Motivation and Rationale

_Currently it is not possible If the user wants to prioritize specific execution of a pipeline. Adding Re-order feature from CLI/GUI will help end-users and operators to change the priority order of a waiting execution for release purpose or any project urgencies._


## Timeline
_The code for CLI command reorder implementation is almost ready for review. 
Plannig to implement this feature in two Phases._

_Phase-1: completing CLI command implementaion and make this feature available for customers to use and work on feedback if any._

_Phase-2: Implemeting GUI for reorder functionality by using the backend that is available from CLI reorder implementaion in Phase-1._

_Proposed timeline for Phase-1 whould be 1-2 weeks as this feature is demoed for some users and got the feedback.
Phase-2 would be 3-4 weeks as there are some existing issues in the waiting executions area of Spinnaker dashboard that needs to be fixed._

## Design

_The Re-ordering waiting executions of a pipeline can be done through Spin CLI command and Spin dashboard._

_Introducing a new CLI command for reorder functionality of a waiting executions, the command will be used to Re-order waiting execution to UP/DOWN or Top/Bottom based on the current position of the provided execution._

_Example Spin CLI Command_
 >>spin pipeline execution reorder --execution-id 01GJQ601MC3SBACVNZB0XC5GHJ --reorder-action UP

_New buttons will be added in Spinnaker dashboard(GUI) for each waiting execution to move the specific execution towrds UP/Down or Top/Bottom based on the current position of the execution._

_New Gate API endpoint will be created for external access_
>>:8084/pipelines/<execution_id>/reorder?reorderAction=<UP/Down/Top/Bottom>

_New Orca API endpoint will be created to access the same way
>>:8083/pipelines/<execution_id>/reorder?reorderAction=<UP/Down/Top/Bottom>

_The main business logic of the feature lies in Spin Orca service, where the **CompoundExecutionOperator** will update the Redis repo using the class **RedisExecutionRepository** and update the pending executions to the user selected position in the Redis repo using the Class **RedisPendingExecutionService**

**_Note_**

_The Re-ordering will not be done for the executions which are not in Waiting state, an appropriate error message should be displayed._

_In the Spinnaker dashboard(GUI), only the waiting executions should have buttons to reorder, buttons should be removed once the execution is started._


### Dependencies

_This proposal depends on existing services Spin CLI, Spin Deck, Spin Gate and Spin Orca_
_No new dependencies are needed, existing systems will be sufficient to implement this feature_

## Drawbacks

_Currently there is no Re-order functionality of an execution_
_Why should we **not** do this?_

## Prior Art and Alternatives

_What other approaches did you consider?_
_What existing solutions are close but not quite right?_
_How will this project replace or integrate with the alternatives?_

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
_What factors may complicate your project?_

_Include: security, complexity, compatibility, latency, service immaturity, lack of team expertise, etc._

## Future Possibilities

_What are the natural extensions and evolution of this RFC that would affect the project as a whole?_

_Feel free to dump ideas here, they're meant to get wheels turning, and won't affect the outcome of the approval process._
