## Manual Judgement Navigation Enhancement

| **Status**        | **Proposed**, Accepted, Implemented, Obsolete           |
| ------------- |-------------|
|  **RFC #**      |  | 
| **Author(s)**      | 	[Rumit Rout (@rumit-opsmx)](https://github.com/rumit-opsmx), [Rajinder Siddhu (@siddhu-opsmx)](https://github.com/siddhu-opsmx)      | 
| **SIG / WG** | sig-ui     | 

## Overview

Spinnaker allows us to apply filters on pipelines execution page to help users view all the pipelines already executed or which are in execution. We have multiple parameters available by default to perform filter operation on pipeline executions i.e, Filter by status, Filter by pipeline Name etc.

In case of multiple pipelines, users may need other filters, aka custom filters to populate pipelines filtered based on some metadata. 

## Problem Definition:

1. If there are many pipelines it may be difficult for the user to serially check all the pipelines that belong to specific groups. We need an easy way to organize, search and filter pipelines across the parameters provided by the user as meta data. 

## Approach



**1.1 Introduced a new property in pipeline config JSON containing the falcon parameters.**
![Configurable Filters](https://drive.google.com/uc?export=view&id=https://drive.google.com/file/d/1lxsc60kum10M7GCK1lzUDN0mU8q5iZmg)
Fig: 1.1  Add metadata to pipeline JSON.

**1.2. Add Parameters in Setting.js file.**
![Configurable Filters View](https://drive.google.com/uc?export=view&id=https://drive.google.com/file/d/1fXssba_eoawz234CBmIKrLOvvC7xFX1W)  //image of setting.js file
The filterRelations are key value pairs, where key represents a key from the filterOn section and value represents a key from the filterOn section that is the parent of the key in the filterRelations section. If a key from the filterOn section does not exist in the filterRelations section then it means that the key does not have any parent relation to another key. For Ex:- In above filterRelation  Falcon Instance does not exist in the filterRelation as a key because it don't have any parent relationship to other keys but other keys like Falcon Domain has a parent relationship with Falcon Instance or Cell has a parent relationship with Falcon Domain. 


**1.3. Introduced Filter block for configurable filters.**
![Configurable Filters Uncheck](https://drive.google.com/uc?export=view&id=1d0qQf4YglzLBrAiy5UYe1-L4RuKFvFDi)
The filters are populated based on the parameters received in the filterOn from the JSON.
If none of the filters are selected then it shows all the executions.


***

**2.1 Filter View.**
![Configurable Filters Uncheck](https://drive.google.com/uc?export=view&id=10Ijb5urbTbtVHdctZx-_IWQ4tWhKhv8G)
    2.1.1 Selecting any falcon filter will filter out the executions.
    2.1.2 It will also add a filter tag in the filter by section just above the executions.
    2.1.3 Selecting any falcon filter will change the options of the filters placed below it based on the current selection.










