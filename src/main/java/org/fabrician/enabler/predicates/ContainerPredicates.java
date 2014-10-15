/*
 * Copyright (c) 2014 TIBCO Software Inc. All Rights Reserved.
 *
 * Use is subject to the terms of the TIBCO license terms accompanying the download of this code.
 * In most instances, the license terms are contained in a file named license.txt.
 */
package org.fabrician.enabler.predicates;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.jclouds.util.Predicates2.retry;

import org.jclouds.docker.domain.Container;
import org.jclouds.docker.domain.State;
import org.jclouds.docker.features.RemoteApi;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;

/**
 * Common Docker container instance predicates. Some tests to see if an instance has reached a specified status. This class is most useful when paired with a RetryablePredicate as in the code below.
 * This class can be used to block execution until the container has reached a desired state. This is useful when your container instance needs to be 100% ready before you can continue with
 * any execution.
 * 
 * <pre>
 * <code>
 * RemoteApi api=....
 * Container c = api.inspect("my_container");
 * 
 * RetryablePredicate<Container> awaitState = ContainerPredicates.awaitState(api,State.builder().running(false).build(), 600, 10);
 * 
 * if (!awaitState.apply(c)) {
 *    throw new TimeoutException("Timeout on container instance: " + c); 
 * }    
 * </code>
 * </pre>
 * 
 * You can also use the static convenience methods as so.
 * 
 * <pre>
 * <code>
 * RemoteApi api=....
 * Container c = api.inspect("my_container");
 * 
 * if (!ContainerPredicates.awaitRunning(api).apply(c) {
 *    throw new TimeoutException("Timeout on container instance : " + c);     
 * }
 * </code>
 * </pre>
 */
public class ContainerPredicates {
    public static final long DEFAULT_TIMEOUT = 300; // 5mins
    public static final long DEFAULT_POLL_PERIOD = 10; // 10secs
    public static State RUNNING= running();
    public static State STOPPED= stopped();
    private static State running(){
        return State.builder()//
                .running(true)//
                .exitCode(0)//
                .finishedAt("")//
                .startedAt("")//
                .ghost(false)//
                .pid(-1)//
                .build();
    }
    private static State stopped(){
        return State.builder()//
                .running(false)//
                .exitCode(0)//
                .finishedAt("")//
                .startedAt("")//
                .ghost(false)//
                .pid(-1)//
                .build();
    }
    /**
     * Wait until a Docker Container instance is in RUNNING state.
     * 
     * @param api
     *            the remote Api associated with Docker host where the container instance resides
     * @return RetryablePredicate That will check the status every 10 seconds for a maxiumum of 5 minutes.
     */
    public static Predicate<Container> awaitRunning(RemoteApi api) {
        StateUpdatedPredicate statePredicate = new StateUpdatedPredicate(api,RUNNING);
        return retry(statePredicate, DEFAULT_TIMEOUT, DEFAULT_POLL_PERIOD, DEFAULT_POLL_PERIOD, SECONDS);
    }

    /**
     * Wait until a container instance is in non-running state.
     * 
     * @param api
     *            the remote Api associated with Docker host where the container instance resides
     * @return RetryablePredicate That will check the status every 10 seconds for a maxiumum of 5 minutes.
     */
    public static Predicate<Container> awaitStopped(RemoteApi api) {
        StateUpdatedPredicate statusPredicate = new StateUpdatedPredicate(api, STOPPED);
        return retry(statusPredicate, DEFAULT_TIMEOUT, DEFAULT_POLL_PERIOD, DEFAULT_POLL_PERIOD, SECONDS);
    }

    /**
     * Wait until a container instance no longer exists.
     * 
     * @param api
     *            the remote Api associated with Docker host where the container instance resides
     * @return RetryablePredicate That will check the whether the server exists every 10 seconds for a maxiumum of 5 minutes.
     */
    public static Predicate<Container> awaitRemoved(RemoteApi api) {
        RemovedPredicate deletedPredicate = new RemovedPredicate(api);
        return retry(deletedPredicate, DEFAULT_TIMEOUT, DEFAULT_POLL_PERIOD, DEFAULT_POLL_PERIOD, SECONDS);
    }

    /**
     * Wait until a container instance reaches a specified state
     * 
     * @param api
     *             the remote Api associated with Docker host where the container instance resides
     * @param state
     *            the container status to wait for
     * @param maxWaitInSec
     *            maximum time in seconds to wait for the state
     * @param periodInSec
     *            polling period in seconds to check for container state
     * @return RetryablePredicate That will check the whether the container reaches the specified state every periodInSec for a maximum of maxWaitInSec
     */
    public static Predicate<Container> awaitState(RemoteApi api, State state, long maxWaitInSec, long periodInSec) {
        StateUpdatedPredicate statusPredicate = new StateUpdatedPredicate(api, state);
        return retry(statusPredicate, maxWaitInSec, periodInSec, periodInSec, SECONDS);
    }

    private static class StateUpdatedPredicate implements Predicate<Container> {
        private final RemoteApi api;
        private final State state;

        public StateUpdatedPredicate(RemoteApi api, State state) {
            this.api = checkNotNull(api, "api must be defined");
            this.state = checkNotNull(state, "state must be defined");
        }

        /**
         * @return boolean Return true when the container instance reaches specified state, false otherwise
         */
        @Override
        public boolean apply(Container c) {
            checkNotNull(c, "container c instance must be defined");

            if (state.isRunning() == c.getState().isRunning()) {
                return true;
            } else {
                Container containerUpdated = api.inspectContainer(c.getName());
                checkNotNull(containerUpdated, "Container instance %s not found.",c.getName());
                return state.isRunning()==c.getState().isRunning();
            }
        }
    }

    private static class RemovedPredicate implements Predicate<Container> {
        private final RemoteApi api;

        public RemovedPredicate(RemoteApi api) {
            this.api = checkNotNull(api, "api must be defined");
        }

        /**
         * @return boolean Return true when the container is removed, false otherwise
         */
        @Override
        public boolean apply(Container c) {
            checkNotNull(c, "container instance must be defined");
            return api.inspectContainer(c.getId())==null;
        }
    }

    /**
     * Returns a predicate for filtering-in Container instances created with an Image id.
     * 
     * @param imageId
     *            id of the image of the container instance
     */
    public static Predicate<Container> imageId(final String imageId) {
        Predicate<Container> p = new Predicate<Container>() {

            @Override
            public boolean apply(Container c) {
                return c.getImage().equals(imageId);
            }

        };
        return p;
    }


    /**
     * Returns a predicate for filtering-in container instances in "running" state. 
     * @param state
     *            the state of container
     */
    public static Predicate<Container> running (final State state) {
        Predicate<Container> p = new Predicate<Container>() {

            @Override
            public boolean apply(Container c) {
                return c.getState().isRunning() == state.isRunning();
            }

        };
        return p;
    }

    /**
     * Returns a predicate for filtering-in container instances NOT in "running" state. 
     * @param state
     *            the state of container
     */
    public static Predicate<Container> notRunning(final State state) {
        return Predicates.not(running(state));
    }


    /**
     * Returns a predicate for filtering-in container instances having a certain name.
     * 
     * @param name
     *            the name of user defined metadata use to tag the container instance
     */
    public static Predicate<Container> name(final String name) {
        Predicate<Container> p = new Predicate<Container>() {

            @Override
            public boolean apply(Container c) {
                return c.getName().equals(name);
            }

        };
        return p;
    }

   

    private ContainerPredicates() {}
}
