import java.util.ArrayList;

import javax.vecmath.Vector3f;


/**
 * Used to store an objects Vertices and faces.
 * @author Thomas Ryabin
 *
 */
public class Object {

	private ArrayList<Vertex> object_verts = new ArrayList<Vertex>();
	private ArrayList<Integer> object_faces = new ArrayList<Integer>();
	private ArrayList<Vector3f> object_normals = new ArrayList<Vector3f>();
	
	public Object(Object object) {
		for (Vertex v : object.getVerts()) {
			object_verts.add(v.clone());
		}
		for (int face_index : object.getFaces()) {
			object_faces.add(face_index);
		}
		for (Vector3f normal : object.getNormals()) {
			object_normals.add(new Vector3f(normal));
		}
	}
	
	public Object(ArrayList<Vertex> input_verts, ArrayList<Integer> input_faces, ArrayList<Vector3f> input_normals) {
		for (Vertex v : input_verts) {
			object_verts.add(v.clone());
		}
		for (int face_index : input_faces) {
			object_faces.add(face_index);
		}
		for (Vector3f normal : input_normals) {
			object_normals.add(new Vector3f(normal));
		}
		
//		System.out.println("object_verts.size() = " + object_verts.size());
		
	}
	
	public ArrayList<Vertex> getVerts() {
		return object_verts;
	}
	public ArrayList<Integer> getFaces() {
		return object_faces;
	}
	public ArrayList<Vector3f> getNormals() {
		return object_normals;
	}
}
