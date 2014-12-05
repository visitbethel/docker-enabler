Silver Fabric stack composed from 2 Docker components
=========================================================

Introduction
-------------
This stack is composed from 2 Silver Fabric components associated with a [RockMongo](https://registry.hub.docker.com/u/gilacode/rockmongo/) and [MongoDb](https://registry.hub.docker.com/u/tutum/mongodb/) Docker containers.
`RockMongo` is an OSS Web Administrative GUI for `MongoDB`. In the stack, we create a `component dependency` where the `RockMongo` component depends on the 'MongoDb` component.


Highlights
----------
 - shows how to map Docker host ports to internal Docker container ports
 - automatic port increments across Silver Fabric engines for both MongoDb and RockMongo listening ports
 - shows how to set environment variables into Docker containers
 - shows usage of [Silver Fabric runtime context variables substitutions and exporting](https://github.com/fabrician/docker-enabler/blob/master/examples/images/example_mongodb_rcv.gif)
 - shows how [exported variables](https://github.com/fabrician/docker-enabler/blob/master/examples/images/example_rockmongo_rcv.gif) from MongoDb component are being dynamically used to "link" to the RockMongo web container
Setup
------
- (1) Do a pull of MongoDb and RockMongo via docker command:

    ```bash
       docker pull tutum/mongodb
       docker pull gilacode/rockmongo
    ```
- (2) Download the `SilverFabricCLI.tar.gz` library from the Broker and unzip into a directory.
- (3) Download and unzip `MyMongoDBStack2-ant-package.zip`  into the same directory as above
- (4) Run the Ant task `build.xml` against a Silver Fabric Broker with this `Silver Fabric Docker Enabler` installed

   ```ant
      ant -f build.xml
   ```
   This reconstructs a stack `MyMongoDBStack2` and 2 components `Tutum_MongoDb` and `RockMongo` its composed from.
   
- (5) Run the stack `MyMongoDBStack2` via the Silver Fabric Admin UI.
      
      This will launch 1 instance of `MongoDb` Docker container first, followed by 1 instance of `RockMongo` container.

- (6) Manage the `MongoDb` via the `RockMongo` web app: 
- 
      You access `RockMongo` via web browser:
      
      ```
      http://<broker_host>:8080/rockmongo
      
      ```
      which will redirect to 
      
      ```
      http://<docker_host>:<docker_port>/
      
      ```
      
      where `docker_host` is the host where the `RockMongo` Docker container is running
            `docker_port` is the public port mapped for the `RockMongo` Docker container
            
Note: The user name and login password for this `MongoDB` example container is `admin` and `genghis` respectively;
These are dynamically exported and made available to the depending component `RockMongo`.
