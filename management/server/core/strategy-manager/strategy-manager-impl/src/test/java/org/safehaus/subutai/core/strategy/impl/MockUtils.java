/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.safehaus.subutai.core.strategy.impl;


import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.safehaus.subutai.common.protocol.Agent;
import org.safehaus.subutai.core.monitor.api.Metric;
import org.safehaus.subutai.core.strategy.api.ContainerPlacementStrategy;
import org.safehaus.subutai.core.strategy.api.ServerMetric;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


/**
 * Mocking utilities
 */
public class MockUtils
{

    private static final UUID physicalUUID = UUID.randomUUID();
    private static final UUID lxcUUID = UUID.randomUUID();
    private static final String PHYSICAL_HOSTNAME = "py111";
    private static final String LXC_HOSTNAME = "py111-lxc-222";


    public static Agent getPhysicalAgent()
    {
        Agent agent = mock( Agent.class );
        when( agent.getHostname() ).thenReturn( PHYSICAL_HOSTNAME );
        when( agent.getUuid() ).thenReturn( physicalUUID );
        when( agent.isLXC() ).thenReturn( false );
        when( agent.getParentHostName() ).thenReturn( null );
        when( agent.compareTo( agent ) ).thenCallRealMethod();
        return agent;
    }


    public static ContainerPlacementStrategy getDefaultContainerPlacementStrategy()
    {
        return new DefaultContainerPlacementStrategy();
    }


    public static ContainerPlacementStrategy getRoundRobinPlacementStrategy()
    {
        return new RoundRobinStrategy();
    }


    public static ContainerPlacementStrategy getBestServerPlacementStrategy()
    {
        return new BestServerStrategy();
    }

//
//    public static Map<Agent, ServerMetric> getServerMetrics()
//    {
//        Map<Agent, ServerMetric> result = new HashMap<>();
//
//        Map<Metric, Double> avgMetric = new HashMap<>();
//        avgMetric.put( Metric.CPU_USER, 1.0 );
//        avgMetric.put( Metric.MEM_FREE, 4096.0 );
//        ServerMetric metric = new ServerMetric( 20000, 40000, 30, 4, avgMetric );
//        result.put( getPhysicalAgent(), metric );
//        return result;
//    }


    public Map<Agent, ServerMetric> getAvgMetrics()
    {
        Map<Agent, ServerMetric> result = new HashMap<>();
        return result;
    }

    //
    //    public static Agent getPhysicalAgent()
    //    {
    //        Agent agent = mock( Agent.class );
    //        when( agent.getHostname() ).thenReturn( PHYSICAL_HOSTNAME );
    //        when( agent.getUuid() ).thenReturn( physicalUUID );
    //        when( agent.isLXC() ).thenReturn( false );
    //        when( agent.getParentHostName() ).thenReturn( null );
    //        when( agent.compareTo( agent ) ).thenCallRealMethod();
    //        return agent;
    //    }
    //
    //
    //    public static Agent getLxcAgent()
    //    {
    //        Agent agent = mock( Agent.class );
    //        when( agent.getHostname() ).thenReturn( LXC_HOSTNAME );
    //        when( agent.getUuid() ).thenReturn( lxcUUID );
    //        when( agent.isLXC() ).thenReturn( true );
    //        when( agent.getParentHostName() ).thenReturn( PHYSICAL_HOSTNAME );
    //        when( agent.compareTo( agent ) ).thenCallRealMethod();
    //        return agent;
    //    }
    //
    //
    //    public static CommandRunner getAutoCommandRunner()
    //    {
    //        CommandRunner commandRunner = mock( CommandRunner.class );
    //        when( commandRunner.createCommand( any( RequestBuilder.class ), any( Set.class ) ) )
    //                .thenAnswer( new Answer<Command>()
    //                {
    //
    //                    public Command answer( InvocationOnMock invocation ) throws Throwable
    //                    {
    //                        Object[] arguments = invocation.getArguments();
    //                        RequestBuilder requestBuilder = ( RequestBuilder ) arguments[0];
    //                        Request request = requestBuilder.build( physicalUUID, UUID.randomUUID() );
    //                        if ( request.getProgram().contains( "lxc-info" ) )
    //                        {
    //                            return MockUtils.getCommand( true, true, 0, "RUNNING", null );
    //                        }
    //                        else if ( request.getProgram().contains( "lxc-ls" ) )
    //                        {
    //                            return MockUtils.getCommand( true, true, 0, getLxcListOutput(), null );
    //                        }
    //                        else if ( request.getProgram().contains( "free -m" ) )
    //                        {
    //                            return MockUtils.getCommand( true, true, 0, getMetricsOutput(), null );
    //                        }
    //                        return MockUtils.getCommand( true, true, 0, null, null );
    //                    }
    //                } );
    //
    //        return commandRunner;
    //    }
    //
    //
    //    public static Command getCommand( boolean completed, boolean succeeded, Integer exitCode, String stdOut,
    //                                      String stdErr )
    //    {
    //        Command command = mock( Command.class );
    //        when( command.hasCompleted() ).thenReturn( completed );
    //        when( command.hasSucceeded() ).thenReturn( succeeded );
    //        AgentResult agentResult = mock( AgentResult.class );
    //        when( agentResult.getExitCode() ).thenReturn( exitCode );
    //        when( agentResult.getStdOut() ).thenReturn( stdOut );
    //        when( agentResult.getStdErr() ).thenReturn( stdErr );
    //        when( agentResult.getAgentUUID() ).thenReturn( physicalUUID );
    //        Map<UUID, AgentResult> results = new HashMap<>();
    //        results.put( physicalUUID, agentResult );
    //        when( command.getResults() ).thenReturn( results );
    //        return command;
    //    }
    //
    //
    //    public static String getLxcListOutput()
    //    {
    //        return "RUNNING\n" + "  py111-lxc-222\n" + "  py111-lxc-888ba0c7-c559-11e3-acc9-af19ef907de1\n"
    //                + "  py111-lxc-888bc7d8-c559-11e3-acc9-af19ef907de1\n"
    //                + "  py111-lxc-888bc7da-c559-11e3-acc9-af19ef907de1\n"
    //                + "  py111-lxc-99780f42-c573-11e3-8493-59facd645e07\n"
    //                + "  py111-lxc-99783654-c573-11e3-8493-59facd645e07\n"
    //                + "  py111-lxc-99783656-c573-11e3-8493-59facd645e07\n"
    //                + "  py111-lxc-99783658-c573-11e3-8493-59facd645e07\n"
    //                + "  py111-lxc-acda8860-c55d-11e3-acc9-af19ef907de1\n"
    //                + "  py111-lxc-acda8861-c55d-11e3-acc9-af19ef907de1\n"
    //                + "  py111-lxc-acda8863-c55d-11e3-acc9-af19ef907de1\n"
    //                + "  py111-lxc-acda8865-c55d-11e3-acc9-af19ef907de1\n"
    //                + "  py111-lxc-acdaaf77-c55d-11e3-acc9-af19ef907de1\n" + "\n" + "FROZEN\n" + "\n" + "STOPPED\n"
    //                + "  base-container\n" + "  py111-lxc-test1\n" + "  py111-lxc-test2";
    //    }
    //
    //
    //    public static String getMetricsOutput()
    //    {
    //        return "-/+ buffers/cache:       8561       7362\n" + "/dev/sda1      950982348 15033292 887635220   2%
    // /\n"
    //                + " 15:05:11 up 7 days, 21:28,  1 user,  load average: 0.01, 0.09, 0.27\n" + "8";
    //    }
    //
    //
    //    public static CommandRunner getHardCodedCommandRunner( boolean completed, boolean succeeded, Integer exitCode,
    //                                                           String stdOut, String stdErr )
    //    {
    //        CommandRunner commandRunner = mock( CommandRunner.class );
    //        Command command = MockUtils.getCommand( completed, succeeded, exitCode, stdOut, stdErr );
    //        when( commandRunner.createCommand( any( RequestBuilder.class ), any( Set.class ) ) ).thenReturn(
    // command );
    //
    //        return commandRunner;
    //    }
}