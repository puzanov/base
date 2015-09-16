package io.subutai.core.peer.impl.container;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.subutai.common.peer.ContainersDestructionResult;
import io.subutai.core.peer.api.LocalPeer;
import io.subutai.core.peer.api.Payload;
import io.subutai.core.peer.api.RequestListener;
import io.subutai.core.peer.impl.RecipientType;


public class DestroyEnvironmentContainerGroupRequestListener extends RequestListener
{
    private static final Logger LOG =
            LoggerFactory.getLogger( DestroyEnvironmentContainerGroupRequestListener.class.getName() );

    private LocalPeer localPeer;


    public DestroyEnvironmentContainerGroupRequestListener( LocalPeer localPeer )
    {
        super( RecipientType.DESTROY_ENVIRONMENT_CONTAINER_GROUP_REQUEST.name() );

        this.localPeer = localPeer;
    }


    @Override
    public Object onRequest( final Payload payload ) throws Exception
    {
        DestroyEnvironmentContainersRequest request = payload.getMessage( DestroyEnvironmentContainersRequest.class );

        if ( request != null )
        {
            ContainersDestructionResult result =
                    localPeer.destroyEnvironmentContainerGroup( request.getEnvironmentId() );

            return new DestroyEnvironmentContainersResponse( result.getDestroyedContainersIds(),
                    result.getException() );
        }
        else
        {
            LOG.warn( "Null request" );
        }

        return null;
    }
}
