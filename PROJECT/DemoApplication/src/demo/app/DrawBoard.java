package demo.app;

import java.awt.*;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.TimerTask;
import java.util.Timer;

import javax.swing.*;

public class DrawBoard {
	private final static boolean RIGHT_TO_LEFT = false;
	private static int BOARD_SIZE = 5;
	private static HashMap<Integer, Agent> agentList;
	private static JButton[][] buttonGrid;
	private static final Color GRID_CELL_COLOR = Color.white;
	private static Color AGENT_CELL_COLOR;
	private static final Color PREVIOUS_CELL_COLOR = Color.gray;
	private static final Color FAILED_CELL_COLOR = Color.red;
	private static final int FAIL_CHECK_PERIOD = 800000;

	public static void addComponentsToPane(Container contentPane) {
		if (RIGHT_TO_LEFT) {
			contentPane
					.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
		}
		contentPane.setLayout(new GridLayout(BOARD_SIZE, BOARD_SIZE));
		buttonGrid = new JButton[BOARD_SIZE][BOARD_SIZE];
		for (int y = 0; y < BOARD_SIZE; y++) {
			for (int x = 0; x < BOARD_SIZE; x++) {
				buttonGrid[x][y] = new JButton();
				buttonGrid[x][y].setBackground(GRID_CELL_COLOR);
				buttonGrid[x][y].setBorder(BorderFactory
						.createLineBorder(Color.WHITE));
				contentPane.add(buttonGrid[x][y]);
			}
		}
	}

	public static void createAndShowGUI(String filePath) {
		LoadAgents(filePath);
		JFrame.setDefaultLookAndFeelDecorated(true);
		JFrame frame = new JFrame("CONSENSUS DECISION BOARD");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		addComponentsToPane(frame.getContentPane());
		RenderAgents();
		frame.pack();
		frame.setVisible(true);
	}

	private static void LoadAgents(String filePath) {
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new InputStreamReader(
					new FileInputStream(filePath)));

			String line;
			boolean hasMoreLines = true;
			int counter = 0;
			// reads lines of type: <process number> <IP address> <port>
			while (hasMoreLines) {
				line = reader.readLine();
				if (line == null) {
					break;
				} else {
					counter++;
				}
			}
			if (BOARD_SIZE < counter)
				BOARD_SIZE = counter;
			agentList = new HashMap<Integer, Agent>();
			for (int i = 0; i < counter; i++) {
				agentList.put(i, new Agent(i, 3, i));
			}

		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	private static void RenderAgents() {
		Agent agent;
		JButton button;
		for (int i = 0; i < agentList.size(); i++) {
			agent = agentList.get(i);
			button = buttonGrid[agent.getX()][agent.getY()];
			button.setBackground(AGENT_CELL_COLOR);
			button.setText("PID=" + agent.getPid());
		}

		// Schedule timer to detect failed agents
		Timer timer = new Timer(true);
		timer.scheduleAtFixedRate(new TimerTask() {
			public void run() {
				Agent agent;
				JButton button;
				long currentTime = System.currentTimeMillis();
				for (int i = 0; i < agentList.size(); i++) {
					agent = agentList.get(i);
					button = buttonGrid[agent.getX()][agent.getY()];
					if (currentTime - agent.getLastUpdateTime() > FAIL_CHECK_PERIOD) {
						button.setBackground(FAILED_CELL_COLOR);
						button.setOpaque(true);
						button.setText("PID=" + agent.getPid());
					}
				}

			}
		}, 0, 10 * 1000);
	}

	public static void UpdateMovement(int direction, int pid, int round, int cin) {
		Agent agent = agentList.get(pid);
		agent.setLastUpdateTime();
		int x = agent.getX();
		int y = agent.getY();
		JButton button;
		if (agent.getPreviousx() >= 0 && agent.getPreviousy() >= 0) {
			button = buttonGrid[agent.getPreviousx()][agent.getPreviousy()];
			button.setBackground(GRID_CELL_COLOR);
			button.setOpaque(true);
			button.setText("");
		}

		// before move set current values to the previous
		agent.setPreviousx(x);
		agent.setPreviousy(y);
		button = buttonGrid[x][y];
		button.setBackground(PREVIOUS_CELL_COLOR);
		button.setText("PID=" + agent.getPid());
		//1 = Up - Green
		//2 = Up - Blue
		//3 = Down - Yellow
		//4 = Down - Pink
		if (direction == 1) {
			AGENT_CELL_COLOR = Color.green;
			if (y == 0) {
				agent.setY(BOARD_SIZE - 1);
			} else {
				agent.setY(y - 1);
			}
		} else if (direction == 3) {
			AGENT_CELL_COLOR = Color.yellow;
			if (y == (BOARD_SIZE - 1))
				agent.setY(0);
			else
				agent.setY(y + 1);
		} else if (direction == 2) {
			AGENT_CELL_COLOR = Color.blue;
			if (y == 0) {
				agent.setY(BOARD_SIZE - 1);
			} else {
				agent.setY(y - 1);
			}
		} else if (direction == 4) {
		    AGENT_CELL_COLOR = Color.pink;
			if (y == (BOARD_SIZE - 1))
				agent.setY(0);
			else
				agent.setY(y + 1);
		}
		// draw after move
		button = buttonGrid[agent.getX()][agent.getY()];
		button.setBackground(AGENT_CELL_COLOR);
		button.setOpaque(true);
		button.setText("PID=" + agent.getPid() + " CIN=" + cin + " RND="
				+ round+" DES="+direction);
	}

}

class Agent {
	private int currentx, currenty, previousx = -1, previousy = -1, pid;
	private long lastUpdateTime;

	public Agent(int currentx, int currenty, int pid) {
		this.currentx = currentx;
		this.currenty = currenty;
		this.pid = pid;
		lastUpdateTime = System.currentTimeMillis();
	}

	public int getPid() {
		return pid;
	}

	public long getLastUpdateTime() {
		return lastUpdateTime;
	}

	public void setLastUpdateTime() {
		this.lastUpdateTime = System.currentTimeMillis();
	}

	public int getPreviousx() {
		return previousx;
	}

	public void setPreviousx(int previousx) {
		this.previousx = previousx;
	}

	public int getPreviousy() {
		return previousy;
	}

	public void setPreviousy(int previousy) {
		this.previousy = previousy;
	}

	public int getX() {
		return currentx;
	}

	public void setX(int x) {
		this.currentx = x;
	}

	public int getY() {
		return currenty;
	}

	public void setY(int y) {
		this.currenty = y;
	}

}