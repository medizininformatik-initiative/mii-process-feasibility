package de.medizininformatik_initiative.process.feasibility;

import de.medizininformatik_initiative.process.feasibility.client.flare.FlareWebserviceClient;
import dev.dsf.bpe.v2.ProcessPluginDeploymentListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import java.util.List;

import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.FEASIBILITY_EXECUTE_PROCESS_ID;
import static java.util.Objects.requireNonNull;

/**
 * After process deployment starts a connection test if the <i>feasibilityExecute</i> process is active.
 * <p>
 * The configured {@link EvaluationStrategy} determines which client is used for the connection test.
 * </p>
 *
 * @author <a href="mailto:math2306@hotmail.com">Mathias Rühle</a>
 */
public class FeasibilityProcessPluginDeploymentStateListener
        implements ProcessPluginDeploymentListener, InitializingBean {

    private static final Logger logger = LoggerFactory.getLogger(FeasibilityProcessPluginDeploymentStateListener.class);

    private EvaluationStrategy strategy;
    private FlareWebserviceClient flareWebserviceClient;

    public FeasibilityProcessPluginDeploymentStateListener(EvaluationStrategy strategy,
            FlareWebserviceClient flareWebserviceClient) {
        this.strategy = strategy;
        this.flareWebserviceClient = flareWebserviceClient;
    }

    @Override
    public void onProcessesDeployed(List<String> processes) {
        if (processes.contains(FEASIBILITY_EXECUTE_PROCESS_ID)) {
            if (strategy.equals(EvaluationStrategy.STRUCTURED_QUERY)) {
                try {
                    flareWebserviceClient.testConnection();
                    logger.info("Feasibility plugin connection test to flare SUCCEEDED.");
                } catch (Exception e) {
                    logger.error("Feasibility plugin connection test to flare FAILED. Error: {} - {}",
                            e.getClass().getName(), e.getMessage());
                }
            }
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        requireNonNull(flareWebserviceClient);
    }
}
