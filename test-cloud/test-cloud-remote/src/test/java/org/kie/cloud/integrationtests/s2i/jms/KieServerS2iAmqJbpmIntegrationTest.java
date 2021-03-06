/*
 * Copyright 2019 JBoss by Red Hat.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
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
package org.kie.cloud.integrationtests.s2i.jms;

import java.util.List;

import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.kie.cloud.api.scenario.WorkbenchRuntimeSmartRouterImmutableKieServerAmqWithDatabaseScenario;
import org.kie.cloud.api.settings.GitSettings;
import org.kie.cloud.common.provider.KieServerClientProvider;
import org.kie.cloud.git.GitUtils;
import org.kie.cloud.integrationtests.category.JBPMOnly;
import org.kie.cloud.integrationtests.category.MonitoringK8sFs;
import org.kie.cloud.integrationtests.s2i.KieServerS2iJbpmIntegrationTest;
import org.kie.cloud.integrationtests.testproviders.HttpsKieServerTestProvider;
import org.kie.cloud.integrationtests.testproviders.HttpsWorkbenchTestProvider;
import org.kie.cloud.tests.common.AbstractCloudIntegrationTest;
import org.kie.cloud.tests.common.ScenarioDeployer;
import org.kie.cloud.tests.common.client.util.Kjar;
import org.kie.cloud.tests.common.time.Constants;
import org.kie.server.api.model.KieContainerResource;
import org.kie.server.api.model.ReleaseId;
import org.kie.server.api.model.instance.ProcessInstance;
import org.kie.server.api.model.instance.TaskSummary;
import org.kie.server.client.KieServicesClient;
import org.kie.server.client.ProcessServicesClient;
import org.kie.server.client.UserTaskServicesClient;

import static org.assertj.core.api.Assertions.assertThat;

@Category({JBPMOnly.class, MonitoringK8sFs.class})
public class KieServerS2iAmqJbpmIntegrationTest extends AbstractCloudIntegrationTest {

    private static final String REPOSITORY_NAME = generateNameWithPrefix("KieServerS2iAmqJbpmRepository");

    private static WorkbenchRuntimeSmartRouterImmutableKieServerAmqWithDatabaseScenario deploymentScenario;

    private static HttpsKieServerTestProvider httpsKieServerTestProvider;
    private static HttpsWorkbenchTestProvider httpsWorkbenchTestProvider;

    private KieServicesClient kieServicesClient;
    private ProcessServicesClient processServicesClient;
    private UserTaskServicesClient taskServicesClient;

    private static final String KIE_CONTAINER_DEPLOYMENT = CONTAINER_ID + "=" + Kjar.DEFINITION.toString();

    private static final String REPO_BRANCH = "master";
    private static final String PROJECT_SOURCE_FOLDER = "/kjars-sources";

    @BeforeClass
    public static void initializeDeployment() {
        try {
            deploymentScenario = deploymentScenarioFactory.getWorkbenchRuntimeSmartRouterImmutableKieServerAmqWithPostgreSqlScenarioBuilder()
                    .withContainerDeployment(KIE_CONTAINER_DEPLOYMENT)
                    .withGitSettings(GitSettings.fromProperties()
                                     .withRepository(REPOSITORY_NAME,
                                                     KieServerS2iJbpmIntegrationTest.class.getResource(PROJECT_SOURCE_FOLDER).getFile()))
                    .withSourceLocation(REPO_BRANCH, DEFINITION_PROJECT_NAME)
                    .build();
        } catch (UnsupportedOperationException ex) {
            Assume.assumeFalse(ex.getMessage().startsWith("Not supported"));
        }

        deploymentScenario.setLogFolderName(KieServerS2iJbpmIntegrationTest.class.getSimpleName());
        ScenarioDeployer.deployScenario(deploymentScenario);

        // Setup test providers
        httpsKieServerTestProvider = HttpsKieServerTestProvider.create();
        httpsWorkbenchTestProvider = HttpsWorkbenchTestProvider.create();
    }

    @AfterClass
    public static void cleanEnvironment() {
        GitUtils.deleteGitRepository(REPOSITORY_NAME, deploymentScenario);
        ScenarioDeployer.undeployScenario(deploymentScenario);
    }

    @Before
    public void setUp() {
        kieServicesClient = KieServerClientProvider.getKieServerJmsClient(deploymentScenario.getAmqDeployment().getTcpSslUrl());
        processServicesClient = KieServerClientProvider.getProcessJmsClient(kieServicesClient);
        taskServicesClient = KieServerClientProvider.getTaskJmsClient(kieServicesClient);
    }

    @Test
    public void testContainerAfterExecServerS2IStart() {
        List<KieContainerResource> containers = kieServicesClient.listContainers().getResult().getContainers();
        assertThat(containers).isNotNull().hasSize(1);

        KieContainerResource container = containers.get(0);
        assertThat(container).isNotNull();
        assertThat(container.getContainerId()).isNotNull().isEqualTo(CONTAINER_ID);

        ReleaseId containerReleaseId = container.getResolvedReleaseId();
        assertThat(containerReleaseId).isNotNull();
        assertThat(containerReleaseId.getGroupId()).isNotNull().isEqualTo(PROJECT_GROUP_ID);
        assertThat(containerReleaseId.getArtifactId()).isNotNull().isEqualTo(DEFINITION_PROJECT_NAME);
        assertThat(containerReleaseId.getVersion()).isNotNull().isEqualTo(DEFINITION_PROJECT_VERSION);

        Long processId = processServicesClient.startProcess(CONTAINER_ID, Constants.ProcessId.USERTASK);
        assertThat(processId).isNotNull();

        List<TaskSummary> tasks = taskServicesClient.findTasks(Constants.User.YODA, 0, 10);
        assertThat(tasks).isNotNull().hasSize(1);

        taskServicesClient.completeAutoProgress(CONTAINER_ID, tasks.get(0).getId(), Constants.User.YODA, null);

        ProcessInstance userTaskPi = processServicesClient.getProcessInstance(CONTAINER_ID, processId);
        assertThat(userTaskPi).isNotNull();
        assertThat(userTaskPi.getState()).isEqualTo(org.kie.api.runtime.process.ProcessInstance.STATE_COMPLETED);
    }

    @Test
    public void testKieServerHttps() {
        httpsKieServerTestProvider.testKieServerInfo(deploymentScenario.getKieServerDeployment(), false);
    }

    @Test
    public void testWorkbenchHttps() {
        httpsWorkbenchTestProvider.testLoginScreen(deploymentScenario.getWorkbenchRuntimeDeployment(), false);
    }
}
