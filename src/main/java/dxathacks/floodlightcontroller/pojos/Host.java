package dxathacks.floodlightcontroller.pojos;

public class Host {
	private String dhcpName = "";
	private String hostId = "";
	private String mac = "00:00:00:00:00:00";
	private String ipv4 = "x.x.x.x"; // List of IPv4 interfaces
	private int vlan = 1; // VLAN tags
	private String swId = ""; // Switch where it is connected
	private int portId = 0; // Port where it is connected

	public String getDhcpName() {
		return dhcpName;
	}

	public void setDhcpName(String dhcpName) {
		this.dhcpName = dhcpName;
	}

	public String getHostId() {
		return hostId;
	}

	public void setHostId(String hostId) {
		this.hostId = hostId;
	}

	public String getMac() {
		return mac;
	}

	public void setMac(String mac) {
		this.mac = mac;
	}

	public String getIpv4() {
		return ipv4;
	}

	public void setIpv4(String ipv4) {
		this.ipv4 = ipv4;
	}

	public int getVlan() {
		return vlan;
	}

	public void setVlan(int vlan) {
		this.vlan = vlan;
	}

	public String getSwId() {
		return swId;
	}

	public void setSwId(String swId) {
		this.swId = swId;
	}

	public int getPortId() {
		return portId;
	}

	public void setPortId(int portId) {
		this.portId = portId;
	}

}