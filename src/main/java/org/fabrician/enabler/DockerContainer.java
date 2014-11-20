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
import java.util.Properties;
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
import org.fabrician.enabler.util.DockerfileBuildLock;
import org.fabrician.enabler.util.ExecCmdProcessInjector;
import org.fabrician.enabler.util.RunCmdAuxiliaryOptions;
import org.fabrician.enabler.util.SpecialDirective;

import com.datasynapse.fabric.admin.info.NotificationEngineInfo;
import com.datasynapse.fabric.common.ActivationInfo;
import com.datasynapse.fabric.common.ArchiveActivationInfo;
import com.datasynapse.fabric.common.RuntimeContextVariable;
import com.datasynapse.fabric.common.message.ArchiveLocator;
import com.datasynapse.fabric.container.AbstractContainer;
import com.datasynapse.fabric.container.ArchiveDetail;
import com.datasynapse.fabric.container.ArchiveManagement;
import com.datasynapse.fabric.container.ArchiveProvider;
import com.datasynapse.fabric.container.ExecContainer;
import com.datasynapse.fabric.container.Feature;
import com.datasynapse.fabric.container.ProcessWrapper;
import com.datasynapse.fabric.domain.featureinfo.HttpFeatureInfo;
import com.datasynapse.fabric.util.ContainerUtils;
import com.datasynapse.fabric.util.DynamicVarsUtils;
import com.datasynapse.gridserver.engine.EngineProperties;
import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.google.common.io.BaseEncoding;
import static org.fabrician.enabler.util.SpecialDirective.PORT_EXPOSE;
import static org.fabrician.enabler.util.SpecialDirective.PORT_MAP;
import static org.fabrician.enabler.util.SpecialDirective.VOL_MAP;
import static org.fabrician.enabler.util.SpecialDirective.ENV_VAR;
import static org.fabrician.enabler.util.SpecialDirective.ENV_FILE;
import static org.fabrician.enabler.util.SpecialDirective.SEC_OPT;

/**
 * A Silver Fabric Docker container proxy.
 * <p>
 * This proxy manages the Docker container lifecycle spawned and integrates the docker container to the Silver Fabric broker infrastructure.
 * </p>
 * 
 */
public class DockerContainer extends ExecContainer implements ArchiveManagement, ArchiveProvider {
    private static final long serialVersionUID = 5194159591618328739L;
    private static final String BIND_ON_ALL_LOCAL_ADDRESSES_VAR = "BIND_ON_ALL_LOCAL_ADDRESSES";
    private static final String DOCKER_CONTAINER_BIND_ADDRESS_VAR = "DOCKER_CONTAINER_BIND_ADDRESS";
    private static final String DOCKER_CONTAINER_NAME_VAR = "DOCKER_CONTAINER_NAME";
    private static final String DOCKER_CONTAINER_TAG_VAR = "DOCKER_CONTAINER_TAG";
    private static final String USE_SUDO_VAR = "USE_SUDO";
    private static final String REUSE_CONTAINER_VAR = "REUSE_CONTAINER";

    private static final String EXEC_CMD_FILE_VAR = "EXEC_CMD_FILE";
    private static final String EXEC_CMD_DELAY_VAR = "EXEC_CMD_DELAY";
    private static final String EXEC_CMD_ENABLED_VAR = "EXEC_CMD_ENABLED";

    private static final String DOCKER_PORTS_EXPOSED_VAR = "DOCKER_PORTS_EXPOSED";
    private static final String DOCKER_PORT_MAPPINGS_VAR = "DOCKER_PORT_MAPPINGS";
    private static final String DOCKER_VOL_MAPPINGS_VAR = "DOCKER_VOL_MAPPINGS";
    private static final String DOCKER_ENVS_VAR = "DOCKER_ENVS";
    private static final String DOCKER_AUXILIARY_OPTIONS_VAR = "DOCKER_AUXILIARY_OPTIONS";
    private static final String DOCKER_SECURITY_OPTIONS_VAR = "DOCKER_SECURITY_OPTS";

    // build
    private static final String DOCKER_CONTEXT_DIR_VAR = "DOCKER_CONTEXT_DIR";
    private static final String DOCKER_IMAGE_NAME_VAR = "DOCKER_IMAGE_NAME";
    private static final String BUILD_ALWAYS_VAR = "BUILD_ALWAYS";
    private static final String REUSE_IMAGE_VAR = "REUSE_IMAGE";
    private static final String BUILD_TIMEOUT_VAR = "BUILD_TIMEOUT";

    private HttpFeatureInfo httpFeatureInfo = null;
    private static final int UNDEFINED_PORT = -1;
    private String dockerContainerTag;
    private String dockerImage;
    private String dockerContainerId;
    private String dockerBindAddress = "0.0.0.0";
    private boolean reuseContainer = false;
    private boolean useSudo = false;
    private DockerClient dockerClient;
    private int maxBuildLockRetries = 20;
    private int buildLockRetryPause = 10;

    public boolean isHttpEnabled() {
        return (httpFeatureInfo != null ? httpFeatureInfo.isHttpEnabled() : false);
    }

    public boolean isHttpsEnabled() {
        return (httpFeatureInfo != null ? httpFeatureInfo.isHttpsEnabled() : false);
    }

    public int getMaxBuildLockRetries() {
        return maxBuildLockRetries;
    }

    public void setMaxBuildLockRetries(int maxBuildLockRetries) {
        this.maxBuildLockRetries = maxBuildLockRetries;
    }

    public int getBuildLockRetryPause() {
        return buildLockRetryPause;
    }

    public void setBuildLockRetryPause(int buildLockRetryPause) {
        this.buildLockRetryPause = buildLockRetryPause;
    }

    @Override
    protected void doInit(List<RuntimeContextVariable> additionalVariables) throws Exception {
        // Initialization done here before running Docker container, including building
        // Dockerfile
        getEngineLogger().fine("Invoking doInit()...");
        validateHttpFeature();
        dockerClient = DockerClient.getInstance();
        dockerContainerTag = resolveDockerContainerTag();
        dockerImage = resolveDockerImage();
        useSudo = resolveToBoolean(USE_SUDO_VAR);
        reuseContainer = resolveToBoolean(REUSE_CONTAINER_VAR);
        dockerBindAddress = resolveDockerBindingAddress();
        // build image first if needed
        // check if we should build?
        boolean shouldBuild = resolveToBoolean(BUILD_ALWAYS_VAR);
        if (!shouldBuild) {
            getEngineLogger().info("Skipping build..");
            // check if the Docker image exists or not
            boolean image_exist = dockerClient().isImageExist(dockerImage);
            Validate.isTrue(image_exist, "Docker image specified [" + dockerImage + "] is not available on Docker host.");
        } else {
            File dockerFile = resolveDockerfile();
            Optional<DockerfileBuildLock> lock = DockerfileBuildLock.acquire(dockerImage, dockerFile, getMaxBuildLockRetries(), getBuildLockRetryPause());
            if (lock.isPresent()) {
                buildDockerImage();
                lock.get().release();
            } else {
                throw new Exception("Can't build from Docker file due to failure to acquire build lock!");
            }
        }

        additionalVariables.add(new RuntimeContextVariable(DOCKER_CONTAINER_TAG_VAR, dockerContainerTag, RuntimeContextVariable.ENVIRONMENT_TYPE));
        additionalVariables.add(new RuntimeContextVariable(DOCKER_CONTAINER_BIND_ADDRESS_VAR, dockerBindAddress, RuntimeContextVariable.STRING_TYPE));
        // expose any additional private ports
        buildDockerContainerExposedPorts(additionalVariables);
        // construct a host ports to docker container ports mappings
        buildHostToDockerContainerPortMappings(additionalVariables);
        // construct a host vols to docker container vol mappings
        buildHostToDockerContainerVolumeMappings(additionalVariables);
        // construct env to be set inside docker container
        buildDockerContainerEnvs(additionalVariables);
        // construct other options not related to env,ports or volumes
        buildDockerContainerAuxiliaryOptions(additionalVariables);
        // construct security options
        buildDockerContainerSecurityOptions(additionalVariables);
        super.doInit(additionalVariables);
        getEngineLogger().fine("Invoked doInit()...");
    }

    private void buildDockerContainerExposedPorts(List<RuntimeContextVariable> additionalVariables) throws Exception {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < getRuntimeContext().getVariableCount(); i++) {
            RuntimeContextVariable var = getRuntimeContext().getVariable(i);
            if (var.getTypeInt() != RuntimeContextVariable.OBJECT_TYPE) {
                String name = var.getName();
                if (!PORT_EXPOSE.prefix(name)) {
                    continue;
                }
                String currentValue=resolveAndSet(var);
                if (currentValue.isEmpty()) {
                    continue;
                }
                sb.append(" --expose ").append(currentValue);
            }
        }

        additionalVariables.add(new RuntimeContextVariable(DOCKER_PORTS_EXPOSED_VAR, sb.toString(), RuntimeContextVariable.ENVIRONMENT_TYPE));
    }

    private void buildHostToDockerContainerPortMappings(List<RuntimeContextVariable> additionalVariables) throws Exception {
        StringBuilder sb = new StringBuilder();
        Map<String, String> badPortMap = new HashMap<String, String>();
        for (int i = 0; i < getRuntimeContext().getVariableCount(); i++) {
            RuntimeContextVariable var = getRuntimeContext().getVariable(i);
            if (var.getTypeInt() != RuntimeContextVariable.OBJECT_TYPE) {
                String name = var.getName();
                if (!PORT_MAP.prefix(name)) {
                    continue;
                }
                String currentValue=resolveAndSet(var);
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
        additionalVariables.add(new RuntimeContextVariable(DOCKER_PORT_MAPPINGS_VAR, resolveVariables(sb.toString()), RuntimeContextVariable.ENVIRONMENT_TYPE));
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
                if (!VOL_MAP.prefix(name)) {
                    continue;
                }
                String currentValue=resolveAndSet(var);
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
                if (!(ENV_VAR.prefix(name) || ENV_FILE.prefix(name))) {
                    continue;
                }

                String currentValue=resolveAndSet(var);
                if (currentValue.isEmpty()) {
                    continue;
                }
                if (ENV_VAR.prefix(name)) {
                    sb.append(" -e ").append(currentValue);
                } else if (ENV_FILE.prefix(name)) {
                    sb.append(" --env-file ").append(currentValue);
                }
            }
        }
        additionalVariables.add(new RuntimeContextVariable(DOCKER_ENVS_VAR, sb.toString(), RuntimeContextVariable.ENVIRONMENT_TYPE));
    }

    private void buildDockerContainerAuxiliaryOptions(List<RuntimeContextVariable> additionalVariables) throws Exception {
        String options = RunCmdAuxiliaryOptions.buildAll(getRuntimeContext());
        additionalVariables.add(new RuntimeContextVariable(DOCKER_AUXILIARY_OPTIONS_VAR, resolveVariables(options), RuntimeContextVariable.ENVIRONMENT_TYPE));
    }

    private void buildDockerContainerSecurityOptions(List<RuntimeContextVariable> additionalVariables) throws Exception {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < getRuntimeContext().getVariableCount(); i++) {
            RuntimeContextVariable var = getRuntimeContext().getVariable(i);
            if (var.getTypeInt() != RuntimeContextVariable.OBJECT_TYPE) {
                String name = var.getName();
                if (!SEC_OPT.prefix(name)) {
                    continue;
                }
                String currentValue=resolveAndSet(var);
                if (currentValue.isEmpty()) {
                    continue;
                }
                sb.append(" --security-opt ").append(currentValue);
            }
        }
        additionalVariables.add(new RuntimeContextVariable(DOCKER_SECURITY_OPTIONS_VAR, sb.toString(), RuntimeContextVariable.ENVIRONMENT_TYPE));
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
        dockerContainerId = DockerActivationInfo.inject(info, this.dockerContainerTag, this.dockerClient()).getDockerProperty(Entry.docker_id).orNull();
        if (isHttpEnabled()) {
            info.setProperty(HttpFeatureInfo.HTTP_PORT_VAR, String.valueOf(getHttpPort()));
        }
        if (isHttpEnabled()) {
            info.setProperty(HttpFeatureInfo.HTTPS_PORT_VAR, String.valueOf(getHttpsPort()));
        }
        super.doInstall(info);
        // log the docker inspect container to the engine logger
        logDockerInspectMetadata(dockerContainerId);
        // inject helper processes into activated parent Docker container
        injectDockerHelperProcesses();
        getEngineLogger().fine("doInstall() invoked");
    }

    // this logs the docker container inspect metadata into the engine log
    private void logDockerInspectMetadata(final String dockerContainerId) throws Exception {
        getEngineLogger().info(Strings.repeat("-", 10) + "inspect metadata for [" + dockerContainerId + "]" + Strings.repeat("-", 10));
        String command = "docker inspect " + dockerContainerId;
        if (useSudo) {
            command = "sudo " + command;
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
            getEngineLogger().log(Level.SEVERE, "while inspecting docker container [" + dockerContainerId + "]", ex);
        }
    }

    private void injectDockerHelperProcesses() throws Exception {
        getEngineLogger().info(Strings.repeat("-", 10) + "injecting post-activation helper processes into docker container [" + dockerContainerId + "]" + Strings.repeat("-", 10));
        boolean inject = resolveToBoolean(EXEC_CMD_ENABLED_VAR);
        if (inject) {
            File cmdFile = resolveToFile(EXEC_CMD_FILE_VAR);
            Validate.isTrue(cmdFile.exists(), "The command file for 'docker exec' use does not exist at path [" + cmdFile.getCanonicalPath() + "]");
            int delay = resolveToInteger(EXEC_CMD_DELAY_VAR);
            ExecCmdProcessInjector.exec(this, cmdFile.toURI().toURL(), delay);
        } else {
            getEngineLogger().info("Skipping 'docker exec' since " + EXEC_CMD_ENABLED_VAR + " is disabled.");
        }
    }

    public ProcessWrapper getExecCmdProcessWrapper(String execCmd) throws Exception {
        String command = "docker exec -d " + dockerContainerTag + " " + execCmd;
        if (useSudo) {
            command = "sudo " + command;
        }
        File workDir = new File(getWorkDir());
        HashFunction hf = Hashing.md5();
        HashCode hc = hf.newHasher().putString(command, Charsets.UTF_8).hash();
        String cmd_pid = "_cmd_" + BaseEncoding.base64Url().encode(hc.asBytes()) + ".pid";
        try {
            String engineOS = getEngineProperty(EngineProperties.OS);
            ProcessWrapper p = getProcessWrapper(command, workDir, engineOS + cmd_pid);
            return p;
        } catch (Exception ex) {
            getEngineLogger().log(Level.SEVERE, "while getting a process wrapper for 'docker exec' command [" + execCmd + "]", ex);
            throw ex;
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
            command = "sudo " + command;
        }
        getEngineLogger().info(Strings.repeat("-", 10) + " begin build command " + Strings.repeat("-", 10));
        getEngineLogger().info(command);
        getEngineLogger().info(Strings.repeat("-", 10) + " end build command " + Strings.repeat("-", 10));
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
        if (!image_exist) {
            throw new Exception("Build failed for image [" + dockerImage + "]");
        } else {
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

    private File resolveDockerfile() throws Exception {
        File dockerContextDir = resolveToFile(DOCKER_CONTEXT_DIR_VAR);
        File dockerFile = new File(dockerContextDir, "Dockerfile");
        Validate.isTrue(dockerFile.exists(), "Required Dockerfile for build does not exist at path [" + dockerFile.getCanonicalPath() + "]");
        return dockerFile;
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

    private String createDockerContainerTag() {
        try {
            Joiner joiner = Joiner.on("_").skipNulls();
            String componentName = getComponentName();
            getEngineLogger().info("component name=[" + componentName + "]");
            String username = getEngineUsername();
            getEngineLogger().info("username=[" + username + "]");
            String instance = getEngineInstanceId();
            return joiner.join(componentName, username, instance).toLowerCase();
        } catch (Exception ex) {

        }
        return UUID.randomUUID().toString();
    }

    private String getEngineInstanceId() throws Exception {
        return getEngineProperty(EngineProperties.INSTANCE);
    }

    private String getEngineUsername() throws Exception {
        String username = getEngineProperty(EngineProperties.USERNAME);
        username = StringUtils.replaceChars(username, '-', '_');
        return username;
    }

    private String getComponentName() {
        String componentName = getCurrentDomain().getName();
        if (componentName != null) {
            Iterable<String> parts = Splitter.on(" ").omitEmptyStrings().trimResults().split(componentName);
            Joiner joiner = Joiner.on("_").skipNulls();
            componentName = joiner.join(parts);
        }
        componentName = StringUtils.replaceChars(componentName, '-', '_');
        return componentName;
    }

    private String resolveDockerBindingAddress() throws Exception {
        String bindAllStr = getStringVariableValue(BIND_ON_ALL_LOCAL_ADDRESSES_VAR, "true");
        boolean bindAll = Boolean.valueOf(bindAllStr);
        String bindAddress = bindAll ? "0.0.0.0" : getStringVariableValue(LISTEN_ADDRESS_VAR);
        return bindAddress;
    }

    private String resolveDockerImage() throws Exception {
        String image = resolveToString(DOCKER_IMAGE_NAME_VAR);
        Validate.notEmpty(image, DOCKER_IMAGE_NAME_VAR + " must be specified.");
        return image;
    }

    private String resolveDockerContainerTag() throws Exception {
        String tag = resolveToString(DOCKER_CONTAINER_NAME_VAR);
        if (tag.isEmpty() || NumberUtils.isDigits(tag)) {
            tag = createDockerContainerTag();
            getEngineLogger().info("Auto-generate Docker container name tag to [" + tag + "]");
        }
        return tag;
    }

    private String resolveToString(String runtimeContextVariableName) throws Exception {
        String val = SpecialDirective.resolveStringValue(this, StringUtils.trimToEmpty(getStringVariableValue(runtimeContextVariableName)));
        return val;
    }

    private int resolveToInteger(String runtimeContextVariableName) throws Exception {
        String val = SpecialDirective.resolveStringValue(this, StringUtils.trimToEmpty(getStringVariableValue(runtimeContextVariableName)));
        return NumberUtils.toInt(val);
    }

    private boolean resolveToBoolean(String runtimeContextVariableName) throws Exception {
        String val = SpecialDirective.resolveStringValue(this, StringUtils.trimToEmpty(getStringVariableValue(runtimeContextVariableName)));
        return BooleanUtils.toBoolean(val);
    }

    private File resolveToFile(String runtimeContextVariableName) throws Exception {
        String val = SpecialDirective.resolveStringValue(this, StringUtils.trimToEmpty(getStringVariableValue(runtimeContextVariableName)));
        return new File(val).getCanonicalFile();
    }

    private String resolveAndSet(RuntimeContextVariable var) throws Exception {
        String name = var.getName();
        String currentValue = StringUtils.trimToEmpty(getStringVariableValue(name));
        getEngineLogger().fine("Runtime context variable [" + name + "] has current value [" + currentValue + "].");
        String newValue = resolveToString(name);
        getEngineLogger().fine("Runtime context variable [" + name + "] has resolved value [" + newValue + "].");
        if (!newValue.equals(currentValue)) {
            getEngineLogger().fine("Setting runtime context variable [" + name + "] to new resolved value [" + newValue + "].");
            var.setValue(newValue);
        }
        return newValue;
    }

    private void validateHttpFeature() throws Exception {
        httpFeatureInfo = (HttpFeatureInfo) ContainerUtils.getFeatureInfo(Feature.HTTP_FEATURE_NAME, this, getCurrentDomain());
        if (httpFeatureInfo != null) {
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
        } else {
            getEngineLogger().warning("HTTP Feature is disabled");
        }
    }

    Logger logger() {
        return getEngineLogger();
    }

    DockerClient dockerClient() {
        return this.dockerClient;
    }

    public String dockerContainerId() {
        return this.dockerContainerId;
    }

    public String dockerContainerName() {
        return this.dockerContainerTag;
    }

    public String dockerContainerInfo() {
        return "[" + dockerContainerName() + "][" + dockerContainerId() + "]";
    }

    // If ArchiveManagementFeatureInfo is supported, then ArchiveManagement methods must scripted
    // Otherwise it got intercepted here and UnsupportedOperationException is thrown.
    public void archiveDeploy(String archiveName, List<ArchiveLocator> archiveLocators, Properties properties) throws Exception {
        throw new UnsupportedOperationException("Method archiveDeploy(...) is not implemented in scripting or this Enabler. Please see interface '"
                + ArchiveManagement.class.getName() + "' in the SDK API.");
    }

    public ArchiveActivationInfo archiveStart(String archiveName, Properties properties) throws Exception {
        throw new UnsupportedOperationException("Method archiveStart(...) is not implemented in scripting or this Enabler. Please see interface '"
                + ArchiveManagement.class.getName() + "' in the SDK API.");
    }

    public ArchiveActivationInfo archiveScaleUp(String archiveName, List<ArchiveLocator> archiveLocators) throws Exception {
        throw new UnsupportedOperationException("Method archiveScaleUp(...) is not implemented in scripting or this Enabler. Please see interface '"
                + ArchiveManagement.class.getName() + "' in the SDK API.");
    }

    public void archiveScaleDown(String archiveName, String archiveId) throws Exception {
        throw new UnsupportedOperationException("Method archiveScaleDown(...) is not implemented in scripting or this Enabler. Please see interface '"
                + ArchiveManagement.class.getName() + "' in the SDK API.");

    }

    public void archiveStop(String archiveName, String archiveId, Properties properties) throws Exception {
        throw new UnsupportedOperationException("Method archiveStop(...) is not implemented in scripting or this Enabler. Please see interface '"
                + ArchiveManagement.class.getName() + "' in the SDK API.");
    }

    public void archiveUndeploy(String archiveName, Properties properties) throws Exception {
        throw new UnsupportedOperationException("Method archiveUndeploy(...) is not implemented in scripting or this Enabler. Please see interface '"
                + ArchiveManagement.class.getName() + "' in the SDK API.");
    }

    public ArchiveDetail[] archiveDetect() throws Exception {
        throw new UnsupportedOperationException("Method archiveDetect() is not implemented in scripting or this Enabler. Please see interface '"
                + ArchiveManagement.class.getName() + "' in the SDK API.");
    }

    public String[] urlDetect() throws Exception {
        throw new UnsupportedOperationException("Method urlDetect() is not implemented in scripting or this Enabler. Please see interface '" + ArchiveManagement.class.getName()
                + "' in the SDK API.");
    }

    // For Continuous Deployment
    public File getArchive(String archiveName) throws Exception {
        throw new UnsupportedOperationException("Method getArchive(...) is not implemented in scripting or this Enabler. Please see interface '" + ArchiveProvider.class.getName()
                + "' in the SDK API.");
    }

    @Override
    public File resolveArchive(String deploymentArchiveName, String url, Properties properties, String componentName, File destDir) throws Exception {
        throw new UnsupportedOperationException("Method resolveArchive(...) is not implemented in scripting or this Enabler. Please see abstract class '"
                + AbstractContainer.class.getName() + "' in the SDK API.");
    }

    // For ComponentNotification
    @Override
    public void componentNotification(String componentName, List<NotificationEngineInfo> notificationEngineInfo) {
        throw new UnsupportedOperationException("Method componentNotification(...) is not implemented in scripting or this Enabler. Please see abstract class '"
                + AbstractContainer.class.getName() + "' in the SDK API.");
    }

}
