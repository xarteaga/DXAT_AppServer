package dxat.examples.controllermodule.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

import com.google.gson.Gson;

import dxathacks.floodlightcontroller.pojos.Command;
import dxathacks.floodlightcontroller.pojos.ControllerInterface;
import dxathacks.floodlightcontroller.pojos.Host;
import dxathacks.floodlightcontroller.pojos.Link;
import dxathacks.floodlightcontroller.pojos.PortStatisticsCollection;
import dxathacks.floodlightcontroller.pojos.Switch;

public class ModuleClient implements Runnable {
	private String serverAddr = "";
	private int serverPort = -1;
	private Socket socket = null;
	private BufferedReader reader = null;
	private PrintWriter writer = null;
	private ControllerInterface controller = null;

	public ModuleClient(String serverAddr, int serverPort,
			ControllerInterface controller) {
		this.serverAddr = serverAddr;
		this.serverPort = serverPort;
		this.controller = controller;
	}

	private void getSwitches() {
		Command cmd = new Command();
		cmd.setEvent(Command.GET_SWITCHES);
		cmd.setSource(Switch.class.toString());
		cmd.setObject("ALL_SWITCHES");
		this.sendCommand(cmd);
	}

	private void getHosts() {
		Command cmd = new Command();
		cmd.setEvent(Command.GET_HOSTS);
		cmd.setSource(Host.class.toString());
		cmd.setObject("ALL_HOSTS");
		this.sendCommand(cmd);
	}

	private void getLinks() {
		Command cmd = new Command();
		cmd.setEvent(Command.GET_LINKS);
		cmd.setSource(Link.class.toString());
		cmd.setObject("ALL_LINKS");
		this.sendCommand(cmd);
	}

	private void sendCommand(Command cmd) {
		try {
			this.writer.write(new Gson().toJson(cmd) + "\n");
			this.writer.flush();
		} catch (Exception e) {
			this.printException(e);
		}
	}

	private void processCommand(Command cmd) {
		if (cmd.getEvent().equals(Command.ADD_SWITCH)) {
			controller.addSwitch(new Gson().fromJson(cmd.getObject(),
					Switch.class));
		} else if (cmd.getEvent().equals(Command.DELETE_SWITCH)) {
			controller.deleteSwitch(cmd.getObject());
		} else if (cmd.getEvent().equals(Command.UPDATE_SWITCH)) {
			controller.updateSwitch(new Gson().fromJson(cmd.getObject(),
					Switch.class));
		} else if (cmd.getEvent().equals(Command.ADD_LINK)) {
			controller
					.addLink(new Gson().fromJson(cmd.getObject(), Link.class));
		} else if (cmd.getEvent().equals(Command.DELETE_LINK)) {
			controller.deleteLink(new Gson().fromJson(cmd.getObject(),
					Link.class));
		} else if (cmd.getEvent().equals(Command.UPDATE_LINK)) {
			controller.updateLink(new Gson().fromJson(cmd.getObject(),
					Link.class));
		} else if (cmd.getEvent().equals(Command.ADD_HOST)) {
			controller
					.addHost(new Gson().fromJson(cmd.getObject(), Host.class));
		} else if (cmd.getEvent().equals(Command.DELETE_HOST)) {
			controller.deleteHost(new Gson().fromJson(cmd.getObject(),
					Host.class));
		} else if (cmd.getEvent().equals(Command.UPDATE_HOST)) {
			controller.updateHost(new Gson().fromJson(cmd.getObject(),
					Host.class));
		} else if (cmd.getEvent().equals(Command.PUSH_STATS)) {
			controller.pushStatistics(new Gson().fromJson(cmd.getObject(),
					PortStatisticsCollection.class));
		} else {
			System.out.println("WARNING!! Command not implemented: '"
					+ cmd.getEvent() + "");
		}
	}

	private void connect() {
		// Try connect while is not connected
		while (socket == null) {
			try {
				System.out.println("Trying to connect to the server...");
				// Try create the soccket
				this.socket = new Socket(this.serverAddr, this.serverPort);
				
				// If socket created, set writer and reader
				this.reader = new BufferedReader(new InputStreamReader(
						this.socket.getInputStream()));
				this.writer = new PrintWriter(new OutputStreamWriter(
						socket.getOutputStream()));
			} catch (Exception e) {
				try {
					// If Exception, wait 2 seconds
					Thread.sleep(2000);
				} catch (InterruptedException e1) {
					// Do nothing ...
				}
			}

		}
	}

	public void run() {
		// Forever while
		while (true) {
			// Connect and get writer and reader
			this.connect();
			System.out.println("Connected to the server...");

			// Get current Hosts, Links and Switches
			this.getSwitches();
			this.getLinks();
			this.getHosts();

			// Reader
			while (!socket.isClosed()) {
				try {
					String response = reader.readLine();
					Command cmd = new Gson().fromJson(response, Command.class);
					this.processCommand(cmd);
				} catch (IOException e) {
					try {
						socket.close();
					} catch (IOException e1) {
					}
				}
			}
			socket = null;
		}
	}

	private void printException(Exception e) {
		System.out.println("DXAT CLIENT: Error: '" + e.getMessage() + "'");
	}
}
