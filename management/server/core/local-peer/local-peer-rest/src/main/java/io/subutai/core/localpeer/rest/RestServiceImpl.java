package io.subutai.core.localpeer.rest;


import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.karaf.bundle.core.BundleState;
import org.apache.karaf.bundle.core.BundleStateService;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

import io.subutai.common.environment.Nodes;
import io.subutai.common.host.HostId;
import io.subutai.common.metric.ResourceHostMetrics;
import io.subutai.common.network.NetworkResourceImpl;
import io.subutai.common.network.UsedNetworkResources;
import io.subutai.common.peer.AlertEvent;
import io.subutai.common.peer.EnvironmentId;
import io.subutai.common.peer.LocalPeer;
import io.subutai.common.peer.PeerId;
import io.subutai.common.peer.PeerInfo;
import io.subutai.common.protocol.P2PConfig;
import io.subutai.common.protocol.P2PCredentials;
import io.subutai.common.protocol.P2pIps;
import io.subutai.common.security.PublicKeyContainer;
import io.subutai.common.security.crypto.pgp.PGPKeyUtil;
import io.subutai.common.security.relation.RelationLinkDto;
import io.subutai.common.util.DateTimeParam;
import io.subutai.common.util.ServiceLocator;


public class RestServiceImpl implements RestService
{
    private static final Logger LOGGER = LoggerFactory.getLogger( RestServiceImpl.class );
    private static final int BUNDLE_COUNT = 278;

    private final LocalPeer localPeer;


    public RestServiceImpl( final LocalPeer localPeer )
    {
        Preconditions.checkNotNull( localPeer );

        this.localPeer = localPeer;
    }


    @Override
    public Response isMhPresent()
    {
        return localPeer.isMHPresent() ? Response.ok().build() :
               Response.status( Response.Status.SERVICE_UNAVAILABLE ).build();
    }


    @Override
    public Response isInited()
    {
        return localPeer.isInitialized() ? Response.ok().build() :
               Response.status( Response.Status.SERVICE_UNAVAILABLE ).build();
    }


    @Override
    public Response isReady()
    {
        boolean failed = false;

        boolean ready = true;

        BundleContext ctx = FrameworkUtil.getBundle( RestServiceImpl.class ).getBundleContext();

        BundleStateService bundleStateService = ServiceLocator.lookup( BundleStateService.class );

        Bundle[] bundles = ctx.getBundles();

        if ( bundles.length < BUNDLE_COUNT )
        {
            LOGGER.warn( "Bundle count is {}", bundles.length );

            return Response.status( Response.Status.SERVICE_UNAVAILABLE ).build();
        }

        for ( Bundle bundle : bundles )
        {
            if ( bundleStateService.getState( bundle ) == BundleState.Failure )
            {
                failed = true;

                break;
            }

            if ( !( ( bundle.getState() == Bundle.ACTIVE ) || ( bundle.getState() == Bundle.RESOLVED ) ) )
            {
                ready = false;

                break;
            }
        }

        return failed ? Response.serverError().build() :
               ready ? Response.ok().build() : Response.status( Response.Status.SERVICE_UNAVAILABLE ).build();
    }


    @Override
    public Response ping()
    {
        return Response.ok().build();
    }


    @Override
    public PeerInfo getPeerInfo()
    {
        try
        {
            return localPeer.getPeerInfo();
        }
        catch ( Exception e )
        {
            LOGGER.error( e.getMessage(), e );
            throw new WebApplicationException( Response.serverError().entity( e.getMessage() ).build() );
        }
    }


    @Override
    public Response getEnvironmentContainers( final EnvironmentId environmentId )
    {
        try
        {
            Preconditions.checkNotNull( environmentId );

            return Response.ok( localPeer.getEnvironmentContainers( environmentId ) ).build();
        }
        catch ( Exception e )
        {
            LOGGER.error( e.getMessage(), e );
            throw new WebApplicationException( Response.serverError().entity( e.getMessage() ).build() );
        }
    }


    @Override
    public PublicKeyContainer createEnvironmentKeyPair( final RelationLinkDto environmentId )
    {
        try
        {
            Preconditions.checkNotNull( environmentId );

            return localPeer.createPeerEnvironmentKeyPair( environmentId );
        }
        catch ( Exception e )
        {
            LOGGER.error( e.getMessage(), e );
            throw new WebApplicationException( Response.serverError().entity( e.getMessage() ).build() );
        }
    }


    @Override
    public void updateEnvironmentKey( final PublicKeyContainer publicKeyContainer )
    {
        try
        {
            Preconditions.checkNotNull( publicKeyContainer );
            Preconditions.checkArgument( !Strings.isNullOrEmpty( publicKeyContainer.getKey() ) );
            Preconditions.checkArgument( !Strings.isNullOrEmpty( publicKeyContainer.getHostId() ) );
            Preconditions.checkNotNull( publicKeyContainer.getFingerprint() );

            final PGPPublicKeyRing pubKeyRing = PGPKeyUtil.readPublicKeyRing( publicKeyContainer.getKey() );
            localPeer.updatePeerEnvironmentPubKey( new EnvironmentId( publicKeyContainer.getHostId() ), pubKeyRing );
        }
        catch ( Exception e )
        {
            LOGGER.error( e.getMessage(), e );
            throw new WebApplicationException( Response.serverError().entity( e.getMessage() ).build() );
        }
    }


    @Override
    public void addInitiatorPeerEnvironmentPubKey( final String keyId, final String pek )
    {
        try
        {
            Preconditions.checkArgument( !Strings.isNullOrEmpty( keyId ) );
            Preconditions.checkArgument( !Strings.isNullOrEmpty( pek ) );

            PGPPublicKeyRing pubKeyRing = PGPKeyUtil.readPublicKeyRing( pek );
            localPeer.addPeerEnvironmentPubKey( keyId, pubKeyRing );
        }
        catch ( Exception e )
        {
            LOGGER.error( e.getMessage(), e );
            throw new WebApplicationException( Response.serverError().entity( e.getMessage() ).build() );
        }
    }


    @Override
    public Response setupTunnels( final EnvironmentId environmentId, final P2pIps p2pIps )
    {
        try
        {
            Preconditions.checkNotNull( environmentId );
            Preconditions.checkNotNull( p2pIps );

            localPeer.setupTunnels( p2pIps, environmentId );

            return Response.ok().build();
        }
        catch ( Exception e )
        {
            LOGGER.error( e.getMessage(), e );
            throw new WebApplicationException( Response.serverError().entity( e.getMessage() ).build() );
        }
    }


    @Override
    public ResourceHostMetrics getResources()
    {
        try
        {
            return localPeer.getResourceHostMetrics();
        }
        catch ( Exception e )
        {
            LOGGER.error( e.getMessage(), e );
            throw new WebApplicationException( Response.serverError().entity( e.getMessage() ).build() );
        }
    }


    @Override
    public UsedNetworkResources getUsedNetResources()
    {
        try
        {
            return localPeer.getUsedNetworkResources();
        }
        catch ( Exception e )
        {
            LOGGER.error( e.getMessage(), e );
            throw new WebApplicationException( Response.serverError().entity( e.getMessage() ).build() );
        }
    }


    @Override
    public Integer reserveNetResources( final NetworkResourceImpl networkResource )
    {
        try
        {
            Preconditions.checkNotNull( networkResource );

            return localPeer.reserveNetworkResource( networkResource );
        }
        catch ( Exception e )
        {
            LOGGER.error( e.getMessage(), e );
            throw new WebApplicationException( Response.serverError().entity( e.getMessage() ).build() );
        }
    }


    @Override
    public Boolean canAccommodate( final Nodes nodes )
    {
        try
        {
            Preconditions.checkNotNull( nodes );
            Preconditions.checkArgument( !nodes.getNodes().isEmpty() );

            return localPeer.canAccommodate( nodes );
        }
        catch ( Exception e )
        {
            LOGGER.error( e.getMessage(), e );
            throw new WebApplicationException( Response.serverError().entity( e.getMessage() ).build() );
        }
    }


    @Override
    public void resetP2PSecretKey( final P2PCredentials p2PCredentials )
    {
        try
        {
            localPeer.resetSwarmSecretKey( p2PCredentials );
        }
        catch ( Exception e )
        {
            LOGGER.error( e.getMessage(), e );
            throw new WebApplicationException( Response.serverError().entity( e.getMessage() ).build() );
        }
    }


    @Override
    public void joinP2PSwarm( final P2PConfig config )
    {
        try
        {
            Preconditions.checkNotNull( config );

            localPeer.joinP2PSwarm( config );
        }
        catch ( Exception e )
        {
            LOGGER.error( e.getMessage(), e );
            throw new WebApplicationException( Response.serverError().entity( e.getMessage() ).build() );
        }
    }


    @Override
    public void joinOrUpdateP2PSwarm( final P2PConfig config )
    {
        try
        {
            Preconditions.checkNotNull( config );

            localPeer.joinOrUpdateP2PSwarm( config );
        }
        catch ( Exception e )
        {
            LOGGER.error( e.getMessage(), e );
            throw new WebApplicationException( Response.serverError().entity( e.getMessage() ).build() );
        }
    }


    @Override
    public void cleanupEnvironment( final EnvironmentId environmentId )
    {
        try
        {
            Preconditions.checkNotNull( environmentId );

            localPeer.cleanupEnvironment( environmentId );
        }
        catch ( Exception e )
        {
            LOGGER.error( e.getMessage(), e );
            throw new WebApplicationException( Response.serverError().entity( e.getMessage() ).build() );
        }
    }


    @Override
    public Response putAlert( final AlertEvent alertEvent )
    {
        try
        {
            Preconditions.checkNotNull( alertEvent );

            localPeer.alert( alertEvent );

            return Response.accepted().build();
        }
        catch ( Exception e )
        {
            LOGGER.error( e.getMessage(), e );
            throw new WebApplicationException( Response.serverError().entity( e.getMessage() ).build() );
        }
    }


    @Override
    public Response getHistoricalMetrics( final String hostId, final DateTimeParam startTime,
                                          final DateTimeParam endTime )
    {
        try
        {
            return Response.ok( localPeer
                    .getHistoricalMetrics( new HostId( hostId ), startTime.getDate(), endTime.getDate() ) ).build();
        }
        catch ( Exception e )
        {
            LOGGER.error( e.getMessage(), e );
            throw new WebApplicationException( Response.serverError().entity( e.getMessage() ).build() );
        }
    }


    @Override
    public Response getMetricsSeries( final String hostId, final DateTimeParam startTime, final DateTimeParam endTime )
    {
        try
        {
            return Response
                    .ok( localPeer.getMetricsSeries( new HostId( hostId ), startTime.getDate(), endTime.getDate() ) )
                    .build();
        }
        catch ( Exception e )
        {
            LOGGER.error( e.getMessage(), e );
            throw new WebApplicationException( Response.serverError().entity( e.getMessage() ).build() );
        }
    }


    @Override
    public Response getResourceLimits( final PeerId peerId )
    {
        try
        {
            Preconditions.checkNotNull( peerId );

            return Response.ok( localPeer.getResourceLimits( peerId ) ).build();
        }
        catch ( Exception e )
        {
            LOGGER.error( e.getMessage(), e );
            throw new WebApplicationException( Response.serverError().entity( e.getMessage() ).build() );
        }
    }
}
