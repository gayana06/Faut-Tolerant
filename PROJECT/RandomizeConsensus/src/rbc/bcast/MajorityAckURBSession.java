package rbc.bcast;

import net.sf.appia.core.*;
import net.sf.appia.core.events.SendableEvent;
import net.sf.appia.core.events.channel.ChannelInit;

import java.net.SocketAddress;
import java.util.Hashtable;
import java.util.LinkedList;

import rbc.events.ProcessInitEvent;
import rbc.util.ConsensusMessage;
import rbc.util.MessageID;
import rbc.util.ProcessSet;
import rbc.util.UniqueProcess;

/**
 * Session implementing the Lazy Reliable Broadcast protocol.
 * 
 * @author nuno
 * 
 */
public class MajorityAckURBSession extends Session
{

	private ProcessSet processes;
	private int seqNumber;
	// List of MessageID objects
	private LinkedList<MessageID> delivered;
	private LinkedList<MessageID> pending;
	private Hashtable<String, LinkedList<Integer>> ackTable;

	/**
	 * @param layer
	 */
	public MajorityAckURBSession(MajorityAckURBLayer layer)
	{
		super(layer);
		pending = new LinkedList<MessageID>();
		delivered = new LinkedList<MessageID>();
		ackTable = new Hashtable<String, LinkedList<Integer>>();
	}

	/**
	 * Main event handler
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
				ProcessEventFromTop((SendableEvent) event);
			else
				rbDeliver((SendableEvent) event);
		}
	}

	private void ProcessEventFromTop(SendableEvent event)
	{
		try
		{
			ConsensusMessage message = (ConsensusMessage) event.getMessage().peekObject();
			if (!message.isDecision())
			{
				//System.out.println("A proposal arrived MajorityAckURBSession");
				UniqueProcess self = processes.getSelfProcess();
				MessageID msgID = new MessageID(self.getProcessNumber(), -1,
						false);
				event.getMessage().pushObject(msgID);
				event.go();
			}
			else
			{
				//System.out.println("A decision arrived MajorityAckURBSession");
				rbBroadcast(event);				
			}
		} catch (Exception ex)
		{
			ex.printStackTrace();
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
	}

	/**
	 * @param event
	 */
	private void handleProcessInitEvent(ProcessInitEvent event)
	{
		processes = event.getProcessSet();
		try
		{
			event.go();
		} catch (AppiaEventException e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * Called when the above protocol sends a message.
	 * 
	 * @param event
	 */
	private void rbBroadcast(SendableEvent event)
	{
		UniqueProcess self = processes.getSelfProcess();
		MessageID msgID = new MessageID(self.getProcessNumber(), seqNumber,	true);
		pending.add(msgID);
		// UpdateAcks(msgID,self.getProcessNumber());
		seqNumber++;
		//System.out.println("RB: broadcasting message.");
		event.getMessage().pushObject(msgID);
		bebBroadcast(event);
	}

	private void UpdateAcks(MessageID msgID, int processId)
	{
		if (ackTable.containsKey(GetMessageHashCode(msgID)))
		{
			if (!ackTable.get(GetMessageHashCode(msgID)).contains(processId))
			{
				ackTable.get(GetMessageHashCode(msgID)).add(processId);
			}
		} 
		else
		{
			ackTable.put(GetMessageHashCode(msgID), new LinkedList<Integer>());
			ackTable.get(GetMessageHashCode(msgID)).add(processId);
		}
	}
	
	private String GetMessageHashCode(MessageID msgID)
	{
		return msgID.getHashVal();
	}

	/**
	 * Called when the lower protocol delivers a message.
	 * 
	 * @param event
	 */
	private void rbDeliver(SendableEvent event)
	{
		//System.out.println("RB: Received message from beb.");
		MessageID msgID = (MessageID) event.getMessage().peekObject();
		if (msgID.isReliableBroadcast)
		{
			ProcessReliableBroadcastDelivary(event, msgID);
		}
		else
		{
			ProcessBestEfforDelivery(event);
		}
	}
	
	private void ProcessBestEfforDelivery(SendableEvent event)
	{
		try
		{
			//System.out.println("BestEffort delivery message");
			event.getMessage().popObject(); //this is only to remove the top message
			event.go();
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
		
	}
	
	private void ProcessReliableBroadcastDelivary(SendableEvent event,MessageID msgID)
	{
		//System.out.println("Reliable delivery message");
		UniqueProcess pi = processes.getProcess((SocketAddress) event.source);
		int piNumber = pi.getProcessNumber();
		UpdateAcks(msgID, piNumber);
		if (!pending.contains(msgID))
		{		
			//System.out.println("RB: message is new.");
			pending.add(msgID);
			bebBroadcast(event);
		}
		else
		{
			if (CanDeliver(msgID) && !delivered.contains(msgID))
			{
				//System.out.println("RB: message can deliver now");
				event.getMessage().popObject();
				try
				{
					delivered.add(msgID);
					event.go();
				} catch (AppiaEventException e)
				{
					e.printStackTrace();
				}
			}
		}
	}

	private boolean CanDeliver(MessageID msgID)
	{
		boolean canDeliver = false;
		int processCount = processes.getSize();
		//System.out.println("Process Count : " + processCount);
		//System.out.println("Ack Count:" + ackTable.get(GetMessageHashCode(msgID)).size());
		if (ackTable.get(GetMessageHashCode(msgID)).size() > (processCount / 2))
			canDeliver = true;
		else
			canDeliver = false;
		return canDeliver;
	}

	/**
	 * Called by this protocol to send a message to the lower protocol.
	 * 
	 * @param event
	 */
	private void bebBroadcast(SendableEvent event)
	{
		//System.out.println("RB: sending message to beb.");
		try
		{
			event.setDir(Direction.DOWN);
			event.setSourceSession(this);
			event.init();
			event.go();
		} catch (AppiaEventException e)
		{
			e.printStackTrace();
		}
	}
}
