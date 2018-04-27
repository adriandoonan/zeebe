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
package io.zeebe.client.subscription;

import org.agrona.concurrent.Agent;

public class SubscriptionExecutor implements Agent
{
    public static final String ROLE_NAME = "subscription-executor";

    protected final EventSubscribers topicSubscriptions;
    protected final EventSubscribers taskSubscriptions;

    public SubscriptionExecutor(EventSubscribers topicSubscriptions, EventSubscribers taskSubscriptions)
    {
        this.topicSubscriptions = topicSubscriptions;
        this.taskSubscriptions = taskSubscriptions;
    }

    @Override
    public int doWork() throws Exception
    {
        int workCount = topicSubscriptions.pollManagedSubscribers();
        workCount += taskSubscriptions.pollManagedSubscribers();
        return workCount;
    }

    @Override
    public String roleName()
    {
        return ROLE_NAME;
    }

}