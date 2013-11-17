package dxat.examples.controllermodule.client;

import java.io.IOException;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.traversal.Evaluators;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.graphdb.traversal.Traverser;
import org.neo4j.kernel.Traversal;

import com.google.gson.Gson;

import dxathacks.floodlightcontroller.pojos.ControllerInterface;
import dxathacks.floodlightcontroller.pojos.Host;
import dxathacks.floodlightcontroller.pojos.Interface;
import dxathacks.floodlightcontroller.pojos.InterfacesCollection;
import dxathacks.floodlightcontroller.pojos.Link;
import dxathacks.floodlightcontroller.pojos.PortStatisticsCollection;
import dxathacks.floodlightcontroller.pojos.Switch;

public class ControllerImp implements ControllerInterface {

	private static final String DB_PATH = "/mnt/ram/graph.db";
	private GraphDatabaseService graphDb;
	private static Index<Node> listIfaceDevices; // In order to make indexes
													// over the nodes
	private static Node root;

	private static enum RelTypes implements RelationshipType {
		ELEMENT, HAS, LINK,
	}

	public ControllerImp() {
		// this.killCats();
		Switch sw0 = new Switch();
		Interface itf = new Interface();
		InterfacesCollection itfs = new InterfacesCollection();
		sw0.setInventoryId("SW-0");
		itf.setPortId(0);
		itf.setCurrentSpeed(0);
		itf.setEnabled(true);
		itf.setInventoryId("SW-0:0");
		itf.setMac("LA:CA:SI:TO:SS:!!");
		itf.setStatus(true);
		itfs.getInterfaces().add(itf);
		sw0.setInterfaces(itfs);
		sw0.setDatapath("Sweet home alabama");
		sw0.setHardware("Made in Spain");
		sw0.setManufacturer("Made in HOME");
		sw0.setNports(1);
		sw0.setOfAddr("0:1234:1234");
		sw0.setSerialNum("Bye Bye Men...");
		sw0.setSoftware("Spanion Soft.");
		sw0.setType("MASTER");
		this.addSwitch(sw0);
	}

	@Override
	public void addSwitch(Switch device) {
		setUp();

		Transaction tx = graphDb.beginTx();
		try {
			// Check if the switch already exists
			if (listIfaceDevices.get("inventoryId", device.getInventoryId())
					.getSingle() != null) {
				tx.finish();
				graphDb.shutdown();
				this.updateSwitch(device);
				return;
			}

			Node dev = graphDb.createNode();
			System.out.println("ADD SWITCH(" + dev.getId() + "): "
					+ new Gson().toJson(device));
			dev.setProperty("inventoryId", device.getInventoryId());
			dev.setProperty("status", true);
			dev.setProperty("type", device.getType());
			dev.setProperty("ifaces", device.getNports());
			dev.setProperty("getOfAddr", device.getOfAddr());
			dev.setProperty("software", device.getSoftware());
			dev.setProperty("hardware", device.getHardware());
			dev.setProperty("manufacturer", device.getManufacturer());
			dev.setProperty("serialNumber", device.getSerialNum());
			dev.setProperty("datapath", device.getDatapath());
			dev.setProperty("model", device.getManufacturer());

			listIfaceDevices.add(dev, "inventoryId", device.getInventoryId());

			// Create control interface
			Interface ctlItf = new Interface();
			ctlItf.setInventoryId(device.getInventoryId());
			Node nodectitf = createNodeInterface(ctlItf, "CT");
			dev.createRelationshipTo(nodectitf, RelTypes.HAS);

			// Creating interfaces nodes of the sw
			for (Interface iface : device.getInterfaces().getInterfaces()) {
				// Create the device --> controller relationship
				Node nodeIface = createNodeInterface(iface, "SW");
				// creating the switch --> switch interface relationship
				dev.createRelationshipTo(nodeIface, RelTypes.HAS);
			}

			tx.success();
		} catch (Exception e) {
			System.out.println("[ADD SWITCH EXCEPTION] " + e.getMessage());
		} finally {
			tx.finish();
			graphDb.shutdown();
		}
	}

	@Override
	public void updateSwitch(Switch sw) {
		setUp();
		Transaction tx = graphDb.beginTx();
		try {

			Node saveSw = listIfaceDevices.get("inventoryId",
					sw.getInventoryId()).getSingle();
			if (saveSw == null) {
				throw new Exception("Device '" + sw.getInventoryId()
						+ "' not found.");
			}
			System.out.println("[UPDATE SWITCH (" + saveSw.getId() + ")]");
			saveSw.setProperty("inventoryId", sw.getInventoryId());
			saveSw.setProperty("getOfAddr", sw.getOfAddr());
			saveSw.setProperty("hardware", sw.getHardware());
			saveSw.setProperty("serialNumber", sw.getSerialNum());
			saveSw.setProperty("ifaces", sw.getNports());
			saveSw.setProperty("type", sw.getType());
			saveSw.setProperty("software", sw.getSoftware());
			saveSw.setProperty("manufacturer", sw.getManufacturer());

			Traverser interfacesSw = getInterfaceSwitch(listIfaceDevices.get(
					"inventoryId", sw.getInventoryId()).getSingle());

			for (Path elementsIface : interfacesSw) {
				Node nodeIface = elementsIface.endNode();
				int portId = (int) nodeIface.getProperty("portId");
				for (Interface itf : sw.getInterfaces().getInterfaces()) {
					if (itf.getPortId() == portId) {
						nodeIface.setProperty("inventoryId",
								itf.getInventoryId());
						nodeIface.setProperty("status", itf.getStatus());
						nodeIface.setProperty("enabled", itf.getEnabled());
						nodeIface.setProperty("mac", itf.getMac());
						nodeIface.setProperty("currentSpeed",
								itf.getCurrentSpeed());// 10/100/1000 Mbps
						break;
					}
				}
			}

			tx.success();

		} catch (Exception e) {
			System.out.println("[UPDATE SWITCH EXCEPTION] " + e.getMessage());
		} finally {
			tx.finish();
			graphDb.shutdown();
		}
	}

	@Override
	public void deleteSwitch(String swId) {
		System.out.println("DELETE SWITCH: " + new Gson().toJson(swId));
		setUp();
		Transaction tx = graphDb.beginTx();
		try {
			Node sw = listIfaceDevices.get("inventoryId", swId).getSingle();
			// Get the interfaces from this node
			Traverser elementsTraverserNode = getInterfaceSwitch(sw);
			for (Path elementNode : elementsTraverserNode) {
				// Deleting interface--> interface relationships. Deleting all
				// the LINKS related to this node.
				Relationship pathLink = elementNode.endNode()
						.getSingleRelationship(RelTypes.LINK, Direction.BOTH);
				// Relationship pathHas =
				// elementNode.endNode().getSingleRelationship(RelTypes.HAS,
				// Direction.BOTH);
				if (pathLink != null) {
					elementNode
							.endNode()
							.getSingleRelationship(RelTypes.LINK,
									Direction.BOTH).delete();
					// Deleting interface--> switch relationship
					elementNode
							.endNode()
							.getSingleRelationship(RelTypes.HAS, Direction.BOTH)
							.delete();
				} else
					elementNode
							.endNode()
							.getSingleRelationship(RelTypes.HAS, Direction.BOTH)
							.delete();

				// Deleting interface
				elementNode.endNode().delete();
			}

			// Deleting interface to Controller
			String idCT = "CT-" + sw.getProperty("inventoryId") + ":"
					+ sw.getProperty("portConfig");

			Traverser elementsTraverserController = getInterfaceController(root);
			for (Path elementController : elementsTraverserController) {
				if (idCT.equals(elementController.endNode().getProperty(
						"inventoryId"))) {
					// Deleting interface controller-->controller relationship
					elementController
							.endNode()
							.getSingleRelationship(RelTypes.HAS,
									Direction.INCOMING).delete();
					// Deleting interface controller
					elementController.endNode().delete();
					break;
				}
			}
			// Deleting switch
			listIfaceDevices.remove(sw);
			sw.delete();

			tx.success();

		} finally {
			tx.finish();
			graphDb.shutdown();
		}

	}

	@Override
	public void addLink(Link lnk) {
		setUp();
		Transaction tx = graphDb.beginTx();
		try {
			// Check if the link already exist
			if (this.existLink(lnk)) {
				tx.finish();
				graphDb.shutdown();
				this.updateLink(lnk);
				return;
			}

			// Getting switches that affects the link
			Node srcSwitch = listIfaceDevices.get("inventoryId",
					lnk.getSrcSwitch()).getSingle();
			if (srcSwitch == null)
				throw new Exception("Source switch '" + lnk.getSrcSwitch()
						+ "' not found.");

			Node dstSwitch = listIfaceDevices.get("inventoryId",
					lnk.getDstSwitch()).getSingle();
			if (dstSwitch == null)
				throw new Exception("Destination switch '" + lnk.getDstSwitch()
						+ "' not found.");

			// Getting the interfaces of the switches that affects the links
			Traverser elementsTraverserSrc = getInterfaceSwitch(srcSwitch);
			if (elementsTraverserSrc == null)
				throw new Exception(
						"No traverses found for the source switch '"
								+ lnk.getSrcSwitch() + "'.");

			Traverser elementsTraverserDst = getInterfaceSwitch(dstSwitch);
			if (elementsTraverserDst == null)
				throw new Exception(
						"No traverses found for the destination switch '"
								+ lnk.getDstSwitch() + "'.");

			// Building the interfaces id
			String src = lnk.getSrcSwitch() + ":" + lnk.getSrcPort();
			String dst = lnk.getDstSwitch() + ":" + lnk.getDstPort();

			// Looking for a interface switch of the src
			Node srcIface = null;
			for (Path elementPathSrc : elementsTraverserSrc) {
				if (src.equals(elementPathSrc.endNode().getProperty(
						"inventoryId"))) {
					srcIface = elementPathSrc.endNode();
					break;
				}
			}
			if (srcIface == null)
				throw new Exception("Source interface '" + src + "' not found.");

			// Looking for a interface switch of th dst
			Node dstIface = null;
			for (Path elementPathDst : elementsTraverserDst) {
				if (dst.equals(elementPathDst.endNode().getProperty(
						"inventoryId"))) {
					dstIface = elementPathDst.endNode();
					break;
				}
			}
			if (dstIface == null)
				throw new Exception("Destination interface '" + dst
						+ "' not found.");

			System.out.println("[ADD LINK (" + srcIface.getId() + ")("
					+ dstIface.getId() + ")] " + new Gson().toJson(lnk));

			// Creating the switch interface --> switch interface (LINK)
			// relationship
			Relationship link = srcIface.createRelationshipTo(dstIface,
					RelTypes.LINK);

			link.setProperty("id", lnk.getInventoryId());
			link.setProperty("status", true);
			link.setProperty("srcSwitch", lnk.getSrcSwitch());
			link.setProperty("srcPort", lnk.getSrcPort());
			link.setProperty("dstSwitch", lnk.getDstSwitch());
			link.setProperty("dstPort", lnk.getDstPort());
			link.setProperty("inventoryId", lnk.getInventoryId());
			link.setProperty("type", lnk.getType());

			tx.success();

		} catch (Exception e) {
			System.out.println("[ADD LINK EXCEPTION] " + e.getMessage());
		} finally {
			tx.finish();
			graphDb.shutdown();
		}

	}

	// Implementation for SW-Control link
	public void addInternalLink(Node src, Node dst) {
		try {
			Relationship link = src.createRelationshipTo(dst, RelTypes.LINK);
			link.setProperty("id", (String) src.getProperty("inventoryId")
					+ "-" + (String) dst.getProperty("inventoryId"));

			link.setProperty("status", true);
			link.setProperty("srcSwitch", "");
			link.setProperty("srcPort", "");
			link.setProperty("dstSwitch", "");
			link.setProperty("dstPort", "");
			link.setProperty("type", "");
		} catch (Exception e) {
			System.out.println("[ADD INTERNAL LINK EXCEPTION] "
					+ e.getMessage());
		}
	}

	private boolean existLink(Link lnk) {
		try {
			Transaction tx = graphDb.beginTx();
			// Getting switches that affects the link
			Node srcSwitch = listIfaceDevices.get("inventoryId",
					lnk.getSrcSwitch()).getSingle();

			// Getting the interfaces of the switches that affects the links
			Traverser elementsTraverserSrc = getInterfaceSwitch(srcSwitch);

			// Building the interfaces id
			String src = lnk.getSrcSwitch() + ":" + lnk.getSrcPort();

			// Looking for a interface switch of the src
			for (Path elementPathSrc : elementsTraverserSrc) {
				if (src.equals(elementPathSrc.endNode().getProperty(
						"inventoryId"))) {
					Relationship pathLink = elementPathSrc.endNode()
							.getSingleRelationship(RelTypes.LINK,
									Direction.BOTH);
					if (pathLink != null) {
						return true;
					}
				}
			}
			tx.success();
		} catch (Exception e) {
			return false;
		}

		return false;
	}

	@Override
	public void updateLink(Link lnk) {
		setUp();
		Transaction tx = graphDb.beginTx();
		try {
			if (!this.existLink(lnk)) {
				tx.finish();
				this.graphDb.shutdown();
				this.addLink(lnk);
				return;
			}

			// Getting switches that affects the link
			Node srcSwitch = listIfaceDevices.get("inventoryId",
					lnk.getSrcSwitch()).getSingle();

			// Getting the interfaces of the switches that affects the links
			Traverser elementsTraverserSrc = getInterfaceSwitch(srcSwitch);

			// Building the interfaces id
			String src = lnk.getSrcSwitch() + ":" + lnk.getSrcPort();

			// Looking for a interface switch of the src
			for (Path elementPathSrc : elementsTraverserSrc) {
				if (src.equals(elementPathSrc.endNode().getProperty(
						"inventoryId"))) {
					Relationship pathLink = elementPathSrc.endNode()
							.getSingleRelationship(RelTypes.LINK,
									Direction.BOTH);
					if (pathLink != null) {
						System.out.println("[UPDATE LINK ("
								+ pathLink.getStartNode().getId() + ")("
								+ pathLink.getEndNode().getId() + ")] "
								+ new Gson().toJson(lnk));
						pathLink.setProperty("inventoryId",
								lnk.getInventoryId());
						pathLink.setProperty("srcSwitch", lnk.getSrcSwitch());
						pathLink.setProperty("srcPort", lnk.getSrcPort());
						pathLink.setProperty("dstSwitch", lnk.getDstSwitch());
						pathLink.setProperty("dstPort", lnk.getDstPort());
						pathLink.setProperty("inventoryId",
								lnk.getInventoryId());
						pathLink.setProperty("type", lnk.getType());
					}
					break;
				}
			}
			tx.success();

		} finally {
			tx.finish();
			graphDb.shutdown();
		}
	}

	@Override
	public void deleteLink(Link lnk) {
		System.out.println("DELETE LINK: " + new Gson().toJson(lnk));
		setUp();
		Transaction tx = graphDb.beginTx();
		try {
			if (!this.existLink(lnk))
				throw new Exception("The link '" + lnk.getInventoryId()
						+ "' does not exist");

			// Getting the parameters
			String src = lnk.getSrcSwitch() + ":" + lnk.getSrcPort();

			// Getting Switch nodes that affects links
			Node srcSwitch = listIfaceDevices.get("inventoryId",
					lnk.getSrcSwitch()).getSingle();

			// Getting interfaces of the sw that affects links
			Traverser elementsTraverserSrc = getInterfaceSwitch(srcSwitch);
			// Finding and deleting
			for (Path elementSrc : elementsTraverserSrc) {
				if (src.equals(elementSrc.endNode().getProperty("inventoryId"))) {
					// Deleting link
					Relationship pathLink = elementSrc.endNode()
							.getSingleRelationship(RelTypes.LINK,
									Direction.BOTH);
					if (pathLink != null)
						pathLink.delete();
				}
			}

			tx.success();
		} catch (Exception e) {
			System.out.println("[DELETE LINK EXCEPTION] " + e.getMessage());
		} finally {
			tx.finish();
			graphDb.shutdown();
		}

	}

	@Override
	public void addHost(Host host) {
		setUp();
		Transaction tx = graphDb.beginTx();

		try {
			// Check if the host already exists
			if (listIfaceDevices.get("inventoryId", host.getHostId())
					.getSingle() != null) {
				tx.finish();
				graphDb.shutdown();
				this.updateHost(host);
				return;
			}

			// Create a new Node
			Node newHost = graphDb.createNode();
			System.out.println("[ADD HOST (" + newHost.getId() + ")] "
					+ new Gson().toJson(host));

			// Set Host properties
			newHost.setProperty("inventoryId", host.getHostId());
			newHost.setProperty("DHCP", host.getDhcpName());
			newHost.setProperty("VLAN", host.getVlan());
			newHost.setProperty("IPv4", host.getIpv4());
			newHost.setProperty("MAC", host.getMac());
			newHost.setProperty("SwitchId", host.getMac());
			newHost.setProperty("PortId", host.getMac());

			// Added to index
			listIfaceDevices.add(newHost, "inventoryId", host.getHostId());

			// Create host interface
			Node iface = graphDb.createNode();
			iface.setProperty("inventoryId", host.getHostId() + ":0");
			iface.setProperty("portId", 0);
			iface.setProperty("status", true);
			iface.setProperty("enabled", true);
			iface.setProperty("mac", host.getMac());
			iface.setProperty("ip", host.getIpv4());
			iface.setProperty("currentSpeed", "0.0");// 10/100/1000 Mbps

			// Creating the host --> host interface relationship
			newHost.createRelationshipTo(iface, RelTypes.HAS);

			if (host.getSwId() != "") {
				// Looking for a interface switch to LINK with interface host
				Traverser elementsTraverserSw = getInterfaceSwitch(listIfaceDevices
						.get("inventoryId", host.getSwId()).getSingle());

				if (elementsTraverserSw == null)
					throw new Exception("Switch '" + host.getSwId()
							+ "' Not found.");

				// Looking for a interface switch of the src
				Node swIface = null;
				String swInterface = (String) host.getSwId() + ":"
						+ host.getPortId(); // Neo4j

				// can't accept a list as property
				for (Path elementPathSw : elementsTraverserSw) {
					if (swInterface.equals(elementPathSw.endNode()
							.getProperty("inventoryId").toString())) {
						swIface = elementPathSw.endNode();
						break;
					}
				}

				// Creating LINK interface host --> controllerIface relationship
				if (swIface == null) {
					throw new Exception("Switch Interface '" + swInterface
							+ "' does not exist");
				} else {
					addInternalLink(iface, swIface);
				}
			}
			// Send Transactions
			tx.success();
		} catch (Exception e) {
			System.out.println("[ADD HOST EXCEPTION] " + e.getMessage());
		} finally {
			tx.finish();
			graphDb.shutdown();
		}
	}

	@Override
	public void updateHost(Host host) {
		setUp();
		Transaction tx = graphDb.beginTx();
		try {
			// Get node Host
			Node saveHost = listIfaceDevices.get("inventoryId",
					host.getHostId()).getSingle();

			// Check the existence of the host
			if (saveHost == null) {
				tx.finish();
				graphDb.shutdown();
				this.addHost(host);
				return;
			}

			System.out.println("[UPDATE HOST (" + saveHost.getId() + ")] "
					+ new Gson().toJson(host));

			// Set new properties
			saveHost.setProperty("inventoryId", host.getHostId());
			saveHost.setProperty("DHCP", host.getDhcpName());
			saveHost.setProperty("VLAN", host.getVlan());
			saveHost.setProperty("IPv4", host.getIpv4());
			saveHost.setProperty("MAC", host.getMac());
			saveHost.setProperty("SwitchId", host.getMac());
			saveHost.setProperty("PortId", host.getMac());

			tx.success();
		} catch (Exception e) {
			System.out.println("[UPDATE HOST EXCEPTION] " + e.getMessage());
		} finally {
			tx.finish();
			graphDb.shutdown();
		}

		// Create or destroy link if necessary
		Link lnk = new Link();
		lnk.setDstPort(host.getPortId());
		lnk.setDstSwitch(host.getSwId());
		lnk.setInventoryId(host.getHostId() + ":0_" + host.getSwId() + ":"
				+ host.getPortId());
		lnk.setSrcPort(0);
		lnk.setSrcSwitch(host.getHostId());
		if (host.getSwId() != "") {
			this.updateLink(lnk);
		} else {
			this.deleteLink(lnk);
		}
	}

	@Override
	public void deleteHost(Host host) {
		System.out.println("DELETE HOST: " + new Gson().toJson(host));
		setUp();
		Transaction tx = graphDb.beginTx();
		try {
			// Getting the host node
			Node pc = listIfaceDevices.get("inventoryId", host.getHostId())
					.getSingle();
			// Getting inventoryId of the interface
			String idIface = host.getHostId() + ":0"; // Neo4j doen't accept
														// passing a list as
														// property
			// Getting the interface of the host node
			Traverser elementsTraverserIfaces = getInterfaceSwitch(pc);

			for (Path elementIface : elementsTraverserIfaces) {
				if (idIface.equals(elementIface.endNode().getProperty(
						"inventoryId"))) {
					Relationship pathLink = elementIface.endNode()
							.getSingleRelationship(RelTypes.LINK,
									Direction.BOTH);
					if (pathLink != null) {
						// Deleting links
						pathLink.delete();
						// Deleting host --> interface host relationship
						elementIface
								.endNode()
								.getSingleRelationship(RelTypes.HAS,
										Direction.BOTH).delete();
					} else
						elementIface
								.endNode()
								.getSingleRelationship(RelTypes.HAS,
										Direction.BOTH).delete();
					// Deleting interface host
					elementIface.endNode().delete();
				}
			}
			// Removing host from the index
			listIfaceDevices.remove(pc);
			// Removing host
			pc.delete();

			tx.success();
		} finally {
			tx.finish();
			graphDb.shutdown();
		}

	}

	public void setUp() {
		try {
			graphDb = new GraphDatabaseFactory().newEmbeddedDatabase(DB_PATH);
		} catch (Exception e) {
			// this.killCats();
			graphDb = new GraphDatabaseFactory().newEmbeddedDatabase(DB_PATH);
		}
		listIfaceDevices = graphDb.index().forNodes("inventoryId");

		root = getControllerNode();
	}

	private Node getControllerNode() {
		if (listIfaceDevices.get("inventoryId", "SDNnetwork").getSingle() == null) {
			Transaction tx = graphDb.beginTx();

			try {

				// Create base network node
				Node network = graphDb.createNode();
				network.setProperty("name", "SDNnetwork");
				network.setProperty("inventoryId", "SDNnetwork");
				listIfaceDevices.add(network, "inventoryId", "SDNnetwork");
				// SDNNodeId = network.getId();

				// Create controller node
				Node controller = graphDb.createNode();
				controller.setProperty("inventoryId", "SDNcontroller");
				listIfaceDevices
						.add(controller, "inventoryId", "SDNcontroller");

				// Network-controller relationship
				network.createRelationshipTo(controller, RelTypes.ELEMENT);

				tx.success();
				tx.finish();
				return controller;
			} catch (Exception e) {
				System.out.println("[GET CONTROLLER EXCEPTION] "
						+ e.getMessage());
			} finally {
				tx.finish();
			}
			return null;
		} else {
			return listIfaceDevices.get("inventoryId", "SDNcontroller")
					.getSingle();
		}

	}

	private Node createNodeInterface(Interface interf, String type)
			throws Exception {
		// Create node interface
		Node iface = graphDb.createNode();
		System.out.println("[CREATING ITF (" + iface.getId() + ")]"
				+ new Gson().toJson(interf));
		// Set interface properties
		iface.setProperty("inventoryId", interf.getInventoryId());
		iface.setProperty("portId", interf.getPortId());
		iface.setProperty("status", interf.getStatus());
		iface.setProperty("enabled", interf.getEnabled());
		iface.setProperty("mac", interf.getMac());
		iface.setProperty("currentSpeed", interf.getCurrentSpeed());// 10/100/1000
																	// Mbps
		if (type == "SW" || type == "HOST")
			iface.setProperty("ip", "0.0.0.0"); // NGTH
		else if (type == "CT") {
			// Creating a interface for the controller
			Node controllerIface = createInterfaceController("CT-"
					+ interf.getInventoryId());
			// Creating the controller --> controllerIface relationship
			root.createRelationshipTo(controllerIface, RelTypes.HAS);
			// Creating LINK interface switch --> controllerIface
			// relationship
			iface.createRelationshipTo(controllerIface, RelTypes.LINK);
		} else {
			throw new Exception("Type not allowed");
		}
		return iface;

	}

	private Node createInterfaceController(String name) {
		Node contIface = graphDb.createNode();
		contIface.setProperty("inventoryId", name);
		contIface.setProperty("port", 6633);
		contIface.setProperty("ip", "0.0.0.0");
		return contIface;
	}

	private static Traverser getInterfaceSwitch(final Node element) {
		TraversalDescription td = Traversal.description().breadthFirst()
				.relationships(RelTypes.HAS, Direction.OUTGOING)
				.evaluator(Evaluators.excludeStartPosition());
		td.traverse(element);
		return td.traverse(element);
	}

	private Traverser getInterfaceController(final Node element) {
		TraversalDescription td = Traversal.description().breadthFirst()
				.relationships(RelTypes.HAS, Direction.OUTGOING)
				.evaluator(Evaluators.excludeStartPosition());
		td.traverse(element);
		return td.traverse(element);
	}

	private void killCats() {
		System.out
				.println("[ Cats have several lives, 7 are not too much ... ]");
		try {
			Process p = Runtime.getRuntime().exec("rm -R " + DB_PATH);
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			p.destroy();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void pushStatistics(PortStatisticsCollection stats) {
		System.out.println("PUSHSTAT: " + new Gson().toJson(stats));
	}

}
