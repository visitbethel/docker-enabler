Silver Fabric Docker Enabler FAQ
=================================

####FAQ1. Which `component type` should I use when configuring a component from this enabler?####

If you are not doing any of the following, use `Default` component type; otherwise use `J2EE` component type:

- Configure a component with one or more J2EE archive(.war, .ear, etc) to be deployed/started when the component is activated
- Invoking lifecycle methods : deploy/start/stop/undeploy J2EE archives using `Continuous Deployment` feature
- Running any scaling rules for `microscaling` of archives

***Warning***: If you configured a J2EE component type, you ***MUST*** implement scripting for the interface `ArchiveManagement` and possibly `ArchiveProvider` from the `Silver Fabric SDK API`.


