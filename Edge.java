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

/**
 * An edge in a graph, representing a link in a network topology.
 */
class Edge
{
    private final Vertex srcVert;
    private final Vertex dstVert;
    private final short srcSwPort;
    private final short dstSwPort;
    
    /**
     * Create a new edge, representing a link.
     * @param srcVert the vertex (i.e., switch) on one side of the link
     * @param srcSwPort the switch port on one side of the link
     * @param dstVert the vertex (i.e., switch) on the other side of the link
     * @param dstSwPort the siwtch port on the other side of the link
     */
    public Edge(Vertex srcVert, short srcSwPort, Vertex dstVert, 
    		short dstSwPort) 
    { 
    	this.srcVert = srcVert;
    	this.dstVert = dstVert;
    	this.srcSwPort = srcSwPort;
    	this.dstSwPort = dstSwPort;
    }
    
    /**
     * Get the vertex that can be reached via the edge (i.e., link).
     * @return the vertex that can be reached via the edge (i.e., link) 
     */
    public Vertex getDstVertex()
    { return dstVert; }
    
    /**
     * Get the vertex from which the edge (i.e., link) originates.
     * @return the vertex from which the edge (i.e., link) originates
     */
    public Vertex getSrcVertex()
    { return srcVert; }
    
    /**
     * Get the weight assigned to the edge  (i.e., link).
     * @return 1.0
     */
    public double getWeight()
    { return 1.0; }
    
    /**
     * Get the switch port from which the edge (i.e., link) originates.
     * @return the switch port from which the edge (i.e., link) originates
     */
    public short getSrcSwitchPort()
    { return this.srcSwPort; }
    
    /**
     * Get the switch port that can be reached via the edge (i.e., link).
     * @return the switch port that can be reached via the edge (i.e., link)
     */
    public short getDstSwitchPort()
    { return this.dstSwPort; }
}
