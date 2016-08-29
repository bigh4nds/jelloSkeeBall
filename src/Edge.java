/**
 * The Edge class to store a the vertices that define an edge and the
 * other 2 vertices in the adjacent faces. It also stores the base length
 * of the spring and the spring constant.s
 *
 */
public class Edge {
	
	private int v0, v1;
	private float equilibrium_length, spring_constant;
	
	
	public Edge(int v0, int v1) {
		this.v0 = v0;
		this.v1 = v1;
	}
	
	/**
	 * Getters
	 */
	public int getV0() {
		return v0;
	}
	public int getV1() {
		return v1;
	}
	public float getEquilibriumLength() {
		return equilibrium_length;
	}
	public float getSpringConstant() {
		return spring_constant;
	}
	
	/**
	 * Setters
	 */
	public void setV0(int v0) {
		this.v0 = v0;
	}
	public void setV1(int v1) {
		this.v1 = v1;
	}
	public void setEquilibriumLength(float equilibrium_length) {
		this.equilibrium_length = equilibrium_length;
	}
	public void setSpringConstant(float spring_constant) {
		this.spring_constant = spring_constant;
	}
}