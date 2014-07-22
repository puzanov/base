package org.safehaus.subutai.plugin.zookeeper.impl.handler;

import com.google.common.base.Strings;
import org.safehaus.subutai.api.commandrunner.AgentResult;
import org.safehaus.subutai.api.commandrunner.Command;
import org.safehaus.subutai.api.commandrunner.CommandCallback;
import org.safehaus.subutai.api.lxcmanager.LxcCreateException;
import org.safehaus.subutai.api.lxcmanager.LxcDestroyException;
import org.safehaus.subutai.plugin.zookeeper.api.ZookeeperClusterConfig;
import org.safehaus.subutai.shared.operation.ProductOperation;
import org.safehaus.subutai.plugin.zookeeper.impl.Commands;
import org.safehaus.subutai.plugin.zookeeper.impl.ZookeeperImpl;
import org.safehaus.subutai.shared.operation.AbstractOperationHandler;
import org.safehaus.subutai.shared.protocol.Agent;
import org.safehaus.subutai.shared.protocol.Response;
import org.safehaus.subutai.shared.protocol.Util;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Installs Zookeeper cluster either on newly created lxcs or over hadoop cluster nodes
 */
public class InstallOperationHandler extends AbstractOperationHandler<ZookeeperImpl> {
    private final ProductOperation po;
    private final ZookeeperClusterConfig config;

    public InstallOperationHandler(ZookeeperImpl manager, ZookeeperClusterConfig config) {
        super(manager, config.getClusterName());
        this.config = config;
        po = manager.getTracker().createProductOperation(
                ZookeeperClusterConfig.PRODUCT_KEY, String.format("Installing %s", ZookeeperClusterConfig.PRODUCT_KEY));
    }

    @Override
    public UUID getTrackerId() {
        return po.getId();
    }

    @Override
    public void run() {
        if (Strings.isNullOrEmpty(config.getZkName())
                || Strings.isNullOrEmpty(config.getClusterName())
                //either number of nodes to create or hadoop cluster nodes must be present
                || (config.isStandalone() && config.getNumberOfNodes() <= 0)
                || (!config.isStandalone() && Util.isCollectionEmpty(config.getNodes()))) {
            po.addLogFailed("Malformed configuration\nInstallation aborted");
            return;
        }

        if (manager.getCluster(config.getClusterName()) != null) {
            po.addLogFailed(String.format("Cluster with name '%s' already exists\nInstallation aborted", config.getClusterName()));
            return;
        }

        if (config.isStandalone()) {
            installStandalone();
        } else {
            installOverHadoop();
        }

    }

    /**
     * installs ZK cluster over supplied Hadoop cluster nodes
     */
    private void installOverHadoop() {

        po.addLog("Installing over hadoop cluster nodes");

        //check if node agent is connected
        for (Iterator<Agent> it = config.getNodes().iterator(); it.hasNext(); ) {
            Agent node = it.next();
            if (manager.getAgentManager().getAgentByHostname(node.getHostname()) == null) {
                po.addLog(String.format("Node %s is not connected. Omitting this node from installation", node.getHostname()));
                it.remove();
            }
        }

        if (config.getNodes().isEmpty()) {
            po.addLogFailed("No nodes eligible for installation. Operation aborted");
            return;
        }

        po.addLog("Checking prerequisites...");

        //check installed ksks packages
        Command checkInstalledCommand = Commands.getCheckInstalledCommand(config.getNodes());
        manager.getCommandRunner().runCommand(checkInstalledCommand);

        if (!checkInstalledCommand.hasCompleted()) {
            po.addLogFailed("Failed to check presence of installed ksks packages\nInstallation aborted");
            return;
        }

        for (Iterator<Agent> it = config.getNodes().iterator(); it.hasNext(); ) {
            Agent node = it.next();

            AgentResult result = checkInstalledCommand.getResults().get(node.getUuid());

            if (result.getStdOut().contains("ksks-zookeeper")) {
                po.addLog(String.format("Node %s already has Zookeeper installed. Omitting this node from installation", node.getHostname()));
                it.remove();
            } else if (!result.getStdOut().contains("ksks-hadoop")) {
                po.addLog(String.format("Node %s has no Hadoop installation. Omitting this node from installation", node.getHostname()));
                it.remove();
            }
        }


        if (config.getNodes().isEmpty()) {
            po.addLogFailed("No nodes eligible for installation. Operation aborted");
            return;
        }

        po.addLog("Updating db...");
        //save to db
        if (manager.getDbManager().saveInfo( ZookeeperClusterConfig.PRODUCT_KEY, config.getClusterName(), config)) {
            po.addLog("Cluster info saved to DB\nInstalling Zookeeper...");

            //install
            Command installCommand = Commands.getInstallCommand(config.getNodes());
            manager.getCommandRunner().runCommand(installCommand);

            if (installCommand.hasSucceeded()) {
                po.addLog("Installation succeeded\nUpdating settings...");

                //update settings
                Command updateSettingsCommand = Commands.getUpdateSettingsCommand(config.getZkName(), config.getNodes());
                manager.getCommandRunner().runCommand(updateSettingsCommand);

                if (updateSettingsCommand.hasSucceeded()) {

                    po.addLog(String.format("Settings updated\nStarting %s...", ZookeeperClusterConfig.PRODUCT_KEY));
                    //start all nodes
                    Command startCommand = Commands.getStartCommand(config.getNodes());
                    final AtomicInteger count = new AtomicInteger();
                    manager.getCommandRunner().runCommand(startCommand, new CommandCallback() {
                        @Override
                        public void onResponse(Response response, AgentResult agentResult, Command command) {
                            if (agentResult.getStdOut().contains("STARTED")) {
                                if (count.incrementAndGet() == config.getNodes().size()) {
                                    stop();
                                }
                            }
                        }
                    });

                    if (count.get() == config.getNodes().size()) {
                        po.addLogDone(String.format("Starting %s succeeded\nDone", ZookeeperClusterConfig.PRODUCT_KEY));
                    } else {
                        po.addLogFailed(String.format("Starting %s failed, %s", ZookeeperClusterConfig.PRODUCT_KEY, startCommand.getAllErrors()));
                    }
                } else {
                    po.addLogFailed(String.format(
                            "Failed to update settings, %s\nPlease update settings manually and restart the cluster",
                            updateSettingsCommand.getAllErrors()));
                }
            } else {
                po.addLogFailed(String.format("Installation failed, %s", installCommand.getAllErrors()));
            }
        } else {
            po.addLogFailed("Could not save cluster info to DB! Check logs\nInstallation aborted");
        }
    }

    /**
     * Installs Zk cluster on a newly created set of lxcs
     */
    private void installStandalone() {

        try {
            po.addLog(String.format("Creating %d lxc containers...", config.getNumberOfNodes()));
            Map<Agent, Set<Agent>> lxcAgentsMap = manager.getLxcManager().createLxcs(config.getNumberOfNodes());
            config.setNodes(new HashSet<Agent>());

            for (Map.Entry<Agent, Set<Agent>> entry : lxcAgentsMap.entrySet()) {
                config.getNodes().addAll(entry.getValue());
            }
            po.addLog("Lxc containers created successfully\nUpdating db...");
            if (manager.getDbManager().saveInfo( ZookeeperClusterConfig.PRODUCT_KEY, config.getClusterName(), config)) {

                po.addLog(String.format("Cluster info saved to DB\nInstalling %s...", ZookeeperClusterConfig.PRODUCT_KEY));

                //install
                Command installCommand = Commands.getInstallCommand(config.getNodes());
                manager.getCommandRunner().runCommand(installCommand);

                if (installCommand.hasSucceeded()) {
                    po.addLog("Installation succeeded\nUpdating settings...");

                    //update settings
                    Command updateSettingsCommand = Commands.getUpdateSettingsCommand(config.getZkName(), config.getNodes());
                    manager.getCommandRunner().runCommand(updateSettingsCommand);

                    if (updateSettingsCommand.hasSucceeded()) {

                        po.addLog(String.format("Settings updated\nStarting %s...", ZookeeperClusterConfig.PRODUCT_KEY));
                        //start all nodes
                        Command startCommand = Commands.getStartCommand(config.getNodes());
                        final AtomicInteger count = new AtomicInteger();
                        manager.getCommandRunner().runCommand(startCommand, new CommandCallback() {
                            @Override
                            public void onResponse(Response response, AgentResult agentResult, Command command) {
                                if (agentResult.getStdOut().contains("STARTED")) {
                                    if (count.incrementAndGet() == config.getNodes().size()) {
                                        stop();
                                    }
                                }
                            }
                        });

                        if (count.get() == config.getNodes().size()) {
                            po.addLogDone(String.format("Starting %s succeeded\nDone", ZookeeperClusterConfig.PRODUCT_KEY));
                        } else {
                            po.addLogFailed(String.format("Starting %s failed, %s", ZookeeperClusterConfig.PRODUCT_KEY, startCommand.getAllErrors()));
                        }
                    } else {
                        po.addLogFailed(String.format(
                                "Failed to update settings, %s\nPlease update settings manually and restart the cluster",
                                updateSettingsCommand.getAllErrors()));
                    }
                } else {
                    po.addLogFailed(String.format("Installation failed, %s", installCommand.getAllErrors()));
                }

            } else {
                //destroy all lxcs also
                try {
                    manager.getLxcManager().destroyLxcs(lxcAgentsMap);
                } catch (LxcDestroyException ex) {
                    po.addLogFailed("Could not save cluster info to DB! Please see logs. Use LXC module to cleanup\nInstallation aborted");
                }
                po.addLogFailed("Could not save cluster info to DB! Please see logs\nInstallation aborted");
            }
        } catch (LxcCreateException ex) {
            po.addLogFailed(ex.getMessage());
        }
    }
}
