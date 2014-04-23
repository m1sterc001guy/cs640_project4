package edu.wisc.cs.sdn;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.openflow.protocol.OFMatch;
import org.openflow.protocol.OFMessage;
import org.openflow.protocol.OFPacketIn;
import org.openflow.protocol.OFPhysicalPort;
import org.openflow.protocol.OFPort;
import org.openflow.protocol.OFType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.packet.Ethernet;

/**
 * Module to perform shortest path routing in a network
 */
public class L2Forward implements IFloodlightModule, IOFMessageListener
{
	// Interface to Floodlight core for interacting with connected switches
	private IFloodlightProviderService floodlightProv;
	
	// Interface to the logging system
		private static Logger log = 
				LoggerFactory.getLogger(L2Forward.class.getSimpleName());
		
	// Interface to install flow rules
	private FlowInstaller flowInstaller;
	
	// Map of learned hosts
	private Map<Long, Short> learnedHosts;

    /**
     * Tell the module system which services we provide.
     */
	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleServices()
    {
		return null;
	}

    /**
     * Tell the module system which services we implement.
     */
	@Override
	public Map<Class<? extends IFloodlightService>, IFloodlightService> 
			getServiceImpls() 
    {
		return null;
	}

	/**
	 * Tell the module system which modules we depend on.
	 */
	@Override
	public Collection<Class<? extends IFloodlightService>> 
			getModuleDependencies() 
    {
		Collection<Class<? extends IFloodlightService >> floodlightService = 
			new ArrayList<Class<? extends IFloodlightService>>();
		floodlightService.add(IFloodlightProviderService.class);
		return floodlightService;
	}

	/**
	 * Loads dependencies and initializes data structures.
	 * Important to override! 
	 */
	@Override
	public void init(FloodlightModuleContext context)
			throws FloodlightModuleException 
    {
		floodlightProv=context.getServiceImpl(IFloodlightProviderService.class);
		flowInstaller = new FlowInstaller();
		learnedHosts = new HashMap<Long, Short>();
	}

	/**
	 * Tells the Floodlight core we are interested in PACKET_IN messages.
	 * */
	@Override
	public void startUp(FloodlightModuleContext context) 
    {
		floodlightProv.addOFMessageListener(OFType.PACKET_IN, this);
	}

	/**
	 * Provides an identifier for our OFMessage listener.
	 */
	@Override
	public String getName() 
	{
		return L2Forward.class.getSimpleName();
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
		
		// Determine the port on which the packet was received
		short inPort = pktInMsg.getInPort();
		
		// Create match based on packet
		OFMatch match = new OFMatch();
		match.loadFromPacket(pktInMsg.getPacketData(), inPort);
		
		// Learn the source MAC and source port
		byte[] srcMac = match.getDataLayerSource();
		learnedHosts.put(Ethernet.toLong(srcMac), inPort);
		
		// TODO: Install a rule in the flow table when we learn a new host 
		
		// Get the destination MAC
		byte[] dstMac = match.getDataLayerDestination();

		// If we know the location of the destination, the forward directly
		if (learnedHosts.containsKey(Ethernet.toLong(dstMac)))
		{
			// Forward packet out port
			short outPort = learnedHosts.get(Ethernet.toLong(dstMac));
			log.debug(String.format("Send out port %d", outPort));
			flowInstaller.forwardPacket(sw, outPort, pktInMsg);
		}
		//Otherwise, flood the packet
		else
		{
			// Get a list of all ports on the switch
			Collection<OFPhysicalPort> ports = sw.getPorts();
	
			// Flood out each port
			for(OFPhysicalPort port : ports)
			{
				// Do not flood out the port on which the packet was received
				// Also, do not flood out the special local port
				if (inPort == port.getPortNumber() 
						|| OFPort.OFPP_LOCAL.getValue() == port.getPortNumber())
				{ continue; }
				
				// Forward packet out port
				log.debug(String.format("Send out port %d", port.getPortNumber()));
				flowInstaller.forwardPacket(sw, port.getPortNumber(), pktInMsg);
			}
		}
		return Command.STOP;
	}
}
