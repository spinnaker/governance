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

_Currently the waiting executions of a pipeline runs in FIFO order. But due to certain project urgencies and release purpose, we quite often need to change the executions order of a pipeline._

_Implementing the Re-order functionality helps to run the waiting executions in the order the user wants_

### Goals and Non-Goals

Goals:

_Introduce an option through Spin CLI and Spinnaker dashboard(GUI) to move the specific waiting execution of a Pipeline towards UP/DOWN or Top/Bottom from the current position, which is set in FIFO order by default_

_What problems are not not trying to solve?_

## Motivation and Rationale

_Currently it is not possible If the user wants to prioritize specific execution of a pipeline. Adding Re-order feature from CLI/GUI will help end-users and operators to change the priority order of a waiting execution for release purpose or any project urgencies._


## Timeline
_The code for CLI command reorder implementation is almost ready for review. 
Planning to implement this feature in two Phases._

_Phase-1: completing CLI command implementation and make this feature available for customers to use and work on feedback if any._

_Phase-2: Implementing GUI for reorder functionality by using the backend that is available from CLI reorder implementation in Phase-1._

_Proposed timeline for Phase-1 would be 1-2 weeks as this feature is demoed for some Spinnaker users and got the feedback.
Phase-2 would be 3-4 weeks as there are some existing issues in the waiting executions area of Spinnaker dashboard that needs to be fixed._

## Design

_The Re-ordering waiting executions of a pipeline can be done through Spin CLI command and Spin dashboard._

_Introducing a new CLI command for reorder functionality of a waiting executions, the command will be used to Re-order waiting execution to UP/DOWN or Top/Bottom based on the current position of the provided execution._

_Example Spin CLI Command_
 >>spin pipeline execution reorder --execution-id 01GJQ601MC3SBACVNZB0XC5GHJ --reorder-action UP

_New buttons will be added in Spinnaker dashboard(GUI) for each waiting execution to move the specific execution towards UP/Down or Top/Bottom based on the current position of the execution._

_New Gate API endpoint will be created for external access_
>>:8084/pipelines/<execution_id>/reorder?reorderAction=<UP/Down/Top/Bottom>

_New Orca API endpoint will be created to access the same way_
>>:8083/pipelines/<execution_id>/reorder?reorderAction=<UP/Down/Top/Bottom>

_The main business logic of the feature lies in Spin Orca service, where the **CompoundExecutionOperator** will update the Redis repo using the existing class **RedisExecutionRepository** and update the pending executions to the user selected position in the Redis repo using the Class **RedisPendingExecutionService**._

_A new handler class **ReorderWaitingExecutionHandler** will be created in orca-queue handler package, to handle the reorder of a waiting execution._

**_Note_**

_The Re-ordering will not be done for the executions which are not in Waiting state, an appropriate error message should be displayed._

_In the Spinnaker dashboard(GUI), only the waiting executions should have buttons to reorder, buttons should be removed once the execution is started._


### Dependencies

_This proposal depends on existing services Spin CLI, Spin Deck, Spin Gate and Spin Orca._

_No new dependencies are needed, existing systems will be sufficient to implement this feature_

## Drawbacks

_Currently there is no Re-order functionality of an execution_
_Why should we **not** do this?_

## Prior Art and Alternatives

_There is an alternate method that can be followed by cancelling all the running pipelines and Re-run the executions one after another in the right order in which the user wants. But this is a tedious process to run for every time to change the order of executions._

_This feature will help the user to re-order the executions any number of times until they are in waiting state._


## Known Unknowns

**Knowns**

_Will be implementing re-order feature using CLI/GUI for a waiting executions of a pipeline_

_Re-order with CLI will be implemented and released first to stabilize the features_

**Unknowns**

_The current implementation uses RedisPendingExecutionService and need to check on the requirement of implementing different pending execution services like DualPendingExecutionService, InMemoryPendingExecutionService and SqlPendingExecutionService._

## Security, Privacy, and Compliance

_As this feature is plugin to the existing component, need to check the security aspects with Security SIG or Technical Oversight Committee._

## Operations

_A new CLI will be created to re-order the waiting executions, that user can run using Spin CLI_
_And waiting executions in GUI will have an additional option/button to re-order the executions_

## Risks

_What happens If this feature not implemented with other pending executions services like DualPendingExecutionService, InMemoryPendingExecutionService and SqlPendingExecutionService._

## Future Possibilities

