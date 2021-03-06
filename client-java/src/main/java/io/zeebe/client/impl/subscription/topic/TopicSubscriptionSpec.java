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
package io.zeebe.client.impl.subscription.topic;

import org.agrona.collections.Long2LongHashMap;
import io.zeebe.client.impl.record.UntypedRecordImpl;
import io.zeebe.util.CheckedConsumer;

public class TopicSubscriptionSpec
{

    protected final String topic;
    protected final CheckedConsumer<UntypedRecordImpl> handler;
    protected final boolean forceStart;
    protected final String name;
    protected final int bufferSize;
    protected final long defaultStartPosition;
    protected final Long2LongHashMap startPositions;

    public TopicSubscriptionSpec(
            String topic,
            CheckedConsumer<UntypedRecordImpl> handler,
            long defaultStartPosition,
            Long2LongHashMap startPositions,
            boolean forceStart,
            String name,
            int bufferSize)
    {
        this.topic = topic;
        this.handler = handler;
        this.defaultStartPosition = defaultStartPosition;
        this.startPositions = startPositions;
        this.forceStart = forceStart;
        this.name = name;
        this.bufferSize = bufferSize;
    }

    public String getTopic()
    {
        return topic;
    }

    public CheckedConsumer<UntypedRecordImpl> getHandler()
    {
        return handler;
    }

    public long getStartPosition(int partitionId)
    {
        if (startPositions.containsKey(partitionId))
        {
            return startPositions.get(partitionId);
        }
        else
        {
            return defaultStartPosition;
        }
    }

    public boolean isForceStart()
    {
        return forceStart;
    }
    public String getName()
    {
        return name;
    }
    public int getBufferSize()
    {
        return bufferSize;
    }

    @Override
    public String toString()
    {
        final StringBuilder builder = new StringBuilder();
        builder.append("[topic=");
        builder.append(topic);
        builder.append(", handler=");
        builder.append(handler);
        builder.append(", defaultStartPosition=");
        builder.append(defaultStartPosition);
        builder.append(", startPositions=");
        builder.append(startPositions);
        builder.append(", forceStart=");
        builder.append(forceStart);
        builder.append(", name=");
        builder.append(name);
        builder.append(", bufferSize=");
        builder.append(bufferSize);
        builder.append("]");
        return builder.toString();
    }


}
