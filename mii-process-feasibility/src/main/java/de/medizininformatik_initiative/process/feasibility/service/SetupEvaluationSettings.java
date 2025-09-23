package de.medizininformatik_initiative.process.feasibility.service;

import de.medizininformatik_initiative.process.feasibility.FeasibilitySettings;
import de.medizininformatik_initiative.process.feasibility.ParentOrganizationSelector;
import dev.dsf.bpe.v1.ProcessPluginApi;
import dev.dsf.bpe.v1.activity.AbstractServiceDelegate;
import dev.dsf.bpe.v1.variables.Variables;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import java.util.Objects;

import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.*;

public class SetupEvaluationSettings extends AbstractServiceDelegate
        implements InitializingBean, ParentOrganizationSelector {
    private static final Logger logger = LoggerFactory.getLogger(SetupEvaluationSettings.class);

    private final FeasibilitySettings settings;

    public SetupEvaluationSettings(FeasibilitySettings settings, ProcessPluginApi api) {
        super(api);
        this.settings = settings;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        super.afterPropertiesSet();
        Objects.requireNonNull(settings, "settings");
    }

    @Override
    protected void doExecute(DelegateExecution execution, Variables variables) {
        logger.info("doExecute setup evaluation settings");

        var requester = getRequesterOrganization(variables, api);
        var parentId = getParentOrganizationId(requester, settings, api);

        variables.setString(VARIABLE_REQUESTER_PARENT_ORGANIZATION, parentId);
        variables.setBoolean(VARIABLE_EVALUATION_OBFUSCATION, settings.networks().get(parentId).obfuscate());

        variables.setBoolean(VARIABLE_FEASIBILITY_DISTRIBUTION,
                settings.networks().get(parentId).distributeAsBroker());
        variables.setBoolean(VARIABLE_FEASIBILITY_DISTRIBUTION_AS_SUBSCRIBER,
                settings.networks().get(parentId).distributeAsSubscriber());
    }

}
