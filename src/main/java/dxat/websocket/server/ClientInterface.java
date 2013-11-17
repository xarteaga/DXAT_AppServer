package dxat.websocket.server;

import dxathacks.floodlightcontroller.pojos.Host;
import dxathacks.floodlightcontroller.pojos.Link;
import dxathacks.floodlightcontroller.pojos.PortStatisticsCollection;
import dxathacks.floodlightcontroller.pojos.Switch;

public interface ClientInterface {
	
	//Get info switch
	public void sendSwitch(Switch sw);

	public void Switch(Switch sw);

	public void deleteSwitch(String swId);

	// Links Operations
	public void addLink(Link lnk);

	public void updateLink(Link lnk);

	public void deleteLink(Link lnk);

	// Hosts Operations
	public void addHost(Host host);

	public void updateHost(Host host);

	public void deleteHost(Host host);
	
	// Push Statistics
	public void pushStatistics (PortStatisticsCollection stats);

}
