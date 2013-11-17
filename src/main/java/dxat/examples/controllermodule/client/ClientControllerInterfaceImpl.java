package dxat.examples.controllermodule.client;

import com.google.gson.Gson;

import dxathacks.floodlightcontroller.pojos.ControllerInterface;
import dxathacks.floodlightcontroller.pojos.Host;
import dxathacks.floodlightcontroller.pojos.Link;
import dxathacks.floodlightcontroller.pojos.PortStatisticsCollection;
import dxathacks.floodlightcontroller.pojos.Switch;

public class ClientControllerInterfaceImpl implements ControllerInterface {

	public void addSwitch(Switch sw) {
		System.out.println("ADD Switch: " + new Gson().toJson(sw));
	}

	public void updateSwitch(Switch sw) {
		System.out.println("UPDATE Switch: " + new Gson().toJson(sw));
	}

	public void deleteSwitch(String swId) {
		System.out.println("DELETE Switch: " + swId);
	}

	public void addLink(Link lnk) {
		System.out.println("LINK ADDED: " + new Gson().toJson(lnk));
	}

	public void updateLink(Link lnk) {
		System.out.println("LINK UPDATED: " + new Gson().toJson(lnk));
	}

	@Override
	public void deleteLink(Link lnk) {
		System.out.println("LINK DELETED: " + new Gson().toJson(lnk));
	}

	@Override
	public void addHost(Host host) {
		System.out.println("HOST ADDED: " + new Gson().toJson(host));
	}

	@Override
	public void updateHost(Host host) {
		System.out.println("HOST UPDATED: " + new Gson().toJson(host));
	}

	@Override
	public void deleteHost(Host host) {
		System.out.println("HOST DELETED: " + new Gson().toJson(host));
	}

	@Override
	public void pushStatistics(PortStatisticsCollection stats) {
		System.out.println("STATS PUSHED: " + new Gson().toJson(stats));
	}

}
