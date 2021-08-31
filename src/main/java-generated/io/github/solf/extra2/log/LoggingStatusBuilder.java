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
package io.github.solf.extra2.log;

import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.Nullable;
import org.eclipse.jdt.annotation.NonNullByDefault;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import io.github.solf.extra2.codegenerate.stepbuilder.unused.UnusedInterface;

/**
 *  Step Builder class for {@link LoggingStatus}
 * <p>
 * Logging status.
 *
 *  @author Sergey Olefir
 */
@NonNullByDefault
@SuppressWarnings("unused")
public class LoggingStatusBuilder {

    public interface ZBSI_LoggingStatusBuilder_statusCreatedAt_builder {

        public LoggingStatus buildLoggingStatus();
    }

    public interface ZBSI_LoggingStatusBuilder_statusCreatedAt_arg17 {

        /**
         * Last logged message text of the CRITICAL-type severity, null if no
         * such messages were logged (this does not track messages that were not
         * logged due to low severity or throttling).
         */
        public ZBSI_LoggingStatusBuilder_statusCreatedAt_builder lastLoggedCriticalText(@Nullable String lastLoggedCriticalText);
    }

    public interface ZBSI_LoggingStatusBuilder_statusCreatedAt_arg16 {

        /**
         * Timestamp for the last message (regardless of whether it was logged)
         * of the CRITICAL-type severity, 0 if no such messages were logged.
         */
        public ZBSI_LoggingStatusBuilder_statusCreatedAt_arg17 lastLoggedCriticalTimestamp(long lastLoggedCriticalTimestamp);
    }

    public interface ZBSI_LoggingStatusBuilder_statusCreatedAt_arg15 {

        /**
         * Last logged message text of the ERROR-type severity, null if no
         * such messages were logged (this does not track messages that were not
         * logged due to low severity or throttling).
         */
        public ZBSI_LoggingStatusBuilder_statusCreatedAt_arg16 lastLoggedErrorText(@Nullable String lastLoggedErrorText);
    }

    public interface ZBSI_LoggingStatusBuilder_statusCreatedAt_arg14 {

        /**
         * Timestamp for the last message (regardless of whether it was logged)
         * of the ERROR-type severity, 0 if no such messages were logged.
         */
        public ZBSI_LoggingStatusBuilder_statusCreatedAt_arg15 lastLoggedErrorTimestamp(long lastLoggedErrorTimestamp);
    }

    public interface ZBSI_LoggingStatusBuilder_statusCreatedAt_arg13 {

        /**
         * Last logged message text of the WARN-type severity, null if no
         * such messages were logged (this does not track messages that were not
         * logged due to low severity or throttling).
         */
        public ZBSI_LoggingStatusBuilder_statusCreatedAt_arg14 lastLoggedWarnText(@Nullable String lastLoggedWarnText);
    }

    public interface ZBSI_LoggingStatusBuilder_statusCreatedAt_arg12 {

        /**
         * Timestamp for the last message (regardless of whether it was logged)
         * of the WARN-type severity, 0 if no such messages were logged.
         */
        public ZBSI_LoggingStatusBuilder_statusCreatedAt_arg13 lastLoggedWarnTimestamp(long lastLoggedWarnTimestamp);
    }

    public interface ZBSI_LoggingStatusBuilder_statusCreatedAt_arg11 {

        /**
         * Collects last message text per each severity in {@link LogMessageSeverity}
         * <p>
         * It's {@link AtomicReference} contains null until matching message happens.
         * <p>
         * NOTE: these are tracked ONLY if message is actually sent to logging,
         * i.e. if it passes severity & throttling check.
         * <p>
         * NOTE2: those are not 'atomic' with {@link #getLastTimestampMsgPerSeverityOrdinal()}
         * there can be discrepancies.
         */
        public ZBSI_LoggingStatusBuilder_statusCreatedAt_arg12 lastLoggedTextPerSeverityOrdinal(@Nullable String[] lastLoggedTextPerSeverityOrdinal);
    }

    public interface ZBSI_LoggingStatusBuilder_statusCreatedAt_arg10 {

        /**
         * Collects last message timestamps per each severity in {@link LogMessageSeverity}
         * <p>
         * It is set to 0 until first matching message happens.
         * <p>
         * NOTE: these are tracked even if the message itself is not logged due
         * to log severity settings or something.
         * <p>
         * NOTE2: those are not 'atomic' with {@link #getLastLoggedTextMsgPerSeverityOrdinal()}
         * there can be discrepancies.
         */
        public ZBSI_LoggingStatusBuilder_statusCreatedAt_arg11 lastLoggedTimestampPerSeverityOrdinal(long[] lastLoggedTimestampPerSeverityOrdinal);
    }

    public interface ZBSI_LoggingStatusBuilder_statusCreatedAt_arg9 {

        /**
         * Total count of messages with severity 'error' or higher.
         */
        public ZBSI_LoggingStatusBuilder_statusCreatedAt_arg10 loggedTotalErrorOrHigherCount(long loggedTotalErrorOrHigherCount);
    }

    public interface ZBSI_LoggingStatusBuilder_statusCreatedAt_arg8 {

        /**
         * Total count of messages with severity 'warn' or higher.
         */
        public ZBSI_LoggingStatusBuilder_statusCreatedAt_arg9 loggedTotalWarnOrHigherCount(long loggedTotalWarnOrHigherCount);
    }

    public interface ZBSI_LoggingStatusBuilder_statusCreatedAt_arg7 {

        /**
         * Indicates a critical error (that might well be fatal), meaning the
         * software may well become unusable after this happens.
         */
        public ZBSI_LoggingStatusBuilder_statusCreatedAt_arg8 loggedCriticalCount(long loggedCriticalCount);
    }

    public interface ZBSI_LoggingStatusBuilder_statusCreatedAt_arg6 {

        /**
         * Indicates an error which is likely to be caused by the
         * problems and/or unexpected behavior in the program code itself.
         * <p>
         * Data loss is likely although this should not be fatal.
         */
        public ZBSI_LoggingStatusBuilder_statusCreatedAt_arg7 loggedErrorCount(long loggedErrorCount);
    }

    public interface ZBSI_LoggingStatusBuilder_statusCreatedAt_arg5 {

        /**
         * Indicates an error probably caused by external factors, such
         * as underlying storage failing.
         * <p>
         * This is used when data loss is highly likely, e.g. when implementation
         * gives up on writing piece of data to the underlying storage.
         */
        public ZBSI_LoggingStatusBuilder_statusCreatedAt_arg6 loggedExternalDataLossCount(long loggedExternalDataLossCount);
    }

    public interface ZBSI_LoggingStatusBuilder_statusCreatedAt_arg4 {

        /**
         * Indicates an error probably caused by external factors, such
         * as underlying storage failing.
         * <p>
         * These messages usually indicate that there was no data loss (yet).
         */
        public ZBSI_LoggingStatusBuilder_statusCreatedAt_arg5 loggedExternalErrorCount(long loggedExternalErrorCount);
    }

    public interface ZBSI_LoggingStatusBuilder_statusCreatedAt_arg3 {

        /**
         * Indicates an externally-caused warning.
         * <p>
         * These messages usually indicate that there was no data loss (yet).
         */
        public ZBSI_LoggingStatusBuilder_statusCreatedAt_arg4 loggedExternalWarnCount(long loggedExternalWarnCount);
    }

    public interface ZBSI_LoggingStatusBuilder_statusCreatedAt_arg2 {

        /**
         * Indicates problem that is probably caused by internal somewhat-known
         * factors, such as potential concurrency/race conditions (which normally
         * are not expected to occur).
         * <p>
         * These usually should not result in data loss.
         */
        public ZBSI_LoggingStatusBuilder_statusCreatedAt_arg3 loggedWarnCount(long loggedWarnCount);
    }

    public interface ZBSI_LoggingStatusBuilder_statusCreatedAt_arg1 {

        /**
         * Indicates when status was created (e.g. for caching purposes).
         */
        public ZBSI_LoggingStatusBuilder_statusCreatedAt_arg2 statusCreatedAt(long statusCreatedAt);
    }

    private static final class ZBSI_LoggingStatusBuilder_statusCreatedAt_builderClass implements ZBSI_LoggingStatusBuilder_statusCreatedAt_builder, ZBSI_LoggingStatusBuilder_statusCreatedAt_arg17, ZBSI_LoggingStatusBuilder_statusCreatedAt_arg16, ZBSI_LoggingStatusBuilder_statusCreatedAt_arg15, ZBSI_LoggingStatusBuilder_statusCreatedAt_arg14, ZBSI_LoggingStatusBuilder_statusCreatedAt_arg13, ZBSI_LoggingStatusBuilder_statusCreatedAt_arg12, ZBSI_LoggingStatusBuilder_statusCreatedAt_arg11, ZBSI_LoggingStatusBuilder_statusCreatedAt_arg10, ZBSI_LoggingStatusBuilder_statusCreatedAt_arg9, ZBSI_LoggingStatusBuilder_statusCreatedAt_arg8, ZBSI_LoggingStatusBuilder_statusCreatedAt_arg7, ZBSI_LoggingStatusBuilder_statusCreatedAt_arg6, ZBSI_LoggingStatusBuilder_statusCreatedAt_arg5, ZBSI_LoggingStatusBuilder_statusCreatedAt_arg4, ZBSI_LoggingStatusBuilder_statusCreatedAt_arg3, ZBSI_LoggingStatusBuilder_statusCreatedAt_arg2, ZBSI_LoggingStatusBuilder_statusCreatedAt_arg1 {

        @Nullable
        @SuppressWarnings("all")
        private String lastLoggedCriticalText;

        @SuppressWarnings("all")
        private long lastLoggedCriticalTimestamp;

        @Nullable
        @SuppressWarnings("all")
        private String lastLoggedErrorText;

        @SuppressWarnings("all")
        private long lastLoggedErrorTimestamp;

        @Nullable
        @SuppressWarnings("all")
        private String lastLoggedWarnText;

        @SuppressWarnings("all")
        private long lastLoggedWarnTimestamp;

        @Nullable
        @SuppressWarnings("all")
        private String[] lastLoggedTextPerSeverityOrdinal;

        @SuppressWarnings("all")
        private long[] lastLoggedTimestampPerSeverityOrdinal;

        @SuppressWarnings("all")
        private long loggedTotalErrorOrHigherCount;

        @SuppressWarnings("all")
        private long loggedTotalWarnOrHigherCount;

        @SuppressWarnings("all")
        private long loggedCriticalCount;

        @SuppressWarnings("all")
        private long loggedErrorCount;

        @SuppressWarnings("all")
        private long loggedExternalDataLossCount;

        @SuppressWarnings("all")
        private long loggedExternalErrorCount;

        @SuppressWarnings("all")
        private long loggedExternalWarnCount;

        @SuppressWarnings("all")
        private long loggedWarnCount;

        @SuppressWarnings("all")
        private long statusCreatedAt;

        /**
         * Last logged message text of the CRITICAL-type severity, null if no
         * such messages were logged (this does not track messages that were not
         * logged due to low severity or throttling).
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_LoggingStatusBuilder_statusCreatedAt_builder lastLoggedCriticalText(@Nullable String lastLoggedCriticalText) {
            this.lastLoggedCriticalText = lastLoggedCriticalText;
            return this;
        }

        /**
         * Timestamp for the last message (regardless of whether it was logged)
         * of the CRITICAL-type severity, 0 if no such messages were logged.
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_LoggingStatusBuilder_statusCreatedAt_arg17 lastLoggedCriticalTimestamp(long lastLoggedCriticalTimestamp) {
            this.lastLoggedCriticalTimestamp = lastLoggedCriticalTimestamp;
            return this;
        }

        /**
         * Last logged message text of the ERROR-type severity, null if no
         * such messages were logged (this does not track messages that were not
         * logged due to low severity or throttling).
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_LoggingStatusBuilder_statusCreatedAt_arg16 lastLoggedErrorText(@Nullable String lastLoggedErrorText) {
            this.lastLoggedErrorText = lastLoggedErrorText;
            return this;
        }

        /**
         * Timestamp for the last message (regardless of whether it was logged)
         * of the ERROR-type severity, 0 if no such messages were logged.
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_LoggingStatusBuilder_statusCreatedAt_arg15 lastLoggedErrorTimestamp(long lastLoggedErrorTimestamp) {
            this.lastLoggedErrorTimestamp = lastLoggedErrorTimestamp;
            return this;
        }

        /**
         * Last logged message text of the WARN-type severity, null if no
         * such messages were logged (this does not track messages that were not
         * logged due to low severity or throttling).
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_LoggingStatusBuilder_statusCreatedAt_arg14 lastLoggedWarnText(@Nullable String lastLoggedWarnText) {
            this.lastLoggedWarnText = lastLoggedWarnText;
            return this;
        }

        /**
         * Timestamp for the last message (regardless of whether it was logged)
         * of the WARN-type severity, 0 if no such messages were logged.
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_LoggingStatusBuilder_statusCreatedAt_arg13 lastLoggedWarnTimestamp(long lastLoggedWarnTimestamp) {
            this.lastLoggedWarnTimestamp = lastLoggedWarnTimestamp;
            return this;
        }

        /**
         * Collects last message text per each severity in {@link LogMessageSeverity}
         * <p>
         * It's {@link AtomicReference} contains null until matching message happens.
         * <p>
         * NOTE: these are tracked ONLY if message is actually sent to logging,
         * i.e. if it passes severity & throttling check.
         * <p>
         * NOTE2: those are not 'atomic' with {@link #getLastTimestampMsgPerSeverityOrdinal()}
         * there can be discrepancies.
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_LoggingStatusBuilder_statusCreatedAt_arg12 lastLoggedTextPerSeverityOrdinal(@Nullable String[] lastLoggedTextPerSeverityOrdinal) {
            this.lastLoggedTextPerSeverityOrdinal = lastLoggedTextPerSeverityOrdinal;
            return this;
        }

        /**
         * Collects last message timestamps per each severity in {@link LogMessageSeverity}
         * <p>
         * It is set to 0 until first matching message happens.
         * <p>
         * NOTE: these are tracked even if the message itself is not logged due
         * to log severity settings or something.
         * <p>
         * NOTE2: those are not 'atomic' with {@link #getLastLoggedTextMsgPerSeverityOrdinal()}
         * there can be discrepancies.
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_LoggingStatusBuilder_statusCreatedAt_arg11 lastLoggedTimestampPerSeverityOrdinal(long[] lastLoggedTimestampPerSeverityOrdinal) {
            this.lastLoggedTimestampPerSeverityOrdinal = lastLoggedTimestampPerSeverityOrdinal;
            return this;
        }

        /**
         * Total count of messages with severity 'error' or higher.
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_LoggingStatusBuilder_statusCreatedAt_arg10 loggedTotalErrorOrHigherCount(long loggedTotalErrorOrHigherCount) {
            this.loggedTotalErrorOrHigherCount = loggedTotalErrorOrHigherCount;
            return this;
        }

        /**
         * Total count of messages with severity 'warn' or higher.
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_LoggingStatusBuilder_statusCreatedAt_arg9 loggedTotalWarnOrHigherCount(long loggedTotalWarnOrHigherCount) {
            this.loggedTotalWarnOrHigherCount = loggedTotalWarnOrHigherCount;
            return this;
        }

        /**
         * Indicates a critical error (that might well be fatal), meaning the
         * software may well become unusable after this happens.
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_LoggingStatusBuilder_statusCreatedAt_arg8 loggedCriticalCount(long loggedCriticalCount) {
            this.loggedCriticalCount = loggedCriticalCount;
            return this;
        }

        /**
         * Indicates an error which is likely to be caused by the
         * problems and/or unexpected behavior in the program code itself.
         * <p>
         * Data loss is likely although this should not be fatal.
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_LoggingStatusBuilder_statusCreatedAt_arg7 loggedErrorCount(long loggedErrorCount) {
            this.loggedErrorCount = loggedErrorCount;
            return this;
        }

        /**
         * Indicates an error probably caused by external factors, such
         * as underlying storage failing.
         * <p>
         * This is used when data loss is highly likely, e.g. when implementation
         * gives up on writing piece of data to the underlying storage.
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_LoggingStatusBuilder_statusCreatedAt_arg6 loggedExternalDataLossCount(long loggedExternalDataLossCount) {
            this.loggedExternalDataLossCount = loggedExternalDataLossCount;
            return this;
        }

        /**
         * Indicates an error probably caused by external factors, such
         * as underlying storage failing.
         * <p>
         * These messages usually indicate that there was no data loss (yet).
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_LoggingStatusBuilder_statusCreatedAt_arg5 loggedExternalErrorCount(long loggedExternalErrorCount) {
            this.loggedExternalErrorCount = loggedExternalErrorCount;
            return this;
        }

        /**
         * Indicates an externally-caused warning.
         * <p>
         * These messages usually indicate that there was no data loss (yet).
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_LoggingStatusBuilder_statusCreatedAt_arg4 loggedExternalWarnCount(long loggedExternalWarnCount) {
            this.loggedExternalWarnCount = loggedExternalWarnCount;
            return this;
        }

        /**
         * Indicates problem that is probably caused by internal somewhat-known
         * factors, such as potential concurrency/race conditions (which normally
         * are not expected to occur).
         * <p>
         * These usually should not result in data loss.
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_LoggingStatusBuilder_statusCreatedAt_arg3 loggedWarnCount(long loggedWarnCount) {
            this.loggedWarnCount = loggedWarnCount;
            return this;
        }

        /**
         * Indicates when status was created (e.g. for caching purposes).
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_LoggingStatusBuilder_statusCreatedAt_arg2 statusCreatedAt(long statusCreatedAt) {
            this.statusCreatedAt = statusCreatedAt;
            return this;
        }

        @Override
        @SuppressWarnings("all")
        public LoggingStatus buildLoggingStatus() {
            return new LoggingStatus(statusCreatedAt, loggedWarnCount, loggedExternalWarnCount, loggedExternalErrorCount, loggedExternalDataLossCount, loggedErrorCount, loggedCriticalCount, loggedTotalWarnOrHigherCount, loggedTotalErrorOrHigherCount, lastLoggedTimestampPerSeverityOrdinal, lastLoggedTextPerSeverityOrdinal, lastLoggedWarnTimestamp, lastLoggedWarnText, lastLoggedErrorTimestamp, lastLoggedErrorText, lastLoggedCriticalTimestamp, lastLoggedCriticalText);
        }
    }

    /**
     *  FIELD COMMENT: Indicates when status was created (e.g. for caching purposes).
     * <p>
     * CONSTRUCTOR COMMENT: Indicates when status was created (e.g. for caching purposes).
     */
    public static ZBSI_LoggingStatusBuilder_statusCreatedAt_arg2 statusCreatedAt(long statusCreatedAt) {
        return new ZBSI_LoggingStatusBuilder_statusCreatedAt_builderClass().statusCreatedAt(statusCreatedAt);
    }
}
