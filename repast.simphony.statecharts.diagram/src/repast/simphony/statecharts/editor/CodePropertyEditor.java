/**
 * 
 */
package repast.simphony.statecharts.editor;

import java.util.Iterator;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.text.JavaPairMatcher;
import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.text.JavaTextTools;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.source.IAnnotationAccess;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.IOverviewRuler;
import org.eclipse.jface.text.source.ISharedTextColors;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.OverviewRuler;
import org.eclipse.jface.text.source.VerticalRuler;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IPropertyListener;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.editors.text.TextFileDocumentProvider;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.internal.editors.text.EditorsPlugin;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditorPreferenceConstants;
import org.eclipse.ui.texteditor.AnnotationPreference;
import org.eclipse.ui.texteditor.DefaultMarkerAnnotationAccess;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.MarkerAnnotationPreferences;
import org.eclipse.ui.texteditor.SourceViewerDecorationSupport;

import repast.simphony.statecharts.part.StatechartDiagramEditorPlugin;

/**
 * Minimal editor used for editing code properties. This is necessary in order
 * to use Eclipse's code completion etc.
 * 
 * @author Nick Collier
 */
public class CodePropertyEditor implements ITextEditor {

  private static int VERTICAL_RULER_WIDTH = 12;
  
  // from JavaEditor
  protected final static char[] BRACKETS= { '{', '}', '(', ')', '[', ']', '<', '>' };

  // from AbstractDecoratedTextEditor
  /**
   * Preference key for highlighting current line.
   */
  private final static String CURRENT_LINE = AbstractDecoratedTextEditorPreferenceConstants.EDITOR_CURRENT_LINE;
  /**
   * Preference key for highlight color of current line.
   */
  private final static String CURRENT_LINE_COLOR = AbstractDecoratedTextEditorPreferenceConstants.EDITOR_CURRENT_LINE_COLOR;
  /**
   * Preference key for showing print margin ruler.
   */
  private final static String PRINT_MARGIN = AbstractDecoratedTextEditorPreferenceConstants.EDITOR_PRINT_MARGIN;
  /**
   * Preference key for print margin ruler color.
   */
  private final static String PRINT_MARGIN_COLOR = AbstractDecoratedTextEditorPreferenceConstants.EDITOR_PRINT_MARGIN_COLOR;
  /**
   * Preference key for print margin ruler column.
   */
  private final static String PRINT_MARGIN_COLUMN = AbstractDecoratedTextEditorPreferenceConstants.EDITOR_PRINT_MARGIN_COLUMN;
  
  // from JavaEditor
  /** Preference key for matching brackets. */
  protected final static String MATCHING_BRACKETS=  PreferenceConstants.EDITOR_MATCHING_BRACKETS;

  /**
   * Preference key for highlighting bracket at caret location.
   * 
   * @since 3.8
   */
  protected final static String HIGHLIGHT_BRACKET_AT_CARET_LOCATION= PreferenceConstants.EDITOR_HIGHLIGHT_BRACKET_AT_CARET_LOCATION;
  
  /**
   * Preference key for enclosing brackets.
   * 
   * @since 3.8
   */
  protected final static String ENCLOSING_BRACKETS= PreferenceConstants.EDITOR_ENCLOSING_BRACKETS;

  /** Preference key for matching brackets color. */
  protected final static String MATCHING_BRACKETS_COLOR=  PreferenceConstants.EDITOR_MATCHING_BRACKETS_COLOR;

  private IEditorInput input;
  private IDocumentProvider provider = new TextFileDocumentProvider();
  private JavaSourceViewer viewer;
  private ViewerSupport support;
  private IWorkbenchPartSite site;

  private IDocument doc;
  private IPreferenceStore prefStore;

  /**
   * The annotation preferences.
   */
  private MarkerAnnotationPreferences fAnnotationPreferences;
  private IAnnotationAccess fAnnotationAccess;
  private SourceViewerDecorationSupport fSourceViewerDecorationSupport;

  private IOverviewRuler fOverviewRuler;
  // from JavaEditor
  protected JavaPairMatcher fBracketMatcher= new JavaPairMatcher(BRACKETS);

  public CodePropertyEditor() {
    fAnnotationPreferences = EditorsPlugin.getDefault().getMarkerAnnotationPreferences();
    prefStore = JavaPlugin.getDefault().getCombinedPreferenceStore();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.IEditorPart#getEditorInput()
   */
  @Override
  public IEditorInput getEditorInput() {
    return input;
  }

  public void setEditorInput(IEditorInput input) {
    if (input != null)
      provider.disconnect(input);
    this.input = input;

    try {
      provider.connect(input);
    } catch (CoreException e) {
      e.printStackTrace();
    }
    
 
  }

  protected ISharedTextColors getSharedColors() {
    return EditorsPlugin.getDefault().getSharedTextColors();
  }

  protected SourceViewerDecorationSupport getSourceViewerDecorationSupport(ISourceViewer viewer) {
    if (fSourceViewerDecorationSupport == null) {
      fSourceViewerDecorationSupport = new SourceViewerDecorationSupport(viewer,
          getOverviewRuler(), getAnnotationAccess(), getSharedColors());
      configureSourceViewerDecorationSupport(fSourceViewerDecorationSupport);
    }
    return fSourceViewerDecorationSupport;
  }

  /**
   * Returns the overview ruler.
   * 
   * @return the overview ruler
   */
  protected IOverviewRuler getOverviewRuler() {
    if (fOverviewRuler == null)
      fOverviewRuler = createOverviewRuler(getSharedColors());
    return fOverviewRuler;
  }

  public void createPartControl(Composite parent) {
    viewer = new JavaSourceViewer(parent, new VerticalRuler(VERTICAL_RULER_WIDTH),
        getOverviewRuler());
    getSourceViewerDecorationSupport(viewer);
    
    viewer.configure(prefStore, this);
    getSourceViewerDecorationSupport(viewer).install(prefStore);
    
    IAnnotationModel model = getDocumentProvider().getAnnotationModel(input);
    
    try {
      int offset = doc.getLineOffset(doc.getNumberOfLines() - 4);
      viewer.setDocument(doc, model, offset, 0);
      //doc.replace(offset, 0, vDoc.get());
      //vDoc.addDocumentListener(new UpdatingDocListener(offset, vDoc));
    } catch (BadLocationException e) {
      StatechartDiagramEditorPlugin.getInstance().logError("Error creating code editor document", e);
    }

    // sets up the keyboard actions
    if (support == null)
      support = new ViewerSupport(viewer, (IHandlerService) site.getService(IHandlerService.class));
  }

  @SuppressWarnings("rawtypes")
  protected IOverviewRuler createOverviewRuler(ISharedTextColors sharedColors) {
    IOverviewRuler ruler = new OverviewRuler(getAnnotationAccess(), VERTICAL_RULER_WIDTH,
        sharedColors);

    Iterator e = fAnnotationPreferences.getAnnotationPreferences().iterator();
    while (e.hasNext()) {
      AnnotationPreference preference = (AnnotationPreference) e.next();
      if (preference.contributesToHeader())
        ruler.addHeaderAnnotationType(preference.getAnnotationType());
    }
    return ruler;
  }

  /**
   * Returns the annotation access.
   * 
   * @return the annotation access
   */
  protected IAnnotationAccess getAnnotationAccess() {
    if (fAnnotationAccess == null)
      fAnnotationAccess = createAnnotationAccess();
    return fAnnotationAccess;
  }

  /**
   * Creates the annotation access for this editor.
   * 
   * @return the created annotation access
   */
  protected IAnnotationAccess createAnnotationAccess() {
    return new DefaultMarkerAnnotationAccess();
  }

  /**
   * Configures the decoration support for this editor's source viewer.
   * Subclasses may override this method, but should call their superclass'
   * implementation at some point.
   * 
   * @param support
   *          the decoration support to configure
   */
  @SuppressWarnings("rawtypes")
  protected void configureSourceViewerDecorationSupport(SourceViewerDecorationSupport support) {
    
    fBracketMatcher.setSourceVersion(prefStore.getString(JavaCore.COMPILER_SOURCE));
    support.setCharacterPairMatcher(fBracketMatcher);
    support.setMatchingCharacterPainterPreferenceKeys(MATCHING_BRACKETS, MATCHING_BRACKETS_COLOR, HIGHLIGHT_BRACKET_AT_CARET_LOCATION, ENCLOSING_BRACKETS);

    Iterator e = fAnnotationPreferences.getAnnotationPreferences().iterator();
    while (e.hasNext())
      support.setAnnotationPreference((AnnotationPreference) e.next());

    support.setCursorLinePainterPreferenceKeys(CURRENT_LINE, CURRENT_LINE_COLOR);
    support.setMarginPainterPreferenceKeys(PRINT_MARGIN, PRINT_MARGIN_COLOR, PRINT_MARGIN_COLUMN);
    support.setSymbolicFontName(getFontPropertyPreferenceKey());
  }

  protected final String getFontPropertyPreferenceKey() {
    return JFaceResources.TEXT_FONT;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.IEditorPart#getEditorSite()
   */
  @Override
  public IEditorSite getEditorSite() {
    // no editor site so return null
    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.IEditorPart#init(org.eclipse.ui.IEditorSite,
   * org.eclipse.ui.IEditorInput)
   */
  @Override
  public void init(IEditorSite site, IEditorInput input) throws PartInitException {
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.IEditorPart#init(org.eclipse.ui.IEditorSite,
   * org.eclipse.ui.IEditorInput)
   */

  public void init(IWorkbenchPartSite site, IEditorInput input) {
    setEditorInput(input);
    this.site = site;
    JavaTextTools textTools = JavaPlugin.getDefault().getJavaTextTools();
    IDocumentPartitioner partitioner = textTools.createDocumentPartitioner();
    doc = provider.getDocument(input);
    doc.setDocumentPartitioner(partitioner);
    partitioner.connect(doc);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.IWorkbenchPart#dispose()
   */
  @Override
  public void dispose() {
    fSourceViewerDecorationSupport.uninstall();
    viewer.unconfigure();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.IWorkbenchPart#getSite()
   */
  @Override
  public IWorkbenchPartSite getSite() {
    return site;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.texteditor.ITextEditor#getDocumentProvider()
   */
  @Override
  public IDocumentProvider getDocumentProvider() {
    return provider;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.IWorkbenchPart#addPropertyListener(org.eclipse.ui.
   * IPropertyListener)
   */
  @Override
  public void addPropertyListener(IPropertyListener listener) {
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.IWorkbenchPart#getTitle()
   */
  @Override
  public String getTitle() {
    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.IWorkbenchPart#getTitleImage()
   */
  @Override
  public Image getTitleImage() {
    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.IWorkbenchPart#getTitleToolTip()
   */
  @Override
  public String getTitleToolTip() {
    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.IWorkbenchPart#removePropertyListener(org.eclipse.ui.
   * IPropertyListener)
   */
  @Override
  public void removePropertyListener(IPropertyListener listener) {
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.IWorkbenchPart#setFocus()
   */
  @Override
  public void setFocus() {
    viewer.getTextWidget().setFocus();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.core.runtime.IAdaptable#getAdapter(java.lang.Class)
   */
  @Override
  public Object getAdapter(@SuppressWarnings("rawtypes") Class adapter) {
    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.eclipse.ui.ISaveablePart#doSave(org.eclipse.core.runtime.IProgressMonitor
   * )
   */
  @Override
  public void doSave(IProgressMonitor monitor) {
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.ISaveablePart#doSaveAs()
   */
  @Override
  public void doSaveAs() {
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.ISaveablePart#isDirty()
   */
  @Override
  public boolean isDirty() {
    return false;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.ISaveablePart#isSaveAsAllowed()
   */
  @Override
  public boolean isSaveAsAllowed() {
    return false;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.ISaveablePart#isSaveOnCloseNeeded()
   */
  @Override
  public boolean isSaveOnCloseNeeded() {
    return false;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.texteditor.ITextEditor#close(boolean)
   */
  @Override
  public void close(boolean save) {
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.texteditor.ITextEditor#isEditable()
   */
  @Override
  public boolean isEditable() {
    return true;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.texteditor.ITextEditor#doRevertToSaved()
   */
  @Override
  public void doRevertToSaved() {
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.texteditor.ITextEditor#setAction(java.lang.String,
   * org.eclipse.jface.action.IAction)
   */
  @Override
  public void setAction(String actionID, IAction action) {
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.texteditor.ITextEditor#getAction(java.lang.String)
   */
  @Override
  public IAction getAction(String actionId) {
    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.eclipse.ui.texteditor.ITextEditor#setActionActivationCode(java.lang
   * .String, char, int, int)
   */
  @Override
  public void setActionActivationCode(String actionId, char activationCharacter,
      int activationKeyCode, int activationStateMask) {
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.eclipse.ui.texteditor.ITextEditor#removeActionActivationCode(java.lang
   * .String)
   */
  @Override
  public void removeActionActivationCode(String actionId) {
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.texteditor.ITextEditor#showsHighlightRangeOnly()
   */
  @Override
  public boolean showsHighlightRangeOnly() {
    return false;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.texteditor.ITextEditor#showHighlightRangeOnly(boolean)
   */
  @Override
  public void showHighlightRangeOnly(boolean showHighlightRangeOnly) {
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.texteditor.ITextEditor#setHighlightRange(int, int,
   * boolean)
   */
  @Override
  public void setHighlightRange(int offset, int length, boolean moveCursor) {
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.texteditor.ITextEditor#getHighlightRange()
   */
  @Override
  public IRegion getHighlightRange() {
    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.texteditor.ITextEditor#resetHighlightRange()
   */
  @Override
  public void resetHighlightRange() {
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.texteditor.ITextEditor#getSelectionProvider()
   */
  @Override
  public ISelectionProvider getSelectionProvider() {
    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.texteditor.ITextEditor#selectAndReveal(int, int)
   */
  @Override
  public void selectAndReveal(int offset, int length) {
  }
}
