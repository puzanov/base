package io.subutai.core.network.impl;


import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import io.subutai.common.command.RequestBuilder;
import io.subutai.common.network.LogLevel;
import io.subutai.common.network.ProxyLoadBalanceStrategy;
import io.subutai.common.protocol.LoadBalancing;
import io.subutai.common.protocol.Protocol;


/**
 * Networking commands
 */

public class Commands
{
    private static final String TUNNEL_BINDING = "subutai tunnel";
    private static final String VXLAN_BINDING = "subutai vxlan";
    private static final String P2P_BINDING = "subutai p2p";
    private static final String PROXY_BINDING = "subutai proxy";
    private static final String INFO_BINDING = "subutai info";
    private static final String MAP_BINDING = "subutai map";
    private static final String LOG_BINDING = "subutai log";
    private final SimpleDateFormat p2pDateFormat = new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss" );


    RequestBuilder getGetReservedPortsCommand()
    {
        return new RequestBuilder( INFO_BINDING ).withCmdArgs( Lists.newArrayList( "ports" ) );
    }


    RequestBuilder getGetP2pVersionCommand()
    {
        return new RequestBuilder( P2P_BINDING ).withCmdArgs( Lists.newArrayList( "-v" ) );
    }


    RequestBuilder getP2PConnectionsCommand()
    {
        return new RequestBuilder( P2P_BINDING ).withCmdArgs( Lists.newArrayList( "-p" ) );
    }


    RequestBuilder getJoinP2PSwarmCommand( String interfaceName, String localIp, String p2pHash, String secretKey,
                                           long secretKeyTtlSec, String portRange )
    {
        return new RequestBuilder( P2P_BINDING ).withCmdArgs(
                Lists.newArrayList( "-c", interfaceName, p2pHash, secretKey, String.valueOf( secretKeyTtlSec ), localIp,
                        portRange ) ).withTimeout( 90 );
    }


    RequestBuilder getRemoveP2PSwarmCommand( String p2pHash )
    {
        return new RequestBuilder( P2P_BINDING ).withCmdArgs( Lists.newArrayList( "-d", p2pHash ) ).withTimeout( 90 );
    }


    RequestBuilder getResetP2PSecretKey( String p2pHash, String newSecretKey, long ttlSeconds )
    {
        return new RequestBuilder( P2P_BINDING )
                .withCmdArgs( Lists.newArrayList( "-u", p2pHash, newSecretKey, String.valueOf( ttlSeconds ) ) );
    }


    RequestBuilder getGetP2pLogsCommand( Date from, Date till, LogLevel logLevel )
    {
        List<String> args =
                Lists.newArrayList( "p2p", "-s", p2pDateFormat.format( from ), "-e", p2pDateFormat.format( till ) );

        if ( logLevel != LogLevel.ALL )
        {
            args.add( "-l" );
            args.add( logLevel.getCliParam() );
        }

        return new RequestBuilder( LOG_BINDING ).withCmdArgs( args );
    }


    RequestBuilder getCreateTunnelCommand( String tunnelName, String tunnelIp, int vlan, long vni )
    {
        return new RequestBuilder( VXLAN_BINDING ).withCmdArgs(
                Lists.newArrayList( "-create", tunnelName, "-remoteip", tunnelIp, "-vlan", String.valueOf( vlan ),
                        "-vni", String.valueOf( vni ) ) );
    }


    RequestBuilder getDeleteTunnelCommand( final String tunnelName )
    {
        return new RequestBuilder( VXLAN_BINDING ).withCmdArgs( Lists.newArrayList( "-delete", tunnelName ) );
    }


    RequestBuilder getGetTunnelsCommand()
    {
        return new RequestBuilder( VXLAN_BINDING ).withCmdArgs( Lists.newArrayList( "-list" ) );
    }


    RequestBuilder getGetVlanDomainCommand( int vLanId )
    {
        return new RequestBuilder( PROXY_BINDING )
                .withCmdArgs( Lists.newArrayList( "check", String.valueOf( vLanId ), "-d" ) );
    }


    RequestBuilder getRemoveVlanDomainCommand( final String vLanId )
    {
        return new RequestBuilder( PROXY_BINDING ).withCmdArgs( Lists.newArrayList( "del", vLanId, "-d" ) );
    }


    RequestBuilder getSetVlanDomainCommand( final String vLanId, final String domain,
                                            final ProxyLoadBalanceStrategy proxyLoadBalanceStrategy,
                                            final String sslCertPath )
    {
        List<String> args = Lists.newArrayList( "add", vLanId, "-d", domain );

        if ( proxyLoadBalanceStrategy != ProxyLoadBalanceStrategy.NONE )
        {
            args.add( "-p" );
            args.add( proxyLoadBalanceStrategy.getValue() );
        }

        if ( !Strings.isNullOrEmpty( sslCertPath ) )
        {
            args.add( "-f" );
            args.add( sslCertPath );
        }

        return new RequestBuilder( PROXY_BINDING ).withCmdArgs( args );
    }


    RequestBuilder getCheckIpInVlanDomainCommand( final String hostIp, final int vLanId )
    {
        return new RequestBuilder( PROXY_BINDING )
                .withCmdArgs( Lists.newArrayList( "check", String.valueOf( vLanId ), "-h", hostIp ) );
    }


    RequestBuilder getAddIpToVlanDomainCommand( final String hostIp, final String vLanId )
    {
        return new RequestBuilder( PROXY_BINDING ).withCmdArgs( Lists.newArrayList( "add", vLanId, "-h", hostIp ) );
    }


    RequestBuilder getRemoveIpFromVlanDomainCommand( final String hostIp, final int vLanId )
    {
        return new RequestBuilder( PROXY_BINDING )
                .withCmdArgs( Lists.newArrayList( "del", String.valueOf( vLanId ), "-h", hostIp ) );
    }


    RequestBuilder getSetupContainerSshTunnelCommand( final String containerIp, final int sshIdleTimeout )
    {
        return new RequestBuilder( TUNNEL_BINDING )
                .withCmdArgs( Lists.newArrayList( "add", containerIp, String.valueOf( sshIdleTimeout ) ) );
    }


    RequestBuilder getMapContainerPortToRandomPortCommand( final Protocol protocol, final String containerIp,
                                                           final int containerPort )
    {
        return new RequestBuilder( MAP_BINDING ).withCmdArgs( Lists.newArrayList( protocol.name().toLowerCase(), "-i",
                String.format( "%s:%s", containerIp, containerPort ) ) );
    }


    RequestBuilder getMapContainerPortToSpecificPortCommand( final Protocol protocol, final String containerIp,
                                                             final int containerPort, final int rhPort )
    {
        return new RequestBuilder( MAP_BINDING ).withCmdArgs( Lists.newArrayList( protocol.name().toLowerCase(), "-i",
                String.format( "%s:%s", containerIp, containerPort ), "-e", String.valueOf( rhPort ) ) );
    }


    RequestBuilder getRemoveContainerPortMappingCommand( final Protocol protocol, final String containerIp,
                                                         final int containerPort, final int rhPort )
    {
        return new RequestBuilder( MAP_BINDING ).withCmdArgs( Lists.newArrayList( protocol.name().toLowerCase(), "-i",
                String.format( "%s:%s", containerIp, containerPort ), "-e", String.valueOf( rhPort ), "-r" ) );
    }


    RequestBuilder getMapContainerPortToDomainCommand( final Protocol protocol, final String containerIp,
                                                       final int containerPort, final int rhPort, final String domain,
                                                       final String sslCertPath, final LoadBalancing loadBalancing,
                                                       final boolean sslBackend )
    {
        List<String> args = Lists.newArrayList( protocol.name().toLowerCase(), "-i",
                String.format( "%s:%s", containerIp, containerPort ), "-e", String.valueOf( rhPort ), "-d", domain );

        if ( !Strings.isNullOrEmpty( sslCertPath ) )
        {
            args.add( "-c" );
            args.add( sslCertPath );
        }

        if ( sslBackend )
        {
            args.add( "--sslbackend" );
        }

        if ( loadBalancing != null )
        {
            args.add( "-p" );
            args.add( loadBalancing.name().toLowerCase() );
        }

        return new RequestBuilder( MAP_BINDING ).withCmdArgs( args );
    }


    RequestBuilder getRemoveContainerPortDomainMappingCommand( final Protocol protocol, final String containerIp,
                                                               final int containerPort, final int rhPort,
                                                               final String domain )
    {
        return new RequestBuilder( MAP_BINDING ).withCmdArgs( Lists.newArrayList( protocol.name().toLowerCase(), "-i",
                String.format( "%s:%s", containerIp, containerPort ), "-e", String.valueOf( rhPort ), "-d", domain,
                "-r" ) );
    }


    RequestBuilder getListOfReservedPortMappingCommand()
    {
        return new RequestBuilder( MAP_BINDING ).withCmdArgs( Lists.newArrayList( "-l" ) );
    }


    RequestBuilder getListPortMappingsCommand( final Protocol protocol )
    {
        List<String> args = Lists.newArrayList( "-l" );

        if ( protocol != null )
        {
            args.add( protocol.name().toLowerCase() );
        }

        return new RequestBuilder( MAP_BINDING ).withCmdArgs( args );
    }


    RequestBuilder getGetIPAddressCommand()
    {
        return new RequestBuilder( INFO_BINDING ).withCmdArgs( Lists.newArrayList( "ipaddr" ) );
    }
}
