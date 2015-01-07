Silver Fabric Docker Enabler FAQ
=================================

####FAQ0. One or more examples doesn't seem to work...What happened?####

All examples have been tested with `ubuntu 14.04`. You may need to rebuild it on your OS based on its version. Sometimes breakage happens due to OS update or patches. Docker is also evolving very fast; so sometimes the `Docker Remote API` bindiing maybe broken. Sorry!.

####FAQ1. Which `component type` should I use when configuring a component from this enabler?####

If you are not doing any of the following, use `Default` component type; otherwise use `J2EE` component type:

- Configure a component with one or more J2EE archive(.war, .ear, etc) to be deployed/started when the component is activated
- Invoking lifecycle methods : deploy/start/stop/undeploy J2EE archives using `Continuous Deployment` feature
- Running any scaling rules for `microscaling` of archives

***Warning***: If you configured a J2EE component type, you ***MUST*** implement scripting for the interface `ArchiveManagement` and possibly `ArchiveProvider` from the `Silver Fabric SDK API`.

####FAQ2. Why is my J2EE platform "XYZ" stack not responding immediately after its Docker container is started?####

Dockerizing an applications with lengthy boot time into a container do not in general shorten the boot time. You just need to wait till its booted up. All Docker provides is a lightweight "VM" with good enough isolation in seconds to run your application.

####FAQ3. What is the best way to configure my dockerized application using `Silver Fabric`?####

Depending on how your application has been dockerized, you may use these 4 approaches when configuring a Silver Fabric `component` associated with the application's Docker container:

- Bake the configuration in during a `Dockerfile` build using a combination of content files and variables substitution replacements via a `configure.xml`. The Dockerfile `ADD` and `COPY` command are used in conjunction with the content files.

- Map an external `golden` configuration file/dir to an internal Docker file/dir after preprocessing it using variables substitution replacements via a `configure.xml`. Use `!VOL_MAP_` runtime context variables to accomplish the mapping.

- Pass in Docker container environment variables via `!ENV_VAR_` and `!ENV_FILE_` runtime context variables
- Use `variable providers` as stipulated in `Silver Fabric SDK API` as source of runtime context variables

***Note***: Out the box, Silver Fabric allows you to do the above 3 out of 4 approaches.

####FAQ4. When a Silver Fabric component restarts or crashes, what happens to the underlying Docker container it manages?####

The underlying Docker container if previously provisioned is removed completely. We do not want to leave a possibly corrupted container hanging around, since its much easier to spin a new one up quicker.

####FAQ5. What kind of runtime context variable information should I export for a Docker container when its started by a Silver Fabric component?####

Recommended are portable abstractions like configurational parameters, public ports, login infos, URLs, operational parameters, etc.
Not recommended are `host-to-container volume mappings`, hardware devices since you can't be sure that the all interacting Docker containers are constrained to run on the same `Docker host`.

####FAQ6. I don't want passwords or other sensitive information tokens to be to be seen when building a Dockerfile or passing those tokens via Docker environment variables through `!ENV_VAR_` or `!ENV_FILE_`. What can I do?####

There are 2 things you can do together:

- First, obfuscate/encrypt the passwords or any sensitive information tokens that is to be used in the Dockerfile or
passed as Docker container environment variables. You have to devise a means within the application's start script to obfuscate/decrypt the tokens passed into the container, obviously. This is the first layer of obfuscation.

- Secondly, use runtime context variables of type `encrypted` for the obfuscated/encrypted passwords or environment variables above when configuring them in Silver Fabric components to prevent leakage via Silver Fabric UI, REST API or Ant tasks. This added a second layer of obfuscation.

####FAQ7. What service discovery and configuration mechanism does Silver Fabric provides?####

You may use the following 2 mechanisms:

- Silver Fabric `Component Notification feature`

  This primarily uses Silver Fabric engine `ActivationInfo` as carrier for component(container) info which an engine can query from the broker upon receipt of a notification of one or more components of interest being activated/deactivated.

- Implement a custom `Dynamic Variable Provider` by proxing your favorite distributed key-value stores like `etcd`,`consul`, etc.

####FAQ8. How does this Enabler keeps track of any sidekick processes spawned under a `supervisord` process management?####

Docker monitors one process in each running container and the container lives or dies with that `primary process`. This Enabler monitors out of the box, the `primary process` only. So, if you are using `supervisord`, only the `supervisord` process is being monitored.  So its possible for the secondary process(es) of interest failing to start or crashed while the parent `primary process` still runs, giving a false illusion that the container is doing fine as a whole. The best solution is for the parent `supervisord` to attempt to start/restart a secondary process a set number of times before exiting to signal a failure. 

Note: The same may be said for processes spawned using the alternative process management `CFEngine`

####FAQ9. Is there an alternative to `supervisord`?####

Yes. You may use [S6 supervisor ](http://www.skarnet.org/software/s6/) in manner prescribed [here](http://blog.tutum.co/2014/12/02/docker-and-s6-my-new-favorite-process-supervisor/) to assign a key process whose lifecycle dictates the whole container.

####FAQ10. Why does my container taking a long time to shutdown?####

It is possible that the wrong kind of [base image](http://phusion.github.io/baseimage-docker/) may have been used with the Dockerfile build. As the result, some zombie process(es) may have lingered around that prevented a total shutdown such that Docker have to do a "force kill" eventually.

####FAQ11. `--link` and `--volumes-from` not support? Justify this decision####

First, both introduces local tight coupling between containers as well as an artificial `same-location` requirement.
Secondly, users should deploy applications of like trust and similar performance characteristics on a Docker host. For example, it would not be prudent from security viewpoint to deploy your mission-critical banking application right next to your customer-facing Web service.
Thirdly, an application stack composed from a set of Docker components would be more resilient if it can utilized containers running on different Docker host in the event of a host crash.

####FAQ12. What if this Enabler don't meet my needs?####

You can always extends it or write your own!. Base on the code and examples that comes with this Enabler, you should be able to write an enabler that meets your need.

