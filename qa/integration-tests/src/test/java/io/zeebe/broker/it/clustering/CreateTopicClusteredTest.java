/*
 * Copyright © 2017 camunda services GmbH (info@camunda.com)
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
package io.zeebe.broker.it.clustering;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.Timeout;

import io.zeebe.broker.Broker;
import io.zeebe.broker.it.ClientRule;
import io.zeebe.client.ZeebeClient;
import io.zeebe.client.event.TaskEvent;
import io.zeebe.test.util.AutoCloseableRule;
import io.zeebe.transport.SocketAddress;

import static io.zeebe.broker.it.clustering.Brokers.*;

public class CreateTopicClusteredTest
{

    protected Brokers brokers;

    @Rule
    public AutoCloseableRule closeables = new AutoCloseableRule();

    @Rule
    public ClientRule clientRule = new ClientRule(false);

    @Rule
    public Timeout timeout = new Timeout(30, TimeUnit.SECONDS);

    @Before
    public void setUp()
    {
        brokers = new Brokers();
        closeables.manage(brokers);
    }

    @Test
    // FIXME: https://github.com/zeebe-io/zeebe/issues/561
    @Category(io.zeebe.UnstableTest.class)
    public void shouldCreateTopic()
    {
        // given
        brokers.startBroker(BROKER_1_CLIENT_ADDRESS, BROKER_1_TOML);
        brokers.startBroker(BROKER_2_CLIENT_ADDRESS, BROKER_2_TOML);
        brokers.startBroker(BROKER_3_CLIENT_ADDRESS, BROKER_3_TOML);

        final ZeebeClient client = clientRule.getClient();

        // when
        client.topics().create("foo", 2).execute();

        // then
        final TaskEvent taskEvent = client.tasks().create("foo", "bar").execute();

        assertThat(taskEvent).isNotNull();
        assertThat(taskEvent.getState()).isEqualTo("CREATED");
    }

    @Test
    // FIXME: https://github.com/zeebe-io/zeebe/issues/558
    @Category(io.zeebe.UnstableTest.class)
    public void shouldReplicateNewTopic() throws InterruptedException
    {
        // given
        brokers.startBroker(BROKER_1_CLIENT_ADDRESS, BROKER_1_TOML);
        brokers.startBroker(BROKER_2_CLIENT_ADDRESS, BROKER_2_TOML);
        brokers.startBroker(BROKER_3_CLIENT_ADDRESS, BROKER_3_TOML);

        final ZeebeClient client = clientRule.getClient();
        final TopologyObserver observer = new TopologyObserver(client);

        // wait till all members are known before we create the partition => workaround for https://github.com/zeebe-io/zeebe/issues/534
        observer.waitForBrokers(brokers.getBrokerAddresses());

        client.topics().create("foo", 1).execute();
        final TaskEvent taskEvent = client.tasks().create("foo", "bar").execute();
        final int partitionId = taskEvent.getMetadata().getPartitionId();

        final SocketAddress currentLeaderAddress = observer.waitForLeader(partitionId);
        final Broker currentLeader = brokers.get(currentLeaderAddress);

        final Set<SocketAddress> expectedFollowers = brokers.getBrokerAddresses();
        expectedFollowers.remove(currentLeaderAddress);

        // when
        currentLeader.close();

        // then
        final SocketAddress newLeader = observer.waitForLeader(partitionId, expectedFollowers);
        assertThat(expectedFollowers).contains(newLeader);
    }
}
