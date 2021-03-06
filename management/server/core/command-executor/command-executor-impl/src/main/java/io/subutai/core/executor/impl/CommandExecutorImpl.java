package io.subutai.core.executor.impl;


import com.google.common.base.Preconditions;

import io.subutai.common.command.CommandCallback;
import io.subutai.common.command.CommandException;
import io.subutai.common.command.CommandResult;
import io.subutai.common.command.Request;
import io.subutai.common.command.RequestBuilder;
import io.subutai.core.executor.api.CommandExecutor;


/**
 * Implementation of CommandExecutor
 */
public class CommandExecutorImpl implements CommandExecutor
{

    protected CommandProcessor commandProcessor;


    public CommandExecutorImpl( CommandProcessor commandProcessor )
    {
        Preconditions.checkNotNull( commandProcessor );

        this.commandProcessor = commandProcessor;
    }


    @Override
    public CommandResult execute( final String hostId, final RequestBuilder requestBuilder ) throws CommandException
    {
        return execute( hostId, requestBuilder, new DummyCallback() );
    }


    @Override
    public CommandResult execute( final String hostId, final RequestBuilder requestBuilder,
                                  final CommandCallback callback ) throws CommandException
    {
        Preconditions.checkNotNull( hostId, "Invalid host id" );
        Preconditions.checkNotNull( requestBuilder, "Invalid request builder" );
        Preconditions.checkNotNull( requestBuilder, "Invalid callback" );

        Request request = requestBuilder.build( hostId );

        commandProcessor.execute( request, callback );

        return commandProcessor.getResult( request.getCommandId() );
    }


    @Override
    public void executeAsync( final String hostId, final RequestBuilder requestBuilder ) throws CommandException
    {
        executeAsync( hostId, requestBuilder, new DummyCallback() );
    }


    @Override
    public void executeAsync( final String hostId, final RequestBuilder requestBuilder, final CommandCallback callback )
            throws CommandException
    {
        Preconditions.checkNotNull( hostId, "Invalid host id" );
        Preconditions.checkNotNull( requestBuilder, "Invalid request builder" );
        Preconditions.checkNotNull( requestBuilder, "Invalid callback" );

        commandProcessor.execute( requestBuilder.build( hostId ), callback );
    }
}
