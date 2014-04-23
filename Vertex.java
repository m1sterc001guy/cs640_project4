/* Copyright (c) 2013 the authors listed at the following URL, and/or
the authors of referenced articles or incorporated external code:
http://en.literateprograms.org/Dijkstra's_algorithm_(Java)?action=history&offset=20081113161332

Permission is hereby granted, free of charge, to any person obtaining
a copy of this software and associated documentation files (the
"Software"), to deal in the Software without restriction, including
without limitation the rights to use, copy, modify, merge, publish,
distribute, sublicense, and/or sell copies of the Software, and to
permit persons to whom the Software is furnished to do so, subject to
the following conditions:

The above copyright notice and this permission notice shall be
included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

Retrieved from: http://en.literateprograms.org/Dijkstra's_algorithm_(Java)?oldid=15444

Modified by: Aaron Gember, agember@cs.wisc.edu, University of Wisconsin-Madison
*/

package edu.wisc.cs.sdn;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import net.floodlightcontroller.core.IOFSwitch;

/**
 * A vertex in a graph, representing a switch in a network topology.
 */
class Vertex implements Comparable<Vertex>
{
    private final IOFSwitch sw;
    private Map<Vertex,Edge> adjacencies = new HashMap<Vertex,Edge>();
    public double minDistance = Double.POSITIVE_INFINITY;
    public Vertex previous;
    
    /**
     * Create a new vertex.
     * @param sw the switch this vertex represents
     */
    public Vertex(IOFSwitch sw)
    { this.sw = sw; }
    
    /**
     * Get the switch this vertex represents.
     * @return the switch this vertex represents
     */
    public IOFSwitch getSwitch()
    { return this.sw; }
    
    @Override
    public String toString()
    { return ""+sw.getId(); }
    
    @Override
    public int compareTo(Vertex other)
    { return Double.compare(minDistance, other.minDistance); }
    
    /**
     * Add an edge to a neighboring switch, representing a link to that switch.
     * @param dstSwitch the vertex representing the neighboring switch
     * @param srcSwitchPort the port on this vertex (i.e., switch) that is 
     * 						one end of the link
     * @param dstSwitchPort the port on the neighbor vformsertex (i.e., switch) that
     * 						is the other end of the link
     * @return the edge that was added to the topology
     */
    public Edge addNeighbor(Vertex dstSwitch, short srcSwitchPort, short dstSwitchPort)
    {
    	Edge edge = new Edge(this, srcSwitchPort, dstSwitch, dstSwitchPort);
    	adjacencies.put(dstSwitch, edge); 
    	return edge;
    }
    
    /**
     * Get the edge (i.e., a link) to a neighboring switch.
     * @param neighbor the vertex representing the neighboring switch
     * @return the edge (i.e., a link) to the specified neighboring switch;
     * 	       null if none exists
     */
    public Edge getEdgeToNeighbor(Vertex neighbor)
    { return adjacencies.get(neighbor); }
    
    /**
     * Get a list of all edges (i.e., links) going out from this vertex.
     * @return a list of all edges (i.e., links) going out form this vertex.
     */
    public Collection<Edge> getAdjacencies()
    { return adjacencies.values(); }
}
