/*
 * Copyright 2017, OpenSkywalking Organization All rights reserved.
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
 *
 * Project repository: https://github.com/OpenSkywalking/skywalking
 */

package org.skywalking.apm.collector.stream;

import java.util.LinkedList;
import java.util.List;
import org.skywalking.apm.collector.core.framework.CollectorContextHelper;
import org.skywalking.apm.collector.core.framework.Context;
import org.skywalking.apm.collector.core.framework.DefineException;
import org.skywalking.apm.collector.core.module.SingleModuleInstaller;
import org.skywalking.apm.collector.queue.QueueModuleGroupDefine;
import org.skywalking.apm.collector.stream.worker.AbstractLocalAsyncWorkerProvider;
import org.skywalking.apm.collector.stream.worker.AbstractRemoteWorkerProvider;
import org.skywalking.apm.collector.stream.worker.ClusterWorkerContext;
import org.skywalking.apm.collector.stream.worker.LocalAsyncWorkerProviderDefineLoader;
import org.skywalking.apm.collector.stream.worker.ProviderNotFoundException;
import org.skywalking.apm.collector.stream.worker.RemoteWorkerProviderDefineLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author pengys5
 */
public class StreamModuleInstaller extends SingleModuleInstaller {

    private final Logger logger = LoggerFactory.getLogger(StreamModuleInstaller.class);

    @Override public String groupName() {
        return StreamModuleGroupDefine.GROUP_NAME;
    }

    @Override public Context moduleContext() {
        return new StreamModuleContext(groupName());
    }

    @Override public List<String> dependenceModules() {
        List<String> dependenceModules = new LinkedList<>();
        dependenceModules.add(QueueModuleGroupDefine.GROUP_NAME);
        return dependenceModules;
    }

    @Override public void onAfterInstall() throws DefineException {
        initializeWorker((StreamModuleContext)CollectorContextHelper.INSTANCE.getContext(groupName()));
    }

    private void initializeWorker(StreamModuleContext context) throws DefineException {
        ClusterWorkerContext clusterWorkerContext = new ClusterWorkerContext();
        context.setClusterWorkerContext(clusterWorkerContext);

        LocalAsyncWorkerProviderDefineLoader localAsyncProviderLoader = new LocalAsyncWorkerProviderDefineLoader();
        RemoteWorkerProviderDefineLoader remoteProviderLoader = new RemoteWorkerProviderDefineLoader();
        try {
            List<AbstractLocalAsyncWorkerProvider> localAsyncProviders = localAsyncProviderLoader.load();
            for (AbstractLocalAsyncWorkerProvider provider : localAsyncProviders) {
                provider.setClusterContext(clusterWorkerContext);
                provider.create();
                clusterWorkerContext.putRole(provider.role());
            }

            List<AbstractRemoteWorkerProvider> remoteProviders = remoteProviderLoader.load();
            for (AbstractRemoteWorkerProvider provider : remoteProviders) {
                provider.setClusterContext(clusterWorkerContext);
                clusterWorkerContext.putRole(provider.role());
                clusterWorkerContext.putProvider(provider);
            }
        } catch (ProviderNotFoundException e) {
            logger.error(e.getMessage(), e);
        }
    }
}
