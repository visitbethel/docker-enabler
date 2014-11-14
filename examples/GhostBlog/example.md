Ghost Node.js Docker container
=========================================================

Introduction
-------------
This is an example based on [Ghost](https://registry.hub.docker.com/u/dockerfile/ghost/), a Node.js blogging
platform.

Highlights
----------
 - shows how to map Docker host ports and volumes to internal Docker container ports and volumes
 - automatic Http port increments across Silver Fabric engines for Ghost port
 - shows usage of Silver Fabric runtime context variables and variable substitutions

Setup
------
- (1) Do a pull of Ghost via docker command:

    ```bash
       docker pull dockerfile/ghost
    ```
- (2) Download the `SilverFabricCLI.tar.gz` library from the Broker and unzip into a directory.
- (3) Download and unzip `MyGhost-ant-package.zip`  into the same directory as above
- (4) Run the Ant task `build.xml` against a Silver Fabric Broker with this `Silver Fabric Docker Enabler` installed

   ```ant
      ant -f build.xml
   ```
- (5) Run the stack `MyGhost` via the Silver Fabric Admin UI.

- (6) Access the Ghost blog URL : 

      ```
      http://broker-host:8080/ghost  for blog , or
      http://broker-host:8080/ghost/ghost for admin
      
      ```
      Note: This should redirect you to the real endpoint where the container is running.
