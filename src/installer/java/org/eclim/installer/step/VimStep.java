package org.eclim.installer.step;

import java.awt.Component;

import java.io.File;
import java.io.FileInputStream;

import java.util.ArrayList;
import java.util.Properties;

import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import foxtrot.Task;
import foxtrot.Worker;

import net.miginfocom.swing.MigLayout;

import org.apache.commons.io.IOUtils;

import org.apache.commons.lang.StringUtils;

import org.apache.tools.ant.taskdefs.condition.Os;

import org.formic.Installer;

import org.formic.util.CommandExecutor;

import org.formic.wizard.form.GuiForm;

import org.formic.wizard.form.gui.component.FileChooser;

import org.formic.wizard.form.validator.ValidatorBuilder;

import org.formic.wizard.step.AbstractGuiStep;

/**
 * Step for choosing the vimfiles directory to install vim scripts in.
 *
 * @author Eric Van Dewoestine (ervandew@gmail.com)
 * @version $Revision$
 */
public class VimStep
  extends AbstractGuiStep
{
  private static final String[] WINDOWS_VIMS = {
    "C:/Program Files/Vim/vim70/vim.exe",
    "C:/Program Files/Vim/vim70/gvim.exe",
    "C:/Program Files/Vim/vim71/vim.exe",
    "C:/Program Files/Vim/vim71/gvim.exe"
  };

  private static final String[] UNIX_VIMS = {"vim", "gvim"};

  private static final String COMMAND =
    "redir! > <file> | silent! echo &rtp | quit";

  private JPanel panel;
  private FileChooser fileChooser;
  private boolean rtpAttempted;

  /**
   * Constructs the step.
   */
  public VimStep (String name, Properties properties)
  {
    super(name, properties);
  }

  /**
   * {@inheritDoc}
   * @see org.formic.wizard.step.GuiStep#init()
   */
  public Component init ()
  {
    GuiForm form = createForm();
    String files = fieldName("files");
    fileChooser = new FileChooser(JFileChooser.DIRECTORIES_ONLY);

    panel = new JPanel(new MigLayout(
          "wrap 2", "[fill]", "[] [] [fill, grow]"));
    panel.add(form.createMessagePanel(), "span");
    panel.add(new JLabel(Installer.getString(files)), "split");
    panel.add(fileChooser, "skip");

    form.bind(files, fileChooser.getTextField(),
        new ValidatorBuilder().required().isDirectory().fileExists().validator());

    return panel;
  }

  /**
   * {@inheritDoc}
   * @see org.formic.wizard.WizardStep#displayed()
   */
  public void displayed ()
  {
    if(!rtpAttempted){
      rtpAttempted = true;

      setBusy(true);
      try{
        String[] rtp = (String[])Worker.post(new Task(){
          public Object run () throws Exception {
            return getVimRuntimePath();
          }
        });

        if(rtp != null && rtp.length > 0){
          if(rtp.length == 1){
            fileChooser.getTextField().setText(rtp[0]);
          }else{
            final JList list = new JList(rtp);
            list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            JScrollPane scrollPane = new JScrollPane(list);
            panel.add(scrollPane, "span, grow");

            list.addListSelectionListener(new ListSelectionListener(){
              public void valueChanged (ListSelectionEvent event){
                if(!event.getValueIsAdjusting()){
                  fileChooser.getTextField()
                    .setText((String)list.getSelectedValue());
                }
              }
            });

            list.setSelectedIndex(0);
          }
        }
      }catch(Exception e){
        e.printStackTrace();
      }
      setBusy(false);
      fileChooser.getTextField().grabFocus();
    }
  }

  /**
   * Attempts to determine available paths in vim's runtime path.
   *
   * @return Array of paths or null if unable to determine any.
   */
  private String[] getVimRuntimePath ()
  {
    try{
      File tempFile = File.createTempFile("eclim_installer", null);
      String command = COMMAND.replaceFirst("<file>",
          tempFile.getAbsolutePath().replace('\\', '/').replaceAll(" ", "\\ "));

      String[] vims = null;
      if(Os.isFamily("windows")){
        vims = WINDOWS_VIMS;
      }else{
        vims = UNIX_VIMS;
      }

      String[] args = {null, "-X", "-u", "NONE", "-U", "NONE", "--cmd", command};
      for (int ii = 0; ii < vims.length; ii++){
        args[0] = vims[ii];
        CommandExecutor executor = CommandExecutor.execute(args, 5000);
        if(executor.getReturnCode() == 0){
          return parseVimRuntimePathResults(tempFile);
        }
        executor.destroy();
      }
    }catch(Exception e){
      e.printStackTrace();
    }

    return null;
  }

  /**
   * Parses the results of echoing vim runtime path to a file.
   *
   * @param file The file containing the results.
   * @return The results.
   */
  private String[] parseVimRuntimePathResults (File file)
  {
    FileInputStream in = null;
    try{
      String contents = IOUtils.toString(in = new FileInputStream(file));
      String[] paths = StringUtils.stripAll(StringUtils.split(contents, ','));
      ArrayList results = new ArrayList();
      for (int ii = 0; ii < paths.length; ii++){
        File path = new File(paths[ii]);
        if(path.isDirectory() && path.canWrite()){
          results.add(paths[ii].replace('\\', '/'));
        }
      }
      return (String[])results.toArray(new String[results.size()]);
    }catch(Exception e){
      e.printStackTrace();
    }finally{
      IOUtils.closeQuietly(in);
      file.deleteOnExit();
      file.delete();
    }
    return null;
  }
}
