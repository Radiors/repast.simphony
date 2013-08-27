/**
 * 
 */
package repast.simphony.statecharts.editor;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;

import repast.simphony.statecharts.generator.TemplateGenerator;
import repast.simphony.statecharts.part.StatechartDiagramEditorPlugin;
import repast.simphony.statecharts.scmodel.AbstractState;
import repast.simphony.statecharts.scmodel.Transition;

/**
 * Support for creating, initializing and disposing of CodePropertyEditors.
 * 
 * @author Nick Collier
 */
public class EditorSupport {

  private List<CodePropertyEditor> editors = new ArrayList<>();

  private IProject findProject() {
    IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
    IFileEditorInput input = (IFileEditorInput) window.getActivePage().getActiveEditor()
        .getEditorInput();
    return input.getFile().getProject();
  }

  private void initEditorInput(IFile file, CodePropertyEditor editor) {
    IFileEditorInput oldInput = (IFileEditorInput) editor.getEditorInput();
    IFile oldFile = null;
    if (oldInput != null) {
      oldFile = oldInput.getFile();
    }

    try {
      file.refreshLocal(IResource.DEPTH_ZERO, null);
    } catch (CoreException e) {
      StatechartDiagramEditorPlugin.getInstance().logError("Error refreshing temporary edit file",
          e);
    }
    IFileEditorInput input = new FileEditorInput(file);

    IViewPart part = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
        .findView("org.eclipse.ui.views.PropertySheet");
    editor.init(part.getSite(), input);
    
    if (oldFile != null && oldFile.exists()) {
      try {
        oldFile.getParent().refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
        oldFile.delete(IResource.FORCE, new NullProgressMonitor());
      } catch (CoreException e) {
        StatechartDiagramEditorPlugin.getInstance().logError("Error deleting temporary edit file",
            e);
      }
    }
  }

  public void initTriggerCondition(Transition transition, int editorIndex) {
    IProject proj = findProject();
    TemplateGenerator gen = new TemplateGenerator();
    IPath path = gen.generateTriggerCondition(proj, transition);

    IFile file = proj.getFile(path);
    initEditorInput(file, editors.get(editorIndex));
  }

  public void initTriggerDbl(Transition transition, int editorIndex) {
    IProject proj = findProject();
    TemplateGenerator gen = new TemplateGenerator();
    IPath path = gen.generateTriggerDbl(proj, transition);

    IFile file = proj.getFile(path);
    initEditorInput(file, editors.get(editorIndex));
  }

  public void initTriggerME(Transition transition, int editorIndex) {
    IProject proj = findProject();
    TemplateGenerator gen = new TemplateGenerator();
    IPath path = gen.generateMessageEq(proj, transition);

    IFile file = proj.getFile(path);
    initEditorInput(file, editors.get(editorIndex));
  }

  public void initTriggerMC(Transition transition, int editorIndex) {

    IProject proj = findProject();
    TemplateGenerator gen = new TemplateGenerator();
    IPath path = gen.generateMessageCond(proj, transition);

    IFile file = proj.getFile(path);
    initEditorInput(file, editors.get(editorIndex));
  }

  public void initGuard(Transition transition, int editorIndex) {
    IProject proj = findProject();
    TemplateGenerator gen = new TemplateGenerator();
    IPath path = gen.generateGuard(proj, transition);

    IFile file = proj.getFile(path);
    initEditorInput(file, editors.get(editorIndex));
  }

  public void initOnTrans(Transition transition, int editorIndex) {
    IProject proj = findProject();
    TemplateGenerator gen = new TemplateGenerator();
    IPath path = gen.generateOnTrans(proj, transition);

    IFile file = proj.getFile(path);
    initEditorInput(file, editors.get(editorIndex));
  }

  public void init(AbstractState state) {
    IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
    IFileEditorInput input = (IFileEditorInput) window.getActivePage().getActiveEditor()
        .getEditorInput();
    IProject proj = input.getFile().getProject();

    TemplateGenerator gen = new TemplateGenerator();
    for (CodePropertyEditor editor : editors) {
      IPath path = gen.run(proj, state);

      IFile file = proj.getFile(path);
      try {
        file.refreshLocal(IResource.DEPTH_ZERO, null);
      } catch (CoreException e) {
        StatechartDiagramEditorPlugin.getInstance().logError(
            "Error refreshing temporary edit file", e);
      }
      input = new FileEditorInput(file);

      IViewPart part = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
          .findView("org.eclipse.ui.views.PropertySheet");
      editor.init(part.getSite(), input);
    }

  }

  public void dispose() throws CoreException {
    for (CodePropertyEditor editor : editors) {
      editor.dispose();
      FileEditorInput input = (FileEditorInput) editor.getEditorInput();
      if (input != null)
        input.getFile().delete(true, new NullProgressMonitor());
    }
    editors.clear();
  }

  public CodePropertyEditor createEditor() {
    CodePropertyEditor editor = new CodePropertyEditor();
    editors.add(editor);
    return editor;
  }

  public CodePropertyEditor getEditor(int index) {
    return editors.get(index);
  }
}
