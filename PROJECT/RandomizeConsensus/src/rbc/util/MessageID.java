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

package rbc.util;

import java.io.Serializable;
import java.security.MessageDigest;

/**
 * Message identifier.
 * 
 * @author nuno
 */
public class MessageID implements Serializable
{
	private static final long serialVersionUID = -5632927438973377599L;

	public int process, seqNumber;
	public boolean isReliableBroadcast;

	public MessageID(int p, int s, boolean isReliableBroadcast)
	{
		process = p;
		seqNumber = s;
		this.isReliableBroadcast = isReliableBroadcast;
	}

	public int hashCode()
	{
		return process ^ seqNumber;
	}
	
	public String getHashVal()
	{
		String hashCode=Integer.toString(process)+Integer.toString(seqNumber);
		try
		{
			MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");			
			messageDigest.update(hashCode.getBytes());
			hashCode= new String(messageDigest.digest());
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
		return hashCode;
	}

	public boolean equals(Object o)
	{
		MessageID id = (MessageID) o;
		return process == id.process && seqNumber == id.seqNumber;
	}

	public String toString()
	{
		return " (" + process + "," + seqNumber + ") ";
	}
}
