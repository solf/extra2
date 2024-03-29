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

    public interface ZBSI_LoggingStatusBuilder_statusCreatedAt_arg23 {

        /**
         * Last logged message text of the CRITICAL-type severity, null if no
         * such messages were logged (this does not track messages that were not
         * logged due to low severity or throttling).
         */
        public ZBSI_LoggingStatusBuilder_statusCreatedAt_builder lastLoggedCriticalText(@Nullable String lastLoggedCriticalText);
    }

    public interface ZBSI_LoggingStatusBuilder_statusCreatedAt_arg22 {

        /**
         * Timestamp for the last message (regardless of whether it was logged)
         * of the CRITICAL-type severity, 0 if no such messages were logged.
         */
        public ZBSI_LoggingStatusBuilder_statusCreatedAt_arg23 lastLoggedCriticalTimestamp(long lastLoggedCriticalTimestamp);
    }

    public interface ZBSI_LoggingStatusBuilder_statusCreatedAt_arg21 {

        /**
         * Last logged message text of the DATA_LOSS-type severity, null if no
         * such messages were logged (this does not track messages that were not
         * logged due to low severity or throttling).
         */
        public ZBSI_LoggingStatusBuilder_statusCreatedAt_arg22 lastLoggedDataLossText(@Nullable String lastLoggedDataLossText);
    }

    public interface ZBSI_LoggingStatusBuilder_statusCreatedAt_arg20 {

        /**
         * Timestamp for the last message (regardless of whether it was logged)
         * of the DATA_LOSS-type severity, 0 if no such messages were logged.
         */
        public ZBSI_LoggingStatusBuilder_statusCreatedAt_arg21 lastLoggedDataLossTimestamp(long lastLoggedDataLossTimestamp);
    }

    public interface ZBSI_LoggingStatusBuilder_statusCreatedAt_arg19 {

        /**
         * Last logged message text of the ERROR-type severity, null if no
         * such messages were logged (this does not track messages that were not
         * logged due to low severity or throttling).
         */
        public ZBSI_LoggingStatusBuilder_statusCreatedAt_arg20 lastLoggedErrorText(@Nullable String lastLoggedErrorText);
    }

    public interface ZBSI_LoggingStatusBuilder_statusCreatedAt_arg18 {

        /**
         * Timestamp for the last message (regardless of whether it was logged)
         * of the ERROR-type severity, 0 if no such messages were logged.
         */
        public ZBSI_LoggingStatusBuilder_statusCreatedAt_arg19 lastLoggedErrorTimestamp(long lastLoggedErrorTimestamp);
    }

    public interface ZBSI_LoggingStatusBuilder_statusCreatedAt_arg17 {

        /**
         * Last logged message text of the WARN-type severity, null if no
         * such messages were logged (this does not track messages that were not
         * logged due to low severity or throttling).
         */
        public ZBSI_LoggingStatusBuilder_statusCreatedAt_arg18 lastLoggedWarnText(@Nullable String lastLoggedWarnText);
    }

    public interface ZBSI_LoggingStatusBuilder_statusCreatedAt_arg16 {

        /**
         * Timestamp for the last message (regardless of whether it was logged)
         * of the WARN-type severity, 0 if no such messages were logged.
         */
        public ZBSI_LoggingStatusBuilder_statusCreatedAt_arg17 lastLoggedWarnTimestamp(long lastLoggedWarnTimestamp);
    }

    public interface ZBSI_LoggingStatusBuilder_statusCreatedAt_arg15 {

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
        public ZBSI_LoggingStatusBuilder_statusCreatedAt_arg16 lastLoggedTextPerSeverityOrdinal(@Nullable String[] lastLoggedTextPerSeverityOrdinal);
    }

    public interface ZBSI_LoggingStatusBuilder_statusCreatedAt_arg14 {

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
        public ZBSI_LoggingStatusBuilder_statusCreatedAt_arg15 lastLoggedTimestampPerSeverityOrdinal(long[] lastLoggedTimestampPerSeverityOrdinal);
    }

    public interface ZBSI_LoggingStatusBuilder_statusCreatedAt_arg13 {

        /**
         * Total count of messages with severity 'data loss' or higher.
         */
        public ZBSI_LoggingStatusBuilder_statusCreatedAt_arg14 loggedTotalDataLossOrHigherCount(long loggedTotalDataLossOrHigherCount);
    }

    public interface ZBSI_LoggingStatusBuilder_statusCreatedAt_arg12 {

        /**
         * Total count of messages with severity 'error' or higher.
         */
        public ZBSI_LoggingStatusBuilder_statusCreatedAt_arg13 loggedTotalErrorOrHigherCount(long loggedTotalErrorOrHigherCount);
    }

    public interface ZBSI_LoggingStatusBuilder_statusCreatedAt_arg11 {

        /**
         * Total count of messages with severity 'warn' or higher.
         */
        public ZBSI_LoggingStatusBuilder_statusCreatedAt_arg12 loggedTotalWarnOrHigherCount(long loggedTotalWarnOrHigherCount);
    }

    public interface ZBSI_LoggingStatusBuilder_statusCreatedAt_arg10 {

        /**
         * Indicates a critical error (that might well be fatal), meaning the
         * software may well become unusable after this happens.
         */
        public ZBSI_LoggingStatusBuilder_statusCreatedAt_arg11 loggedCriticalCount(long loggedCriticalCount);
    }

    public interface ZBSI_LoggingStatusBuilder_statusCreatedAt_arg9 {

        /**
         * Indicates an error (which is not likely caused by external factors) that
         * is likely to cause data loss.
         * <p>
         * This is used when data loss is highly likely, e.g. when there's a
         * state in the program that cannot be resolved while ensuring all data
         * is preserved.
         */
        public ZBSI_LoggingStatusBuilder_statusCreatedAt_arg10 loggedDataLossCount(long loggedDataLossCount);
    }

    public interface ZBSI_LoggingStatusBuilder_statusCreatedAt_arg8 {

        /**
         * Indicates an error which is likely to be caused by the
         * problems and/or unexpected behavior in the program code itself.
         * <p>
         * Data loss is likely although this should not be fatal.
         */
        public ZBSI_LoggingStatusBuilder_statusCreatedAt_arg9 loggedErrorCount(long loggedErrorCount);
    }

    public interface ZBSI_LoggingStatusBuilder_statusCreatedAt_arg7 {

        /**
         * Indicates an error caused by security issues, such as client failing to
         * provide proper access key or user failing to provide the correct password
         * (that last one could also reasonably be considered {@link #INVALID_USER_INPUT},
         * however in many cases it is desirable to separate security issues in order
         * to monitor attacks and such).
         * <p>
         * These messages usually indicate that there was no data loss (aside from
         * the potentially lost data in the input that had security issue).
         */
        public ZBSI_LoggingStatusBuilder_statusCreatedAt_arg8 loggedSecurityErrorCount(long loggedSecurityErrorCount);
    }

    public interface ZBSI_LoggingStatusBuilder_statusCreatedAt_arg6 {

        /**
         * Indicates an error probably caused by external factors, such
         * as underlying storage failing.
         * <p>
         * This is used when data loss is highly likely, e.g. when implementation
         * gives up on writing piece of data to the underlying storage.
         */
        public ZBSI_LoggingStatusBuilder_statusCreatedAt_arg7 loggedExternalDataLossCount(long loggedExternalDataLossCount);
    }

    public interface ZBSI_LoggingStatusBuilder_statusCreatedAt_arg5 {

        /**
         * Indicates an error probably caused by external factors, such
         * as underlying storage failing.
         * <p>
         * These messages usually indicate that there was no data loss (yet).
         */
        public ZBSI_LoggingStatusBuilder_statusCreatedAt_arg6 loggedExternalErrorCount(long loggedExternalErrorCount);
    }

    public interface ZBSI_LoggingStatusBuilder_statusCreatedAt_arg4 {

        /**
         * Indicates a problem caused by invalid user input.
         * <p>
         * These messages usually indicate that there was no data loss (aside from
         * the invalid input itself which may be lost).
         */
        public ZBSI_LoggingStatusBuilder_statusCreatedAt_arg5 loggedInvalidUserInputCount(long loggedInvalidUserInputCount);
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

    private static final class ZBSI_LoggingStatusBuilder_statusCreatedAt_builderClass implements ZBSI_LoggingStatusBuilder_statusCreatedAt_builder, ZBSI_LoggingStatusBuilder_statusCreatedAt_arg23, ZBSI_LoggingStatusBuilder_statusCreatedAt_arg22, ZBSI_LoggingStatusBuilder_statusCreatedAt_arg21, ZBSI_LoggingStatusBuilder_statusCreatedAt_arg20, ZBSI_LoggingStatusBuilder_statusCreatedAt_arg19, ZBSI_LoggingStatusBuilder_statusCreatedAt_arg18, ZBSI_LoggingStatusBuilder_statusCreatedAt_arg17, ZBSI_LoggingStatusBuilder_statusCreatedAt_arg16, ZBSI_LoggingStatusBuilder_statusCreatedAt_arg15, ZBSI_LoggingStatusBuilder_statusCreatedAt_arg14, ZBSI_LoggingStatusBuilder_statusCreatedAt_arg13, ZBSI_LoggingStatusBuilder_statusCreatedAt_arg12, ZBSI_LoggingStatusBuilder_statusCreatedAt_arg11, ZBSI_LoggingStatusBuilder_statusCreatedAt_arg10, ZBSI_LoggingStatusBuilder_statusCreatedAt_arg9, ZBSI_LoggingStatusBuilder_statusCreatedAt_arg8, ZBSI_LoggingStatusBuilder_statusCreatedAt_arg7, ZBSI_LoggingStatusBuilder_statusCreatedAt_arg6, ZBSI_LoggingStatusBuilder_statusCreatedAt_arg5, ZBSI_LoggingStatusBuilder_statusCreatedAt_arg4, ZBSI_LoggingStatusBuilder_statusCreatedAt_arg3, ZBSI_LoggingStatusBuilder_statusCreatedAt_arg2, ZBSI_LoggingStatusBuilder_statusCreatedAt_arg1 {

        @Nullable
        @SuppressWarnings("all")
        private String lastLoggedCriticalText;

        @SuppressWarnings("all")
        private long lastLoggedCriticalTimestamp;

        @Nullable
        @SuppressWarnings("all")
        private String lastLoggedDataLossText;

        @SuppressWarnings("all")
        private long lastLoggedDataLossTimestamp;

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
        private long loggedTotalDataLossOrHigherCount;

        @SuppressWarnings("all")
        private long loggedTotalErrorOrHigherCount;

        @SuppressWarnings("all")
        private long loggedTotalWarnOrHigherCount;

        @SuppressWarnings("all")
        private long loggedCriticalCount;

        @SuppressWarnings("all")
        private long loggedDataLossCount;

        @SuppressWarnings("all")
        private long loggedErrorCount;

        @SuppressWarnings("all")
        private long loggedSecurityErrorCount;

        @SuppressWarnings("all")
        private long loggedExternalDataLossCount;

        @SuppressWarnings("all")
        private long loggedExternalErrorCount;

        @SuppressWarnings("all")
        private long loggedInvalidUserInputCount;

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
        public ZBSI_LoggingStatusBuilder_statusCreatedAt_arg23 lastLoggedCriticalTimestamp(long lastLoggedCriticalTimestamp) {
            this.lastLoggedCriticalTimestamp = lastLoggedCriticalTimestamp;
            return this;
        }

        /**
         * Last logged message text of the DATA_LOSS-type severity, null if no
         * such messages were logged (this does not track messages that were not
         * logged due to low severity or throttling).
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_LoggingStatusBuilder_statusCreatedAt_arg22 lastLoggedDataLossText(@Nullable String lastLoggedDataLossText) {
            this.lastLoggedDataLossText = lastLoggedDataLossText;
            return this;
        }

        /**
         * Timestamp for the last message (regardless of whether it was logged)
         * of the DATA_LOSS-type severity, 0 if no such messages were logged.
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_LoggingStatusBuilder_statusCreatedAt_arg21 lastLoggedDataLossTimestamp(long lastLoggedDataLossTimestamp) {
            this.lastLoggedDataLossTimestamp = lastLoggedDataLossTimestamp;
            return this;
        }

        /**
         * Last logged message text of the ERROR-type severity, null if no
         * such messages were logged (this does not track messages that were not
         * logged due to low severity or throttling).
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_LoggingStatusBuilder_statusCreatedAt_arg20 lastLoggedErrorText(@Nullable String lastLoggedErrorText) {
            this.lastLoggedErrorText = lastLoggedErrorText;
            return this;
        }

        /**
         * Timestamp for the last message (regardless of whether it was logged)
         * of the ERROR-type severity, 0 if no such messages were logged.
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_LoggingStatusBuilder_statusCreatedAt_arg19 lastLoggedErrorTimestamp(long lastLoggedErrorTimestamp) {
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
        public ZBSI_LoggingStatusBuilder_statusCreatedAt_arg18 lastLoggedWarnText(@Nullable String lastLoggedWarnText) {
            this.lastLoggedWarnText = lastLoggedWarnText;
            return this;
        }

        /**
         * Timestamp for the last message (regardless of whether it was logged)
         * of the WARN-type severity, 0 if no such messages were logged.
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_LoggingStatusBuilder_statusCreatedAt_arg17 lastLoggedWarnTimestamp(long lastLoggedWarnTimestamp) {
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
        public ZBSI_LoggingStatusBuilder_statusCreatedAt_arg16 lastLoggedTextPerSeverityOrdinal(@Nullable String[] lastLoggedTextPerSeverityOrdinal) {
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
        public ZBSI_LoggingStatusBuilder_statusCreatedAt_arg15 lastLoggedTimestampPerSeverityOrdinal(long[] lastLoggedTimestampPerSeverityOrdinal) {
            this.lastLoggedTimestampPerSeverityOrdinal = lastLoggedTimestampPerSeverityOrdinal;
            return this;
        }

        /**
         * Total count of messages with severity 'data loss' or higher.
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_LoggingStatusBuilder_statusCreatedAt_arg14 loggedTotalDataLossOrHigherCount(long loggedTotalDataLossOrHigherCount) {
            this.loggedTotalDataLossOrHigherCount = loggedTotalDataLossOrHigherCount;
            return this;
        }

        /**
         * Total count of messages with severity 'error' or higher.
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_LoggingStatusBuilder_statusCreatedAt_arg13 loggedTotalErrorOrHigherCount(long loggedTotalErrorOrHigherCount) {
            this.loggedTotalErrorOrHigherCount = loggedTotalErrorOrHigherCount;
            return this;
        }

        /**
         * Total count of messages with severity 'warn' or higher.
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_LoggingStatusBuilder_statusCreatedAt_arg12 loggedTotalWarnOrHigherCount(long loggedTotalWarnOrHigherCount) {
            this.loggedTotalWarnOrHigherCount = loggedTotalWarnOrHigherCount;
            return this;
        }

        /**
         * Indicates a critical error (that might well be fatal), meaning the
         * software may well become unusable after this happens.
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_LoggingStatusBuilder_statusCreatedAt_arg11 loggedCriticalCount(long loggedCriticalCount) {
            this.loggedCriticalCount = loggedCriticalCount;
            return this;
        }

        /**
         * Indicates an error (which is not likely caused by external factors) that
         * is likely to cause data loss.
         * <p>
         * This is used when data loss is highly likely, e.g. when there's a
         * state in the program that cannot be resolved while ensuring all data
         * is preserved.
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_LoggingStatusBuilder_statusCreatedAt_arg10 loggedDataLossCount(long loggedDataLossCount) {
            this.loggedDataLossCount = loggedDataLossCount;
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
        public ZBSI_LoggingStatusBuilder_statusCreatedAt_arg9 loggedErrorCount(long loggedErrorCount) {
            this.loggedErrorCount = loggedErrorCount;
            return this;
        }

        /**
         * Indicates an error caused by security issues, such as client failing to
         * provide proper access key or user failing to provide the correct password
         * (that last one could also reasonably be considered {@link #INVALID_USER_INPUT},
         * however in many cases it is desirable to separate security issues in order
         * to monitor attacks and such).
         * <p>
         * These messages usually indicate that there was no data loss (aside from
         * the potentially lost data in the input that had security issue).
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_LoggingStatusBuilder_statusCreatedAt_arg8 loggedSecurityErrorCount(long loggedSecurityErrorCount) {
            this.loggedSecurityErrorCount = loggedSecurityErrorCount;
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
        public ZBSI_LoggingStatusBuilder_statusCreatedAt_arg7 loggedExternalDataLossCount(long loggedExternalDataLossCount) {
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
        public ZBSI_LoggingStatusBuilder_statusCreatedAt_arg6 loggedExternalErrorCount(long loggedExternalErrorCount) {
            this.loggedExternalErrorCount = loggedExternalErrorCount;
            return this;
        }

        /**
         * Indicates a problem caused by invalid user input.
         * <p>
         * These messages usually indicate that there was no data loss (aside from
         * the invalid input itself which may be lost).
         */
        @Override
        @SuppressWarnings("hiding")
        public ZBSI_LoggingStatusBuilder_statusCreatedAt_arg5 loggedInvalidUserInputCount(long loggedInvalidUserInputCount) {
            this.loggedInvalidUserInputCount = loggedInvalidUserInputCount;
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
            return new LoggingStatus(statusCreatedAt, loggedWarnCount, loggedExternalWarnCount, loggedInvalidUserInputCount, loggedExternalErrorCount, loggedExternalDataLossCount, loggedSecurityErrorCount, loggedErrorCount, loggedDataLossCount, loggedCriticalCount, loggedTotalWarnOrHigherCount, loggedTotalErrorOrHigherCount, loggedTotalDataLossOrHigherCount, lastLoggedTimestampPerSeverityOrdinal, lastLoggedTextPerSeverityOrdinal, lastLoggedWarnTimestamp, lastLoggedWarnText, lastLoggedErrorTimestamp, lastLoggedErrorText, lastLoggedDataLossTimestamp, lastLoggedDataLossText, lastLoggedCriticalTimestamp, lastLoggedCriticalText);
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
