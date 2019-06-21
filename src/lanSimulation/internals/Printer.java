package lanSimulation.internals;

public class Printer extends Node{

	public Printer(byte type, String name) {
		super(type, name);
		// TODO Auto-generated constructor stub
	}

	
	protected void printNodeXML(StringBuffer buf) {
		buf.append("<printer>");
		buf.append(this.name_);
		buf.append("</printer>");
	}

	protected void printNode(StringBuffer buf) {
		buf.append("Printer ");
		buf.append(this.name_);
		buf.append(" [Printer]");
	}
}
