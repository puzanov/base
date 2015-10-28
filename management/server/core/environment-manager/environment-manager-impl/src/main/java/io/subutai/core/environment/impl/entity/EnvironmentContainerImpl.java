package io.subutai.core.environment.impl.entity;


import java.io.Serializable;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

import io.subutai.common.command.CommandCallback;
import io.subutai.common.command.CommandException;
import io.subutai.common.command.CommandResult;
import io.subutai.common.command.RequestBuilder;
import io.subutai.common.environment.Environment;
import io.subutai.common.environment.EnvironmentModificationException;
import io.subutai.common.environment.EnvironmentNotFoundException;
import io.subutai.common.host.ContainerHostState;
import io.subutai.common.host.HostArchitecture;
import io.subutai.common.host.HostInfo;
import io.subutai.common.host.Interface;
import io.subutai.common.host.NullInterface;
import io.subutai.common.metric.ProcessResourceUsage;
import io.subutai.common.peer.ContainerId;
import io.subutai.common.peer.EnvironmentContainerHost;
import io.subutai.common.host.HostInfoModel;
import io.subutai.common.peer.EnvironmentId;
import io.subutai.common.peer.Peer;
import io.subutai.common.peer.PeerException;
import io.subutai.common.peer.PeerId;
import io.subutai.common.protocol.Template;
import io.subutai.common.quota.CpuQuotaInfo;
import io.subutai.common.quota.DiskPartition;
import io.subutai.common.quota.DiskQuota;
import io.subutai.common.quota.RamQuota;
import io.subutai.core.environment.api.EnvironmentManager;


/**
 * Database entity to store environment container host parameters in structured manner.
 */
@Entity
@Table( name = "env_con" )
@Access( AccessType.FIELD )
public class EnvironmentContainerImpl implements EnvironmentContainerHost, Serializable
{
    @Column( name = "peer_id", nullable = false )
    private String peerId;
    @Id
    @Column( name = "host_id", nullable = false )
    private String hostId;
    @Column( name = "hostname", nullable = false )
    private String hostname;
    @Column( name = "containerName", nullable = true )
    private String containerName;
    @Column( name = "node_group_name", nullable = false )
    private String nodeGroupName;
    @Column( name = "creator_peer_id", nullable = false )
    private String creatorPeerId;
    @Column( name = "template_name", nullable = false )
    private String templateName;
    @Column( name = "template_arch", nullable = false )
    private String templateArch;

    @ElementCollection( targetClass = String.class, fetch = FetchType.EAGER )
    private Set<String> tags = new HashSet<>();

    @ManyToOne( targetEntity = EnvironmentImpl.class )
    @JoinColumn( name = "environment_id" )
    private Environment environment;

    @Column( name = "arch", nullable = false )
    @Enumerated
    private HostArchitecture hostArchitecture;

    @OneToMany( mappedBy = "host", fetch = FetchType.EAGER, cascade = CascadeType.ALL, targetEntity =
            HostInterfaceImpl.class, orphanRemoval = true )
    protected Set<Interface> interfaces = new HashSet<>();

    @Column( name = "ssh_group_id" )
    private int sshGroupId;
    @Column( name = "hosts_group_id" )
    private int hostsGroupId;
    @Column( name = "domain_name" )
    private String domainName;


    @Transient
    private Peer peer;

    @Transient
    private EnvironmentManager environmentManager;

    @Transient
    private ContainerId containerId;


    protected EnvironmentContainerImpl()
    {
    }


    public void init()
    {
        // Empty method
    }


    public EnvironmentContainerImpl( final String localPeerId, final Peer peer, final String nodeGroupName,
                                     final HostInfoModel hostInfo, final Template template, int sshGroupId,
                                     int hostsGroupId, String domainName )
    {
        Preconditions.checkNotNull( peer );
        Preconditions.checkArgument( !Strings.isNullOrEmpty( nodeGroupName ) );
        Preconditions.checkArgument( !Strings.isNullOrEmpty( domainName ) );
        Preconditions.checkNotNull( hostInfo );
        Preconditions.checkNotNull( template );


        this.peer = peer;
        this.creatorPeerId = localPeerId;
        this.peerId = peer.getId();
        this.hostId = hostInfo.getId();
        this.hostname = hostInfo.getHostname();
        this.containerName = hostInfo.getContainerName();
        this.hostArchitecture = hostInfo.getArch();
        this.nodeGroupName = nodeGroupName;
        this.templateName = template.getTemplateName();
        this.templateArch = template.getLxcArch();
        this.sshGroupId = sshGroupId;
        this.hostsGroupId = hostsGroupId;
        this.domainName = domainName;
        setNetInterfaces( hostInfo.getInterfaces() );
    }


    public void setNetInterfaces( Set<Interface> interfaces )
    {
        Preconditions.checkNotNull( interfaces );

        this.interfaces.clear();
        for ( Interface iface : interfaces )
        {
            HostInterfaceImpl hostInterface = new HostInterfaceImpl( iface );
            hostInterface.setHost( this );
            this.interfaces.add( hostInterface );
        }
    }


    public void setPeer( final Peer peer )
    {
        Preconditions.checkNotNull( peer );

        this.peer = peer;
    }


    public void setEnvironmentManager( final EnvironmentManager environmentManager )
    {
        Preconditions.checkNotNull( environmentManager );

        this.environmentManager = environmentManager;
    }


    public void setEnvironment( Environment environment )
    {
        Preconditions.checkNotNull( environment );

        this.environment = environment;
    }


    @Override
    public void setDefaultGateway( final String gatewayIp ) throws PeerException
    {
        getPeer().setDefaultGateway( this, gatewayIp );
    }


    @Override
    public boolean isLocal()
    {
        return getPeer().isLocal();
    }


    @Override
    public String getEnvironmentId()
    {
        return environment.getId();
    }


    @Override
    public String getNodeGroupName()
    {
        return this.nodeGroupName;
    }


    @Override
    public ContainerHostState getStatus()
    {
        return getPeer().getContainerState( getContainerId() );
    }


    @Override
    public String getContainerName()
    {
        return containerName;
    }


    @Override
    public void dispose() throws PeerException
    {
        try
        {
            environmentManager.destroyContainer( environment.getId(), this.getId(), false, false );
        }
        catch ( EnvironmentNotFoundException | EnvironmentModificationException e )
        {
            throw new PeerException( e );
        }
    }


    public void destroy() throws PeerException
    {
        getPeer().destroyContainer( getContainerId() );
    }


    @Override
    public void start() throws PeerException
    {
        getPeer().startContainer( getContainerId() );
    }


    @Override
    public void stop() throws PeerException
    {
        getPeer().stopContainer( getContainerId() );
    }


    @Override
    public Peer getPeer()
    {
        return peer;
    }


    @Override
    public Template getTemplate() throws PeerException
    {
        return getPeer().getTemplate( this.templateName );
    }


    @Override
    public String getTemplateName()
    {
        return this.templateName;
    }


    @Override
    public void addTag( final String tag )
    {
        Preconditions.checkArgument( !Strings.isNullOrEmpty( tag ) );
        this.tags.add( tag );
    }


    @Override
    public void removeTag( final String tag )
    {
        Preconditions.checkArgument( !Strings.isNullOrEmpty( tag ) );
        this.tags.remove( tag );
    }


    @Override
    public Set<String> getTags()
    {
        return this.tags;
    }


    @Override
    public String getPeerId()
    {
        return this.peerId;
    }


    @Override
    public String getId()
    {
        return hostId;
    }


    @Override
    public String getHostname()
    {
        return this.hostname;
    }


    public void setHostname( final String hostname )
    {
        this.hostname = hostname;
    }


    @Override
    public CommandResult execute( final RequestBuilder requestBuilder ) throws CommandException
    {
        return getPeer().execute( requestBuilder, this );
    }


    @Override
    public CommandResult execute( final RequestBuilder requestBuilder, final CommandCallback callback )
            throws CommandException
    {
        return getPeer().execute( requestBuilder, this, callback );
    }


    @Override
    public void executeAsync( final RequestBuilder requestBuilder, final CommandCallback callback )
            throws CommandException
    {
        getPeer().executeAsync( requestBuilder, this, callback );
    }


    @Override
    public void executeAsync( final RequestBuilder requestBuilder ) throws CommandException
    {
        getPeer().executeAsync( requestBuilder, this );
    }


    @Override
    public boolean isConnected()
    {
        return ContainerHostState.RUNNING.equals( getStatus() );
    }


    @Override
    public Set<Interface> getInterfaces()
    {
        return interfaces;
    }


    @Override
    public String getIpByInterfaceName( String interfaceName )
    {
        for ( Interface iface : interfaces )
        {
            if ( iface.getName().equalsIgnoreCase( interfaceName ) )
            {
                return iface.getIp();
            }
        }

        return null;
    }


    @Override
    public String getMacByInterfaceName( final String interfaceName )
    {
        for ( Interface iface : interfaces )
        {
            if ( iface.getName().equalsIgnoreCase( interfaceName ) )
            {
                return iface.getMac();
            }
        }

        return null;
    }


    @Override
    public Interface getInterfaceByName( final String interfaceName )
    {
        Interface result = NullInterface.getInstance();
        for ( Iterator<Interface> i = getInterfaces().iterator(); result instanceof NullInterface && i.hasNext(); )
        {
            Interface n = i.next();
            if ( n.getName().equalsIgnoreCase( interfaceName ) )
            {
                result = n;
            }
        }

        return result;
    }


    @Override
    public HostArchitecture getArch()
    {
        return this.hostArchitecture;
    }


    @Override
    public ProcessResourceUsage getProcessResourceUsage( final int processPid ) throws PeerException
    {
        return getPeer().getProcessResourceUsage( getContainerId(), processPid );
    }


    @Override
    public int getRamQuota() throws PeerException
    {
        return getPeer().getRamQuota( this );
    }


    @Override
    public RamQuota getRamQuotaInfo() throws PeerException
    {
        return getPeer().getRamQuotaInfo( this );
    }


    @Override
    public void setRamQuota( final int ramInMb ) throws PeerException
    {
        getPeer().setRamQuota( this, ramInMb );
    }


    @Override
    public int getCpuQuota() throws PeerException
    {
        return getPeer().getCpuQuota( this );
    }


    @Override
    public CpuQuotaInfo getCpuQuotaInfo() throws PeerException
    {
        return getPeer().getCpuQuotaInfo( this );
    }


    @Override
    public void setCpuQuota( final int cpuPercent ) throws PeerException
    {
        getPeer().setCpuQuota( this, cpuPercent );
    }


    @Override
    public Set<Integer> getCpuSet() throws PeerException
    {
        return getPeer().getCpuSet( this );
    }


    @Override
    public void setCpuSet( final Set<Integer> cpuSet ) throws PeerException
    {
        getPeer().setCpuSet( this, cpuSet );
    }


    @Override
    public DiskQuota getDiskQuota( final DiskPartition diskPartition ) throws PeerException
    {
        return getPeer().getDiskQuota( this, diskPartition );
    }


    @Override
    public void setDiskQuota( final DiskQuota diskQuota ) throws PeerException
    {
        getPeer().setDiskQuota( this, diskQuota );
    }


    @Override
    public void setRamQuota( final RamQuota ramQuota ) throws PeerException
    {
        getPeer().setRamQuota( this, ramQuota );
    }


    @Override
    public int getAvailableRamQuota() throws PeerException
    {
        return getPeer().getAvailableRamQuota( this );
    }


    @Override
    public int getAvailableCpuQuota() throws PeerException
    {
        return getPeer().getAvailableCpuQuota( this );
    }


    @Override
    public DiskQuota getAvailableDiskQuota( final DiskPartition diskPartition ) throws PeerException
    {
        return getPeer().getAvailableDiskQuota( this, diskPartition );
    }


    public int getSshGroupId()
    {
        return sshGroupId;
    }


    public int getHostsGroupId()
    {
        return hostsGroupId;
    }


    public String getDomainName()
    {
        return domainName;
    }


    protected void setHostId( String id )
    {
        Preconditions.checkArgument( !Strings.isNullOrEmpty( id ) );
        this.hostId = id;
    }


    @Override
    public String getInitiatorPeerId()
    {
        return this.peerId;
    }


    @Override
    public String getOwnerId()
    {
        throw new UnsupportedOperationException( "Not implemented yet." );
    }


    @Override
    public boolean equals( final Object o )
    {
        if ( this == o )
        {
            return true;
        }
        if ( !( o instanceof EnvironmentContainerImpl ) )
        {
            return false;
        }

        final EnvironmentContainerImpl container = ( EnvironmentContainerImpl ) o;

        if ( hostId != null ? !hostId.equals( container.getId() ) : container.getId() != null )
        {
            return false;
        }

        return true;
    }


    @Override
    public ContainerId getContainerId()
    {
        if ( containerId == null )
        {
            containerId =
                    new ContainerId( getId(), new PeerId( getPeerId() ), new EnvironmentId( getEnvironmentId() ) );
        }
        return containerId;
    }


    @Override
    public int hashCode()
    {
        return hostId != null ? hostId.hashCode() : 0;
    }


    @Override
    public String toString()
    {
        ContainerHostState state = getStatus();

        return MoreObjects.toStringHelper( this ).add( "hostId", hostId ).add( "hostname", hostname )
                          .add( "nodeGroupName", nodeGroupName ).add( "creatorPeerId", creatorPeerId )
                          .add( "templateName", templateName ).add( "environmentId", environment.getId() )
                          .add( "sshGroupId", sshGroupId ).add( "hostsGroupId", hostsGroupId )
                          .add( "domainName", domainName ).add( "tags", tags ).add( "templateArch", templateArch )
                          .add( "hostArchitecture", hostArchitecture ).add( "state", state ).toString();
    }


    @Override
    public int compareTo( final HostInfo o )
    {
        if ( hostname != null && o != null )
        {
            return hostname.compareTo( o.getHostname() );
        }
        return -1;
    }
}
