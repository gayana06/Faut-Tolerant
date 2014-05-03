package demo.app;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

class ComSocket extends Thread 
{
	private Socket socket;

	public ComSocket(Socket socket)
	{
		this.socket = socket;
	}


	public void run()
	{
		try
		{
			BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));			
			while (true)
			{
				String message = in.readLine();	
				System.out.println("Message : "+message);
				if (message == null)
				{
					break;
				}
				else
				{
					MessageProcessor.ProcessMessage(message);
				}				
			}
		} 
		catch (IOException e)
		{
			e.printStackTrace();
		}
		finally
		{
			try
			{
				if(socket!=null)
					socket.close();
			} 
			catch (IOException e)
			{
				e.printStackTrace();
			}

		}
	}
}
