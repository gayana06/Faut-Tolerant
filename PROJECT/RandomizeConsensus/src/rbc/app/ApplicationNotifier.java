package rbc.app;

import java.io.PrintWriter;
import java.net.Socket;

public class ApplicationNotifier
{
	private static String applicationIP="127.0.0.1";
	private static int applicationPort=50000;
	private static final String MSG_MOVE="MOVE";
	
	public static void NotifyMove(int decision,int pid)
	{
		Socket socket=null;
		PrintWriter out=null;
		String message=MSG_MOVE+"|"+decision+"|"+pid;
		try
		{			
			socket = new Socket(applicationIP, applicationPort);        
			out = new PrintWriter(socket.getOutputStream(), true);
			out.write(message);
			out.flush();
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
		finally
		{
			try
			{
				if(socket!=null)
					socket.close();
				if(out!=null)
					out.close();
			}
			catch(Exception ex)
			{
				ex.printStackTrace();
			}
		}
	}
}
