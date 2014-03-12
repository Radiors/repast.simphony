package repast.simphony.visualization.engine;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import repast.simphony.context.Context;
import repast.simphony.engine.environment.RunState;
import repast.simphony.visualization.IDisplay;
import simphony.util.messages.MessageCenter;

/**
 * Produces an IDisplay from a DisplayDescriptor.
 * 
 * @author Nick Collier
 * @author Eric Tatara
 */
public class DisplayProducer {

  private static final MessageCenter msg = MessageCenter.getMessageCenter(DisplayProducer.class);

  private DisplayDescriptor displayDescriptor;

  private Context<?> context;

  public DisplayProducer(Object contextID, RunState runState, DisplayDescriptor displayDescriptor) {
    this.displayDescriptor = displayDescriptor;
    Context<?> masterContext = runState.getMasterContext();
    setContext(masterContext, contextID);
  }

  private void setContext(Context<?> context, Object contextID) {
    if (context.getId().equals(contextID)) {
      this.context = context;
    } else {
      for (Context<?> child : context.getSubContexts()) {
        setContext(child, contextID);
      }
    }
  }

  public IDisplay createDisplay() throws IllegalAccessException, InvocationTargetException,
      InstantiationException, ClassNotFoundException, IOException, DisplayCreationException {
    if (displayDescriptor.getDisplayType() == DisplayType.TWO_D)
      //return new DisplayCreator2D(context, displayDescriptor).createDisplay();
      return new DisplayCreatorOGL2D(context, displayDescriptor).createDisplay();
    else if (displayDescriptor.getDisplayType() == DisplayType.THREE_D)
      return new DisplayCreator3D(context, displayDescriptor).createDisplay(); 
    
    
//    else if (displayDescriptor.getDisplayType() == DisplayDescriptor.DisplayType.GIS)
//      return new DisplayCreatorGIS(context, displayDescriptor).createDisplay();
//    else
//      return new DisplayCreator3DGIS(context, displayDescriptor).createDisplay();
    
    // TODO Projections: above code can be removed in favor of the registry approach
    //       if adopted for other displays.
    
    else {
    	VisualizationRegistryData data = VisualizationRegistry.getDataFor(displayDescriptor.getDisplayType());

    	try{
    		DisplayCreatorFactory factory = data.getDisplayCreatorFactory();
    		return factory.createDisplayCreator(context, displayDescriptor).createDisplay();
    	} 
    	catch (Exception ex) {
    		msg.error("No display implementation found for " + displayDescriptor.getDisplayType(), ex);
    	}
    }
    
    return null;
  }
}