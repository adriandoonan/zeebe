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
package io.zeebe.client.impl.command;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.zeebe.client.api.commands.TopicSubscriptionCommand;
import io.zeebe.client.api.record.RecordMetadata;
import io.zeebe.client.api.record.ZeebeObjectMapper;
import io.zeebe.client.impl.record.TopicSubscriptionRecordImpl;

public class TopicSubscriptionCommandImpl extends TopicSubscriptionRecordImpl implements TopicSubscriptionCommand
{
    private TopicSubscriptionCommandName commandName;

    @JsonCreator
    public TopicSubscriptionCommandImpl(@JacksonInject ZeebeObjectMapper objectMapper)
    {
        super(objectMapper, RecordMetadata.RecordType.COMMAND);
    }

    public TopicSubscriptionCommandImpl(TopicSubscriptionCommandName commandName)
    {
        super(null, RecordMetadata.RecordType.COMMAND);

        this.commandName = commandName;
    }

    @Override
    public TopicSubscriptionCommandName getCommandName()
    {
        return commandName;
    }

    @Override
    protected void mapIntent(String intent)
    {
        commandName = TopicSubscriptionCommandName.valueOf(intent);
    }

    @Override
    public String toString()
    {
        final StringBuilder builder = new StringBuilder();
        builder.append("TopicSubscriptionCommand [command=");
        builder.append(commandName);
        builder.append(", name=");
        builder.append(getName());
        builder.append(", ackPosition=");
        builder.append(getAckPosition());
        builder.append("]");
        return builder.toString();
    }

}