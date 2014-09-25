package org.safehaus.subutai.core.peer.ui.forms;


import java.util.List;
import java.util.UUID;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.safehaus.subutai.core.peer.api.Peer;
import org.safehaus.subutai.core.peer.api.PeerStatus;
import org.safehaus.subutai.core.peer.ui.PeerUI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.cxf.jaxrs.client.WebClient;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.vaadin.annotations.AutoGenerated;
import com.vaadin.ui.AbsoluteLayout;
import com.vaadin.ui.Button;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Table;
import com.vaadin.ui.TextField;


public class PeerRegisterForm extends CustomComponent
{

    /*- VaadinEditorProperties={"grid":"RegularGrid,20","showGrid":true,"snapToGrid":true,"snapToObject":true,
    "movingGuides":false,"snappingDistance":10} */
    private static final Logger LOG = LoggerFactory.getLogger( PeerRegisterForm.class.getName() );
    public final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    @AutoGenerated
    private AbsoluteLayout mainLayout;
    @AutoGenerated
    private AbsoluteLayout peerRegisterLayout;
    @AutoGenerated
    private Table peersTable;
    @AutoGenerated
    private Button showPeersButton;
    @AutoGenerated
    private Button registerRequestButton;
    private Button registerViaRestButton;
    @AutoGenerated
    private Label ID;
    @AutoGenerated
    private TextField idTextField;
    @AutoGenerated
    private TextField ipTextField;
    @AutoGenerated
    private Label IP;
    @AutoGenerated
    private Label peerRegistration;
    @AutoGenerated
    private Label servicePort;
    @AutoGenerated
    private TextField servicePortTextField;

    private PeerUI peerUI;


    /**
     * The constructor should first build the main layout, set the composition root and then do any custom
     * initialization. <p/> The constructor will not be automatically regenerated by the visual editor.
     */
    public PeerRegisterForm( final PeerUI peerUI )
    {
        buildMainLayout();
        setCompositionRoot( mainLayout );

        // TODO add user code here
        this.peerUI = peerUI;
    }


    @AutoGenerated
    private AbsoluteLayout buildMainLayout()
    {
        // common part: create layout
        mainLayout = new AbsoluteLayout();
        mainLayout.setImmediate( false );
        mainLayout.setWidth( "100%" );
        mainLayout.setHeight( "100%" );

        // top-level component properties
        setWidth( "100.0%" );
        setHeight( "100.0%" );

        // peerRegisterLayout
        peerRegisterLayout = buildAbsoluteLayout_2();
        mainLayout.addComponent( peerRegisterLayout, "top:20.0px;right:0.0px;bottom:-20.0px;left:0.0px;" );

        return mainLayout;
    }


    @AutoGenerated
    private AbsoluteLayout buildAbsoluteLayout_2()
    {

        // common part: create layout
        AbsoluteLayout absoluteLayout = new AbsoluteLayout();
        absoluteLayout.setImmediate( false );
        absoluteLayout.setWidth( "100.0%" );
        absoluteLayout.setHeight( "100.0%" );

        // peerRegistration
        peerRegistration = new Label();
        peerRegistration.setImmediate( false );
        peerRegistration.setWidth( "-1px" );
        peerRegistration.setHeight( "-1px" );
        peerRegistration.setValue( "Peer registration" );
        absoluteLayout.addComponent( peerRegistration, "top:0.0px;left:20.0px;" );

        // tags label
        servicePort = new Label();
        servicePort.setImmediate( false );
        servicePort.setWidth( "-1px" );
        servicePort.setHeight( "-1px" );
        servicePort.setValue( "Service Port:" );
        absoluteLayout.addComponent( servicePort, "top:36.0px;left:20.0px;" );

        // servicePortTextField
        servicePortTextField = new TextField();
        servicePortTextField.setImmediate( false );
        servicePortTextField.setWidth( "-1px" );
        servicePortTextField.setHeight( "-1px" );
        servicePortTextField.setMaxLength( 256 );
        absoluteLayout.addComponent( servicePortTextField, "top:36.0px;left:100.0px;" );

        // IP
        IP = new Label();
        IP.setImmediate( false );
        IP.setWidth( "-1px" );
        IP.setHeight( "-1px" );
        IP.setValue( "IP" );
        absoluteLayout.addComponent( IP, "top:80.0px;left:20.0px;" );

        // ipTextField
        ipTextField = new TextField();
        ipTextField.setImmediate( false );
        ipTextField.setWidth( "-1px" );
        ipTextField.setHeight( "-1px" );
        ipTextField.setMaxLength( 15 );
        absoluteLayout.addComponent( ipTextField, "top:80.0px;left:100.0px;" );

        // registerRequestButton
        registerRequestButton = createRegisterButton();
        absoluteLayout.addComponent( registerRequestButton, "top:160.0px;left:20.0px;" );
        registerRequestButton = createRegisterButton();

        // showPeersButton
        showPeersButton = createShowPeersButton();
        absoluteLayout.addComponent( showPeersButton, "top:234.0px;left:20.0px;" );

        // peersTable
        peersTable = new Table();
        peersTable.setCaption( "Peers" );
        peersTable.setImmediate( false );
        peersTable.setWidth( "780px" );
        peersTable.setHeight( "283px" );
        absoluteLayout.addComponent( peersTable, "top:294.0px;left:20.0px;" );

        return absoluteLayout;
    }


    private Button createShowPeersButton()
    {
        showPeersButton = new Button();
        showPeersButton.setCaption( "Show peers" );
        showPeersButton.setImmediate( false );
        showPeersButton.setWidth( "-1px" );
        showPeersButton.setHeight( "-1px" );

        showPeersButton.addClickListener( new Button.ClickListener()
        {
            @Override
            public void buttonClick( final Button.ClickEvent clickEvent )
            {
                populateData();
                peersTable.refreshRowCache();
            }
        } );

        return showPeersButton;
    }


    private void populateData()
    {
        List<Peer> peers = peerUI.getPeerManager().peers();
        peersTable.removeAllItems();
        peersTable.addContainerProperty( "UUID", UUID.class, null );
        peersTable.addContainerProperty( "Name", String.class, null );
        peersTable.addContainerProperty( "IP", String.class, null );
        peersTable.addContainerProperty( "Status", PeerStatus.class, null );
        peersTable.addContainerProperty( "Actions", Button.class, null );

        for ( final Peer peer : peers )
        {
            Button unregisterButton = new Button( "Unregister" );
            switch ( peer.getStatus() )
            {
                case REQUESTED:
                    unregisterButton.setCaption( "Register" );
                    break;
                case APPROVED:
                    unregisterButton.setCaption( "Unregister" );
                    break;
                case REJECTED:
                    continue;
            }
            unregisterButton.addClickListener( new Button.ClickListener()
            {
                @Override
                public void buttonClick( final Button.ClickEvent clickEvent )
                {
                    switch ( peer.getStatus() )
                    {
                        case REQUESTED:
                        {
                            peer.setStatus( PeerStatus.APPROVED );
                            peerUI.getPeerManager().register( peer );
                            clickEvent.getButton().setCaption( "Unregister" );
                            break;
                        }
                        case APPROVED:
                        {
                            peerUI.getPeerManager().unregister( peer.getId().toString() );
                            break;
                        }
                    }
                }
            } );
            peersTable.addItem(
                    new Object[] { peer.getId(), peer.getName(), peer.getIp(), peer.getStatus(), unregisterButton },
                    null );
        }
    }


    private Button createRegisterButton()
    {
        registerRequestButton = new Button();
        registerRequestButton.setCaption( "Register" );
        registerRequestButton.setImmediate( true );
        registerRequestButton.setWidth( "-1px" );
        registerRequestButton.setHeight( "-1px" );

        registerRequestButton.addClickListener( new Button.ClickListener()
        {
            @Override
            public void buttonClick( final Button.ClickEvent clickEvent )
            {
                getUI().access( new Runnable()
                {
                    @Override
                    public void run()
                    {
                        String servicePort = servicePortTextField.getValue();
                        String ip = ipTextField.getValue();
                        LOG.warn( ip );
                        String baseUrl = String.format( "http://%s:%s/cxf", ip, servicePort );
                        String path = "peer";
                        try
                        {

                            WebClient client = WebClient.create( baseUrl );

                            if ( servicePort.length() > 0 && ip.length() > 0 )
                            {
                                String remotePeerJson = client.path( "peer/json" ).accept( MediaType.APPLICATION_JSON )
                                                              .get( String.class );

                                LOG.warn( remotePeerJson );

                                Peer remotePeer = GSON.fromJson( remotePeerJson, Peer.class );
                                remotePeer.setStatus( PeerStatus.REQUESTED );
                                peerUI.getPeerManager().register( remotePeer );
                            }
                            else
                            {
                                Notification.show( "Check form values" );
                                return;
                            }


                            WebClient local =
                                    WebClient.create( String.format( "http://%s:%s/cxf", "127.0.0.1", servicePort ) );
                            String localhostPeer =
                                    local.path( "peer/json" ).accept( MediaType.APPLICATION_JSON ).get( String.class );

                            Peer selfPeer = GSON.fromJson( localhostPeer, Peer.class );
                            selfPeer.setStatus( PeerStatus.REQUESTED );

                            client = WebClient.create( baseUrl );
                            Response response = client.path( "peer/register" ).type( MediaType.TEXT_PLAIN )
                                                      .accept( MediaType.APPLICATION_JSON )
                                                      .query( "peer", GSON.toJson( selfPeer ) ).post( "" );

                            if ( response.getStatus() == Response.Status.OK.getStatusCode() )
                            {
                                LOG.info( response.toString() );
                                Notification.show( String.format( "Request sent to %s!", ip ) );
                            }
                            else
                            {
                                LOG.warn( "Response for registering peer: " + response.toString() );
                            }
                        }
                        catch ( Exception e )
                        {
                            LOG.error( "PeerRegisterForm@createRegisterButton!clickListener" + e.getMessage(), e );
                        }
                    }
                } );
            }
        } );

        return registerRequestButton;
    }
}