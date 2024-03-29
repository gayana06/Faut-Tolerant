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

package tfsd;

import tfsd.pfd.*;
import tfsd.lazy.*;

import net.sf.appia.core.*;
import net.sf.appia.protocols.tcpcomplete.TcpCompleteLayer;
import net.sf.appia.protocols.tcpcomplete.TcpCompleteSession;
import net.sf.appia.protocols.udpsimple.UdpSimpleLayer;

import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.StringTokenizer;

/**
 * This class is the MAIN class to run the Reliable Broadcast protocols.
 * 
 * @author nuno
 */
public class SampleAppl {

  /**
   * Builds the Process set, using the information in the specified file.
   * 
   * @param filename
   *          the location of the file
   * @param selfProc
   *          the number of the self process
   * @return a new ProcessSet
   */
  private static ProcessSet buildProcessSet(String filename, int selfProc) {
    BufferedReader reader = null;
    try {
      reader = new BufferedReader(new InputStreamReader(new FileInputStream(
          filename)));
    } catch (FileNotFoundException e) {
      e.printStackTrace();
      System.exit(0);
    }
    String line;
    StringTokenizer st;
    boolean hasMoreLines = true;
    ProcessSet set = new ProcessSet();

    // reads lines of type: <process number> <IP address> <port>
    while(hasMoreLines) {
      try {
        line = reader.readLine();
        if (line == null)
          break;
        st = new StringTokenizer(line, " ");
        if (st.countTokens() != 3) {
          System.err.println("Wrong line in file: "+st.countTokens() + " ---> " + line);
          continue;
        }
        int procNumber = Integer.parseInt(st.nextToken());
        InetAddress addr = InetAddress.getByName(st.nextToken());
        int portNumber = Integer.parseInt(st.nextToken());
        boolean self = (procNumber == selfProc);
        System.err.println("Starting process " + procNumber + " Addr: " + addr + " Port number: " + portNumber);
        SampleProcess process = new SampleProcess(new InetSocketAddress(addr,
            portNumber), procNumber, self);
        set.addProcess(process, procNumber);
      } catch (IOException e) {
        hasMoreLines = false;
      } catch (NumberFormatException e) {
        System.err.println(e.getMessage());
      }
    } // end of while

    return set;
  }

  /**
   * Builds an Appia channel with the specified QoS
   * 
   * @param set
   *          the ProcessSet
   * @param qos
   *          the specified QoS
   * @return a new uninitialized channel
   */
  private static Channel getChannel(ProcessSet set, String qos) {
    if (qos.equals("beb")){
      return getBebChannel(set);
    }
    if (qos.equals("lrb")){ //For you to implement later
      return getLrbChannel(set);
    }
    else {
        System.out.println("Other");
        return null;
    }
  }


  /**
   * Builds a new Appia Channel with Best Effort Broadcast
   * 
   * @param processes
   *          set of processes
   * @return a new uninitialized Channel
   */
  private static Channel getBebChannel(ProcessSet processes) {
    /* Create layers and put them on a array */
    Layer[] qos = {new TcpCompleteLayer(), new BasicBroadcastLayer(),
        new SampleApplLayer()};

    /* Create a QoS */
    QoS myQoS = null;
    try {
      myQoS = new QoS("Best Effort Broadcast QoS", qos);
    } catch (AppiaInvalidQoSException ex) {
      System.err.println("Invalid QoS");
      System.err.println(ex.getMessage());
      System.exit(1);
    }
    /* Create a channel. Uses default event scheduler. */
    Channel channel = myQoS
        .createUnboundChannel("Best effort Broadcast Channel");
    /*
     * Application Session requires special arguments: filename and . A session
     * is created and binded to the stack. Remaining ones are created by default
     */
    SampleApplSession sas = (SampleApplSession) qos[qos.length - 1]
        .createSession();
    sas.init(processes);
    ChannelCursor cc = channel.getCursor();
    /*
     * Application is the last session of the array. Positioning in it is simple
     */
    try {
      cc.top();
      cc.setSession(sas);
    } catch (AppiaCursorException ex) {
      System.err.println("Unexpected exception in main. Type code:" + ex.type);
      System.exit(1);
    }
    return channel;
  }

  /**
   * Builds a new Appia Channel with Reliable Broadcast
   * 
   * @param processes
   *          set of processes
   * @return a new uninitialized Channel
   */
  private static Channel getLrbChannel(ProcessSet processes) {
    /* Create layers and put them on a array */
    Layer[] qos = {new TcpCompleteLayer(), new BasicBroadcastLayer(),
        new TcpBasedPFDLayer(), new LazyRBLayer(), new SampleApplLayer()};

    /* Create a QoS */
    QoS myQoS = null;
    try {
      myQoS = new QoS("Reliable Broadcast QoS", qos);
    } catch (AppiaInvalidQoSException ex) {
      System.err.println("Invalid QoS");
      System.err.println(ex.getMessage());
      System.exit(1);
    }
    /* Create a channel. Uses default event scheduler. */
    Channel channel = myQoS.createUnboundChannel("Reliable Broadcast Channel");
    /*
     * Application Session requires special arguments: filename and . A session
     * is created and binded to the stack. Remaining ones are created by default
     */
    SampleApplSession sas = (SampleApplSession) qos[qos.length - 1]
        .createSession();
    sas.init(processes);
    ChannelCursor cc = channel.getCursor();
    /*
     * Application is the last session of the array. Positioning in it is simple
     */
    try {
      cc.top();
      cc.setSession(sas);
    } catch (AppiaCursorException ex) {
      System.err.println("Unexpected exception in main. Type code:" + ex.type);
      System.exit(1);
    }
    return channel;
  }

  public static void main(String[] args) {

    int arg = 0, self = -1;
    String filename = null, qos = null;
    try {
      while (arg < args.length) {
        if (args[arg].equals("-f")) {
          arg++;
          filename = args[arg];
          System.out.println("Reading from file: " + filename);
        } else if (args[arg].equals("-n")) {
          arg++;
          try {
            self = Integer.parseInt(args[arg]);
            System.out.println("Process number: " + self);
          } catch (NumberFormatException e) {
            invalidArgs(e.getMessage());
          }
        } else if (args[arg].equals("-qos")) {
          arg++;
          qos = args[arg];
          if (qos.equals("pb")) {
            qos = qos + " " + args[++arg] + " " + args[++arg];
          }
          System.out.println("Starting with QoS: " + qos);
        } else
          invalidArgs("Unknown argument: "+args[arg]);
        arg++;
      }
    } catch (ArrayIndexOutOfBoundsException e) {
      e.printStackTrace();
      invalidArgs(e.getMessage());
    }

    /*
     * gets a new uninitialized Channel with the specified QoS and the Appl
     * session created. Remaining sessions are created by default. Just tell the
     * channel to start.
     */
    Channel channel = getChannel(buildProcessSet(filename, self), qos);
    try {
      channel.start();
    } catch (AppiaDuplicatedSessionsException ex) {
      System.err.println("Sessions binding strangely resulted in "
          + "one single sessions occurring more than " + "once in a channel");
      System.exit(1);
    }

    /* All set. Appia main class will handle the rest */
    System.out.println("Starting Appia...");
    Appia.run();
  }

  /**
   * Prints a error message and exit.
   * @param reason the reason of the failure
   */
  private static void invalidArgs(String reason) {
    System.out
        .println("Invalid args: "+reason+"\nUsage SampleAppl -f filemane -n proc_number -qos QoS_type."
            + "\n QoS can be one of the following:"
            + "\n\t beb - Best Effort Broadcast"
            + "\n\t lrb - Lazy Reliable Broadcast");
    System.exit(1);
  }
}
