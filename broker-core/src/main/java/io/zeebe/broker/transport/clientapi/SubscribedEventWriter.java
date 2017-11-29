/*
 * Zeebe Broker Core
 * Copyright © 2017 camunda services GmbH (info@camunda.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.zeebe.broker.transport.clientapi;

import static io.zeebe.protocol.clientapi.SubscribedEventEncoder.eventHeaderLength;
import static io.zeebe.protocol.clientapi.SubscribedEventEncoder.keyNullValue;
import static io.zeebe.protocol.clientapi.SubscribedEventEncoder.partitionIdNullValue;
import static io.zeebe.protocol.clientapi.SubscribedEventEncoder.positionNullValue;
import static io.zeebe.protocol.clientapi.SubscribedEventEncoder.subscriberKeyNullValue;

import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;

import io.zeebe.protocol.Protocol;
import io.zeebe.protocol.clientapi.EventType;
import io.zeebe.protocol.clientapi.MessageHeaderEncoder;
import io.zeebe.protocol.clientapi.SubscribedEventEncoder;
import io.zeebe.protocol.clientapi.SubscriptionType;
import io.zeebe.transport.ServerOutput;
import io.zeebe.transport.TransportMessage;
import io.zeebe.util.buffer.BufferWriter;
import io.zeebe.util.buffer.DirectBufferWriter;

public class SubscribedEventWriter implements BufferWriter
{
    protected final MessageHeaderEncoder headerEncoder = new MessageHeaderEncoder();
    protected final SubscribedEventEncoder bodyEncoder = new SubscribedEventEncoder();

    protected int partitionId = partitionIdNullValue();
    protected long position = positionNullValue();
    protected long key = keyNullValue();
    protected long subscriberKey = subscriberKeyNullValue();
    protected SubscriptionType subscriptionType;
    protected EventType eventType;
    protected DirectBufferWriter eventBuffer = new DirectBufferWriter();
    protected BufferWriter eventWriter;

    protected final ServerOutput output;
    protected final TransportMessage message = new TransportMessage();

    public SubscribedEventWriter(final ServerOutput output)
    {
        this.output = output;
    }

    public SubscribedEventWriter partitionId(final int partitionId)
    {
        this.partitionId = partitionId;
        return this;
    }

    public SubscribedEventWriter position(final long position)
    {
        this.position = position;
        return this;
    }

    public SubscribedEventWriter key(final long key)
    {
        this.key = key;
        return this;
    }

    public SubscribedEventWriter subscriberKey(final long subscriberKey)
    {
        this.subscriberKey = subscriberKey;
        return this;
    }

    public SubscribedEventWriter subscriptionType(final SubscriptionType subscriptionType)
    {
        this.subscriptionType = subscriptionType;
        return this;
    }

    public SubscribedEventWriter eventType(final EventType eventType)
    {
        this.eventType = eventType;
        return this;
    }

    public SubscribedEventWriter event(final DirectBuffer buffer, final int offset, final int length)
    {
        this.eventBuffer.wrap(buffer, offset, length);
        this.eventWriter = eventBuffer;
        return this;
    }

    public SubscribedEventWriter eventWriter(final BufferWriter eventWriter)
    {
        this.eventWriter = eventWriter;
        return this;
    }

    @Override
    public int getLength()
    {
        return MessageHeaderEncoder.ENCODED_LENGTH +
                SubscribedEventEncoder.BLOCK_LENGTH +
                eventHeaderLength() +
                (eventWriter != null ? eventWriter.getLength() : 0);
    }

    @Override
    public void write(final MutableDirectBuffer buffer, int offset)
    {
        headerEncoder
            .wrap(buffer, offset)
            .blockLength(bodyEncoder.sbeBlockLength())
            .templateId(bodyEncoder.sbeTemplateId())
            .schemaId(bodyEncoder.sbeSchemaId())
            .version(bodyEncoder.sbeSchemaVersion());

        offset += MessageHeaderEncoder.ENCODED_LENGTH;

        bodyEncoder
            .wrap(buffer, offset)
            .partitionId(partitionId)
            .position(position)
            .key(key)
            .subscriberKey(subscriberKey)
            .subscriptionType(subscriptionType)
            .eventType(eventType);

        offset += SubscribedEventEncoder.BLOCK_LENGTH;

        final int eventLength = eventWriter != null ? eventWriter.getLength() : 0;
        buffer.putShort(offset, (short) eventLength, Protocol.ENDIANNESS);

        offset += eventHeaderLength();

        if (eventWriter != null)
        {
            eventWriter.write(buffer, offset);
        }
    }

    public boolean tryWriteMessage(int remoteStreamId)
    {
        try
        {
            message.reset()
                .remoteStreamId(remoteStreamId)
                .writer(this);

            return output.sendMessage(message);
        }
        finally
        {
            reset();
        }
    }

    public SubscribedEventWriter subscriptionTermination(int partition, long subscriberKey)
    {
        reset();
        partitionId(partition)
            .eventType(EventType.NULL_VAL)
            .key(0)
            .position(0)
            .subscriberKey(subscriberKey)
            .subscriptionType(SubscriptionType.TOPIC_SUBSCRIPTION);
        return this;
    }

    protected void reset()
    {
        this.partitionId = partitionIdNullValue();
        this.position = positionNullValue();
        this.key = keyNullValue();
        this.subscriberKey = subscriberKeyNullValue();
        this.subscriptionType = SubscriptionType.NULL_VAL;
        this.eventType = EventType.NULL_VAL;
        this.eventWriter = null;
    }
}
