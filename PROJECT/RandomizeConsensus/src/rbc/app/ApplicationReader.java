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

import net.sf.appia.core.AppiaEventException;
import net.sf.appia.core.Direction;
import net.sf.appia.core.message.Message;

import java.util.StringTokenizer;

import rbc.events.ProcessSendableEvent;
import rbc.util.Commands;

/**
 * Class that reads from the keyboard and generates events to the appia Channel.
 * 
 * @author nuno
 */
public class ApplicationReader extends Thread
{
	private ApplicationSession parentSession;
	private java.io.BufferedReader keyb;
	private String local = null;
	private boolean isRepeat=false;

	public ApplicationReader(ApplicationSession parentSession)
	{
		super();
		this.parentSession = parentSession;
		keyb = new java.io.BufferedReader(new java.io.InputStreamReader(
				System.in));
	}
	
	public ApplicationReader(ApplicationSession parentSession,boolean isRepeat)
	{
		super();
		this.parentSession = parentSession;
		this.isRepeat=isRepeat;
	}
	
	
	
	public void ProcessInput(String local) throws AppiaEventException
	{
		if(local=="")
			return;
		if(local.equals("1"))
			local=Commands.COMMAND_PROPOSE;
		StringTokenizer st = new StringTokenizer(local);
		/*
		 * creates the event, push the message and sends this to the
		 * appia channel.
		 */
		ProcessSendableEvent asyn = new ProcessSendableEvent();
		Message message = asyn.getMessage();
		asyn.setCommand(st.nextToken());
		String msg = "";
		while (st.hasMoreTokens())
			msg += (st.nextToken() + " ");
		message.pushString(msg);
		asyn.asyncGo(parentSession.channel, Direction.DOWN);
		
	}

	public void run()
	{
		while (true)
		{
			try
			{				
				if(isRepeat)
				{
					Thread.sleep(7000);
					ProcessInput("1");
					break;
				}
				Thread.sleep(500);	
				System.out.println("> Please type 1 and enter to propose a value.... ");
				local = keyb.readLine();				
				ProcessInput(local);
			} 
			catch (java.io.IOException e)
			{
				e.printStackTrace();
			} 
			catch (AppiaEventException e)
			{
				e.printStackTrace();
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}
}
