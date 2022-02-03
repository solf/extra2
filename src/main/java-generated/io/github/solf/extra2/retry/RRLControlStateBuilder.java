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
package io.github.solf.extra2.retry;

import javax.annotation.Nullable;
import org.eclipse.jdt.annotation.NonNullByDefault;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.With;
import io.github.solf.extra2.codegenerate.stepbuilder.unused.UnusedInterface;

/**
 *  Step Builder class for {@link RRLControlState}
 * <p>
 * Controls the running state of {@link RetryAndRateLimitService}
 *
 *  @author Sergey Olefir
 */
@NonNullByDefault
@SuppressWarnings("unused")
public class RRLControlStateBuilder {

    public interface ZBSI_RRLControlStateBuilder_description_builder {

        public RRLControlState buildRRLControlState();
    }

    public interface ZBSI_RRLControlStateBuilder_description_arg9 {

        /**
         * Specifies whether to wait for tickets:
         * <p>
         * true -- wait for tickets normally (potentially indefinitely, depending
         * 		on {@link #isLimitWaitingForTicket()})
         * <p>
         * false -- do not wait for tickets at all, obtain ticket if immediately
         * 		available, otherwise fail
         * <p>
         * NULL -- ignore tickets completely, assume they are available and proceed
         * 		with processing
         */
        public ZBSI_RRLControlStateBuilder_description_builder waitForTickets(@Nullable Boolean waitForTickets);
    }

    public interface ZBSI_RRLControlStateBuilder_description_arg8 {

        /**
         * If true AND {@link #getSpooldownTargetTimestamp()} is positive, then
         * waiting for ticket is limited by however many
         * requests are still to be processed (are in main queue) and the spooldown
         * target timestamp.
         */
        public ZBSI_RRLControlStateBuilder_description_arg9 limitWaitingForTicket(boolean limitWaitingForTicket);
    }

    public interface ZBSI_RRLControlStateBuilder_description_arg7 {

        /**
         * If true AND {@link #getSpooldownTargetTimestamp()} is positive, then
         * waiting for processing thread is limited by however many
         * requests are still to be processed (are in main queue) and the spooldown
         * target timestamp.
         */
        public ZBSI_RRLControlStateBuilder_description_arg8 limitWaitingForProcessingThread(boolean limitWaitingForProcessingThread);
    }

    public interface ZBSI_RRLControlStateBuilder_description_arg6 {

        /**
         * Timestamp by which service should attempt to spooldown (complete
         * processing of all the pending requests).
         * <p>
         * Non-positive value means there's no target for spooldown.
         */
        public ZBSI_RRLControlStateBuilder_description_arg7 spooldownTargetTimestamp(long spooldownTargetTimestamp);
    }

    public interface ZBSI_RRLControlStateBuilder_description_arg5 {

        /**
         * If true, requests will automatically time-out after a failed attempt;
         * this can be useful e.g. during shutdown.
         */
        public ZBSI_RRLControlStateBuilder_description_arg6 timeoutRequestsAfterFailedAttempt(boolean timeoutRequestsAfterFailedAttempt);
    }

    public interface ZBSI_RRLControlStateBuilder_description_arg4 {

        /**
         * If true, all pending requests will timeout without making further attempts;
         * this can be useful e.g. during quick shutdown.
         */
        public ZBSI_RRLControlStateBuilder_description_arg5 timeoutAllPendingRequests(boolean timeoutAllPendingRequests);
    }

    public interface ZBSI_RRLControlStateBuilder_description_arg3 {

        /**
         * Whether specified delays (initial and after retries) are respected; if
         * true, the delays will be ignored (which might be useful during the
         * shutdown and/or spooldown).
         */
        public ZBSI_RRLControlStateBuilder_description_arg4 ignoreDelays(boolean ignoreDelays);
    }

    public interface ZBSI_RRLControlStateBuilder_description_arg2 {

        /**
         * Null value means requests are accepted; non-null value means they are
         * rejected with the given string as part of an exception.
         */
        public ZBSI_RRLControlStateBuilder_description_arg3 rejectRequestsString(@Nullable String rejectRequestsString);
    }

    public interface ZBSI_RRLControlStateBuilder_description_arg1 {

        /**
         * Description of this control state for logging and similar stuff (e.g.
         * "NOT_STARTED", "RUNNING", "SHUTDOWN").
         */
        public ZBSI_RRLControlStateBuilder_description_arg2 description(String description);
    }

    private static final class ZBSI_RRLControlStateBuilder_description_builderClass implements ZBSI_RRLControlStateBuilder_description_builder, ZBSI_RRLControlStateBuilder_description_arg9, ZBSI_RRLControlStateBuilder_description_arg8, ZBSI_RRLControlStateBuilder_description_arg7, ZBSI_RRLControlStateBuilder_description_arg6, ZBSI_RRLControlStateBuilder_description_arg5, ZBSI_RRLControlStateBuilder_description_arg4, ZBSI_RRLControlStateBuilder_description_arg3, ZBSI_RRLControlStateBuilder_description_arg2, ZBSI_RRLControlStateBuilder_description_arg1 {

        @Nullable
        @SuppressWarnings("all")
        private Boolean waitForTickets;

        @SuppressWarnings("all")
        private boolean limitWaitingForTicket;

        @SuppressWarnings("all")
        private boolean limitWaitingForProcessingThread;

        @SuppressWarnings("all")
        private long spooldownTargetTimestamp;

        @SuppressWarnings("all")
        private boolean timeoutRequestsAfterFailedAttempt;

        @SuppressWarnings("all")
        private boolean timeoutAllPendingRequests;

        @SuppressWarnings("all")
        private boolean ignoreDelays;

        @Nullable
        @SuppressWarnings("all")
        private String rejectRequestsString;

        @SuppressWarnings("all")
        private String description;

        /**
         * Specifies whether to wait for tickets:
         * <p>
         * true -- wait for tickets normally (potentially indefinitely, depending
         * 		on {@link #isLimitWaitingForTicket()})
         * <p>
         * false -- do not wait for tickets at all, obtain ticket if immediately
         * 		available, otherwise fail
         * <p>
         * NULL -- ignore tickets completely, assume they are available and proceed
         * 		with processing
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_RRLControlStateBuilder_description_builder waitForTickets(@Nullable Boolean waitForTickets) {
            this.waitForTickets = waitForTickets;
            return this;
        }

        /**
         * If true AND {@link #getSpooldownTargetTimestamp()} is positive, then
         * waiting for ticket is limited by however many
         * requests are still to be processed (are in main queue) and the spooldown
         * target timestamp.
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_RRLControlStateBuilder_description_arg9 limitWaitingForTicket(boolean limitWaitingForTicket) {
            this.limitWaitingForTicket = limitWaitingForTicket;
            return this;
        }

        /**
         * If true AND {@link #getSpooldownTargetTimestamp()} is positive, then
         * waiting for processing thread is limited by however many
         * requests are still to be processed (are in main queue) and the spooldown
         * target timestamp.
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_RRLControlStateBuilder_description_arg8 limitWaitingForProcessingThread(boolean limitWaitingForProcessingThread) {
            this.limitWaitingForProcessingThread = limitWaitingForProcessingThread;
            return this;
        }

        /**
         * Timestamp by which service should attempt to spooldown (complete
         * processing of all the pending requests).
         * <p>
         * Non-positive value means there's no target for spooldown.
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_RRLControlStateBuilder_description_arg7 spooldownTargetTimestamp(long spooldownTargetTimestamp) {
            this.spooldownTargetTimestamp = spooldownTargetTimestamp;
            return this;
        }

        /**
         * If true, requests will automatically time-out after a failed attempt;
         * this can be useful e.g. during shutdown.
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_RRLControlStateBuilder_description_arg6 timeoutRequestsAfterFailedAttempt(boolean timeoutRequestsAfterFailedAttempt) {
            this.timeoutRequestsAfterFailedAttempt = timeoutRequestsAfterFailedAttempt;
            return this;
        }

        /**
         * If true, all pending requests will timeout without making further attempts;
         * this can be useful e.g. during quick shutdown.
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_RRLControlStateBuilder_description_arg5 timeoutAllPendingRequests(boolean timeoutAllPendingRequests) {
            this.timeoutAllPendingRequests = timeoutAllPendingRequests;
            return this;
        }

        /**
         * Whether specified delays (initial and after retries) are respected; if
         * true, the delays will be ignored (which might be useful during the
         * shutdown and/or spooldown).
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_RRLControlStateBuilder_description_arg4 ignoreDelays(boolean ignoreDelays) {
            this.ignoreDelays = ignoreDelays;
            return this;
        }

        /**
         * Null value means requests are accepted; non-null value means they are
         * rejected with the given string as part of an exception.
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_RRLControlStateBuilder_description_arg3 rejectRequestsString(@Nullable String rejectRequestsString) {
            this.rejectRequestsString = rejectRequestsString;
            return this;
        }

        /**
         * Description of this control state for logging and similar stuff (e.g.
         * "NOT_STARTED", "RUNNING", "SHUTDOWN").
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_RRLControlStateBuilder_description_arg2 description(String description) {
            this.description = description;
            return this;
        }

        @Override
        public RRLControlState buildRRLControlState() {
            return new RRLControlState(description, rejectRequestsString, ignoreDelays, timeoutAllPendingRequests, timeoutRequestsAfterFailedAttempt, spooldownTargetTimestamp, limitWaitingForProcessingThread, limitWaitingForTicket, waitForTickets);
        }
    }

    /**
     *  FIELD COMMENT: Description of this control state for logging and similar stuff (e.g.
     *  "NOT_STARTED", "RUNNING", "SHUTDOWN").
     * <p>
     * CONSTRUCTOR COMMENT: Description of this control state for logging and similar stuff (e.g.
     *  "NOT_STARTED", "RUNNING", "SHUTDOWN").
     */
    public static ZBSI_RRLControlStateBuilder_description_arg2 description(String description) {
        return new ZBSI_RRLControlStateBuilder_description_builderClass().description(description);
    }
}
