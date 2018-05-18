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

import io.zeebe.protocol.Protocol;
import io.zeebe.protocol.clientapi.ExecuteCommandResponseEncoder;
import io.zeebe.protocol.clientapi.MessageHeaderEncoder;
import io.zeebe.protocol.clientapi.RecordType;
import io.zeebe.protocol.clientapi.ValueType;
import io.zeebe.protocol.intent.Intent;
import io.zeebe.transport.ServerOutput;
import io.zeebe.transport.ServerResponse;
import io.zeebe.util.buffer.BufferWriter;
import org.agrona.MutableDirectBuffer;

import java.util.Objects;

import static io.zeebe.protocol.clientapi.ExecuteCommandResponseEncoder.*;

public class CommandResponseWriter implements BufferWriter
{
    protected final MessageHeaderEncoder messageHeaderEncoder = new MessageHeaderEncoder();
    protected final ExecuteCommandResponseEncoder responseEncoder = new ExecuteCommandResponseEncoder();

    protected int partitionId = partitionIdNullValue();
    protected long position = positionNullValue();
    private long sourceRecordPosition = sourceRecordPositionNullValue();
    protected long key = keyNullValue();
    protected long timestamp = timestampNullValue();
    private RecordType recordType = RecordType.NULL_VAL;
    private ValueType valueType = ValueType.NULL_VAL;
    private short intent = Intent.NULL_VAL;

    protected BufferWriter valueWriter;
    protected final ServerResponse response = new ServerResponse();
    protected final ServerOutput output;

    public CommandResponseWriter(final ServerOutput output)
    {
        this.output = output;
    }

    public CommandResponseWriter recordType(RecordType recordType)
    {
        this.recordType = recordType;
        return this;
    }

    public CommandResponseWriter intent(Intent intent)
    {
        this.intent = intent.value();
        return this;
    }

    public CommandResponseWriter valueType(ValueType valueType)
    {
        this.valueType = valueType;
        return this;
    }

    public CommandResponseWriter partitionId(final int partitionId)
    {
        this.partitionId = partitionId;
        return this;
    }

    public CommandResponseWriter position(final long position)
    {
        this.position = position;
        return this;
    }

    public CommandResponseWriter sourcePosition(final long sourcePosition)
    {
        this.sourceRecordPosition = sourcePosition;
        return this;
    }

    public CommandResponseWriter key(final long key)
    {
        this.key = key;
        return this;
    }

    public CommandResponseWriter timestamp(final long timestamp)
    {
        this.timestamp = timestamp;
        return this;
    }

    public CommandResponseWriter valueWriter(final BufferWriter writer)
    {
        this.valueWriter = writer;
        return this;
    }

    public boolean tryWriteResponse(int remoteStreamId, long requestId)
    {
        Objects.requireNonNull(valueWriter);

        try
        {
            response.reset()
                .remoteStreamId(remoteStreamId)
                .requestId(requestId)
                .writer(this);

            return output.sendResponse(response);
        }
        finally
        {
            reset();
        }
    }

    @Override
    public void write(final MutableDirectBuffer buffer, int offset)
    {
        // protocol header
        messageHeaderEncoder
            .wrap(buffer, offset)
            .blockLength(responseEncoder.sbeBlockLength())
            .templateId(responseEncoder.sbeTemplateId())
            .schemaId(responseEncoder.sbeSchemaId())
            .version(responseEncoder.sbeSchemaVersion());

        offset += messageHeaderEncoder.encodedLength();

        // protocol message
        responseEncoder
            .wrap(buffer, offset)
            .recordType(recordType)
            .partitionId(partitionId)
            .position(position)
            .sourceRecordPosition(sourceRecordPosition)
            .valueType(valueType)
            .intent(intent)
            .key(key)
            .timestamp(timestamp);

        offset = responseEncoder.limit();

        final int eventLength = valueWriter.getLength();
        buffer.putShort(offset, (short) eventLength, Protocol.ENDIANNESS);

        offset += valueHeaderLength();
        valueWriter.write(buffer, offset);
    }

    @Override
    public int getLength()
    {
        return MessageHeaderEncoder.ENCODED_LENGTH +
                ExecuteCommandResponseEncoder.BLOCK_LENGTH +
                valueHeaderLength() +
                valueWriter.getLength();
    }

    protected void reset()
    {
        partitionId = partitionIdNullValue();
        key = keyNullValue();
        position = positionNullValue();
        sourceRecordPosition = sourceRecordPositionNullValue();
        valueWriter = null;
        recordType = RecordType.NULL_VAL;
        intent = Intent.NULL_VAL;
        valueType = ValueType.NULL_VAL;
        timestamp = timestampNullValue();
    }

}
