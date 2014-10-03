[fabrician.org](http://fabrician.org/)
==========================================================================
Silver Fabric Docker Enabler Guide
==========================================================================

Introduction
--------------------------------------
A Silver Fabric Docker Enabler allows an external application or application platform, such as a 
J2EE application server that has been dockerized to run in a TIBCO Silver Fabric software environment.
This enabler essentially provides integration between Silver Fabric and the dockerized. 
The Enabler automatically provisions, orchestrates, controls and manages a dockerized  application. 

Supported Platforms
--------------------------------------
* Silver Fabric 5.6.x or above
* Windows, Linux

Installation
--------------------------------------
The Silver fabric Docker Enabler consists of an Enabler Runtime Grid Library only.
The Enabler Runtime contains information specific to a Silver Fabric version that is 
used to integrate the Enabler.
Installation of the Silver Fabric Docker Enabler involves copying the Grid 
Library to the SF_HOME/webapps/livecluster/deploy/resources/gridlib directory on the Silver Fabric Broker. 

Additionally, this Enabler requires the Silver Fabric Engine use Java 1.7. 

Runtime Grid Library
--------------------------------------
The Enabler Runtime Grid Library is created by building the maven project. The build depends on the SilverFabricSDK jar file that is distributed with TIBCO Silver Fabric. 
The SilverFabricSDK.jar file needs to be referenced in the maven pom.xml or it can be placed in the project root directory.

```bash
mvn package
```
The version of the distribution is defaulted to 1.0.0  However, it can be optionally overridden:
```bash
mvn package -Ddistribution.version=1.0.1
```


Overview
--------------------------------------
xxxx

Statistics
--------------------------------------
xxxx

Runtime Context Variables
--------------------------------------

                
###JDK and JVM-related runtimes-related:###
         
* **JDK_NAME** -  The name of the required JDK
    * Default value: j2sdk 
    * Type: String
	
* **JDK_VERSION** - The version of the required JDK
    * Default value: 1.7 
    * Type: String
	
* **JAVA_HOME** -  The Java home directory
    * Default value: ${GRIDLIB_JAVA_HOME} 
    * Type: Environment
	


        
###Others :###
         
* **BIND_ON_ALL_LOCAL_ADDRESSES** -  Specify if all network interfaces should be bounded for the SSHd, JMX server, HTTP(s) connectors
    * Default value: true 
    * Type: String 
            
* **LISTEN_ADDRESS_NET_MASK** -A comma delimited list of net masks in CIDR notation.  The first IP address found that matches one of the net masks is used as the listen address.  Note that BIND_ON_ALL_LOCAL_ADDRESSES overrides this setting.  
    * Default value:
    * Type: Environment 
           
* **DELETE_RUNTIME_DIR_ON_SHUTDOWN** -  Whether to delete the Karaf runtime directory on shutdown. Note: Set to false if you need to review if any configuration is done correctly with runtime context variables.
    * Default value: true 
    * Type: Environment 
            

            