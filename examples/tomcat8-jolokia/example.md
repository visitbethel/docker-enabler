Tomcat8/Jolokia JMX-HTTP Bridge-enabled Docker container
=========================================================

Introduction
-------------
This is an example based on [Tomcat 8 with Jolokia JMX Bridge](https://registry.hub.docker.com/u/fabric8/tomcat-8.0/)
for `JMX` statistics collections

Highlights
----------
 - shows how to map Docker host ports and volumes to internal Docker container ports and volumes
 - automatic Http port increments across Silver Fabric engines for Tomcat8 and Jolokia ports
 - shows usage of Silver Fabric runtime context variables and variable substitutions
 - shows Tomcat8 performance stats collections via scripting to Jolokia client API
 - shows Tomcat8 logs checkpoint and log collections
 - Shows `ComponentNotificationFeature` by listening on component `J2EE Example` lifecycle via scripting
 - Shows minimal scripting for `ArchiveManagement` interface

Setup
------
- 1. Do a pull of Tomcat 8 with Jolokia JMX Bridge via docker command:

    ```bash
       docker pull fabric8/tomcat-8.0
    ```
- 2. Download the `SilverFabricCLI.tar.gz` library from the Broker and unzip into a directory.
- 3. Download and unzip `S_Tomcat8-ant-package.zip`  into the same directory as above
- 4. Run the Ant task `build.xml` against a Silver Fabric Broker with this `Silver Fabric Docker Enabler` installed

   ```ant
      ant -f build.xml
   ```
- 5. Run the stack `S_Tomcat8` via the Silver Fabric Adnin UI.

