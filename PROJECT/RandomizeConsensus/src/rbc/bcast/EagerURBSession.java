package rbc.bcast;

import java.util.ArrayList;

import net.sf.appia.core.*;
import net.sf.appia.core.events.SendableEvent;
import net.sf.appia.core.events.channel.ChannelInit;


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
public class EagerURBSession extends Session
{

	private ProcessSet processes;
	private int seqNumber;	
	private ArrayList<String> deliveredMessages;

	/**
	 * @param layer
	 */
	public EagerURBSession(EagerURBLayer layer)
	{
		super(layer);
		deliveredMessages=new ArrayList<String>();
		seqNumber=0;
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
				UniqueProcess self = processes.getSelfProcess();
				MessageID msgID = new MessageID(self.getProcessNumber(), -1,
						false);
				event.getMessage().pushObject(msgID);
				event.go();
			}
			else
			{
				//System.out.println("A decision arrived MajorityAckURBSession");
				//rbBroadcast(event);	
				EagerReliableBcast(event);
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
	
	private void EagerReliableBcast(SendableEvent event)
	{
		UniqueProcess self = processes.getSelfProcess();
		MessageID msgID = new MessageID(self.getProcessNumber(), seqNumber,	true);
		seqNumber++;
		event.getMessage().pushObject(msgID);
		bebBroadcast(event);
	}
		
	private void EagerReliableBcastDelivary(SendableEvent event,MessageID msgID)
	{
		try
		{
			String messageHash=GetMessageHashCode(msgID);
			if(!deliveredMessages.contains(messageHash))
			{
				deliveredMessages.add(messageHash);
				SendableEvent clonedEvent=(SendableEvent)event.cloneEvent();
				event.getMessage().popObject();//this is just to remove the messageId object
				event.go();				
				bebBroadcast(clonedEvent);				
			}
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
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
		MessageID msgID = (MessageID) event.getMessage().peekObject();
		if (msgID.isReliableBroadcast)
		{
			EagerReliableBcastDelivary(event, msgID);
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
			event.getMessage().popObject(); //this is only to remove the top message
			event.go();
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
		
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
