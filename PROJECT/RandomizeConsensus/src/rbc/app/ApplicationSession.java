/*
 *
 * Hands-On code of the book Introduction to Reliable Distributed Programming
 * by Christian Cachin, Rachid Guerraoui and Luis Rodrigues
 * Copyright (C) 2005-2011 Luis Rodrigues
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA
 *
 * Contact
 * 	Address:
 *		Rua Alves Redol 9, Office 605
 *		1000-029 Lisboa
 *		PORTUGAL
 * 	Email:
 * 		ler@ist.utl.pt
 * 	Web:
 *		http://homepages.gsd.inesc-id.pt/~ler/
 * 
 */

package rbc.app;

import net.sf.appia.core.*;
import net.sf.appia.core.events.channel.ChannelClose;
import net.sf.appia.core.events.channel.ChannelInit;
import net.sf.appia.protocols.common.RegisterSocketEvent;

import java.net.InetSocketAddress;

import rbc.events.ProcessInitEvent;
import rbc.events.ProcessSendableEvent;
import rbc.util.Commands;
import rbc.util.ConsensusMessage;
import rbc.util.ProcessSet;

/**
 * Session implementing the sample application.
 * 
 * @author nuno
 */
public class ApplicationSession extends Session
{

	Channel channel;
	private ProcessSet processes;
	private ApplicationReader reader;
	private boolean blocked = false;

	public ApplicationSession(Layer layer)
	{
		super(layer);
	}

	public void init(ProcessSet processes)
	{
		this.processes = processes;
	}

	public void handle(Event event)
	{
		if (event instanceof ProcessSendableEvent)
			handleProcessSendableEvent((ProcessSendableEvent) event);
		else if (event instanceof ChannelInit)
			handleChannelInit((ChannelInit) event);
		else if (event instanceof ChannelClose)
			handleChannelClose((ChannelClose) event);
		else if (event instanceof RegisterSocketEvent)
			handleRegisterSocket((RegisterSocketEvent) event);
	}

	/**
	 * @param event
	 */
	private void handleRegisterSocket(RegisterSocketEvent event)
	{
		if (event.error)
		{
			System.out.println("Address already in use!");
			System.exit(2);
		}
	}

	/**
	 * @param init
	 */
	private void handleChannelInit(ChannelInit init)
	{
		try
		{
			init.go();
		} catch (AppiaEventException e)
		{
			e.printStackTrace();
		}
		channel = init.getChannel();

		try
		{
			// sends this event to open a socket in the layer that is used has
			// perfect
			// point to point
			// channels or unreliable point to point channels.
			RegisterSocketEvent rse = new RegisterSocketEvent(channel,
					Direction.DOWN, this);
			rse.port = ((InetSocketAddress) processes.getSelfProcess()
					.getSocketAddress()).getPort();
			rse.localHost = ((InetSocketAddress) processes.getSelfProcess()
					.getSocketAddress()).getAddress();
			rse.go();
			ProcessInitEvent processInit = new ProcessInitEvent(channel,
					Direction.DOWN, this);
			processInit.setProcessSet(processes);
			processInit.go();
		} catch (AppiaEventException e1)
		{
			e1.printStackTrace();
		}
		System.out.println("Channel is open.");
		// starts the thread that reads from the keyboard.
		reader = new ApplicationReader(this);
		reader.start();
	}

	/**
	 * @param close
	 */
	private void handleChannelClose(ChannelClose close)
	{
		channel = null;
		System.out.println("Channel is closed.");
	}

	/**
	 * @param event
	 */
	private void handleProcessSendableEvent(ProcessSendableEvent event)
	{
		if (event.getDir() == Direction.DOWN)
			handleOutgoingEvent(event);
		else
			handleIncomingEvent(event);
	}

	/**
	 * @param event
	 */
	private void handleIncomingEvent(ProcessSendableEvent event)
	{
		ConsensusMessage message = (ConsensusMessage)event.getMessage().popObject();		
		System.out.print("Received decision with value: " + message.getDecision() + "\n>");
		ApplicationNotifier.NotifyMove(message.getDecision(), message.getProcessRank());
	}
	
	

	/**
	 * @param event
	 */
	private void handleOutgoingEvent(ProcessSendableEvent event)
	{
		String command = event.getCommand();
		if(Commands.COMMAND_PROPOSE.equalsIgnoreCase(command))
			handleRandomizedConsesus(event);			
		else if ("help".equals(command))
			printHelp();
		else
		{
			System.out.println("Invalid command: " + command);
			printHelp();
		}
	}

	private void handleRandomizedConsesus(ProcessSendableEvent event)
	{
		if (blocked)
		{
			System.out
					.println("The group is blocked, therefore message can not be sent.");
			return;
		}

		try
		{
			event.go();
		} catch (AppiaEventException e)
		{
			e.printStackTrace();
		}
	}
	/**
	 * 
	 */
	private void printHelp()
	{
		System.out
				.println("Available commands:\n"
						+ "bcast <msg> - Broadcast the message \"msg\"\n"
						+ "lrbcast <msg> - For you to implement in lab4\n"
						+ "startpfd - starts the Perfect Failure detector (when it applies)\n"
						+ "aabcast <msg> - For you to implement in lab5\n"
						+ "mrbcast <msg> - For you to implement in lab5\n"
						+"start - For the randomized consensus\n"
						+ "help - Print this help information.");
	}

}
