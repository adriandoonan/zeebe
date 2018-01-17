package io.zeebe.broker.it.clustering;

import static io.zeebe.test.util.TestUtil.doRepeatedly;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.InjectableValues;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.zeebe.broker.Broker;
import io.zeebe.broker.clustering.handler.Topology;
import io.zeebe.broker.it.ClientRule;
import io.zeebe.client.ZeebeClient;
import io.zeebe.client.clustering.impl.TopicLeader;
import io.zeebe.client.clustering.impl.TopologyResponse;
import io.zeebe.client.impl.data.MsgPackConverter;
import io.zeebe.gossip.Loggers;
import io.zeebe.model.bpmn.Bpmn;
import io.zeebe.model.bpmn.instance.WorkflowDefinition;
import io.zeebe.test.util.AutoCloseableRule;
import io.zeebe.transport.SocketAddress;
import io.zeebe.util.buffer.BufferUtil;
import org.agrona.DirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import org.agrona.io.DirectBufferInputStream;
import org.junit.*;
import org.junit.rules.ExpectedException;
import org.junit.rules.Timeout;
import org.msgpack.jackson.dataformat.MessagePackFactory;

/**
 *
 */
public class GossipClusteringTest
{
    private static final int PARTITION_COUNT = 5;

    @Rule
    public AutoCloseableRule closeables = new AutoCloseableRule();

    @Rule
    public ClientRule clientRule = new ClientRule(false);

    @Rule
    public Timeout timeout = new Timeout(15, TimeUnit.SECONDS);

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private ZeebeClient client;
    private List<Broker> brokers;

    @Before
    public void startUp()
    {
        client = clientRule.getClient();
        brokers = new ArrayList<>();
    }

    @Test
    public void shouldStartCluster()
    {
        // given

        // when
        brokers.add(startBroker("zeebe.cluster.1.cfg.toml"));
        brokers.add(startBroker("zeebe.cluster.2.cfg.toml"));
        brokers.add(startBroker("zeebe.cluster.3.cfg.toml"));

        // then wait until cluster is ready
        final List<SocketAddress> topologyBrokers =
            doRepeatedly(() -> client.requestTopology().execute().getBrokers())
                .until(topologyBroker -> topologyBroker != null && topologyBroker.size() == 3);

        assertThat(topologyBrokers).containsExactly(new SocketAddress("localhost", 51015),
                                                    new SocketAddress("localhost", 41015),
                                                    new SocketAddress("localhost", 31015));
    }

    @Test
    public void shouldDistributePartitionsAndLeaderInformationInCluster()
    {
        // given
        brokers.add(startBroker("zeebe.cluster.1.cfg.toml"));
        brokers.add(startBroker("zeebe.cluster.2.cfg.toml"));
        brokers.add(startBroker("zeebe.cluster.3.cfg.toml"));

        doRepeatedly(() -> client.requestTopology().execute().getBrokers())
            .until(topologyBroker -> topologyBroker != null && topologyBroker.size() == 3);

        // when
        client.topics().create("test", PARTITION_COUNT).execute();

        // then
        final List<TopicLeader> topicLeaders = doRepeatedly(() -> client.requestTopology()
                                                                 .execute()
                                                                 .getTopicLeaders()).until(leaders -> leaders != null && leaders.size() >= 5);
        final long partitionLeaderCount = topicLeaders.stream()
                                                      .filter((leader) -> leader.getTopicName().equals("test"))
                                                      .count();
        assertThat(partitionLeaderCount).isEqualTo(PARTITION_COUNT);
    }

    @Test
    public void shouldRemoveMemberFromTopology()
    {
        // given
        brokers.add(startBroker("zeebe.cluster.1.cfg.toml"));
        brokers.add(startBroker("zeebe.cluster.2.cfg.toml"));
        brokers.add(startBroker("zeebe.cluster.3.cfg.toml"));

        doRepeatedly(() -> client.requestTopology().execute().getBrokers())
            .until(topologyBroker -> topologyBroker != null && topologyBroker.size() == 3);

        // when
        final Broker removedBroker = brokers.remove(2);
        removedBroker.close();

        // then
        final List<SocketAddress> topologyBrokers =
            doRepeatedly(() -> client.requestTopology().execute().getBrokers())
                .until(topologyBroker -> topologyBroker != null && topologyBroker.size() == 2);

        assertThat(topologyBrokers).containsExactly(new SocketAddress("localhost", 51015),
                                                    new SocketAddress("localhost", 41015));
    }

    @Test
    @Ignore("https://github.com/zeebe-io/zeebe/issues/617")
    public void shouldRemoveLeaderFromCluster()
    {
        // given
        brokers.add(startBroker("zeebe.cluster.1.cfg.toml"));
        brokers.add(startBroker("zeebe.cluster.2.cfg.toml"));
        brokers.add(startBroker("zeebe.cluster.3.cfg.toml"));

        doRepeatedly(() -> client.requestTopology().execute().getBrokers())
            .until(topologyBroker -> topologyBroker != null && topologyBroker.size() == 3);

        // when
        final Broker removedBroker = brokers.remove(0);
        removedBroker.close();

        // then
        final List<SocketAddress> topologyBrokers =
            doRepeatedly(() -> client.requestTopology().execute().getBrokers())
                .until(topologyBroker -> topologyBroker != null && topologyBroker.size() == 2);

        assertThat(topologyBrokers).containsExactly(new SocketAddress("localhost", 51015),
                                                    new SocketAddress("localhost", 41015));
    }

    @Test
    public void shouldAddLaterToCluster()
    {
        // given
        brokers.add(startBroker("zeebe.cluster.1.cfg.toml"));
        brokers.add(startBroker("zeebe.cluster.2.cfg.toml"));

        doRepeatedly(() -> client.requestTopology().execute().getBrokers())
            .until(topologyBroker -> topologyBroker != null && topologyBroker.size() == 2);

        // when
        brokers.add(startBroker("zeebe.cluster.3.cfg.toml"));

        // then
        final List<SocketAddress> topologyBrokers =
            doRepeatedly(() -> client.requestTopology().execute().getBrokers())
                .until(topologyBroker -> topologyBroker != null && topologyBroker.size() == 3);

        assertThat(topologyBrokers).containsExactly(new SocketAddress("localhost", 51015),
                                                    new SocketAddress("localhost", 41015),
                                                    new SocketAddress("localhost", 31015));
    }

    @Test
    public void shouldReAddToCluster()
    {
        // given
        brokers.add(startBroker("zeebe.cluster.1.cfg.toml"));
        brokers.add(startBroker("zeebe.cluster.2.cfg.toml"));
        brokers.add(startBroker("zeebe.cluster.3.cfg.toml"));

        doRepeatedly(() -> client.requestTopology().execute().getBrokers())
            .until(topologyBroker -> topologyBroker != null && topologyBroker.size() == 3);

        final Broker removedBroker = brokers.remove(2);
        removedBroker.close();

        doRepeatedly(() -> client.requestTopology().execute().getBrokers())
            .until(topologyBroker -> topologyBroker != null && topologyBroker.size() == 2);

        // when
        brokers.add(startBroker("zeebe.cluster.3.cfg.toml"));

        // then
        final List<SocketAddress> topologyBrokers =
            doRepeatedly(() -> client.requestTopology().execute().getBrokers())
                .until(topologyBroker -> topologyBroker != null && topologyBroker.size() == 3);

        assertThat(topologyBrokers).containsExactly(new SocketAddress("localhost", 51015),
                                                    new SocketAddress("localhost", 41015),
                                                    new SocketAddress("localhost", 31015));
    }

    private Broker startBroker(String configFile)
    {
        final InputStream config = this.getClass().getClassLoader().getResourceAsStream(configFile);
        final Broker broker = new Broker(config);
        closeables.manage(broker);

        return broker;
    }
}
