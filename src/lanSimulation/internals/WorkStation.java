package lanSimulation.internals;

public class WorkStation extends Node{

	public WorkStation(byte type, String name, Node nextNode) {
		super(type, name, nextNode);
		// TODO Auto-generated constructor stub
	}

	public WorkStation(byte workstation, String string) {
		// TODO Auto-generated constructor stub
		super(workstation,string);
	}
	protected void printNodeXML(StringBuffer buf) {
		buf.append("<workstation>");
		buf.append(this.name_);
		buf.append("</workstation>");
	}

	protected void printNode(StringBuffer buf) {
		buf.append("Workstation ");
		buf.append(this.name_);
		buf.append(" [Workstation]");
	}
}
