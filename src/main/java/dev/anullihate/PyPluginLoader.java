package dev.anullihate;

import cn.nukkit.Server;
import cn.nukkit.event.plugin.PluginDisableEvent;
import cn.nukkit.event.plugin.PluginEnableEvent;
import cn.nukkit.plugin.*;
import cn.nukkit.utils.Utils;
import org.python.core.*;
import org.python.util.PythonInterpreter;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.*;
import java.util.regex.Pattern;

public class PyPluginLoader implements PluginLoader {

    private PyPlugins plugin;
    private Server server;

    private Map<PluginBase, File> pluginPath = new HashMap<>();

    public PyPluginLoader(Server server) {
        this.server = server;
        this.plugin = PyPlugins.getInstance();
    }

    public Plugin loadPlugin(File file) throws Exception {
        Properties props;
        PluginBase pythonPlugin = null;
        PluginDescription description = this.getPluginDescription(file);


        if (description != null) {
            if (this.server.getPluginManager().getPlugin(description.getName()) != null) {
                this.plugin.getLogger().warning("Can't load source plugin \"" + description.getName() + "\": plugin exists");
                return null;
            } else {
                try {
                    props = PySystemState.getBaseProperties();
                    props = setDefaultPythonPath(props, file.getAbsolutePath());

                    PySystemState state = new PySystemState();
                    state.initialize(System.getProperties(), props, null);
                    PyList pythonpath = state.path;
                    PyString filepath = new PyString(file.getAbsolutePath());
                    pythonpath.append(filepath);

                    String mainfile = "plugin.py";
                    InputStream instream = new FileInputStream(new File(file.getAbsolutePath(), mainfile));

                    PyDictionary table = new PyDictionary();
                    PythonInterpreter interpreter = new PythonInterpreter(table, state);

                    interpreter.execfile(instream);
                    instream.close();

                    String mainclass = description.getMain();
                    PyObject pyClass = interpreter.get(mainclass);

                    pythonPlugin = (PluginBase) pyClass.__call__().__tojava__(PluginBase.class);

                    File dataFolder = new File(file.getParentFile(), description.getName());

                    this.initPlugin(pythonPlugin, description, dataFolder, file);
                    this.pluginPath.put(plugin, file);
                    return pythonPlugin;
                } catch (ClassCastException e) {
                    throw new Exception("Main class \"" + description.getMain() + "\" does not extend PluginBase");
                }
            }
        } else {
            return null;
        }
    }

    private Properties setDefaultPythonPath(Properties props, String file_path) {
        String pythonPathProp = props.getProperty("python.path");
        String new_value;
        if (pythonPathProp==null)
        {
            new_value  = file_path;
        } else {
            new_value = pythonPathProp +java.io.File.pathSeparator + file_path + java.io.File.pathSeparator;
        }
        props.setProperty("python.path",new_value);
        return props;
    }

    public PluginDescription getPluginDescription(File file) {
        try {
            File yml = new File(file.getAbsolutePath(), "plugin.yml");
            return file.isDirectory() && yml.isFile() ? new PluginDescription(Utils.readFile(yml)) : null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public Pattern[] getPluginFilters() {
        return new Pattern[] {
                Pattern.compile("^(.*)_py$")
        };
    }

    // nukkit impl

    public Plugin loadPlugin(String filename) throws Exception {
        return this.loadPlugin(new File(filename));
    }

    public PluginDescription getPluginDescription(String filename) {
        return this.getPluginDescription(new File(filename));
    }

    private void initPlugin(PluginBase plugin, PluginDescription description, File dataFolder, File file) {
        plugin.init(this, this.server, description, dataFolder, file);
        plugin.onLoad();
    }

    public void enablePlugin(Plugin plugin) {
        if (!(plugin instanceof PluginBase)) {
            throw new IllegalArgumentException("Plugin is not associated with this PluginLoader");
        }

        if (!plugin.isEnabled()) {
            PluginBase pythonPlugin = (PluginBase) plugin;

            String pluginName = pythonPlugin.getDescription().getName();

            try {
                pythonPlugin.setEnabled(true);
            } catch (Throwable e) {
                server.getLogger().critical("Error occurred while enabling " + plugin.getDescription().getFullName()
                        + " There must be something wrong in your onEnable() in your python plugin");
            }

            server.getPluginManager().callEvent(new PluginEnableEvent(plugin));
        }
    }

    public void disablePlugin(Plugin plugin) {
        if (!(plugin instanceof PluginBase)) {
            throw new IllegalArgumentException("Plugin is not associated with this PluginLoader");
        }

        if (plugin.isEnabled()) {
            PluginBase pythonPlugin = (PluginBase) plugin;

            try {
                pythonPlugin.setEnabled(false);
            } catch (Throwable e) {
                server.getLogger().critical("Error occurred while disabling " + plugin.getDescription().getFullName()
                                + " There must be something wrong in your onEnable() in your python plugin");
            }

            server.getPluginManager().callEvent(new PluginDisableEvent(plugin));
        }
    }
}
