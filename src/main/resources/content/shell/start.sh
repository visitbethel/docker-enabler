#!/bin/bash
# Copyright (c) 2014 TIBCO Software Inc. 
# All Rights Reserved. 
#
# Redistribution and use in source and binary forms, with or without
# modification, are permitted provided that the following conditions are met:
# 1. Redistributions of source code must retain the above copyright 
#    notice, this list of conditions and the following disclaimer.
# 2. Redistributions in binary form must reproduce the above copyright 
#    notice, this list of conditions and the following disclaimer in the 
#    documentation and/or other materials provided with the distribution.
# 3. Neither the name of TIBCO Software Inc.  nor the 
#    names of any contributors may  be used to endorse or promote products 
#    derived from this software without specific prior written permission. 
#
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT OWNER AND CONTRIBUTORS "AS IS" AND ANY 
# EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED 
# WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE 
# DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY 
# DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES 
# (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; 
# LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND 
# ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT 
# (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS 
# SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
env
echo "Running $DOCKER_CONTAINER_NAME"
start_cmd="docker run $DOCKER_ENVS $DOCKER_PORTS_EXPOSED $DOCKER_PORT_MAPPINGS $DOCKER_VOL_MAPPINGS $DOCKER_AUXILIARY_OPTIONS $DOCKER_SECURITY_OPTS --name $DOCKER_CONTAINER_TAG $DOCKER_IMAGE_NAME $CMD_OVERRIDE"
cmd=
if [ "$USE_SUDO" == "true" ]; then
  cmd="sudo $start_cmd"
else
  cmd="$start_cmd"
fi
echo "--------------------------------------------------------------"
echo "$cmd"
echo "--------------------------------------------------------------"
echo $($cmd)
