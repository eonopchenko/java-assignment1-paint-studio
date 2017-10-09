package nz.ac.unitec.client;

public class Tool extends Object {
	
	public static final Tool PEN = new Tool(0);
	public static final Tool LINE = new Tool(1);
	public static final Tool CIRCLE = new Tool(2);
	public static final Tool RECTANGLE = new Tool(3);
	
	public static Integer valueOf(String type) {
		if(type.equals("pen")) {
			return 0;
		} else if(type.equals("line")) {
			return 1;
		} else if(type.equals("circle")) {
			return 2;
		} else if(type.equals("rectangle")) {
			return 3;
		}
		
		return -1;
	}
	
	private int Type;
	
	Tool(int type) {
		Type = type;
	};
	
	@Override
    public boolean equals(Object obj) {
		return this.Type == ((Tool)obj).Type;
    }
	
	@Override
	public String toString() {
		if(Type == 0) {
			return "pen";
		} else if(Type == 1) {
			return "line";
		} else if(Type == 2) {
			return "circle";
		} else if(Type == 3) {
			return "rectangle";
		}
		
		return "";
	}
}
