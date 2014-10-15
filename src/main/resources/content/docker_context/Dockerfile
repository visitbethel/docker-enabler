# This is just a sample Dockerfile that user should replaced with their own for building their docker image
# that they want to run with this Sivler Fabric Docker Enabler
# Source : https://docs.docker.com/examples/running_ssh_service/
# See above URL for usage
#
# sshd
#
# VERSION               0.0.1

FROM     ubuntu:14.04
MAINTAINER Thatcher R. Peskens "thatcher@dotcloud.com"

RUN apt-get update && apt-get install -y openssh-server
RUN mkdir /var/run/sshd
RUN echo 'root:screencast' | chpasswd
RUN sed -i 's/PermitRootLogin without-password/PermitRootLogin yes/' /etc/ssh/sshd_config

EXPOSE 22
CMD ["/usr/sbin/sshd", "-D"]