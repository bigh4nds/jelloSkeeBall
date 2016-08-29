import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

public class Plane {
	private Point3f point;
	private Vector3f normal;
	private float spring_constant;
	
	public Plane() {}
	
	public Plane(Point3f point, Vector3f normal, float spring_constant) {
		this.point = point;
		this.normal = normal;
		this.normal.normalize();
		this.spring_constant = spring_constant;
	}
	
	public Plane(Point3f p0, Point3f p1, Point3f p2) {
		Vector3f normal = new Vector3f();
		Vector3f v1 = new Vector3f();
		Vector3f v2 = new Vector3f();
		v1.sub(p1, p0);
		v2.sub(p2, p0);
		normal.cross(v1, v2);
		normal.normalize();	
		
		this.point = new Point3f(p0);
		this.normal = normal;
	}
	
	/**
	 * Gets the distance to the plane from the given point.
	 * @param position
	 * @return The distance to the plane. This value may be negative.
	 */
	public float getDistanceToPlane(Point3f position) {
		Vector3f temp = new Vector3f(position);
		temp.sub(point);
		float distance_to_plane = temp.dot(normal);
		
		return distance_to_plane;
	}
	
	/**
	 * Gets the vector to the plane from the given point.
	 * @param position
	 * @return The vector that if added to the given point will cause
	 * the point to lie on the plane.
	 */
	public Vector3f getVectorToPlane(Point3f position) {
		float distance_to_plane = getDistanceToPlane(position);
		Vector3f vector_to_plane = new Vector3f(normal);
		vector_to_plane.scale(distance_to_plane);
		vector_to_plane.negate();
		
		return vector_to_plane;
	}
	
	/**
	 * Getters
	 */
	public Point3f getPoint() {
		return point;
	}
	public Vector3f getNormal() {
		return normal;
	}
	public float getSpringConstant() {
		return spring_constant;
	}
	
	/**
	 * Setters
	 */
	public void setPoint(Point3f point) {
		this.point = point;
	}
	public void setNormal(Vector3f normal) {
		this.normal = normal;
	}
	public void setSpringConstant(float spring_constant) {
		this.spring_constant = spring_constant;
	}
	public void setPlane(Point3f p0, Point3f p1, Point3f p2) {
		Vector3f normal = new Vector3f();
		Vector3f v1 = new Vector3f();
		Vector3f v2 = new Vector3f();
		v1.sub(p1, p0);
		v2.sub(p2, p0);
		normal.cross(v1, v2);
		normal.normalize();	

		this.setPoint(p0);
		this.setNormal(normal);
	}
}