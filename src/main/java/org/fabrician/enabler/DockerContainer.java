/*
 * Copyright (c) 2014 TIBCO Software Inc. All Rights Reserved.
 *
 * Use is subject to the terms of the TIBCO license terms accompanying the download of this code.
 * In most instances, the license terms are contained in a file named license.txt.
 */
package org.fabrician.enabler;

import java.io.File;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.math.NumberUtils;
import org.fabrician.enabler.predicates.ContainerPredicates;
import org.fabrician.enabler.util.BuildCmdOptions;
import org.fabrician.enabler.util.DockerActivationInfo;
import org.fabrician.enabler.util.DockerActivationInfo.Entry;
import org.fabrician.enabler.util.RunCmdAuxiliaryOptions;

import com.datasynapse.fabric.common.ActivationInfo;
import com.datasynapse.fabric.common.RuntimeContextVariable;
import com.datasynapse.fabric.container.ExecContainer;
import com.datasynapse.fabric.container.Feature;
import com.datasynapse.fabric.container.ProcessWrapper;
import com.datasynapse.fabric.domain.featureinfo.HttpFeatureInfo;
import com.datasynapse.fabric.util.ContainerUtils;
import com.datasynapse.fabric.util.DynamicVarsUtils;
import com.datasynapse.gridserver.engine.EngineProperties;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;

/**
 * A Silver Fabric Docker container proxy.
 * <p>
 * This proxy manages the Docker container lifecycle spawned and integrates the docker container to the Silver Fabric broker infrastructure.
 * </p>
 * 
 */
public class DockerContainer extends ExecContainer {
    private static final long serialVersionUID = 5194159591618328739L;
    private static final String BIND_ON_ALL_LOCAL_ADDRESSES_VAR = "BIND_ON_ALL_LOCAL_ADDRESSES";
    private static final String DOCKER_CONTAINER_BIND_ADDRESS_VAR = "DOCKER_CONTAINER_BIND_ADDRESS";
    private static final String DOCKER_CONTAINER_NAME_VAR = "DOCKER_CONTAINER_NAME";
    private static final String USE_SUDO_VAR = "USE_SUDO";
    private static final String REUSE_CONTAINER_VAR = "REUSE_CONTAINER";
    private static final String PORT_MAP_PREFIX = "!PORT_MAP_"; // all context vars that starts with "!PORT_MAP_" are reckoned to be a host-to-container ports mapping by convention
    private static final String VOL_MAP_PREFIX = "!VOL_MAP_"; // all context vars that starts with "!VOL_MAP_" are reckoned to be a host-to-container volume mapping by convention
    private static final String ENV_VAR_PREFIX = "!ENV_VAR_";// all context vars that starts with "!ENV_VAR" are reckoned to be container enviromental variables mapping by convention
    private static final String ENV_FILE_PREFIX = "!ENV_FILE_";// all context vars that starts with "!ENV_FILE_" are reckoned to be container enviromental variables mapping by convention
    private static final String DOCKER_PORT_MAPPINGS_VAR = "DOCKER_PORT_MAPPINGS";
    private static final String DOCKER_VOL_MAPPINGS_VAR = "DOCKER_VOL_MAPPINGS";
    private static final String DOCKER_ENVS_VAR = "DOCKER_ENVS";
    private static final String DOCKER_AUXILIARY_OPTIONS_VAR = "DOCKER_AUXILIARY_OPTIONS";
    // build
    private static final String DOCKER_CONTEXT_DIR_VAR = "DOCKER_CONTEXT_DIR";
    private static final String DOCKER_IMAGE_NAME_VAR = "DOCKER_IMAGE_NAME";
    private static final String BUILD_ALWAYS_VAR = "BUILD_ALWAYS";
    private static final String REUSE_IMAGE_VAR = "REUSE_IMAGE";
    private static final String BUILD_TIMEOUT_VAR = "BUILD_TIMEOUT";

    private HttpFeatureInfo httpFeatureInfo = null;
    private static final int UNDEFINED_PORT = -1;
    private String dockerContainerName;
    private String dockerImage;
    private String dockerContainerId;
    private boolean reuseContainer = false;
    private boolean useSudo = false;
    private DockerClient dockerClient;

    public boolean isHttpEnabled() {
        return (httpFeatureInfo != null ? httpFeatureInfo.isHttpEnabled() : false);
    }

    public boolean isHttpsEnabled() {
        return (httpFeatureInfo != null ? httpFeatureInfo.isHttpsEnabled() : false);
    }

    @Override
    protected void doInit(List<RuntimeContextVariable> additionalVariables) throws Exception {
        // Initialization done here before running Docker container, including building
        // Dockerfile
        getEngineLogger().fine("Invoking doInit()...");
        dockerClient = DockerClient.getInstance();
        dockerContainerName = resolveToString(DOCKER_CONTAINER_NAME_VAR);
        if (dockerContainerName.isEmpty()) {
            dockerContainerName = getDefaultDockerContainerName();
        }
        dockerImage = resolveToString(DOCKER_IMAGE_NAME_VAR);
        Validate.notEmpty(dockerImage, DOCKER_IMAGE_NAME_VAR + " must be specified.");

        useSudo = resolveToBoolean(USE_SUDO_VAR);
        reuseContainer = resolveToBoolean(REUSE_CONTAINER_VAR);
        // build image first if needed
        // check if we should build?
        boolean shouldBuild = resolveToBoolean(BUILD_ALWAYS_VAR);
        if (!shouldBuild) {
            getEngineLogger().info("Skipping build..");
            // check if the Docker image exists or not
            boolean image_exist = dockerClient().isImageExist(dockerImage);
            Validate.isTrue(image_exist, "Docker image specified [" + dockerImage + "] is not available on Docker host.");
        } else {
            buildDockerImage();
        }

        // /
        httpFeatureInfo = (HttpFeatureInfo) ContainerUtils.getFeatureInfo(Feature.HTTP_FEATURE_NAME, this, getCurrentDomain());

        boolean httpEnabled = isHttpEnabled();
        boolean httpsEnabled = isHttpsEnabled();
        if (!httpEnabled && !httpsEnabled) {
            throw new Exception("HTTP or HTTPS must be enabled in the Domain");
        }
        if (httpEnabled && !DynamicVarsUtils.validateIntegerVariable(this, HttpFeatureInfo.HTTP_PORT_VAR)) {
            throw new Exception("HTTP is enabled but the " + HttpFeatureInfo.HTTP_PORT_VAR + " runtime context variable is not set");
        }
        if (httpsEnabled && !DynamicVarsUtils.validateIntegerVariable(this, HttpFeatureInfo.HTTPS_PORT_VAR)) {
            throw new Exception("HTTPS is enabled but the " + HttpFeatureInfo.HTTPS_PORT_VAR + " runtime context variable is not set");
        }

        String bindAllStr = getStringVariableValue(BIND_ON_ALL_LOCAL_ADDRESSES_VAR, "true");
        boolean bindAll = Boolean.valueOf(bindAllStr);
        String dockerBindAddress = bindAll ? "0.0.0.0" : getStringVariableValue(LISTEN_ADDRESS_VAR);

        additionalVariables.add(new RuntimeContextVariable(DOCKER_CONTAINER_BIND_ADDRESS_VAR, dockerBindAddress, RuntimeContextVariable.STRING_TYPE));
        // construct a host ports to docker container ports mappings
        buildHostToDockerContainerPortMappings(additionalVariables);
        // construct a host vols to docker container vol mappings
        buildHostToDockerContainerVolumeMappings(additionalVariables);
        // construct env to be set inside docker container
        buildDockerContainerEnvs(additionalVariables);
        // construct other options not related to env,ports or volumes
        buildDockerContainerAuxiliaryOptions(additionalVariables);
        super.doInit(additionalVariables);
        getEngineLogger().fine("Invoked doInit()...");
    }

    private void buildHostToDockerContainerPortMappings(List<RuntimeContextVariable> additionalVariables) throws Exception {
        StringBuilder sb = new StringBuilder();
        Map<String, String> badPortMap = new HashMap<String, String>();
        for (int i = 0; i < getRuntimeContext().getVariableCount(); i++) {
            RuntimeContextVariable var = getRuntimeContext().getVariable(i);
            if (var.getTypeInt() != RuntimeContextVariable.OBJECT_TYPE) {
                String name = var.getName();
                if (!name.startsWith(PORT_MAP_PREFIX)) {
                    continue;
                }
                String currentValue = StringUtils.trimToEmpty((String) var.getValue());
                if (currentValue.isEmpty()) {
                    continue;
                }
                if (!isHostPortVacant(name, currentValue)) {
                    badPortMap.put(name, currentValue);
                }
                String newValue = "${DOCKER_CONTAINER_BIND_ADDRESS}:" + currentValue;
                sb.append(" -p ").append(newValue);
            }
        }
        if (!badPortMap.isEmpty()) {
            throw new Exception("Bad host-to-docker container port mappings detected :\n" + badPortMap);
        }
        additionalVariables.add(new RuntimeContextVariable(DOCKER_PORT_MAPPINGS_VAR, sb.toString(), RuntimeContextVariable.ENVIRONMENT_TYPE));
    }

    private boolean isHostPortVacant(final String varName, final String varValue) {
        String host_port_str = StringUtils.substringBefore(varValue, ":");
        int host_port = NumberUtils.createInteger(host_port_str);
        if (host_port < 1024) {
            throw new RuntimeException("Illegal Host port [" + host_port + "] specified in the [0-1023] well-known TCP/UDP ports range.");
        }
        if (checkServerPortInUse(host_port)) {
            getEngineLogger().warning("External host port [" + host_port_str + "] associated with runtime context variable [" + varName + "] is already in use.");
            return false;
        }
        return true;

    }

    private void buildHostToDockerContainerVolumeMappings(List<RuntimeContextVariable> additionalVariables) throws Exception {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < getRuntimeContext().getVariableCount(); i++) {
            RuntimeContextVariable var = getRuntimeContext().getVariable(i);
            if (var.getTypeInt() != RuntimeContextVariable.OBJECT_TYPE) {
                String name = var.getName();
                if (!name.startsWith(VOL_MAP_PREFIX)) {
                    continue;
                }
                String currentValue = StringUtils.trimToEmpty((String) var.getValue());
                if (currentValue.isEmpty()) {
                    continue;
                }
                sb.append(" -v ").append(currentValue);
            }
        }
        additionalVariables.add(new RuntimeContextVariable(DOCKER_VOL_MAPPINGS_VAR, sb.toString(), RuntimeContextVariable.ENVIRONMENT_TYPE));
    }

    private void buildDockerContainerEnvs(List<RuntimeContextVariable> additionalVariables) throws Exception {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < getRuntimeContext().getVariableCount(); i++) {
            RuntimeContextVariable var = getRuntimeContext().getVariable(i);
            if (var.getTypeInt() != RuntimeContextVariable.OBJECT_TYPE) {
                String name = var.getName();
                if (!(name.startsWith(ENV_VAR_PREFIX) || name.startsWith(ENV_FILE_PREFIX))) {
                    continue;
                }

                String currentValue = StringUtils.trimToEmpty((String) var.getValue());
                if (currentValue.isEmpty()) {
                    continue;
                }
                if (name.startsWith(ENV_VAR_PREFIX)) {
                    sb.append(" -e ").append(currentValue);
                } else if (name.startsWith(ENV_FILE_PREFIX)) {
                    sb.append(" --env-file ").append(currentValue);
                }
            }
        }
        additionalVariables.add(new RuntimeContextVariable(DOCKER_ENVS_VAR, sb.toString(), RuntimeContextVariable.ENVIRONMENT_TYPE));
    }

    private void buildDockerContainerAuxiliaryOptions(List<RuntimeContextVariable> additionalVariables) throws Exception {
        String options = RunCmdAuxiliaryOptions.buildAll(getRuntimeContext());
        additionalVariables.add(new RuntimeContextVariable(DOCKER_AUXILIARY_OPTIONS_VAR, options, RuntimeContextVariable.ENVIRONMENT_TYPE));
    }

    @Override
    protected void doStart() throws Exception {
        // run the Docker container here
        getEngineLogger().fine("Invoking doStart()...");
        // remove existing container if exists
        removeExistingContainer();
        super.doStart();
        getEngineLogger().fine("doStart() invoked");
    }

    @Override
    protected void doInstall(ActivationInfo info) throws Exception {
        // things to do after starting docker container
        getEngineLogger().fine("Invoking doInstall()...");
        // Add any additional Docker container inspection info here
        dockerContainerId = DockerActivationInfo.inject(info, this.dockerContainerName, this.dockerClient()).getDockerProperty(Entry.docker_id).orNull();
        if (isHttpEnabled()) {
            info.setProperty(HttpFeatureInfo.HTTP_PORT_VAR, String.valueOf(getHttpPort()));
        }
        if (isHttpEnabled()) {
            info.setProperty(HttpFeatureInfo.HTTPS_PORT_VAR, String.valueOf(getHttpsPort()));
        }
        super.doInstall(info);
        // log the docker inspect container to the engine logger
        logDockerInspectMetadata(dockerContainerId);
        getEngineLogger().fine("doInstall() invoked");
    }

    // this logs the docker container inspect metadata into the engine log
    private void logDockerInspectMetadata(final String dockerContainerId) throws Exception {
        getEngineLogger().info(Strings.repeat("-", 10) + "inspect metadata for [" + dockerContainerId + "]" + Strings.repeat("-", 10));
        String command = "docker inspect " + dockerContainerId;
        if (useSudo) {
            command += "sudo ";
        }
        File workDir = new File(getWorkDir());
        ProcessWrapper p = null;
        try {
            String engineOS = getEngineProperty(EngineProperties.OS);
            p = getProcessWrapper(command, workDir, engineOS + "_inspect.pid");
            Object lock = p.getLock();
            p.exec();
            synchronized (lock) {
                try {
                    if (p.isRunning()) {
                        lock.wait(3000); // 3 secs
                        if (p.isRunning()) {
                            p.destroy();
                        }
                    }
                } catch (InterruptedException e) {
                    getEngineLogger().warning("logDockerInspectMetadata() thread was interrupted");
                }
            }
        } catch (Exception ex) {
            getEngineLogger().log(Level.SEVERE, "while inspecting docker container [" + getName() + "]", ex);
        }
    }

    private void buildDockerImage() throws Exception {
        getEngineLogger().info(Strings.repeat("-", 10) + "building [" + dockerImage + "]" + Strings.repeat("-", 10));
        // check if we should reuse existing image?
        boolean reuse_image = resolveToBoolean(REUSE_IMAGE_VAR);
        boolean image_exist = dockerClient().isImageExist(dockerImage);
        if (reuse_image) {
            if (image_exist) {
                getEngineLogger().info("Existing docker image [" + dockerImage + "] exists and will be reused.");
                return;
            }
        } else {
            // delete existing image before build
            if (image_exist) {
                getEngineLogger().info("Deleting existing docker image [" + dockerImage + "] before build.");
                dockerClient().deleteImageByTag(dockerImage);
            }
        }
        // check if Docker file exits?
        File dockerContextDir = resolveToFile(DOCKER_CONTEXT_DIR_VAR);
        File dockerFile = new File(dockerContextDir, "Dockerfile");
        Validate.isTrue(dockerFile.exists(), "Required Dockerfile for build does not exist at path [" + dockerFile.getCanonicalPath() + "]");
        // construct build options
        String command = "docker build " + BuildCmdOptions.buildAll(getRuntimeContext()) + " -t " + dockerImage + " . ";
        if (useSudo) {
            command += "sudo ";
        }
        getEngineLogger().info(Strings.repeat("-",10) + " begin build command " + Strings.repeat("-",10));
        getEngineLogger().info(command);
        getEngineLogger().info(Strings.repeat("-",10) + " end build command " + Strings.repeat("-",10));
        ProcessWrapper p = null;
        int build_timeout = resolveToInteger(BUILD_TIMEOUT_VAR);// in secs
        try {
            String engineOS = getEngineProperty(EngineProperties.OS);
            p = getProcessWrapper(command, dockerContextDir, engineOS + "_build.pid");
            Object lock = p.getLock();
            p.exec();
            synchronized (lock) {
                try {
                    if (p.isRunning()) {
                        lock.wait(build_timeout * 1000);
                        if (p.isRunning()) {
                            p.destroy();
                        }
                    }
                } catch (InterruptedException e) {
                    getEngineLogger().warning("buildDockerImage() thread was interrupted");
                }
            }
        } catch (Exception ex) {
            getEngineLogger().log(Level.SEVERE, "while building docker image [" + dockerImage + "]", ex);
            throw ex;
        }
        // check for image
        
        image_exist = dockerClient().isImageExist(dockerImage);
        if(!image_exist){
            throw new Exception("Build failed for image [" + dockerImage + "]");
        }else{
            getEngineLogger().info("Build succeeded for image [" + dockerImage + "]");
        }
    }

    @Override
    protected void doUninstall() throws Exception {
        // things to do before stopping docker container
        super.doUninstall();
    }

    @Override
    protected void doShutdown() throws Exception {
        getEngineLogger().fine("Invoking doShutdown...");
        getEngineLogger().info("Stopping docker container : " + dockerContainerInfo() + "...");
        long shutdownStart = System.currentTimeMillis();
        if ((getProcess() != null) && (getProcess().isRunning())) {
            getEngineLogger().info("Doing blocking process shutdown...");
            super.doShutdown();
            waitForShutdown(shutdownStart);
        } else {
            // we may have a docker container started in detach mode with "-d" switch
            getEngineLogger().info("Doing non-blocking process shutdown...");
            super.doShutdown();
            ContainerPredicates.awaitStopped(dockerClient().remoteApi());
        }
        // stop monitoring for unexpected container crash now.
        setRunCrashMonitor(false);
        getEngineLogger().fine("doShutdown invoked");
    }

    @Override
    public void cleanup() throws Exception {
        getEngineLogger().fine("Invoking container cleanup()...");
        // delete docker container first
        boolean deleteWorkDir = Boolean.valueOf(getStringVariableValue(DELETE_RUNTIME_DIR_ON_SHUTDOWN_VAR, "true")).booleanValue();
        if (deleteWorkDir) {
            getEngineLogger().info("Removing docker container : " + dockerContainerInfo() + "...");
            dockerClient().removeContainer(dockerContainerId(), null);
        }
        // next delete work area
        super.cleanup();
        getEngineLogger().fine("Invoked container cleanup()...");
    }

    private void removeExistingContainer() {
        if (!reuseContainer) {
            dockerClient().removeContainer(dockerContainerId(), null);
        }
    }

    // try and open a server socket. If we can then close it and return false
    // otherwise assume its in use and return true
    private boolean checkServerPortInUse(int port) {
        if (port == UNDEFINED_PORT) {
            return false;
        }
        ServerSocket srv = null;
        boolean inUse = false;
        try {
            srv = new ServerSocket(port);
            srv.close();
        } catch (Exception e) {
            getEngineLogger().finest("serverPortInUse: debug exception: " + e);
            inUse = true;
        }
        return inUse;
    }

    private int getHttpPort() throws Exception {
        return Integer.valueOf(getStringVariableValue(HttpFeatureInfo.HTTP_PORT_VAR, null));
    }

    private int getHttpsPort() throws Exception {
        return Integer.valueOf(getStringVariableValue(HttpFeatureInfo.HTTPS_PORT_VAR, null));
    }

    private String getDefaultDockerContainerName() {
        try {
            Joiner joiner = Joiner.on("_").skipNulls();
            String componentName = getComponentName();
            String username = getEngineUsername();
            String instance = getEngineInstanceId();
            return joiner.join(username, componentName, instance).toLowerCase();
        } catch (Exception ex) {

        }
        return UUID.randomUUID().toString();
    }

    private String getEngineInstanceId() throws Exception {
        return getEngineProperty(EngineProperties.INSTANCE);
    }

    private String getEngineUsername() throws Exception {
        return getEngineProperty(EngineProperties.USERNAME);
    }

    private String getComponentName() {
        String componentName = getCurrentDomain().getName();
        Iterable<String> parts = Splitter.on(" ").omitEmptyStrings().trimResults().split(componentName);
        Joiner joiner = Joiner.on("_").skipNulls();
        return joiner.join(parts);
    }

    private String resolveToString(String runtimeContextVariableName) throws Exception {
        String val = StringUtils.trimToEmpty(getStringVariableValue(runtimeContextVariableName));
        return val;
    }

    private int resolveToInteger(String runtimeContextVariableName) throws Exception {
        String val = StringUtils.trimToEmpty(getStringVariableValue(runtimeContextVariableName));
        return NumberUtils.toInt(val);
    }

    private boolean resolveToBoolean(String runtimeContextVariableName) throws Exception {
        String val = StringUtils.trimToEmpty(getStringVariableValue(runtimeContextVariableName));
        return BooleanUtils.toBoolean(val);
    }

    private File resolveToFile(String runtimeContextVariableName) throws Exception {
        String val = StringUtils.trimToEmpty(getStringVariableValue(runtimeContextVariableName));
        return new File(val).getCanonicalFile();
    }

    Logger logger() {
        return getEngineLogger();
    }

    DockerClient dockerClient() {
        return this.dockerClient;
    }

    String dockerContainerId() {
        return this.dockerContainerId;
    }

    String dockerContainerName() {
        return this.dockerContainerName;
    }

    String dockerContainerInfo() {
        return "[" + dockerContainerName() + "][" + dockerContainerId() + "]";
    }

}
