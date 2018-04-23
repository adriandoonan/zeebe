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
package io.zeebe.broker.system.configuration;

import java.io.File;

import io.zeebe.util.FileUtil;

public abstract class DirectoryCfg extends GlobalCfgSupport
{
    private static final String SUB_DIRECTORY_NAME_PATTERN = "%s" + File.separator + "%s";

    protected String directory;

    @Override
    public void applyGlobalConfiguration(GlobalCfg globalConfiguration)
    {
        String localDirectory = directory;

        if (localDirectory == null || localDirectory.isEmpty())
        {
            final String globalDirectory = globalConfiguration.getDirectory();
            final String subDirectory = componentDirectoryName();

            if (subDirectory != null && !subDirectory.isEmpty())
            {
                localDirectory = String.format(SUB_DIRECTORY_NAME_PATTERN, globalDirectory, subDirectory);
            }
            else
            {
                localDirectory = globalDirectory;
            }
        }

        directory = FileUtil.getCanonicalPath(localDirectory);
    }

    protected String componentDirectoryName()
    {
        return null;
    }

    public String getDirectory()
    {
        return directory;
    }

    public void setDirectory(String directory)
    {
        this.directory = directory;
    }
}
