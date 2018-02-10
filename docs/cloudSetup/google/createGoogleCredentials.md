---
nav-sort: 110
nav-level: 1
---
# Creating Google Credentials
Before deploying a service to Google it is necessary to create a Service Account and make the credentials for this account available to the Kubernetes cluster on which the application will be deployed. For real applications you should create service accounts with the minimum necessary privileges, and in general topics and subscriptions should be created at install time to avoid initialization race conditions.

The Google Pub/Sub example application attempts to create a topic and subscription if it does not already exist, so to run the example we need to create a service account with 

Instructions can be found at 
[Authenticating to Cloud Platform with Service Accounts](https://cloud.google.com/kubernetes-engine/docs/tutorials/authenticating-to-cloud-platform)
 
