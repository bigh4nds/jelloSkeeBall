import java.util.ArrayList;

import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

/**
 * The Vertex class stores information about a vertex.
 */
public class Vertex {
	
	private Point3f position;
	private Point3f prev_position;
	private float mass;
	private Vector3f velocity;
	private boolean is_colliding;
	private Plane collision_plane;
	private ArrayList<Integer> collision_face_indices = new ArrayList<Integer>();
	
	public Vertex(Point3f position) {
		this.position = position;
	}
	
	public Vertex(Point3f position, float mass, Vector3f velocity) {
		this.position = position;
		this.prev_position = position;
		this.mass = mass;
		this.velocity = velocity;
		is_colliding = false;
	}
	
	public Vertex clone() {
		Point3f new_pos = new Point3f(this.getPosition());
		Vector3f new_vel = new Vector3f(this.getVelocity());

		Vertex v2 = new Vertex(new_pos, this.getMass(), new_vel);
		return v2;
	}
	
	/**
	 * Getters
	 */
	public Point3f getPosition() {
		return position;
	}
	public Point3f getPrevPosition() {
		return prev_position;
	}
	public float getMass() {
		return mass;
	}
	public Vector3f getVelocity() {
		return velocity;
	}
	public boolean isColliding() {
		return is_colliding;
	}
	public Plane getCollisionPlane() {
		return collision_plane;
	}
	public ArrayList<Integer> getCollisionFaceIndices() {
		return collision_face_indices;
	}
	
	/**
	 * Setters
	 */
	public void setPosition(Point3f position) {
		this.position = position;
	}
	public void setPrevPosition(Point3f prev_position) {
		this.prev_position = prev_position;
	}
	public void setMass(float mass) {
		this.mass = mass;
	}
	public void setVelocity(Vector3f velocity) {
		this.velocity = velocity;
	}
	public void setIsColliding(boolean is_colliding) {
		this.is_colliding = is_colliding;
	}
	public void setCollisionPlane(Plane collision_plane) {
		this.collision_plane = collision_plane;
	}
	public void setCollisionFaceIndices(ArrayList<Integer> collision_face_indices) {
		this.collision_face_indices = collision_face_indices;
	}
}