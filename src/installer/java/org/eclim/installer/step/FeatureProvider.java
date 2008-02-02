package org.eclim.installer.step;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import java.io.File;
import java.io.FilenameFilter;

import org.formic.Installer;

import org.formic.wizard.step.shared.Feature;

/**
 * Provider to supply avaiable features to FeatureListStep.
 *
 * @author Eric Van Dewoestine (ervandew@gmail.com)
 * @version $Revision$
 */
public class FeatureProvider
  implements org.formic.wizard.step.shared.FeatureProvider, PropertyChangeListener
{
  private static final String[] FEATURES =
    {"ant", "maven", "jdt", "wst", "python"};
    //{"ant", "maven", "jdt", "wst", "pdt", "python"};
    //{"ant", "maven", "jdt", "wst", "pydev"};

  private static final boolean[] FEATURES_ENABLED =
    {true, true, true, false, false};
    //{true, true, true, false, false, false};

  private static final String[][] FEATURES_DEPENDS =
    {{"jdt"}, null, null, null, null};
    //{{"jdt"}, null, null, null, {"wst"}, null};

  /**
   * {@inheritDoc}
   * @see org.formic.wizard.step.shared.FeatureProvider#getFeatures()
   */
  public Feature[] getFeatures ()
  {
    boolean[] enabled = new boolean[FEATURES.length];
    for (int ii = 0; ii < FEATURES.length; ii++){
      String path = Installer.getProject()
        .replaceProperties("${eclipse.home}/plugins/");
      final String pluginPath = "org.eclim." + FEATURES[ii] + "_";
      String[] list = new File(path).list(new FilenameFilter(){
        public boolean accept (File file, String name) {
          return name.startsWith(pluginPath);
        }
      });

      enabled[ii] = list.length > 0 ? true : FEATURES_ENABLED[ii];
    }

    Feature[] features = new Feature[FEATURES.length];
    for (int ii = 0; ii < features.length; ii++){
      features[ii] = new Feature(
          FEATURES[ii], enabled[ii], FEATURES_DEPENDS[ii]);
    }

    return features;
  }

  /**
   * {@inheritDoc}
   * @see PropertyChangeListener#propertyChange(PropertyChangeEvent)
   */
  public void propertyChange (PropertyChangeEvent evt)
  {
    // do nothing for now.
  }
}
