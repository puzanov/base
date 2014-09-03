package org.safehaus.subutai.plugin.hadoop.impl;


import org.safehaus.subutai.common.exception.ClusterSetupException;
import org.safehaus.subutai.common.protocol.Agent;
import org.safehaus.subutai.common.protocol.ClusterSetupStrategy;
import org.safehaus.subutai.common.protocol.PlacementStrategy;
import org.safehaus.subutai.common.tracker.ProductOperation;
import org.safehaus.subutai.core.command.api.Command;
import org.safehaus.subutai.core.container.api.lxcmanager.LxcDestroyException;
import org.safehaus.subutai.core.environment.api.exception.EnvironmentBuildException;
import org.safehaus.subutai.core.environment.api.helper.Environment;
import org.safehaus.subutai.plugin.hadoop.api.HadoopClusterConfig;
import org.safehaus.subutai.plugin.hadoop.api.NodeType;
import org.safehaus.subutai.plugin.hadoop.impl.operation.common.InstallHadoopOperation;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;


/**
 * This is a hadoop cluster init strategy.
 */
public class HadoopSetupStrategy implements ClusterSetupStrategy {

    private Environment environment;
    private HadoopImpl hadoopManager;
    private ProductOperation po;
    private HadoopClusterConfig hadoopClusterConfig;


    public HadoopSetupStrategy( ProductOperation po, HadoopImpl hadoopManager,
                                HadoopClusterConfig hadoopClusterConfig ) {
        Preconditions.checkNotNull( hadoopClusterConfig, "Hadoop cluster config is null" );
        Preconditions.checkNotNull( po, "Product operation tracker is null" );
        Preconditions.checkNotNull( hadoopManager, "Hadoop manager is null" );

        this.hadoopManager = hadoopManager;
        this.po = po;
        this.hadoopClusterConfig = hadoopClusterConfig;
    }


    public HadoopSetupStrategy( ProductOperation po, HadoopImpl hadoopManager, HadoopClusterConfig hadoopClusterConfig,
                                Environment environment ) {
        Preconditions.checkNotNull( hadoopClusterConfig, "Hadoop cluster config is null" );
        Preconditions.checkNotNull( environment, "Environment is null" );
        Preconditions.checkNotNull( po, "Product operation tracker is null" );
        Preconditions.checkNotNull( hadoopManager, "Hadoop manager is null" );

        this.hadoopManager = hadoopManager;
        this.po = po;
        this.hadoopClusterConfig = hadoopClusterConfig;
        this.environment = environment;
    }


    public static PlacementStrategy getNodePlacementStrategyByNodeType( NodeType nodeType ) {
        switch ( nodeType ) {
            case MASTER_NODE:
                return PlacementStrategy.MORE_RAM;
            case SLAVE_NODE:
                return PlacementStrategy.MORE_HDD;
            default:
                return PlacementStrategy.ROUND_ROBIN;
        }
    }


    @Override
    public HadoopClusterConfig setup() throws ClusterSetupException {

        if ( hadoopClusterConfig == null ||
                Strings.isNullOrEmpty( hadoopClusterConfig.getClusterName() ) ||
                Strings.isNullOrEmpty( hadoopClusterConfig.getDomainName() ) ) {
            po.addLogFailed( "Malformed configuration\nHadoop installation aborted" );
        }
        else {
            //check if mongo cluster with the same name already exists
            if ( hadoopManager.getCluster( hadoopClusterConfig.getClusterName() ) != null ) {
                po.addLogFailed( String.format( "Cluster with name '%s' already exists\nInstallation aborted",
                        hadoopClusterConfig.getClusterName() ) );
            }
            else {
                try {
                    po.addLog(
                            String.format( "Creating %d servers...", hadoopClusterConfig.getCountOfSlaveNodes() + 3 ) );

                    EnvironmentConfig config;
                    if ( environment == null ) {
                        config = new EnvironmentConfig( hadoopClusterConfig,
                                hadoopManager.getDefaultEnvironmentBlueprint( hadoopClusterConfig ) );
                    }
                    else {
                        config = new EnvironmentConfig( hadoopClusterConfig, environment );
                    }
                    hadoopClusterConfig = config.init();


                    po.addLog( "Lxc containers created successfully" );

                    //continue installation here
                    installHadoopCluster();

                    po.addLogDone( String.format( "Cluster '%s' \nInstallation finished",
                            hadoopClusterConfig.getClusterName() ) );
                }
                catch ( EnvironmentBuildException e ) {
                    destroyLXC( po,
                            "Destroying lxc containers after cluster installation failure.\n" + e.getMessage() );
                }
            }
        }

        return hadoopClusterConfig;
    }


    private void installHadoopCluster() throws ClusterSetupException {

        po.addLog( "Hadoop installation started" );
        if ( HadoopImpl.getDbManager().saveInfo( HadoopClusterConfig.PRODUCT_KEY, hadoopClusterConfig.getClusterName(),
                hadoopClusterConfig ) ) {
            po.addLog( "Cluster info saved to DB" );

            InstallHadoopOperation installOperation = new InstallHadoopOperation( hadoopClusterConfig );
            for ( Command command : installOperation.getCommandList() ) {
                po.addLog( ( String.format( "%s started...", command.getDescription() ) ) );
                HadoopImpl.getCommandRunner().runCommand( command );

                if ( command.hasSucceeded() ) {
                    po.addLog( String.format( "%s succeeded", command.getDescription() ) );
                }
                else {
                    po.addLogFailed(
                            String.format( "%s failed, %s", command.getDescription(), command.getAllErrors() ) );
                }
            }
        }
        else {
            destroyLXC( po, "Could not save cluster info to DB! Please see logs\nInstallation aborted" );
        }
    }


    private void destroyLXC( ProductOperation po, String log ) {
        //destroy all lxcs also
        po.addLog( "Destroying lxc containers" );
        try {
            for ( Agent agent : hadoopClusterConfig.getAllNodes() ) {
                HadoopImpl.getContainerManager().cloneDestroy( agent.getParentHostName(), agent.getHostname() );
            }
            po.addLog( "Lxc containers successfully destroyed" );
        }
        catch ( LxcDestroyException ex ) {
            po.addLog( String.format( "%s, skipping...", ex.getMessage() ) );
        }
        po.addLogFailed( log );
    }
}
