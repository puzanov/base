package io.subutai.core.registration.impl.entity.entity;


import java.util.HashSet;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import io.subutai.common.host.HostArchitecture;
import io.subutai.common.host.HostInterface;
import io.subutai.core.registration.api.ResourceHostRegistrationStatus;
import io.subutai.core.registration.impl.entity.RequestedHostImpl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;


public class RequestedHostImplTest
{

    private RequestedHostImpl requestedHost;
    UUID uuid = UUID.randomUUID();


    @Before
    public void setUp() throws Exception
    {
        requestedHost =
                new RequestedHostImpl( uuid.toString(), "hostname", HostArchitecture.AMD64, "secret", "publicKey",
                        ResourceHostRegistrationStatus.REQUESTED, new HashSet<HostInterface>() );
    }


    @Test
    public void testGetId() throws Exception
    {
        assertEquals( uuid.toString(), requestedHost.getId() );
    }


    @Test
    public void testGetHostname() throws Exception
    {
        assertEquals( "hostname", requestedHost.getHostname() );
    }



    @Test
    public void testGetArch() throws Exception
    {
        assertEquals( HostArchitecture.AMD64, requestedHost.getArch() );
    }


    @Test
    public void testGetPublicKey() throws Exception
    {
        assertEquals( "publicKey", requestedHost.getPublicKey() );
    }


    @Test
    public void testGetStatus() throws Exception
    {
        assertEquals( ResourceHostRegistrationStatus.REQUESTED, requestedHost.getStatus() );
    }


    @Test
    public void testSetStatus() throws Exception
    {
        requestedHost.setStatus( ResourceHostRegistrationStatus.REJECTED );
        assertNotEquals( ResourceHostRegistrationStatus.REQUESTED, requestedHost.getStatus() );
    }


    @Test
    public void testGetSecret() throws Exception
    {
        requestedHost.setSecret( "secret" );
        assertEquals( "secret", requestedHost.getSecret() );
    }


    @Test
    public void testSetSecret() throws Exception
    {
        requestedHost.setSecret( "secret1" );
        assertNotEquals( "secret", requestedHost.getSecret() );
    }


    @Test
    public void testEquals() throws Exception
    {

    }


    @Test
    public void testHashCode() throws Exception
    {

    }


    @Test
    public void testToString() throws Exception
    {

    }
}