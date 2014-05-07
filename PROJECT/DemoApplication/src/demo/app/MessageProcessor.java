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
		int round=-1;
		int cin=-1;
		if(messageArray[0].equals(MSG_MOVE))
		{
			direction=DemoUtil.ConvertStringToUnt(messageArray[1]);
			pid=DemoUtil.ConvertStringToUnt(messageArray[2]);
			round=DemoUtil.ConvertStringToUnt(messageArray[3]);
			cin=DemoUtil.ConvertStringToUnt(messageArray[4]);
			DrawBoard.UpdateMovement(direction, pid,round,cin);
		}
		else
			System.out.println("Unknown message");				
				
		
	}	
}
