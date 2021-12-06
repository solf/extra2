/**
 * Copyright Sergey Olefir
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.solf.extra2.concurrent.retry;

import java.util.List;
import org.eclipse.jdt.annotation.NonNullByDefault;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import io.github.solf.extra2.codegenerate.stepbuilder.unused.UnusedInterface;

/**
 *  Step Builder class for {@link RRLStatus}
 * <p>
 * {@link RetryAndRateLimitService} status (for e.g. monitoring).
 *
 *  @author Sergey Olefir
 */
@NonNullByDefault
@SuppressWarnings("unused")
public class RRLStatusBuilder {

    public interface ZBSI_RRLStatusBuilder_statusCreatedAt_builder {

        public RRLStatus buildRRLStatus();
    }

    public interface ZBSI_RRLStatusBuilder_statusCreatedAt_arg14 {

        /**
         * (Configuration) Grace period that allows requests to be processed this much earlier than intended.
         */
        public ZBSI_RRLStatusBuilder_statusCreatedAt_builder configRequestEarlyProcessingGracePeriod(long configRequestEarlyProcessingGracePeriod);
    }

    public interface ZBSI_RRLStatusBuilder_statusCreatedAt_arg13 {

        /**
         * (Configuration) The maximum number of pending/in-flight requests that the service is allowed to handle.
         */
        public ZBSI_RRLStatusBuilder_statusCreatedAt_arg14 configMaxPendingRequests(int configMaxPendingRequests);
    }

    public interface ZBSI_RRLStatusBuilder_statusCreatedAt_arg12 {

        /**
         * (Configuration) A list of delays applied after each subsequent failure in order.
         */
        public ZBSI_RRLStatusBuilder_statusCreatedAt_arg13 configDelaysAfterFailure(List<Long> configDelaysAfterFailure);
    }

    public interface ZBSI_RRLStatusBuilder_statusCreatedAt_arg11 {

        /**
         * (Configuration) max attempts per request.
         */
        public ZBSI_RRLStatusBuilder_statusCreatedAt_arg12 configMaxAttempts(int configMaxAttempts);
    }

    public interface ZBSI_RRLStatusBuilder_statusCreatedAt_arg10 {

        /**
         * Main processing queue size.
         */
        public ZBSI_RRLStatusBuilder_statusCreatedAt_arg11 mainQueueSize(int mainQueueSize);
    }

    public interface ZBSI_RRLStatusBuilder_statusCreatedAt_arg9 {

        /**
         * Count of the requests currently being processed.
         */
        public ZBSI_RRLStatusBuilder_statusCreatedAt_arg10 currentProcessingRequestsCount(int currentProcessingRequestsCount);
    }

    public interface ZBSI_RRLStatusBuilder_statusCreatedAt_arg8 {

        /**
         * Whether service itself AND all the threads & thread pools required for the
         * service operation are still alive.
         */
        public ZBSI_RRLStatusBuilder_statusCreatedAt_arg9 everythingAlive(boolean everythingAlive);
    }

    public interface ZBSI_RRLStatusBuilder_statusCreatedAt_arg7 {

        /**
         * Number of currently active threads in the pool.
         */
        public ZBSI_RRLStatusBuilder_statusCreatedAt_arg8 requestsExecutorServiceActiveThreads(int requestsExecutorServiceActiveThreads);
    }

    public interface ZBSI_RRLStatusBuilder_statusCreatedAt_arg6 {

        /**
         * Whether requests executor service is alive.
         */
        public ZBSI_RRLStatusBuilder_statusCreatedAt_arg7 requestsExecutorServiceAlive(boolean requestsExecutorServiceAlive);
    }

    public interface ZBSI_RRLStatusBuilder_statusCreatedAt_arg5 {

        /**
         * Whether all delay queue processing threads are alive.
         */
        public ZBSI_RRLStatusBuilder_statusCreatedAt_arg6 delayQueueProcessingThreadsAreAlive(boolean delayQueueProcessingThreadsAreAlive);
    }

    public interface ZBSI_RRLStatusBuilder_statusCreatedAt_arg4 {

        /**
         * Whether thread is alive.
         */
        public ZBSI_RRLStatusBuilder_statusCreatedAt_arg5 mainQueueProcessingThreadAlive(boolean mainQueueProcessingThreadAlive);
    }

    public interface ZBSI_RRLStatusBuilder_statusCreatedAt_arg3 {

        /**
         * Whether service is usable -- that is standard submit operations
         * can be performed; this can differ from {@link #isServiceAlive()} value
         * for various reasons such as service shutdown in progress.
         */
        public ZBSI_RRLStatusBuilder_statusCreatedAt_arg4 serviceUsable(boolean serviceUsable);
    }

    public interface ZBSI_RRLStatusBuilder_statusCreatedAt_arg2 {

        /**
         * Whether service is alive (that is it was started and not stopped yet).
         */
        public ZBSI_RRLStatusBuilder_statusCreatedAt_arg3 serviceAlive(boolean serviceAlive);
    }

    public interface ZBSI_RRLStatusBuilder_statusCreatedAt_arg1 {

        /**
         * Indicates when status was created (e.g. for caching purposes).
         */
        public ZBSI_RRLStatusBuilder_statusCreatedAt_arg2 statusCreatedAt(long statusCreatedAt);
    }

    private static final class ZBSI_RRLStatusBuilder_statusCreatedAt_builderClass implements ZBSI_RRLStatusBuilder_statusCreatedAt_builder, ZBSI_RRLStatusBuilder_statusCreatedAt_arg14, ZBSI_RRLStatusBuilder_statusCreatedAt_arg13, ZBSI_RRLStatusBuilder_statusCreatedAt_arg12, ZBSI_RRLStatusBuilder_statusCreatedAt_arg11, ZBSI_RRLStatusBuilder_statusCreatedAt_arg10, ZBSI_RRLStatusBuilder_statusCreatedAt_arg9, ZBSI_RRLStatusBuilder_statusCreatedAt_arg8, ZBSI_RRLStatusBuilder_statusCreatedAt_arg7, ZBSI_RRLStatusBuilder_statusCreatedAt_arg6, ZBSI_RRLStatusBuilder_statusCreatedAt_arg5, ZBSI_RRLStatusBuilder_statusCreatedAt_arg4, ZBSI_RRLStatusBuilder_statusCreatedAt_arg3, ZBSI_RRLStatusBuilder_statusCreatedAt_arg2, ZBSI_RRLStatusBuilder_statusCreatedAt_arg1 {

        @SuppressWarnings("all")
        private long configRequestEarlyProcessingGracePeriod;

        @SuppressWarnings("all")
        private int configMaxPendingRequests;

        @SuppressWarnings("all")
        private List<Long> configDelaysAfterFailure;

        @SuppressWarnings("all")
        private int configMaxAttempts;

        @SuppressWarnings("all")
        private int mainQueueSize;

        @SuppressWarnings("all")
        private int currentProcessingRequestsCount;

        @SuppressWarnings("all")
        private boolean everythingAlive;

        @SuppressWarnings("all")
        private int requestsExecutorServiceActiveThreads;

        @SuppressWarnings("all")
        private boolean requestsExecutorServiceAlive;

        @SuppressWarnings("all")
        private boolean delayQueueProcessingThreadsAreAlive;

        @SuppressWarnings("all")
        private boolean mainQueueProcessingThreadAlive;

        @SuppressWarnings("all")
        private boolean serviceUsable;

        @SuppressWarnings("all")
        private boolean serviceAlive;

        @SuppressWarnings("all")
        private long statusCreatedAt;

        /**
         * (Configuration) Grace period that allows requests to be processed this much earlier than intended.
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_RRLStatusBuilder_statusCreatedAt_builder configRequestEarlyProcessingGracePeriod(long configRequestEarlyProcessingGracePeriod) {
            this.configRequestEarlyProcessingGracePeriod = configRequestEarlyProcessingGracePeriod;
            return this;
        }

        /**
         * (Configuration) The maximum number of pending/in-flight requests that the service is allowed to handle.
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_RRLStatusBuilder_statusCreatedAt_arg14 configMaxPendingRequests(int configMaxPendingRequests) {
            this.configMaxPendingRequests = configMaxPendingRequests;
            return this;
        }

        /**
         * (Configuration) A list of delays applied after each subsequent failure in order.
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_RRLStatusBuilder_statusCreatedAt_arg13 configDelaysAfterFailure(List<Long> configDelaysAfterFailure) {
            this.configDelaysAfterFailure = configDelaysAfterFailure;
            return this;
        }

        /**
         * (Configuration) max attempts per request.
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_RRLStatusBuilder_statusCreatedAt_arg12 configMaxAttempts(int configMaxAttempts) {
            this.configMaxAttempts = configMaxAttempts;
            return this;
        }

        /**
         * Main processing queue size.
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_RRLStatusBuilder_statusCreatedAt_arg11 mainQueueSize(int mainQueueSize) {
            this.mainQueueSize = mainQueueSize;
            return this;
        }

        /**
         * Count of the requests currently being processed.
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_RRLStatusBuilder_statusCreatedAt_arg10 currentProcessingRequestsCount(int currentProcessingRequestsCount) {
            this.currentProcessingRequestsCount = currentProcessingRequestsCount;
            return this;
        }

        /**
         * Whether service itself AND all the threads & thread pools required for the
         * service operation are still alive.
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_RRLStatusBuilder_statusCreatedAt_arg9 everythingAlive(boolean everythingAlive) {
            this.everythingAlive = everythingAlive;
            return this;
        }

        /**
         * Number of currently active threads in the pool.
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_RRLStatusBuilder_statusCreatedAt_arg8 requestsExecutorServiceActiveThreads(int requestsExecutorServiceActiveThreads) {
            this.requestsExecutorServiceActiveThreads = requestsExecutorServiceActiveThreads;
            return this;
        }

        /**
         * Whether requests executor service is alive.
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_RRLStatusBuilder_statusCreatedAt_arg7 requestsExecutorServiceAlive(boolean requestsExecutorServiceAlive) {
            this.requestsExecutorServiceAlive = requestsExecutorServiceAlive;
            return this;
        }

        /**
         * Whether all delay queue processing threads are alive.
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_RRLStatusBuilder_statusCreatedAt_arg6 delayQueueProcessingThreadsAreAlive(boolean delayQueueProcessingThreadsAreAlive) {
            this.delayQueueProcessingThreadsAreAlive = delayQueueProcessingThreadsAreAlive;
            return this;
        }

        /**
         * Whether thread is alive.
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_RRLStatusBuilder_statusCreatedAt_arg5 mainQueueProcessingThreadAlive(boolean mainQueueProcessingThreadAlive) {
            this.mainQueueProcessingThreadAlive = mainQueueProcessingThreadAlive;
            return this;
        }

        /**
         * Whether service is usable -- that is standard submit operations
         * can be performed; this can differ from {@link #isServiceAlive()} value
         * for various reasons such as service shutdown in progress.
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_RRLStatusBuilder_statusCreatedAt_arg4 serviceUsable(boolean serviceUsable) {
            this.serviceUsable = serviceUsable;
            return this;
        }

        /**
         * Whether service is alive (that is it was started and not stopped yet).
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_RRLStatusBuilder_statusCreatedAt_arg3 serviceAlive(boolean serviceAlive) {
            this.serviceAlive = serviceAlive;
            return this;
        }

        /**
         * Indicates when status was created (e.g. for caching purposes).
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_RRLStatusBuilder_statusCreatedAt_arg2 statusCreatedAt(long statusCreatedAt) {
            this.statusCreatedAt = statusCreatedAt;
            return this;
        }

        @Override
        public RRLStatus buildRRLStatus() {
            return new RRLStatus(statusCreatedAt, serviceAlive, serviceUsable, mainQueueProcessingThreadAlive, delayQueueProcessingThreadsAreAlive, requestsExecutorServiceAlive, requestsExecutorServiceActiveThreads, everythingAlive, currentProcessingRequestsCount, mainQueueSize, configMaxAttempts, configDelaysAfterFailure, configMaxPendingRequests, configRequestEarlyProcessingGracePeriod);
        }
    }

    /**
     *  FIELD COMMENT: Indicates when status was created (e.g. for caching purposes).
     * <p>
     * CONSTRUCTOR COMMENT: Indicates when status was created (e.g. for caching purposes).
     */
    public static ZBSI_RRLStatusBuilder_statusCreatedAt_arg2 statusCreatedAt(long statusCreatedAt) {
        return new ZBSI_RRLStatusBuilder_statusCreatedAt_builderClass().statusCreatedAt(statusCreatedAt);
    }
}
