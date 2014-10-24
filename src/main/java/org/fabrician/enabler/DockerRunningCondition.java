/*
 * Copyright (c) 2014 TIBCO Software Inc. All Rights Reserved.
 *
 * Use is subject to the terms of the TIBCO license terms accompanying the download of this code.
 * In most instances, the license terms are contained in a file named license.txt.
 */
package org.fabrician.enabler;

import java.util.logging.Logger;

import org.fabrician.enabler.util.DockerActivationInfo;
import org.fabrician.enabler.util.TimeUtil;

import com.datasynapse.fabric.common.ActivationInfo;
import com.datasynapse.fabric.common.RunningCondition;
import com.datasynapse.fabric.common.RuntimeContext;
import com.datasynapse.fabric.container.Container;
import com.datasynapse.fabric.container.ProcessWrapper;
import com.datasynapse.fabric.domain.Domain;
import com.google.common.base.Optional;

public class DockerRunningCondition implements RunningCondition {
    private DockerContainer enabler;
    private Domain component;
    private RuntimeContext ctx;
    private static final long DEFAULT_POLL_PERIOD = 5000; // 5secs
    private long pollPeriod = DEFAULT_POLL_PERIOD;
    private DockerClient dockerClient;
    private String dockerContainerId;
    private String dockerContainerName;

    public DockerRunningCondition() {}

    private Logger logger() {
        return this.enabler.logger();
    }

    @Override
    public void init(Container c, Domain component, ProcessWrapper process, RuntimeContext runtimeContext) {
        this.enabler = (DockerContainer) c;
        this.dockerClient = enabler.dockerClient();
        this.dockerContainerId = enabler.dockerContainerId();
        this.dockerContainerName = enabler.dockerContainerName();
        this.component = component;
        this.ctx = ctx;
    }

    @Override
    public long getPollPeriod() {
        return this.pollPeriod;
    }

    @Override
    public void setPollPeriod(long pollPeriod) {
        this.pollPeriod = pollPeriod;
    }

    @Override
    public boolean isRunning() {
        logger().fine("Checking if docker container [" + dockerContainerName + "] is still running...");
        Optional<org.jclouds.docker.domain.Container> dockerContainer = dockerClient.searchContainer(dockerContainerName, true);
        if (dockerContainer.isPresent()) {
            ActivationInfo info = enabler.getActivationInfo();
            String status = "Up " + TimeUtil.formatDurationMsg(dockerContainer.get().getState().getStartedAt());
            info.setProperty(DockerActivationInfo.Entry.docker_status.name(), status);
            enabler.updateActivationInfoProperties(info);
        }
        return dockerContainer.isPresent();
    }

    @Override
    public String getErrorMessage() {
        return "Docker container [" + dockerContainerName + "] is no longer running...";
    }

}
