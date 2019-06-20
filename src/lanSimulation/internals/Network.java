/*   This file is part of lanSimulation.
 *
 *   lanSimulation is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation; either version 2 of the License, or
 *   (at your option) any later version.
 *
 *   lanSimulation is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with lanSimulation; if not, write to the Free Software
 *   Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 *
 *   Copyright original Java version: 2004 Bart Du Bois, Serge Demeyer
 *   Copyright C++ version: 2006 Matthias Rieger, Bart Van Rompaey
 */
package lanSimulation.internals;

import lanSimulation.internals.*;
import java.util.Hashtable;
import java.util.Enumeration;
import java.io.*;

/**
A <em>Network</em> represents the basic data stucture for simulating a Local Area Network (LAN).
The LAN network architecture is a token ring, implying that packahes will be passed from one node to another, until they reached their destination, or until they travelled the whole token ring.
 */
public class Network {
	/**
    Holds a pointer to myself.
    Used to verify whether I am properly initialized.
	 */
	private Network initPtr_;
	/**
    Holds a pointer to some "first" node in the token ring.
    Used to ensure that various printing operations return expected behaviour.
	 */
	public Node firstNode_;
	/**
    Maps the names of workstations on the actual workstations.
    Used to initiate the requests for the network.
	 */
	private Hashtable workstations_;

	/**
Construct a <em>Network</em> suitable for holding #size Workstations.
<p><strong>Postcondition:</strong>(result.isInitialized()) & (! result.consistentNetwork());</p>
	 */
	public Network(int size) {
		assert size > 0;
		initPtr_ = this;
		firstNode_ = null;
		workstations_ = new Hashtable(size, 1.0f);
		assert isInitialized();
		assert ! consistentNetwork();
	}

	/**
Return a <em>Network</em> that may serve as starting point for various experiments.
Currently, the network looks as follows.
    <pre>
    Workstation Filip [Workstation] -> Node -> Workstation Hans [Workstation]
    -> Printer Andy [Printer] -> ... 
    </pre>
<p><strong>Postcondition:</strong>result.isInitialized() & result.consistentNetwork();</p>
	 */
	public static Network DefaultExample () {
		Network network = new Network (2);

		Node wsFilip = new Node (Node.WORKSTATION, "Filip");
		Node n1 = new Node(Node.NODE, "n1");
		Node wsHans = new Node (Node.WORKSTATION, "Hans");
		Node prAndy = new Node (Node.PRINTER, "Andy");

		wsFilip.nextNode_ = n1;
		n1.nextNode_ = wsHans;
		wsHans.nextNode_ = prAndy;
		prAndy.nextNode_ = wsFilip;

		network.workstations_.put(wsFilip.name_, wsFilip);
		network.workstations_.put(wsHans.name_, wsHans);
		network.firstNode_ = wsFilip;

		assert network.isInitialized();
		assert network.consistentNetwork();
		return network;
	}

	/**
Answer whether #receiver is properly initialized.
	 */
	public boolean isInitialized () {
		return (initPtr_ == this);
	};

	/**
Answer whether #receiver contains a workstation with the given name.
<p><strong>Precondition:</strong>this.isInitialized();</p>
	 */
	public boolean hasWorkstation (String ws) {
		//return workstations_.containsKey(ws);
		Node n;

		assert isInitialized();
		n = (Node) workstations_.get(ws);
		if (n == null) {
			return false;
		} else {
			return n.type_ == Node.WORKSTATION;
		}
	};

	/**
Answer whether #receiver is a consistent token ring network.
A consistent token ring network
 - contains at least one workstation and one printer
 - is circular
 - all registered workstations are on the token ring
 - all workstations on the token ring are registered.
<p><strong>Precondition:</strong>this.isInitialized();</p>
	 */
	public boolean consistentNetwork () {
		assert isInitialized();
		Enumeration iter;
		Node currentNode;
		int printersFound = 0, workstationsFound = 0;
		Hashtable encountered = new Hashtable(workstations_.size() * 2, 1.0f);

		if (workstations_.isEmpty()) {return false;};
		if (firstNode_ == null) {return false;};
		//verify whether all registered workstations are indeed workstations
		iter = workstations_.elements();
		while (iter.hasMoreElements()) {
			currentNode = (Node) iter.nextElement();
			if (currentNode.type_ != Node.WORKSTATION) {return false;};
		};
		//enumerate the token ring, verifying whether all workstations are registered
		//also count the number of printers and see whether the ring is circular
		currentNode = firstNode_;
		while (! encountered.containsKey(currentNode.name_)) {
			encountered.put(currentNode.name_, currentNode);
			if (currentNode.type_ == Node.WORKSTATION) {workstationsFound++;};
			if (currentNode.type_ == Node.PRINTER) {printersFound++;};
			currentNode = currentNode.nextNode_;
		};
		if (currentNode != firstNode_) {return false;};//not circular
		if (printersFound == 0) {return false;};//does not contain a printer
		if (workstationsFound != workstations_.size()) {return false;}; //not all workstations are registered
		//all verifications succeedeed
		return true;}

	/**
The #receiver is requested to broadcast a message to all nodes.
Therefore #receiver sends a special broadcast packet across the token ring network,
which should be treated by all nodes.
<p><strong>Precondition:</strong> consistentNetwork();</p>
@param report Stream that will hold a report about what happened when handling the request.
@return Anwer #true when the broadcast operation was succesful and #false otherwise
	 */
	public boolean requestBroadcast(Writer report) {
		assert consistentNetwork();

		try {
			report.write("Broadcast Request\n");
		} catch (IOException exc) {
			// just ignore
		};

		Node currentNode = firstNode_;
		Packet packet = new Packet("BROADCAST", firstNode_.name_, firstNode_.name_);
		currentNode = send(report, currentNode, packet);

		try {
			report.write(">>> Broadcast travelled whole token ring.\n\n");
		} catch (IOException exc) {
			// just ignore
		};
		return true;
	}

	private Node send(Writer report, Node currentNode, Packet packet) {
		do {
			try {
				report.write("\tNode '");
				report.write(currentNode.name_);
				report.write("' accepts broadcase packet.\n");
				currentNode.logging(report, this);
				report.flush();
			} catch (IOException exc) {
				// just ignore
			};
			currentNode = currentNode.nextNode_;
		} while (atDestination(currentNode, packet));
		return currentNode;
	}

	private boolean atDestination(Node currentNode, Packet packet) {
		return ! packet.destination_.equals(currentNode.name_);
	}

	/**
The #receiver is requested by #workstation to print #document on #printer.
Therefore #receiver sends a packet across the token ring network, until either
(1) #printer is reached or (2) the packet travelled complete token ring.
<p><strong>Precondition:</strong> consistentNetwork() & hasWorkstation(workstation);</p>
@param workstation Name of the workstation requesting the service.
@param document Contents that should be printed on the printer.
@param printer Name of the printer that should receive the document.
@param report Stream that will hold a report about what happened when handling the request.
@return Anwer #true when the print operation was succesful and #false otherwise
	 */
	public boolean requestWorkstationPrintsDocument(String workstation, String document,
			String printer, Writer report) {
		assert consistentNetwork() & hasWorkstation(workstation);

		try {
			report.write("'");
			report.write(workstation);
			report.write("' requests printing of '");
			report.write(document);
			report.write("' on '");
			report.write(printer);
			report.write("' ...\n");
		} catch (IOException exc) {
			// just ignore
		};

		boolean result = false;
		Node startNode, currentNode;
		Packet packet = new Packet(document, workstation, printer);

		startNode = (Node) workstations_.get(workstation);

		try {
			startNode.logging(report, this);
			report.flush();
		} catch (IOException exc) {
			// just ignore
		};
		currentNode = startNode.nextNode_;
		while (atDestination(currentNode, packet)
				& (! packet.origin_.equals(currentNode.name_))) {
			try {
				currentNode.logging(report, this);
				report.flush();
			} catch (IOException exc) {
				// just ignore
			};
			currentNode = currentNode.nextNode_;
		};

		if (packet.destination_.equals(currentNode.name_)) {
			result = currentNode.printDocument(this, packet, report);
		} else {
			try {
				report.write(">>> Destinition not found, print job cancelled.\n\n");
				report.flush();
			} catch (IOException exc) {
				// just ignore
			};
			result = false;
		}

		return result;
	}

	public void accounting(Writer report, String author, String title) throws IOException {
		report.write("\tAccounting -- author = '");
		report.write(author);
		report.write("' -- title = '");
		report.write(title);
		report.write("'\n");
		report.write(">>> Postscript job delivered.\n\n");
		report.flush();
	}

	/**
Return a printable representation of #receiver.
 <p><strong>Precondition:</strong> isInitialized();</p>
	 */
	public String toString () {
		assert isInitialized();
		StringBuffer buf = new StringBuffer(30 * workstations_.size());
		printOn(buf);
		return buf.toString();
	}

	/**
Write a printable representation of #receiver on the given #buf.
<p><strong>Precondition:</strong> isInitialized();</p>
	 */
	public void printOn (StringBuffer buf) {
		assert isInitialized();
		Node currentNode = firstNode_;
		do {
			switch (currentNode.type_) {
			case Node.NODE:
				buf.append("Node ");
				buf.append(currentNode.name_);
				buf.append(" [Node]");
				break;
			case Node.WORKSTATION:
				buf.append("Workstation ");
				buf.append(currentNode.name_);
				buf.append(" [Workstation]");
				break;
			case Node.PRINTER:
				buf.append("Printer ");
				buf.append(currentNode.name_);
				buf.append(" [Printer]");
				break;
			default:
				buf.append("(Unexpected)");;
				break;
			};
			buf.append(" -> ");
			currentNode = currentNode.nextNode_;
		} while (currentNode != firstNode_);
		buf.append(" ... ");
	}

}
