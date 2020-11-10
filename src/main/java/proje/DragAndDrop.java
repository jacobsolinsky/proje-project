package proje;


import javafx.application.Application;
import javafx.scene.input.ClipboardContent;
import javafx.scene.text.Text;
import javafx.scene.input.TransferMode;
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
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseStart;
import static com.github.javaparser.Providers.provider;
import org.graphstream.graph.Graph;
import org.graphstream.graph.implementations.*;
import static org.graphstream.algorithm.Toolkit.nodePosition;
import org.graphstream.ui.layout.springbox.implementations.SpringBox;
import java.util.Optional;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.util.ArrayList;
import javafx.scene.Node;
import java.io.IOException;
import javafx.stage.Popup;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.SimpleName;

public class DragAndDrop extends Application {
	HashMap<String, EditingType> typeMap = new HashMap<>();
	HashMap<String, EditingPackage> packageMap = new HashMap<>();
	static EditingMember passMember = null;
	static EditingImport passImport = null;
	
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
	
	class EditingImport{
		class ImportTextEdit{
			VBox vb = new VBox();
			HBox hb = new HBox();
			Button deleteButton = new Button("Delete");
			Button addAfterButton =  new Button("+");
			TextField tf = new TextField();
			{
				deleteButton.setOnAction(e -> delete());
				addAfterButton.setOnAction(e -> addImportAfter());
				addAfterButton.setOnDragOver(e -> {
					e.acceptTransferModes(TransferMode.MOVE);
					e.consume();
				});
				addAfterButton.setOnDragDropped(e -> {
					passImport.moveImport(parent.importList.indexOf(EditingImport.this), parent);
					e.setDropCompleted(true);
					e.consume();
				});
				hb.getChildren().addAll(deleteButton, tf);
				vb.getChildren().addAll(addAfterButton, hb);
				tf.setText(innerText);
				updateParse(innerText);
				tf.textProperty().addListener((e, olds, news) -> updateInnerText(news));
				tf.focusedProperty().addListener((e, olds, news) -> {
						if (!news) {
							updateParse(tf.getText());
						}});
				
			}
			Node getMainNode() {
				return vb;
			}
		}
		String innerText;
		ImportDeclaration innerNode;
		ArrayList<ImportTextEdit> views = new ArrayList<>();
		EditingCompilationUnit parent;
		EditingImport(ImportDeclaration bd, EditingCompilationUnit parent, Integer idx){
			this.parent = parent;
			if (idx == null) {
				parent.importList.add(this);
			} else {
				parent.importList.add(idx, this);
			}
			innerText = bd.toString();
			innerNode = bd;
			
		}
		ImportTextEdit makeNode() {
			var retval = new ImportTextEdit();
			views.add(retval);
			return retval;
		}
		void updateParse(String s) {
			 var pr = jp.parse(ParseStart.IMPORT_DECLARATION, provider(s));
			 if (pr.isSuccessful()) {
				 updateInnerText(s);
				 var newbd = pr.getResult().get();
				 innerNode.replace(newbd);
				 innerNode = newbd;
			 } else {
				 updateInnerText(innerNode.toString());
			 }
		}
		void updateInnerText(String s) {
			innerText = s;
    		for (var ite: views) {
    			ite.tf.setText(innerText);
    		}
		}
		void delete() {
    		for (var ite: views) {
    			var mn = ite.getMainNode();
    			((VBox) mn.getParent()).getChildren().remove(mn);
    		}
    		parent.cu.getImports().remove(innerNode);
			parent.importList.remove(this);
		}
		void addImportAfter() {
			parent.addImport(parent.importList.indexOf(this));
		}
		void moveImport(int idx, EditingCompilationUnit newParent) {
			delete();
			newParent.addImport(idx, innerNode);
		}
		
	}
	
	class EditingMember{
		class InnerTextEdit{
			TitledPane tp = new TitledPane();
			VBox vb = new VBox();
			HBox hb = new HBox();
			Button deleteButton = new Button("Delete");
			Button addAfterButton =  new Button("+");
			TextArea tf = new TextArea();
			{
				deleteButton.setOnAction(e -> delete());
				addAfterButton.setOnAction(e -> addMemberAfter());
				addAfterButton.setOnDragOver(e -> {
					e.acceptTransferModes(TransferMode.MOVE);
					e.consume();
				});
				addAfterButton.setOnDragDropped(e -> {
					passMember.moveMember(parent.memberList.indexOf(EditingMember.this), parent);
					e.setDropCompleted(true);
					e.consume();
				});
				
				hb.getChildren().addAll(deleteButton, tf);
				tp.setContent(hb);
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
				vb.getChildren().addAll(addAfterButton, tp);
				tp.setOnDragDetected(e -> {
					var db = tp.startDragAndDrop(TransferMode.MOVE);
					db.setDragView(new Text(titleText).snapshot(null, null), e.getX(), e.getY());
					passMember = EditingMember.this;
					var content = new ClipboardContent();
			        content.putString("whatever");
			        db.setContent(content);
					e.consume();
				});
				
			}
			Node getMainNode() {
				return vb;
			}
		}
		String innerText;
		String titleText;
		BodyDeclaration<?> innerNode;
		ArrayList<InnerTextEdit> views = new ArrayList<>();
		EditingType parent;
		EditingMember(BodyDeclaration<?> bd, EditingType parent, Integer idx){
			this.parent = parent;
			if (idx == null) {
				parent.memberList.add(this);
			} else {
				parent.memberList.add(idx, this);
			}
			innerText = bd.toString();
			innerNode = bd;
			
		}
		InnerTextEdit makeNode() {
			var retval = new InnerTextEdit();
			views.add(retval);
			return retval;
		}
		void updateParse(String s) {
			 var pr = jp.parse(ParseStart.CLASS_BODY, provider(s));
			 if (pr.isSuccessful()) {
				 updateInnerText(s);
				 var newbd = pr.getResult().get();
				 setTitleText(newbd);
				 innerNode.replace(newbd);
				 innerNode = newbd;
			 } else {
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
    		for (var ite: views) {
    			var mn = ite.getMainNode();
    			((VBox) mn.getParent()).getChildren().remove(mn);
    		}
    		parent.td.remove(innerNode);
			parent.memberList.remove(this);
		}
		void addMemberAfter() {
			parent.addMember(parent.memberList.indexOf(this));
		}
		void moveMember(int idx, EditingType newParent) {
			delete();
			newParent.addMember(idx, innerNode);
		}
		
	}
	
	class EditingType{
		class VbNode{
			TitledPane tp = new TitledPane();
			VBox outerVb = new VBox();
			VBox vb = new VBox();
			Button addMemberButton = new Button("+");
			{
				addMemberButton.setOnAction(e -> addMember(memberList.size()));
				var deleteButton = new Button("Delete this class");
				deleteButton.setOnAction(e -> delete());
				outerVb.getChildren().addAll(vb, addMemberButton, deleteButton);
				tp.setContent(outerVb);
				tp.setAnimated(false);
				tp.setText(typeName);
				tp.expandedProperty().set(false);
				for (EditingMember e: memberList) {
					vb.getChildren().add(e.makeNode().getMainNode());
				}
			}
		}
		Node makeNode() {
			var retval = new VbNode();
			vbList.add(retval);
			return retval.tp;
		}
		
		ArrayList<EditingMember> memberList = new ArrayList<>();
		ArrayList<VbNode> vbList = new ArrayList<>();
		EditingCompilationUnit parent;
		TypeDeclaration<?> td;
		String typeName;
		EditingType(String typeName, TypeDeclaration<?> td, EditingCompilationUnit parent){
			this.parent = parent;
			this.typeName = typeName;
			this.td = td;
			for (BodyDeclaration<?> bd: td.getMembers()) {
				new EditingMember(bd, this, null);
			}
			typeMap.put(typeName, this);
			parent.cuTypeMap.put(typeName, this);
			parent.parent.packTypeMap.put(typeName, this);
			
		}
		
		void addMember(int idx) {
			var bd = new FieldDeclaration();
			td.getMembers().add(idx, bd);
			addMember(idx, bd);
		}
		void addMember(int idx, BodyDeclaration<?> bd) {
			var nem = new EditingMember(bd, this, idx);
			for (VbNode vbn: vbList) {
				var nn = nem.makeNode();
				vbn.vb.getChildren().add(idx, nn.getMainNode());
				nn.tp.expandedProperty().set(true);
				nn.tf.requestFocus();
			}
		}
		void delete() {
    		for (VbNode vbn: vbList) {
    			((VBox) vbn.tp.getParent()).getChildren().remove(vbn.tp);
    		}
			parent.cuTypeMap.remove(this.typeName);
			parent.parent.packTypeMap.remove(this.typeName);
			typeMap.remove(this.typeName);
		}
		
	}
	
	class EditingCompilationUnit{
		class CuNode{
			TitledPane tp = new TitledPane();
			VBox typeVb = new VBox();
			TitledPane importTp = new TitledPane();
			VBox importVb = new VBox();
			{
				tp.setContent(typeVb);
				tp.setAnimated(false);
				tp.setText(fileName);
				tp.expandedProperty().set(false);
				importTp.setContent(importVb);
				importTp.setAnimated(false);
				importTp.setText("imports");
				importTp.expandedProperty().set(false);
				var openInNewWindowButton = new Button("Open in new window");
				openInNewWindowButton.setOnAction(e -> {
					var newNode = makeNode().getMainNode();
					((TitledPane) newNode).expandedProperty().set(true);
					addStage(newNode);
					});
				var hb = new HBox();
				var addTypeButton = new Button("Add class");
				var tf = new TextField();
				addTypeButton.setOnAction(e -> addType(tf.getText()));
				var addImportButton = new Button("Add import");
				addImportButton.setOnAction(e -> addImport(importList.size()));
				hb.getChildren().addAll(addTypeButton, tf);
				typeVb.getChildren().addAll(openInNewWindowButton, addImportButton, importTp, hb);
				for (EditingType e: cuTypeMap.values()) {
					typeVb.getChildren().add(e.makeNode());
				}
				for (EditingImport e: importList) {
					importVb.getChildren().add(e.makeNode().getMainNode());
				}
			}
			Node getMainNode() {
				return tp;
			}
		}
		CuNode makeNode() {
			var retval = new CuNode();
			cuNodeList.add(retval);
			return retval;
		}
		ArrayList<EditingImport> importList = new ArrayList<>();
		ArrayList<CuNode> cuNodeList = new ArrayList<>();
		HashMap<String, EditingType> cuTypeMap = new HashMap<>();
		String fileName;
		CompilationUnit cu;
		String packageName;
		EditingPackage parent;
		EditingCompilationUnit(String packageName, String firstClassName){
			var fileName = packageName.toString().replaceAll("\\.", "/") + firstClassName + ".java";
			var cu = new CompilationUnit();
			if (!packageName.equals("")) {
				cu.setPackageDeclaration(packageName);
			}
			var td = new ClassOrInterfaceDeclaration();
			td.setName(new SimpleName(firstClassName));
			cu.addType(td);
			realConstructor(cu, fileName);
		}
		EditingCompilationUnit(CompilationUnit cu, String fileName){
			realConstructor(cu, fileName);
		}
		void realConstructor(CompilationUnit cu, String fileName) {
			this.fileName = fileName;
			this.cu = cu;
			packageName = "";
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
			this.parent = pack;
			parent.cuMap.put(packageName, this);
			for (TypeDeclaration<?> td: cu.findAll(TypeDeclaration.class)){
				var typeName = td.getName().toString();
				if (!packageName.equals("")) {
					typeName = packageName + "." + typeName;
				}
				new EditingType(typeName, td, this);
			}
			for (ImportDeclaration id: cu.getImports()){
				new EditingImport(id, this, null);
			}
		}
		void save() {
			var f = Paths.get(savePath.toString(), fileName).toFile();
			if (!f.exists()) {
				Paths.get(f.getParent()).toFile().mkdirs();
			}
			try {
				Files.write(f.toPath(), cu.toString().getBytes());
			} catch (IOException e) {
				Popup p = new Popup();
				Label l = new Label(f.toString());
				p.getContent().add(l); 
				p.setAutoHide(true);
				p.show(new Stage());
			}
		}
	  void addType(String typeName) {
		  	String fqTypeName = typeName;
		  	if (!packageName.equals("")) {
		  		fqTypeName = packageName + "." + typeName;
		  	}
			var td = new ClassOrInterfaceDeclaration();
			td.setName(new SimpleName(typeName));
			var ed = new EditingType(fqTypeName, td, this);
			for (CuNode cn: cuNodeList) {
				cn.typeVb.getChildren().add(ed.makeNode());
			}
	  }
		void addImport(int idx) {
			var bd = jp.parse(ParseStart.IMPORT_DECLARATION, provider("import java.lang.*;")).getResult().get();
			cu.getImports().add(idx, bd);
			addImport(idx, bd);
		}
		void addImport(int idx, ImportDeclaration bd) {
			var nem = new EditingImport(bd, this, idx);
			for (var cn: cuNodeList) {
				var nn = nem.makeNode();
				cn.importVb.getChildren().add(idx, nn.getMainNode());
				nn.tf.requestFocus();
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
				var atcbh = new HBox();
				var atcbb = new Button("Add top level class");
				var atcbt = new TextField();
				atcbh.getChildren().addAll(atcbb, atcbt);
				atcbb.setOnAction(e -> {
					addCompilationUnit(atcbt.getText());
				});
				vb.getChildren().add(atcbh);
				for (EditingCompilationUnit e: cuMap.values()) {
					vb.getChildren().add(e.makeNode().getMainNode());
				}
			}
		}
		Node makeNode() {
			var retval = new PackNode();
			pn = retval;
			return retval.tp;
		}
		PackNode pn;
		HashMap<String, EditingCompilationUnit> cuMap = new HashMap<>();
		HashMap<String, EditingType> packTypeMap = new HashMap<>();
		String packageName;
		EditingPackage(String packageName){
			this.packageName = packageName;
			packageMap.put(packageName, this);
		}
		void delete() {
    		root.getChildren().remove(pn.tp);
    		packageMap.remove(this.packageName);
		}
		void addCompilationUnit(String firstClassName) {
			pn.vb.getChildren().add(new EditingCompilationUnit(packageName, firstClassName).makeNode().getMainNode());
		}
	}
  void addPackage(String packageName) {
	  root.getChildren().add(new EditingPackage(packageName).makeNode());
  }
	

  public static int countLines(String s) {
	  return s.split(System.getProperty("line.separator")).length + 1;
  }
  Random random = new Random();

  double orgSceneX, orgSceneY;
  VBox root;
  Scene scene;
  JavaParser jp;
  Path savePath;
  
  
  public void saveProject() {
	  var fileStage = new Stage();
	  fileStage.show();
	  var selectedFile =  new DirectoryChooser().showDialog(fileStage);
	  savePath = selectedFile.toPath();
	  fileStage.close();
	  for (EditingPackage ep: packageMap.values()) {
		  for (EditingCompilationUnit cu: ep.cuMap.values()) {
			  cu.save();
		  }
	  }
	  
  }
  public void startProject() {
	  var fileStage = new Stage();
	  fileStage.show();
	  var selectedFile =  new DirectoryChooser().showDialog(fileStage);
	  srcPath = selectedFile.toString();
	  fileStage.close();
	  
  }
  public Stage addStage(Node n) {
	var st = new Stage();
    ScrollPane sp = new ScrollPane();
    sp.setContent(n);
    var bp = new BorderPane();
    bp.setCenter(sp);
    st.setScene(new Scene(bp));
    st.show();
    return st;
  }

  public void initStage(Stage st) {
	    var sp = new ScrollPane();
	    sp.setContent(root);
	    var bp = new BorderPane();
	    var mb = new MenuBar();
	    var fileMenu = new Menu("File");
	    var runMenu = new Menu("Run");
	    bp.setCenter(sp);
	    bp.setTop(mb);
	    var saveMi = new MenuItem("Save");
	    saveMi.setOnAction(e -> saveProject());
	    var addPackageMi = new MenuItem("Add package");
	    addPackageMi.setOnAction(e -> {
	    	var hb = new HBox();
	    	var tf = new TextField();
	    	var l = new Button("Add package");

	    	hb.getChildren().addAll(l, tf);
	    	var ist = addStage(hb);
	    	l.setOnAction(f -> {
	    		addPackage(tf.getText());
	    		ist.close();
	    	});
	    });
    	var np = new MenuItem("New Project");
    	np.setOnAction(f ->{
	        new DragAndDrop().start(new Stage());
    	});
	    fileMenu.getItems().addAll(saveMi, addPackageMi, np);
	    mb.getMenus().addAll(fileMenu, runMenu);
	    var scene = new Scene(bp, 700, 460);
	    scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
	    st.setScene(scene);
	    st.show();
  }
  
  String srcPath;
  @Override
  public void start(Stage primaryStage) {
	jp = new JavaParser();
    root = new VBox();
    initStage(primaryStage);
	startProject();
    var thing = ProjectParser.parse(srcPath);
	for (Map.Entry<String, CompilationUnit> entry : thing.entrySet()) {
		new EditingCompilationUnit(entry.getValue(), entry.getKey());
	}
	for (EditingPackage e: packageMap.values()) {
		root.getChildren().add(e.makeNode());
	}
	
  }
}