# Proposal: Spinnaker AWS Special Interest Group

## Roles & Responsibilities of the SIG Leads 

### SIG Lead(s):

**[Clare Liguori](https://github.com/clareliguori)** \
Principal Software Engineer \
_AWS_

**[Jeyrs Chabu](https://github.com/jeyrschabu)** \
Senior Software Engineer \
_Netflix_

**[Isaac Mosquera](https://github.com/imosquera)** \
Software Engineer  \
_Armory_


### SIG Lead Responsibility

1. Coordinate communication.
2. Set meeting dates and objectives for the coming year.
3. Curate agenda for each meeting.
4. Raise awareness and invite appropriate participants.
5. Encourage and foster an inclusive environment.
6. Facilitate the identification and development of resources (people or funding) for support of AWS within Spinnaker.


### Meeting Cadence
 - 1 hour, monthly, on **[Google Hangouts](https://meet.google.com/yfp-ekod-sxj)**
 - Third Wednesday of every month at 3 PM US/Pacific ([See in your time zone](https://www.thetimezoneconverter.com/?t=3pm&tz=San%20Francisco))
 - [AWS SIG Notes](https://docs.google.com/document/d/1TB7dSQDTM9jFBsttuevxOsP6MGQ3-z0wrYMIGxYxhYQ/edit)

# Responsibilities

## Description 

AWS SIG is responsible for the creation and maintenance of subprojects & subservices necessary to integrate AWS services for the operation and management of Spinnaker on AWS as well as using AWS as a deployment target. 

AWS SIG also acts as a forum for discussions and feature demos for Spinnaker users on AWS users/developers to raise their feature requests and support issues. SIG leads in collaboration with SIG members will develop, maintain and prioritize a backlog of features.  This prioritization will be published as the AWS SIG Roadmap.


## Scope of Work

Spinnaker integrations specific to AWS including:

*   Integrations, interfaces, libraries and extension points for all low-level AWS modules such as IAM, storage, networking, load balancers, registry, security, monitoring/logging at the instance or container level
*   Development of Spinnaker APIs to provide software delivery best practices for AWS compute services which might include, but are not limited to, the following:
    *   EC2
    *   ECS / Fargate
    *   Lambda
    *   Elastic Beanstalk
    *   CloudFormation
*   Integrations for AWS services into the release pipeline
    *   CodeCommit for source
    *   CodeBuild for build / bake
    *   ECR for Docker registry
    *   CloudWatch for canary analysis (Kayenta)
*   Support users on their issues and feature requests
*   Documentation for all things AWS for Spinnaker
*   Consult with other SIGs and the community on how to apply mechanisms owned by SIG AWS. Examples include:
    *   Review escalation implications of feature and API designs as it relates to core Spinnaker components (orca, clouddriver, echo, deck, etc)
    *   AWS Clouddriver implementation and interface design
    *   Implementing and improving core functionality (such as tests and documentation) to clarify and validate the contract between the core and and the AWS specific functionality


