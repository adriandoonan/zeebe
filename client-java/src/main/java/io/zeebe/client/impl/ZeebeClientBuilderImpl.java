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
package io.zeebe.client.impl;

import static io.zeebe.client.ClientProperties.CLIENT_MAXREQUESTS;
import static io.zeebe.client.ClientProperties.CLIENT_REQUEST_BLOCKTIME_MILLIS;
import static io.zeebe.client.ClientProperties.CLIENT_SENDBUFFER_SIZE;

import java.time.Duration;
import java.util.Properties;

import io.zeebe.client.ClientProperties;
import io.zeebe.client.ZeebeClient;
import io.zeebe.client.ZeebeClientBuilder;
import io.zeebe.client.ZeebeClientConfiguration;

public class ZeebeClientBuilderImpl implements ZeebeClientBuilder, ZeebeClientConfiguration
{
    private String brokerContactPoint = "127.0.0.1:51015";
    private int maxRequests = 128;
    private Duration requestTimeout = Duration.ofSeconds(15);
    private Duration requestBlocktime = Duration.ofSeconds(15);
    private int sendBufferSize = 16;
    private int numManagementThreads = 1;
    private int numSubscriptionExecutionThreads = 1;
    private int topicSubscriptionPrefetchCapacity = 32;
    private Duration tcpChannelKeepAlivePeriod;


    @Override
    public String getBrokerContactPoint()
    {
        return brokerContactPoint;
    }

    @Override
    public ZeebeClientBuilder brokerContactPoint(String contactPoint)
    {
        this.brokerContactPoint = contactPoint;
        return this;
    }

    @Override
    public int getMaxRequests()
    {
        return maxRequests;
    }

    @Override
    public ZeebeClientBuilder maxRequests(int maxRequests)
    {
        this.maxRequests = maxRequests;
        return this;
    }

    @Override
    public Duration getRequestTimeout()
    {
        return requestTimeout;
    }

    @Override
    public ZeebeClientBuilder requestTimeout(Duration requestTimeout)
    {
        this.requestTimeout = requestTimeout;
        return this;
    }

    @Override
    public Duration getRequestBlocktime()
    {
        return requestBlocktime;
    }

    @Override
    public ZeebeClientBuilder requestBlocktime(Duration requestBlockTime)
    {
        this.requestBlocktime = requestBlockTime;
        return this;
    }

    @Override
    public int getSendBufferSize()
    {
        return sendBufferSize;
    }

    @Override
    public ZeebeClientBuilder sendBufferSize(int sendBufferSize)
    {
        this.sendBufferSize = sendBufferSize;
        return this;
    }

    @Override
    public int getNumManagementThreads()
    {
        return numManagementThreads;
    }

    @Override
    public ZeebeClientBuilder numManagementThreads(int numManagementThreads)
    {
        this.numManagementThreads = numManagementThreads;
        return this;
    }

    @Override
    public int getNumSubscriptionExecutionThreads()
    {
        return numSubscriptionExecutionThreads;
    }

    @Override
    public ZeebeClientBuilder numSubscriptionExecutionThreads(int numSubscriptionThreads)
    {
        this.numSubscriptionExecutionThreads = numSubscriptionThreads;
        return this;
    }

    @Override
    public int getTopicSubscriptionPrefetchCapacity()
    {
        return topicSubscriptionPrefetchCapacity;
    }

    @Override
    public ZeebeClientBuilder topicSubscriptionPrefetchCapacity(int topicSubscriptionPrefetchCapacity)
    {
        this.topicSubscriptionPrefetchCapacity = topicSubscriptionPrefetchCapacity;
        return this;
    }

    @Override
    public Duration getTcpChannelKeepAlivePeriod()
    {
        return tcpChannelKeepAlivePeriod;
    }

    @Override
    public ZeebeClientBuilder tcpChannelKeepAlivePeriod(Duration tcpChannelKeepAlivePeriod)
    {
        this.tcpChannelKeepAlivePeriod = tcpChannelKeepAlivePeriod;
        return this;
    }

    @Override
    public ZeebeClient build()
    {
        return new ZeebeClientImpl(this);
    }

    public static ZeebeClientBuilderImpl fromProperties(Properties properties)
    {
        final ZeebeClientBuilderImpl builder = new ZeebeClientBuilderImpl();

        if (properties.containsKey(ClientProperties.BROKER_CONTACTPOINT))
        {
            builder.brokerContactPoint(properties.getProperty(ClientProperties.BROKER_CONTACTPOINT));
        }
        if (properties.containsKey(CLIENT_MAXREQUESTS))
        {
            builder.maxRequests(Integer.parseInt(properties.getProperty(CLIENT_MAXREQUESTS)));
        }
        if (properties.containsKey(ClientProperties.CLIENT_REQUEST_TIMEOUT_SEC))
        {
            builder.requestTimeout(Duration.ofSeconds(Integer.parseInt(properties.getProperty(ClientProperties.CLIENT_REQUEST_TIMEOUT_SEC))));
        }
        if (properties.containsKey(CLIENT_REQUEST_BLOCKTIME_MILLIS))
        {
            builder.requestBlocktime(Duration.ofMillis(Integer.parseInt(properties.getProperty(CLIENT_REQUEST_BLOCKTIME_MILLIS))));
        }
        if (properties.containsKey(CLIENT_SENDBUFFER_SIZE))
        {
            builder.sendBufferSize(Integer.parseInt(properties.getProperty(CLIENT_SENDBUFFER_SIZE)));
        }
        if (properties.containsKey(ClientProperties.CLIENT_MANAGEMENT_THREADS))
        {
            builder.numManagementThreads(Integer.parseInt(properties.getProperty(ClientProperties.CLIENT_MANAGEMENT_THREADS)));
        }
        if (properties.containsKey(ClientProperties.CLIENT_TCP_CHANNEL_KEEP_ALIVE_PERIOD))
        {
            builder.tcpChannelKeepAlivePeriod(Duration.ofMillis(Long.parseLong(properties.getProperty(ClientProperties.CLIENT_TCP_CHANNEL_KEEP_ALIVE_PERIOD))));
        }
        if (properties.containsKey(ClientProperties.CLIENT_SUBSCRIPTION_EXECUTION_THREADS))
        {
            builder.numSubscriptionExecutionThreads(Integer.parseInt(properties.getProperty(ClientProperties.CLIENT_SUBSCRIPTION_EXECUTION_THREADS)));
        }
        if (properties.containsKey(ClientProperties.CLIENT_TOPIC_SUBSCRIPTION_PREFETCH_CAPACITY))
        {
            builder.topicSubscriptionPrefetchCapacity(Integer.parseInt(properties.getProperty(ClientProperties.CLIENT_TOPIC_SUBSCRIPTION_PREFETCH_CAPACITY)));
        }

        return builder;
    }

}
