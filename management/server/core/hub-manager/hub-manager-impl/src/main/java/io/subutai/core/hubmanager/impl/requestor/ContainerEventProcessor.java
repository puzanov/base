package io.subutai.core.hubmanager.impl.requestor;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import io.subutai.common.peer.ContainerHost;
import io.subutai.common.peer.ResourceHost;
import io.subutai.common.settings.Common;
import io.subutai.core.hubmanager.api.HubRequester;
import io.subutai.core.hubmanager.api.RestClient;
import io.subutai.core.hubmanager.api.RestResult;
import io.subutai.core.hubmanager.api.exception.HubManagerException;
import io.subutai.core.hubmanager.impl.HubManagerImpl;
import io.subutai.core.peer.api.PeerManager;
import io.subutai.hub.share.dto.environment.ContainerStateDto;
import io.subutai.hub.share.dto.environment.container.ContainerEventDto;


public class ContainerEventProcessor extends HubRequester
{
    private final Logger log = LoggerFactory.getLogger( getClass() );

    private PeerManager peerManager;


    public ContainerEventProcessor( final HubManagerImpl hubManager, final PeerManager peerManager,
                                    final RestClient restClient )
    {
        super( hubManager, restClient );
        this.peerManager = peerManager;
    }


    @Override
    public void request() throws HubManagerException
    {
        process();
    }


    public void process() throws HubManagerException
    {
        try
        {
            for ( ResourceHost rh : peerManager.getLocalPeer().getResourceHosts() )
            {
                sendContainerStates( rh );
            }
        }
        catch ( Exception e )
        {
            log.error( "Oops error: ", e.getMessage() );
        }
    }


    private void sendContainerStates( ResourceHost rh ) throws HubManagerException
    {
        log.info( "ResourceHost: id={}, hostname={}, containers={}", rh.getId(), rh.getHostname(),
                rh.getContainerHosts().size() );

        for ( ContainerHost ch : rh.getContainerHosts() )
        {
            if ( !Common.MANAGEMENT_HOSTNAME.equals( ch.getContainerName() ) )
            {
                sendContainerState( ch );
            }
        }
    }


    private void sendContainerState( ContainerHost ch ) throws HubManagerException
    {
        log.info( "- ContainerHost: id={}, name={}, environmentId={}, state={}", ch.getId(), ch.getContainerName(),
                ch.getEnvironmentId(), ch.getState() );

        ContainerStateDto state = ContainerStateDto.valueOf( ch.getState().name() );

        ContainerEventDto dto = new ContainerEventDto( ch.getId(), ch.getEnvironmentId().getId(), state );

        RestResult res = doRequest( dto );

        log.info( "Response status: {}", res.getStatus() );
    }


    private RestResult doRequest( ContainerEventDto dto ) throws HubManagerException
    {
        try
        {
            String path = String.format( "/rest/v1/containers/%s/events", dto.getContainerId() );

            return restClient.post( path, dto );
        }
        catch ( Exception e )
        {
            throw new HubManagerException( e );
        }
    }
}
