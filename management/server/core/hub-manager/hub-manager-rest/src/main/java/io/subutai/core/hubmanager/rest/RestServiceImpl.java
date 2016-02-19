package io.subutai.core.hubmanager.rest;


import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.subutai.core.hubmanager.api.HubPluginException;
import io.subutai.core.hubmanager.api.Integration;


public class RestServiceImpl implements RestService
{
    private static final Logger LOG = LoggerFactory.getLogger( RestServiceImpl.class.getName() );
    private Integration integration;


    public void setIntegration( final Integration integration )
    {
        this.integration = integration;
    }


    public Response sendHeartbeat( final String hubIp )
    {
        try
        {
            integration.sendHeartbeat();
            return Response.ok().build();
        }
        catch ( HubPluginException e )
        {
            LOG.error( e.getMessage() );
            return Response.status( Response.Status.INTERNAL_SERVER_ERROR ).
                    entity( e.getMessage() ).build();
        }
    }


    public Response register( final String hubIp, final String email, final String password )
    {
        try
        {
            integration.registerPeer( hubIp, email, password );
            return Response.ok().build();
        }
        catch ( HubPluginException e )
        {
            LOG.error( e.getMessage() );
            return Response.status( Response.Status.INTERNAL_SERVER_ERROR ).
                    entity( e.getMessage() ).build();
        }
    }


    public Response sendRHConfigurations( final String hubIp )
    {
        try
        {
            integration.sendResourceHostInfo();
            return Response.ok().build();
        }
        catch ( HubPluginException e )
        {
            LOG.error( e.getMessage() );
            return Response.status( Response.Status.INTERNAL_SERVER_ERROR ).
                    entity( e.getMessage() ).build();
        }
    }


    public Response getHubDns()
    {
        try
        {
            return Response.ok( integration.getHubDns() ).build();
        }
        catch ( HubPluginException e )
        {
            LOG.error( e.getMessage() );
            return Response.status( Response.Status.BAD_REQUEST ).
                    entity( "Could not get Hub IP" ).build();
        }
    }
}
