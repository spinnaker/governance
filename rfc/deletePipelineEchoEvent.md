# Delete pipeline echo event implementation

| | |
|-|-|
| **Status**     | _**Proposed**, Accepted, Implemented, Obsolete_ |
| **RFC #**      | https://github.com/spinnaker/governance/pull/195 |
| **Author(s)**  | Pranav Bhaskaran (@Pranav-b-7) (https://github.com/Pranav-b-7)
| **SIG / WG**   | sig-security
| **Obsoletes**  | _<RFC-#s>, if any, or remove header_ |

## Overview

This is a bug fix. The echo events are not generated when a user deletes a pipeline from spinnaker. 

As part of this PR fixes are made to receive echo events when a pipeline is deleted from spinnaker.

### Goals and Non-Goals

Spinnaker is generating echo events for the operations createApplication, updateApplication, deleteApplication, savePipeline, updatePipeline
and pipeline execution. The only operation that was missed out was deletePipeline. 

## Motivation and Rationale

This would be useful for the users who have configured the webhooks in spinnaker to listen to the events. 
The configured webhook URLs will now receive deletePipeline event.

## Timeline

Implementation is done.

## Design

**Existing design:**

- When a delete pipeline request is received in GATE service. GATE authenticates the request and routes it to the front50 service.
- This will only delete the pipeline from the database but, events will not be generated.

**New design:**

==***GATE service changes:***==

- When a delete pipeline request is received in GATE service. GATE authenticates the request and form a task object.
- This task object is passed as a parameter to /ops API in ORCA service.
- Once submitted GATE service will keep polling the ORCA service to track the task status.
- This is exactly the same process followed for other operations like, savePipeline, updatePipeline etc.

*==**ORCA service changes:***==

- A new task type "deletePipeline" is created and registered in ORCA.
- ORCA service already have the support to process , monitor the task and generate events by routing it to Echo service.
- Only changes needed was to register the new task "deletePipeline"
- Once the task is registered , ORCA service takes up the responsibility of generating events by routing it to Echo service.
- The request will also be routed to Front50 service to actually delete the pipeline.


**Rest API:**

==***GATE service:***==

- enpoint : /pipelines/{application}/{pipelineName:.+} - DELETE API

==***ORCA service:***==

- endpoint : /ops - POST API

sample request payload: 

{
	"description": "Delete pipeline pc",
	"application": "demopc",
	"job": [{
		"type": "deletePipeline",
		"pipeline": {
			"keepWaitingPipelines": false,
			"limitConcurrent": true,
			"application": "demopc",
			"spelEvaluator": "v4",
			"lastModifiedBy": "user2",
			"name": "pc",
			"stages": [{
				"name": "Wait",
				"refId": "1",
				"requisiteStageRefIds": [],
				"type": "wait",
				"waitTime": 5.0
			}, {
				"name": "testgate",
				"parameters": {
					"connectors": [{
						"connectorType": "PRISMACLOUD",
						"helpText": "PrismaCloud",
						"isMultiSupported": false,
						"label": "PrismaCloud",
						"supportedParams": [{
							"helpText": "ImageID",
							"label": "ImageID",
							"name": "imageId"
						}],
						"values": [{
							"imageId": "sha256:390cc7609d3bd3ab1fe8620fbf28d3e9c912c69a901f99e93af7d3b081ff6de2,sha25,sha256:ddbd686f8b5e3d3744443ebbdb0d91284da61f30f78604eabf193e065870f0726:6fed5fb61064c25e91e8afad7e199f20fd1422893bac05fdd2cae274790319fb"
						}]
					}],
					"gateUrl": "https://example.com/visibilityservice/v5/approvalGates/203/trigger",
					"imageIds": "nginx:1.14.2"
				},
				"refId": "2",
				"requisiteStageRefIds": ["1"],
				"type": "approval"
			}, {
				"account": "kubeacc",
				"cloudProvider": "kubernetes",
				"manifests": [{
					"apiVersion": "apps/v1",
					"kind": "Deployment",
					"metadata": {
						"labels": {
							"app": "nginx"
						},
						"name": "nginx-deployment"
					},
					"spec": {
						"replicas": 3.0,
						"selector": {
							"matchLabels": {
								"app": "nginx"
							}
						},
						"template": {
							"metadata": {
								"labels": {
									"app": "nginx"
								}
							},
							"spec": {
								"containers": [{
									"image": "nginx:1.14.2",
									"name": "nginx",
									"ports": [{
										"containerPort": 80.0
									}]
								}]
							}
						}
					}
				}],
				"moniker": {
					"app": "demopc"
				},
				"name": "Deploy (Manifest)",
				"namespaceOverride": "oes-agent",
				"refId": "3",
				"requisiteStageRefIds": ["2"],
				"skipExpressionEvaluation": false,
				"source": "text",
				"trafficManagement": {
					"enabled": false,
					"options": {
						"enableTraffic": false
					}
				},
				"type": "deployManifest"
			}],
			"index": 0.0,
			"id": "4a5bd769-1887-42ed-a1e1-277417a8e366",
			"triggers": [],
			"updateTs": "1628769454000"
		},
		"user": "user2"
	}]
}

