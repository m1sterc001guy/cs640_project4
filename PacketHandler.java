package edu.wisc.cs.sdn;

import java.util.Collection;
import java.util.List;
import java.util.Arrays;

import org.openflow.protocol.OFMatch;
import org.openflow.protocol.OFMessage;
import org.openflow.protocol.OFPacketIn;
import org.openflow.protocol.OFType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.util.MACAddress;
import net.floodlightcontroller.devicemanager.IDevice;
import net.floodlightcontroller.devicemanager.SwitchPort;
import net.floodlightcontroller.devicemanager.SwitchPort.ErrorStatus;


/**
 * Handle packet-in messages from switches.
 */
public class PacketHandler implements IOFMessageListener 
{	
	private NetworkTopology netTopo;
	
	private FlowInstaller flowInstaller;
	
	// Interface to the logging system
	protected static Logger log = 
			LoggerFactory.getLogger(PacketHandler.class.getSimpleName());
	
	/**
	 * Create a new object to handle packet-in messages from switches.
	 * @param netTopo interface to access network topology information
	 * @param flowInstaller interface to install flow table rules
	 */
	public PacketHandler(NetworkTopology netTopo, FlowInstaller flowInstaller)
	{
		this.netTopo = netTopo;
		this.flowInstaller = flowInstaller;
	}
	
	/**
	 * Provides an identifier for our OFMessage listener.
	 */
	@Override
	public String getName() 
    {
		return Routing.class.getSimpleName();
	}

    /**
     * Tell the module system if another module must be called before us.
     */
	@Override
	public boolean isCallbackOrderingPrereq(OFType type, String name) 
    {
		return false;
	}

    /**
     * Tell the module system if another module must be called after us.
     */
	@Override
	public boolean isCallbackOrderingPostreq(OFType type, String name) 
    {
		return false;
	}
	
	/**
	 * Receives an OpenFlow message from the Floodlight core and initiates the 
	 * appropriate control logic.
	 */
	@Override
	public Command receive(IOFSwitch sw, OFMessage msg, FloodlightContext cntx) 
    {
		// We only care about packet-in messages
		if (msg.getType() != OFType.PACKET_IN) 
		 { return Command.CONTINUE; }
		OFPacketIn pktInMsg = (OFPacketIn)msg;
		
		// Create match based on packet
        OFMatch match = new OFMatch();
        match.loadFromPacket(pktInMsg.getPacketData(), pktInMsg.getInPort());
        
        // Ignore packets of type 0x86DD
        if ((short)0x86DD == match.getDataLayerType())
        { return Command.CONTINUE; }
        
		log.debug(String.format("Received a packet-in message from switch %d",
				sw.getId()));
		
        installPathForFlow(sw, pktInMsg);
       
		return Command.CONTINUE;
    }
	
	
	/**
	 * Performs flow installation based on a packet-in OpenFlow message for an 
	 * IPv4 packet.
	 */
	private void installPathForFlow(IOFSwitch inSwitch, OFPacketIn pktInMsg) 
    {	
		// Create match based on packet
        OFMatch match = new OFMatch();
        match.loadFromPacket(pktInMsg.getPacketData(), pktInMsg.getInPort());

        log.debug("INSTALL PATH FOR FLOW CALLED!!!!");



        byte[] dstMacBytes = match.getDataLayerDestination();
        MACAddress dstMac = new MACAddress(dstMacBytes);
        long dstMacLong = dstMac.toLong();

        IDevice device = null;
        for(IDevice d : netTopo.getHosts()){
            long macToCompare = d.getMACAddress();
            if(macToCompare == dstMacLong){
               device = d;
               break;
            }
        }

        //this should only return 1 switchPort right?
        //if it doesnt, that would imply that this host is connected to 2 switches

        SwitchPort[] switchPorts = device.getAttachmentPoints();
        long dstId = -1;
        short dstPort = -1;
        for(int i = 0; i < switchPorts.length; i++){
            dstId = switchPorts[i].getSwitchDPID();
            dstPort = (short)switchPorts[i].getPort();
        }

        if(dstId == -1 || dstPort == -1){
            log.error("ERROR dstId never found");
            return;
        }

        
        ///////////////////////////////////////////////////////////////////////
        
        // Get the full network topology
        Collection<Vertex> fullTopo = netTopo.getFullTopology();
        
        Vertex srcVertex = null;
        Vertex dstVertex = null;
        
        for(Vertex v : fullTopo){
            if(v.getSwitch().getId() == dstId){
               dstVertex = v;
               break;
            }
        }

        for(Vertex v : fullTopo){
            if(v.getSwitch().getId() == inSwitch.getId()){
               srcVertex = v;
               break;
            }
        }

        
        ///////////////////////////////////////////////////////////////////////
        
        if (null == srcVertex || null == dstVertex)
        {
        	log.error("Missing source and/or destination vertex");
        	return;
        }
        
        
        // Find the shortest path through the network from source to destination
        Dijkstra.computePaths(srcVertex);
        List<Vertex> path = Dijkstra.getShortestPathTo(dstVertex);

        FlowInstaller installer = new FlowInstaller();
        if(srcVertex.compareTo(dstVertex) == 0){
           installer.installRule(inSwitch, pktInMsg.getInPort(), dstPort, match);
           installer.forwardPacket(inSwitch, dstPort, pktInMsg);
           return;
        }

        for(int i = 0; i < path.size() - 1; i++){
            Vertex currVertex = path.get(i);
            Vertex nextVertex = path.get(i+1);
            Edge edge = currVertex.getEdgeToNeighbor(nextVertex);
            installer.installRule(currVertex.getSwitch(), pktInMsg.getInPort(), edge.getSrcSwitchPort(), match);
        } 

        Vertex currVertex = path.get(0);
        Vertex nextVertex = path.get(1);
        Edge edge = currVertex.getEdgeToNeighbor(nextVertex);
        installer.forwardPacket(currVertex.getSwitch(), edge.getSrcSwitchPort(), pktInMsg);

        
        ///////////////////////////////////////////////////////////////////////
	}
}
