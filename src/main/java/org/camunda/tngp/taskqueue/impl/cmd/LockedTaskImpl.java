package org.camunda.tngp.taskqueue.impl.cmd;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

import org.camunda.tngp.taskqueue.client.cmd.LockedTask;

import uk.co.real_logic.agrona.MutableDirectBuffer;
import uk.co.real_logic.agrona.concurrent.UnsafeBuffer;
import uk.co.real_logic.agrona.io.DirectBufferInputStream;

public class LockedTaskImpl implements LockedTask
{
    protected long id;

    protected UnsafeBuffer payloadBuffer;

    public LockedTaskImpl(int payloadLength)
    {
        payloadBuffer = new UnsafeBuffer(new byte[payloadLength]);
    }

    public void setId(long taskId)
    {
        this.id = taskId;

    }

    @Override
    public long getId()
    {
        return id;
    }

    public MutableDirectBuffer getPayloadBuffer()
    {
        return payloadBuffer;
    }

    @Override
    public int payloadLength()
    {
        return payloadBuffer.capacity();
    }

    @Override
    public int putPayload(ByteBuffer buffer)
    {
        final int bytesWritten = Math.min(buffer.remaining(), payloadLength());

        payloadBuffer.getBytes(0, buffer, bytesWritten);

        return bytesWritten;
    }

    @Override
    public int putPayload(MutableDirectBuffer buffer, int offset, int lenght)
    {
        final int bytesWritten = Math.min(lenght, payloadLength());

        payloadBuffer.getBytes(0, buffer, offset, bytesWritten);

        return bytesWritten;
    }

    @Override
    public byte[] getPayloadBytes()
    {
        return payloadBuffer.byteArray();
    }

    @Override
    public String getPayloadString()
    {
        return new String(getPayloadBytes(), StandardCharsets.UTF_8);
    }

    @Override
    public InputStream getPayloadStream()
    {
        return new DirectBufferInputStream(payloadBuffer);
    }

}