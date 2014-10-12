package org.safehaus.subutai.plugin.hive.api;


import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.safehaus.subutai.common.protocol.Agent;
import org.safehaus.subutai.common.protocol.ConfigBase;


public class HiveConfig implements ConfigBase
{

    public static final String PRODUCT_KEY = "Hive";
    public static final String TEMPLATE_NAME = "hadoophive";

    private SetupType setupType;
    private String clusterName = "";
    private String hadoopClusterName = "";
    private Agent server;
    private Set<Agent> clients = new HashSet();
    private Set<Agent> hadoopNodes = new HashSet<>();


    public SetupType getSetupType()
    {
        return setupType;
    }


    public void setSetupType( SetupType setupType )
    {
        this.setupType = setupType;
    }


    @Override
    public String getClusterName()
    {
        return clusterName;
    }


    public void setClusterName( String clusterName )
    {
        this.clusterName = clusterName;
    }


    @Override
    public String getProductName()
    {
        return PRODUCT_KEY;
    }

    @Override
    public String getProductKey() {
        return PRODUCT_KEY;
    }


    public String getHadoopClusterName()
    {
        return hadoopClusterName;
    }


    public void setHadoopClusterName( String hadoopClusterName )
    {
        this.hadoopClusterName = hadoopClusterName;
    }


    public Agent getServer()
    {
        return server;
    }


    public void setServer( Agent server )
    {
        this.server = server;
    }


    public Set<Agent> getClients()
    {
        return clients;
    }


    public void setClients( Set<Agent> clients )
    {
        this.clients = clients;
    }


    public Set<Agent> getHadoopNodes()
    {
        return hadoopNodes;
    }


    public void setHadoopNodes( Set<Agent> hadoopNodes )
    {
        this.hadoopNodes = hadoopNodes;
    }


    public Set<Agent> getAllNodes()
    {
        Set<Agent> allNodes = new HashSet<>();
        if ( clients != null )
        {
            allNodes.addAll( clients );
        }
        if ( server != null )
        {
            allNodes.add( server );
        }
        return allNodes;
    }


    @Override
    public int hashCode()
    {
        int hash = 7;
        hash = 29 * hash + Objects.hashCode( this.clusterName );
        return hash;
    }


    @Override
    public boolean equals( Object obj )
    {
        if ( obj instanceof HiveConfig )
        {
            HiveConfig o = ( HiveConfig ) obj;
            return clusterName != null ? clusterName.equals( o.clusterName ) : false;
        }
        return false;
    }


    @Override
    public String toString()
    {
        return "Config{" + "clusterName=" + clusterName + ", server=" + server + ", clients=" + ( clients != null ?
                                                                                                  clients.size() : 0 )
                + '}';
    }
}
