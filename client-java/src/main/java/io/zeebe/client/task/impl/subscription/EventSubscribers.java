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
package io.zeebe.client.task.impl.subscription;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.ToIntFunction;

import org.agrona.collections.Int2ObjectHashMap;
import org.agrona.collections.Long2ObjectHashMap;
import org.slf4j.Logger;

import io.zeebe.client.impl.Loggers;
import io.zeebe.transport.RemoteAddress;

@SuppressWarnings("rawtypes")
public class EventSubscribers
{
    protected static final Logger LOGGER = Loggers.SUBSCRIPTION_LOGGER;

    // partitionId => subscriberKey => subscription (subscriber keys are not guaranteed to be globally unique)
    protected Int2ObjectHashMap<Long2ObjectHashMap<EventSubscriber>> activeSubscribers = new Int2ObjectHashMap<>();

    protected final List<EventSubscriber> subscribers = new CopyOnWriteArrayList<>();
    protected final List<EventSubscriberGroup> pollableSubscriberGroups = new CopyOnWriteArrayList<>();
    protected final List<EventSubscriberGroup> managedSubscriberGroups = new CopyOnWriteArrayList<>();

    public void addGroup(final EventSubscriberGroup subscription)
    {
        if (subscription.isManagedGroup())
        {
            addManagedGroup(subscription);
        }
        else
        {
            addPollableGroup(subscription);
        }
    }

    public void addSubscriber(final EventSubscriber subscriber)
    {
        this.subscribers.add(subscriber);
    }

    public void removeSubscriber(final EventSubscriber subscriber)
    {
        this.subscribers.remove(subscriber);
    }

    protected void addPollableGroup(final EventSubscriberGroup subscription)
    {
        this.pollableSubscriberGroups.add(subscription);
    }

    protected void addManagedGroup(final EventSubscriberGroup subscription)
    {
        this.managedSubscriberGroups.add(subscription);
    }

    public void closeAllGroups()
    {
        for (final EventSubscriberGroup group : pollableSubscriberGroups)
        {
            closeGroup(group);
        }

        for (final EventSubscriberGroup group : managedSubscriberGroups)
        {
            closeGroup(group);
        }
    }

    protected void closeGroup(EventSubscriberGroup group)
    {
        try
        {
            group.close();
        }
        catch (final Exception e)
        {
            // TODO: log error with useful subscription summary and reason
//            LOGGER.error("Unable to close subscription with key: " + subscription.getSubscriberKey(), e);
        }
    }

    public void reopenSubscriptionsForRemote(RemoteAddress remoteAddress)
    {
        forAllDoConsume(managedSubscriberGroups, s ->
        {
            s.reopenSubscriptionsForRemote(remoteAddress);
        });

        forAllDoConsume(pollableSubscriberGroups, s ->
        {
            s.reopenSubscriptionsForRemote(remoteAddress);
        });
    }

    public void activate(EventSubscriber subscription)
    {
        this.activeSubscribers
            .computeIfAbsent(subscription.getPartitionId(), partitionId -> new Long2ObjectHashMap<>())
            .put(subscription.getSubscriberKey(), subscription);
    }

    public void deactivate(final EventSubscriber subscription)
    {
        final int partitionId = subscription.getPartitionId();

        final Long2ObjectHashMap<EventSubscriber> subscriptionsForPartition = activeSubscribers.get(partitionId);
        if (subscriptionsForPartition != null)
        {
            subscriptionsForPartition.remove(subscription.getSubscriberKey());

            if (subscriptionsForPartition.isEmpty())
            {
                activeSubscribers.remove(partitionId);
            }
        }

    }

    public void removeGroup(EventSubscriberGroup group)
    {
        pollableSubscriberGroups.remove(group);
        managedSubscriberGroups.remove(group);
    }

    public EventSubscriber getSubscriber(final int partitionId, final long subscriberKey)
    {
        final Long2ObjectHashMap<EventSubscriber> subscriptionsForPartition = activeSubscribers.get(partitionId);

        if (subscriptionsForPartition != null)
        {
            return subscriptionsForPartition.get(subscriberKey);
        }

        return null;
    }

    public int maintainState()
    {
        int workCount = forAllDo(managedSubscriberGroups, s -> s.maintainState());
        workCount += forAllDo(pollableSubscriberGroups, s -> s.maintainState());
        return workCount;
    }

    protected int forAllDo(List<EventSubscriberGroup> groups, ToIntFunction<EventSubscriberGroup> action)
    {
        int workCount = 0;

        for (EventSubscriberGroup subscription : groups)
        {
            workCount += action.applyAsInt(subscription);
        }

        return workCount;
    }

    protected void forAllDoConsume(List<EventSubscriberGroup> groups, Consumer<EventSubscriberGroup> action)
    {
        for (EventSubscriberGroup subscription : groups)
        {
            action.accept(subscription);
        }
    }

    public int pollManagedSubscriptions()
    {
        return forAllDo(managedSubscriberGroups, s -> s.poll());
    }

    public boolean isAnySubscriberOpening()
    {
        for (EventSubscriber subscriber : subscribers)
        {
            if (subscriber.isOpening())
            {
                return true;
            }
        }

        return false;
    }
}
