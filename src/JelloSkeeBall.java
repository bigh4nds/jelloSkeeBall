import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Rectangle2D;
import java.io.*;
import javax.media.opengl.*;
import javax.media.opengl.glu.GLU;
import javax.swing.JFrame;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

import com.sun.opengl.util.FPSAnimator;
import com.sun.opengl.util.j2d.TextRenderer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

class JelloSkeeBall extends JFrame implements GLEventListener, KeyListener, MouseListener, MouseMotionListener, ActionListener {


	public enum GameState {
		 MAIN_MENU, PLAYING_BEFORE_TOSS, PLAYING_AFTER_TOSS, GAME_OVER;
	}
	
	/* 'object_verts' and 'object_faces' store the jello object mesh that will be displayed on the screen
	 * the elements stored in these arrays are the same with the input mesh
	 * 'curr_normals' stores the normal of each face and is necessary for shading calculation
	 * you don't have to compute the normals yourself: once you have curr_verts and curr_faces
	 * ready, you just need to call estimateFaceNormal to generate normal data
	 */
	
//	The game state.
	private static GameState game_state;
	
//	The data structures that hold the jello objects and collision environment.
	private static ArrayList<Object> jello_objects = new ArrayList<Object>();
	private static Object cur_jello_object;
	private static Object collision_environment;
	private static Object goal_plane; // Placed behind the goals and is used to determine which goal was scored in.
	private static HashMap<Integer, Edge> edge_adjacency; // Stores the springs in each edge of the jello.
	
//	Used to scale jello objects to a similar size.
	private static Vector3f jello_object_max_dimensions = new Vector3f(.2f, .2f, .2f);
	
//	The point around where the jello objects are spawned.
	private static Point3f startPoint = new Point3f(0f, 1f, -4f);
	
//	The acceleration structure for collisions with the collision environment.
	private static CollisionTree collision_tree;
	private static CollisionTree goal_plane_collision_tree;
	
	private static ArrayList<Point3f> goal_point_locations = new ArrayList<Point3f>(); // Used to determine which goal the jello went into.
	private static ArrayList<Integer> goal_points = new ArrayList<Integer>(); // Used to determine the amount of points awarded per goal.
	private static ArrayList<Integer> jello_object_multipliers = new ArrayList<Integer>(); //Used to determine the point multiplier for jello objects.		
	private static int cur_jello_object_index; // Used to determine which multiplier to use.
	private static int score = 0;
	private static float time_left_in_round_seconds;
	private static float time_left_in_toss_seconds;
	private static float time_per_round_seconds = 61;
	private static float max_time_per_toss_seconds = 3;
	private static float downward_velocity;
	private static float downward_velocity_scale_factor = .01f;
	private static int last_points_scored;
	
	/* GL related variables */
	private final GLCanvas canvas;
	private static GL gl;
	private final GLU glu = new GLU();
	private FPSAnimator animator;
	private float fov = 60.0f;
	
	// initial window size
	private static int winW = 800, winH = 600;
	
	// toggle parameters
	private static boolean wireframe = false;
	private static boolean cullface = true;
	
	// transformation and mouse control parameters
	private static Point3f camera_eye = new Point3f(0, 1, -5);
	private static Point3f camera_ref_point = new Point3f(0, 1, 1);
	private static float xpos = 0, ypos = 0, zpos = 0;
	private static float xmin, ymin, zmin;
	private static float xmax, ymax, zmax;
	private static float centerx, centery, centerz;
	private static float roth = 0, rotv = 0;
	private static float znear, zfar;
	private static int mouseX, mouseY, mouseButton;
	private static int mouseX2, mouseY2; //Used to to instill velocity on the jello with the mouse.
	private static boolean mouse_dragged = false;
	private static float mouseVelocityScale = 2f;
	private static float mousePositionScale = .003f;
	private static int fps = 60;
	
	// Physics parameters
	private static float vertex_mass = .005f;
	private static float object_spring_constant = 150;
	private static float collision_spring_constant = 500;
	private static Vector3f gravity = new Vector3f(0, -1f, 0);
	private static int num_physics_iterations_per_frame = 5;
	private static float physics_time_interval = .001f;
	private static int support_spring_modulus = 1; // Every nth vertice will have a support spring.
	                                               // A value of one indicates there will be a spring
//	                                                  between every vertice. Performance and stability will go down
//	                                                  if there are too many springs.
	private static float cushion_amount = .2f;
	
	
	/* load a simple .obj mesh from disk file
	 * note that each face *must* be a triangle and cannot be a quad or other types of polygon
	 * 'input_verts' and 'input_faces' store the vertex and face data of the input mesh respectively
	 * each element in 'input_verts' is a 3D points defining the vertex
	 * every three integers in 'input_faces' define the indexes of the three vertices that make a triangle
	 * there are in total input_faces.size()/3 triangles
	 */ 
	private static void loadMesh(String filename, ArrayList<Point3f> input_verts, ArrayList<Integer> input_faces) {
		BufferedReader in = null;
		try {
			in = new BufferedReader(new FileReader(filename));
		} catch (IOException e) {
			System.out.println("Error reading from file " + filename);
			System.exit(0);
		}

		float x, y, z;
		int v1, v2, v3;
		String line;
		String[] tokens;
		try {
		while ((line = in.readLine()) != null) {
			if (line.length() == 0)
				continue;
			switch(line.charAt(0)) {
			case 'v':
				System.out.println(line);
				tokens = line.split("[ ]+");
				x = Float.valueOf(tokens[1]);
				y = Float.valueOf(tokens[2]);
				z = Float.valueOf(tokens[3]);
				input_verts.add(new Point3f(x, y, z));
				break;
			case 'f':
				tokens = line.split("[ ]+");
				/* when defining faces, .obj assumes the vertex index starts from 1
				 * so we should subtract 1 from each index 
				 */
				v1 = Integer.valueOf(tokens[1])-1;
				v2 = Integer.valueOf(tokens[2])-1;
				v3 = Integer.valueOf(tokens[3])-1;
				input_faces.add(v1);
				input_faces.add(v2);
				input_faces.add(v3);				
				break;
			default:
				continue;
			}
		}
		in.close();	
		} catch(IOException e) {
			// error reading file
		}

		System.out.println("Read " + input_verts.size() +
					   	" vertices and " + input_faces.size()/3 + " faces.");

	}
	

	
	/* creates OpenGL window */
	public JelloSkeeBall() {
		super("3D Jello Skee Ball");
		
		game_state = GameState.MAIN_MENU;
		
		canvas = new GLCanvas();
		canvas.addGLEventListener(this);
		canvas.addKeyListener(this);
		canvas.addMouseListener(this);
		canvas.addMouseMotionListener(this);
		animator = new FPSAnimator(canvas, fps);
		getContentPane().add(canvas);
		setExtendedState(MAXIMIZED_BOTH);
//		setSize(winW, winH);
		setLocationRelativeTo(null);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setVisible(true);
		animator.start();
		canvas.requestFocus();
	}
	
	/* 
	 * initializes the springs for the mesh.
	 */
	public static void initSprings() {
		
//		Compute the 2 vertices that define each edge, and store the other 2 vertices that are 
//		part of the edge's adjacent faces.
		edge_adjacency = new HashMap<Integer, Edge>();
		ArrayList<Vertex> object_verts = cur_jello_object.getVerts();
		ArrayList<Integer> object_faces = cur_jello_object.getFaces();
		
		for (int i = 0; i < object_faces.size(); i += 3) {
			
			int v0 = object_faces.get(i);
			int v1 = object_faces.get(i+1);
			int v2 = object_faces.get(i+2);		
			
			Vertex v0_vert = object_verts.get(v0);
			Vertex v1_vert = object_verts.get(v1);
			Vertex v2_vert = object_verts.get(v2);
			
//			For each edge, see if an entry for it already exists in the HashMap. If not, create a new 
//			entry and add the two defining vertices, as well as the distance between the vertices
//			and the spring constant.
			int key01 = (v0 < v1) ? v0*object_verts.size() + v1 : v1*object_verts.size() + v0;
			int key02 = (v0 < v2) ? v0*object_verts.size() + v2 : v2*object_verts.size() + v0;
			int key12 = (v1 < v2) ? v1*object_verts.size() + v2 : v2*object_verts.size() + v1;
			
			if(!edge_adjacency.containsKey(key01)) {	
				Edge edge = new Edge(v0, v1);
				edge.setEquilibriumLength(v0_vert.getPosition().distance(v1_vert.getPosition()));
				edge.setSpringConstant(object_spring_constant);
				edge_adjacency.put(key01, edge);
			}
			if(!edge_adjacency.containsKey(key02)) {
				Edge edge = new Edge(v0, v2);
				edge.setEquilibriumLength(v0_vert.getPosition().distance(v2_vert.getPosition()));
				edge.setSpringConstant(object_spring_constant);
				edge_adjacency.put(key02, edge);
			}
			if(!edge_adjacency.containsKey(key12)) {	
				Edge edge = new Edge(v1, v2);
				edge.setEquilibriumLength(v1_vert.getPosition().distance(v2_vert.getPosition()));
				edge.setSpringConstant(object_spring_constant);
				edge_adjacency.put(key12, edge);
			}
		}
		
//		Add some support springs inside the object.
		for (int i = 0; i < object_verts.size(); i++) {
			int v0 = i;
			Vertex v0_vert = object_verts.get(v0);
			for (int j = 0; j < object_verts.size(); j++) {
				int v1 = j;
				Vertex v1_vert = object_verts.get(v1);
				int key = (v0 < v1) ? v0*object_verts.size() + v1 : v1*object_verts.size() + v0;
				
				if (j % support_spring_modulus == 0 && i != j && !edge_adjacency.containsKey(key)) {
					Edge edge = new Edge(v0, v1);
					edge.setEquilibriumLength(v0_vert.getPosition().distance(v1_vert.getPosition()));
					edge.setSpringConstant(object_spring_constant);
					edge_adjacency.put(key, edge);
				}
			}
		}
	}
	
	public static void printUsage() {
		System.out.println("Usage: java JelloSkeeBall jello1.obj,jello2.obj environment.obj goal_plane.obj");
		System.exit(1);
	}

	public static void main(String[] args) {

//		if (args.length < 3) {
//			printUsage();
//		}
//		String jelloObjectFilenames = args[0];
//		String[] jelloObjectFilenamesArray = jelloObjectFilenames.split(",");
//		String collisionEnvironmentFilename = args[1];
//		String goalPlaneFilename = args[2];
		String jelloObjectFilenames = "chamfer.obj,iso.obj,box.obj,pyramid.obj";
		String[] jelloObjectFilenamesArray = jelloObjectFilenames.split(",");
		String collisionEnvironmentFilename = "skeeball_map.obj";
		String goalPlaneFilename = "skeeball_goal_plane.obj";
		
//		Initialize the holders to load meshes into.
		ArrayList<Point3f> input_verts = new ArrayList<Point3f>();
		ArrayList<Integer> input_faces = new ArrayList<Integer>();
		ArrayList<Vertex> object_verts = new ArrayList<Vertex>();
		ArrayList<Vector3f> object_normals = new ArrayList<Vector3f>();
		
//		Load and initialize the jello objects.
		for (String input_filename : jelloObjectFilenamesArray) {	

			input_verts = new ArrayList<Point3f>();
			input_faces = new ArrayList<Integer>();
			object_verts = new ArrayList<Vertex>();
			object_normals = new ArrayList<Vector3f>();
			
			loadMesh(input_filename, input_verts, input_faces);
			for (int i = 0; i < input_verts.size(); i++) {
				Point3f cur_position = new Point3f(input_verts.get(i));
				Vector3f cur_velocity = new Vector3f(0, 0, 0);
				Vertex cur_vert = new Vertex(cur_position, vertex_mass, cur_velocity);
				object_verts.add(cur_vert);
			}

//			Scale down the object to fit in a certain bounding box.
			Vector3f boundingBox = computeBoundingBox(object_verts);
			Vector3f scaleFactors = new Vector3f(jello_object_max_dimensions.x/boundingBox.x, jello_object_max_dimensions.y/boundingBox.y, jello_object_max_dimensions.z/boundingBox.z);
			float scaleFactor = scaleFactors.x > scaleFactors.y ? scaleFactors.x : scaleFactors.y;
			scaleFactor = scaleFactors.z > scaleFactor ? scaleFactors.z : scaleFactor;
			for (Vertex v : object_verts) {
				v.getPosition().scale(scaleFactor);
			}
			
//			Move the object to the starting point.
			Point3f pointOnObject = object_verts.get(0).getPosition();
			Point3f vectorToStartPoint = new Point3f(startPoint);
			vectorToStartPoint.sub(pointOnObject);
			for (Vertex v : object_verts) {
				v.getPosition().add(vectorToStartPoint);
			}
			
			estimateFaceNormal(object_verts, input_faces, object_normals);
			jello_objects.add(new Object(object_verts, input_faces, object_normals));
			
		}


//		Load and initialize the collision surface.
		input_verts = new ArrayList<Point3f>();
		input_faces = new ArrayList<Integer>();
		object_verts = new ArrayList<Vertex>();
		object_normals = new ArrayList<Vector3f>();
		
		loadMesh(collisionEnvironmentFilename, input_verts, input_faces);
		for (int i = 0; i < input_verts.size(); i++) {
			Point3f cur_position = new Point3f(input_verts.get(i));
			Vector3f cur_velocity = new Vector3f(0, 0, 0);
			Vertex cur_vert = new Vertex(cur_position, vertex_mass, cur_velocity);
			object_verts.add(cur_vert);
		}
		estimateFaceNormal(object_verts, input_faces, object_normals);
		collision_environment = new Object(object_verts, input_faces, object_normals);
		
//		Build the acceleration structure for the collision surface.
		collision_tree = new CollisionTree(object_verts, input_faces);
		
		
//		Load and initialize the goal plane surface.
		input_verts = new ArrayList<Point3f>();
		input_faces = new ArrayList<Integer>();
		object_verts = new ArrayList<Vertex>();
		object_normals = new ArrayList<Vector3f>();
		
		loadMesh(goalPlaneFilename, input_verts, input_faces);
		for (int i = 0; i < input_verts.size(); i++) {
			Point3f cur_position = new Point3f(input_verts.get(i));
			Vector3f cur_velocity = new Vector3f(0, 0, 0);
			Vertex cur_vert = new Vertex(cur_position, vertex_mass, cur_velocity);
			object_verts.add(cur_vert);
		}
		estimateFaceNormal(object_verts, input_faces, object_normals);
		goal_plane = new Object(object_verts, input_faces, object_normals);
		
//		Build the acceleration structure for the goal plane collision surface.
		goal_plane_collision_tree = new CollisionTree(object_verts, input_faces);
		
		
//		Initialize the goal plane locations.
		Point3f top_left = new Point3f(1f, 1.5f, .5f);
		Point3f top_right = new Point3f(-1f, 1.5f, .5f);
		Point3f top_middle = new Point3f(0f, 1.5f, .5f);
		Point3f middle_left = new Point3f(-.3f, 1f, 0f);
		Point3f middle_right = new Point3f(.3f, 1f, 0f);
		Point3f bottom_right = new Point3f(-.6f, .3f, -.8f);
		Point3f bottom_left = new Point3f(.6f, .3f, -.8f);
		goal_point_locations.add(top_left);
		goal_point_locations.add(top_right);
		goal_point_locations.add(top_middle);
		goal_point_locations.add(middle_left);
		goal_point_locations.add(middle_right);
		goal_point_locations.add(bottom_right);
		goal_point_locations.add(bottom_left);
		
//		Initialize the goal points.
		goal_points.add(100);
		goal_points.add(100);
		goal_points.add(200);
		goal_points.add(50);
		goal_points.add(50);
		goal_points.add(25);
		goal_points.add(25);
				
//		Initialize the jello object multipliers.
		jello_object_multipliers.add(1); // chamfer box
		jello_object_multipliers.add(2); // isohedron
		jello_object_multipliers.add(3); // box
		jello_object_multipliers.add(4); // pyramid
		
		new JelloSkeeBall();
	}

	
	/**
	 * Used to update the jello objects position via the mouse.
	 * @param xDiff
	 * @param yDiff
	 */
	public void updateVertexPositions(int xDiff, int yDiff) {

		ArrayList<Vertex> object_verts = cur_jello_object.getVerts();
		
		for (int i = 0; i < object_verts.size(); i++) {
			Point3f cur_position = object_verts.get(i).getPosition();
			cur_position.x -= (float)xDiff * mousePositionScale;
			cur_position.z -= (float)yDiff * mousePositionScale;
		}
	}
	
	/**
	 * Used to update the jello objects velocity via the mouse.
	 * @param xDiff
	 * @param yDiff
	 */
	public void updateVertexVelocities(int xDiff, int yDiff) {
		
		ArrayList<Vertex> object_verts = cur_jello_object.getVerts();
		
		for (int i = 0; i < object_verts.size(); i++) {
			Vector3f cur_velocity = object_verts.get(i).getVelocity();
			cur_velocity.x = -(float)xDiff * mouseVelocityScale;
			cur_velocity.z = -(float)yDiff * mouseVelocityScale;
			cur_velocity.y = -downward_velocity;
		}
	}
	
	/* setup GL display parameters */
	public void init(GLAutoDrawable drawable) {
		gl = drawable.getGL();

		initViewParameters();
		
		gl.glClearColor(.3f, .3f, .3f, 1f);
		gl.glClearDepth(1.0f);

		float mat_specular[] = {0.9f, 0.9f, 0.9f, 1.0f};
		float mat_diffuse[] = {0.8f, 0.5f, 0.2f, 1.0f};
		float mat_shiny[] = {40.f};
		
		gl.glEnable(GL.GL_DEPTH_TEST);
		gl.glDepthFunc(GL.GL_LESS);
		gl.glHint(GL.GL_PERSPECTIVE_CORRECTION_HINT, GL.GL_NICEST);
		
		gl.glCullFace(GL.GL_BACK);
		gl.glEnable(GL.GL_CULL_FACE);
		gl.glShadeModel(GL.GL_SMOOTH);		

		gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL.GL_SPECULAR, mat_specular, 0);
		gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL.GL_DIFFUSE, mat_diffuse, 0);
		gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL.GL_SHININESS, mat_shiny, 0);
		
		gl.glEnable(GL.GL_LIGHTING);
		gl.glEnable(GL.GL_LIGHT0);

	}
	
	/* mouse and keyboard callback functions */
	public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
		winW = width;
		winH = height;

		gl.glViewport(0, 0, width, height);
		gl.glMatrixMode(GL.GL_PROJECTION);
			gl.glLoadIdentity();
			glu.gluPerspective(fov, (float)width/(float)height, znear, zfar);
		gl.glMatrixMode(GL.GL_MODELVIEW);
	}
	
	/**
	 * Load a random jello object.
	 */
	public void spawnRandomJelloObject() {
		Random r = new Random();
		double rand = r.nextDouble();
		cur_jello_object_index = (int) (rand*jello_objects.size());
		cur_jello_object = new Object(jello_objects.get(cur_jello_object_index));
		initSprings();
	}
	
	public void mousePressed(MouseEvent e) {	
		mouseX = e.getX();
		mouseY = e.getY();
		mouseButton = e.getButton();
//		canvas.display();
	}
	
	public void mouseReleased(MouseEvent e) {
//		mouseButton = MouseEvent.NOBUTTON;
		
		int x = e.getX();
		int y = e.getY();
		if (mouseButton == MouseEvent.BUTTON1) {
			if (game_state == GameState.MAIN_MENU) {
				String text = "Play";
				TextRenderer renderer = new TextRenderer(new Font("Serif", Font.BOLD, 72), true, true);
				Rectangle2D bounds = renderer.getBounds(text);
				float w = (float) bounds.getWidth();
				float h = (float) bounds.getHeight();
				
				Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
				int winW = dim.width;
				int winH = dim.height;
				
				int xt = x - winW/2;
				int yt = winH/2 - y;
				
				if (xt > 0 && xt < w &&
					yt > 0 && yt < h) {
					
//					Play was clicked.
					game_state = GameState.PLAYING_BEFORE_TOSS;
					spawnRandomJelloObject();
					score = 0;
					downward_velocity = 0;
					time_left_in_round_seconds = time_per_round_seconds;
					last_points_scored = 0;
				}
				
			}
			else if (game_state == GameState.PLAYING_BEFORE_TOSS) {
				mouse_dragged = false;
				updateVertexVelocities(x - mouseX2, y - mouseY2);
				game_state = GameState.PLAYING_AFTER_TOSS;
				time_left_in_toss_seconds = max_time_per_toss_seconds;
				last_points_scored = 0;
			}
		}
	}	
	
	public void mouseDragged(MouseEvent e) {
		int x = e.getX();
		int y = e.getY();
		if (game_state == GameState.PLAYING_BEFORE_TOSS && mouseButton == MouseEvent.BUTTON1) {
			mouse_dragged = true;
			updateVertexPositions(x - mouseX, y - mouseY);
			mouseX2 = mouseX;
			mouseY2 = mouseY;
			mouseX = x;
			mouseY = y;
		}
		
		
//		 Could be useful for zoom in/out & rotation of the level
		/*
		if (mouseButton == MouseEvent.BUTTON3) {
			zpos -= (y - mouseY) * motionSpeed;
			mouseX = x;
			mouseY = y;
//			canvas.display();
		} else if (mouseButton == MouseEvent.BUTTON2) {
			xpos -= (x - mouseX) * motionSpeed;
			ypos += (y - mouseY) * motionSpeed;
			mouseX = x;
			mouseY = y;
//			canvas.display();
		} else if (mouseButton == MouseEvent.BUTTON1) {
			roth -= (x - mouseX) * rotateSpeed;
			rotv += (y - mouseY) * rotateSpeed;
			mouseX = x;
			mouseY = y;
//			canvas.display();
		}
		*/
		
	}

	public void keyPressed(KeyEvent e) {
		switch(e.getKeyCode()) {
		case KeyEvent.VK_ESCAPE:
		case KeyEvent.VK_Q:
			System.exit(0);
			break;		
		case 'r':
		case 'R':
			initViewParameters();
			break;
		case 'w':
		case 'W':
			wireframe = ! wireframe;
			break;
		case 'b':
		case 'B':
			cullface = !cullface;
			break;
		case KeyEvent.VK_DOWN:
			if (game_state == GameState.PLAYING_BEFORE_TOSS) {
				downward_velocity++;
			}
			break;
		case KeyEvent.VK_UP:
			if (game_state == GameState.PLAYING_BEFORE_TOSS) {
				downward_velocity--;
			}
			break;
		default:
			break;
		}
//		canvas.display();
	}
	
	/* display the current subdivision mesh */
	public void display(GLAutoDrawable drawable) {
		
		
		// setup lighting
		float lightPos[] = {0.f, 2.f, 0.f, 1.0f};
		gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
		
		gl.glPolygonMode(GL.GL_FRONT_AND_BACK, wireframe ? GL.GL_LINE : GL.GL_FILL);	
		if (cullface)
			gl.glEnable(GL.GL_CULL_FACE);
		else
			gl.glDisable(GL.GL_CULL_FACE);		
		
		gl.glDisable(GL.GL_CULL_FACE);
		
		
		gl.glLoadIdentity();
		
		gl.glLightfv(GL.GL_LIGHT0, GL.GL_POSITION, lightPos, 0);
		gl.glLightModeli(GL.GL_LIGHT_MODEL_TWO_SIDE, GL.GL_TRUE);
		
//		Move the camera in position.
		glu.gluLookAt(camera_eye.x, camera_eye.y, camera_eye.z, camera_ref_point.x, camera_ref_point.y, camera_ref_point.z, 0, 1, 0);
		
		
//		Check the game state to see what to do.
		if (game_state == GameState.PLAYING_BEFORE_TOSS || game_state == GameState.PLAYING_AFTER_TOSS) {
			time_left_in_round_seconds -= 1/(float)fps;
			
//			Check to see if the round time is up.
			if (time_left_in_round_seconds < 0) {
				game_state = GameState.MAIN_MENU;
			}
			
			ArrayList<Vertex> object_verts = cur_jello_object.getVerts();
			ArrayList<Integer> object_faces = cur_jello_object.getFaces();
			ArrayList<Vector3f> object_normals = cur_jello_object.getNormals();

			ArrayList<Vertex> collision_verts = collision_environment.getVerts();
			ArrayList<Integer> collision_faces = collision_environment.getFaces();
			ArrayList<Vector3f> collision_normals = collision_environment.getNormals();

			ArrayList<Vertex> goal_plane_verts = goal_plane.getVerts();
			ArrayList<Integer> goal_plane_faces = goal_plane.getFaces();
			ArrayList<Vector3f> goal_plane_normals = goal_plane.getNormals();
			
//			Simulate the spring and collision physics if the jello has been tossed.
			if (game_state == GameState.PLAYING_AFTER_TOSS) {
				
				time_left_in_toss_seconds -= 1f/(float)fps;
				
//				Check to see if the toss time is up.
				if (time_left_in_toss_seconds < 0) {
					game_state = GameState.PLAYING_BEFORE_TOSS;
					spawnRandomJelloObject();
					object_verts = cur_jello_object.getVerts();
					object_faces = cur_jello_object.getFaces();
					object_normals = cur_jello_object.getNormals();
				}
				
				
				
				for (int i = 0; i < num_physics_iterations_per_frame; i++) {
					Physics.computeRungeKutta(object_verts, 
							edge_adjacency, 
							object_normals, 
							collision_verts, 
							collision_faces, 
							collision_tree, 
							collision_spring_constant, 
							cushion_amount, 
							gravity, 
							physics_time_interval);	
				}
				
//				Check to see if a goal was scored.
				for (Vertex v : object_verts) {
					
					ArrayList<Integer> collision_face_indices = goal_plane_collision_tree.detectCollision(goal_plane_verts, goal_plane_faces, v.getPrevPosition(), v.getPosition(), cushion_amount);
					
					if (collision_face_indices.size() > 0) {
						
//						A goal was scored. Find out which goal was scored in
//						and update the score.
						Point3f position = v.getPosition();
						float min_dist = 99999f;
						int min_index = 0;
						
						for (int i = 0; i < goal_points.size(); i++) {
							float cur_dist = position.distance(goal_point_locations.get(i));
							if (cur_dist < min_dist) {
								min_dist = cur_dist;
								min_index = i;
							}
						}
						
						score += goal_points.get(min_index) * jello_object_multipliers.get(cur_jello_object_index);
						
						last_points_scored = goal_points.get(min_index) * jello_object_multipliers.get(cur_jello_object_index);
						
						
//						Reset the jello object.
						spawnRandomJelloObject();
						game_state = GameState.PLAYING_BEFORE_TOSS;
						break;
					}
				}
			}
			
			estimateFaceNormal(object_verts, object_faces, object_normals);
			

			int i;
			gl.glBegin(GL.GL_TRIANGLES);

//			Draw the jello object.
			for (i = 0; i < object_faces.size(); i ++) {
				int vid = object_faces.get(i);
				gl.glNormal3f(object_normals.get(i).x, object_normals.get(i).y, object_normals.get(i).z);
				Point3f cur_position = object_verts.get(vid).getPosition();
				gl.glVertex3f(cur_position.x, cur_position.y, cur_position.z);
			}

//			Draw the collision mesh.
			for (i = 0; i < collision_faces.size(); i ++) {
				int vid = collision_faces.get(i);
				gl.glNormal3f(collision_normals.get(i).x, collision_normals.get(i).y, collision_normals.get(i).z);
				Point3f cur_position = collision_verts.get(vid).getPosition();
				gl.glVertex3f(cur_position.x, cur_position.y, cur_position.z);
			}

//			Draw the goal plane.
			/*
			for (i = 0; i < goal_plane_faces.size(); i ++) {
				int vid = goal_plane_faces.get(i);
				gl.glNormal3f(goal_plane_normals.get(i).x, goal_plane_normals.get(i).y, goal_plane_normals.get(i).z);
				Point3f cur_position = goal_plane_verts.get(vid).getPosition();
				gl.glVertex3f(cur_position.x, cur_position.y, cur_position.z);
			}
			*/
			
			
//			Draw the downward velocity arrow.
			if (game_state == GameState.PLAYING_BEFORE_TOSS) {
				gl.glVertex3f(.6f, 1.2f, -4);
				gl.glVertex3f(.7f, 1.2f, -4);
				gl.glVertex3f(.65f, 1.2f-downward_velocity*downward_velocity_scale_factor, -4);
			}
			
			gl.glEnd();
			

			
//			Draw the time left in the round and the score, and the last points scored if applicable.
			String score_str = "Score: " + Integer.toString(score);
			String time_str = "Time left: " + Integer.toString((int)time_left_in_round_seconds);
			String last_points_scored_str = "+" + Integer.toString(last_points_scored);
			
			TextRenderer renderer = new TextRenderer(new Font("Serif", Font.BOLD, 72), true, true);
			renderer.beginRendering(drawable.getWidth(), drawable.getHeight());
			
			// set the color
			renderer.setColor(1.0f, 0.2f, 0.2f, 0.8f);
			renderer.draw(score_str, (int)(drawable.getWidth()*.6), drawable.getHeight() - 50);
			renderer.draw(time_str, (int)(drawable.getWidth()*.3), drawable.getHeight() - 50);
			if (last_points_scored > 0) {
				renderer.draw(last_points_scored_str, drawable.getWidth()/2 - 50, drawable.getHeight() - 150);
			}
			
			renderer.endRendering();
			
			

		}
		
//		Draw the main menu.
		else if (game_state == GameState.MAIN_MENU) {
			String text = "Play";
			String score_str = "Score: " + Integer.toString(score);
			
			TextRenderer renderer = new TextRenderer(new Font("Serif", Font.BOLD, 72), true, true);
			renderer.beginRendering(drawable.getWidth(), drawable.getHeight());
			
			// set the color
			renderer.setColor(1.0f, 0.2f, 0.2f, 0.8f);
			renderer.draw(text, drawable.getWidth()/2, drawable.getHeight()/2);
			if(score > 0) {
				renderer.draw(score_str, drawable.getWidth()/2, drawable.getHeight()/2 + 100);
			}
			renderer.endRendering();
		}
	}
	
	/* computes optimal transformation parameters for OpenGL rendering.
	 * these parameters will place the object at the center of the screen,
	 * and zoom out just enough so that the entire object is visible in the window
	 */
	void initViewParameters()
	{
		roth = rotv = 0;

		float ball_r = (float) Math.sqrt((xmax-xmin)*(xmax-xmin)
							+ (ymax-ymin)*(ymax-ymin)
							+ (zmax-zmin)*(zmax-zmin)) * 0.707f;

		centerx = (xmax+xmin)/2.f;
		centery = (ymax+ymin)/2.f;
		centerz = (zmax+zmin)/2.f;
		xpos = centerx;
		ypos = centery;
		float extra = 100;
		zpos = ball_r/(float) Math.sin(30.f*Math.PI/180.f)+centerz + extra;

		znear = 0.02f;
		zfar  = zpos - centerz + 3.f * ball_r;
	}	
	
	/* estimate face normals */
	private static void estimateFaceNormal(ArrayList<Vertex> verts, ArrayList<Integer> faces, ArrayList<Vector3f> normals) {
		int i;
		normals.clear();
		for (i = 0; i < faces.size(); i ++) {
			normals.add(new Vector3f());
		}
		
		Vector3f e1 = new Vector3f();
		Vector3f e2 = new Vector3f();
		for (i = 0; i < faces.size()/3; i ++) {
			// get face
			int v1 = faces.get(3*i+0);
			int v2 = faces.get(3*i+1);
			int v3 = faces.get(3*i+2);
			
			Point3f v1_position = verts.get(v1).getPosition();
			Point3f v2_position = verts.get(v2).getPosition();
			Point3f v3_position = verts.get(v3).getPosition();
			
			// compute normal
			e1.sub(v2_position, v1_position);
			e2.sub(v3_position, v1_position);
			normals.get(i*3+0).cross(e1, e2);
			normals.get(i*3+0).normalize();
			normals.get(i*3+1).cross(e1, e2);
			normals.get(i*3+1).normalize();
			normals.get(i*3+2).cross(e1, e2);
			normals.get(i*3+2).normalize();
		}
	}
	
	/* find the bounding box for the vertices */
	private static Vector3f computeBoundingBox(ArrayList<Vertex> verts) {
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

		Vector3f box = new Vector3f(xmax - xmin, ymax - ymin, zmax - zmin);
		
		return box;
	}
	
	// these event functions are not used for this assignment
	public void displayChanged(GLAutoDrawable drawable, boolean modeChanged, boolean deviceChanged) { }
	public void keyTyped(KeyEvent e) { }
	public void keyReleased(KeyEvent e) { }
	public void mouseMoved(MouseEvent e) { }
	public void actionPerformed(ActionEvent e) { }
	public void mouseClicked(MouseEvent e) { }
	public void mouseEntered(MouseEvent e) { }
	public void mouseExited(MouseEvent e) {	}	

	
}

