package org.safehaus.subutai.plugin.hadoop.api;


import java.util.List;
import java.util.UUID;

import org.safehaus.subutai.common.enums.NodeState;
import org.safehaus.subutai.common.protocol.CompleteEvent;
import org.safehaus.subutai.common.tracker.OperationState;
import org.safehaus.subutai.common.tracker.TrackerOperationView;
import org.safehaus.subutai.core.peer.api.ContainerHost;
import org.safehaus.subutai.core.tracker.api.Tracker;
import org.safehaus.subutai.plugin.common.api.OperationType;
import org.safehaus.subutai.plugin.common.impl.AbstractNodeOperationTask;


public class NodeOperationTask extends AbstractNodeOperationTask implements Runnable
{
    private final String clusterName;
    private final ContainerHost containerHost;
    private final CompleteEvent completeEvent;
    private final Hadoop hadoop;
    private final Tracker tracker;
    private OperationType operationType;


    public NodeOperationTask( Hadoop hadoop, Tracker tracker, String clusterName, ContainerHost containerHost,
                              OperationType operationType, CompleteEvent completeEvent, UUID trackID )
    {
        super( tracker, operationType, hadoop.getCluster( clusterName ), completeEvent, trackID, containerHost );
        this.hadoop = hadoop;
        this.tracker = tracker;
        this.clusterName = clusterName;
        this.containerHost = containerHost;
        this.completeEvent = completeEvent;
        this.operationType = operationType;
    }


    @Override
    public UUID runTask()
    {
        UUID trackID = null;
        List<NodeType> roles = HadoopClusterConfig.getNodeRoles( hadoop.getCluster( clusterName ), containerHost );

        for ( NodeType role : roles )
        {
            switch ( role )
            {
                case NAMENODE:
                    switch ( operationType )
                    {
                        case START:
                            trackID = hadoop.startNameNode( hadoop.getCluster( clusterName ) );
                            break;
                        case STOP:
                            trackID = hadoop.stopNameNode( hadoop.getCluster( clusterName ) );
                            break;
                        case STATUS:
                            trackID = hadoop.statusNameNode( hadoop.getCluster( clusterName ) );
                            break;
                    }
                    break;
                case JOBTRACKER:
                    switch ( operationType )
                    {
                        case START:
                            trackID = hadoop.startJobTracker( hadoop.getCluster( clusterName ) );
                            break;
                        case STOP:
                            trackID = hadoop.stopJobTracker( hadoop.getCluster( clusterName ) );
                            break;
                        case STATUS:
                            trackID = hadoop.statusJobTracker( hadoop.getCluster( clusterName ) );
                            break;
                    }
                    break;
                case SECONDARY_NAMENODE:
                    switch ( operationType )
                    {
                        case STATUS:
                            trackID = hadoop.statusSecondaryNameNode( hadoop.getCluster( clusterName ) );
                            break;
                    }
                    break;
                case DATANODE:
                    switch ( operationType )
                    {
                        case START:
                            trackID = hadoop.startDataNode( hadoop.getCluster( clusterName ),
                                    containerHost.getHostname() );
                            break;
                        case STOP:
                            trackID = hadoop.stopDataNode( hadoop.getCluster( clusterName ),
                                    containerHost.getHostname() );
                            break;
                        case STATUS:
                            trackID = hadoop.statusDataNode( hadoop.getCluster( clusterName ),
                                    containerHost.getHostname() );
                            break;
                    }
                    break;
                case TASKTRACKER:
                    switch ( operationType )
                    {
                        case START:
                            trackID = hadoop.startTaskTracker( hadoop.getCluster( clusterName ),
                                    containerHost.getHostname() );
                            break;
                        case STOP:
                            trackID = hadoop.stopTaskTracker( hadoop.getCluster( clusterName ),
                                    containerHost.getHostname() );
                            break;
                        case STATUS:
                            trackID = hadoop.statusTaskTracker( hadoop.getCluster( clusterName ),
                                    containerHost.getHostname() );
                            break;
                    }
                    break;
            }
        }
        return trackID;
    }


    @Override
    public String getProductStoppedIdentifier()
    {
        return NodeState.STOPPED.name();
    }


    @Override
    public String getProductRunningIdentifier()
    {
        return NodeState.RUNNING.name();
    }
}
