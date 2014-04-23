package edu.wisc.cs.sdn;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.openflow.protocol.OFType;

import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.devicemanager.IDeviceService;
import net.floodlightcontroller.linkdiscovery.ILinkDiscoveryService;

/**
 * Module to perform shortest path routing in a network
 */
public class Routing implements IFloodlightModule 
{
	// Interface to Floodlight core for interacting with connected switches
	private IFloodlightProviderService floodlightProv;
	
	// Interface to link discovery service
	private ILinkDiscoveryService linkDiscProv;
	
	// Interface to device manager service
	private IDeviceService deviceProv;
	
	// Interface for obtaining network topology information
	private NetworkTopology netTopo;
	
	// Handler for packet-in messages
	private PacketHandler pktHandler;
	
	// Interface to install flow rules
	private FlowInstaller flowInstaller;

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
		floodlightService.add(ILinkDiscoveryService.class);
		floodlightService.add(IDeviceService.class);
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
		linkDiscProv = context.getServiceImpl(ILinkDiscoveryService.class);
		deviceProv = context.getServiceImpl(IDeviceService.class);
		netTopo = new NetworkTopology(floodlightProv, linkDiscProv, deviceProv);
		flowInstaller = new FlowInstaller();
		pktHandler = new PacketHandler(netTopo, flowInstaller);
	}

	/**
	 * Tells the Floodlight core we are interested in PACKET_IN messages.
	 * */
	@Override
	public void startUp(FloodlightModuleContext context) 
    {
		floodlightProv.addOFMessageListener(OFType.PACKET_IN, pktHandler);
	}
}
