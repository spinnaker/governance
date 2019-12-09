# Select roles that can execute manual judgement stage

| | |
|-|-|
| **Status**     | _**Proposed**, Accepted, Implemented, Obsolete_ |
| **RFC #**      | https://github.com/spinnaker/deck/pull/7636
| **Author(s)**  | Sanjeev Thatiparthi (@sanopsmx) (https://github.com/sanopsmx)
| **SIG / WG**   | sig-ux

## Overview

Display all the roles the user have in the manual judgment stage.

### Goals and Non-Goals

Display authorized groups dropdown list only when the user selects the manual judgment stage.
Display all the groups the user has selected while creating application(permissions) in the authorized groups dropdown.
When the pipeline is executed, disable the manual judgment 'continue/cancel' based on the user role selected in the authorized groups.

opsmxemp1 : ["mail-qa","sra-dev","mail-dev","sra-qa","empdev"]

opsmxemp2 : ["sra-dev","mail-dev","empdev"]


Testcase No. # Application Name # Application Roles # Manual Judgment Roles # User # Result

1 # testmanualjud001 # [sra-dev,sra-qa] # sra-dev # [opsmxemp1] # Enable Button
					                              # [opsmxemp2] # Enable Button
2 # testmanualjud002 # N.A # N.A # opsmxemp1 # Enable Button
				                 # opsmxemp2 # Enable Button
3 # testmanualjud003 # [sra-qa,sra-dev] # N.A # opsmxemp1 # Enable Button
					                          # opsmxemp2 # Enable Button
4 # testmanualjud004 # [sra-dev,sra-qa,mail-dev,mail-qa,emp-dev,finance] # [sra-qa,mail-dev,finance] # opsmxemp1 # Enable Button
											  	                                                     # opsmxemp2 # Enable Button
5 # testmanualjud005 # [sra-dev,mail-qa,operations] # operations # opsmxemp1 # Disable Button
								                                 # opsmxemp2 # Disable Button


## Motivation and Rationale

Only legitimate users can approve the manual judgment stage so that it can be moved to downstream stages.

## Timeline

Already Implemented

## Design

1. Enhanced stage.html to
   
   Get the roles of the application.
   Display the roles of the application as a list only if the stage is Manual Judgment.
   
2. Enhanced stage.module.js to
   
   Fetch the permissions of the application from the gate application url.
   populate the list with only the roles of the application with no duplicates.
   
3.  Enhanced ApplicationReader.ts to
   
   Fetch the permissions of the application from the gate application url.
   
4.  Enhanced ManualJudgmentApproval.tsx to
   
   Fetch the roles of the application, stage and the user during the execution.
   Iterate through each of the user role to check if the role exists in the stage and application.
   If yes/no, enable/disable the continue button.
   Display the instruction that User does not have permissions to continue
