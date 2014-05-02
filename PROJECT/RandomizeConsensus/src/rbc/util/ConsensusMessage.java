package rbc.util;

import java.io.Serializable;

public class ConsensusMessage implements Serializable
{
	private static final long serialVersionUID = -5632927438973377590L;
	
	private boolean isDecision;
	private int processRank;
	private int round;
	private int phase;
	private int proposal;
	private int decision;
	private int consensusInstance;
	
	public void SetProposal(int processRank,int round, int phase,int proposal,int consensusInstance)
	{
		this.isDecision=false;
		this.processRank=processRank;
		this.round=round;
		this.phase=phase;
		this.proposal=proposal;
		this.decision=-1;
		this.consensusInstance=consensusInstance;
	}
	
	public void SetDecision(int processRank,int round, int phase,int decision,int consensusInstance)
	{
		this.isDecision=true;
		this.processRank=processRank;
		this.round=round;
		this.phase=phase;
		this.proposal=-1;
		this.decision=decision;
		this.consensusInstance=consensusInstance;
	}


	public boolean isDecision()
	{
		return isDecision;
	}

	public int getProcessRank()
	{
		return processRank;
	}

	public int getRound()
	{
		return round;
	}

	public int getPhase()
	{
		return phase;
	}

	public int getProposal()
	{
		return proposal;
	}

	public int getDecision()
	{
		return decision;
	}
	
	public int getConsensusInstance()
	{
		return consensusInstance;
	}
}
