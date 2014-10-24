/*
 * Copyright (c) 2014 TIBCO Software Inc. All Rights Reserved.
 *
 * Use is subject to the terms of the TIBCO license terms accompanying the download of this code.
 * In most instances, the license terms are contained in a file named license.txt.
 */
package org.fabrician.enabler.util;

import org.apache.commons.lang3.StringUtils;
import org.fabrician.enabler.DockerClient;
import org.jclouds.docker.domain.Container;

import com.datasynapse.fabric.common.ActivationInfo;
import com.google.common.base.Optional;

/**
 * Injects an instance of Silver Fabric Engine activation info with useful selected data from a Docker container
 * 
 */
public class DockerActivationInfo {
    public enum Entry {
        docker_name, docker_id, docker_created, docker_image, docker_state, docker_status, docker_host_config, docker_config, docker_networks, docker_volumes, docker_vol_rw;
    }

    private final ActivationInfo info;
    private final Container docker;

    private DockerActivationInfo(final ActivationInfo info, final Container docker) {
        this.info = info;
        this.docker = docker;
    }

    public static DockerActivationInfo instance(final ActivationInfo info, final Container dockerContainer) {
        return new DockerActivationInfo(info, dockerContainer);
    }

    public DockerActivationInfo inject() {
        info.setProperty(Entry.docker_name.name(), docker.getName());
        info.setProperty(Entry.docker_id.name(), docker.getId());
        info.setProperty(Entry.docker_created.name(), docker.getCreated());
        info.setProperty(Entry.docker_image.name(), docker.getImage());
        info.setProperty(Entry.docker_config.name(), StringUtils.substringAfter(docker.getContainerConfig().toString(), "Config"));
        info.setProperty(Entry.docker_state.name(), StringUtils.substringAfter(docker.getState().toString(), "State"));
        info.setProperty(Entry.docker_status.name(), "Up " + TimeUtil.formatDurationMsg(docker.getState().getStartedAt()));
        info.setProperty(Entry.docker_host_config.name(), StringUtils.substringAfter(docker.getHostConfig().toString(), "HostConfig"));
        info.setProperty(Entry.docker_networks.name(), StringUtils.substringAfter(docker.getNetworkSettings().toString(), "NetworkSettings"));
        info.setProperty(Entry.docker_volumes.name(), docker.getVolumes().toString());
        info.setProperty(Entry.docker_vol_rw.name(), docker.getvolumesRW().toString());
        return this;
    }

    public Optional<String> getDockerProperty(Entry entry) {
        return Optional.fromNullable(info.getProperty(entry.name()));
    }

    public static DockerActivationInfo inject(final ActivationInfo info, final String containerIdentity, final DockerClient client) {
        Optional<Container> c = client.inspectContainer(containerIdentity);
        if (c.isPresent()) {
            DockerActivationInfo.instance(info, c.get()).inject();
        }
        return DockerActivationInfo.instance(info, null);
    }
}
