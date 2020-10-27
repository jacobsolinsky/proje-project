package proje;

import javafx.application.Application;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Line;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.stage.DirectoryChooser;
import java.util.HashMap;
import java.util.Random;
import java.util.HashSet;
import java.util.Map;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseStart;
import static com.github.javaparser.Providers.provider;
import org.graphstream.graph.Graph;
import org.graphstream.graph.implementations.*;
import static org.graphstream.algorithm.Toolkit.nodePosition;
import org.graphstream.ui.layout.springbox.implementations.SpringBox;
import java.util.Optional;
import java.nio.file.Paths;
import java.util.ArrayList;
import javafx.scene.Node;


public class DragAndDrop extends Application {
	HashMap<String, EditingType> typeMap = new HashMap<>();
	HashMap<String, EditingPackage> packageMap = new HashMap<>();
	
	static int lineCol2Idx(String s, int l, int col) {
		char cur;
		int lc = 0;
		int i = 0;
		for (i = 0; i < s.length(); i++) {
			if (lc == l - 1) {break;}
			cur = s.charAt(i);
			if (cur == '\n') {lc += 1;}
		}
		int retval = i + col - 1;
		if (retval >= s.length()) {retval = s.length() - 1;}
		return retval;
	}
	
	class EditingMember{
		class InnerTextEdit{
			TitledPane tp = new TitledPane();
			TextArea tf = new TextArea();
			{
				tp.setContent(tf);
				tp.setAnimated(false);
				tf.setText(innerText);
				updateParse(innerText);
				tp.setText(titleText);
				tp.expandedProperty().set(false);
				tf.textProperty().addListener((e, olds, news) -> updateInnerText(news));
				tf.focusedProperty().addListener((e, olds, news) -> {
						if (!news) {
							updateParse(tf.getText());
						}});
			}
		}
		String innerText;
		String titleText;
		BodyDeclaration<?> innerNode;
		ArrayList<InnerTextEdit> views = new ArrayList<>();
		EditingType parent;
		EditingMember(BodyDeclaration<?> bd, EditingType parent){
			this.parent = parent;
			innerText = bd.toString();
			innerNode = bd;
			
		}
		Node makeNode() {
			var retval = new InnerTextEdit();
			views.add(retval);
			return retval.tp;
		}
		void updateParse(String s) {
			 var pr = jp.parse(ParseStart.CLASS_BODY, provider(s));
			 if (pr.isSuccessful()) {
				 System.out.println("nuna");
				 updateInnerText(s);
				 var newbd = pr.getResult().get();
				 setTitleText(newbd);
				 innerNode.replace(newbd);
				 innerNode = newbd;
			 } else {
				 System.out.println("nano");
				 updateInnerText(innerNode.toString());
			 }
		}
		void setTitleText(BodyDeclaration<?> bd){
			bd.ifCallableDeclaration(e -> titleText = e.getDeclarationAsString());
    		bd.ifAnnotationDeclaration(e -> titleText = "@interface" + e.getName().toString());
    		bd.ifClassOrInterfaceDeclaration(e ->{
    			if (e.isInterface()) {
    				innerText = "interface" + e.getName().toString();
    			} else {
    				innerText = "class" + e.getName().toString();
    			}
    		});
    		bd.ifFieldDeclaration(e -> titleText = e.toString());
    		bd.ifEnumConstantDeclaration(e -> titleText = e.toString());
    		bd.ifEnumDeclaration(e -> titleText = "enum" + e.getName().toString());
    		bd.ifInitializerDeclaration(e -> {
    			if (e.isStatic()) {
    				titleText = "static initializer{}";
    			} else {
    				titleText = "initializer {}";
    			}
    			});
    		updateTitleText();

		}
		void updateTitleText() {
    		for (InnerTextEdit ite: views) {
    			ite.tp.setText(titleText);
    		}
		}
		void updateInnerText(String s) {
			innerText = s;
    		for (InnerTextEdit ite: views) {
    			ite.tf.setText(innerText);
    		}
		}
		void delete() {
    		for (InnerTextEdit ite: views) {
    			((VBox) ite.tf.getParent()).getChildren().remove(ite.tf);
    		}
			parent.memberList.remove(this);
		}
		
	}
	
	class EditingType{
		class VbNode{
			TitledPane tp = new TitledPane();
			VBox vb = new VBox();
			{
				tp.setContent(vb);
				tp.setAnimated(false);
				tp.setText(typeName);
				tp.expandedProperty().set(false);
				for (EditingMember e: memberList) {
					vb.getChildren().add(e.makeNode());
				}
			}
		}
		Node makeNode() {
			return new VbNode().tp;
		}
		
		ArrayList<EditingMember> memberList = new ArrayList<>();
		ArrayList<VbNode> vbList = new ArrayList<>();
		TypeDeclaration<?> td;
		String typeName;
		EditingType(String typeName, TypeDeclaration<?> td){
			this.typeName = typeName;
			this.td = td;
			for (BodyDeclaration<?> bd: td.getMembers()) {
				addMember(bd);
			}
		}
		void addMember(BodyDeclaration<?> bd) {
			var nem = new EditingMember(bd, this);
			this.memberList.add(nem);
			for (VbNode vbn: vbList) {
				vbn.vb.getChildren().add(nem.makeNode());
			}
		}
		
	}
	
	class EditingCompilationUnit{
		class CuNode{
			TitledPane tp = new TitledPane();
			VBox vb = new VBox();
			{
				tp.setContent(vb);
				tp.setAnimated(false);
				tp.setText(fileName);
				tp.expandedProperty().set(false);
				var openInNewWindowButton = new Button("Open in new window");
				openInNewWindowButton.setOnAction(e -> addStage(makeNode()));
				vb.getChildren().add(openInNewWindowButton);
				for (EditingType e: cuTypeMap.values()) {
					vb.getChildren().add(e.makeNode());
				}
			}
		}
		Node makeNode() {
			return new CuNode().tp;
		}
		HashMap<String, EditingType> cuTypeMap = new HashMap<>();
		String fileName;
		EditingCompilationUnit(CompilationUnit cu, String fileName){
			this.fileName = fileName;
			String packageName = "";
			if (cu.getPackageDeclaration().isPresent()) {
				packageName = cu.getPackageDeclaration().get().getName().toString();
			}
			EditingPackage pack;
			if (packageMap.containsKey(packageName)){
				pack = packageMap.get(packageName);
			} else {
				pack = new EditingPackage(packageName);
				System.out.println(packageName);
			}
			pack.cuMap.put(packageName, this);
			for (TypeDeclaration<?> td: cu.findAll(TypeDeclaration.class)){
				var typeName = td.getName().toString();
				if (!packageName.equals("")) {
					typeName = packageName + "." + typeName;
				}
				cuTypeMap.put(typeName, new EditingType(typeName, td));
			}
		}
		void save() {
			var f = Paths.get(srcPath.toString(), fileName).toFile();
			if (!f.exists()) {
				f.mkdirs();
			}
		}
	}
	class EditingPackage{
		class PackNode{
			TitledPane tp = new TitledPane();
			VBox vb = new VBox();
			{
				tp.setContent(vb);
				tp.setAnimated(false);
				tp.setText(packageName);
				tp.expandedProperty().set(false);
				for (EditingCompilationUnit e: cuMap.values()) {
					vb.getChildren().add(e.makeNode());
				}
			}
		}
		Node makeNode() {
			return new PackNode().tp;
		}
		HashMap<String, EditingCompilationUnit> cuMap = new HashMap<>();
		HashMap<String, EditingType> packTypeMap = new HashMap<>();
		String packageName;
		EditingPackage(String packageName){
			this.packageName = packageName;
			packageMap.put(packageName, this);
		}
	}
	

  public static int countLines(String s) {
	  return s.split(System.getProperty("line.separator")).length + 1;
  }
  static Random random = new Random();

  double orgSceneX, orgSceneY;
  VBox root;
  Scene scene;
  JavaParser jp;
  
  
  public static void saveProject() {
	  var fileStage = new Stage();
	  fileStage.show();
	  var selectedFile =  new DirectoryChooser().showDialog(fileStage);
	  
  }
  public void addStage(Node n) {
	var st = new Stage();
    ScrollPane sp = new ScrollPane();
    sp.setContent(n);
    var bp = new BorderPane();
    bp.setCenter(sp);
    st.setScene(new Scene(bp, 700, 460));
    st.show();
  }
  
  public void initStage(Stage st) {
	    ScrollPane sp = new ScrollPane();
	    sp.setContent(root);
	    var bp = new BorderPane();
	    HBox topButtons = new HBox();
	    bp.setCenter(sp);
	    bp.setBottom(topButtons);
	    var saveButton = new Button("Save");
	    saveButton.setOnAction(e -> saveProject());
	    topButtons.getChildren().addAll(saveButton);
	    st.setScene(new Scene(bp, 700, 460));
	    st.show();
  }
  
  String srcPath;
  @Override
  public void start(Stage primaryStage) {
	jp = new JavaParser();
    root = new VBox();
    initStage(primaryStage);
    srcPath = "/Users/jacobsolinsky/programming/cytoscape-impl/app-impl";
    var thing = ProjectParser.parse(srcPath);
	for (Map.Entry<String, CompilationUnit> entry : thing.entrySet()) {
		new EditingCompilationUnit(entry.getValue(), entry.getKey());
	}
	for (EditingPackage e: packageMap.values()) {
		root.getChildren().add(e.makeNode());
	}
	
  }
}