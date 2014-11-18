MongoDB Docker container
=========================================================

Introduction
-------------
This is an example based on [MongoDb](https://registry.hub.docker.com/u/tutum/mongodb/),an open-source document database, and the leading NoSQL database.

Highlights
----------
 - shows how to map Docker host ports to internal Docker container ports
 - automatic port increments across Silver Fabric engines for MongoDb listening port
 - shows how to set environment variables into Docker container
 - shows usage of [Silver Fabric runtime context variables and variable substitutions](https://github.com/fabrician/docker-enabler/blob/master/examples/images/example_mongodb_rcv.gif)

Setup
------
- (1) Do a pull of MongoDb via docker command:

    ```bash
       docker pull tutum/mongodb
    ```
- (2) Download the `SilverFabricCLI.tar.gz` library from the Broker and unzip into a directory.
- (3) Download and unzip `MyMongoDB-ant-package.zip`  into the same directory as above
- (4) Run the Ant task `build.xml` against a Silver Fabric Broker with this `Silver Fabric Docker Enabler` installed

   ```ant
      ant -f build.xml
   ```
- (5) Run the stack `MyMongoDb` via the Silver Fabric Admin UI.
      
      This will launch 1 instance of MongoDb Docker container.

- (6) Access the MongoDb instance: 

      ```
      curl --user admin:genghis --digest http://<docker_host>:28017/
      ```
      where <docker_host> is the host where the MongoDb conatainer is running.
