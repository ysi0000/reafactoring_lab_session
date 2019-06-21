package lanSimulation.internals;

public class WorkStation extends Node{

	public WorkStation(String name, Node nextNode) {
		super(name, nextNode);
		// TODO Auto-generated constructor stub
	}

	public WorkStation( String string) {
		// TODO Auto-generated constructor stub
		super(string);
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
