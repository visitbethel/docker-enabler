/*
 * Copyright (c) 2014 TIBCO Software Inc. All Rights Reserved.
 *
 * Use is subject to the terms of the TIBCO license terms accompanying the download of this code.
 * In most instances, the license terms are contained in a file named license.txt.
 */
package org.fabrician.enabler;

import java.util.logging.Logger;

import com.datasynapse.fabric.common.RuntimeContext;
import com.datasynapse.fabric.common.StartCondition;
import com.datasynapse.fabric.container.Container;
import com.datasynapse.fabric.container.ProcessWrapper;
import com.datasynapse.fabric.domain.Domain;

public class DockerStartCondition implements StartCondition {
    private DockerContainer container;
    private Domain component;
    private RuntimeContext ctx;
    private static final long DEFAULT_POLL_PERIOD = 5000; // 5secs
    private long pollPeriod = DEFAULT_POLL_PERIOD;
    private DockerClient dockerClient;
    private String dockerContainerId;
    private String dockerContainerName;

    public DockerStartCondition() {}
    public void init(Container c, Domain d, ProcessWrapper process, RuntimeContext ctx) {
        this.container = (DockerContainer) c;
        this.dockerClient=container.dockerClient();
        this.dockerContainerId=container.dockerContainerId();
        this.dockerContainerName=container.dockerContainerName();
        this.component = d;
        this.ctx = ctx;
    }

    private Logger logger(){
        return this.container.logger();
    }
    public long getPollPeriod() {
        return this.pollPeriod;
    }

    public void setPollPeriod(long pollPeriod) {
        this.pollPeriod=pollPeriod;
        
    }

    public boolean hasStarted() throws Exception {
        logger().fine("Checking if docker container [" + dockerContainerName + "] has started...");
        return dockerClient.searchContainer(dockerContainerName,true).isPresent();
    }

}
