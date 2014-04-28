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

package rbc.consensus;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;

import rbc.events.ProcessInitEvent;
import rbc.events.ProcessSendableEvent;
import rbc.util.Commands;
import rbc.util.ConsensusMessage;
import rbc.util.ProcessSet;
import net.sf.appia.core.AppiaEventException;
import net.sf.appia.core.Direction;
import net.sf.appia.core.Event;
import net.sf.appia.core.Layer;
import net.sf.appia.core.Session;
import net.sf.appia.core.events.SendableEvent;
import net.sf.appia.core.events.channel.ChannelInit;

/**
 * Session implementing the Basic Broadcast protocol.
 * 
 * @author nuno
 * 
 */
public class RandomizedConsesusSession extends Session
{

	/*
	 * State of the protocol: the set of processes in the group
	 */
	private ProcessSet processes;
	private Random coin;
	private int round;
	private int phase;
	private int proposal;
	private int decision;
	private int[] val;
	private int f;

	/**
	 * Builds a new BEBSession.
	 * 
	 * @param layer
	 */
	public RandomizedConsesusSession(Layer layer)
	{
		super(layer);
	}

	private void Init()
	{
		round = 0;
		phase = 0;
		proposal = -1;
		decision = -1;
		f=(processes.getSize()-1)/2;
		InitVal();
	}
	
	private void InitVal()
	{
		val = new int[processes.getSize()];
		for (int i = 0; i < val.length; i++)
		{
			val[i] = -1;
		}
	}

	private void Propose(int value, SendableEvent event)
	{
		try
		{
			proposal = value;
			round = 1;
			phase = 1;
			ConsensusMessage message = new ConsensusMessage();
			message.SetProposal(processes.getSelfRank(), round, phase, proposal);
			event.getMessage().pushObject(message);
			event.go();
			System.out.println("Proposed value by processId = "+processes.getSelfRank());
		} catch (AppiaEventException ex)
		{
			ex.printStackTrace();
		}
	}

	/**
	 * Handles incoming events.
	 * 
	 * @see appia.Session#handle(appia.Event)
	 */
	public void handle(Event event)
	{
		if (event instanceof ChannelInit)
			handleChannelInit((ChannelInit) event);
		else if (event instanceof ProcessInitEvent)
			handleProcessInitEvent((ProcessInitEvent) event);
		else if (event instanceof SendableEvent)
		{
			if (event.getDir() == Direction.DOWN)
				ProcessAplicationInstructions((SendableEvent) event);
			else
				BroadcastDelivery((SendableEvent) event);
		}
	}

	private void ProcessAplicationInstructions(SendableEvent event)
	{
		String command = ((ProcessSendableEvent) event).getCommand();
		System.out.println("Command at ProcessAplicationInstructions at RandomizedConsesusSession :"+ command);

		switch (command.toUpperCase())
		{
		case Commands.COMMAND_PROPOSE:
			Propose(1, event);
			break;
		}
	}

	/**
	 * Gets the process set and forwards the event to other layers.
	 * 
	 * @param event
	 */
	private void handleProcessInitEvent(ProcessInitEvent event)
	{
		processes = event.getProcessSet();
		coin=event.getCoin();
		Init();
		try
		{
			event.go();
		} catch (AppiaEventException e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * Handles the first event that arrives to the protocol session. In this
	 * case, just forwards it.
	 * 
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
	}

	/**
	 * Delivers an incoming message.
	 * 
	 * @param event
	 */
	private void BroadcastDelivery(SendableEvent event)
	{
		try
		{
			ConsensusMessage message = (ConsensusMessage) event.getMessage().popObject();
			if(message.isDecision())
			{
				System.out.println("Decision ="+message.getDecision()+" received from processId = "+message.getProcessRank()+", round = "+message.getRound()+", phase = "+message.getPhase());
				decision=message.getDecision();
				SendDecisionToApplication(event);
			}
			else
			{
				System.out.println("Proposal = "+message.getProposal()+" received from processId = "+message.getProcessRank()+", round = "+message.getRound()+", phase = "+message.getPhase());
				if (message.getRound() == this.round && (phase == 1 || phase==2))
				{
					val[message.getProcessRank()] = message.getProposal();
				}
				ProceedPhase(message, event);
			}			
		} 
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	private void ProceedPhase(ConsensusMessage message, SendableEvent event)
	{
		try
		{
			if(phase==1 && decision<0 && CheckMajorityReplied())
			{
				proposal=GetMajorityRepliedValuePhase1();
				InitVal();
				phase=2;
				message = new ConsensusMessage();
				message.SetProposal(processes.getSelfRank(), round, phase, proposal);
				event = (SendableEvent) event.cloneEvent();
				event.getMessage().pushObject(message);
				event.setDir(Direction.DOWN);
				event.setSourceSession(this);
				event.init();
				event.go();
				System.out.println("ProcessId "+processes.getSelfRank()+" moved to phase 2 of round "+round );
			}
			else if(phase==2 && decision<0 && CheckMajorityReplied())
			{
				phase=0;
				CoinOutput(coin.nextInt(2), event);
				System.out.println("ProcessId "+processes.getSelfRank()+" moved to phase 0 of round "+round );
			}
			else
			{
				System.out.println("ProceedPhase, all conditions has failed for this message");
			}
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}

	}
	
	private void CoinOutput(int c, SendableEvent event)
	{
		try
		{
			String[] result=GetCoinValuePhase0(c);
			int resultValue=Integer.parseInt(result[0]);
			boolean resultIsDecision=Boolean.parseBoolean(result[1]);
			if(resultIsDecision)
			{
				decision=resultValue;
				System.out.println("ProcessId "+processes.getSelfRank()+" took the decision = "+decision+" at round "+round );
				ConsensusMessage message = new ConsensusMessage();
				message.SetDecision(processes.getSelfRank(), round, phase, decision);				
				//Broadcast the decision
				SendableEvent cloneEvent = (SendableEvent) event.cloneEvent();
				cloneEvent.getMessage().pushObject(message);
				cloneEvent.setDir(Direction.DOWN);
				cloneEvent.setSourceSession(this);
				cloneEvent.init();
				cloneEvent.go();
				
				SendDecisionToApplication(event);

			}
			else
			{
				proposal=resultValue;
				InitVal();
				round=round+1;
				phase=1;
				ConsensusMessage message = new ConsensusMessage();
				message.SetProposal(processes.getSelfRank(), round, phase, proposal);
				event = (SendableEvent) event.cloneEvent();
				event.getMessage().pushObject(message);
				event.setDir(Direction.DOWN);
				event.setSourceSession(this);
				event.init();
				event.go();
				System.out.println("ProcessId "+processes.getSelfRank()+" moved to phase 1 of round "+round );
			}
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
	}
	
	private void SendDecisionToApplication(SendableEvent event)
	{
		try
		{
			ConsensusMessage message = new ConsensusMessage();
			message.SetDecision(processes.getSelfRank(), round, phase, decision);
			event.getMessage().pushObject(message);
			event.go();	
			
			System.out.println("ProcessId "+processes.getSelfRank()+" sent the decision = "+decision+" to the application at round "+round );
			
			Init();
		}
		catch(AppiaEventException ex)
		{
			ex.printStackTrace();
		}
	}


	private int GetMajorityRepliedValuePhase1()
	{
		int value = -1;		
		HashMap<Integer, Integer> countMap= GetCounterHashMap();
		if(countMap.size()>0)
		{
			int selection;
			int iteratedValue;
		    Iterator<Integer> it = countMap.keySet().iterator();
		    while (it.hasNext()) 
		    {
		    	selection=it.next();
		    	iteratedValue=countMap.get(selection);
				if(iteratedValue>(processes.getSize()/2))
				{
					value=selection;
					break;
				}
		    }
		}		
		return value;
	}
	
	//String[0]= value
	//String[1]=true/false, true = decision, false= proposal
	private String[] GetCoinValuePhase0(int c)
	{
		String[] result=new String[2];
		HashMap<Integer, Integer> countMap= GetCounterHashMap();
		if(countMap.size()>0)
		{
			int selection;
			int iteratedValue;
		    Iterator<Integer> it = countMap.keySet().iterator();		    
		    while (it.hasNext()) 
		    {
		    	selection=it.next();
		    	iteratedValue=countMap.get(selection);
				if(iteratedValue>f)
				{
					result[0]=Integer.toString(selection);
					result[1]=Boolean.toString(true);
					break;
				}
				
				if(!it.hasNext())
				{
					result[0]=Integer.toString(selection);
					result[1]=Boolean.toString(false);
				}
		    }
		}
		else
		{
			result[0]=Integer.toString(c);
			result[1]=Boolean.toString(false);
		}		
		return result;
	}
	
	private HashMap<Integer, Integer> GetCounterHashMap()
	{
		HashMap<Integer, Integer> countMap = new HashMap<Integer, Integer>();
		for (int i=0;i<val.length;i++)
		{
			if(val[i]>=0)
			{
				if (!countMap.containsKey(val[i]))
				{
					countMap.put(val[i], 1);
				}
				else
				{
					Integer tmpcount = countMap.get(val[i]);
					tmpcount = tmpcount + 1;
					countMap.put(val[i], tmpcount);
				}
			}
		}
		return countMap;
	}

	
	private boolean CheckMajorityReplied()
	{
		boolean hasReplied = false;
		int count = 0;
		for (int i = 0; i < val.length; i++)
		{
			if (val[i] >= 0)
			{
				count++;
			}
		}
		if (phase == 1)
		{
			if (count > (processes.getSize() / 2))
				hasReplied = true;
		}
		else if(phase==2)
		{
			if(count>=(processes.getSize()-f))
				hasReplied=true;
		}
		return hasReplied;
	}

}
