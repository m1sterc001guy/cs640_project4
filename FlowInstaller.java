package edu.wisc.cs.sdn;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.openflow.protocol.OFFlowMod;
import org.openflow.protocol.OFMatch;
import org.openflow.protocol.OFPacketIn;
import org.openflow.protocol.OFPacketOut;
import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.action.OFActionOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.floodlightcontroller.core.IOFSwitch;

/**
 * Provides an interface to install rules in flow tables and send packets to 
 * switches for forwarding.
 */
public class FlowInstaller 
{
	private static final short HARD_TIMEOUT = 0;
	private static final short IDLE_TIMEOUT = 20;
	private static final short PRIORITY = 1000;
	
	// Interface to the logging system
	private static Logger log = 
			LoggerFactory.getLogger(PacketHandler.class.getSimpleName());
	
	/**
	 * Creates a flow installer object.
	 */
	public FlowInstaller()
	{ }
	
	/**
	 * Installs a forwarding rule in a switch.
	 * @param sw the switch out which the packet should be forwarded
	 * @param inSwPort the switch port on which the packet should be received
	 * @param outSwPort the switch port out which the packet should be forwarded
	 * @param matchCriteria the match criteria describing the flow
	 * @return true if the rule was sent to the switch, otherwise false
	 */
	public boolean installRule(IOFSwitch sw, short inSwPort, short outSwPort, 
			OFMatch matchCriteria)
	{
		OFFlowMod rule = new OFFlowMod();
		rule.setHardTimeout(HARD_TIMEOUT);
		rule.setIdleTimeout(IDLE_TIMEOUT);
		rule.setPriority(PRIORITY);
		rule.setBufferId(OFPacketOut.BUFFER_ID_NONE);
		
		OFMatch match = matchCriteria.clone();
		match.setInputPort(inSwPort);
		rule.setMatch(match);
		
		List<OFAction> actions = new ArrayList<OFAction>();
		actions.add(new OFActionOutput(outSwPort));
		rule.setActions(actions);
		
		rule.setLength((short)(OFFlowMod.MINIMUM_LENGTH 
				+ OFActionOutput.MINIMUM_LENGTH));
		
		try 
		{
			sw.write(rule, null);
			sw.flush();
			log.debug("Installed rule: "+rule);
		}
		catch (IOException e) 
		{
			log.error("Failed to install rule: "+rule);
			return false;
		}
		
		return true;
	}
	
	/**
	 * Forwards a packet out of a switch.
	 * @param sw the switch out which the packet should be forwarded
	 * @param outSwPort the switch port out which the packet should be forwarded
	 * @param pktInMsg the packet-in message containing the packet to forward 
	 * @return true if the packet was sent to the switch, otherwise false
	 */
	public boolean forwardPacket(IOFSwitch sw, short outSwPort, 
			OFPacketIn pktInMsg) 
    {
		// Create an OFPacketOut for the packet
        OFPacketOut pktOut = new OFPacketOut();        
        
        // Update the input port and buffer ID
        pktOut.setInPort(pktInMsg.getInPort());
        //pktOut.setBufferId(pktInMsg.getBufferId());
        pktOut.setBufferId(OFPacketOut.BUFFER_ID_NONE);
                
        // Set the actions to apply for this packet
        List<OFAction> actions = new ArrayList<OFAction>();
        actions.add(new OFActionOutput(outSwPort));
        pktOut.setActions(actions);
        pktOut.setActionsLength((short)OFActionOutput.MINIMUM_LENGTH);
	        
        // Set data if it is included in the packet in but buffer id is NONE
        if (pktOut.getBufferId() == OFPacketOut.BUFFER_ID_NONE) 
        {
            byte[] packetData = pktInMsg.getPacketData();
            pktOut.setLength((short)(OFPacketOut.MINIMUM_LENGTH
                    + pktOut.getActionsLength() + packetData.length));
            pktOut.setPacketData(packetData);
        }
        else 
        {
        	pktOut.setLength((short)(OFPacketOut.MINIMUM_LENGTH
                    + pktOut.getActionsLength()));
        }
        
        // Send the packet to the switch
        try 
        {
            sw.write(pktOut, null);
            sw.flush();
            log.debug("Forwarded packet");
        }
        catch (IOException e) 
        {
        	log.error("Failed to forward packet");
			return false;
        }
        
        return true;
	}

}
