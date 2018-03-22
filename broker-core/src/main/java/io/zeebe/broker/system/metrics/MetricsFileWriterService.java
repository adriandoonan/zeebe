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
package io.zeebe.broker.system.metrics;

import java.io.File;
import java.time.Duration;

import io.zeebe.broker.system.metrics.cfg.MetricsCfg;
import io.zeebe.servicecontainer.*;
import io.zeebe.util.metrics.MetricsManager;
import io.zeebe.util.sched.ActorScheduler;
import io.zeebe.util.sched.SchedulingHints;

public class MetricsFileWriterService implements Service<MetricsFileWriter>
{
    private final Injector<MetricsManager> metricsManagerInjector = new Injector<>();
    private MetricsFileWriter metricsFileWriter;
    private MetricsCfg cfg;

    public MetricsFileWriterService(MetricsCfg cfg)
    {
        this.cfg = cfg;
    }

    @Override
    public void start(ServiceStartContext startContext)
    {
        final ActorScheduler scheduler = startContext.getScheduler();
        final MetricsManager metricsManager = metricsManagerInjector.getValue();

        final String metricsFileName = new File(cfg.getDirectory(), "zeebe.prom").getAbsolutePath();

        metricsFileWriter = new MetricsFileWriter(Duration.ofSeconds(5), metricsFileName, metricsManager);
        startContext.async(scheduler.submitActor(metricsFileWriter, SchedulingHints.isIoBound(0)));
    }

    @Override
    public void stop(ServiceStopContext stopContext)
    {
        stopContext.async(metricsFileWriter.close());
    }

    @Override
    public MetricsFileWriter get()
    {
        return metricsFileWriter;
    }

    public Injector<MetricsManager> getMetricsManagerInjector()
    {
        return metricsManagerInjector;
    }

}
