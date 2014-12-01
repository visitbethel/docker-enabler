NGINX Docker container
=========================================================

Introduction
-------------
This is an example based on [Nginx](https://registry.hub.docker.com/_/nginx/), an open source reverse proxy server for HTTP, HTTPS, SMTP, POP3, and IMAP protocols, as well as a load balancer, HTTP cache, and a web server (origin server)

Highlights
----------
 - shows how to map Docker host ports to internal Docker container ports
 - automatic port increments across Silver Fabric engines for MongoDb listening port
 - shows how to add content files to Dockeffile build context directory and use them with `ADD`(or `COPY`) command
 - shows usage of [Silver Fabric runtime context variables and variable substitutions](https://github.com/fabrician/docker-enabler/blob/master/examples/images/example_nginx_rcv.gif) for
   - [Dockerfile](https://github.com/fabrician/docker-enabler/blob/master/examples/Nginx/example_nginx_dockerfile.gif)
   - [Content file directory](https://github.com/fabrician/docker-enabler/blob/master/examples/Nginx/example_nginx_contentfile.gif)

Setup
------

- (1) Download the `SilverFabricCLI.tar.gz` library from the Broker and unzip into a directory.
- (2) Download and unzip `MyNginx-ant-package.zip`  into the same directory as above
- (3) Run the Ant task `build.xml` against a Silver Fabric Broker with this `Silver Fabric Docker Enabler` installed

   ```ant
      ant -f build.xml
   ```
- (4) Run the stack `MyNginx` via the Silver Fabric Admin UI.
      
      This will do a Dockerfile build first and then launch 1 instance of Nginx Docker container.

- (5) Access the sample `simple.html` page instance via the browser:

      ```
      http://<docker_host>:<docker port>/simple.html
      ```
     
