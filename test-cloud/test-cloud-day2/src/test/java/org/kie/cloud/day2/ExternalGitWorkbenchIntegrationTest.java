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

import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.kie.cloud.api.DeploymentScenarioBuilderFactory;
import org.kie.cloud.api.DeploymentScenarioBuilderFactoryLoader;
import org.kie.cloud.api.deployment.WorkbenchDeployment;
import org.kie.cloud.api.scenario.WorkbenchKieServerScenario;
import org.kie.cloud.api.settings.GitSettings;
import org.kie.cloud.common.provider.WorkbenchClientProvider;
import org.kie.cloud.day2.category.Day2;
import org.kie.cloud.tests.common.AbstractCloudIntegrationTest;
import org.kie.cloud.tests.common.ScenarioDeployer;
import org.kie.cloud.tests.common.client.util.Kjar;
import org.kie.wb.test.rest.client.WorkbenchClient;

@Category({Day2.class})
public class ExternalGitWorkbenchIntegrationTest extends AbstractCloudIntegrationTest {


    private static final String REPOSITORY_NAME = generateNameWithPrefix(ExternalGitWorkbenchIntegrationTest.class.getSimpleName());    

    //public KieDeploymentScenario<?> workbenchKieServerScenario;

    private static WorkbenchClient workbenchClient;

    private static WorkbenchDeployment workbenchDeployment;

    private static GitSettings gitSettings = GitSettings.fromProperties()
        .withRepository(REPOSITORY_NAME, ExternalGitWorkbenchIntegrationTest.class.getResource(PROJECT_SOURCE_FOLDER + "/" );
        //.withRepository(REPOSITORY_NAME, ExternalGitWorkbenchIntegrationTest.class.getResource(PROJECT_SOURCE_FOLDER + "/" + Kjar.HELLO_RULES.getArtifactName()).getFile());

    private static final String GIT_HOOKS_DIR = "/opt/kie/data/git/hooks";

    private static DeploymentScenarioBuilderFactory deploymentScenarioFactory = DeploymentScenarioBuilderFactoryLoader.getInstance();

    @BeforeClass
    public static void setUp() {
        try{
            WorkbenchKieServerScenario workbenchKieServerPersistentGitScenario = deploymentScenarioFactory.getWorkbenchKieServerPersistentScenarioBuilder()
                .withGitHooksDir(GIT_HOOKS_DIR)
                .withGitSettings(gitSettings)
                .build();
            ScenarioDeployer.deployScenario(workbenchKieServerPersistentGitScenario);
            workbenchDeployment = workbenchKieServerPersistentGitScenario.getWorkbenchDeployment();
            workbenchClient = WorkbenchClientProvider.getWorkbenchClient(workbenchDeployment);
        }
        catch(UnsupportedOperationException ex){
            Assume.assumeFalse(ex.getMessage().startsWith("Not supported"));
        }
    }

    /*@AfterClass
    public static void cleanEnvironment() {
        ScenarioDeployer.undeployScenario();
    }
    */

    @Test
    public void sampleTest() {
        /*
        String projectName = "testProject";
        workbenchClient.createSpace("Test space", workbenchDeployment.getUsername());
        workbenchClient.createProject("Test space", projectName, PROJECT_GROUP_ID, "1.0");

        Collection<ProjectResponse> projects = workbenchClient.getProjects(projectName);
        */
        assert(true);
    }

}
