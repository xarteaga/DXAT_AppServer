package dxat.examples.controllermodule.client;

public class Test {

	public static void main(String[] args) {
		System.out.println("Loading app...");
		ModuleClient client = new ModuleClient("192.168.1.13", 7666, new ControllerImp());
		Thread clientThreat = new Thread(client, "Controller interface thread");
		clientThreat.start();
	}

}
