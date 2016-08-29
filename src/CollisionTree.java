import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;



public class CollisionTree { 
	private Vector3f min, max;
	private ArrayList<Integer> face_indices = new ArrayList<Integer>();
    private ArrayList<CollisionTree> children = new ArrayList<CollisionTree>();
    private int num_divisions = 2;
    
    public CollisionTree() {}
    
    public CollisionTree(ArrayList<Vertex> verts, ArrayList<Integer> faces) { 
    	
    	System.out.println("CollisionTree()");
    	System.out.println("verts.size() = " + verts.size());
    	System.out.println("faces.size() = " + faces.size());
    	
    	computeBoundingBox(verts);
    	computeChildren(verts, faces);
    }
    
    /* find the bounding box for the vertices */
	private void computeBoundingBox(ArrayList<Vertex> verts) {
		float xmin, ymin, zmin, xmax, ymax, zmax;
		xmax = xmin = verts.get(0).getPosition().x;
		ymax = ymin = verts.get(0).getPosition().y;
		zmax = zmin = verts.get(0).getPosition().z;
		
		for (int i = 1; i < verts.size(); i ++) {
			Point3f position = verts.get(i).getPosition();
			xmax = Math.max(xmax, position.x);
			xmin = Math.min(xmin, position.x);
			ymax = Math.max(ymax, position.y);
			ymin = Math.min(ymin, position.y);
			zmax = Math.max(zmax, position.z);
			zmin = Math.min(zmin, position.z);			
		}
		
		min = new Vector3f(xmin, ymin, zmin);
		max = new Vector3f(xmax, ymax, zmax);
	}
	
	/**
	 * Divide up the bounding box of the node into 8 cubes and compute the children of the node. 
	 * Any faces that don[t fit into a child get assigned to the node.
	 * @param verts
	 * @param faces
	 */
	private void computeChildren(ArrayList<Vertex> verts, ArrayList<Integer> faces) {
		
		System.out.println("computeChildren()");
		
		
//    	If there is only a single face inside the current bounding box then don't add
//    	any children and just set that face for the node.
		if (faces.size()/3 == 1) {
			face_indices.add(faces.get(0));
			return;
		}
		
//    	The width of the 8 bounding boxes inside the current one in 3 dimensions.
		float x_increment = (max.x - min.x)/num_divisions;
		float y_increment = (max.y - min.y)/num_divisions;
		float z_increment = (max.z - min.z)/num_divisions;
		
//    	Used to map the faces to the sub-bounding box they were found in.
		HashMap<Integer, BoundingBox> valid_boxes = new HashMap<Integer, BoundingBox>();
		
//    	Loop through all the faces and see which lie in the sub bounding boxes.
		for(int i = 0; i < faces.size(); i += 3) {
			
			int v0_index = faces.get(i);
			int v1_index = faces.get(i+1);
			int v2_index = faces.get(i+2);
			
			Point3f v0 = verts.get(v0_index).getPosition();
			Point3f v1 = verts.get(v1_index).getPosition();
			Point3f v2 = verts.get(v2_index).getPosition();
			
//			Used to determine if the face doesn't fit into any sub boxes.
			boolean face_assigned_to_sub_box = false;
			
			for (int j = 0; j < num_divisions; j++) {
				for (int k = 0; k < num_divisions; k++) {
					for (int m = 0; m < num_divisions; m++) {
				 
//    					Determine the 2 vectors that define the sub bounding box.
						Vector3f cur_min = new Vector3f(min.x + x_increment*j, min.y + y_increment*k, min.z + z_increment*m);
						Vector3f cur_max = new Vector3f(min.x + x_increment*(j+1), min.y + y_increment*(k+1), min.z + z_increment*(m+1));
						
//    					Determine if the face lies inside the sub bounding box.
						if (isInsideBoundingBox(v0, cur_min, cur_max) &&
							isInsideBoundingBox(v1, cur_min, cur_max) &&
							isInsideBoundingBox(v2, cur_min, cur_max)) {
								
								face_assigned_to_sub_box = true;
//    							Compute the key for the sub bounding box.
								int key = (int) (m*Math.pow(num_divisions, 2) + k*num_divisions + j);
								
//    							Initialize the key entry in the HashMap if it does not exist.
								if (!valid_boxes.containsKey(key)) {
									BoundingBox new_valid_box = new BoundingBox();
									new_valid_box.faces = new ArrayList<Integer>();
									new_valid_box.min = new Vector3f(cur_min);
									new_valid_box.max = new Vector3f(cur_max);
									valid_boxes.put(key, new_valid_box);
								}
								
//    							Add the face as a valid face for the sub bounding box.
								ArrayList<Integer> box_faces = valid_boxes.get(key).faces;
								box_faces.add(v0_index);
								box_faces.add(v1_index);
								box_faces.add(v2_index);
						}
					}
				}
			}
			
//			If the face did not fit into any sub boxes, add it to the list for the
//			current node.
			if (!face_assigned_to_sub_box) {
				face_indices.add(i);
			}
		}
		

//		Recursively create the children for the current node.
//		children = new ArrayList<CollisionTree>();
		Set<Integer> valid_children = valid_boxes.keySet();
		for (int key : valid_children) {
			BoundingBox box = valid_boxes.get(key);
			CollisionTree child = new CollisionTree();
			child.min = new Vector3f(box.min);
			child.max = new Vector3f(box.max);
			
			child.computeChildren(verts, box.faces);
			children.add(child);
		}	
	}
	
	/**
	 * Determines if the given vertex is inside the bounding box specified
	 * by the given vectors.
	 * @param vert
	 * @param min
	 * @param max
	 * @return
	 */
	private boolean isInsideBoundingBox(Point3f vert, Vector3f min, Vector3f max) {
		if (vert.x >= min.x && vert.x <= max.x &&
			vert.y >= min.y && vert.y <= max.y &&
			vert.z >= min.z && vert.z <= max.z) {
			
			return true;
		}
		else {
			return false;
		}
	}
	
	/**
	 * Detects if there was a collision between the ray defined by the given points and a face in the collision tree.
	 * @param start
	 * @param end
	 * @param collision_plane
	 * @return An ArrayList containing the starting indices of all colliding faces.
	 */
	public ArrayList<Integer> detectCollision(ArrayList<Vertex> verts, ArrayList<Integer> faces, Point3f start, Point3f end, float cushion_amount) {
		
//		Compute the direction of the start to the end.
		Vector3f direction = new Vector3f(end);
		direction.sub(start);
		direction.normalize();
		
//		Compute tmin and tmax.
		float tmin = 0;
		float tmax_x = Math.abs(end.x - start.x);
		float tmax_y = Math.abs(end.y - start.y);
		float tmax_z = Math.abs(end.z - start.z);
		
		float tmax = tmax_x > tmax_y ? tmax_x : tmax_y;
		tmax = tmax_z > tmax ? tmax_z : tmax;
		
		ArrayList<Integer> collision_face_indices = new ArrayList<Integer>();
		
//		System.out.println("face_indices.size() = " + face_indices.size());
		
//		Check for a collision against the faces in the node.
		for (int face_index : face_indices) {
			
			Point3f p0 = verts.get(faces.get(face_index)).getPosition();
			Point3f p1 = verts.get(faces.get(face_index + 1)).getPosition();
			Point3f p2 = verts.get(faces.get(face_index + 2)).getPosition();
			
//			Move the origin slightly closer to the plane.
			Plane temp_collision_plane = new Plane(p0, p1, p2);
			Vector3f vector_to_move_closer_to_plane = new Vector3f(temp_collision_plane.getNormal());
			vector_to_move_closer_to_plane.negate();
			vector_to_move_closer_to_plane.scale(cushion_amount);
			
			Vector3f origin = new Vector3f(end);
			origin.add(vector_to_move_closer_to_plane);
			
			float dist_to_collision_plane = temp_collision_plane.getDistanceToPlane(end);
			
			boolean prev_pos_hit = hit(new Vector3f(start), temp_collision_plane.getNormal(), p0, p1, p2, -9999f, 9999f);
			boolean cur_pos_hit =  hit(new Vector3f(end), temp_collision_plane.getNormal(), p0, p1, p2, -9999f, 9999f);
			
//			Check for a collision. If there is a collision, set the 'collision_plane' and 'collision_face_index' variables.
			/*
			if (hit(origin, direction, p0, p1, p2, tmin, tmax)) {
					collision_plane.setPlane(p0, p1, p2);
					collision_face_index = face_index;
					
					return collision_face_index;
			}
			*/
			
			if (hit(origin, direction, p0, p1, p2, tmin, tmax) ||
				(cur_pos_hit && dist_to_collision_plane < cushion_amount && dist_to_collision_plane > 0)) {
				collision_face_indices.add(face_index);
			}
			
		}

		
//		Check to see if the points lie inside the bounding boxes of one of the children, and recursively 
//		detect a collision in the child.
		for (CollisionTree child : children) {
			
			Vector3f min = child.min;
			Vector3f max = child.max;
			
	//		Check to see if the starting or the ending points are inside the bounding Box for the child.
			if (isInsideBoundingBox(start, min, max) ||
				isInsideBoundingBox(end, min, max)) {
				ArrayList<Integer> cur_collision_face_indices = child.detectCollision(verts, faces, start, end, cushion_amount);
				
				for (int collision_face_index : cur_collision_face_indices) {
					collision_face_indices.add(collision_face_index);
				}
			}
			
		}
		
		return collision_face_indices;
	}
	
	
	/**
	 * Returns whether or not the given ray intersects the given triangle.
	 * @param origin
	 * @param direction
	 * @param p0
	 * @param p1
	 * @param p2
	 * @param tmin
	 * @param tmax
	 * @return
	 */
	public static boolean hit(Vector3f origin, Vector3f direction, Point3f p0, Point3f p1, Point3f p2, float tmin, float tmax) {
		
//		Initialize variables so it is easy to read the code.
		Vector3f D = direction;
		Vector3f O = origin;
		
//		Setup the vectors in the matrix.
		Vector3f v1 = D;
		Vector3f v2 = new Vector3f(p2);
		v2.sub(p0);
		Vector3f v3 = new Vector3f(p2);
		v3.sub(p1);
		Vector3f b = new Vector3f(p2);
		b.sub(O);
		
//		Compute the determinant of the 3x3 matrix on the left.
		Vector3f temp = new Vector3f();
		temp.cross(v2,  v3);
		float denom_det = v1.dot(temp);
		
//		Return no intersection if the determinant is zero.
		if (denom_det == 0f)
			return false;
		
//		Compute the determinant when b replaces v1.
		float t_det = b.dot(temp);
		
//		Compute the determinant when b replaces v2.
		temp = new Vector3f();
		temp.cross(b, v3);
		float a_det = v1.dot(temp);
		
//		Compute the determinant when b replaces v3.		
		temp = new Vector3f();
		temp.cross(v2, b);
		float b_det = v1.dot(temp);
		
//		Compute t, alpha and beta.
		float t = t_det / denom_det;
		float alpha = a_det / denom_det;
		float beta = b_det / denom_det;
		
		
		
//		Check the constraints.
		if (!(alpha >= 0 && beta >= 0 && alpha + beta <= 1)) {
//			System.out.println("doesn't hit the triangle");
			return false;
		}

		
//		Return no intersection if t is outside the given range.
		if (t < tmin || t > tmax + .01f) {
			Vector3f point_t = new Vector3f();
			point_t.scaleAdd(t, direction, origin);
//			System.out.println("point_t.y = " + point_t.y);
			
			Vector3f point_tmax = new Vector3f();
			point_tmax.scaleAdd(t, direction, origin);
//			System.out.println("point_tmax.y = " + point_tmax.y);

			return false;
		}
		
		return true;
	}
	
	private class BoundingBox {
		ArrayList<Integer> faces;
		private Vector3f min, max;
	}
} 

