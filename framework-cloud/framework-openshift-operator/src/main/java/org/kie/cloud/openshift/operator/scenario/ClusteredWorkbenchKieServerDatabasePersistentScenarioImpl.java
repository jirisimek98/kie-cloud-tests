/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/

package org.kie.cloud.openshift.operator.scenario;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import cz.xtf.wait.SimpleWaiter;
import org.kie.cloud.api.deployment.ControllerDeployment;
import org.kie.cloud.api.deployment.DatabaseDeployment;
import org.kie.cloud.api.deployment.Deployment;
import org.kie.cloud.api.deployment.KieServerDeployment;
import org.kie.cloud.api.deployment.SmartRouterDeployment;
import org.kie.cloud.api.deployment.SsoDeployment;
import org.kie.cloud.api.deployment.WorkbenchDeployment;
import org.kie.cloud.api.deployment.constants.DeploymentConstants;
import org.kie.cloud.api.scenario.ClusteredWorkbenchKieServerDatabasePersistentScenario;
import org.kie.cloud.api.scenario.KieServerWithExternalDatabaseScenario;
import org.kie.cloud.openshift.deployment.DatabaseDeploymentImpl;
import org.kie.cloud.openshift.deployment.KieServerDeploymentImpl;
import org.kie.cloud.openshift.deployment.WorkbenchDeploymentImpl;
import org.kie.cloud.openshift.operator.model.KieApp;
import org.kie.cloud.openshift.operator.model.components.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClusteredWorkbenchKieServerDatabasePersistentScenarioImpl extends OpenShiftOperatorScenario<ClusteredWorkbenchKieServerDatabasePersistentScenario> implements ClusteredWorkbenchKieServerDatabasePersistentScenario {

    private KieApp kieApp;

    private WorkbenchDeploymentImpl workbenchDeployment;
    private KieServerDeploymentImpl kieServerDeployment;
    private DatabaseDeploymentImpl databaseDeployment;

    private static final Logger logger = LoggerFactory.getLogger(KieServerWithExternalDatabaseScenario.class);

    public ClusteredWorkbenchKieServerDatabasePersistentScenarioImpl(KieApp kieApp) {
        this.kieApp = kieApp;
    }

    @Override
    public WorkbenchDeployment getWorkbenchDeployment() {
        return workbenchDeployment;
    }

    @Override
    public KieServerDeployment getKieServerDeployment() {
        return kieServerDeployment;
    }

    @Override
    public DatabaseDeployment getDatabaseDeployment() {
        return databaseDeployment;
    }

    @Override
    protected void deployCustomResource() {
        registerCustomTrustedSecret(kieApp.getSpec().getObjects().getConsole());
        for (Server server : kieApp.getSpec().getObjects().getServers()) {
            registerCustomTrustedSecret(server);
        }

        // deploy application
        getKieAppClient().create(kieApp);

        workbenchDeployment = new WorkbenchDeploymentImpl(project);
        workbenchDeployment.setUsername(DeploymentConstants.getWorkbenchUser());
        workbenchDeployment.setPassword(DeploymentConstants.getWorkbenchPassword());

        kieServerDeployment = new KieServerDeploymentImpl(project);
        kieServerDeployment.setUsername(DeploymentConstants.getKieServerUser());
        kieServerDeployment.setPassword(DeploymentConstants.getKieServerPassword());

        databaseDeployment = new DatabaseDeploymentImpl(project);

        logger.info("Waiting until all services are created.");
        try {
            new SimpleWaiter(() -> workbenchDeployment.isReady()).reason("Waiting for Workbench service to be created.").timeout(TimeUnit.MINUTES, 1).execute();
            new SimpleWaiter(() -> kieServerDeployment.isReady()).reason("Waiting for Kie server service to be created.").timeout(TimeUnit.MINUTES, 1).execute();
            new SimpleWaiter(() -> databaseDeployment.isReady()).reason("Waiting for Database service to be created.").timeout(TimeUnit.MINUTES, 1).execute();
        } catch (TimeoutException e) {
            throw new RuntimeException("Timeout while deploying application.", e);
        }

        logger.info("Waiting for Database deployment to become ready.");
        databaseDeployment.waitForScale();

        logger.info("Waiting for Workbench deployment to become ready.");
        workbenchDeployment.waitForScale();

        logger.info("Waiting for Kie server deployment to become ready.");
        kieServerDeployment.waitForScale();

        logNodeNameOfAllInstances();
    }

    @Override public List<Deployment> getDeployments() {
        List<Deployment> deployments = new ArrayList<Deployment>(Arrays.asList(workbenchDeployment, kieServerDeployment, databaseDeployment));
        deployments.removeAll(Collections.singleton(null));
        return deployments;
    }

    @Override
    public List<WorkbenchDeployment> getWorkbenchDeployments() {
        return Arrays.asList(workbenchDeployment);
    }

    @Override
    public List<KieServerDeployment> getKieServerDeployments() {
        return Arrays.asList(kieServerDeployment);
    }

    @Override
    public List<SmartRouterDeployment> getSmartRouterDeployments() {
        return Collections.emptyList();
    }

    @Override
    public List<ControllerDeployment> getControllerDeployments() {
        return Collections.emptyList();
    }

    @Override
    public SsoDeployment getSsoDeployment() {
        throw new UnsupportedOperationException("Not implemented yet");
	}
}