package demo.app;

public class DemoApplication
{
	private static String filePath;
	public static void main(String[] args)
	{
		filePath=args[0];
		javax.swing.SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				DrawBoard.createAndShowGUI(filePath);
			}
		});
		
		try
		{
			Communicator c = new Communicator();
			c.ServerListen();
		} catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}
}
