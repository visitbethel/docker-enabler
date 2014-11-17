Silver Fabric Docker Enabler Examples
======================================
All examples are based on Docker containers running on `Ubuntu 14.04 LTS`.

Prerequisites
-------------
Checklist:

- You need to pull down the desired images from `Docker Hub` to the `Docker host` where the `Silver Fabric Engine Daemon` and Docker are installed and properly configured or you may use the `Puppet and Chef support` feature. 

The latter assumes that you have installed and setup Puppet/Chef on the same `Docker host` to be able to run a Puppet manifest or Chef recipie.

- You need to download the `SilverFabricCLI.tar.gz` from the Silver Fabric Broker to a client machine(Windows or Linux) and extract it to a directory.

- You need to download and install  `Apache Ant version 1.8.4+` on the same client machine above.
  See installation guide in the Silver Fabric documentation.

- Run the Ant packages for the examples against the Silver Fabric Broker  to recreate Silver Fabric stacks and its components.
