How Tos
-------
 ***(1) How do I map a public host port(ex.9090) to an internal Docker container port(ex.8080)?***
```

  First, create an auto-increment "Numeric" runtime context variable of type "String" for host port like  so:

  "MY_PORT=9090"
  
  Next, create a port mapping runtime context variable with name prefixed by "!PORT_MAP_" like so:

  "!PORT_MAP_my_http=${MY_PORT}:8080"
  
  Note: The internal port 8080 will be publically exposed as port 9090,9091, 9092,...depending on the engine     instances the Docker container is managed from.
```

 ***(2) How do I mount a host directory(ex. "/logs") to an internal Docker container directory(ex. "/my_logs")?***

```
  
  First, make sure the host directory is created first or exists first.
  Next, create a runtime context variable of type "String" with name prefixed by "!VOL_MAP_" like so:
  
  "!VOL_MAP_logs=/logs:/my_logs:rw"
  where 'rw' can be replaced by 'ro'
  
```

 ***(3) How do I add one or more environmental variables to the Docker container?***

```
  There are 2 ways that you can do that:
  
  (a) Create a runtime context variable of type "String" with a name prefixed by "!ENV_VAR_"
  
  Example : "!ENV_VAR_myvar1=Hello"

  
  (b) Create a runtime context variable of type "String" with a name prefixed by "!ENV_FILE_"
  
  Example: "!ENV_FILE_env1=/env_config/env1.properties" where "env1.properties" holds each line of environment variable in key-value pair format.
  
  Note: There is a already a preconfigured "!ENV_FILE_default" environment files that you can used. Use this
  unless there is a need to add another environment file
  

```

  ***(4) How do I build and run a Docker file?***

```
  Upload your Dockerfile as a content file using relative content   "docker/docker_context" directory as the     target directory for the Dockerfile.
  Then set runtime context variable "BUILD_ALWAYS=true" and "DOCKER_IMAGE_NAME=<namespace>/<app name>:[tag]"
  
  Example: "DOCKER_IMAGE_NAME=bbrets/tomcat:6.0.36"

Note: If you are using `COPY` or `ADD` commands in the Dockerfile, you can add the files to be copied/added in those commands as content files in the same relative content dir above.
```

  ***(5) How do I collect JMX stats from a J2EE Docker container?***
  
  Basically, you have 2 main choices:
  - work with [JMX] directly
    This is harder since you may need take into account `RMI` idiosyncrasies and scripting in raw JMX API.
  - work with a [JMX-HTTP Bridge]
    This is easier and this enabler bundles `Jolokia JMX-HTTP Bridge version 1.2.2` Java client which allows you to  script in an easier API, especially if you need to deal with complex JMX data types.
  
  Assume that you have a [Jolokia-enabled J2EE image](https://registry.hub.docker.com/u/fabric8/tomcat-8.0/),you need to supply a statistic script in Jython, JRuby or ECMAScript and implement a `getStatistic` method and runtime context variable `JOLOKIA_URL` that binds to the [Tomcat8-Jolokia] url:
```python
from org.jolokia.client import J4pClient;
from org.jolokia.client.request import J4pReadRequest;
from org.jolokia.client.request import J4pReadResponse;
from javax.management import ObjectName;

client = None;

def getRcv(varName):
    rcv = runtimeContext.getVariable(varName)
    if rcv == None:
        return None;
    else:
        return rcv.getValue();
    
def getClient():
    global client
    if client == None :
        url=getRcv("JOLOKIA_URL");
        client =J4pClient(url);
    return client;
        
    
    
def usedMemory():
    obj_name= ObjectName("java.lang:type=Memory");
    request = J4pReadRequest(obj_name,["HeapMemoryUsage"]);
    request.setPath("used");
    response = getClient().execute(request);
    return str(response.get(0).getValue());

def peakThreadCount():
    obj_name=ObjectName("java.lang:type=Threading");
    request = J4pReadRequest(obj_name,["PeakThreadCount"]);
    response = getClient().execute(request);
    return str(response.get(0).getValue());

def uptime():
    obj_name = ObjectName("java.lang:type=Runtime");
    request = J4pReadRequest(obj_name,["Uptime"]);
    response = getClient().execute(request);
    return str(response.get(0).getValue());
    
def getStatistic(statName):
    statValue = 0.0;
    if statName == "USED_MEMORY" :
        statValue=usedMemory();
    elif statName == "PEAK_THREAD_COUNT":
        statValue = peakThreadCount();
    elif statName == "UPTIME":
        statValue = uptime();
    else:
        logger.warning('Unknown statistic: ' + statName);
        
    return statValue;
  
```
  ***(6) How do I script a component notification when one or more dependent components is down?***
  
  First, you need to register one or more dependent components from the view point of the listening component, using `ComponentNotificationFeature`. Then add a script implementing the `componentNotification` method like so:
  ```python
from com.datasynapse.fabric.admin.info import NotificationEngineInfo;
from com.datasynapse.fabric.admin import AdminManager;

def componentNotification(componentName, notificationEngineInfoList):
    logger.info("Notification from component '" + componentName + "'.");
    logger.info("Component engines located at :" + notificationEngineInfoList.toString());
    engineAdmin=AdminManager.getEngineAdmin();
    n= NotificationEngineInfo();
    for n in notificationEngineInfoList:
        engine_id=n.getEngineId();
        instance=n.getInstance();
        info=engineAdmin.getEngineInfo(engine_id,instance);
        alloc=info.getAllocationInfo();
        props=alloc.getProperties();
        logger.info("--------------properties are -----------------------");
        for p in props:
           logger.info(p.getName() + "=" + p.getValue())
        logger.info("-----------------------------------------------------");
        
  ```
Note: You may use the class `AdminManager` to query for any additional information from the Silver Fabric broker.

[JMX]:http://ptmccarthy.github.io/2014/07/24/remote-jmx-with-docker/
[JMX-HTTP Bridge]:http://www.jolokia.org/
[Tomcat8-Jolokia]:https://github.com/fabrician/docker-enabler/blob/master/examples/images/example_tomcat8_jolokia_rcv.gif
