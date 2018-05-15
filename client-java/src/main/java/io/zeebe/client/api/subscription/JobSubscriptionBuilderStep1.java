package io.zeebe.client.api.subscription;

import java.time.Duration;

public interface JobSubscriptionBuilderStep1
{
    int DEFAULT_JOB_FETCH_SIZE = 32;

    /**
     * Set the type of jobs to work on.
     *
     * @param type
     *            the type of jobs (e.g. "payment")
     *
     * @return the builder for this subscription
     */
    JobSubscriptionBuilderStep2 jobType(String type);

    interface JobSubscriptionBuilderStep2
    {
        /**
         * Set the handler to process the jobs. At the end of the processing,
         * the handler should complete the job or mark it as failed;
         * <p>
         * Example JobHandler implementation:
         * <pre>
         * public class PaymentHandler implements JobHandler
         * {
         *   &#64;Override
         *   public void handle(JobClient client, JobEvent jobEvent)
         *   {
         *     String json = jobEvent.getPayload();
         *     // modify payload
         *
         *     client
         *      .newCompleteCommand()
         *      .event(jobEvent)
         *      .payload(json)
         *      .send();
         *   }
         * };
         * </pre>
         *
         * The handler must be be thread-safe.
         *
         * @param handler
         *            the handle to process the jobs
         *
         * @return the builder for this subscription
         */
        JobSubscriptionBuilderStep3 handler(JobHandler handler);
    }

    interface JobSubscriptionBuilderStep3
    {
        /**
         * Set the time for how long a job is exclusively assigned for this
         * subscription.
         * <p>
         * In this time, the job can not be assigned by other subscriptions to
         * ensure that only one subscription work on the job. When the time is
         * over then the job can be assigned again by this or other subscription
         * if it's not completed yet.
         * <p>
         * If no time is set then the default is used from the configuration.
         *
         * @param lockTime
         *            the time in milliseconds
         *
         * @return the builder for this subscription
         */
        JobSubscriptionBuilderStep3 lockTime(long lockTime);

        /**
         * Set the time for how long a job is exclusively assigned for this
         * subscription.
         * <p>
         * In this time, the job can not be assigned by other subscriptions to
         * ensure that only one subscription work on the job. When the time is
         * over then the job can be assigned again by this or other subscription
         * if it's not completed yet.
         * <p>
         * If no time is set then the default is used from the configuration.
         *
         * @param lockTime
         *            the time as duration (e.g. "Duration.ofMinutes(5)")
         *
         * @return the builder for this subscription
         */
        JobSubscriptionBuilderStep3 lockTime(Duration lockTime);

        /**
         * Set the name of the subscription owner.
         * <p>
         * This name is used to identify the subscription which a job is exclusively assigned to.
         * <p>
         * If no name is set then the default is used from the configuration.
         *
         * @param lockOwner the name of the subscription owner (e.g. "payment-service")
         *
         * @return the builder for this subscription
         */
        JobSubscriptionBuilderStep3 lockOwner(String lockOwner);

        /**
         * Set the maximum number of jobs which will be exclusively assigned to
         * this subscription at the same time.
         * <p>
         * This is used to control the backpressure of the subscription. When
         * the number of assigned jobs is reached then the broker will no assign
         * more jobs to the subscription to not overwhelm the client and give
         * other subscriptions the chance to work on the jobs. The broker will
         * assign new jobs again when jobs are completed (or marked as failed) which
         * were assigned to the subscription.
         * <p>
         * If no fetch size is set then the default is used from the
         * configuration.
         *
         * @param fetchSize
         *            the number of assigned jobs
         *
         * @return the builder for this subscription
         */
        JobSubscriptionBuilderStep3 fetchSize(int fetchSize);

        /**
         * Open the subscription and start to work on available tasks.
         *
         * @return the subscription
         */
        JobSubscription open();
    }

}
