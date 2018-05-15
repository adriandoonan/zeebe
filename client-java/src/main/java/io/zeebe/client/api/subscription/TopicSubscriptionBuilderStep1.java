package io.zeebe.client.api.subscription;

import io.zeebe.client.api.record.RecordMetadata;

public interface TopicSubscriptionBuilderStep1
{

    /**
     * Set the (unique) name of the subscription.
     * <p>
     * The name is used by the broker to persist the acknowledged
     * subscription's position. When a subscription with the same name is
     * (re-)opened then the broker resumes the subscription at the last
     * acknowledged position and starts publishing with the next
     * event/command.
     * <p>
     * The initial position of the subscription can be defined by calling
     * <code>startAtHeadOfTopic()</code>, <code>startAtTailOfTopic()</code>
     * or <code>startAtPosition()</code>. If the subscription has already an
     * acknowledged position then these calls are ignored and the
     * subscription resumes at the acknowledged position. Use
     * <code>forcedStart()</code> to enforce starting at the supplied start
     * position.
     * <p>
     * Example:
     * <pre>
     * TopicSubscription subscription = subscriptionClient
     *  .newTopicSubscription()
     *  .managed()
     *  .name("my-app")
     *  .workflowInstanceEventHandler(wfEventHandler)
     *  .startAtPosition(0)
     *  .open();
     * </pre>
     *
     * When executed the first time, this snippet creates a new subscription
     * beginning at position 0. When executed a second time, this snippet
     * creates a new subscription beginning at the position at which the
     * first subscription left off.
     *
     * @param name
     *            the (unique) name of the subscription
     * @return the builder for this subscription
     */
    TopicSubscriptionBuilderStep2 name(String name);

    interface TopicSubscriptionBuilderStep2
    {
        /**
         * Register a handler that processes all types of topic records.
         *
         * @param handler
         *            the handler to process all types of topic records
         * @return the builder for this subscription
         */
        TopicSubscriptionBuilderStep3 recordHandler(RecordHandler handler);

        /**
         * Register a handler that processes all job events.
         *
         * @param handler
         *            the handler to process all job events
         * @return the builder for this subscription
         */
        TopicSubscriptionBuilderStep3 jobEventHandler(JobEventHandler handler);

        /**
         * Register a handler that processes all job commands.
         *
         * @param handler
         *            the handler to process all job commands
         * @return the builder for this subscription
         */
        TopicSubscriptionBuilderStep3 jobCommandHandler(JobCommandHandler handler);

        /**
         * Register a handler that processes all workflow instance events.
         *
         * @param handler
         *            the handler to process all workflow instance events
         * @return the builder for this subscription
         */
        TopicSubscriptionBuilderStep3 workflowInstanceEventHandler(WorkflowInstanceEventHandler handler);

        /**
         * Register a handler that processes all workflow instance commands.
         *
         * @param handler
         *            the handler to process all workflow instance commands
         * @return the builder for this subscription
         */
        TopicSubscriptionBuilderStep3 workflowInstanceCommandHandler(WorkflowInstanceCommandHandler handler);

        /**
         * Register a handler that processes all incident events.
         *
         * @param handler
         *            the handler to process all incident events
         * @return the builder for this subscription
         */
        TopicSubscriptionBuilderStep3 incidentEventHandler(IncidentEventHandler handler);

        /**
         * Register a handler that processes all incident commands.
         *
         * @param handler
         *            the handler to process all incident commands
         * @return the builder for this subscription
         */
        TopicSubscriptionBuilderStep3 incidentCommandHandler(IncidentCommandHandler handler);

        /**
         * Register a handler that processes all raft events.
         *
         * @param handler
         *            the handler to process all raft events
         * @return the builder for this subscription
         */
        TopicSubscriptionBuilderStep3 raftEventHandler(RaftEventHandler handler);
    }

    interface TopicSubscriptionBuilderStep3 extends TopicSubscriptionBuilderStep2
    {
        /**
         * Set the initial position of a partition to start publishing from. Can
         * be called multiple times for different partitions.
         * <p>
         * If the subscription has already an acknowledged position then this
         * call is ignored. Call <code>forcedStart()</code> to enforce starting
         * at the supplied position.
         * <p>
         * A <code>position</code> greater than the current tail position of the
         * partition is equivalent to starting at the tail position. In this
         * case, events with a lower position than the supplied position may be
         * received.
         *
         * @param partitionId
         *            the partition the start position applies to. Corresponds
         *            to the partition ID accessible via
         *            {@link RecordMetadata#getPartitionId()}.
         * @param position
         *            the position in the partition at which to start publishing
         *            events from
         * @return the builder for this subscription
         */
        TopicSubscriptionBuilderStep3 startAtPosition(int partitionId, long position);

        /**
         * Start publishing at the current tails of all of the partitions. Can
         * be overridden per partition by calling
         * {@link #startAtPosition(int, long)}.
         * <p>
         * If the subscription has already an acknowledged position then this
         * call is ignored. Call <code>forcedStart()</code> to enforce starting
         * at the tail.
         * <p>
         * It is guaranteed that this subscription does not receive any
         * event/command that was receivable before this subscription is opened.
         *
         * @return the builder for this subscription
         */
        TopicSubscriptionBuilderStep3 startAtTailOfTopic();

        /**
         * Start publishing at the head (i.e. the begin) of all of the
         * partitions. Can be overridden per partition by calling
         * {@link #startAtPosition(int, long)}.
         * <p>
         * If the subscription has already an acknowledged position then this
         * call is ignored. Call <code>forcedStart()</code> to enforce starting
         * at the begin.
         *
         * @return the builder for this subscription
         */
        TopicSubscriptionBuilderStep3 startAtHeadOfTopic();

        /**
         * Force the subscription to start at the given position.
         * <p>
         * This discards the persisted state of the acknowledged position in the
         * broker.
         *
         * @return the builder for this subscription
         */
        TopicSubscriptionBuilderStep3 forcedStart();

        /**
         * Open the subscription and start to process available events/commands.
         *
         * @return the subscription
         */
        TopicSubscription open();
    }

}
