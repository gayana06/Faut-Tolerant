package demo.app;

import java.io.IOException;
import java.net.ServerSocket;

public class Communicator
{
	public void ServerListen() throws IOException 
	{
		ServerSocket listener=null;
		try
		{
			int port=50000;
			listener = new ServerSocket(port);
			System.out.println("Server listening on port "+port);
			while (true)
			{
				new ComSocket(listener.accept()).start();
			}
		} 
		catch(Exception ex)
		{
			ex.printStackTrace();				
		}
		finally
		{
			listener.close();
		}
	}
}
