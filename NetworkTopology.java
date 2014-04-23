package edu.wisc.cs.sdn;

import java.util.Collection;
import java.util.Iterator;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.devicemanager.IDevice;
import net.floodlightcontroller.devicemanager.IDeviceService;
import net.floodlightcontroller.devicemanager.SwitchPort;
import net.floodlightcontroller.linkdiscovery.ILinkDiscoveryService;
import net.floodlightcontroller.routing.Link;

/**
 * Provides an interface to obtain information about the current network topology.
 */
public class NetworkTopology 
{
	// Interface to Floodlight core for interacting with connected switches
	private IFloodlightProviderService floodlightProv;
		
	// Interface to link discovery service
	private ILinkDiscoveryService linkDiscProv;
	
	// Interface to device manager service
	private IDeviceService deviceProv;
	
	// Interface to the logging system
	private static Logger log = 
			LoggerFactory.getLogger(NetworkTopology.class.getSimpleName());
		
	/**
	 * Creates a network topology object.
	 * @param floodlightProv interface to floodlight core service
	 * @param linkDiscProv interface to link discovery service
	 * @param deviceProv interface to device manager service
	 */
	public NetworkTopology(IFloodlightProviderService floodlightProv,
			ILinkDiscoveryService linkDiscProv, IDeviceService deviceProv)
	{
		this.floodlightProv = floodlightProv;
		this.linkDiscProv = linkDiscProv;
		this.deviceProv = deviceProv;
	}
	
	/**
	 * Gets the full topology of switches and links.
	 * @return a list of vertices, where each vertex is a switch
	 */
	public Collection<Vertex> getFullTopology()
	{
        ///////////////////////////////////////////////////////////////////////
		// TODO: Construct the full topology of switches and links (do not 
		//		 include hosts)
		// Hint: you should have a vertex for each switch and you should have 
		//		 edges for each link

                Collection<Vertex> topo = new ArrayList<Vertex>();
		
                log.debug("GETFULLTOPOLOGY");
                Collection<IOFSwitch> allSwitches =  this.getSwitches();
                for(IOFSwitch s : allSwitches){
                    Vertex v = new Vertex(s);
                    topo.add(v);
                }

                Collection<Link> links =  this.getLinks();
                for(Link l : links){
                    //log.debug("src: " + l.getSrc() + " dst: " + l.getDst());
                    
                    //get the source vertex first
                    Vertex src = null;
                    for(Vertex v : topo){
                        if(v.getSwitch().getId() == l.getSrc()){
                           src = v;
                           break;
                        }
                    }

                    //find the destination vertex next
                    Vertex dst = null;
                    for(Vertex v : topo){
                        if(v.getSwitch().getId() == l.getDst()){
                           dst = v;
                           break;
                        }
                    }

                    if(src == null || dst == null){
                       log.error("ERROR link has vertex not found in graph");
                       return null;
                    }

                    src.addNeighbor(dst, l.getSrcPort(), l.getDstPort());
                    //log.debug("src vertex: " + src.getSwitch().getId() + " dst vertex: " + dst.getSwitch().getId() + " src port: " + l.getSrcPort() + " dst port: " + l.getDstPort());

                }
                return topo;

                /*
                log.debug("NUM VERTICES: " + topo.size());
                for(Vertex v : topo){
                    log.debug("vertex " + v.getSwitch().getId() + " has " + v.getAdjacencies().size() + " links");
                }
                */

                //this.printHosts();
                //this.printSwitches();
                //this.printLinks();
                //return null;
		
		///////////////////////////////////////////////////////////////////////
	}

	/**
	 * Gets the switch and port to which a host is connected.
	 * @param mac the host's MAC address
	 */
	public SwitchPort getSwitchPortForHost(long mac)
    {
		// Find device based on MAC address address
		Iterator<? extends IDevice> deviceIterator = 
				deviceProv.queryDevices(mac, null, null, null, null);
		
		// Select first matching device
		if (deviceIterator.hasNext()) 
		{
			IDevice device = deviceIterator.next();
			
			// Get device attachment points
			SwitchPort[] deviceSwitchPorts = device.getAttachmentPoints();
			
			// Select first matching attachment point
			if (deviceSwitchPorts.length >= 1) 
			{
				return deviceSwitchPorts[0];
			}
		}
		return null;
	}
	
	/**
	 * Get a list of all hosts in the network.
	 */
	public Collection<? extends IDevice> getHosts() 
    {
		return deviceProv.getAllDevices();
    }
	
	/**
	 * Print a list of all hosts in the network.
	 */
	public void printHosts() 
    {
        log.debug("HOSTS");
		for (IDevice device : this.getHosts()) 
		{
			log.debug("MAC="+device.getMACAddressString());
		}
	}
	
	/**
	 * Get a list of all switches in the network.
	 */
	public Collection<IOFSwitch> getSwitches() 
    {
		return floodlightProv.getSwitches().values();
    }
	
	/**
	 * Print a list of all switches in the network.
	 */
	public void printSwitches() 
    {
        log.debug("SWITCHES");
		for (IOFSwitch sw : this.getSwitches()) 
		{
			log.debug("SwitchId="+sw.getId());
		}
	}
	
	/**
	 * Get a list of all links in the network.
	 */
	public Collection<Link> getLinks() 
    {
        return linkDiscProv.getLinks().keySet();
	}
	
	/**
	 * Print a list of all links in the network
	 */
	public void printLinks() 
    {
        log.debug("LINKS");
		for (Link link : this.getLinks()) 
		{
			log.debug("SrcSwitch="+link.getSrc()+" SrcPort="+link.getSrcPort()
				+", DstSwitch="+link.getDst()+", DstPort="+link.getDstPort());
		}
	}
}
