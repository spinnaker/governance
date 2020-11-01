# Improve the sql storage performance by using bulk save api

| | |
|-|-|
| **Status**     | _**Proposed**, Accepted, Implemented, Obsolete_ |
| **RFC #**      | https://github.com/spinnaker/gate/pull/8368 | https://github.com/spinnaker/orca/pull/3763 | https://github.com/spinnaker/front50/pull/3763
| **Author(s)**  | Sanjeev Thatiparthi (@sanopsmx) (https://github.com/sanopsmx)
| **SIG / WG**   | sig-security

## Overview

 This is valid only for sql storage.

 When we have multiple pipelines to save , we need to call front50 save api serially one after the other.
 This reduces the performance of front50 save api.

 We improve the front50 save performance by using bulk insert query.
 This will help save many pipelines at once which greatly improves the performance of front50.
 We need to write a bulk save api which accepts an list of pipelines in a single rest api call
 and save them in front50 using bulk insert query.

### Goals and Non-Goals

 We improve the front50 save performance by using bulk insert query.
 This will help save many pipelines at once which greatly improves the performance of front50.
 We need to write a bulk save api which accepts an list of pipelines in a single rest api call
 and save them in front50 using bulk insert query.

## Motivation and Rationale

To improve the performance of multiple saving of pipelines at once using bulk insert sql query.

## Timeline

Already Implemented

## Design

1. Deck calls the gate api:

   1.1	url: 		http://<ip address>:8084/pipelines/bulksave
   method: 	POST
   payload:	[{"keepWaitingPipelines":false,"limitConcurrent":true,"application":"apptest","spelEvaluator":"v4",
   "lastModifiedBy":"admin","name":"pipeline_test","stages":[{"refId":"1","requisiteStageRefIds":[],
   "type":"wait","name":"Wait","waitTime":30}],"index":3,"id":"66a5c77b-3998-495d-ad61-117313c5800c",
   "triggers":[],"updateTs":"1599652266000"},{"keepWaitingPipelines":false,"limitConcurrent":true,
   "application":"apptest","spelEvaluator":"v4","lastModifiedBy":"admin","name":"pipeline_test1",
   "stages":[{"refId":"1","requisiteStageRefIds":[],"type":"wait","name":"Wait","waitTime":30}],
   "index":4,"id":"66a5c77b-3998-495d-ad61-117313c5800c","triggers":[],"updateTs":"1599652266000"}......]


2. Gate api flow:

   2.1	The post request sent by the deck will hit the PipelineController Rest Controller endpoint.

        com.netflix.spinnaker.gate.controllers.PipelineController.bulksavePipeline(Body pipeline..)
   2.2 	TaskService will create a new task(synchronous job) for saving the pipeline.
        taskService.createAndWaitForCompletion(operation)

   2.3	The above new task will call the orca service post api to save the pipeline.

        orcaService.doOperation(@Body Map<String, ? extends Object> body)
        url: 		http://<ip address>:8083/ops
        method: 	POST

3.  Orca api flow:

    3.1	The post request sent by the gate will hit the OperationsController Rest Controller endpoint.

        com.netflix.spinnaker.orca.controllers.OperationsController Map<String, String> ops(@RequestBody Map input) {

    3.2	The above method will start a new task with QueueExecutionRunner.kt.
    3.3	The QueueExecutionRunner will call the StartExecutionHandler.kt.
    3.4	The StartExecutionHandler.kt will call the SavePipelineTask.java.
    3.5	SavePipelineTask will call the front 50 rest api.
        Response response = front50Service.savePipelineList(List<pipeline>);
    	url: 		http://<ip address>:8080/pipelines/bulksave
    	method: 	POST

4.  Fiat api flow:

    4.1	Front 50 will check with fiat for permissions.

        @PreAuthorize( "@fiatPermissionEvaluator.storeWholePermission() and
        authorizationSupport.hasRunAsUserPermission(List<pipeline>)")

    4.2 storeWholePermission() will check if the permissions object is not null..
    4.3	hasPermission() will check whether that particular resource(APPLICATION) has ‘WRITE’ permission to save or not.
    4.4	authorizationSupport.hasRunAsUserPermission() check happens at front50,
        which calls fiat to check whether that particular resource(Application) has ‘EXECUTE’ permission or not.

5.  Front50 api flow:

    5.1	The post request sent by the orca will hit the PipelineController Rest Controller endpoint.

        com.netflix.spinnaker.front50.controllers.OperationsController.bulksave(
          		@RequestBody List<Pipeline> pipelineList,
        @RequestParam(value = "staleCheck", required = false, Boolean staleCheck) {

    5.2	PipelineDAO will now save the pipeline list and return the response.

    		pipelineDAO.bulkImport(savePipelineList);


*Usage:*
The bulk API is invoked using Gate. The invocation is:

 curl -X POST -H "Content-type: application/json" -d '[{
   "keepWaitingPipelines": false,
   "limitConcurrent": true,
   "application": "test_004",
   "spelEvaluator": "v4",
   "name": "pipe1001",
   "stages": [
     {
       "requisiteStageRefIds": [],
       "name": "Wait",
       "refId": "1",
       "type": "wait",
       "waitTime": 6
     }
   ],
   "index": 1000,
   "triggers": []
 },
 {
   "keepWaitingPipelines": false,
   "limitConcurrent": true,
   "application": "test_005",
   "spelEvaluator": "v4",
   "name": "pipe1002",
   "stages": [
     {
       "requisiteStageRefIds": [],
       "name": "Wait",
       "refId": "1",
       "type": "wait",
       "waitTime": 6
     }
   ],
   "index": 1001,
   "triggers": []
 }]' http://<gate ip>:8084/pipelines/bulksave

*Output:

{
  “Successful”: <count>,
  “Failed”: <cound>,
  “Failed_list”: [<array of failed pipelines - (application, pipelinename, etc) and the error message]
}*

*Assumptions:*
This solution works only for RDBMS database store.
Save pipelines run in the context of an application, irrespective of the content of the pipeline.
In the case of bulk save, we will use the application name as “bulk save”.
The implication is that the tasks for bulk save will not show up under tasks in their respective applications

*Sample json payload:*
An array of valid Spinnaker pipeline jsons.

Example:
[
 {
   "keepWaitingPipelines": false,
   "limitConcurrent": true,
   "application": "test_004",
   "spelEvaluator": "v4",
   "name": "pipe1001",
   "stages": [
     {
       "requisiteStageRefIds": [],
       "name": "Wait",
       "refId": "1",
       "type": "wait",
       "waitTime": 6
     }
   ],
   "index": 1000,
   "triggers": []
 },
 {
   "keepWaitingPipelines": false,
   "limitConcurrent": true,
   "application": "test_005",
   "spelEvaluator": "v4",
   "name": "pipe1002",
   "stages": [
     {
       "requisiteStageRefIds": [],
       "name": "Wait",
       "refId": "1",
       "type": "wait",
       "waitTime": 6
     }
   ],
   "index": 1001,
   "triggers": []
 },
 {
   "keepWaitingPipelines": false,
   "limitConcurrent": true,
   "application": "test_006",
   "spelEvaluator": "v4",
   "name": "pipe1003",
   "stages": [
     {
       "requisiteStageRefIds": [],
       "name": "Wait",
       "refId": "1",
       "type": "wait",
       "waitTime": 6
     }
   ],
   "index": 1002,
   "triggers": []
 }
]
