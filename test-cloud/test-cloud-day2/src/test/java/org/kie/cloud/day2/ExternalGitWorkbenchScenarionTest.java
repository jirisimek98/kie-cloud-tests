/*
 * Copyright 2018 JBoss by Red Hat.
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
package org.kie.cloud.day2;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.kie.cloud.api.deployment.WorkbenchDeployment;
import org.kie.cloud.api.scenario.KieDeploymentScenario;
import org.kie.cloud.api.scenario.WorkbenchKieServerScenario;
import org.kie.cloud.common.provider.WorkbenchClientProvider;
import org.kie.cloud.day2.category.Day2;
import org.kie.cloud.tests.common.AbstractCloudIntegrationTest;
import org.kie.wb.test.rest.client.WorkbenchClient;

@Category({Day2.class})
public class ExternalGitWorkbenchScenarionTest extends AbstractCloudIntegrationTest {


    public String testScenarioName;

    public KieDeploymentScenario<?> workbenchKieServerScenario;

    private WorkbenchClient workbenchClient;

    private WorkbenchDeployment workbenchDeployment;

    private static final String GIT_HOOKS_REMOTE_DIR = "/opt/kie/data/git/hooks";

    WorkbenchKieServerScenario workbenchKieServerPersistentScenario = deploymentScenarioFactory.getWorkbenchKieServerPersistentScenarioBuilder()
                .withGitHooksDir(GIT_HOOKS_REMOTE_DIR)
                .build();

    



    @Before
    public void setUp() {
        workbenchDeployment = workbenchKieServerPersistentScenario.getWorkbenchDeployment();
        workbenchClient = WorkbenchClientProvider.getWorkbenchClient(workbenchDeployment);
    }

    @Test
    public void test() {
        
        String projectName = "testProject";
        workbenchClient.createSpace("Test space", workbenchDeployment.getUsername());
        workbenchClient.createProject("Test space", projectName, PROJECT_GROUP_ID, "1.0");
        

    }

}
