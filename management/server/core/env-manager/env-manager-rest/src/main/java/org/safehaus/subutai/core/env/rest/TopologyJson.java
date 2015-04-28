package org.safehaus.subutai.core.env.rest;


import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.safehaus.subutai.common.environment.NodeGroup;


public class TopologyJson
{

    private Map<UUID, Set<NodeGroup>> nodeGroupPlacement;


    public Map<UUID, Set<NodeGroup>> getNodeGroupPlacement()
    {
        return nodeGroupPlacement;
    }


    public void setNodeGroupPlacement( final Map<UUID, Set<NodeGroup>> nodeGroupPlacement )
    {
        this.nodeGroupPlacement = nodeGroupPlacement;
    }
}