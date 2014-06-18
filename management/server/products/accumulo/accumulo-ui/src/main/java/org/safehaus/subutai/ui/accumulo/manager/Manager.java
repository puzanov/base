/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.safehaus.subutai.ui.accumulo.manager;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.safehaus.subutai.api.accumulo.Config;
import org.safehaus.subutai.api.accumulo.NodeType;
import org.safehaus.subutai.server.ui.ConfirmationDialogCallback;
import org.safehaus.subutai.server.ui.MgmtApplication;
import org.safehaus.subutai.shared.protocol.Agent;
import org.safehaus.subutai.ui.accumulo.AccumuloUI;
import org.safehaus.subutai.ui.accumulo.common.UiUtil;

import com.google.common.base.Strings;
import com.vaadin.data.Property;
import com.vaadin.terminal.Sizeable;
import com.vaadin.terminal.ThemeResource;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.Embedded;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Table;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;


/**
 * @author dilshat
 */
public class Manager {

    private final VerticalLayout contentRoot;
    private final ComboBox clusterCombo;
    private final Table mastersTable;
    private final Table tracersTable;
    private final Table slavesTable;
    private final Pattern masterPattern = Pattern.compile( ".*(Master.+?g).*" );
    private final Pattern gcPattern = Pattern.compile( ".*(GC.+?g).*" );
    private final Pattern monitorPattern = Pattern.compile( ".*(Monitor.+?g).*" );
    private final Pattern tracerPattern = Pattern.compile( ".*(Tracer.+?g).*" );
    private final Pattern loggerPattern = Pattern.compile( ".*(Logger.+?g).*" );
    private final Pattern tabletServerPattern = Pattern.compile( ".*(Tablet Server.+?g).*" );
    private Config config;


    public Manager() {

        contentRoot = new VerticalLayout();
        contentRoot.setSpacing( true );
        contentRoot.setWidth( 100, Unit.PERCENTAGE );
        contentRoot.setHeight( 100, Unit.PERCENTAGE );

        final VerticalLayout content = new VerticalLayout();
        content.setWidth( 100, Unit.PERCENTAGE );
        content.setHeight( 100, Unit.PERCENTAGE );

        contentRoot.addComponent( content );
        contentRoot.setComponentAlignment( content, Alignment.TOP_CENTER );
        contentRoot.setMargin( true );

        //tables go here
        mastersTable = UiUtil.createTableTemplate( "Masters", 150, contentRoot, false );
        tracersTable = UiUtil.createTableTemplate( "Tracers", 150, contentRoot, true );
        slavesTable = UiUtil.createTableTemplate( "Slaves", 150, contentRoot, true );
        //tables go here

        HorizontalLayout controlsContent = new HorizontalLayout();
        controlsContent.setSpacing( true );

        Label clusterNameLabel = new Label( "Select the cluster" );
        controlsContent.addComponent( clusterNameLabel );

        clusterCombo = new ComboBox();
        clusterCombo.setMultiSelect( false );
        clusterCombo.setImmediate( true );
        clusterCombo.setTextInputAllowed( false );
        clusterCombo.setWidth( 200, Unit.PIXELS );
        clusterCombo.addListener( new Property.ValueChangeListener() {

            @Override
            public void valueChange( Property.ValueChangeEvent event ) {
                config = ( Config ) event.getProperty().getValue();
                refreshUI();
            }
        } );
        controlsContent.addComponent( clusterCombo );

        Button refreshClustersBtn = new Button( "Refresh clusters" );
        refreshClustersBtn.addListener( new Button.ClickListener() {

            @Override
            public void buttonClick( Button.ClickEvent event ) {
                refreshClustersInfo();
            }
        } );
        controlsContent.addComponent( refreshClustersBtn );

        Button checkAllBtn = new Button( "Check all" );
        checkAllBtn.addListener( new Button.ClickListener() {

            @Override
            public void buttonClick( Button.ClickEvent event ) {
                checkNodesStatus( mastersTable );
                checkNodesStatus( slavesTable );
                checkNodesStatus( tracersTable );
            }
        } );
        controlsContent.addComponent( checkAllBtn );

        Button startClusterBtn = new Button( "Start cluster" );
        startClusterBtn.addListener( new Button.ClickListener() {

            @Override
            public void buttonClick( Button.ClickEvent event ) {
                UUID trackID = AccumuloUI.getAccumuloManager().startCluster( config.getClusterName() );
                MgmtApplication.showProgressWindow( Config.PRODUCT_KEY, trackID, null );
            }
        } );
        controlsContent.addComponent( startClusterBtn );

        Button stopClusterBtn = new Button( "Stop cluster" );
        stopClusterBtn.addListener( new Button.ClickListener() {

            @Override
            public void buttonClick( Button.ClickEvent event ) {
                UUID trackID = AccumuloUI.getAccumuloManager().stopCluster( config.getClusterName() );
                MgmtApplication.showProgressWindow( Config.PRODUCT_KEY, trackID, null );
            }
        } );
        controlsContent.addComponent( stopClusterBtn );

        Button destroyClusterBtn = new Button( "Destroy cluster" );
        destroyClusterBtn.addListener( new Button.ClickListener() {

            @Override
            public void buttonClick( Button.ClickEvent event ) {
                if ( config != null ) {
                    MgmtApplication.showConfirmationDialog( "Cluster destruction confirmation",
                            String.format( "Do you want to destroy the %s cluster?", config.getClusterName() ), "Yes",
                            "No", new ConfirmationDialogCallback() {

                                @Override
                                public void response( boolean ok ) {
                                    if ( ok ) {
                                        UUID trackID = AccumuloUI.getAccumuloManager()
                                                                 .uninstallCluster( config.getClusterName() );
                                        MgmtApplication.showProgressWindow( Config.PRODUCT_KEY, trackID,
                                                new Window.CloseListener() {

                                                    public void windowClose( Window.CloseEvent e ) {
                                                        refreshClustersInfo();
                                                    }
                                                }
                                                                          );
                                    }
                                }
                            }
                                                          );
                }
                else {
                    UiUtil.showMsg( "Please, select cluster", contentRoot.getWindow() );
                }
            }
        } );
        controlsContent.addComponent( destroyClusterBtn );

        Button addTracerBtn = new Button( "Add Tracer" );
        addTracerBtn.addListener( new Button.ClickListener() {

            @Override
            public void buttonClick( Button.ClickEvent event ) {
                if ( config != null ) {

                    org.safehaus.subutai.api.hadoop.Config hadoopConfig =
                            AccumuloUI.getHadoopManager().getCluster( config.getClusterName() );
                    org.safehaus.subutai.api.zookeeper.Config zkConfig =
                            AccumuloUI.getZookeeperManager().getCluster( config.getClusterName() );
                    if ( hadoopConfig != null ) {
                        if ( zkConfig != null ) {
                            Set<Agent> availableNodes = new HashSet<>( hadoopConfig.getAllNodes() );
                            availableNodes.retainAll( zkConfig.getNodes() );
                            availableNodes.removeAll( config.getTracers() );
                            if ( availableNodes.isEmpty() ) {
                                UiUtil.showMsg( "All Hadoop nodes already have tracers installed",
                                        contentRoot.getWindow() );
                                return;
                            }

                            AddNodeWindow addNodeWindow = new AddNodeWindow( config, availableNodes, NodeType.TRACER );
                            MgmtApplication.addCustomWindow( addNodeWindow );
                            addNodeWindow.addListener( new Window.CloseListener() {

                                public void windowClose( Window.CloseEvent e ) {
                                    refreshClustersInfo();
                                }
                            } );
                        }
                        else {
                            UiUtil.showMsg( String.format( "Zookeeper cluster %s not found", config.getClusterName() ),
                                    contentRoot.getWindow() );
                        }
                    }
                    else {
                        UiUtil.showMsg( String.format( "Hadoop cluster %s not found", config.getClusterName() ),
                                contentRoot.getWindow() );
                    }
                }
                else {
                    UiUtil.showMsg( "Please, select cluster", contentRoot.getWindow() );
                }
            }
        } );
        controlsContent.addComponent( addTracerBtn );

        Button addSlaveBtn = new Button( "Add Slave" );
        addSlaveBtn.addListener( new Button.ClickListener() {

            @Override
            public void buttonClick( Button.ClickEvent event ) {
                if ( config != null ) {

                    org.safehaus.subutai.api.hadoop.Config hadoopConfig =
                            AccumuloUI.getHadoopManager().getCluster( config.getClusterName() );
                    org.safehaus.subutai.api.zookeeper.Config zkConfig =
                            AccumuloUI.getZookeeperManager().getCluster( config.getClusterName() );
                    if ( hadoopConfig != null ) {
                        if ( zkConfig != null ) {
                            Set<Agent> availableNodes = new HashSet<>( hadoopConfig.getAllNodes() );
                            availableNodes.retainAll( zkConfig.getNodes() );
                            availableNodes.removeAll( config.getSlaves() );
                            if ( availableNodes.isEmpty() ) {
                                UiUtil.showMsg( "All Hadoop nodes already have slaves installed",
                                        contentRoot.getWindow() );
                                return;
                            }

                            AddNodeWindow addNodeWindow = new AddNodeWindow( config, availableNodes, NodeType.LOGGER );
                            MgmtApplication.addCustomWindow( addNodeWindow );
                            addNodeWindow.addListener( new Window.CloseListener() {

                                public void windowClose( Window.CloseEvent e ) {
                                    refreshClustersInfo();
                                }
                            } );
                        }
                        else {
                            UiUtil.showMsg( String.format( "Zookeeper cluster %s not found", config.getClusterName() ),
                                    contentRoot.getWindow() );
                        }
                    }
                    else {
                        UiUtil.showMsg( String.format( "Hadoop cluster %s not found", config.getClusterName() ),
                                contentRoot.getWindow() );
                    }
                }
                else {
                    UiUtil.showMsg( "Please, select cluster", contentRoot.getWindow() );
                }
            }
        } );
        controlsContent.addComponent( addSlaveBtn );

        HorizontalLayout customPropertyContent = new HorizontalLayout();
        customPropertyContent.setSpacing( true );

        Label propertyNameLabel = new Label( "Property Name" );
        customPropertyContent.addComponent( propertyNameLabel );
        final TextField propertyNameTextField = new TextField();
        customPropertyContent.addComponent( propertyNameTextField );

        Button removePropertyBtn = new Button( "Remove" );
        removePropertyBtn.addListener( new Button.ClickListener() {
            @Override
            public void buttonClick( Button.ClickEvent clickEvent ) {
                if ( config != null ) {
                    String propertyName = ( String ) propertyNameTextField.getValue();
                    if ( Strings.isNullOrEmpty( propertyName ) ) {
                        UiUtil.showMsg( "Please, specify property name to remove", contentRoot.getWindow() );
                    }
                    else {
                        UUID trackID =
                                AccumuloUI.getAccumuloManager().removeProperty( config.getClusterName(), propertyName );
                        MgmtApplication.showProgressWindow( Config.PRODUCT_KEY, trackID, null );
                    }
                }
                else {
                    UiUtil.showMsg( "Please, select cluster", contentRoot.getWindow() );
                }
            }
        } );
        customPropertyContent.addComponent( removePropertyBtn );

        Label propertyValueLabel = new Label( "Property Value" );
        customPropertyContent.addComponent( propertyValueLabel );
        final TextField propertyValueTextField = new TextField();
        customPropertyContent.addComponent( propertyValueTextField );
        Button addPropertyBtn = new Button( "Add" );
        addPropertyBtn.addListener( new Button.ClickListener() {
            @Override
            public void buttonClick( Button.ClickEvent clickEvent ) {
                if ( config != null ) {
                    String propertyName = ( String ) propertyNameTextField.getValue();
                    String propertyValue = ( String ) propertyValueTextField.getValue();
                    if ( Strings.isNullOrEmpty( propertyName ) ) {
                        UiUtil.showMsg( "Please, specify property name to add", contentRoot.getWindow() );
                    }
                    else if ( Strings.isNullOrEmpty( propertyValue ) ) {
                        UiUtil.showMsg( "Please, specify property value to set", contentRoot.getWindow() );
                    }
                    else {
                        UUID trackID = AccumuloUI.getAccumuloManager()
                                                 .addProperty( config.getClusterName(), propertyName, propertyValue );
                        MgmtApplication.showProgressWindow( Config.PRODUCT_KEY, trackID, null );
                    }
                }
                else {
                    UiUtil.showMsg( "Please, select cluster", contentRoot.getWindow() );
                }
            }
        } );
        customPropertyContent.addComponent( addPropertyBtn );

        content.addComponent( controlsContent );
        content.addComponent( customPropertyContent );
        content.setComponentAlignment( controlsContent, Alignment.TOP_RIGHT );
        content.setComponentAlignment( customPropertyContent, Alignment.TOP_RIGHT );

        content.addComponent( mastersTable );
        content.addComponent( tracersTable );
        content.addComponent( slavesTable );
    }


    public static void checkNodesStatus( Table table ) {
        UiUtil.clickAllButtonsInTable( table, "Check" );
    }


    private void refreshUI() {
        if ( config != null ) {
            populateTable( slavesTable, new ArrayList<>( config.getSlaves() ), false );
            populateTable( tracersTable, new ArrayList<>( config.getTracers() ), false );
            List<Agent> masters = new ArrayList<>();
            masters.add( config.getMasterNode() );
            masters.add( config.getGcNode() );
            masters.add( config.getMonitor() );
            populateTable( mastersTable, masters, true );
        }
        else {
            slavesTable.removeAllItems();
            tracersTable.removeAllItems();
            mastersTable.removeAllItems();
        }
    }


    private void populateTable( final Table table, List<Agent> agents, final boolean masters ) {

        table.removeAllItems();

        int i = 0;
        for ( final Agent agent : agents ) {
            i++;
            final Button checkBtn = new Button( "Check" );
            final Button destroyBtn = new Button( "Destroy" );
            final Embedded progressIcon =
                    new Embedded( "", new ThemeResource( "img/spinner.gif" ) );
            final Label resultHolder = new Label();
            destroyBtn.setEnabled( false );
            progressIcon.setVisible( false );

            table.addItem( masters ? new Object[] {
                            ( i == 1 ? UiUtil.MASTER_PREFIX : i == 2 ? UiUtil.GC_PREFIX : UiUtil.MONITOR_PREFIX )
                                    + agent.getHostname(), checkBtn, resultHolder, progressIcon
                    } : new Object[] {
                            agent.getHostname(), checkBtn, destroyBtn, resultHolder, progressIcon
                    }, null
                         );

            checkBtn.addListener( new Button.ClickListener() {

                @Override
                public void buttonClick( Button.ClickEvent event ) {
                    progressIcon.setVisible( true );

                    AccumuloUI.getExecutor().execute(
                            new CheckTask( config.getClusterName(), agent.getHostname(), new CompleteEvent() {

                                public void onComplete( String result ) {
                                    synchronized ( progressIcon ) {
                                        if ( masters ) {
                                            resultHolder.setValue( parseMastersState( result ) );
                                        }
                                        else if ( table == tracersTable ) {
                                            resultHolder.setValue( parseTracersState( result ) );
                                        }
                                        else if ( table == slavesTable ) {
                                            resultHolder.setValue( parseSlavesState( result ) );
                                        }
                                        destroyBtn.setEnabled( true );
                                        progressIcon.setVisible( false );
                                    }
                                }
                            } )
                                                    );
                }
            } );


            destroyBtn.addListener( new Button.ClickListener() {

                @Override
                public void buttonClick( Button.ClickEvent event ) {

                    MgmtApplication.showConfirmationDialog( "Node destruction confirmation",
                            String.format( "Do you want to destroy the %s node?", agent.getHostname() ), "Yes", "No",
                            new ConfirmationDialogCallback() {

                                @Override
                                public void response( boolean ok ) {
                                    if ( ok ) {
                                        UUID trackID = AccumuloUI.getAccumuloManager()
                                                                 .destroyNode( config.getClusterName(),
                                                                         agent.getHostname(),
                                                                         table == tracersTable ? NodeType.TRACER :
                                                                         NodeType.LOGGER
                                                                             );
                                        MgmtApplication.showProgressWindow( Config.PRODUCT_KEY, trackID,
                                                new Window.CloseListener() {

                                                    public void windowClose( Window.CloseEvent e ) {
                                                        refreshClustersInfo();
                                                    }
                                                }
                                                                          );
                                    }
                                }
                            }
                                                          );
                }
            } );
        }
    }


    private String parseMastersState( String result ) {
        StringBuilder parsedResult = new StringBuilder();
        Matcher masterMatcher = masterPattern.matcher( result );
        if ( masterMatcher.find() ) {
            parsedResult.append( masterMatcher.group( 1 ) ).append( " " );
        }
        Matcher gcMatcher = gcPattern.matcher( result );
        if ( gcMatcher.find() ) {
            parsedResult.append( gcMatcher.group( 1 ) ).append( " " );
        }
        Matcher monitorMatcher = monitorPattern.matcher( result );
        if ( monitorMatcher.find() ) {
            parsedResult.append( monitorMatcher.group( 1 ) ).append( " " );
        }

        return parsedResult.toString();
    }


    private String parseTracersState( String result ) {
        StringBuilder parsedResult = new StringBuilder();
        Matcher tracersMatcher = tracerPattern.matcher( result );
        if ( tracersMatcher.find() ) {
            parsedResult.append( tracersMatcher.group( 1 ) ).append( " " );
        }

        return parsedResult.toString();
    }


    private String parseSlavesState( String result ) {
        StringBuilder parsedResult = new StringBuilder();
        Matcher loggersMatcher = loggerPattern.matcher( result );
        if ( loggersMatcher.find() ) {
            parsedResult.append( loggersMatcher.group( 1 ) ).append( " " );
        }
        Matcher tablerServersMatcher = tabletServerPattern.matcher( result );
        if ( tablerServersMatcher.find() ) {
            parsedResult.append( tablerServersMatcher.group( 1 ) ).append( " " );
        }

        return parsedResult.toString();
    }


    public void refreshClustersInfo() {
        List<Config> mongoClusterInfos = AccumuloUI.getAccumuloManager().getClusters();
        Config clusterInfo = ( Config ) clusterCombo.getValue();
        clusterCombo.removeAllItems();
        if ( mongoClusterInfos != null && mongoClusterInfos.size() > 0 ) {
            for ( Config mongoClusterInfo : mongoClusterInfos ) {
                clusterCombo.addItem( mongoClusterInfo );
                clusterCombo.setItemCaption( mongoClusterInfo, mongoClusterInfo.getClusterName() );
            }
            if ( clusterInfo != null ) {
                for ( Config mongoClusterInfo : mongoClusterInfos ) {
                    if ( mongoClusterInfo.getClusterName().equals( clusterInfo.getClusterName() ) ) {
                        clusterCombo.setValue( mongoClusterInfo );
                        return;
                    }
                }
            }
            else {
                clusterCombo.setValue( mongoClusterInfos.iterator().next() );
            }
        }
    }


    public Component getContent() {
        return contentRoot;
    }
}
