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

        ///////////////////////////////////////////////////////////////////////
        // TODO: Get destination MAC & find where destination host is connected
        // Hint: You can get the destination MAC address of the packet from the 
        //       match structure
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

        //log.debug(device.toString());
        //this should only return 1 switchPort right?
        //if it doesnt, that would imply that this host is connected to 2 switches

        SwitchPort[] switchPorts = device.getAttachmentPoints();
        long dstId = -1;
        for(int i = 0; i < switchPorts.length; i++){
            log.debug("this dst host is connected to vertex: " + switchPorts[i].getSwitchDPID());
            dstId = switchPorts[i].getSwitchDPID();
        }

        if(dstId == -1){
            log.error("ERROR dstId never found");
            return;
        }

        
        ///////////////////////////////////////////////////////////////////////
        
        // Get the full network topology
        Collection<Vertex> fullTopo = netTopo.getFullTopology();
        
        ///////////////////////////////////////////////////////////////////////
        // TODO: Determine the source and destination switches (or vertices) in 
        //       the network graph. Make sure you choose vertex objects in the
        //       fullTopo collection!
        
        Vertex srcVertex = null; // Hint: Where was the packet received from?
        Vertex dstVertex = null; // Hint: Where is the host that the packet is 
        						 //		  destined for?
        
        for(Vertex v : fullTopo){
            if(v.getSwitch().getId() == dstId){
               dstVertex = v;
               log.debug("setting dstVertex...");
               break;
            }
        }

        for(Vertex v : fullTopo){
            if(v.getSwitch().getId() == inSwitch.getId()){
               srcVertex = v;
               log.debug("setting srcVertex...");
               break;
            }
        }


        
        ///////////////////////////////////////////////////////////////////////
        
        if (null == srcVertex || null == dstVertex)
        {
        	log.error("Missing source and/or destination vertex");
        	return;
        }
        
        log.debug(String.format("SrcSwitch = %s, DstSwitch = %s", 
        		srcVertex, dstVertex));
        
        // Find the shortest path through the network from source to destination
        Dijkstra.computePaths(srcVertex);
        List<Vertex> path = Dijkstra.getShortestPathTo(dstVertex);

        ///////////////////////////////////////////////////////////////////////
        // TODO: Install flow rules in all switches along the path
        // Hint: Don't forget to handle the case where both source and
        //       and destination hosts are connect to the same switch

        
        
        ///////////////////////////////////////////////////////////////////////
	}
}