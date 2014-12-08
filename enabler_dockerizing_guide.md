Silver Fabric Docker Enabler dockerizing guide
===============================================

This guide is written to help you through the main steps and decisions to "dockerize" your existing applications to use `TIBCO Silver Fabric` in conjunction with this Enabler to launch and orchestrate your Docker containers.

1. Dockerizability Factor
--------------------------

Docker is not a Silver bullet to all types of applications; in particular existing `monolithic` applications.
You should considered the issue of degree or level of [dockerizability](https://medium.com/@behruz/dockerizability-is-better-than-dockerized-3c08b9dbd84c) of your application. Any major application refactoring and decomposition into "dockerizable parts" needed?. Is the effort hard and worth it?

Though a Docker container can start up in secs, in contrast the `dockerized` application process encapsulated within it may take much longer time to to reach its "service available" state. Any application process that takes more than 30 secs to start/shutdown to/from "service available" state is unlikely to be a suitable Docker container candidate.

2. P.E.V Factor
--------------------
For each dockerized application process into a container, consider and evaluate :

- `Ports` : what ports are to be exposed for inter-container interaction and for consuming clients of the application stack.
- `Environment variables` : what environment variables are used to customize the container when launched?
In particular, a container's launch script needs to read its environment variables dynamically and use suitable default values if one or more variables are absent or not specified.

- `Volume mappings` : what volumes needs to be mapped for logs, common data, etc.

3. Container dependencies
--------------------------

Containers dependencies are translated into Silver Fabric `component dependencies`. You need to consider and evaluate:

- `Activation sequence` : what order the containers should be started?
- `Runtime context variable exports` : which variables to export to depending containers?


4. Statistics collections
--------------------------

- Application-level statistics
- Container-level statistics

5. Logs collections
--------------------

- Log to volume


