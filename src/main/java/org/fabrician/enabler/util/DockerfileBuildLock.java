/*
 * Copyright (c) 2014 TIBCO Software Inc. All Rights Reserved.
 *
 * Use is subject to the terms of the TIBCO license terms accompanying the download of this code.
 * In most instances, the license terms are contained in a file named license.txt.
 */
package org.fabrician.enabler.util;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;

import com.datasynapse.fabric.util.ContainerUtils;
import com.datasynapse.gridserver.engine.EngineProperties;
import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Splitter;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.google.common.io.BaseEncoding;

/**
 * An utility file to ensure that concurrent Dockerfile build for same image tag don't step on each other.
 * <p>
 * In particular, if an image associated with a Dockerfile is already built, we don't want it to go through the process of building again.
 * </p>
 */
public class DockerfileBuildLock implements Closeable {
    private static Logger logger = ContainerUtils.getLogger(DockerfileBuildLock.class);
    private final String dockerImageName;
    private final File dockerFilePath;

    private File lock_file = null;
    private FileLock lock = null;
    private FileChannel lock_channel = null;

    private DockerfileBuildLock(String dockerImageName, File dockerFilePath) throws Exception {
        this.dockerImageName = dockerImageName;
        this.dockerFilePath = dockerFilePath;

        byte[] docker_bytes = FileUtils.readFileToByteArray(dockerFilePath);
        // we create a hash file name from the image and dockerfile content to build with...
        HashFunction hf = Hashing.md5();
        HashCode hc = hf.newHasher().putString(dockerImageName, Charsets.UTF_8).putBytes(docker_bytes).hash();
        String dockerFileHash = BaseEncoding.base64Url().encode(hc.asBytes());
        File tmpDir = new File(System.getProperty("java.io.tmpdir"));
        lock_file = new File(tmpDir, dockerFileHash + ".dockerfile_lck");
        logger.info("Attempt to acquire Dockerfile build lock at path :" + lock_file);
        lock_channel = FileUtils.openOutputStream(lock_file).getChannel();
        lock = lock_channel.tryLock();

        if (lock == null) {
            throw new Exception("Can't create exclusive build lock for image [" + dockerImageName + "] for Dockerfile [" + dockerFilePath + "]");
        } else {
            logger.info("Acquired Dockerfile build lock at lock path : [" + lock_file + "]");
        }
    }

    private void releaseLock() throws Throwable {
        logger.info("Attempt to release Dockerfile build lock at lock path : [" + lock_file + "]");
        if (lock != null) {
            if (lock.isValid()) {
                lock.release();
            }
        }
        if (lock_channel != null) {
            if (lock_channel.isOpen()) {
                lock_channel.close();
            }
        }
        if (lock_file != null) {
            if (lock_file.exists()) {
                lock_file.delete();
            }
        }
        logger.info("Released Dockerfile build lock at lock path : [" + lock_file + "]");
    }

    @Override
    protected void finalize() throws Throwable {
        this.release();
        super.finalize();
    }

    public void release() {
        try {
            releaseLock();
        } catch (Throwable ex) {
            logger.log(Level.SEVERE, "Fail to release build lock associated with image [" + dockerImageName + "] at lock path [ " + lock_file + "]", ex);
        }
    }

    public void close() throws IOException {
        release();
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this).omitNullValues().add("dockerImageName", dockerImageName).add("dockerFilePath", dockerFilePath).add("lock_file", lock_file).toString();

    }

    /**
     * Attempt to acquire a Docker file build lock
     * 
     * @param dockerImageName
     *            docker image name to create . ex. "joe/sshd:1.0.1"
     * @param dockerFilePath
     *            file path to the Dockerfile to use for build
     * @return Optional<DockerfileBuildLock>
     */
    public static Optional<DockerfileBuildLock> acquire(String dockerImageName, File dockerFilePath) {
        DockerfileBuildLock lck = null;
        try {
            lck = new DockerfileBuildLock(dockerImageName, dockerFilePath);
        } catch (Exception ex) {
            logger.log(Level.FINE, "Fail to acquire build lock associated with image [" + dockerImageName + "] for Dockerfile [" + dockerFilePath + "]", ex);
            
        }
        return Optional.fromNullable(lck);
    }

    /**
     * Attempt to acquire a Docker file build log with multiples tries
     * 
     * @param dockerImageName
     *            docker image name to create . ex. "joe/sshd:1.0.1"
     * @param dockerFilePath
     *            file path to the Dockerfile to use for build
     * @param maxRetries
     *            max number of retries to acquire lock
     * @param retryPause
     *            pause in secs between retries
     * @return Optional<DockerfileBuildLock>
     * @throws InterruptedException
     */
    public static Optional<DockerfileBuildLock> acquire(String dockerImageName, File dockerFilePath, int maxRetries, int retryPause) throws InterruptedException {
        for (int i = 0; i < maxRetries; i++) {
            Optional<DockerfileBuildLock> lock = DockerfileBuildLock.acquire(dockerImageName, dockerFilePath);
            if (lock.isPresent()) {
                return lock;
            }
            try {
                TimeUnit.SECONDS.sleep(retryPause);
            } catch (InterruptedException ex) {
                logger.warning("Retry pause interrupted.");
                throw ex;
            }
        }
        logger.severe("Failed to acquire build lock associated with image [" + dockerImageName + "] for Dockerfile [" + dockerFilePath + "]");
        return Optional.absent();
    }
}
