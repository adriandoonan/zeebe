package io.zeebe.broker.it.job;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Properties;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.RuleChain;
import org.junit.rules.Timeout;

import io.zeebe.broker.it.ClientRule;
import io.zeebe.broker.it.EmbeddedBrokerRule;
import io.zeebe.client.ClientProperties;
import io.zeebe.client.api.clients.JobClient;
import io.zeebe.client.api.events.JobEvent;

public class CreateJobTest
{

    public EmbeddedBrokerRule brokerRule = new EmbeddedBrokerRule();

    public ClientRule clientRule = new ClientRule(() ->
    {
        final Properties p = new Properties();
        p.setProperty(ClientProperties.CLIENT_REQUEST_TIMEOUT_SEC, "3");
        return p;
    }, true);

    @Rule
    public RuleChain ruleChain = RuleChain
        .outerRule(brokerRule)
        .around(clientRule);

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Rule
    public Timeout testTimeout = Timeout.seconds(15);

    @Test
    public void shouldCreateJob()
    {
        // given
        final JobClient jobClient = clientRule.getClient().topicClient().jobClient();

        // when
        final JobEvent job = jobClient.newCreateCommand()
            .jobType("foo")
            .addCustomHeader("k1", "a")
            .addCustomHeader("k2", "b")
            .payload("{ \"payload\" : 123 }")
            .send()
            .join();

        // then
        assertThat(job).isNotNull();

        final long jobKey = job.getKey();
        assertThat(jobKey).isGreaterThanOrEqualTo(0);
    }

    @Test
    public void shouldFailCreateJobIfTopicNameIsNotValid()
    {
        // given
        final JobClient jobClient = clientRule.getClient().topicClient("unknown-topic").jobClient();

        // then
        thrown.expect(RuntimeException.class);
        thrown.expectMessage("Cannot determine target partition for request. " +
                "Request was: [ topic = unknown-topic, partition = any, value type = JOB, command = CREATE ]");

        // when
        jobClient.newCreateCommand()
            .jobType("foo")
            .send()
            .join();
    }
}
