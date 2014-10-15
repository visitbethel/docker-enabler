/*
 * Copyright (c) 2014 TIBCO Software Inc. All Rights Reserved.
 *
 * Use is subject to the terms of the TIBCO license terms accompanying the download of this code.
 * In most instances, the license terms are contained in a file named license.txt.
 */
package org.fabrician.enabler;

import java.util.logging.Logger;

import com.datasynapse.fabric.common.RunningCondition;
import com.datasynapse.fabric.common.RuntimeContext;
import com.datasynapse.fabric.container.Container;
import com.datasynapse.fabric.container.ProcessWrapper;
import com.datasynapse.fabric.domain.Domain;

public class DockerRunningCondition implements RunningCondition {
    private DockerContainer container;
    private Domain component;
    private RuntimeContext ctx;
    private static final long DEFAULT_POLL_PERIOD = 5000; // 5secs
    private long pollPeriod = DEFAULT_POLL_PERIOD;
    private DockerClient dockerClient;
    private String dockerContainerId;
    private String dockerContainerName;
    public DockerRunningCondition(){}
    private Logger logger(){
        return this.container.logger();
    }
    @Override
    public void init(Container c, Domain d, ProcessWrapper process, RuntimeContext runtimeContext) {
        this.container = (DockerContainer) c;
        this.dockerClient=container.dockerClient();
        this.dockerContainerId=container.dockerContainerId();
        this.dockerContainerName=container.dockerContainerName();
        this.component = d;
        this.ctx = ctx;
    }

    @Override
    public long getPollPeriod() {
        return this.pollPeriod;
    }

    @Override
    public void setPollPeriod(long pollPeriod) {
        this.pollPeriod=pollPeriod;
    }

    @Override
    public boolean isRunning() {
        logger().fine("Checking if docker container [" + dockerContainerName + "] is still running...");
        return dockerClient.searchContainer(dockerContainerName,true).isPresent();
    }

    @Override
    public String getErrorMessage() {
        return "Docker container [" + dockerContainerName + "] is no longer running...";
    }

}
