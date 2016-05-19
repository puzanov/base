package io.subutai.core.hubmanager.impl.environment.state.build;


import java.util.concurrent.Callable;

import io.subutai.common.network.NetworkResourceImpl;
import io.subutai.common.protocol.P2PConfig;
import io.subutai.core.hubmanager.impl.entity.RhP2PIpEntity;
import io.subutai.core.hubmanager.impl.environment.state.Context;
import io.subutai.core.hubmanager.impl.environment.state.StateHandler;
import io.subutai.core.hubmanager.impl.util.AsyncUtil;
import io.subutai.hub.share.dto.environment.EnvironmentInfoDto;
import io.subutai.hub.share.dto.environment.EnvironmentPeerDto;
import io.subutai.hub.share.dto.environment.EnvironmentPeerRHDto;


public class ReserveNetworkStateHandler extends StateHandler
{
    public ReserveNetworkStateHandler( Context ctx )
    {
        super( ctx );
    }


    @Override
    protected Object doHandle( EnvironmentPeerDto peerDto ) throws Exception
    {
        reserveNetwork( peerDto );

        return setupP2P( peerDto );
    }


    private void reserveNetwork( EnvironmentPeerDto peerDto ) throws Exception
    {
        EnvironmentInfoDto env = peerDto.getEnvironmentInfo();

        String subnetWithoutMask = env.getSubnetCidr().replace( "/24", "" );

        final NetworkResourceImpl networkResource = new NetworkResourceImpl( env.getId(), env.getVni(), env.getP2pSubnet(), subnetWithoutMask );

        AsyncUtil.execute( new Callable<Void>()
        {
            @Override
            public Void call() throws Exception
            {
                ctx.localPeer.reserveNetworkResource( networkResource );

                return null;
            }
        } );
    }


    private EnvironmentPeerDto setupP2P( EnvironmentPeerDto peerDto ) throws Exception
    {
        EnvironmentInfoDto env = peerDto.getEnvironmentInfo();

        final P2PConfig p2pConfig = new P2PConfig( peerDto.getPeerId(), env.getId(), env.getP2pHash(), env.getP2pKey(), env.getP2pTTL() );

        for ( EnvironmentPeerRHDto rhDto : peerDto.getRhs() )
        {
            p2pConfig.addRhP2pIp( new RhP2PIpEntity( rhDto.getId(), rhDto.getP2pIp() ) );
        }

        AsyncUtil.execute( new Callable<Void>()
        {
            @Override
            public Void call() throws Exception
            {
                ctx.localPeer.joinP2PSwarm( p2pConfig );

                return null;
            }
        } );

        return peerDto;
    }
}