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

import java.util.ArrayList;
import java.util.Arrays;
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
	private int[] val_phase1;
	private int[] val_phase2;
	private int[] domain_values = {1, 2, 3, 4};
	private int f;
	private ArrayList<ConsensusMessage> messageBuffer;
	private int decidedConsensusInstance;
	private int consesnsusInstance;
	private boolean hasProposed;
	/**
	 * Builds a new BEBSession.
	 * 
	 * @param layer
	 */
	public RandomizedConsesusSession(Layer layer)
	{
		super(layer);
		messageBuffer=new ArrayList<ConsensusMessage>();
		decidedConsensusInstance=-1;
		consesnsusInstance=1;
	}

	private void Init()
	{
		hasProposed=false;
		round = 0;
		phase = 0;
		proposal = -2;
		decision = -2;
		f=(processes.getSize()-1)/2;
		ClearPhase1_ValueArray();
		ClearPhase2_ValueArray();
	}
	
	private void ClearPhase1_ValueArray()
	{
		val_phase1 = new int[processes.getSize()];
		for (int i = 0; i < val_phase1.length; i++)
		{
			val_phase1[i] = -2;
		}
	}
	
	private void ClearPhase2_ValueArray()
	{
		val_phase2 = new int[processes.getSize()];
		for (int i = 0; i < val_phase2.length; i++)
		{
			val_phase2[i]=-2;
		}
	}
	

	private synchronized void Propose(int value, SendableEvent event)
	{
		try
		{
			if(!hasProposed)
			{
				hasProposed=true;
				//proposal = coin.nextInt(2);
				proposal = getRandom(domain_values);
				round = 1;
				phase = 1;
				ConsensusMessage message = new ConsensusMessage();
				message.SetProposal(processes.getSelfRank(), round, phase, proposal,consesnsusInstance);
				event.getMessage().pushObject(message);
				event.go();
				System.out.println("Proposed value by processId = "+processes.getSelfRank()+" Proposed value = "+proposal);				
			}
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
			if(message.getConsensusInstance()==consesnsusInstance)
			{
				if(message.isDecision())
				{
					PrintDetails("Decision received", message);
					decision=message.getDecision();
					SendDecisionToApplication(event,message.getConsensusInstance());
				}
				else
				{				
					if(message.getRound() == this.round && phase==message.getPhase())
					{
						if(phase==1)
						{
							val_phase1[message.getProcessRank()] = message.getProposal();
							PrintDetails("Received a proposal and updated val_phase1", message);
						}
						if(phase==2)
						{						
							val_phase2[message.getProcessRank()] = message.getProposal();
							PrintDetails("Received a proposal and updated val_phase2", message);
						}
					}
					else
					{					
						messageBuffer.add(message);
						PrintDetails("Buffered message", message);
					}
					
					//If not yet proposed a value, propose automatically
					if(round == 0 && phase==0 && decision<0)
					{
						SendableEvent newEvent = (SendableEvent)event.cloneEvent();
						newEvent.setDir(Direction.DOWN);
						newEvent.setSourceSession(this);					
						newEvent.init();
						Propose(1, newEvent);
					}
					
					AnalyzeBufferedMessages();
					ProceedPhase(message, event);
				}	
			}
			else
			{
				if(message.isDecision() && message.getConsensusInstance()<consesnsusInstance)
				{
					System.out.println("Discard message. An olders decision. Already delivered.");
				}
				else
				{
					//Need to think, do we need message.getConsensusInstance()<consesnsusInstance messages to be buffered
					System.out.println("Wait until the previous decision deliver");
					messageBuffer.add(message);
					PrintDetails("Buffered message of a different consensus instance", message);
				}
			}
		} 
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	private void AnalyzeBufferedMessages()
	{
		ConsensusMessage message;
		ArrayList<ConsensusMessage> tempMessages=new ArrayList<ConsensusMessage>();
		for(int i=0;i<messageBuffer.size();i++)
		{
			message=messageBuffer.get(i);
			if(message.getRound()==round && message.getPhase()==phase && message.getConsensusInstance()==consesnsusInstance)
			{
				if(phase==1)
				{
					val_phase1[message.getProcessRank()]=message.getProposal();
					//values[message.getProcessRank()] = message.getProposal();
					tempMessages.add(message);
					PrintDetails("Recovered an earlier message from",message);
				}
				if(phase==2)
				{
					val_phase2[message.getProcessRank()]=message.getProposal();
					tempMessages.add(message);
					PrintDetails("Recovered an earlier message from",message);
				}
			}
		}
		
		for(int i=0;i<tempMessages.size();i++)
		{
			messageBuffer.remove(tempMessages.get(i));
		}
		
	}

	private void ProceedPhase(ConsensusMessage message, SendableEvent event)
	{
		try
		{
			if(phase==1 && decision<0 && CheckMajorityReplied(val_phase1))
			{
				proposal=GetMajorityRepliedValuePhase1();
				ClearPhase1_ValueArray();
				phase=2;
				message = new ConsensusMessage();
				message.SetProposal(processes.getSelfRank(), round, phase, proposal,consesnsusInstance);
				event = (SendableEvent) event.cloneEvent();
				event.getMessage().pushObject(message);
				event.setDir(Direction.DOWN);
				event.setSourceSession(this);
				event.init();
				event.go();
				System.out.println("ProcessId "+processes.getSelfRank()+" moved to phase 2 of round "+round );
			}
			else if(phase==2 && decision<0 && CheckMajorityReplied(val_phase2))
			{
				phase=0;
				System.out.println("ProcessId "+processes.getSelfRank()+" moved to phase 0 of round "+round );
				CoinOutput(getRandom(domain_values), event);
				//CoinOutput(coin.nextInt(2), event);				
			}
		/*	else
			{
				System.out.println("ProceedPhase, all conditions has failed for this message");
			}*/
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
				System.out.println("ProcessId "+processes.getSelfRank()+" took the decision = "+decision+" at round "+round + " at consensusIntance " + consesnsusInstance );
				ConsensusMessage message = new ConsensusMessage();
				message.SetDecision(processes.getSelfRank(), round, phase, decision,consesnsusInstance);
				messageBuffer.clear();
				//Broadcast the decision
				SendableEvent cloneEvent = (SendableEvent) event.cloneEvent();
				cloneEvent.getMessage().pushObject(message);
				cloneEvent.setDir(Direction.DOWN);
				cloneEvent.setSourceSession(this);
				cloneEvent.init();
				cloneEvent.go();
				
				SendDecisionToApplication(event,consesnsusInstance);
			}
			else
			{
				proposal=resultValue;
				ClearPhase1_ValueArray();
				ClearPhase2_ValueArray();
				round=round+1;
				phase=1;
				ConsensusMessage message = new ConsensusMessage();
				message.SetProposal(processes.getSelfRank(), round, phase, proposal,consesnsusInstance);
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
	
	private void SendDecisionToApplication(SendableEvent event,int decisionConsensusInstance)
	{
		try
		{
			if(decidedConsensusInstance<decisionConsensusInstance)
			{
				decidedConsensusInstance=decisionConsensusInstance;
				ConsensusMessage message = new ConsensusMessage();
				message.SetDecision(processes.getSelfRank(), round, phase, decision,consesnsusInstance);
				event.getMessage().pushObject(message);
				event.go();	
				System.out.println("ProcessId "+processes.getSelfRank()+" sent the decision = "+decision+" to the application at round "+round +", consensus instance = "+decisionConsensusInstance);
				consesnsusInstance=decisionConsensusInstance+1;
				Init();
			}
		}
		catch(AppiaEventException ex)
		{
			ex.printStackTrace();
		}
	}


	private int GetMajorityRepliedValuePhase1()
	{
		int value = -1;		
		HashMap<Integer, Integer> countMap= GetCounterHashMap(val_phase1);
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
		HashMap<Integer, Integer> countMap= GetCounterHashMap(val_phase2);
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
	
	private HashMap<Integer, Integer> GetCounterHashMap(int[] val)
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
	
	public int getRandom(int[] array)
	{
		int randomIndex = new Random().nextInt(array.length);
		return array[randomIndex];
	}

	
	private boolean CheckMajorityReplied(int[] val)
	{
		boolean hasReplied = false;
		int count = 0;
		for (int i = 0; i < val.length; i++)
		{
			if (val[i] >= -1)
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
	
	private void PrintDetails(String detail,ConsensusMessage message)
	{
		PrintDetails(detail,message.getRound(), message.getPhase(), message.getProcessRank(), message.getProposal(), message.getDecision(),message.getConsensusInstance());
	}

	private void PrintDetails(String message,int round,int phase,int processId,int proposal,int decision,int cinstance)
	{
		System.out.println(message +" PID="+processId+", R="+round+", PH="+phase+", PROP="+proposal+", DEC="+decision+", CIN="+cinstance);
	}
}
