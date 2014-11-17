Silver Fabric Docker Enabler FAQ
=================================

####FAQ1. Which `component type` should I use when configuring a component from this enabler?####

If you are not doing any of the following, use `Default` component type; otherwise use `J2EE` component type:

- configure a component with one or more J2EE archive(.war, .ear, etc)
- deploy/start/top/undeploy J2EE archives using `Continuous Deployment` feature

***Warning***: If you configured a J2EE component type, you ***MUST*** implement scripting for the interface `ArchiveManagement` and possibly `ArchiveProvider` from the `Silver Fabric SDK API`.


