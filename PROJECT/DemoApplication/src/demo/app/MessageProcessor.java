package demo.app;

public class MessageProcessor
{
	private static final String MSG_MOVE="MOVE";
	
	public static void ProcessMessage(String message)
	{
		//message 1 => MOVE|DIRECTION|PID ,i.e => MOVE|1|5
		String[] messageArray=message.split("\\|");
		int direction=-1;
		int pid=-1;
		if(messageArray[0].equals(MSG_MOVE))
		{
			direction=DemoUtil.ConvertStringToUnt(messageArray[1]);
			pid=DemoUtil.ConvertStringToUnt(messageArray[2]);
			DrawBoard.UpdateMovement(direction, pid);
		}
		else
			System.out.println("Unknown message");				
				
		
	}	
}
