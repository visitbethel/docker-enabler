/*
 * Copyright (c) 2014 TIBCO Software Inc. All Rights Reserved.
 *
 * Use is subject to the terms of the TIBCO license terms accompanying the download of this code.
 * In most instances, the license terms are contained in a file named license.txt.
 */
package org.fabrician.enabler;

import static com.google.common.util.concurrent.MoreExecutors.sameThreadExecutor;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;

import org.fabrician.enabler.predicates.ContainerPredicates;
import org.jclouds.Constants;
import org.jclouds.ContextBuilder;
import org.jclouds.concurrent.config.ExecutorServiceModule;
import org.jclouds.docker.DockerApi;
import org.jclouds.docker.domain.Config;
import org.jclouds.docker.domain.Container;
import org.jclouds.docker.domain.HostConfig;
import org.jclouds.docker.domain.Image;
import org.jclouds.docker.features.RemoteApi;
import org.jclouds.docker.options.BuildOptions;
import org.jclouds.docker.options.ListContainerOptions;
import org.jclouds.docker.options.RemoveContainerOptions;

import com.datasynapse.commons.util.StringUtils;
import com.datasynapse.fabric.util.ContainerUtils;
import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.io.CharStreams;
import com.google.common.io.Closeables;
import com.google.inject.Module;

/**
 * A simplified helper client around JCloud Docker.
 * 
 */
public class DockerClient implements Closeable {

    public static final int DEFAULT_REMOTE_PORT = 2375;
    public static final String DEFAULT_REMOTE_HOST = "127.0.0.1";
    public static final String PROVIDER_NAME = "docker";
    private final String dockerHostname;
    private final Integer dockerPort;
    private DockerApi dockerApi = null;
    private final Set<Module> modules = ImmutableSet.<Module> of(new ExecutorServiceModule(sameThreadExecutor(), sameThreadExecutor()));
    private Logger logger = ContainerUtils.getLogger(this);

    public static DockerClient getInstance() {
        DockerClient doc = new DockerClient(null, null);
        doc.init();
        return doc;
    }

    public static DockerClient getInstance(final String dockerHostname, final Integer dockerPort) {
        DockerClient doc = new DockerClient(dockerHostname, dockerPort);
        doc.init();
        return doc;
    }

    private DockerClient(String dockerHostname, Integer dockerPort) {
        if (StringUtils.isEmptyOrBlank(dockerHostname)) {
            dockerHostname = DEFAULT_REMOTE_HOST;
        }
        if (dockerPort == null) {
            dockerPort = DEFAULT_REMOTE_PORT;
        }
        this.dockerHostname = dockerHostname;
        this.dockerPort = dockerPort;
    }

    private void init() {
        this.dockerApi = getApi("http://" + this.dockerHostname + ":" + this.dockerPort);
    }

    RemoteApi remoteApi() {
        return this.dockerApi.getRemoteApi();
    }

    private DockerApi getApi(final String url) {
        return ContextBuilder.newBuilder(PROVIDER_NAME)//
                .credentials("clientid", "apikey")//
                .endpoint(url)//
                .modules(modules)//
                .overrides(setupOverides())//
                .buildApi(DockerApi.class);
    }

    private Properties setupOverides() {
        Properties props = new Properties();
        props.setProperty(Constants.PROPERTY_TRUST_ALL_CERTS, "true"); // accepts all HTTPS certs
        props.setProperty(Constants.PROPERTY_RELAX_HOSTNAME, "true"); // don't validate host name
        props.setProperty(Constants.PROPERTY_MAX_RETRIES, "15"); // how times to retry a command
        return props;
    }

    public Optional<Image> buildImagePortable(File dockerFile, BuildOptions options) {
        InputStream is = null;
        try {
            is = remoteApi().build(dockerFile, options);
            String msg = readStream("while building image from Dockerfile @ [" + dockerFile + "]", is);
            logger.info(msg);
            String tag = getFirstValue(options.buildQueryParameters(), "tag").or("N/A");
            return inspectImage(tag);
        } catch (Exception ex) {
            Closeables.closeQuietly(is);
        }
        return Optional.absent();
    }

    public Optional<Image> buildImage(File buildContextDir, BuildOptions options) {
        //TODO: we need to build a tar payload from the Docker build context
        return null;
    }

    /**
     * Inspect an image residing on the Docker host
     * 
     * @param imageIdentity
     *            the identity of image. This can be id("f0fe5d88bf998abaf4bd61a8d9ba0d637cc9a5dc33183c9de783e6782aa68e36") or tag. ex. "ubuntu:precise","jamtur01/nginx:latest"
     * @return Optional<Image>
     */
    public Optional<Image> inspectImage(String imageIdentity) {
        try {
            return Optional.fromNullable(remoteApi().inspectImage(imageIdentity));
        } catch (Exception ex) {
            logger.info(ex.getMessage());
        }
        return Optional.absent();
    }

    /**
     * Check if an image exists
     * 
     * @param imageIdentity
     *            the identity of image. This can be id("f0fe5d88bf998abaf4bd61a8d9ba0d637cc9a5dc33183c9de783e6782aa68e36") or tag. ex. "ubuntu:precise","jamtur01/nginx:latest"
     * @return true if exists;false otherwise
     */
    public boolean isImageExist(String imageIdentity) {
        return inspectImage(imageIdentity).isPresent();
    }

    /**
     * Search for an image cached on Docker host
     * 
     * @param id
     *            id of the image. ex. "822a01ae9a156790e516b034972c54f038bddf55437f562060420716dafe72fa"
     * @return an Image
     */
    public Optional<Image> searchImageById(String id) {
        for (Image img : listAllImages()) {
            if (id.equals(img.getId())) {
                return Optional.of(img);
            }
        }
        return Optional.absent();
    }

    /**
     * Search for an image cached on the Docker host
     * 
     * @param tag
     *            repo tag. ex "ubuntu:precise","jamtur01/nginx:latest"
     * @return an Image
     */
    public Optional<Image> searchImageByTag(String tag) {
        for (Image img : listAllImages()) {
            for (String t : img.getRepoTags()) {
                if (tag.equals(t)) {
                    return Optional.of(img);
                }
            }
        }
        return Optional.absent();
    }

    /**
     * List all images cached on the Docker host
     * 
     * @return a set of Images
     */
    public Set<Image> listAllImages() {
        Set<Image> images = remoteApi().listImages();
        return images;
    }

    /**
     * Delete an image using its tag
     * 
     * @param imageTag
     *            tag of an image. ex "ubuntu:precise","jamtur01/nginx:latest"
     * @return true if delete successful;false otherwise
     */
    public boolean deleteImageByTag(String imageTag) {
        Optional<Image> img = searchImageByTag(imageTag);
        if (img.isPresent()) {
            List<String> tags = img.get().getRepoTags();
            String imageId = img.get().getId();
            // need to delete all repo tags associated with an image before it can be deleted totally; the last one will remove the image totally.
            for (String t : tags) {
                InputStream is = remoteApi().deleteImage(t);
                String msg = readStream("while deleting image [" + imageId + "] tag by [" + t + "]", is);
                logger.info(msg);
            }
        }
        // validate deletion is successful
        img = searchImageByTag(imageTag);
        return !img.isPresent();
    }

    /**
     * Delete an image using its id
     * 
     * @param imageId
     *            id of image. ex. "822a01ae9a156790e516b034972c54f038bddf55437f562060420716dafe72fa"
     * @return true if delete is successful; false otherwise
     */
    public boolean deleteImageById(String imageId) {
        Optional<Image> img = searchImageById(imageId);
        if (img.isPresent()) {
            List<String> tags = img.get().getRepoTags();
            // need to delete all repo tags associated with an image before it can be deleted totally; the last one will remove the image totally.
            for (String t : tags) {
                InputStream is = remoteApi().deleteImage(t);
                String msg = readStream("while deleting image [" + imageId + "] tag by [" + t + "]", is);
                logger.info(msg);
            }
        }

        // validate deletion is successful
        img = searchImageById(imageId);
        return !img.isPresent();
    }

    /**
     * Create a container on the Docker host. Note: A created container is not running!
     * 
     * @param containerName
     *            the name to give the container. ex. "my-container1".
     * @param config
     *            the configuration of container to create
     * @return a Optional<Container> object
     */
    public Optional<Container> createContainer(String containerName, Config config) {
        try {
            Container c = remoteApi().createContainer(containerName, config);
            return Optional.of(c);
        } catch (Exception ex) {
            logger.severe("Docker container [" + containerName + "] failed :" + ex.getMessage());
        }
        return Optional.absent();
    }

    /**
     * Start a created container on the Docker host.
     * 
     * @param containerIdentity
     *            the identity of the container ex. id("be7dcbf09d17c6605fdde7d66846dd98e3ba06579d4c9ff1cf8fa682be90db2e") or name("my_container1")
     * @param hostConfig
     *            the host binding parameters when container is started
     * @return true if container is starting or already running. User must poll to see if container reach state "running==true".
     */
    public boolean startContainer(String containerIdentity, HostConfig hostConfig) {
        if (isContainerRunning(containerIdentity)) {
            return true;
        }
        try {
            if (hostConfig == null) {
                remoteApi().startContainer(containerIdentity);
            } else {
                remoteApi().startContainer(containerIdentity, hostConfig);
            }
        } catch (Exception ex) {
            logger.severe("while starting  Docker container [" + containerIdentity + "] :" + ex.getMessage());
        }
        return true;
    }

    /**
     * Stop a running container on the Docker host.
     * 
     * @param containerIdentity
     *            the identity of the container ex. id("be7dcbf09d17c6605fdde7d66846dd98e3ba06579d4c9ff1cf8fa682be90db2e") or name("my_container1")
     * @return true if container is stopping or already stopped. User must poll to see if container reach state "running==false"
     */
    public boolean stopContainer(String containerIdentity) {
        if (isContainerStopped(containerIdentity)) {
            return true;
        }
        try {
            remoteApi().stopContainer(containerIdentity);
            Optional<Container> c = inspectContainer(containerIdentity);
            if (c.isPresent()) {
                ContainerPredicates.awaitStopped(remoteApi()).apply(c.get());
            }
        } catch (Exception ex) {
            logger.severe("while stopping  Docker container [" + containerIdentity + "] :" + ex.getMessage());
        }
        return true;
    }

    /**
     * Remove a container from the Docker host
     * 
     * @param containerIdentity
     *            the identity of the container ex. id("be7dcbf09d17c6605fdde7d66846dd98e3ba06579d4c9ff1cf8fa682be90db2e" or name("my_container1")
     * @param options
     *            removal options
     * @return true if container is being removed or already removed
     */
    public boolean removeContainer(String containerIdentity, RemoveContainerOptions options) {
        if (!isContainerExist(containerIdentity)) {
            return true;
        }
        if (stopContainer(containerIdentity)) {
            try {
                if (options == null) {
                    remoteApi().removeContainer(containerIdentity);
                } else {
                    remoteApi().removeContainer(containerIdentity, options);
                }
            } catch (Exception ex) {
                logger.severe("while removing  Docker container [" + containerIdentity + "] :" + ex.getMessage());
            }
        }
        return true;
    }

    /**
     * Search a container by id on the Docker host
     * 
     * @param containerIdentity
     *            identity of a container. ex. id("be7dcbf09d17c6605fdde7d66846dd98e3ba06579d4c9ff1cf8fa682be90db2e", name("my_container1")
     * 
     * @param runningOnly
     *            true if only running containers should be checked;false otherwise
     * @return a Container
     */
    public Optional<Container> searchContainer(String containerIdentity, boolean runningOnly) {
        Optional<Container> c = inspectContainer(containerIdentity);
        if (!c.isPresent()) {
            return Optional.absent();
        }
        if (runningOnly) {
            if (c.get().getState().isRunning()) {
                return c;
            } else {
                return Optional.absent();
            }
        } else {
            return c;
        }
    }

    /**
     * List only running containers on the Docker host
     * 
     * @return a set of running containers
     */
    public Set<Container> listRunningContainers() {
        Set<Container> conts = remoteApi().listContainers();
        return conts;
    }

    /**
     * List all containers on the Docker host
     * 
     * @return
     */
    public Set<Container> listAllContainers() {
        Set<Container> conts = remoteApi().listContainers(ListContainerOptions.Builder.all(true));
        return conts;
    }

    /**
     * Inspect a container
     * 
     * @param containerIdentity
     *            the identity of container to inspect. This can be id("3f35f4a383b8c3b848ec775f91d7ac6e69c1f6dd234624768f98af25b375ee7a") or name("my_container1")
     * @return Optional<Container>
     */
    public Optional<Container> inspectContainer(String containerIdentity) {
        try {
            return Optional.fromNullable(remoteApi().inspectContainer(containerIdentity));
        } catch (Exception ex) {
            logger.info(ex.getMessage());
        }
        return Optional.absent();
    }

    /**
     * Check if a container exist
     * 
     * @param containerIdentity
     *            the identity of container to inspect. This can be id("3f35f4a383b8c3b848ec775f91d7ac6e69c1f6dd234624768f98af25b375ee7a") or name("my_container1")
     * @return true if exists; false otherwise
     */
    public boolean isContainerExist(String containerIdentity) {
        return inspectContainer(containerIdentity).isPresent();
    }

    public boolean isContainerRunning(String containerIdentity) {
        Optional<Container> c = inspectContainer(containerIdentity);
        if (!c.isPresent()) {
            return false;
        }
        return c.get().getState().isRunning();
    }

    public boolean isContainerStopped(String containerIdentity) {
        return !isContainerRunning(containerIdentity);
    }

    @Override
    public void close() {
        try {
            Closeables.close(dockerApi, true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String readStream(String context, InputStream is) {
        String result = "";
        try {
            result = CharStreams.toString(new InputStreamReader(is, Charsets.UTF_8));
        } catch (IOException ex) {
            throw new RuntimeException(context, ex);
        } finally {
            Closeables.closeQuietly(is);
        }
        return result;
    }

    private Optional<String> getFirstValue(Multimap<String, String> map, String key) {
        Collection<String> values = map.get(key);
        return Optional.fromNullable((values != null && values.size() >= 1) ? values.iterator().next() : null);
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        DockerClient client = null;
        try {
            String c_id = "3f35f4a383b8c3b848ec775f91d7ac6e69c1f6dd234624768f98af25b375ee7a";
            String c_name = "my_archiva1";
            String img_id = "f0fe5d88bf998abaf4bd61a8d9ba0d637cc9a5dc33183c9de783e6782aa68e36";
            String img_name = "blau/archiva211:latest";
            client = DockerClient.getInstance("bklau-hp", 2375);
            Optional<Container> c = client.inspectContainer("MyDocker1");
            if (c.isPresent()) {
                System.out.println(c.get());
                client.removeContainer(c.get().getId(),null);
            }
            // client.createContainer(c_name, Config.builder().imageId(img_name).build());
            // System.out.println("Start by c_id :" + client.startContainer(c_name,null));
            // Thread.sleep(30000);
            // System.out.println(client.inspectContainer(c_name));
            // System.out.println("Stop by c_id :" + client.stopContainer(c_name));
            // Thread.sleep(30000);
            // System.out.println(client.inspectContainer(c_name));
            // System.out.println("Remove by c_id :" + client.removeContainer(c_name,null));
            // Thread.sleep(30000);
            // //System.out.println("Cont removed :" + client.isContainerExist(c_name));
            // System.out.println("Cont removed :" + !client.isContainerExist(c_name));
            // System.out.println("Found :" + client.searchContainer(c_name, false));
            // System.out.println("Found :" + client.searchContainer(c_name, true));

        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            client.close();
        }
    }

}
