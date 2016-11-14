/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.camunda.tngp.broker.event;

import org.camunda.tngp.broker.log.LogManager;
import org.camunda.tngp.servicecontainer.Injector;
import org.camunda.tngp.servicecontainer.Service;
import org.camunda.tngp.servicecontainer.ServiceStartContext;
import org.camunda.tngp.servicecontainer.ServiceStopContext;

public class EventContextService implements Service<EventContext>
{
    protected Injector<LogManager> logManagerInjector = new Injector<>();

    protected EventContext context = new EventContext();

    @Override
    public void start(ServiceStartContext ctx)
    {
        context.setLogManager(logManagerInjector.getValue());
    }

    @Override
    public void stop(ServiceStopContext ctx)
    {
        // nothing to do
    }

    @Override
    public EventContext get()
    {
        return context;
    }

    public Injector<LogManager> getLogManagerInjector()
    {
        return logManagerInjector;
    }

}