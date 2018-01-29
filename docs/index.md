---
nav-sort: 1
---
# Symphony REST Tools
This project provides a set of low level tools and diagnostics for use with the Symphony public REST API.  Although the symphony-rest-tools projects are implemented primarily in Java, they are intended to support development of API clients in any language, and to be a useful tool in understanding the interaction of higher level abstractions and language specific bindings with the underlying REST endpoints. 

This project therefore attempts to expose all of the details and features of the REST API endpoints, some of which might be purposefully hidden or abstracted away by other language specific API bindings. It is possible to call any API endpoint using any HTTP client, and this project attempts to provide easy ways to make ad hoc calls to ny API endpoint. This means that it is possible to implement client applications directly using the tools provided by this project, and for some small use cases this may be convenient and appropriate.

Notwithstanding the above, this project is intended to be a development and diagnostic kit bag rather than an application development framework, and it is specifically not intended to supersede or replace the Symphony Java Client or any other language bindings for application development against the public API.

This project uses JCurl as the HTTP client and leverages the JSON parsing capabilities
of that project. This project provides a binary distribution with convenience scripts to 
allow of the ad hoc exercising of various API endpoints from the command line.

It also provides an Eclipse based UI which allows capabilities of this project to be exercised in a UI context.

