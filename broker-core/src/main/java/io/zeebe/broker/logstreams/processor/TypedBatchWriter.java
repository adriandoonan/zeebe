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
package io.zeebe.broker.logstreams.processor;

import java.util.function.Consumer;

import io.zeebe.msgpack.UnpackedObject;
import io.zeebe.protocol.impl.RecordMetadata;
import io.zeebe.protocol.intent.Intent;

public interface TypedBatchWriter
{

    TypedBatchWriter addRejection(TypedRecord<? extends UnpackedObject> command);
    TypedBatchWriter addRejection(TypedRecord<? extends UnpackedObject> command, Consumer<RecordMetadata> metadata);

    TypedBatchWriter addNewCommand(Intent intent, UnpackedObject value);
    TypedBatchWriter addFollowUpCommand(long key, Intent intent, UnpackedObject value);

    TypedBatchWriter addNewEvent(Intent intent, UnpackedObject value);
    TypedBatchWriter addFollowUpEvent(long key, Intent intent, UnpackedObject value);
    TypedBatchWriter addFollowUpEvent(long key, Intent intent, UnpackedObject value, Consumer<RecordMetadata> metadata);

    /**
     * @return position of new event, negative value on failure
     */
    long write();
}
